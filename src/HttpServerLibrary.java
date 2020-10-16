import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

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
            System.out.println("Listening for connection on port " + port + " ....");
            while(true) {

                // Client connects to server
                try (Socket socket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    System.out.println("Client connected to server");

                    // Read HTTP request from the client socket
                    readRequest(in);

                    // Prepare an HTTP response and send to the client
                    sendResponse(out);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method reads the requests sent by the client
    private void readRequest(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        while (!line.isEmpty()) {
            System.out.println(line);
            line = reader.readLine();
        }
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

    }

    // This method constructs a post response
    private void sendPostResponse(PrintWriter out) {

    }

    // TODO: remove b/c using try-with-ressource
    private void closeClientConnection(PrintWriter out, BufferedReader in, Socket clientSocket) {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
