import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServerLibrary {
    private ServerSocket serverSocket;
    private int port;
    private final int DEFAULT_PORT = 8080;

    public HttpServerLibrary(int port) {
        this.port = port;
        start();
    }

    public HttpServerLibrary() {
        this.port = DEFAULT_PORT;
        start();
    }

    private void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Listening on port " + port + " ...."); // TODO: debugging message
            while(true) {

                // Client connects to server
                // TODO: add timeout, 408 ERROR CODE
                try (Socket socket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    System.out.println("Client connected to server"); // TODO: debugging message

                    // Read HTTP request from the client socket
                    System.out.println("Reading client's request..."); // TODO: debugging message
                    readRequest(in);

                    // Prepare an HTTP response and send to the client
                    System.out.println("Sending response to client..."); // TODO: debugging message
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

        if(!line.isEmpty()) {
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
        if(true) { // GET request
            sendGetResponse(out);
        }
        else if (true) { // POST request
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
