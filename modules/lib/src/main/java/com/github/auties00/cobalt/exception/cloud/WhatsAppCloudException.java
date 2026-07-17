package com.github.auties00.cobalt.exception.cloud;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorHandler;
import com.github.auties00.cobalt.exception.WhatsAppException;

/**
 * Sealed root for failures encountered while talking to Meta's WhatsApp Cloud API.
 *
 * <p>The Cloud transport reaches {@code graph.facebook.com} over HTTPS for outbound traffic and
 * receives inbound traffic through a webhook the integrator hosts. Each layer has its own failure
 * mode, enumerated by the permitted subtypes: the graph endpoint rejected a request with a structured
 * error envelope ({@link WhatsAppCloudApiException}), the configured access token is missing, invalid, or
 * expired so no request can succeed ({@link WhatsAppCloudAuthException}), an inbound webhook delivery failed
 * its signature check or could not be parsed ({@link WhatsAppCloudWebhookException}), or an operation requires
 * a newer Cloud API version than the client is configured to target
 * ({@link WhatsAppCloudUnsupportedVersionException}).
 *
 * @apiNote
 * Embedders typically observe these through the configured
 * {@link WhatsAppLinkedClientErrorHandler} rather than around individual
 * calls; pattern-match the concrete subtype to react to a specific failure mode.
 *
 * @see WhatsAppCloudApiException
 * @see WhatsAppCloudAuthException
 * @see WhatsAppCloudWebhookException
 * @see WhatsAppCloudUnsupportedVersionException
 */
public sealed abstract class WhatsAppCloudException extends WhatsAppException
        permits WhatsAppCloudApiException,
                WhatsAppCloudAuthException,
                WhatsAppCloudUnsupportedVersionException,
                WhatsAppCloudWebhookException {

    /**
     * Constructs a new Cloud exception with the specified detail message.
     *
     * @param message the detail message describing the Cloud failure
     */
    protected WhatsAppCloudException(String message) {
        super(message);
    }

    /**
     * Constructs a new Cloud exception with the specified detail message and cause.
     *
     * @param message the detail message describing the Cloud failure
     * @param cause   the underlying cause of this exception
     */
    protected WhatsAppCloudException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link WhatsAppLinkedClientErrorResult#DISCARD}: a single Cloud failure
     * leaves the access token and the rest of the Cloud session usable. {@link WhatsAppCloudAuthException}
     * overrides this because a credential failure makes the whole session unusable.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }
}
