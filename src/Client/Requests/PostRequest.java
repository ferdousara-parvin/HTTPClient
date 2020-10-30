package Client.Requests;

import Helpers.HTTPMethod;

import java.util.List;

/**
 * This class creates a POST request.
 */
public class PostRequest extends Request {
    private String data;

    public PostRequest(String host, String path, String query, List<String> headers, String data, int port) {
        super(host, path, query, HTTPMethod.POST, headers, port);
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
