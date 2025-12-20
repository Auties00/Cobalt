package com.github.auties00.cobalt.exception;

public sealed class WebAppStateRetryableSyncException
        extends WebAppStateSyncException permits WebAppStateMissingKeyException {
    public WebAppStateRetryableSyncException(String message) {
        super(message);
    }

    public WebAppStateRetryableSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
