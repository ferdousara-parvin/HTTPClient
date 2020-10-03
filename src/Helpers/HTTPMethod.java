package Helpers;

public enum HTTPMethod {
    GET("GET"),
    POST("POST");

    final String value;

    HTTPMethod(String method) {
        this.value = method;
    }
}
