package com.github.auties00.cobalt.exception.linked.mobile;

import com.github.auties00.cobalt.exception.linked.WhatsAppLinkedException;

/**
 * Sealed root for failures specific to the mobile primary device flavour of
 * the Linked transport.
 *
 * <p>These failures arise from behaviour that only the mobile primary
 * client exhibits, as opposed to the transport-wide faults carried directly
 * by {@link WhatsAppLinkedException}. The single permitted subtype covers
 * the phone-number registration ceremony (SMS, voice, or OTP verification)
 * that a primary device performs before it can connect
 * ({@link WhatsAppRegistrationException}). The permits list is closed, so a
 * {@code switch} over a {@code WhatsAppMobileException} can be exhaustive.
 *
 * @apiNote
 * Catch this base type to react to every mobile-primary failure at once;
 * failures that also occur on the web/desktop companion are carried by
 * {@link WhatsAppLinkedException} or {@link com.github.auties00.cobalt.exception.linked.web.WhatsAppWebException}
 * rather than here.
 */
public abstract sealed class WhatsAppMobileException extends WhatsAppLinkedException
        permits WhatsAppRegistrationException {

    /**
     * Constructs a new mobile exception with no detail message.
     */
    protected WhatsAppMobileException() {
        super();
    }

    /**
     * Constructs a new mobile exception with the specified detail message.
     *
     * @param message the detail message describing the error condition
     */
    protected WhatsAppMobileException(String message) {
        super(message);
    }

    /**
     * Constructs a new mobile exception with the specified detail message and cause.
     *
     * @param message the detail message describing the error condition
     * @param cause   the underlying cause of this exception
     */
    protected WhatsAppMobileException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new mobile exception wrapping the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    protected WhatsAppMobileException(Throwable cause) {
        super(cause);
    }
}
