package com.github.auties00.cobalt.exception;

public sealed class WebAppStateSyncGenericRetryableException
        extends WebAppStateSyncException permits WebAppStateSyncMissingKeyException {
    public WebAppStateSyncGenericRetryableException(String message) {
        super(message);
    }

    public WebAppStateSyncGenericRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
