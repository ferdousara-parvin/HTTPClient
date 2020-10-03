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
    private static HTTPMethod httpMethod;
    private static List<String> headers = new ArrayList<>();
    private static String data = "";
    static int currentIndex = 0;

    public static void main(String[] args) {
        Request request = constructRequestFromArgs(args);
        if (request == null) showErrorAndExit();
        new HttpClientLibrary(request, isVerbose);
    }

    private static Request constructRequestFromArgs(String[] args) {
        if (args.length < 1) showErrorAndExit();

        setHTTPMethod(args);
        currentIndex++;
        parseOptions(args);
        if (currentIndex != args.length - 1) showErrorAndExit();  // After parsing all the options, there should remain an argument which is the URL

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
                request = new GetRequest(url.getHost(), url.getPath(), url.getQuery(), headers);
                break;
            case POST:
                request = new PostRequest(url.getHost(), url.getPath(), url.getQuery(), headers, data);
                break;
            default:
                showErrorAndExit();
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
                showErrorAndExit();
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
                    showErrorAndExit();
            }
        }
        else
            showErrorAndExit();
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
                        showErrorAndExit();

                    // Check for exclusivity (either -d or -f)
                    if(hasDataSource)
                        showErrorAndExit();
                    else
                        hasDataSource = true;

                    // Get data from command line
                    if (currentIndex++ < args.length && !args[currentIndex].startsWith("-")) {
                        data = getOptionValue(args);
                        break;
                    }
                case "-f":
                    if(httpMethod == HTTPMethod.GET)
                        showErrorAndExit();

                    // Check for exclusivity (either -d or -f)
                    if(hasDataSource)
                        showErrorAndExit();
                    else
                        hasDataSource = true;

                    // Get data from file
                    if (currentIndex++ < args.length) {
                        data = extractDataFromFile(args[currentIndex]);
                        break;
                    }
                case "-o":
                    // TODO: Bonus -- Implement option -o
                default:
                    showErrorAndExit();
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data.toString().trim();
    }

    private static void showErrorAndExit() {
        System.out.print(HelpMessage.INCORRECT_PARAM.getMessage());
        System.exit(0);
    }

}
