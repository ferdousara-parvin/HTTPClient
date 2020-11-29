package Server;

import Client.HttpClientLibrary;
import Helpers.*;
import Server.Responses.Response;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is the server library. It takes care of opening the UDP connection, reading the request and sending the response.
 */
class HttpServerLibrary {
    private int port;
    private Path baseDirectory;
    private DatagramSocket serverSocket;
    private ArrayList<Packet> finalPacketsInOrder;

    private int peerPort;
    private InetAddress peerAddress;

    private final static String EOL = "\r\n";

    private static final Logger logger = Logger.getLogger(HttpServerLibrary.class.getName());

    HttpServerLibrary(boolean isVerbose, int port, Path baseDirectory) {
        this.port = port;
        this.baseDirectory = baseDirectory;

        logger.setLevel(isVerbose ? Level.INFO : Level.WARNING);

        start();
    }

    private void start() {
        try {
            serverSocket = new DatagramSocket(port);
            logger.log(Level.INFO, "Listening on port " + port + " ...");
        } catch (IOException e) {
            e.printStackTrace();
        }

        threeWayHandshake();
        sendResponse();
        closeUDPConnection();
    }

    // ------------- 3-way handshake -----------------------

    private void threeWayHandshake() {
        // Receive SYN
        Packet packetSYN = receiveAndVerifySYN();
        peerAddress = packetSYN.getPeerAddress();
        peerPort = packetSYN.getPeerPort();

        // Send SYN_ACK
        int sequenceNumberToSynchronize = UDPConnection.getRandomSequenceNumber();
        logger.info(" Respond with a SYN_ACK {SYN:" + sequenceNumberToSynchronize +
                ", ACK: " + (packetSYN.getSequenceNumber() + 1) + "}");
        UDPConnection.sendSYN_ACK(packetSYN.getSequenceNumber() + 1,
                sequenceNumberToSynchronize, packetSYN.getPeerPort(), packetSYN.getPeerAddress(), serverSocket);

        // Receive ACK
        UDPConnection.receiveAndVerifyFinalACK(sequenceNumberToSynchronize, serverSocket);
    }

    private Packet receiveAndVerifySYN() {
        Packet packet = UDPConnection.receivePacket(serverSocket);

        UDPConnection.verifyPacketType(PacketType.SYN, packet, serverSocket);
        logger.info("Received a SYN packet");
        return packet;
    }

    // ------------- 3-way handshake -----------------------

    private void sendResponse() {
        logger.log(Level.INFO, "Receiving packets from client...");
        finalPacketsInOrder = UDPConnection.receiveAllPackets(serverSocket);

        logger.log(Level.INFO, "Building response from packets...");
        Response response = createResponseFrom(createRequestFromPackets());

        logger.log(Level.INFO, "Sending response to client...");
        sendResponse(response);
    }

    // This method reads the requests sent by the client and creates a Response object
    private Response createResponseFrom(String request) {
        // Parse request line
        HTTPMethod requestHttpMethod = null;
        File file = null;

        String[] requestLines = request.split(EOL);
        int lineCounter = 0;
        String line = "";

        line = requestLines.length >= 1 ? requestLines[lineCounter] : null;

        if (line != null) {
            String[] statusLineComponents = line.trim().split(" ");
            if (statusLineComponents.length == 3) {

                for (int position = 0; position < statusLineComponents.length; position++) {
                    final int METHOD = 0;
                    final int URL = 1;
                    final int HTTP_VERSION = 2;

                    switch (position) {
                        case METHOD:
                            requestHttpMethod = getMethodFromRequest(statusLineComponents[METHOD]);
                            if (requestHttpMethod == null) return new Response(Status.NOT_IMPLEMENTED);
                            break;
                        case URL:
                            try {
                                if (statusLineComponents[URL].contains("../"))
                                    return new Response(Status.BAD_REQUEST);
                                Path path = baseDirectory.getFileSystem().getPath(statusLineComponents[URL]);
                                file = Paths.get(baseDirectory.toString(), path.toString()).toFile();
                            } catch (InvalidPathException exception) {
                                logger.log(Level.WARNING, "Request path is invalid!", exception);
                                return new Response(Status.BAD_REQUEST);
                            }
                            break;
                        case HTTP_VERSION:
//                                httpVersion = statusLineComponents[HTTP_VERSION];
                            // Uncomment these lines if the server does only support early versions of HTTP (kept for demonstration purposes)
//                                if (!(httpVersion.equalsIgnoreCase("HTTP/1.0") || httpVersion.equalsIgnoreCase("HTTP/1.1")))
//                                    return new Response(Status.BAD_REQUEST);
//                                if (httpVersion.equalsIgnoreCase("HTTP/1.1"))
//                                    return new Response(Status.HTTP_VERSION_NOT_SUPPORTED);
                            break;
                        default:
                            break;
                    }
                }
            } else {
                return new Response(Status.BAD_REQUEST);
            }
        } else {
            return new Response(Status.BAD_REQUEST);
        }

        // Parse Headers
        List<String> clientHeaders = new ArrayList<>();
        line = requestLines.length >= 2 ? requestLines[++lineCounter] : null;
        while (line != null && !line.isEmpty()) {
            clientHeaders.add(line);
            line = requestLines.length >= ++lineCounter ? requestLines[lineCounter] : null;
        }

        // Parse data (for POST)
        StringBuilder data = new StringBuilder();
        if (requestHttpMethod.equals(HTTPMethod.POST)) {
            line = requestLines.length >= ++lineCounter ? requestLines[lineCounter] : null;
            while (line != null && !line.isEmpty()) {
                data.append(line);
                data.append("\n");
                line = requestLines.length >= ++lineCounter ? requestLines[lineCounter] : null;
            }
        }

        return new Response(requestHttpMethod, Status.OK, clientHeaders, data.toString(), file);

    }

