package it.auties.whatsapp.exception;

public final class WebAppStatePullException extends WebAppStateException{
    public WebAppStatePullException(String message) {
        super(message);
    }
    
    public WebAppStatePullException(String message, Throwable cause) {
        super(message, cause);
    }
}
