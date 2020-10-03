import Helpers.HTTPMethod;
import Helpers.HelpMessage;
import Requests.GetRequest;
import Requests.PostRequest;
import Requests.Request;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//TODO: Add documentation
public class HttpCli {

    private static boolean isVerbose = false;
    private static HTTPMethod chosenHTTPMethod;
    private static List<String> headers = new ArrayList<>();
    private static String data = "";
    static int currentIndex = 0;

    public static void main(String[] args) {
        // test it out by putting this params in your command line:
        // post -h Content-Type:application/json -d '{"Assignment": 1}' http://httpbin.org/post
        Request request = constructRequestFromArgs(args);
        if (request == null) showErrorAndExit();
        HTTPClientLibrary clientLibrary = new HTTPClientLibrary(request);
    }

    public static Request constructRequestFromArgs(String[] args) {
        if (args.length < 1) showErrorAndExit();

        setHTTPMethodFromArgs(args);
        currentIndex++;

        parseOptions(args);

        // After parsing all the options, the only argument left should be the URL
        if (currentIndex != args.length - 1) showErrorAndExit();

        String urlString = args[currentIndex];

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        Request request = null;
        switch (chosenHTTPMethod) {
            case GET:
                request = new GetRequest(url.getHost(), url.getPath(), headers);
                break;
            case POST:
                request = new PostRequest(url.getHost(), url.getPath(), headers, data);
                break;
            default:
                showErrorAndExit();
        }

        return request;
    }

    public static void setHTTPMethodFromArgs(String[] args) {
        switch (args[currentIndex]) {
            case "help":
                parseHelp(args);
                System.exit(0);
            case "get":
                chosenHTTPMethod = HTTPMethod.GET;
                break;
            case "post":
                chosenHTTPMethod = HTTPMethod.POST;
                break;
            default:
                showErrorAndExit();
        }

    }

    public static void parseHelp(String[] args) {
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
                    showErrorAndExit();
            }
        }
    }

    public static void parseOptions(String[] args) {
        //TODO: Create a variable to check for the exclusive or (d| f)
        // also check that GET does not have a -d or -f
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
                case "-d"://if get method, then error
                    if (currentIndex++ < args.length && !args[currentIndex].startsWith("-")) {
                        data = getOptionValue(args);
                        break;
                    }
                case "-f":// if get method then error
                    //TODO: Implement option -f
                    if (currentIndex++ < args.length) {
                        break;
                    }
                case "-o":
                    // TODO: Implement option -o
                default:
                    showErrorAndExit();
            }
            currentIndex++;
        }
    }

    public static String getOptionValue(String[] args) {
        if ((args[currentIndex].startsWith("\"")) || (args[currentIndex].startsWith("\'"))) {
            StringBuilder value = new StringBuilder();
            value.append(args[currentIndex].substring(1));
            while ((args[currentIndex].startsWith("\"") && !args[currentIndex].endsWith("\"")) || (args[currentIndex].startsWith("\'") && !args[currentIndex].endsWith("\'"))) {
                currentIndex++;
                value.append(args[currentIndex].substring(0, args[currentIndex].length() - 1));
            }
            return value.toString();
        }
        return args[currentIndex];
    }

    public static void showErrorAndExit() {
        System.out.print(HelpMessage.INCORRECT_PARAM.getMessage());
        System.exit(0);
    }
}
