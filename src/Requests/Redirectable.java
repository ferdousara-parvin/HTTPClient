package Requests;

public interface Redirectable {
    int redirectCode = 301;

    // Method that will return the same request to the redirected URI
    Request getRedirectRequest(String redirectURI);
}
