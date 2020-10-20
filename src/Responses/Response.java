package Responses;

import Helpers.HTTPMethod;
import Helpers.Status;

import java.io.File;
import java.util.List;

public class Response {
    private Status status;
    private List<String> clientHeaders;
    private String data;
    private File file;
    private HTTPMethod httpMethod;
    
    private final String HTTP_VERSION = "HTTP/1.0";

    public Response(Status status) {
        this.status = status;
    }

    public Response(HTTPMethod httpMethod, Status status, List<String> headers, String data, File file) {
        this.httpMethod = httpMethod;
        this.status = status;
        this.clientHeaders = headers;
        this.data = data;
        this.file = file;
    }
    
    public String getStatusLine() {
        return HTTP_VERSION + " " + status + "\r\n"; // TODO: add toString()?
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<String> getClientHeaders() {
        return clientHeaders;
    }

    public void setClientHeaders(List<String> clientHeaders) {
        this.clientHeaders = clientHeaders;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HTTPMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

}
