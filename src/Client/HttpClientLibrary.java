package Client;

import Client.Requests.PostRequest;
import Client.Requests.Redirectable;
import Client.Requests.Request;

import java.io.*;
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
    private String responseFilePath;
    private int redirectCounter = 0;
    private final static int REDIRECT_MAXIMUM = 5;
    private BufferedWriter writer;
    private final static String EOL = "\r\n";

    public HttpClientLibrary(Request request, boolean isVerbose) {
        this(request, isVerbose, "");
    }

    public HttpClientLibrary(Request request, boolean isVerbose, String responseFilePath) {
        this.request = request;
        this.isVerbose = isVerbose;
        this.responseFilePath = responseFilePath;
        try {
            if (!responseFilePath.isEmpty())
                writer = new BufferedWriter(new FileWriter(responseFilePath));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        performRequest();
    }

    private void performRequest() {
        try {
            clientSocket = new Socket(request.getHost(), request.getPort());
            out = new PrintStream(clientSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            sendRequest();
            readResponse();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeTCPConnection();
            System.exit(0);
        }
    }

    private void sendRequest() {
        out.print(request.getMethod().name() + " " + request.getPath() + request.getQuery() + " " + "HTTP/1.0" + EOL);
        out.print("Host: " + request.getHost() + EOL);

        // Send request headers
        if(request.getHeaders().size() > 0) {
            for (String header : request.getHeaders()) {
                out.print(header + EOL);
            }
        }

        // Send data
        if (request instanceof PostRequest) {
            out.print(EOL);
            out.print(((PostRequest) request).getData() + EOL);
        }
        out.print(EOL);
    }

    private void readResponse() {
        String line = "";

        try {
            // Read status line and check if it is a redirect
            line = in.readLine();

            boolean shouldRedirect = shouldRedirect(line);

            // Parse through response headers
            line = in.readLine();
            while (line != null && !line.isEmpty() && !line.startsWith("{")) {

                //Search headers for Location: redirectURI
                if (shouldRedirect && line.contains("Location:")) {
                    printLine(line); // print the location header
                    String redirectURI = line.substring(line.indexOf(":") + 1).trim();
                    redirectTo(redirectURI);
                    return;
                }

                if (isVerbose)
                    printLine(line);
                line = in.readLine();
            }

            // There is an error if the redirect link is not in the response headers
            if (shouldRedirect) {
                System.out.println("Response code 302 but no redirection URI found!");
                System.exit(0);
            }

            // Print out response body
            while (line != null) {
                printLine(line);
                line = in.readLine();
            }

            if (writer != null)
                writer.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void closeTCPConnection() {
        // Close streams
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private boolean shouldRedirect(String line) {
        boolean shouldRedirect = false;
        if (line != null) {
            String[] statusLineComponents = line.trim().split(" ");
            if (statusLineComponents.length >= 3) {
                if (isVerbose) printLine(line);
                try {
                    int statusCode = Integer.parseInt(statusLineComponents[1]);
                    boolean isRedirectCode = statusCode == Redirectable.StatusCode.MOVED_PERMANENTLY.code ||
                            statusCode == Redirectable.StatusCode.FOUND.code ||
                            statusCode == Redirectable.StatusCode.TEMPORARY_REDIRECT.code;
                    shouldRedirect = isRedirectCode && request instanceof Redirectable;
                } catch (NumberFormatException exception) {
                    System.out.println("Status code cannot be converted to int: " + exception);
                    System.exit(0);
                }

            } else {
                System.out.println("Response's status line is not formatted properly: " + line);
                System.exit(0);
            }

        } else {
            System.out.println("Status line is not there!");
            System.exit(0);
        }
        return shouldRedirect;
    }

    private void redirectTo(String redirectURI) {
        // Close existing socket
        closeTCPConnection();

        if (redirectCounter < REDIRECT_MAXIMUM && request instanceof Redirectable) {
            System.out.println("------------ REDIRECTED -------------");
            this.request = ((Redirectable) request).getRedirectRequest(redirectURI);
            redirectCounter++;
            performRequest();
        }
    }

    private void writeToFile(String line) {
        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void printLine(String line) {
        if (writer != null)
            writeToFile(line);
        else
            System.out.println(line);
    }

}
