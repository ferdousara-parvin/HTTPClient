package Client.Requests;

import Helpers.HTTPMethod;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * This parent class creates a Request object.
 */
public abstract class Request {
    private String host;
    private String path;
    private String query;
    private HTTPMethod method;
    private List<String> headers;
    private int port;
    private InetAddress ipAddress;

    Request(String host, String path, String query, HTTPMethod method, List<String> headers, int port) {
        this.host = host;
        this.path = path;
        this.query = query == null ? "" : "?" + query;
        this.method = method;
        this.headers = headers;
        this.port = port == -1 ? 8080 : port;
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

    public InetAddress getAddress() {
        ipAddress = ipAddress == null ? new InetSocketAddress(host, port).getAddress() : new InetSocketAddress("localhost", port).getAddress();
        return  ipAddress;
    }
}

