package Client;

import Client.Requests.PostRequest;
import Client.Requests.Redirectable;
import Client.Requests.Request;
import Helpers.Packet;
import Helpers.PacketType;
import Helpers.Status;
import Helpers.UDPConnection;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.sql.Time;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is the client library. It takes care of opening the UDP connection, sending the request and reading the response.
 */
public class HttpClientLibrary {

    private DatagramSocket clientSocket;
    private Request request;
    private boolean isVerbose;
    private String responseFilePath;
    private int redirectCounter = 0;
    private final static int REDIRECT_MAXIMUM = 5;
    private BufferedWriter writer;
    private final static String EOL = "\r\n";
    private ArrayList<Packet> finalPacketsInOrder;

    private boolean SYN_ACKReceivedForHandshake = false;

    private static final Logger logger = Logger.getLogger(HttpClientLibrary.class.getName());

    public HttpClientLibrary(Request request, boolean isVerbose) {
        this(request, isVerbose, "");
    }

    public HttpClientLibrary(Request request, boolean isVerbose, String responseFilePath) {
        this.request = request;
        this.isVerbose = isVerbose;
        this.responseFilePath = responseFilePath;
        try {
            if (!responseFilePath.isEmpty())
                writer = new BufferedWriter(new FileWriter(responseFilePath));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        performRequest();
    }

    private void performRequest() {
        try {
            clientSocket = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }

        threeWayHandshake();
        sendRequest();
        readResponse();
        closeUDPConnection();
    }

    // ------------ 3-way Handshake --------------------------
    private void threeWayHandshake() {
        int initialSequenceNumber = UDPConnection.getRandomSequenceNumber();

        // Send SYN
        logger.info("Initiate 3-way handshake ...");
        logger.info("Send SYN packet with seq number " + initialSequenceNumber);
        UDPConnection.sendSYN(initialSequenceNumber, request.getPort(), request.getAddress(), clientSocket);

        // Start a timer
        Timer timer = new Timer();
        timer.schedule(new ResendSyn(initialSequenceNumber), UDPConnection.DELAY_BEFORE_TIMEOUT);

        // Receive SYN_ACK
        Packet packetSYNACK = receiveAndVerifySYN_ACK(initialSequenceNumber);

        // Send ACK
        logger.info("Respond with an ACK {ACK:" + (packetSYNACK.getSequenceNumber() + 1) + "}");
        UDPConnection.sendACK(packetSYNACK.getSequenceNumber() + 1, packetSYNACK.getPeerPort(), packetSYNACK.getPeerAddress(), clientSocket);

//        // Start a timer
//        Timer timer2 = new Timer();
//        timer2.scheduleAtFixedRate(new ResendAck(packetSYNACK), new Date(), UDPConnection.DELAY_BEFORE_TIMEOUT);
    }

    private Packet receiveAndVerifySYN_ACK(int initialSequenceNumber) {
        Packet packet;
        do {
            packet = UDPConnection.receivePacket(clientSocket);
        } while(packet.getType() != PacketType.SYN_ACK.value);

        SYN_ACKReceivedForHandshake = true;
        logger.info("Received a SYN_ACK packet");

        logger.info("Verifying ACK ...");
        int receivedAcknowledgment = getIntFromPayload(packet.getPayload());
        if (receivedAcknowledgment != initialSequenceNumber + 1) {
            logger.info("Unexpected ACK sequence number " + receivedAcknowledgment + "instead of " + (initialSequenceNumber + 1));
            UDPConnection.sendNAK(packet.getPeerPort(), packet.getPeerAddress(), clientSocket);
//            System.exit(-1);
        }

        logger.info("ACK is verified: {seq sent: " + initialSequenceNumber + ", seq received: " + receivedAcknowledgment + "}");
        return packet;
    }

    // ------------ 3-way Handshake --------------------------

    private void sendRequest() {
        logger.log(Level.INFO, "Constructing request to send to server...");
        String payload = constructPayload();

        logger.log(Level.INFO, "Building packets from request object...");
        ArrayList<Packet> packets = UDPConnection.buildPackets(payload, PacketType.DATA, request.getPort(), request.getAddress());

        logger.log(Level.INFO, "Sending packets to server using selective repeat...");
        UDPConnection.sendUsingSelectiveRepeat(packets, request.getPort(), request.getAddress(), clientSocket);
    }

    private String constructPayload() {
        String requestLine = request.getMethod().name() + " " + request.getPath() + request.getQuery() + " " + "HTTP/1.0" + EOL;
        String hostHeader = "Host: " + request.getHost() + EOL;

        String headers = "";
        if (request.getHeaders().size() > 0) {
            for (String header : request.getHeaders()) {
                headers += header + EOL;
            }
        }

        String body = "";
        if (request instanceof PostRequest) {
            body = EOL +
                    ((PostRequest) request).getData() + EOL;
        }

        return requestLine + hostHeader + headers + body + EOL;
    }

    private void readResponse() {
        // Receive all DATA packets from server
        finalPacketsInOrder = UDPConnection.receiveAllPackets(clientSocket);

        // Read response
         readResponseFrom(createResponseFromPackets());
    }

    private void readResponseFrom(String responsePayload) {
        logger.log(Level.INFO, "Reading server's response...");

        String[] responseLines = responsePayload.split(EOL);
        int lineCounter = 0;
        try {
            // Read status line and check if it is a redirect
            String line = responseLines.length >= 1 ? responseLines[lineCounter] : null;

            boolean shouldRedirect = shouldRedirect(line);

            // Parse through response headers
            line = responseLines.length >= 2 ? responseLines[++lineCounter] : null;
            while (line != null && !line.isEmpty() && !line.startsWith("{")) {

                //Search headers for Location: redirectURI
                if (shouldRedirect && line.contains("Location:")) {
                    printLine(line); // print the location header
                    String redirectURI = line.substring(line.indexOf(":") + 1).trim();
                    redirectTo(redirectURI);
                    return;
                }

                if (isVerbose)
                    printLine(line);
                line = (responseLines.length-1) >= ++lineCounter ? responseLines[lineCounter] : null;
            }

            // There is an error if the redirect link is not in the response headers
            if (shouldRedirect) {
                System.out.println("Response code 302 but no redirection URI found!");
                System.exit(0);
            }

            // Print out response body
            while (line != null) {
                printLine(line);
                line = (responseLines.length-1) >= ++lineCounter ? responseLines[lineCounter] : null;
            }

            if (writer != null)
                writer.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private String createResponseFromPackets() {
        String finalRequest = "";
        for(Packet packet: finalPacketsInOrder) {
            finalRequest += new String(packet.getPayload(), UTF_8);
        }

        return finalRequest;
    }

    private void closeUDPConnection() {
        logger.log(Level.INFO, "Client closing connection...");
        clientSocket.close();
        System.exit(0);
    }

    private boolean shouldRedirect(String line) {
        boolean shouldRedirect = false;
        if (line != null) {
            String[] statusLineComponents = line.trim().split(" ");
            if (statusLineComponents.length >= 3) {
                if (isVerbose) printLine(line);
                try {
                    int statusCode = Integer.parseInt(statusLineComponents[1]);
                    boolean isRedirectCode = statusCode == Status.MOVED_PERMANENTLY.getCode() ||
                            statusCode == Status.FOUND.getCode() ||
                            statusCode == Status.TEMPORARY_REDIRECT.getCode();
                    shouldRedirect = isRedirectCode && request instanceof Redirectable;
                } catch (NumberFormatException exception) {
                    System.out.println("Status code cannot be converted to int: " + exception);
                    System.exit(0);
                }

            } else {
                System.out.println("Response's status line is not formatted properly: " + line);
                System.exit(0);
            }

        } else {
            System.out.println("Status line is not there!");
            System.exit(0);
        }
        return shouldRedirect;
    }

    private void redirectTo(String redirectURI) {
        // Close existing socket
        closeUDPConnection();

        if (redirectCounter < REDIRECT_MAXIMUM && request instanceof Redirectable) {
            System.out.println("------------ REDIRECTED -------------");
            this.request = ((Redirectable) request).getRedirectRequest(redirectURI);
            redirectCounter++;
            performRequest();
        }
    }

    private void writeToFile(String line) {
        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void printLine(String line) {
        if (writer != null)
            writeToFile(line);
        else
            System.out.println(line);
    }

    private int getIntFromPayload(byte[] payload){
        IntBuffer intBuf = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        int[] array = new int[intBuf.remaining()];
        intBuf.get(array);
        return array[0];
    }

    private class ResendSyn extends TimerTask {
        private int initialSequenceNumber;

        ResendSyn(int initialSequenceNumber) {
            this.initialSequenceNumber = initialSequenceNumber;
        }

        public void run() {
            if (!SYN_ACKReceivedForHandshake) {
                UDPConnection.sendSYN(initialSequenceNumber, request.getPort(), request.getAddress(), clientSocket);

                // Start a timer
                Timer timer = new Timer();
                timer.schedule(new ResendSyn(initialSequenceNumber), UDPConnection.DELAY_BEFORE_TIMEOUT);
            }
        }
    }

//    private class ResendAck extends TimerTask {
//        private Packet packetToResend;
//
//        ResendAck(Packet packetToResend) {
//            this.packetToResend = packetToResend;
//        }
//
//        public void run() {
//            // check if syn_ack received b/c if yes, handshake didnt work properly
//            if(UDPConnection.receivePacket(clientSocket).getType() == PacketType.SYN_ACK.value) {
//                UDPConnection.sendACK(packetToResend.getSequenceNumber() + 1, packetToResend.getPeerPort(), packetToResend.getPeerAddress(), clientSocket);
//
//                // Start a timer
//                Timer timer = new Timer();
//                timer.schedule(new ResendAck(packetToResend), UDPConnection.DELAY_BEFORE_TIMEOUT);
//            }
//        }
//    }
}
