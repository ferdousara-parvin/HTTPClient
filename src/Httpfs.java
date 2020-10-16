import Helpers.HelpMessage;

public class Httpfs {

    private static boolean isVerbose = false;

    public static void main(String[] args) {
        parseArgs(args);
        new HttpServerLibrary();
    }

    // Parse the arguments given and initialize the server
    private static void parseArgs(String[] args) {
        if (args.length < 1) showErrorAndExit(HelpMessage.INCORRECT_PARAM_HTTPFS.getMessage());

        if (args[0].equalsIgnoreCase("help")) {
            System.out.print(HelpMessage.SERVER.getMessage());
            System.exit(0);
        }

        // can have verbose (debugging messages)
        // can have the port
        // can have directory

    }

    // Helper method to show the error message before exiting
    private static void showErrorAndExit(String message) {
        System.err.print(message + "\n");
        System.exit(0);
    }
}
