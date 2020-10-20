package Client.Requests;

public interface Redirectable {

    // TODO: merge w/ Status enum
    enum StatusCode {
        MOVED_PERMANENTLY(301), FOUND(302), TEMPORARY_REDIRECT(307);
        public int code;

        StatusCode(int code) {
            this.code = code;
        }
    }

    // Method that will return the same request to the redirected URI
    Request getRedirectRequest(String redirectURI);
}
