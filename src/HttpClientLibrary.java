import Requests.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * This class is the client library. It takes care of opening the TCP connection, sending the request and reading the response.
 */
public class HttpClientLibrary {

    private PrintStream out;
    private BufferedReader in;
    private Socket clientSocket;
    private Request request;

    public HttpClientLibrary(Request request) {
        this.request = request;
        openTCPConnection();
        sendRequest(request);
        readResponse();
    }

    private void openTCPConnection(){
        try {
            // Connect to the server
            clientSocket = new Socket(request.getHost(), request.getPort());

            // Create input output streams to read and write to the server
            out = new PrintStream(clientSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void sendRequest(Request request) {
            // Perform request
            request.performRequest(out);
    }

    private void readResponse() {
        // Read response from server
        String line = "";
        try {
            line = in.readLine();

            while (line != null) {
                System.out.println(line);
                line = in.readLine();
            }

            // Close streams
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
