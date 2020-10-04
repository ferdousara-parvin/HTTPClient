package Requests;

import Helpers.HTTPMethod;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * This class creates a GET request.
 */
public class GetRequest extends Request implements Redirectable {

    public GetRequest(String host, String path, String query, List<String> headers) {
        super(host, path, query, HTTPMethod.GET, headers);
    }

    public void sendRequest(PrintStream out) {
        //Send request line
        super.sendRequest(out);

        //Send request headers
        for (String header : headers) {
            out.print(header + eol);
        }
        out.print(eol);
    }

    @Override
    public Request getRedirectRequest(String redirectURI) {
        URL url = null;
        try {
            url = new URL(redirectURI);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        return new GetRequest(url.getHost(),
                url.getPath(),
                url.getQuery(),
                this.headers);
    }
}
