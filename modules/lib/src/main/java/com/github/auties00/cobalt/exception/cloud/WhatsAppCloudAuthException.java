package com.github.auties00.cobalt.exception.cloud;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

/**
 * Thrown when the configured access token is missing, malformed, invalid, or expired.
 *
 * <p>Unlike a {@link WhatsAppCloudApiException}, which scopes to one request, an authentication failure
 * means no request can succeed until the integrator supplies fresh credentials, so the whole
 * Cloud session is unusable.
 */
public final class WhatsAppCloudAuthException extends WhatsAppCloudException {
    /**
     * Constructs a new Cloud authentication exception with the specified detail message.
     *
     * @param message the detail message describing the authentication failure
     */
    public WhatsAppCloudAuthException(String message) {
        super(message);
    }

    /**
     * Constructs a new Cloud authentication exception with the specified detail message and
     * cause.
     *
     * @param message the detail message describing the authentication failure
     * @param cause   the underlying cause of this exception
     */
    public WhatsAppCloudAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link WhatsAppLinkedClientErrorResult#DISCONNECT}: with no valid
     * access token the Cloud client cannot send or receive anything, so the session is unusable
     * until it is re-credentialed.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCONNECT;
    }
}
