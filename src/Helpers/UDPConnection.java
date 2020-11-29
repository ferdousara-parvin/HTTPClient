package Helpers;

import Client.Requests.Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class UDPConnection {
    //TODO: Think about how to implement the queue of chopped up payload packets will be ... when to move the window?
    public final static int WINDOW_SIZE = 5;
    public final static int MAX_SEQUENCE_NUMBER = 20;
    public final static SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);
    public final static Random random = new Random(400);
    public final static int MAX_PAYLOAD_SIZE = Packet.MAX_LEN - Packet.MIN_LEN;

    public static void sendData(int sequenceNumber, String payload, Request request, DatagramSocket socket) throws IOException {
        if (payload.getBytes().length > MAX_PAYLOAD_SIZE) {
            //TODO: Divide the data into correctly sized payloads
        }

        Packet packet = new Packet.Builder()
                .setType(PacketType.DATA.value)
                .setSequenceNumber(sequenceNumber)
                .setPortNumber(request.getPort())
                .setPeerAddress(request.getAddress())
                .setPayload(payload.getBytes())
                .create();

        byte[] packetToBytes = packet.toBytes();
        socket.send(new DatagramPacket(packetToBytes, packetToBytes.length, routerAddress));
    }

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
                Packet p = buildPacket(packetType, payload, peer_port, peer_address);
                p = p.toBuilder().setSequenceNumber(counterSequenceNumber % MAX_SEQUENCE_NUMBER).create();
                counterSequenceNumber++;
                arrayOfPackets.add(p);
                counter = counter + len;
            }
            // TODO: window tail
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

    public static void sendSYN(int sequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) {
        send(PacketType.SYN, sequenceNumber, peer_port, peer_address, socket);
    }

    public static void sendACK(int incrementedSequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) {
        send(PacketType.ACK, incrementedSequenceNumber, peer_port, peer_address, socket);
    }

    public static void sendNAK(int peer_port, InetAddress peer_address, DatagramSocket socket) {
        send(PacketType.NAK, 0, peer_port, peer_address, socket);
    }

    public static void sendFIN(int sequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) {
        send(PacketType.FIN, sequenceNumber, peer_port, peer_address, socket);
    }


    public static void sendSYN_ACK(int incrementedSequenceNumber, int sequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) throws IOException {
        // Send acknowledgment as payload and new number to synchronize as sequence number
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        byteBuffer.putInt(incrementedSequenceNumber);
        byte[] payload = byteBuffer.array();
        Packet packet = new Packet.Builder()
                .setType(PacketType.SYN_ACK.value)
                .setSequenceNumber(sequenceNumber)
                .setPortNumber(peer_port)
                .setPeerAddress(peer_address)
                .setPayload(payload)
                .create();

        byte[] packetToBytes = packet.toBytes();
        socket.send(new DatagramPacket(packetToBytes, packetToBytes.length, routerAddress));
    }

    // TODO: use sendPAcket
    private static void send(PacketType type, int sequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) {
        Packet packet = new Packet.Builder()
                .setType(type.value)
                .setSequenceNumber(sequenceNumber)
                .setPortNumber(peer_port)
                .setPeerAddress(peer_address)
                .create();

        byte[] packetToBytes = packet.toBytes();
        try {
            socket.send(new DatagramPacket(packetToBytes, packetToBytes.length, routerAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendPacket(Packet packet, DatagramSocket socket) {
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

    public static int getRandomSequenceNumber() {
        int sequenceNumber = random.nextInt();
        sequenceNumber *= sequenceNumber < 0 ? -1 : 1;
        return sequenceNumber;
    }

    public static void verifyPacketType(PacketType expectedPacketType, Packet packet, DatagramSocket socket) throws IOException {
        while (packet.getType() != expectedPacketType.value) {
            if (packet.getType() != PacketType.NAK.value) {
                sendNAK(packet.getPeerPort(), packet.getPeerAddress(), socket);
            }
            socket.close();
            System.exit(-1);
        }
    }

}
