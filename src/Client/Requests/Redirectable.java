package Client.Requests;

public interface Redirectable {
    // Method that will return the same request to the redirected URI
    Request getRedirectRequest(String redirectURI);
}
