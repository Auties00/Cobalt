package com.github.auties00.cobalt.exception;

public final class WebAppStatePullException extends WebAppStateException{
    public WebAppStatePullException(String message) {
        super(message);
    }
    
    public WebAppStatePullException(String message, Throwable cause) {
        super(message, cause);
    }
}
