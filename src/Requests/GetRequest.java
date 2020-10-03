package Requests;

import Helpers.HTTPMethod;

import java.io.PrintStream;
import java.util.List;

/**
 * This class creates a GET request.
 */
public class GetRequest extends Request {

    public GetRequest(String host, String path, List<String> headers) {
        super(host, path, HTTPMethod.GET, headers);
    }

    public void performRequest(PrintStream out) {
        //Send request line
        super.performRequest(out);

        //Send request headers
        for (String header: headers) {
            out.print(header + eol);
        }
        out.print(eol);
    }
}
