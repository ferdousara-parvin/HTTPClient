import Helpers.HTTPMethod;
import Helpers.HelpMessage;
import Requests.GetRequest;
import Requests.PostRequest;
import Requests.Request;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class acts as the command line interface. It parses the client input and generates the appropriate Request object from it.
 */
public class HttpCli {

    private static boolean isVerbose = false;
    private static boolean writeResponseToFile = false;
    private static String responseFilePath = "";
    private static HTTPMethod httpMethod;
    private static List<String> headers = new ArrayList<>();
    private static String data = "";
    static int currentIndex = 0;

    public static void main(String[] args) {
        // TEST: post -h Content-Type:application/json -d '{"Assignment": 1}' http://httpbin.org/post OR post -h Content-Type:application/json -f C:\Users\tlgmz\Desktop\test.txt http://httpbin.org/post
        Request request = constructRequestFromArgs(args);
        if (request == null) showErrorAndExit("Request is null.");
        new HttpClientLibrary(request, isVerbose, writeResponseToFile, responseFilePath);
    }

    private static Request constructRequestFromArgs(String[] args) {
        if (args.length < 1) showErrorAndExit("Incorrect number of parameters.");

        setHTTPMethod(args);
        currentIndex++;
        parseOptions(args);
        if (currentIndex != args.length - 1) showErrorAndExit("URL is missing.");  // Assumption: last input should be URL

        // Create URL object
        String urlString = args[currentIndex];
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        // Create Request object
        Request request = null;
        switch (httpMethod) {
            case GET:
                request = new GetRequest(url.getHost(), url.getPath(), headers);
                break;
            case POST:
                request = new PostRequest(url.getHost(), url.getPath(), headers, data);
                break;
            default:
                showErrorAndExit("Request was not properly created.");
        }

        return request;
    }

    private static void setHTTPMethod(String[] args) {
        switch (args[currentIndex]) {
            case "help":
                parseHelp(args);
                System.exit(0);
            case "get":
                httpMethod = HTTPMethod.GET;
                break;
            case "post":
                httpMethod = HTTPMethod.POST;
                break;
            default:
                showErrorAndExit(HelpMessage.INCORRECT_PARAM.getMessage());
        }

    }

    private static void parseHelp(String[] args) {
        if (args.length == 1) {
            System.out.print(HelpMessage.GENERAL.getMessage());
        } else if (args.length == 2) { // args.length > 1
            switch (args[1]) {
                case "get":
                    System.out.print(HelpMessage.GET.getMessage());
                    break;
                case "post":
                    System.out.print(HelpMessage.POST.getMessage());
                    break;
                default:
                    showErrorAndExit("Incorrect parameters. The following are supported: help get, help post.");
            }
        }
        else
            showErrorAndExit(HelpMessage.INCORRECT_PARAM.getMessage());
    }

    private static void parseOptions(String[] args) {
        boolean hasDataSource = false;
        while (currentIndex < args.length && args[currentIndex].startsWith("-")) {
            switch (args[currentIndex]) {
                case "-v":
                    isVerbose = true;
                    break;
                case "-h":
                    if (currentIndex++ < args.length && !args[currentIndex].startsWith("-")) {
                        String header = getOptionValue(args);
                        headers.add(header);
                        break;
                    }
                case "-d":
                    if(httpMethod == HTTPMethod.GET)
                        showErrorAndExit("Cannot use -d option in a GET request.");

                    // Check for exclusivity (either -d or -f)
                    if(hasDataSource)
                        showErrorAndExit("Cannot have both -d and -f options for a POST request.");
                    else
                        hasDataSource = true;

                    // Get data from command line
                    if (currentIndex++ < args.length && !args[currentIndex].startsWith("-")) {
                        data = getOptionValue(args);
                        break;
                    }
                case "-f":
                    if(httpMethod == HTTPMethod.GET)
                        showErrorAndExit("Cannot use -f option in a GET request.");

                    // Check for exclusivity (either -d or -f)
                    if(hasDataSource)
                        showErrorAndExit("Cannot have both -d and -f options for a POST request.");
                    else
                        hasDataSource = true;

                    // Get data from file
                    if (currentIndex++ < args.length) {
                        data = extractDataFromFile(args[currentIndex]);
                        break;
                    }
                case "-o":
                    writeResponseToFile = true;
                    currentIndex++;
                    responseFilePath = getOptionValue(args);
                    break;
                default:
                    showErrorAndExit("Option is not supported. Here's the list of supported options: -v, -d, -f, -o, -h.");
            }
            currentIndex++;
        }
    }

    private static String getOptionValue(String[] args) {
        if ((args[currentIndex].startsWith("\'")) || (args[currentIndex].startsWith("\""))) {
            StringBuilder value = new StringBuilder();
            value.append(args[currentIndex]);
            while ((value.toString().startsWith("\'") && !value.toString().endsWith("\'")) || (value.toString().startsWith("\"") && !value.toString().endsWith("\""))) {
                currentIndex++;
                value.append(args[currentIndex]);
            }
            return value.toString().trim().substring(1, value.length() - 1);
        }
        return args[currentIndex];
    }

    private static String extractDataFromFile(String filePath) {
        StringBuilder data = new StringBuilder();
        File file = new File(filePath);

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String string = "";
            while ((string = br.readLine()) != null)
                data.append(string).append(" ");
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data.toString().trim();
    }

    private static void showErrorAndExit(String message) {
        System.out.print(message + "\n");
        System.exit(0);
    }

}
