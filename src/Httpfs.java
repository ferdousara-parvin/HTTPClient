import Helpers.HelpMessage;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "Httpfs")
public class Httpfs implements Callable<Integer> {

    @CommandLine.Option(names = "-v") boolean verbose;
    @CommandLine.Option(names = "-p") int port = 8080;
    @CommandLine.Option(names = "-d") String pathToDirectory = "/";
    @CommandLine.Option(names = "help") boolean isHelpRequested;
    @CommandLine.Unmatched String[] unmatchedValues;

    public static void main(String[] args) {
        Httpfs serverCli = new Httpfs();
        int exit = new CommandLine(serverCli).execute(args);
        System.exit(exit);
    }


    @Override
    public Integer call() throws Exception {
        System.out.print("Parsing command line args ... ");
//        System.out.print("\nverbose : " + verbose);
//        System.out.print("\nport : " + port);
//        System.out.print("\npath to directory : " + directoryPath);

        if(unmatchedValues == null || unmatchedValues.length != 0) {
            System.err.print(HelpMessage.INCORRECT_PARAM_HTTPFS.getMessage());
            return 1;
        }

        if(isHelpRequested) {
            System.out.print(HelpMessage.SERVER.getMessage());
            return 0;
        }
        return 0;
    }
}
