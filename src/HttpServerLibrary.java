import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServerLibrary {
    private static Logger logger = Logger.getLogger(HttpServerLibrary.class.getName());

    private ServerSocket serverSocket;
    private int port;
    private String pathToDirectory;

    public HttpServerLibrary(boolean isVerbose, int port, String pathToDirectory) {
        this.port = port;
        this.pathToDirectory = pathToDirectory;

        logger.setLevel(isVerbose ? Level.INFO : Level.WARNING);

        start();
    }

    private void start() {
        try {
            serverSocket = new ServerSocket(port);
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
                    readRequest(in);

                    // Prepare an HTTP response and send to the client
                    logger.log(Level.INFO, "Sending response to client...");
                    sendResponse(out);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method reads the requests sent by the client
    private String readRequest(BufferedReader reader) throws IOException {
        String line = reader.readLine(); // Status line
        if (line.isEmpty()) {
            showErrorAndExit("Bad request."); // TODO: change for an actual error code
        }

        if (!line.isEmpty()) {
            System.out.println(line);
            line = reader.readLine();
        }

        // 1st param will be either GET or POST
        // If post, then must collect the content of the body of the POST request since that's what will be dumped into the file

        // 2nd param will be file path

        // 3rd param will be http version

        return line;
    }

    // This method determines which type of response to create
    private void sendResponse(PrintWriter out) {
        if (true) { // GET request
            sendGetResponse(out);
        } else if (true) { // POST request
            sendPostResponse(out);
        }
    }

    // This method constructs a get response
    private void sendGetResponse(PrintWriter out) {
        // IF directory, then returns a list of the current files in the data directory
        // IF file, then returns the content of the file in the data directory
        // Stick with 1 format: simplest will be plaintext i think?
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
}
