package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.exception.WhatsAppException;
import com.github.auties00.cobalt.exception.linked.mobile.WhatsAppMobileException;
import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebException;

/**
 * Sealed root for every failure raised by the Linked transport.
 *
 * <p>The Linked transport is Cobalt's reimplementation of WhatsApp Web,
 * WhatsApp Desktop, and WhatsApp Mobile over the encrypted binary-XMPP
 * socket with Signal/Noise cryptography, driven by
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient}.
 * Its permitted subtypes split into two groups: transport-wide failures
 * that can arise on any Linked flavour (a session abort, a message or media
 * failure, an app-state or history sync fault, and so on), and two
 * platform-scoped branches, {@link WhatsAppWebException} for failures
 * specific to the WhatsApp Web/Desktop companion surface and
 * {@link WhatsAppMobileException} for failures specific to the mobile
 * primary device. The permits list is closed, so a {@code switch} over a
 * {@code WhatsAppLinkedException} can be exhaustive.
 *
 * @apiNote
 * Catch this base type to react to every Linked failure mode at once,
 * regardless of flavour; the Cloud transport raises
 * {@link com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException}
 * instead, so the two transports never share a concrete exception type.
 */
public abstract sealed class WhatsAppLinkedException extends WhatsAppException
        permits WhatsAppABPropException,
                WhatsAppAdvValidationException,
                WhatsAppBotSignatureException,
                WhatsAppCallException,
                WhatsAppConnectionException,
                WhatsAppCorruptedStoreException,
                WhatsAppDeviceSyncException,
                WhatsAppFacebookGraphQlException,
                WhatsAppHistorySyncException,
                WhatsAppIntegrityChallengeException,
                WhatsAppLidMigrationException,
                WhatsAppMediaException,
                WhatsAppMessageException,
                WhatsAppOwnDeviceListExpiredException,
                WhatsAppPrivateStatsTokenIssuerException,
                WhatsAppReconnectionException,
                WhatsAppServerRuntimeException,
                WhatsAppSessionException,
                WhatsAppStreamException,
                WhatsAppMobileException,
                WhatsAppWebException {

    /**
     * Constructs a new Linked exception with no detail message.
     */
    protected WhatsAppLinkedException() {
        super();
    }

    /**
     * Constructs a new Linked exception with the specified detail message.
     *
     * @param message the detail message describing the error condition
     */
    protected WhatsAppLinkedException(String message) {
        super(message);
    }

    /**
     * Constructs a new Linked exception with the specified detail message and cause.
     *
     * @param message the detail message describing the error condition
     * @param cause   the underlying cause of this exception
     */
    protected WhatsAppLinkedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new Linked exception wrapping the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    protected WhatsAppLinkedException(Throwable cause) {
        super(cause);
    }
}
