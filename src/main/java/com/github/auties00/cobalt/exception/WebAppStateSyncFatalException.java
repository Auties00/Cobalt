package com.github.auties00.cobalt.exception;

public final class WebAppStateSyncFatalException
        extends WebAppStateSyncException {
    public WebAppStateSyncFatalException(String message) {
        super(message);
    }

    public WebAppStateSyncFatalException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebAppStateSyncFatalException(Throwable cause) {
        super(cause);
    }
}
