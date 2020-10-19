import Helpers.HelpMessage;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.util.concurrent.Callable;

@Command(name = "Httpfs")
public class Httpfs implements Callable<Integer> {

    @Option(names = "-v") boolean isVerbose;
    @Option(names = "-p") int port = 8080;
    @Option(names = "-d") String pathToDirectory = "";
    @Option(names = "help") boolean isHelpRequested;
    @Unmatched String[] unmatchedValues;

    public static void main(String[] args) {
        Httpfs serverCli = new Httpfs();
        int exit = new CommandLine(serverCli).execute(args);

        if (exit == 0)
            new HttpServerLibrary(serverCli.isVerbose, serverCli.port, serverCli.pathToDirectory);
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

        return 0;
    }
}
