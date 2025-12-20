package com.github.auties00.cobalt.exception;

/**
 * Exception thrown when WhatsApp Web App State synchronization fails.
 */
public abstract sealed class WebAppStateSyncException
        extends RuntimeException
        permits WebAppStateFatalSyncException, WebAppStateRetryableSyncException {
    public WebAppStateSyncException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new WebAppStateSyncException with the specified message.
     *
     * @param message the detail message
     */
    public WebAppStateSyncException(String message) {
        super(message);
    }

    /**
     * Creates a new WebAppStateSyncException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public WebAppStateSyncException(String message, Throwable cause) {
        super(message, cause);
    }

}
