import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Request {
    String serverUrl;
    String path;
    HTTPMethod method;
    String[] headers;
    int port;
    private static final int webContentPort = 80;
    private static final String httpVerion = "HTTP/1.0";
    private static final String eol = "\r\n";


    public Request(String serverUrl, String path, HTTPMethod method, String[] headers) {
        this.serverUrl = serverUrl;
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.port = webContentPort;
    }

    public String createRequestLine() {
        return this.method.value + " " + this.path + " " + httpVerion + eol;
    }

    public String createRequestHeaders() {
//        String result = "";
//        for (String header : headers)
//            result += header + eol;
//        return result;
        return "";
    }
}

