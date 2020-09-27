import java.util.HashMap;

public class Request {
    String serverUrl;
    String path;
    HTTPMethod method;
    HashMap<String, String> headers;
    private static final String httpVerion = " HTTP/1.0";
    private static final String eol = "\r\n";


    public Request(String serverUrl, String path, HTTPMethod method, HashMap<String, String> headers) {
        this.serverUrl = serverUrl;
        this.path = path;
        this.method = method;
        this.headers = headers;
    }

    public String createRequestLine() {
        return this.method.value + this.path + httpVerion + eol;
    }
    
    public String createRequestHeaders() {
//        head
        String result = "";
//        headers.forEach((key, value) -> result += key + ": " + value + eol);
        return "";
    }
}

