import Helpers.HTTPMethod;
import Helpers.Status;
import Responses.Response;

import java.io.*;
import java.net.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class HttpServerLibrary {
    private int port;
    private Path baseDirectory;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private static final Logger logger = Logger.getLogger(HttpServerLibrary.class.getName());
    private final String EOL = "\r\n";

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

//                // TODO: debugging purposes
//                String line = in.readLine();
//                while(line != null) {
//                    System.out.println(line);
//                    line = in.readLine();
//                }

            logger.log(Level.INFO, "Reading client's request...");
            Response response = createResponse();

            logger.log(Level.INFO, "Sending response to client...");
            sendResponse(response);

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
                    // TODO: do switch case
                    for (int position = 0; position < statusLineComponents.length; position++) {
                        int methodIndex = 0;
                        int urlIndex = 1;
                        int httpVersionIndex = 2;
                        if (position == methodIndex) {
                            requestHttpMethod = getMethodFromRequest(statusLineComponents[methodIndex]);
                            if (requestHttpMethod == null) {
                                return new Response(Status.NOT_IMPLEMENTED);
                            }
                        } else if (position == urlIndex) {
                            try {
                                Path path = baseDirectory.getFileSystem().getPath(statusLineComponents[urlIndex]); // TODO: how does it work? Can we have a null Path?
                                file = Paths.get(baseDirectory.toString(), path.toString()).toFile();
                            } catch (InvalidPathException exception) {
                                logger.log(Level.WARNING, "Request path is invalid!", exception);
                                return new Response(Status.BAD_REQUEST);
                            }
                        } else {
                            httpVersion = statusLineComponents[httpVersionIndex];
                            if (!(httpVersion.equalsIgnoreCase("HTTP/1.0" ) || httpVersion.equalsIgnoreCase("HTTP/1.1"))) {
                                return new Response(Status.BAD_REQUEST);
                            }

                            if (httpVersion.equalsIgnoreCase("HTTP/1.1")) {
                                return new Response(Status.HTTP_VERSION_NOT_SUPPORTED);
                            }
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

            status = Status.OK;
            return new Response(requestHttpMethod, status, clientHeaders, data.toString(), file);

        } catch (IOException exception) {
            return new Response(Status.INTERNAL_SERVER_ERROR);
        }
    }

    // This method determines which type of response to create
    private void sendResponse(Response response) {
        if (response.getHttpMethod().equals(HTTPMethod.GET)) {
            sendGetResponse(response);
        } else if (response.getHttpMethod().equals(HTTPMethod.POST)) {
            sendPostResponse(response);
        } else {
            out.print(response.getStatusLine());
            out.print(EOL);
        }
    }

    // This method constructs a get response
    private void sendGetResponse(Response response) {
        // Populate data to send back
        File file = response.getFile();
        if (file.isDirectory()) {
            String[] children = file.list();
            if (children != null) {
                for (String child : children)
                    response.setData(response.getData() + child + "\n");
            }
            else
                response.setData("No files in the directory.");
        } else {
            String fileContent = extractContentFromFile(file.getAbsolutePath(), response);
            response.setData(fileContent);
        }

        // TODO: Go through client headers for content-length, content-type, content disposition

        // Output response to client
        out.print(response.getStatusLine());
        if (!response.getStatus().equals(Status.NOT_FOUND)) { // TODO: NOT_FOUND is set in extractContentFromFile() if file cannot be open. Not sure if best place to do it.
            // TODO: Send a proper GET response message
        }
        out.print(EOL);
    }

    // This method constructs a post response
    private void sendPostResponse(Response response) {
        // TODO: should create OR overwrite the file specified by the method in the data directory with the content of the body of the request.

        // Get Content-Length
        int contentLength = response.getData().length();
        for(String header: response.getClientHeaders()) {
            if (header.contains("Content-Length:")) {
                contentLength = Integer.parseInt(header.substring(header.indexOf(":") + 1).trim());
            }
        }

        // TODO: Output data to file


        // Output response to client
        out.print(response.getStatusLine());
        out.print("Server: localhost"); // TODO: correct?
        out.print("Content-Length: " + contentLength);
        out.print(EOL);
    }

    private HTTPMethod getMethodFromRequest(String requestMethod) {
        if (requestMethod.equalsIgnoreCase(HTTPMethod.GET.name())) return HTTPMethod.GET;
        else if (requestMethod.equalsIgnoreCase(HTTPMethod.POST.name())) return HTTPMethod.POST;
        else
            return null;
    }

    // Helper method to extract data from a file given its path
    private static String extractContentFromFile(String filePath, Response response) {
        System.out.println("File path: " + filePath); // TODO: debugging purposes, remove later
        StringBuilder data = new StringBuilder();
        File file = new File(filePath);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String content = "";
            while ((content = br.readLine()) != null)
                data.append(content).append("\n");
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Requested file was not found!", exception);
            response.setStatus(Status.NOT_FOUND);
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
