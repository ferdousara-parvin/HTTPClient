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
    private boolean isVerbose;

    /**
     * A HttpClientLibrary constructor.
     * @param request: A Request object
     * @param isVerbose: A boolean value
     */
    public HttpClientLibrary(Request request, boolean isVerbose) {
        this.request = request;
        this.isVerbose = isVerbose;
        openTCPConnection();
        sendRequest(request);
        readResponse();
    }

    /**
     * This method opens a TCP connection using a socket.
     */
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

    /**
     * This method sends a request to the server.
     * @param request: A request object
     */
    private void sendRequest(Request request) {
        request.performRequest(out);
    }

    /**
     * This method reads the response from the server.
     */
    private void readResponse() {
        String line = "";
        try {
            line = in.readLine();

            // Consider verbose option
            if (!isVerbose) {
                while (line != null && !line.startsWith("{")) {
                    line = in.readLine();
                }
            }

            // Print out response
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
