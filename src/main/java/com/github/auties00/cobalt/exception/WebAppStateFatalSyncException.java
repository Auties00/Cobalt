package com.github.auties00.cobalt.exception;

public final class WebAppStateFatalSyncException
        extends WebAppStateSyncException {
    public WebAppStateFatalSyncException(String message) {
        super(message);
    }

    public WebAppStateFatalSyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebAppStateFatalSyncException(Throwable cause) {
        super(cause);
    }
}
