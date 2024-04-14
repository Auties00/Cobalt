package it.auties.whatsapp.exception;

/**
 * This exception is thrown when a request cannot be sent to Whatsapp's socket
 */
public class RequestException extends RuntimeException {
    public RequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestException(Throwable cause) {
        super(cause);
    }
}
