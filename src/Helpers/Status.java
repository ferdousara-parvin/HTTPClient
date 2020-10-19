package Helpers;

/**
 * This enum class contains all the different status codes that a server can respond with.
 */
public enum Status {
    OK(200, "OK"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    NOT_IMPLEMENTED(501, "Not Implemented");

    final int code;
    final String description;

    Status(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String toString() {
        return this.code + " " + this.description + "\r\n";
    }
}
