import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;

public class HTTPClient {

    private PrintStream out;
    private BufferedReader in;

    public static void main(String[] args) {
//        // Verify validity of command
//        if (args.length < 2) {
//            System.out.println("Missing some parameters");
//            System.exit(0);
//        }
//
//        if(args[1].equalsIgnoreCase("help")) { // Help messages
//            if(args.length == 2) {
////                HttpcHelp.printHelpMessage();
//            }
//            else {
////                HttpcHelp.printMethodHelpMessage(args[2]);
//            }
//
//            System.exit(0);
//        }


        Request request =  new Request("www.google.com",
                "/search?sxsrf=ALeKk02K4A2OBcJYap3QKIQrqpU5MClZaw%3A1601219232074&source=hp&ei=oKpwX83_AYW3gger4rXIDA&q=cats&oq=cats&gs_lcp=CgZwc3ktYWIQAzIKCC4QsQMQQxCTAjICCAAyBQgAELEDMgUILhCxAzICCC4yBQguELEDMgUILhCxAzIICC4QsQMQgwEyCAgAELEDEIMBMggILhCxAxCDAToECCMQJzoFCAAQkQI6CwguELEDEMcBEKMCOg4ILhCxAxCDARDHARCjAjoHCC4QsQMQQzoECAAQQzoICC4QxwEQowJQjwNYxwVgigdoAHAAeAGAAcIBiAGFBJIBAzAuNJgBAKABAaoBB2d3cy13aXo&sclient=psy-ab&ved=0ahUKEwjNz6G8zonsAhWFm-AKHStxDckQ4dUDCAg&uact=5",
                HTTPMethod.GET, new HashMap<String, String>());
        new HTTPClient(request);

    }

    //httpheader -> dict(string:string)
    public HTTPClient(Request request){
        try {
            // Connect to the server
            Socket clientSocket = new Socket(request.serverUrl, 80);

            // Create input output streams to read and write to the server
             out = new PrintStream(clientSocket.getOutputStream());
             in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

             // Perform request
            out.println(request.createRequestLine());
            out.println(request.createRequestHeaders());

            // Read response from server
            String line = in.readLine();
            while(line != null)
            {
                System.out.println( line );
                line = in.readLine();
            }

            // Close streams
            in.close();
            out.close();
            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openConnection(){
//        Server
    }

}
