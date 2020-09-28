import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class HTTPClient {

    private PrintStream out;
    private BufferedReader in;
    private Socket clientSocket;
    private static boolean isVerbose = false;

    public static void main(String[] args) {
        String method = "get";
        String urlString = "http://httpbin.org/get?author=The+Geeky+Asian&course=GET+Request+Using+Sockets+in+Java";
//        args = new String[]{"httpc", method, urlString};
//        args = new String[]{"httpc", "help", "you"};


//        parseArgs(args);

        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            String path = url.getPath();
            String param = url.getQuery();
            System.out.println(host + "->" + path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Testing purposes
        List headers = new ArrayList<>();
//        headers.add("Content-Type: application/json");

//        Request request = new Request("www.google.com",
//                "/search?sxsrf=ALeKk02K4A2OBcJYap3QKIQrqpU5MClZaw%3A1601219232074&source=hp&ei=oKpwX83_AYW3gger4rXIDA&q=cats&oq=cats&gs_lcp=CgZwc3ktYWIQAzIKCC4QsQMQQxCTAjICCAAyBQgAELEDMgUILhCxAzICCC4yBQguELEDMgUILhCxAzIICC4QsQMQgwEyCAgAELEDEIMBMggILhCxAxCDAToECCMQJzoFCAAQkQI6CwguELEDEMcBEKMCOg4ILhCxAxCDARDHARCjAjoHCC4QsQMQQzoECAAQQzoICC4QxwEQowJQjwNYxwVgigdoAHAAeAGAAcIBiAGFBJIBAzAuNJgBAKABAaoBB2d3cy13aXo&sclient=psy-ab&ved=0ahUKEwjNz6G8zonsAhWFm-AKHStxDckQ4dUDCAg&uact=5",
//                HTTPMethod.GET,
//                headers);

        String data = "{" +
                "\"author\" : \"The Geeky Asian\"," +
                "\"course\" : \"POST Request Using Sockets in Java\"" +
                "}";

        Request request = new Request("httpbin.org",
                "/post",
                HTTPMethod.POST,
                headers, data);

        new HTTPClient(request);

    }

    public static void parseArgs(String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("help")) { // Help messages
            if (args.length == 2) {
                // httpc help
                System.out.print(HelpMessage.GENERAL.getMessage());
            } else { // args.length > 2
                // httpc help (get|post)
                switch (args[2]) {
                    case "get":
                        System.out.print(HelpMessage.GET.getMessage());
                        break;
                    case "post":
                        System.out.print(HelpMessage.POST.getMessage());
                        break;
                    default:
                        System.out.print(HelpMessage.INCORRECT_PARAM.getMessage());
                }
            }
            System.exit(0);
        }

        // Verify validity of command
        if (args.length <= 2) {
            System.out.print(HelpMessage.INCORRECT_PARAM.getMessage());
            System.exit(0);
        }
        if (args[1].equals("get")) {
            try {
                ArrayList<String> headers = new ArrayList<>();
                String currentParameter = args[2];
                int headerTagIndex = currentParameter.equals("-v") ? 3 : 2;
                isVerbose = currentParameter.equals("-v");

                while (args[headerTagIndex].equals("-h")) {
                    headers.add(args[++headerTagIndex]);
                    headerTagIndex++;
                }

                int urlIndex = headerTagIndex;
                String urlString = args[urlIndex];

//                URL url = new URL(urlString);
//                Request request = new Request(url.getHost(), url.getPath(), HTTPMethod.GET, headers.toArray(new String[]));
//                Request getRequest = new Request()
            } catch (NullPointerException exception) {

            }
        } else if (args[2].equals("post")) {
            // Parse commands for post
        }
    }

    public HTTPClient(Request request) {
        sendRequest(request);
        readResponse();
    }

    private void sendRequest(Request request) {
        try {
            // Connect to the server
            clientSocket = new Socket(request.getHost(), request.getPort());

            // Create input output streams to read and write to the server
            out = new PrintStream(clientSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Perform request
            if (request.getMethod().equals(HTTPMethod.GET)) {
                request.getRequest(out);
            }
            else if (request.getMethod().equals(HTTPMethod.POST)) {
                request.postRequest(out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
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
