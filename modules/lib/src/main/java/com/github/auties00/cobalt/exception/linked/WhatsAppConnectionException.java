package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Thrown when Cobalt cannot establish the initial connection to the
 * WhatsApp servers.
 *
 * Covers DNS resolution, TCP, TLS, the WebSocket upgrade, and the Noise XX
 * handshake that authenticates the client. Any failure along that path
 * raises this exception. It is distinct from
 * {@link WhatsAppReconnectionException}, which fires when a retry cannot
 * complete after a previously successful session was lost.
 *
 * @apiNote
 * {@link #toErrorResult()} reports {@link WhatsAppLinkedClientErrorResult#DISCONNECT}:
 * there is no live session to recover, so the configured error handler cannot
 * reconnect on the existing one. Embedders typically respond by surfacing the
 * failure to the user or scheduling a fresh connection attempt.
 *
 * @implNote
 * This implementation always classifies the failure as
 * {@link WhatsAppLinkedClientErrorResult#DISCONNECT} because there is no live
 * session to recover.
 *
 * @see WhatsAppReconnectionException
 * @see WhatsAppSessionException.Closed
 */
public final class WhatsAppConnectionException extends WhatsAppLinkedException {

    /**
     * Constructs a new connection exception with the specified detail message.
     *
     * @param message the detail message describing the connection failure
     */
    public WhatsAppConnectionException(String message) {
        super(message);
    }

    /**
     * Constructs a new connection exception with the specified detail message and cause.
     *
     * @param message the detail message describing the connection failure
     * @param cause   the underlying cause of the connection failure
     */
    public WhatsAppConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCONNECT}: no session has been
     * established when a connection exception is thrown, so there is nothing
     * to reconnect and the failure is surfaced to the caller.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCONNECT;
    }
}