    private String createRequestFromPackets() {
        String finalRequest = "";
        for(Packet packet: finalPacketsInOrder) {
            finalRequest += new String(packet.getPayload(), UTF_8);
        }

        return finalRequest;
    }

    private void sendResponse(Response response) {
        logger.log(Level.INFO, "Constructing response to send to client...");
        if (response.getHttpMethod() != null) {
            switch (response.getHttpMethod()) {
                case GET:
                    performGet(response);
                    break;
                case POST:
                    performPost(response);
                    break;
            }
        }

        logger.log(Level.INFO, "Building packets from response object...");
        ArrayList<Packet> packets = UDPConnection.buildPackets(response.getResponse(), PacketType.DATA, peerPort, peerAddress);

        logger.log(Level.INFO, "Sending packets to client using selective repeat...");
        UDPConnection.sendUsingSelectiveRepeat(packets, peerPort, peerAddress, serverSocket);
    }

    // This method constructs a get response
    private void performGet(Response response) {
        if (!response.getFile().exists()) {
            response.setStatus(Status.NOT_FOUND);
        } else {
            if (Files.isReadable(response.getFile().toPath())) {
                // Populate data to send back
                StringBuilder data = new StringBuilder();
                File file = response.getFile();
                if (file.isDirectory()) { // Directory
                    String[] children = file.list();
                    if (children != null) {
                        for (String child : children)
                            data.append(child + "\n");
                        response.setData(data.toString());
                    } else
                        response.setData("No files in the directory.");
                } else { // File
                    String fileContent = extractContent(response.getFile().getAbsoluteFile());
                    if (fileContent == null) {
                        response.setStatus(Status.NOT_FOUND);
                    }
                    response.setData(fileContent);
                }
            } else
                response.setStatus(Status.FORBIDDEN);
        }
    }

    // This method constructs a post response
    private void performPost(Response response) {
        // Output data to file
        response.getFile().getParentFile().mkdirs();
        boolean isWritable = (response.getFile().exists() && Files.isWritable(response.getFile().toPath())) || (!response.getFile().exists() && Files.isWritable(response.getFile().getParentFile().toPath()));
        if (isWritable) {
            if (!response.getFile().exists()) {
                response.setStatus(Status.CREATED);
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(response.getFile()))) {
                writer.write(response.getData());
            } catch (IOException e) {
                e.printStackTrace();
                response.setStatus(Status.NOT_FOUND);
            }
        } else
            response.setStatus(Status.FORBIDDEN);
    }

    private HTTPMethod getMethodFromRequest(String requestMethod) {
        if (requestMethod.equalsIgnoreCase(HTTPMethod.GET.name())) return HTTPMethod.GET;
        else if (requestMethod.equalsIgnoreCase(HTTPMethod.POST.name())) return HTTPMethod.POST;
        else
            return null;
    }

    // Helper method to extract data from a file given its path
    private static String extractContent(File file) {
        StringBuilder data = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String content = "";
            while ((content = br.readLine()) != null)
                data.append(content).append("\n");
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Requested file was not found!", exception);
            return null;
        }
        return data.toString().trim();
    }

    private void closeUDPConnection() {
        logger.log(Level.INFO, "Server closing connection...");
        serverSocket.close();
        System.exit(0);
    }
}
