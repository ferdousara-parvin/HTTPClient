package Helpers;

/**
 * This enum class contains all the types of HTTP methods.
 */
public enum HTTPMethod {
    GET("GET"),
    POST("POST");

    final String value;

    HTTPMethod(String method) {
        this.value = method;
    }
}
