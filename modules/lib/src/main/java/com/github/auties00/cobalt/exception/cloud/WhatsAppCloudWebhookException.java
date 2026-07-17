package com.github.auties00.cobalt.exception.cloud;

/**
 * Thrown when an inbound webhook delivery fails verification or cannot be parsed.
 *
 * <p>Raised when the {@code X-Hub-Signature-256} header does not match the HMAC computed over the
 * raw request body, or when the {@code object=whatsapp_business_account} envelope is malformed.
 * The failure is scoped to a single delivery and does not affect the rest of the session.
 */
public final class WhatsAppCloudWebhookException extends WhatsAppCloudException {
    /**
     * Constructs a new Cloud webhook exception with the specified detail message.
     *
     * @param message the detail message describing the webhook failure
     */
    public WhatsAppCloudWebhookException(String message) {
        super(message);
    }

    /**
     * Constructs a new Cloud webhook exception with the specified detail message and cause.
     *
     * @param message the detail message describing the webhook failure
     * @param cause   the underlying cause of this exception
     */
    public WhatsAppCloudWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
