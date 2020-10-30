package Client;

import Helpers.HTTPMethod;
import Helpers.HelpMessage;
import Client.Requests.GetRequest;
import Client.Requests.PostRequest;
import Client.Requests.Request;

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
    private static String responseFilePath = "";
    private static HTTPMethod httpMethod;
    private static List<String> headers = new ArrayList<>();
    private static String data = "";
    private static int currentIndex = 0;
    private static int port = -1;

    public static void main(String[] args) {
        Request request = constructRequestFromArgs(args);
        if (request == null) showErrorAndExit("Request is null.");
        if (responseFilePath.isEmpty())
            new HttpClientLibrary(request, isVerbose);
        else
            new HttpClientLibrary(request, isVerbose, responseFilePath);
    }

    // Parse the arguments given and create a request from them
    private static Request constructRequestFromArgs(String[] args) {
        if (args.length < 1) showErrorAndExit(HelpMessage.INCORRECT_PARAM_HTTPC.getMessage());

        setHTTPMethod(args);
        currentIndex++;
        parseOptions(args);
        if (currentIndex != args.length - 1) showErrorAndExit("URL is missing.");

        // Create URL object
        String urlString = cleanUpUrl(args[currentIndex]);
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
                request = new GetRequest(url.getHost(), url.getPath(), url.getQuery(), headers, port);
                break;
            case POST:
                request = new PostRequest(url.getHost(), url.getPath(), url.getQuery(), headers, data, port);
                break;
            default:
                showErrorAndExit("Request was not properly created.");
        }

        return request;
    }

    // Helper method to determine the http method type from the given arguments
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
                showErrorAndExit(HelpMessage.INCORRECT_PARAM_HTTPC.getMessage());
        }

    }

    // Helper method to show the correct help message depending on the given arguments
    private static void parseHelp(String[] args) {
        if (args.length == 1) {
            System.out.print(HelpMessage.CLIENT.getMessage());
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
        } else
            showErrorAndExit(HelpMessage.INCORRECT_PARAM_HTTPC.getMessage());
    }

    // Helper method to determine which options are asked for from the given arguments
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
                    if (httpMethod == HTTPMethod.GET)
                        showErrorAndExit("Cannot use -d option in a GET request.");

                    // Check for exclusivity (either -d or -f)
                    if (hasDataSource)
                        showErrorAndExit("Cannot have both -d and -f options for a POST request.");
                    else
                        hasDataSource = true;

                    // Get data from command line
                    if (currentIndex++ < args.length && !args[currentIndex].startsWith("-")) {
                        data = getOptionValue(args);
                        break;
                    }
                case "-f":
                    if (httpMethod == HTTPMethod.GET)
                        showErrorAndExit("Cannot use -f option in a GET request.");

                    // Check for exclusivity (either -d or -f)
                    if (hasDataSource)
                        showErrorAndExit("Cannot have both -d and -f options for a POST request.");
                    else
                        hasDataSource = true;

                    // Get data from file
                    if (currentIndex++ < args.length) {
                        data = extractDataFromFile(args[currentIndex]);
                        break;
                    }
                case "-o":
                    currentIndex++;
                    responseFilePath = getOptionValue(args);
                    break;
                case "-p":
                    if (currentIndex++ < args.length && !args[currentIndex].startsWith("-"))
                        port = Integer.valueOf(args[currentIndex]);
                    break;
                 default:
                    showErrorAndExit("Option is not supported. Here's the list of supported options: -v, -d, -f, -o, -h, -p.");
            }
            currentIndex++;
        }
    }

    // Helper method to determine the value for the options given the arguments.
    // The returned value will not have enclosing quotations.
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

    // Helper method to extract data from a file given it's path
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

    // Helper method to remove the quotations enclosing the URL if there is any
    private static String cleanUpUrl(String url) {
        if (url.startsWith("\'") || url.startsWith("\""))
            return url.substring(1, url.length() - 1);

        return url;
    }

    // Helper method to show the error message before exiting
    private static void showErrorAndExit(String message) {
        System.err.print(message + "\n");
        System.exit(0);
    }

}
