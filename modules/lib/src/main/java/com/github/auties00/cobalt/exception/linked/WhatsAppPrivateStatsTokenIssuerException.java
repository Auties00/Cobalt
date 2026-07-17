package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Thrown when Cobalt cannot obtain a private-stats authentication token
 * from the WhatsApp servers.
 *
 * <p>The private-stats upload backend that feeds the WAM telemetry
 * pipeline gates each upload with a single-use, blinded credential the
 * server issues on request. This exception covers every way that issuance
 * can fail: the server returns an error instead of a credential, a
 * required element of the reply is missing or malformed, or the returned
 * Ed25519 material does not decode as a valid curve point so the
 * unblinding step cannot run.
 *
 * @apiNote
 * Raised only on the WAM telemetry path, which most embedders do not run.
 * {@link #toErrorResult()} returns
 * {@link WhatsAppLinkedClientErrorResult#DISCARD}, so a configured
 * {@code WhatsAppClientErrorHandler} can leave the session running and
 * the upload can be retried independently.
 */
@WhatsAppWebModule(moduleName = "WAWebIssuePrivateStatsToken")
public final class WhatsAppPrivateStatsTokenIssuerException extends WhatsAppLinkedException {

    /**
     * Constructs a new token-issuance exception with the specified detail message.
     *
     * @param message the detail message describing the failure
     */
    public WhatsAppPrivateStatsTokenIssuerException(String message) {
        super(message);
    }

    /**
     * Constructs a new token-issuance exception with the specified detail message and cause.
     *
     * @param message the detail message describing the failure
     * @param cause   the underlying cause
     */
    public WhatsAppPrivateStatsTokenIssuerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: token issuance is scoped
     * to a single telemetry upload, which can be retried independently.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }
}
