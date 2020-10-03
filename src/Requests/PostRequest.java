package Requests;

import Helpers.HTTPMethod;

import java.io.PrintStream;
import java.util.List;

public class PostRequest extends Request {
    private String data;

    public PostRequest(String host, String path, List<String> headers, String data) {
        super(host, path, HTTPMethod.POST, headers);
        this.data = data;
    }

    public void performRequest(PrintStream out) {
        //Send request line
        super.performRequest(out);

        // Send request headers
        out.print("Content-Length: " + this.data.length() + eol);
        for (String header : this.headers) {
            out.print(header + eol);
        }

        // Writing an empty line just to notify the server the header ends here
        out.print(eol);

        //Send data
        out.print(this.data);
        out.print(eol);
    }
}
