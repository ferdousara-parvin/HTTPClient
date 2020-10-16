package Helpers;

/**
 * This enum class contains the different types of help messages that can be sent to the user.
 */
public enum HelpMessage {
    INCORRECT_PARAM_HTTPC, INCORRECT_PARAM_HTTPFS, CLIENT, GET, POST, SERVER;

    public String getMessage() {
        String message = "";
        switch (this) {
            case INCORRECT_PARAM_HTTPC:
                message = "Incorrect parameters! Try httpc help for more information.";
                break;
            case INCORRECT_PARAM_HTTPFS:
                message = "Incorrect parameters! Try httpfs help for more information.";
                break;
            case CLIENT:
                message = "httpc is curl-like application but supports HTTP protocol only.\n" +
                        "Usage:\n" +
                        "\thttpc command [arguments]\n" +
                        "The commands are:\n" +
                        "\tget \texecutes a HTTP GET request and prints the response.\n" +
                        "\tpost\texecutes a HTTP POST request and prints the response.\n" +
                        "\thelp\tprints this screen.\n" +
                        "Use \"httpcs help [command]\" for more information about a command.\n";
                break;
            case GET:
                message = "usage: httpc get [-v] [-h key:value] URL\n" +
                        "Get executes a HTTP GET request for a given URL.\n" +
                        "\t-v          \t" +
                        "Prints the detail of the response such as protocol, status and headers.\n" +
                        "\t-h key:value\t" +
                        "Associates headers to HTTP Request with the format 'key:value'.\n";
                break;
            case POST:
                message = "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n" +
                        "Post executes a HTTP POST request for a given URL with inline data from file.\n" +
                        "\t-v          \t" +
                        "Prints the detail of the response such as protocol, status and headers.\n" +
                        "\t-h key:value\t" +
                        "Associates headers to HTTP Request with the format 'key:value'.\n" +
                        "\t-d string    \t" +
                        "Associates the inline data to the body HTTP POST request.\n" +
                        "\t-f file      \t" +
                        "Associates the content of a file to the body HTTP POST request.\n\n" +
                        "Either [-d] or [-f] can be used but not both.\n";
                break;
            case SERVER:
                message = "httpfs is a simple file server.\n" +
                        "usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]\n" +
                        "\t-v \tPrints debugging messages.\n" +
                        "\t-p \tSpecifies the port number that the server will listen and serve at.\n" +
                        "\t\tDefault is 8080.\n" +
                        "\t-d \tSpecifies the directory that the server will use to read/write requested files. Default is the current directory when launching the application.";
                break;
        }
        return message;
    }
}
