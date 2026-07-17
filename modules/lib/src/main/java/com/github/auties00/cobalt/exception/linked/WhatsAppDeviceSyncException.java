package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Thrown when a USync device-list query against the WhatsApp servers
 * returns an error.
 *
 * USync is the request Cobalt issues before sending a message to learn the
 * set of devices that belong to each recipient. The server can reject the
 * request as a whole (a batch-wide failure that blocks the entire send) or
 * report a per-device issue inside an otherwise successful response (a
 * partial failure that still lets other recipients be addressed). The
 * {@code fatal} flag passed to the constructor mirrors that distinction
 * and is reflected in {@link #isFatal()}; the server-reported error code is
 * available through {@link #errorCode()}.
 *
 * @apiNote
 * Inspect {@link #isFatal()} to tell a batch-wide rejection from a partial
 * one: a partial instance leaves the rest of the USync response usable, so
 * the send can proceed to the recipients that resolved. The session-level
 * recovery is unaffected by that distinction: {@link #toErrorResult()}
 * returns {@link WhatsAppLinkedClientErrorResult#DISCARD} for both, since a USync
 * failure never tears the session down.
 */
public final class WhatsAppDeviceSyncException extends WhatsAppLinkedException {
    /**
     * The numeric error code returned in the USync error stanza.
     */
    private final int errorCode;

    /**
     * Whether the USync server response marked this failure as
     * batch-wide.
     */
    private final boolean fatal;

    /**
     * Constructs a new device sync exception.
     *
     * @param errorCode the numeric error code returned by the server
     * @param errorText the human-readable description returned by the server
     * @param fatal     {@code true} when the failure affects the whole batch,
     *                  {@code false} when only a subset of devices failed
     */
    public WhatsAppDeviceSyncException(int errorCode, String errorText, boolean fatal) {
        super("USync error " + errorCode + ": " + errorText);
        this.errorCode = errorCode;
        this.fatal = fatal;
    }

    /**
     * Returns the numeric error code returned by the USync server.
     *
     * The code is the value the server reported for this failure, copied
     * verbatim at construction time.
     *
     * @apiNote
     * Use it to disambiguate the server-side failure mode beyond the
     * coarse {@link #isFatal()} distinction.
     *
     * @return the error code
     */
    public int errorCode() {
        return errorCode;
    }

    /**
     * Returns whether the USync server response marked this failure as
     * batch-wide.
     *
     * <p>A batch-wide rejection blocks the entire send, while a partial
     * failure leaves the rest of the USync response usable so the send can
     * proceed to the recipients that resolved. This classification is
     * independent of {@link #toErrorResult()}, which returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD} either way because a USync
     * device-list failure never tears the session down.
     *
     * @return {@code true} for a batch-wide rejection, {@code false} when
     *         only a subset of devices failed
     */
    public boolean isFatal() {
        return fatal;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link WhatsAppLinkedClientErrorResult#DISCARD}:
     * a USync device-list error is a per-request failure that leaves the
     * session running, whether it is batch-wide or partial (see
     * {@link #isFatal()}).
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }
}
