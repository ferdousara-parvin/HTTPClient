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
        System.out.println("Hello World!");
        Request req =  new Request("www.google.com",
                "/search?sxsrf=ALeKk02K4A2OBcJYap3QKIQrqpU5MClZaw%3A1601219232074&source=hp&ei=oKpwX83_AYW3gger4rXIDA&q=cats&oq=cats&gs_lcp=CgZwc3ktYWIQAzIKCC4QsQMQQxCTAjICCAAyBQgAELEDMgUILhCxAzICCC4yBQguELEDMgUILhCxAzIICC4QsQMQgwEyCAgAELEDEIMBMggILhCxAxCDAToECCMQJzoFCAAQkQI6CwguELEDEMcBEKMCOg4ILhCxAxCDARDHARCjAjoHCC4QsQMQQzoECAAQQzoICC4QxwEQowJQjwNYxwVgigdoAHAAeAGAAcIBiAGFBJIBAzAuNJgBAKABAaoBB2d3cy13aXo&sclient=psy-ab&ved=0ahUKEwjNz6G8zonsAhWFm-AKHStxDckQ4dUDCAg&uact=5",
                HTTPMethod.get, new HashMap<String, String>());
        new HTTPClient(req);
    }

    //httpheader -> dict(string:string)
    public HTTPClient(Request req){

        try {
            //Connect to the server
            Socket socket = new Socket(req.serverUrl, 80);

            // Create input output streams to read and write to the server
             out = new PrintStream(socket.getOutputStream());
             in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(req.createRequestLine());
            out.println(req.createRequestHeaders());

            // Read data from the server until we finish reading the document
            String line = in.readLine();
            while( line != null )
            {
                System.out.println( line );
                line = in.readLine();
            }

            // Close our streams
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openConnection(){
//        Server
    }

}
