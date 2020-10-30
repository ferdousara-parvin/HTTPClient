package Client.Requests;

import Helpers.HTTPMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * This class creates a GET request.
 */
public class GetRequest extends Request implements Redirectable {

    public GetRequest(String host, String path, String query, List<String> headers, int port) {
        super(host, path, query, HTTPMethod.GET, headers, port);
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
                this.getHeaders(), this.getPort());
    }
}
