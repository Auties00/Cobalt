package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.github.auties00.cobalt.exception.WhatsAppException;

/**
 * Enumerates the recovery actions that an error handler can request.
 *
 * <p>The value returned from
 * {@link WhatsAppLinkedClientErrorHandler#handleError(LinkedWhatsAppClient, WhatsAppException)}
 * is translated by the client into a concrete
 * {@link WhatsAppClientDisconnectReason}: {@code DISCARD} leaves the
 * session running, whereas the other values disconnect with the
 * corresponding reason.
 */
public enum WhatsAppLinkedClientErrorResult {
    /**
     * Swallows the error and leaves the session running.
     *
     * <p>Appropriate for transient faults that do not compromise the
     * session state.
     */
    DISCARD,

    /**
     * Tears the session down while preserving the credentials on disk.
     *
     * <p>The client emits
     * {@link WhatsAppClientDisconnectReason#DISCONNECTED} and does not
     * attempt to reconnect.
     */
    DISCONNECT,

    /**
     * Tears the session down and immediately re-establishes the
     * connection.
     *
     * <p>The client emits
     * {@link WhatsAppClientDisconnectReason#RECONNECTING} and, after
     * notifying listeners, calls {@link LinkedWhatsAppClient#connect()}
     * internally.
     */
    RECONNECT,

    /**
     * Terminates the session and treats the account as banned.
     *
     * <p>The client emits
     * {@link WhatsAppClientDisconnectReason#BANNED} and deletes the
     * session store; reconnection is not attempted.
     */
    BAN,

    /**
     * Terminates the session and logs the account out.
     *
     * <p>The client emits
     * {@link WhatsAppClientDisconnectReason#LOGGED_OUT} and deletes the
     * session store so that fresh credentials must be obtained before
     * the next connection.
     */
    LOG_OUT
}
