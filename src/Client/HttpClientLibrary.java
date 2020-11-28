package Client;

import Client.Requests.PostRequest;
import Client.Requests.Redirectable;
import Client.Requests.Request;
import Helpers.Packet;
import Helpers.Status;
import Helpers.UDPConnection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is the client library. It takes care of opening the TCP connection, sending the request and reading the response.
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
            handshake();
            sendRequest();
            readResponse();
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            closeUDPConnection();
            System.exit(0);
        }
    }

    //TODO: Implement the 3-way handshake
    private void handshake() {
        // Random sequence number
//        int initialSequenceNumber = 0;
//        sendSYN(initialSequenceNumber);
//        int secondSequenceNumber = receiveSYN_ACK(initialSequenceNumber);
//        sendSYN(++secondSequenceNumber);
    }
//
//    //Send synchronization sequence number
//    private void sendSYN(int sequenceNumber) {
//        //1. send a SYNC packet with the sequenceNumber
//    }
//
//    //Receive and verify acknowledgment, return the second synchronization seq number
//    private int receiveSYN_ACK(int initialSequenceNumber) {
//        // 1. Make sure that the received sequence number is equal to initialSequenceNumber + 1
//        //if not verifies, send nak
//        // 2. return the synchronization sequence number sent by the server
//        return 0;
//    }

    private void sendRequest() throws IOException {
        String payload = constructPayload();
        UDPConnection.sendData(1, payload, request, clientSocket);
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
        logger.log(Level.INFO, "Reading server's response...");
        Packet responsePacket;
        String responsePayload = "";
        try {
            byte[] buff = new byte[Packet.MAX_LEN];
            DatagramPacket datagramPacket = new DatagramPacket(buff, Packet.MAX_LEN);
            clientSocket.receive(datagramPacket);

            responsePacket = Packet.fromBytes(datagramPacket.getData());
            responsePayload = new String(responsePacket.getPayload(), UTF_8);
            logger.info("Packet: {" + responsePacket + "}");
            logger.info("Payload: {" + responsePayload + "}");

        } catch (SocketException socketException) {
            socketException.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

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
                line = responseLines.length >= ++lineCounter ? responseLines[lineCounter] : null;
            }

            // There is an error if the redirect link is not in the response headers
            if (shouldRedirect) {
                System.out.println("Response code 302 but no redirection URI found!");
                System.exit(0);
            }

            // Print out response body
            while (line != null) {
                printLine(line);
                line = responseLines.length >= ++lineCounter ? responseLines[lineCounter] : null;
            }

            if (writer != null)
                writer.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void closeUDPConnection() {
        clientSocket.close();
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

}
