import Helpers.HTTPMethod;
import Requests.GetRequest;
import Requests.PostRequest;
import Requests.Request;

import java.io.*;
import java.net.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServerLibrary {
    private static final Logger logger = Logger.getLogger(HttpServerLibrary.class.getName());

    private final int port;
    private final Path baseDirectory;

    public HttpServerLibrary(boolean isVerbose, int port, String pathToDirectory) {
        this.port = port;
        this.baseDirectory = Paths.get(pathToDirectory).toAbsolutePath();

        logger.setLevel(isVerbose ? Level.INFO : Level.WARNING);

        start();
    }

    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            logger.log(Level.INFO, "Listening on port " + port + " ....");
            while (true) {

                // Client connects to server
                // TODO: add timeout, 408 ERROR CODE
                try (Socket socket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    logger.log(Level.INFO, "Client connected to server");

                    // Read HTTP request from the client socket
                    logger.log(Level.INFO, "Reading client's request...");
                    Request request = createRequest(in);

                    // Prepare an HTTP response and send to the client
                    logger.log(Level.INFO, "Sending response to client...");
                    sendResponse(out, request);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method reads the requests sent by the client
    private Request createRequest(BufferedReader in) throws IOException {
        //Components needed to create a request
        HTTPMethod requestHttpMethod = null;
        Path path = null;
        // int content-length
        // String data

        String line = in.readLine(); // Status line

        //Extract url and http method from request line
        String[] requestLineContents = line.split(" ");

        if (line != null) {
            String[] statusLineComponents = line.trim().split(" ");
            if (statusLineComponents.length >= 3) {
                for (int position = 0; position < statusLineComponents.length; position++) {
                    int METHOD_INDEX = 0;
                    int URL_INDEX = 1;
                    if (position == METHOD_INDEX)
                        requestHttpMethod = getMethodFromRequest(statusLineComponents[METHOD_INDEX]);
                    else if (position == URL_INDEX) {
                        try {
                            path = baseDirectory.getFileSystem().getPath(statusLineComponents[URL_INDEX]);
                        } catch (InvalidPathException exception) {
                            logger.log(Level.WARNING, "Request path is not valid!", exception);

                        }
                    } else break;
                }
            } else {
                logger.log(Level.WARNING, "Request not formatted properly");
                //TODO: Send response with malformed request code
                return null;
            }
        } else {
            logger.log(Level.WARNING, "Request line not present");
            //TODO: Send response with malformed request code
            return null;
        }

        // Skip through all header lines
        line = in.readLine();
        while (line != null && !line.isEmpty() && !line.startsWith("{")) {
            //TODO: Search headers for content-length if the method is POST
//            if (POST && line.contains("Content-length:")) {
//            }
            line = in.readLine();
        }

        //TODO: Construct data from the request body
        // Should we let the user know that it is weird for them to have a body with a GET request?
        while (line != null) {
            line = in.readLine();
        }

        switch (requestHttpMethod) {
            case GET:
                return new GetRequest("", path == null ? null : path.toString(), "", null);
            case POST:
//                return new PostRequest();
                return null;
            default:
                return null;
        }
    }

    // This method determines which type of response to create
    private void sendResponse(PrintWriter out, Request request) throws IOException {
        if (request instanceof GetRequest) { // GET request
            sendGetResponse(out, (GetRequest) request);
        } else if (request instanceof PostRequest) { // POST request
            sendPostResponse(out);
        } else if (request == null) {
            //TODO: What are the reasons for request to be null?
        }
    }

    // This method constructs a get response
    private void sendGetResponse(PrintWriter out, GetRequest getRequest) throws IOException {
        // IF directory, then returns a list of the current files in the data directory
        // IF file, then returns the content of the file in the data directory
        // Stick with 1 format: simplest will be plaintext i think?
        if (getRequest.getPath() == null) {
            //TODO: Show error + set the request status line
            logger.log(Level.WARNING, "Requested Path was malformed");
        } else {
            File file = Paths.get(baseDirectory.toString(), getRequest.getPath()).toFile();
            if (file.isDirectory()) {
                for (String child : file.list())
                    //TODO: print to the outputstream
                    System.out.println(child);
            } else {
                String fileContent = extractContentFromFile(file.getAbsolutePath());
                //TODO: print to the outputstream
                System.out.println(fileContent);
            }
        }
    }

    // This method constructs a post response
    private void sendPostResponse(PrintWriter out) {
        // should create OR overwrite the file specified by the method in the data directory with the content of the body of the request.
    }

    // Helper method to show the error message before exiting
    // TODO: change to display error codes instead
    private static void showErrorAndExit(String message) {
        System.err.print(message + "\n");
        System.exit(0);
    }

    private HTTPMethod getMethodFromRequest(String requestMethod) {
        if (requestMethod.equalsIgnoreCase(HTTPMethod.GET.name())) return HTTPMethod.GET;
        else if (requestMethod.equalsIgnoreCase(HTTPMethod.POST.name())) return HTTPMethod.POST;
        else {
            logger.log(Level.WARNING, "Request method is unknown");
            return null;
        }
    }

    // Helper method to extract data from a file given it's path
    //TODO: Duplicate method (legit copy pasted) from HttpCli => reintroduce the HttpLibrary parent class?
    private static String extractContentFromFile(String filePath) {
        StringBuilder data = new StringBuilder();
        File file = new File(filePath);

        try( BufferedReader br = new BufferedReader(new FileReader(file))) {
            String content = "";
            while ((content = br.readLine()) != null)
                data.append(content).append("\n");
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Requested file was not found!", exception);
            //TODO: Output status line with code 404 (Resource not found)
        }

        return data.toString().trim();
    }
}
