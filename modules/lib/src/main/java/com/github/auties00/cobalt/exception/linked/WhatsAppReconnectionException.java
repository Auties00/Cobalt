package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Thrown when an attempt to re-establish a previously open WhatsApp
 * session fails.
 *
 * <p>After a session is dropped (a network blip, the server cycling the
 * connection, or a {@link WhatsAppSessionException} being raised), Cobalt
 * schedules a reconnect attempt with backoff; this exception reports that
 * such an attempt did not complete. It is distinct from
 * {@link WhatsAppConnectionException}, which covers the very first
 * connection, before any session has existed.
 *
 * @apiNote
 * Raised after a reconnect attempt fails; {@link #attempts()} carries the
 * number of attempts made so far so the caller can bound retries or
 * compute a backoff. {@link #toErrorResult()} returns
 * {@link WhatsAppLinkedClientErrorResult#DISCARD} so the reconnect supervisor
 * retries on the next backoff tick rather than tearing down a session that
 * is already gone.
 *
 * @see WhatsAppConnectionException
 */
public final class WhatsAppReconnectionException extends WhatsAppLinkedException {

    /**
     * The number of reconnection attempts that have already been made
     * when this exception is raised.
     */
    private final int attempts;

    /**
     * Constructs a new reconnection exception with the specified message and attempt count.
     *
     * @param message  the detail message describing the reconnection failure
     * @param attempts the number of reconnection attempts made so far
     */
    public WhatsAppReconnectionException(String message, int attempts) {
        super(message);
        this.attempts = attempts;
    }

    /**
     * Constructs a new reconnection exception with a message, attempt count, and cause.
     *
     * @param message  the detail message describing the reconnection failure
     * @param attempts the number of reconnection attempts made so far
     * @param cause    the underlying cause of the reconnection failure
     */
    public WhatsAppReconnectionException(String message, int attempts, Throwable cause) {
        super(message, cause);
        this.attempts = attempts;
    }

    /**
     * Returns the number of reconnection attempts already made.
     *
     * @apiNote
     * Use the count to bound retries or to compute a backoff delay
     * before scheduling another attempt.
     *
     * @return the attempt count, always non-negative
     */
    public int attempts() {
        return attempts;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: the reconnect supervisor
     * retries on the next backoff tick, so the error is swallowed rather
     * than tearing down a session that is already gone.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }
}
