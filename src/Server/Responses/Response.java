package Server.Responses;

import Helpers.HTTPMethod;
import Helpers.Status;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

/**
 * This class creates a Response object.
 */
public class Response {
    private Status status;
    private List<String> clientHeaders;
    private String data;
    private File file;
    private HTTPMethod httpMethod;

    private final String HTTP_VERSION = "HTTP/1.0";
    private final String EOL = "\r\n";

    // TODO: Optional content-type, content disposition

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

    public String getResponse() {
        StringBuilder response = new StringBuilder();
        response.append(getStatusLine());
        response.append(getServerHeaders());
        if (status.equals(Status.OK) && httpMethod.equals(HTTPMethod.GET)) {
            response.append(EOL);
            response.append(getData());
        }
        response.append(EOL);

        return response.toString();
    }

    private String getStatusLine() {
        return HTTP_VERSION + " " + status + "\r\n";
    }

    private int getContentLength() {
        int contentLength = data.length();
        for (String header : clientHeaders) {
            if (header.contains("Content-Length:")) {
                contentLength = Integer.parseInt(header.substring(header.indexOf(":") + 1).trim());
            }
        }

        return contentLength;
    }

    private String getServerHeaders() {
        StringBuilder headers = new StringBuilder();
        headers.append("Server: localhost" + EOL);
        headers.append("Date: " + new Date() + EOL);
        headers.append(status == Status.OK ? "Content-Length: " + this.getContentLength() + EOL : "");
        boolean shouldContentTypeHeaderBePresent = httpMethod == HTTPMethod.GET && data != null && !data.isEmpty() && !file.isDirectory();
        headers.append(shouldContentTypeHeaderBePresent ? getContentTypeHeader() + EOL : "");
        return headers.toString();
    }

    private String getContentTypeHeader() {
        String contentType = null;
        try {
            contentType = Files.probeContentType(file.toPath());
        } catch (IOException exception) {
            status = Status.NOT_FOUND;
        }
        return contentType == null ? "" : "Content-Type: " + contentType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getData() {
        return data.substring(0, getContentLength());
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

}
