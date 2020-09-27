public enum HTTPMethod {
    get("GET "), post("POST ");
    String value;

    HTTPMethod(String method) {
        value = method;
    }
}
