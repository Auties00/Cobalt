package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Thrown when the message history transfer from the primary phone to a
 * newly linked companion device fails.
 *
 * WhatsApp ships chat history to a freshly paired companion as a sequence
 * of encrypted blobs uploaded by the primary phone and downloaded,
 * decrypted, and applied by the companion. This exception is raised when
 * any stage fails: downloading the blob, decrypting it, parsing the
 * embedded payload, or persisting the resulting messages.
 *
 * @apiNote
 * The failure only affects the backfill of past messages on this device;
 * {@link #toErrorResult()} returns
 * {@link WhatsAppLinkedClientErrorResult#DISCARD} so the session keeps receiving
 * new messages, and a partial or missing history can be retried later when
 * the primary phone is online again.
 *
 * @implNote
 * This implementation has {@link #toErrorResult()} return
 * {@link WhatsAppLinkedClientErrorResult#DISCARD}: it never affects the active
 * session, only the history backfill.
 */
public final class WhatsAppHistorySyncException extends WhatsAppLinkedException {

    /**
     * Constructs a new history sync exception with no detail message.
     */
    public WhatsAppHistorySyncException() {
        super();
    }

    /**
     * Constructs a new history sync exception with the specified detail message.
     *
     * @param message the detail message describing the sync failure
     */
    public WhatsAppHistorySyncException(String message) {
        super(message);
    }

    /**
     * Constructs a new history sync exception with a detail message and cause.
     *
     * @param message the detail message describing the sync failure
     * @param cause   the underlying cause of the sync failure
     */
    public WhatsAppHistorySyncException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new history sync exception wrapping the specified cause.
     *
     * @param cause the underlying cause of the sync failure
     */
    public WhatsAppHistorySyncException(Throwable cause) {
        super(cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: a history backfill failure
     * only affects the population of past messages on this device, not the
     * active session.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }
}
