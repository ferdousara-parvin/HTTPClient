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

    final String eol = "\r\n";

    Request(String host, String path, String query, HTTPMethod method, List<String> headers) {
        this.host = host;
        this.path = path;
        this.query = query == null ? "" : "?" + query;
        this.method = method;
        this.headers = headers;
        this.port = DEFAULT_PORT;
    }

    public void sendRequest(PrintStream out) {
        out.print(this.method.name() + " " + this.path + this.query + " " + "HTTP/1.0" + eol);
        out.print("Host: " + this.host + eol);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}

