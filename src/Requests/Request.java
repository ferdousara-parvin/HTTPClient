package Requests;

import Helpers.HTTPMethod;

import java.io.PrintStream;
import java.util.List;

/**
 * This parent class creates a Request object.
 */
public class Request {
    protected String host;
    protected String path;
    protected HTTPMethod method;
    protected List<String> headers;
    protected int port;

    protected final String eol = "\r\n";

    public Request(String host, String path, HTTPMethod method, List<String> headers) {
        this.host = host;
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.port = 80;
    }

    public void performRequest(PrintStream out) {
        out.print(this.method.name() + " " + this.path + " " + "HTTP/1.0" + eol);
        out.print("Host: " + this.host + eol);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}

