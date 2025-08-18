package it.auties.whatsapp.exception;

/**
 * This exception is thrown when a request cannot be sent to Whatsapp's socket
 */
public class RequestException extends RuntimeException {
    public RequestException(String message) {
        super(message, null);
    }
}
