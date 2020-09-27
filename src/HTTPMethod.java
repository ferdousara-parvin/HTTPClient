public enum HTTPMethod {
    GET ("GET"),
    POST ("POST");

    String value;

    HTTPMethod(String method) {
        this.value = method;
    }
}
