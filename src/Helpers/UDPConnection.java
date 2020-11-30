package Helpers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Logger;

public class UDPConnection {
    public final static int WINDOW_SIZE = 3;
    public final static int MAX_SEQUENCE_NUMBER =10000 * WINDOW_SIZE;
    public final static int MAX_PAYLOAD_SIZE = Packet.MAX_LEN - Packet.MIN_LEN;
    public final static long DELAY_BEFORE_TIMEOUT = 100000;
    public final static SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);

    private static final Logger logger = Logger.getLogger(UDPConnection.class.getName());

    // Selective repeat
    private static int windowHead = 0;
    private static int windowTail = UDPConnection.WINDOW_SIZE - 1;
    private static ArrayList<Boolean> ackList;
    private static ArrayList<Boolean> sentList;

    // Receiver receives packets from sender
    private static int rcv_base = 0;
    private static int rcv_tail = UDPConnection.WINDOW_SIZE - 1;
    private static ArrayList<Packet> finalPacketsInOrder = new ArrayList<>();
    private static ArrayList<Packet> packetsInBuffer = new ArrayList<>(MAX_SEQUENCE_NUMBER);

    // -----------BUILD PACKETS---------------------

    public static ArrayList<Packet> buildPackets(String entirePayload, PacketType packetType, int peer_port, InetAddress peer_address) {
        // Note: Payload of each packet should be between 0 and 1013 bytes
        ArrayList<Packet> arrayOfPackets = new ArrayList<>();
        byte[] entirePayloadInBytes = entirePayload.getBytes();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(entirePayloadInBytes);
        byte[] buffer = new byte[UDPConnection.MAX_PAYLOAD_SIZE];
        byte[] payload;
        int len;
        int counter = 0;
        int counterSequenceNumber = 0;

        try {
            while ((len = byteArrayInputStream.read(buffer)) > 0) {
                payload = Arrays.copyOfRange(entirePayloadInBytes, counter, counter + len);
                Packet packet = buildPacket(packetType, payload, peer_port, peer_address);
                packet = packet.toBuilder().setSequenceNumber(counterSequenceNumber % MAX_SEQUENCE_NUMBER).create();
                arrayOfPackets.add(packet);
                counterSequenceNumber++;
                counter = counter + len;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return arrayOfPackets;
    }

    private static Packet buildPacket(PacketType packetType, byte[] payload, int peer_port, InetAddress peer_address) {
        return new Packet.Builder()
                .setType(packetType.value)
                .setPortNumber(peer_port)
                .setPeerAddress(peer_address)
                .setPayload(payload)
                .create();
    }

    // -----------FLAGS---------------------

    public static void sendSYN(int randomSequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) {
        byte[] payload = {};
        send(PacketType.SYN, randomSequenceNumber, peer_port, peer_address, socket, payload);
    }

    public static void sendACK(int incrementedSequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) {
        byte[] payload = {};
        send(PacketType.ACK, incrementedSequenceNumber, peer_port, peer_address, socket, payload);
    }

    public static void sendNAK(int peer_port, InetAddress peer_address, DatagramSocket socket) {
        byte[] payload = {};
        send(PacketType.NAK, 0, peer_port, peer_address, socket, payload);
    }

    public static void sendFIN(int randomSequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) {
        byte[] payload = {};
        send(PacketType.FIN, randomSequenceNumber, peer_port, peer_address, socket, payload);
    }

    public static void sendSYN_ACK(int incrementedSequenceNumber, int randomSequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) {
        // Send acknowledgment as payload and new number to synchronize as sequence number
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        byteBuffer.putInt(incrementedSequenceNumber);
        byte[] payload = byteBuffer.array();
        send(PacketType.SYN_ACK, randomSequenceNumber, peer_port, peer_address, socket, payload);
    }


    private static void send(PacketType type, int sequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket, byte[] payload) {
        Packet packet = new Packet.Builder()
                .setType(type.value)
                .setSequenceNumber(sequenceNumber)
                .setPortNumber(peer_port)
                .setPeerAddress(peer_address)
                .setPayload(payload)
                .create();

        sendPacket(packet, socket);
    }

    private static void sendPacket(Packet packet, DatagramSocket socket) {
        // Send packet
        byte[] packetToBytes = packet.toBytes();
        try {
            socket.send(new DatagramPacket(packetToBytes, packetToBytes.length, routerAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Packet receivePacket(DatagramSocket socket) {
        DatagramPacket datagramPacket;
        try {
            byte[] buff = new byte[Packet.MAX_LEN];
            datagramPacket = new DatagramPacket(buff, Packet.MAX_LEN);
            socket.receive(datagramPacket);
            return Packet.fromBytes(datagramPacket.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void receiveAndVerifyFinalACK(int sequenceNumberToSynchronize, DatagramSocket socket) {
        Packet packetACK = UDPConnection.receivePacket(socket);
        UDPConnection.verifyPacketType(PacketType.ACK, packetACK, socket);

        logger.info("Received a ACK packet");
        logger.info("Verifying ACK...");
        if (packetACK.getSequenceNumber() != sequenceNumberToSynchronize + 1) {
            logger.info("Unexpected ACK sequence number " + packetACK.getSequenceNumber() + "instead of " + (sequenceNumberToSynchronize + 1));
            UDPConnection.sendNAK(packetACK.getPeerPort(), packetACK.getPeerAddress(), socket);
            System.exit(-1);
        }
        logger.info("ACK is verified: {seq sent: " + sequenceNumberToSynchronize + ", seq received: " + packetACK.getSequenceNumber() + "}");
    }

    public static void verifyPacketType(PacketType expectedPacketType, Packet packet, DatagramSocket socket) {
        if (packet.getType() != expectedPacketType.value) {
            if (packet.getType() != PacketType.NAK.value) {
                sendNAK(packet.getPeerPort(), packet.getPeerAddress(), socket);
            }
            socket.close();
            System.exit(-1);
        }
    }

    public static int getRandomSequenceNumber() {
        return (int) (Math.random() * 100) + 500;
    }

    // --------------SELECTIVE REPEAT------------------------------
    public static void sendUsingSelectiveRepeat(ArrayList<Packet> packets, int peerPort, InetAddress peerAddress, DatagramSocket socket) {
        // Set up
        ackList = new ArrayList<>(Arrays.asList(new Boolean[packets.size()]));
        sentList = new ArrayList<>(Arrays.asList(new Boolean[packets.size()]));
        Collections.fill(ackList, Boolean.FALSE);
        Collections.fill(sentList, Boolean.FALSE);

        // Send data packets using selective repeat
        while (ackList.contains(false)) {
            sendWindow(packets, socket);

            Packet response = receivePacket(socket);
            if (response != null && response.getType() == PacketType.ACK.value) {
                ackList.set((int) response.getSequenceNumber() - 1, true);
                // Slide window
                if (windowTail < ackList.size() && ackList.get(windowHead)) {
                    int newWindowHead = windowHead;
                    int newWindowTail = windowTail;
                    for (int i = windowHead; i <= windowTail; i++) {
                        if (ackList.get(i)) {
                            newWindowHead += 1;
                            newWindowTail += 1;
                        } else {
                            break;
                        }
                    }
                    windowHead = newWindowHead;
                    windowTail = newWindowTail;
                }
            }
        }

        // Send FIN to let server know that client is done sending data
        int finalSequenceNumber = UDPConnection.getRandomSequenceNumber();
        sendFIN(finalSequenceNumber, peerPort, peerAddress, socket);

        // Wait for ACK from server
        receiveAndVerifyFinalACK(finalSequenceNumber, socket);

        resetVars();
    }

    private static void sendWindow(ArrayList<Packet> packets, DatagramSocket socket) {
        for (int i = windowHead; i <= windowTail && windowTail < ackList.size(); i++) {
            if (!ackList.get(i) && !sentList.get(i)) { // TODO: do we actually need ackList check here?
                Packet packet = packets.get(i);
                sendPacket(packets.get(i), socket);

                // Start a timer
                Timer timer = new Timer();
                timer.schedule(new ResendPacket(packet, i, socket), DELAY_BEFORE_TIMEOUT);

                sentList.set(i, true);
            }
        }
    }

    private static class ResendPacket extends TimerTask {
        private Packet packetToBeSentAgain;
        private int indexInAckList;
        private DatagramSocket socket;

        ResendPacket(Packet packetToBeSentAgain, int indexInAckList, DatagramSocket socket) {
            this.packetToBeSentAgain = packetToBeSentAgain;
            this.indexInAckList = indexInAckList;
            this.socket = socket;
        }

        public void run() {
            // ackList can be null when the last packet has been already acknowledged and the vars for the SR have been reset
            if (ackList != null && !ackList.get(indexInAckList)) {
                logger.info("Packet #" + packetToBeSentAgain.getSequenceNumber() + " has been resent due to timeout");
                UDPConnection.sendPacket(packetToBeSentAgain, socket);

                // Start a timer
                Timer timer = new Timer();
                timer.schedule(new ResendPacket(packetToBeSentAgain, indexInAckList, socket), DELAY_BEFORE_TIMEOUT);
            }
        }
    }

    // --------------SELECTIVE REPEAT------------------------------

    // seq in [head -n, head - 1]

    private static boolean inRange(int number, int lowerBound, int upperBound) {
        return number >= lowerBound && number <= upperBound;
    }

    private static boolean isSequenceNumberInPreviousWindow(int sequenceNumber) {
        // if (h - n < 0)
        if (rcv_base - WINDOW_SIZE < 0) {
//            [0, h-1] || [(h-n)%MAX, MAX -1]
            return inRange(sequenceNumber, 0, rcv_base - 1)
                    || inRange(sequenceNumber, (rcv_base - WINDOW_SIZE) % MAX_SEQUENCE_NUMBER, MAX_SEQUENCE_NUMBER - 1);
        }

        return inRange(sequenceNumber, rcv_base - WINDOW_SIZE, rcv_base - 1);
    }


    public static ArrayList<Packet> receiveAllPackets(DatagramSocket socket) {
        Packet receivedPacket = receivePacket(socket);

        // TODO: handle null exception
        while (receivedPacket != null && receivedPacket.getType() != PacketType.FIN.value) {
            if (receivedPacket.getType() == PacketType.DATA.value) {
                // Packet with sequence number b/w rcv_base and rcv_base+N-1 where N = window size
                if (rcv_tail > rcv_base) {
                    // seq in [head, tail]
                    if (receivedPacket.getSequenceNumber() >= rcv_base && receivedPacket.getSequenceNumber() <= rcv_tail) {
                        addPacketInBuffer(socket, receivedPacket);
                    }
                    // seq in [head -n, head - 1]
                    else if (isSequenceNumberInPreviousWindow(receivedPacket.getSequenceNumber())) {
                        UDPConnection.sendACK(receivedPacket.getSequenceNumber() + 1, receivedPacket.getPeerPort(), receivedPacket.getPeerAddress(), socket);
                    }
                } else if (rcv_tail < rcv_base) {
                    // seq in [h, MAX-1] || [0, t]
                    if (receivedPacket.getSequenceNumber() >= rcv_base && receivedPacket.getSequenceNumber() <= MAX_SEQUENCE_NUMBER - 1
                            || receivedPacket.getSequenceNumber() >= 0 && receivedPacket.getSequenceNumber() <= rcv_tail) {
                        // Send ACK
                        addPacketInBuffer(socket, receivedPacket);
                    }
                    // seq in [head -n, head - 1]
                    else if (isSequenceNumberInPreviousWindow(receivedPacket.getSequenceNumber())) {
                        UDPConnection.sendACK(receivedPacket.getSequenceNumber() + 1, receivedPacket.getPeerPort(), receivedPacket.getPeerAddress(), socket);
                    }
                } else {
                    //ERROR: CRASH
                }

            }

            receivedPacket = UDPConnection.receivePacket(socket);
        }

        logger.info("Receiver received all packets from sender since receiver received FIN with sequence number " + receivedPacket.getSequenceNumber());

        UDPConnection.sendACK(receivedPacket.getSequenceNumber() + 1, receivedPacket.getPeerPort(), receivedPacket.getPeerAddress(), socket);

        return finalPacketsInOrder;
    }

    private static void addPacketInBuffer(DatagramSocket socket, Packet receivedPacket) {
        // Send ACK
        UDPConnection.sendACK(receivedPacket.getSequenceNumber() + 1, receivedPacket.getPeerPort(), receivedPacket.getPeerAddress(), socket);

        // Buffer packet
        packetsInBuffer.add(receivedPacket.getSequenceNumber(), receivedPacket);

        //Slide Window
        if (receivedPacket.getSequenceNumber() == rcv_base) {
            for (int i = rcv_base; i < packetsInBuffer.size() && packetsInBuffer.get(i) != null; i++) {
                finalPacketsInOrder.add(packetsInBuffer.get(i));
                packetsInBuffer.set(i, null);
                rcv_base = (rcv_base + 1) % MAX_SEQUENCE_NUMBER;
            }

        }

        rcv_tail = (rcv_base + UDPConnection.WINDOW_SIZE - 1) % MAX_SEQUENCE_NUMBER;
    }

    private static void resetVars() {
        windowHead = 0;
        windowTail = UDPConnection.WINDOW_SIZE - 1;
        ackList = null;
        sentList = null;

        rcv_base = 0;
        rcv_tail = UDPConnection.WINDOW_SIZE - 1;
        finalPacketsInOrder = new ArrayList<>();
        packetsInBuffer = new ArrayList<>(MAX_SEQUENCE_NUMBER);
    }
}
