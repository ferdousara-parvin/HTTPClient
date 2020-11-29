package Helpers;

import Client.Requests.Request;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class UDPConnection {
    //TODO: Think about how to implement the queue of chopped up payload packets will be ... when to move the window?
     public final static int WINDOW_SIZE = 5;
     public final static SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);
     public final static Random random = new Random(400);

    public static void sendData(int sequenceNumber, String payload, Request request, DatagramSocket socket) throws IOException {
        int MAX_PAYLOAD_SIZE = Packet.MAX_LEN - Packet.MIN_LEN;

        if(payload.getBytes().length > MAX_PAYLOAD_SIZE){
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

    public static void sendSYN(int sequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) throws IOException {
        send(PacketType.SYN, sequenceNumber, peer_port, peer_address, socket);
    }

    public static void sendACK(int incrementedSequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) throws IOException {
        send(PacketType.ACK, incrementedSequenceNumber, peer_port, peer_address, socket);
    }

    public static void sendNAK(int peer_port, InetAddress peer_address, DatagramSocket socket) throws IOException {
        send(PacketType.NAK, 0, peer_port, peer_address, socket);
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

    private static void send(PacketType type, int sequenceNumber, int peer_port, InetAddress peer_address, DatagramSocket socket) throws IOException {
        Packet packet = new Packet.Builder()
                .setType(type.value)
                .setSequenceNumber(sequenceNumber)
                .setPortNumber(peer_port)
                .setPeerAddress(peer_address)
                .create();

        byte[] packetToBytes = packet.toBytes();
        socket.send(new DatagramPacket(packetToBytes, packetToBytes.length, routerAddress));
    }

    public static Packet receivePacket(DatagramSocket socket) throws IOException {
        byte[] buff = new byte[Packet.MAX_LEN];
        DatagramPacket datagramPacket = new DatagramPacket(buff, Packet.MAX_LEN);
        socket.receive(datagramPacket);

        return Packet.fromBytes(datagramPacket.getData());
    }

    public static int getRandomSequenceNumber(){
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
