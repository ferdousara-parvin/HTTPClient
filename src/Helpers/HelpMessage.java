package Helpers;

/**
 * This enum class contians the different types of help messages that can be sent to the user.
 */
public enum HelpMessage {
    INCORRECT_PARAM, GENERAL, GET, POST;

    //TODO: Use stringf to create the help message
    public String getMessage() {
        String message = "";
        switch (this) {
            case INCORRECT_PARAM:
                message = "Incorrect parameters! Try httpc help for more information.";
                break;
            case GENERAL:
                message = "httpc is curl-like application but supports HTTP protocol only.\n" +
                        "Usage:\nhttpc command [arguments]\n" +
                        "The commands are:\n" +
                        "get \texecutes a HTTP GET request and prints the response.\n" +
                        "post\texecutes a HTTP POST request and prints the response.\n" +
                        "help\tprints this screen.\n" +
                        "Use \"httpcs help [command]\" for more information about a command.\n";
                break;
            case GET:
                message = "Usage:\nhttpc get [-v] [-h key:value] URL\n" +
                        "Get executes a HTTP GET request for a given URL.\n" +
                        "-v          \t" +
                        "Prints the detail of the response such as protocol, status and headers.\n" +
                        "-h key:value\t" +
                        "Associates headers to HTTP Request with the format 'key:value'.\n";
                break;
            case POST:
                message = "Usage:\nhttpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n" +
                        "Post executes a HTTP POST request for a given URL with inline data from file.\n" +
                        "-v          \t" +
                        "Prints the detail of the response such as protocol, status and headers.\n" +
                        "-h key:value\t" +
                        "Associates headers to HTTP Request with the format 'key:value'.\n" +
                        "-d string    \t" +
                        "Associates the inline data to the body HTTP POST request.\n" +
                        "-f file      \t" +
                        "Associates the content of a file to the body HTTP POST request.\n\n" +
                        "Either [-d] or [-f] can be used but not both.\n";
        }
        return message;
    }
}
