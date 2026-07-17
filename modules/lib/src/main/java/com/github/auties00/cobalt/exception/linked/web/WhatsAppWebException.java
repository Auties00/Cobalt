package com.github.auties00.cobalt.exception.linked.web;

import com.github.auties00.cobalt.exception.linked.WhatsAppLinkedException;

/**
 * Sealed root for failures specific to the WhatsApp Web/Desktop companion
 * surface of the Linked transport.
 *
 * <p>These failures arise from behaviour that only the browser-based
 * WhatsApp Web and Electron-era WhatsApp Desktop companion clients exhibit,
 * as opposed to the transport-wide faults carried directly by
 * {@link WhatsAppLinkedException}. The permitted subtypes cover the
 * companion app-state (syncd) sync pipeline
 * ({@link WhatsAppWebAppStateSyncException}) and the browser-surface
 * GraphQL session-credential refresh
 * ({@link WhatsAppWebGraphQlException}). The permits list is closed, so a
 * {@code switch} over a {@code WhatsAppWebException} can be exhaustive.
 *
 * @apiNote
 * Catch this base type to react to every web/desktop companion failure at
 * once; failures that also occur on the mobile primary device are carried
 * by {@link WhatsAppLinkedException} directly rather than here.
 */
public abstract sealed class WhatsAppWebException extends WhatsAppLinkedException
        permits WhatsAppWebAppStateSyncException,
                WhatsAppWebGraphQlException {

    /**
     * Constructs a new web exception with no detail message.
     */
    protected WhatsAppWebException() {
        super();
    }

    /**
     * Constructs a new web exception with the specified detail message.
     *
     * @param message the detail message describing the error condition
     */
    protected WhatsAppWebException(String message) {
        super(message);
    }

    /**
     * Constructs a new web exception with the specified detail message and cause.
     *
     * @param message the detail message describing the error condition
     * @param cause   the underlying cause of this exception
     */
    protected WhatsAppWebException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new web exception wrapping the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    protected WhatsAppWebException(Throwable cause) {
        super(cause);
    }
}
