package Requests;

import Helpers.HTTPMethod;

import java.util.List;

/**
 * This class creates a POST request.
 */
public class PostRequest extends Request {
    private String data;

    public PostRequest(String host, String path, String query, List<String> headers, String data) {
        super(host, path, query, HTTPMethod.POST, headers);
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
