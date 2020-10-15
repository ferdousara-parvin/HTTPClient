import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServerLibrary {
    private ServerSocket serverSocket;
    private Socket serverClientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final int PORT = 8080;

    public HttpServerLibrary() {
        openTCPConnection();
        readRequest();
        sendResponse();
        closeTCPConnection();
    }

    private void openTCPConnection() {
        try {
            serverSocket = new ServerSocket(PORT);
            serverClientSocket = serverSocket.accept();
            out = new PrintWriter(serverClientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(serverClientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method reads the requests sent by the client
    private void readRequest() {

    }

    // This method determines which type of response to create
    private void sendResponse() {

    }

    // This method constructs a get response
    private void constructGetResponse() {

    }

    // This method constructs a post response
    private void constructPostResponse() {

    }

    private void closeTCPConnection() {
        try {
            in.close();
            out.close();
            serverClientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
