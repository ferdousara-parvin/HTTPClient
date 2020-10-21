package Server;

import Helpers.HTTPMethod;
import Helpers.Status;
import Server.Responses.Response;

import java.io.*;
import java.net.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the server library. It takes care of opening the TCP connection, reading the request and sending the response.
 */
class HttpServerLibrary {
    private int port;
    private Path baseDirectory;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private static final Logger logger = Logger.getLogger(HttpServerLibrary.class.getName());

    HttpServerLibrary(boolean isVerbose, int port, String pathToDirectory) {
        this.port = port;
        this.baseDirectory = Paths.get(pathToDirectory).toAbsolutePath();

        logger.setLevel(isVerbose ? Level.INFO : Level.WARNING);

        start();
    }

    private void start() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Server socket was unable to be initialized", exception);
            System.exit(3);
        }

        logger.log(Level.INFO, "Listening on port " + port + " ...");

        while (true) {
            try {
                socket = serverSocket.accept();
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException exception) {
                logger.log(Level.WARNING, "Server socket was unable to connect to the client", exception);
                continue;
            }

            // TODO: Nice to have: add timeout if client hasn't send anything after some time, 408 ERROR CODE
            logger.log(Level.INFO, "Client connected to server");

            logger.log(Level.INFO, "Reading client's request...");
            Response response = createResponse();

            logger.log(Level.INFO, "Sending response to client...");
            sendResponse(response);

            logger.log(Level.INFO, "Server closing connection...");
            closeTCPConnection();
        }
    }

    // This method reads the requests sent by the client and creates a Response object
    private Response createResponse() {

        // Parse request line
        HTTPMethod requestHttpMethod = null;
        File file = null;
        String httpVersion = "";
        Status status;

        String line;
        try {
            line = in.readLine();

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
                                    Path path = baseDirectory.getFileSystem().getPath(statusLineComponents[URL]);
                                    file = Paths.get(baseDirectory.toString(), path.toString()).toFile();
                                } catch (InvalidPathException exception) {
                                    logger.log(Level.WARNING, "Request path is invalid!", exception);
                                    return new Response(Status.BAD_REQUEST);
                                }
                                break;
                            case HTTP_VERSION:
                                httpVersion = statusLineComponents[HTTP_VERSION];
                                if (!(httpVersion.equalsIgnoreCase("HTTP/1.0") || httpVersion.equalsIgnoreCase("HTTP/1.1")))
                                    return new Response(Status.BAD_REQUEST);
                                if (httpVersion.equalsIgnoreCase("HTTP/1.1"))
                                    return new Response(Status.HTTP_VERSION_NOT_SUPPORTED);
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
            line = in.readLine();
            while (line != null && !line.isEmpty()) {
                clientHeaders.add(line);
                line = in.readLine();
            }

            // Parse data (for POST)
            StringBuilder data = new StringBuilder();
            if (requestHttpMethod.equals(HTTPMethod.POST)) {
                line = in.readLine();
                while (line != null && !line.isEmpty()) {
                    data.append(line);
                    data.append("\n");
                    line = in.readLine();
                }
            }

            return new Response(requestHttpMethod, Status.OK, clientHeaders, data.toString(), file);

        } catch (IOException exception) {
            return new Response(Status.INTERNAL_SERVER_ERROR);
        }
    }

    // This method determines which type of response to create
    private void sendResponse(Response response) {
        switch (response.getHttpMethod()) {
            case GET:
                performGet(response);
                break;
            case POST:
                performPost(response);
                break;
        }

        out.print(response.getResponse());
        out.flush();
    }

    // This method constructs a get response
    private void performGet(Response response) {
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
            String fileContent = extractContentFromFile(file.getAbsolutePath());
            if (fileContent == null) {
                response.setStatus(Status.NOT_FOUND);
            }
            response.setData(fileContent);
        }
    }

    // This method constructs a post response
    private void performPost(Response response) {
        // Output data to file
        response.getFile().getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(response.getFile()))) {
            writer.write(response.getData());
        } catch (IOException e) {
            e.printStackTrace();
            response.setStatus(Status.NOT_FOUND);
        }
    }

    private HTTPMethod getMethodFromRequest(String requestMethod) {
        if (requestMethod.equalsIgnoreCase(HTTPMethod.GET.name())) return HTTPMethod.GET;
        else if (requestMethod.equalsIgnoreCase(HTTPMethod.POST.name())) return HTTPMethod.POST;
        else
            return null;
    }

    // Helper method to extract data from a file given its path
    private static String extractContentFromFile(String filePath) {
        StringBuilder data = new StringBuilder();
        File file = new File(filePath);

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

    private void closeTCPConnection() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Server was unable to close the connection with the client", exception);
        }
    }
}
