package it.auties.whatsapp.exception;

public final class WebAppStatePushException extends WebAppStateException{
    public WebAppStatePushException(String message) {
        super(message);
    }

    public WebAppStatePushException(String message, Throwable cause) {
        super(message, cause);
    }
}
