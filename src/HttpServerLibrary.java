import Helpers.HTTPMethod;
import Helpers.Status;
import Requests.GetRequest;
import Requests.PostRequest;
import Requests.Request;

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
    private static final Logger logger = Logger.getLogger(HttpServerLibrary.class.getName());
    private int port;
    private Path baseDirectory;

    HttpServerLibrary(boolean isVerbose, int port, String pathToDirectory) {
        this.port = port;
        this.baseDirectory = Paths.get(pathToDirectory).toAbsolutePath();

        logger.setLevel(isVerbose ? Level.INFO : Level.WARNING);

        start();
    }

    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            logger.log(Level.INFO, "Listening on port " + port + " ...");
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                logger.log(Level.INFO, "Client connected to server");

                // TODO: Nice to have: add timeout if client hasn't send anything after some time, 408 ERROR CODE

//                // TODO: debugging purposes
//                String line = in.readLine();
//                while(line != null) {
//                    System.out.println(line);
//                    line = in.readLine();
//                }

                logger.log(Level.INFO, "Reading client's request...");
                Request request = createRequest(in);


                logger.log(Level.INFO, "Sending response to client...");
                sendResponse(out, request);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method reads the requests sent by the client
    private Request createRequest(BufferedReader in) throws IOException {
        // Parse request line
        HTTPMethod requestHttpMethod = null;
        Path path = null;
        String httpVersion = "";

        String line = in.readLine();

        if (line != null) {
            String[] statusLineComponents = line.trim().split(" ");
            if (statusLineComponents.length >= 3) {
                for (int position = 0; position < statusLineComponents.length; position++) {
                    int methodIndex = 0;
                    int urlIndex = 1;
                    int httpVersionIndex = 2;
                    if (position == methodIndex)
                        requestHttpMethod = getMethodFromRequest(statusLineComponents[methodIndex]);
                    else if (position == urlIndex) {
                        try {
                            path = baseDirectory.getFileSystem().getPath(statusLineComponents[urlIndex]);
                        } catch (InvalidPathException exception) {
                            logger.log(Level.WARNING, "Request path is invalid!", exception);
                            return null;
//                            out.print(Status.BAD_REQUEST.toString());
                        }
                    } else if (position == httpVersionIndex) {
                        httpVersion = statusLineComponents[httpVersionIndex];
                    } else break;
                }
            } else {
//                out.print(Status.BAD_REQUEST.toString());
                return null;
            }
        } else {
            logger.log(Level.WARNING, "No request sent to the server");
            //TODO: Send response with malformed request code.
            return null;
        }

        // Parse Headers
        int contentLength = 0;
        List<String> headers = new ArrayList<>();
        line = in.readLine();
        while (line != null && !line.isEmpty()) {
            headers.add(line);
            if (line.contains("Content-Length:")) {
                contentLength = Integer.parseInt(line.substring(15).trim());
            }
            line = in.readLine();
        }

        // Parse data
        StringBuilder data = new StringBuilder();
        if (requestHttpMethod.equals(HTTPMethod.POST)) {
            line = in.readLine();
            StringBuilder allData = new StringBuilder();
            while (line != null && !line.isEmpty()) {
                allData.append(line);
                line = in.readLine();
            }

            if (allData.length() > 0) {
                if (contentLength == 0) {
                    contentLength = allData.length();
                }

                data.append(allData.substring(0, contentLength-1));
            }
        }

        switch (requestHttpMethod) {
            case GET:
                return new GetRequest("", path == null ? null : path.toString(), "", headers);
            case POST:
                return new PostRequest("", path == null ? null : path.toString(), "", headers, data.toString());
            default:
//                out.print(Status.NOT_IMPLEMENTED.toString());
                return null;
        }
    }

    // This method determines which type of response to create
    private void sendResponse(PrintWriter out, Request request) throws IOException {
        if (request instanceof GetRequest) {
            sendGetResponse(out, (GetRequest) request);
        } else if (request instanceof PostRequest) {
            sendPostResponse(out);
        } else if (request == null) {
            out.print(Status.BAD_REQUEST.toString());
        }
    }

    // This method constructs a get response
    private void sendGetResponse(PrintWriter out, GetRequest getRequest) {
        if (getRequest.getPath() == null) {
            logger.log(Level.WARNING, "Requested path was malformed");
            out.print(Status.NOT_FOUND.toString());
        } else {
            File file = Paths.get(baseDirectory.toString(), getRequest.getPath()).toFile();
            if (file.isDirectory()) {
                out.print(Status.OK.toString());
                for (String child : file.list())
                    out.println(child);
            } else {
                String fileContent = extractContentFromFile(file.getAbsolutePath());
                out.print(Status.OK.toString());
                out.println(fileContent);
            }
        }
    }

    // This method constructs a post response
    private void sendPostResponse(PrintWriter out) {
        // should create OR overwrite the file specified by the method in the data directory with the content of the body of the request.
    }

    private HTTPMethod getMethodFromRequest(String requestMethod) {
        if (requestMethod.equalsIgnoreCase(HTTPMethod.GET.name())) return HTTPMethod.GET;
        else if (requestMethod.equalsIgnoreCase(HTTPMethod.POST.name())) return HTTPMethod.POST;
        else {
//            out.print(Status.NOT_IMPLEMENTED.toString());
            return null;
        }
    }

    // Helper method to extract data from a file given its path
    private static String extractContentFromFile(String filePath) {
        System.out.println(filePath); // TODO: debugging purposes
        StringBuilder data = new StringBuilder();
        File file = new File(filePath);

        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String content = "";
            while ((content = br.readLine()) != null)
                data.append(content).append("\n");
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Requested file was not found!", exception);
//            out.print(Status.NOT_FOUND.toString());
        }

        return data.toString().trim();
    }
}
