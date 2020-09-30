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
        parseArgs(args); // test it out by putting this params in your command line: post -h Content-Type:application/json -d '{"Assignment": 1}' http://httpbin.org/post

//        // Testing purposes
//        List headers = new ArrayList<>();
////        headers.add("Content-Type: application/json");
//
////        Request request = new Request("www.google.com",
////                "/search?sxsrf=ALeKk02K4A2OBcJYap3QKIQrqpU5MClZaw%3A1601219232074&source=hp&ei=oKpwX83_AYW3gger4rXIDA&q=cats&oq=cats&gs_lcp=CgZwc3ktYWIQAzIKCC4QsQMQQxCTAjICCAAyBQgAELEDMgUILhCxAzICCC4yBQguELEDMgUILhCxAzIICC4QsQMQgwEyCAgAELEDEIMBMggILhCxAxCDAToECCMQJzoFCAAQkQI6CwguELEDEMcBEKMCOg4ILhCxAxCDARDHARCjAjoHCC4QsQMQQzoECAAQQzoICC4QxwEQowJQjwNYxwVgigdoAHAAeAGAAcIBiAGFBJIBAzAuNJgBAKABAaoBB2d3cy13aXo&sclient=psy-ab&ved=0ahUKEwjNz6G8zonsAhWFm-AKHStxDckQ4dUDCAg&uact=5",
////                HTTPMethod.GET,
////                headers);
//
//        String data = "{" +
//                "\"author\" : \"The Geeky Asian\"," +
//                "\"course\" : \"POST Request Using Sockets in Java\"" +
//                "}";
//
//        Request request = new Request("httpbin.org",
//                "/post",
//                HTTPMethod.POST,
//                headers, data);
//
//        new HTTPClient(request);

    }

    public HTTPClient(Request request) {
        sendRequest(request);
        readResponse();
    }

    public static void parseArgs(String[] args) {
        if (args.length < 1) {
            System.out.print(HelpMessage.INCORRECT_PARAM.getMessage());
            System.exit(0);
        }

        if (args[0].equalsIgnoreCase("help")) {
            parseHelp(args);
        }
        else if (args[0].equals("get")) {
            parseGet(args);
        } else if (args[0].equals("post")) {
            parsePost(args);
        }
        else {
            System.out.print(HelpMessage.INCORRECT_PARAM.getMessage());
            System.exit(0);
        }
    }

    public static void parsePost(String[] args) {
        int currentIndex = 1;
        List<String> headers = new ArrayList<>();
        String data = "";
        String urlString = "";
        String host = "";
        String path = "";

        while (args.length > currentIndex) {
            if(args[currentIndex].startsWith("-")) {
                switch (args[currentIndex]) {
                    case "-v":
                        isVerbose = true;
                        break;
                    case "-h":
                        if(currentIndex++ < args.length && !args[currentIndex].startsWith("-")) {
                            if((args[currentIndex].startsWith("\"")) || (args[currentIndex].startsWith("\'"))) {
                                StringBuilder header = new StringBuilder();
                                header.append(args[currentIndex]);
                                while((args[currentIndex].startsWith("\"") && !args[currentIndex].endsWith("\"")) || (args[currentIndex].startsWith("\'") && !args[currentIndex].endsWith("\'")))  {
                                    currentIndex++;
                                    header.append(args[currentIndex]);
                                }
                                headers.add(header);
                            }
                            else
                                headers.add(args[currentIndex]);
                            break;
                        }
                    case "-d":
                        //"", '', nothing. When there's a space, by convention, there's guillements
                        if(currentIndex++ < args.length && !args[currentIndex].startsWith("-")) {
                            if((args[currentIndex].startsWith("\"")) || (args[currentIndex].startsWith("\'"))) {
                                StringBuilder dataSB = new StringBuilder();
                                dataSB.append(args[currentIndex]);
                                while((args[currentIndex].startsWith("\"") && !args[currentIndex].endsWith("\"")) || (args[currentIndex].startsWith("\'") && !args[currentIndex].endsWith("\'")))  {
                                    currentIndex++;
                                    dataSB.append(args[currentIndex]);
                                }
                                data = dataSB.toString();
                            }
                            else
                               data = args[currentIndex];
                            break;
                        }
                    case "-f":
                        if(currentIndex++ < args.length) {

                            break;
                        }
                    case "-o":
                        //
                    default:
                        System.out.print(HelpMessage.INCORRECT_PARAM.getMessage());
                        System.exit(0);
                }

                currentIndex++;
            }
            else {
                urlString = args[currentIndex];
                try {
                    URL url = new URL(urlString);
                    host = url.getHost();
                    path = url.getPath();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                break;
            }
        }

        Request request = new Request(host, path, HTTPMethod.POST, headers, data);
        new HTTPClient(request);
    }

    public static void parseHelp(String[] args) { // TODO: verify format
        if (args.length == 1) {
            System.out.print(HelpMessage.GENERAL.getMessage());
        } else { // args.length > 1
            switch (args[1]) {
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
    }

    public static void parseGet(String[] args) { // TODO: redo
        try {
            ArrayList<String> headers = new ArrayList<>();
            String currentParameter = args[2];
            int headerTagIndex = currentParameter.equals("-v") ? 3 : 2;
//            isVerbose = currentParameter.equals("-v");

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
