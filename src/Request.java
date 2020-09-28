import java.io.PrintStream;
import java.util.List;

public class Request {
    private String host;
    private String path;
    private HTTPMethod method;
    private List<String> headers;
    private int port;
    private String data;
    private static final int webContentPort = 80;
    private static final String httpVersion = "HTTP/1.0";
    private static final String eol = "\r\n";

    public Request(String host, String path, HTTPMethod method, List<String> headers, String data) {
        this.host = host;
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.data = data;
        this.port = webContentPort;
    }

    public void postRequest(PrintStream out) {
        out.print("POST " + this.path + " " + httpVersion + eol);
        out.print("Content-Length: " + this.data.length() + eol);
        for (String header: this.headers) {
            out.print(header + eol);
        }
        out.print(eol);   // Writing an empty line just to notify the server the header ends here
        out.print(this.data);
        out.print(eol);
    }

    public void getRequest(PrintStream out) {
        out.print("GET " + this.path + " " + httpVersion + eol);
        for (String header: this.headers) {
            out.print(header + eol);
        }
        out.print(eol);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public HTTPMethod getMethod() {
        return method;
    }

}

