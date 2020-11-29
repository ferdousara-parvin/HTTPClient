package Server;

import Helpers.*;
import Server.Responses.Response;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is the server library. It takes care of opening the TCP connection, reading the request and sending the response.
 */
class HttpServerLibrary {
    private int port;
    private Path baseDirectory;
    private DatagramSocket serverSocket;

    private final static String EOL = "\r\n";

    private static final Logger logger = Logger.getLogger(HttpServerLibrary.class.getName());

    HttpServerLibrary(boolean isVerbose, int port, Path baseDirectory) {
        this.port = port;
        this.baseDirectory = baseDirectory;

        logger.setLevel(isVerbose ? Level.INFO : Level.WARNING);

        start();
    }

    private void start() {
        String requestPayload = "";
        Packet requestPacket = null;
        try {
            serverSocket = new DatagramSocket(port);
            logger.log(Level.INFO, "Listening on port " + port + " ...");

            handshake();

            requestPacket = UDPConnection.receivePacket(serverSocket);
            requestPayload = new String(requestPacket.getPayload(), UTF_8);

        } catch (IOException socketException) {
            socketException.printStackTrace();
        }

        logger.log(Level.INFO, "Reading client's request...");
        Response response = createResponseFrom(requestPayload);

        logger.log(Level.INFO, "Sending response to client...");
        sendResponse(response, requestPacket);

        logger.log(Level.INFO, "Server closing connection...");
        closeUDPConnection();
    }

    //TODO: Implement the 3-way handshake
    private void handshake() throws IOException {
        // Receive SYN
        Packet packet = receiveSYNPacket();

        //Send SYN_ACK
        int sequenceNumberToSynchronize = UDPConnection.getRandomSequenceNumber();
        respondWithSYN_ACK(sequenceNumberToSynchronize, packet);

        // Receive ACK
        receiveAndVerifyFinalACK(sequenceNumberToSynchronize);
    }

    private Packet receiveSYNPacket() throws IOException {
        Packet packet = UDPConnection.receivePacket(serverSocket);

        UDPConnection.verifyPacketType(PacketType.SYN, packet, serverSocket);
        logger.info("Received a SYN packet");
        return packet;
    }

    private void respondWithSYN_ACK(int sequenceNumber, Packet packet) throws IOException {
        logger.info(" Respond with a SYN_ACK {SYN:" + sequenceNumber +
                ", ACK: " + (packet.getSequenceNumber() + 1) + "}");
        UDPConnection.sendSYN_ACK(packet.getSequenceNumber() + 1,
                sequenceNumber, packet.getPeerPort(), packet.getPeerAddress(), serverSocket);
    }

    private void receiveAndVerifyFinalACK(int sequenceNumberToSynchronize) throws IOException {
        Packet packet = UDPConnection.receivePacket(serverSocket);
        UDPConnection.verifyPacketType(PacketType.ACK, packet, serverSocket);

        logger.info("Received a ACK packet");
        logger.info("Verifying ACK ...");
        if (packet.getSequenceNumber() != sequenceNumberToSynchronize + 1) {
            logger.info("Unexpected ACK sequence number " + packet.getSequenceNumber() + "instead of " + (sequenceNumberToSynchronize + 1));
            UDPConnection.sendNAK(packet.getPeerPort(), packet.getPeerAddress(), serverSocket);
            System.exit(-1);
        }
        logger.info("ACK is verified: {seq sent: " + sequenceNumberToSynchronize + ", seq received: " + packet.getSequenceNumber());
    }

    //TODO: Receive data using the ARQ method   [IMPLEMENT THE SLECTIVE-REPEAT ARQ]
    private void receiveData() {
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

    // This method determines which type of response to create
    private void sendResponse(Response response, Packet requestPacket) {
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

        logger.log(Level.INFO, "Server constructed response...");
        logger.log(Level.INFO, response.getResponse());

        String payload = response.getResponse();
        Packet responsePacket = requestPacket.toBuilder()
                .setPayload(payload.getBytes())
                .create();

        byte[] packetToBytes = responsePacket.toBytes();

        try {
            serverSocket.send(new DatagramPacket(packetToBytes, packetToBytes.length, UDPConnection.routerAddress));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
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
        serverSocket.close();
    }
}
