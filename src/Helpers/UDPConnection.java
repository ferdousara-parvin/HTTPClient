package Helpers;

import Client.Requests.Request;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class UDPConnection {
    //TODO: Think about how to implement the queue of chopped up payload packets will be ... when to move the window?
     public final static int WINDOW_SIZE = 5;
     public final static SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);

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

    public static Packet receiveData(DatagramSocket socket) throws IOException {
        byte[] buff = new byte[Packet.MAX_LEN];
        DatagramPacket datagramPacket = new DatagramPacket(buff, Packet.MAX_LEN);
        socket.receive(datagramPacket);

        return Packet.fromBytes(datagramPacket.getData());
    }

}
