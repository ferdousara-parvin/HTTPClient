package Server;

import Helpers.HelpMessage;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * This class acts as the command line interface. It parses the server initialization and generates a HttpServerLibrary object.
 */
@Command(name = "Server.Httpfs")
public class Httpfs implements Callable<Integer> {

    @Option(names = "-v") private boolean isVerbose;
    @Option(names = "-p") private int port = 8080;
    @Option(names = "-d") private String pathToDirectory = "";
    @Option(names = "help") private boolean isHelpRequested;
    @Unmatched private String[] unmatchedValues;

    public static void main(String[] args) {
        Httpfs serverCli = new Httpfs();
        int exit = new CommandLine(serverCli).execute(args);

        if (exit == 0)
            new HttpServerLibrary(serverCli.isVerbose, serverCli.port, Paths.get(serverCli.pathToDirectory).toAbsolutePath());
        else
            System.exit(exit);
    }


    @Override
    public Integer call() {
        if(unmatchedValues != null) {
            System.err.println(HelpMessage.INCORRECT_PARAM_HTTPFS.getMessage());
            return 1;
        }

        if(isHelpRequested) {
            System.out.print(HelpMessage.SERVER.getMessage());
            return 2;
        }

        if(!pathToDirectory.isEmpty()) {
            Path baseDirectory = Paths.get(pathToDirectory).toAbsolutePath();

            if(!baseDirectory.toFile().exists()) {
                System.err.println(HelpMessage.INVALID_BASE_DIRECTORY.getMessage());
                return 3;
            }
        }

        if(port < 1024 || port > 65535) {
            System.err.println(HelpMessage.INVALID_PORT_NUMBER.getMessage());
            return 4;
        }

        return 0;
    }
}
