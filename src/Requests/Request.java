package Requests;

import Helpers.HTTPMethod;

import java.io.PrintStream;
import java.util.List;

/**
 * This parent class creates a Request object.
 */
public abstract class Request {
    private String host;
    private String path;
    private String query;
    private HTTPMethod method;
    List<String> headers;
    private int port;
    private final int DEFAULT_PORT = 8080;

    Request(String host, String path, String query, HTTPMethod method, List<String> headers) {
        this.host = host;
        this.path = path;
        this.query = query == null ? "" : "?" + query;
        this.method = method;
        this.headers = headers;
        this.port = DEFAULT_PORT;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    public HTTPMethod getMethod() {
        return method;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}

