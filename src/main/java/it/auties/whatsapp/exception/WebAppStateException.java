package it.auties.whatsapp.exception;

public sealed abstract class WebAppStateException extends RuntimeException permits WebAppStatePullException, WebAppStatePushException {
    public WebAppStateException(String message) {
        super(message);
    }

    public WebAppStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
