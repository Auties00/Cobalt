package com.github.auties00.cobalt.stream.notification.business;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientPasskeyAuthenticator;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.NodeStreamService;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;

/**
 * Routes inbound {@code <notification>} stanzas whose category covers WhatsApp Business to the matching per-type handler.
 *
 * <p>The covered categories are verified name, business profile, product catalog, subscriptions, click-to-WhatsApp
 * suggestions, small-business privacy settings, ad-account nonces, marketing-campaign updates, Meta Exchange GraphQL
 * events, and payment transactions. The parent notification pipeline forwards every stanza whose {@code type} attribute
 * falls in the business branch to this dispatcher; it owns one instance of each concrete handler and forwards each
 * stanza to the handler whose category matches the {@code type} attribute.
 *
 * @implNote
 * This implementation merges three WhatsApp Web modules under one Cobalt dispatcher because they share the
 * business-account fan-out surface; each sub-handler remains keyed to its source module via the
 * {@link WhatsAppWebModule} annotation on the handler class.
 */
@WhatsAppWebModule(moduleName = "WAWebCommsHandleLoggedInStanza")
public final class NotificationBusinessDispatcher extends SocketStreamHandler.Concurrent {
    /** The logger for {@link NotificationBusinessDispatcher}. */
    private static final System.Logger LOGGER = Log.get(NotificationBusinessDispatcher.class);

    /**
     * Handles {@code business}, {@code digital_commerce_subscription}, and {@code fb:update} notifications.
     */
    private final NotificationBusinessStreamHandler businessHandler;

    /**
     * Handles {@code mex} notifications carrying Meta Exchange GraphQL subscription update payloads.
     */
    private final NotificationMexStreamHandler mexHandler;

    /**
     * Handles {@code pay} notifications carrying payment-invite and payment-transaction stanzas.
     */
    private final NotificationPaymentStreamHandler paymentHandler;

    /**
     * Constructs the dispatcher and eagerly instantiates every sub-handler with the shared client and migration service.
     *
     * <p>Called once during {@link NodeStreamService} setup. The {@code lidMigrationService} is consumed only by the
     * {@code NotificationMexStreamHandler} when applying LID-change Meta Exchange events; the {@link LinkedWhatsAppClient}
     * and {@link AckSender} are forwarded to every sub-handler for store and stanza access and for emitting the
     * per-notification outbound {@code <ack>} stanza.
     *
     * @param whatsapp             the client forwarded to every sub-handler
     * @param lidMigrationService  the LID migration service forwarded to the Meta Exchange handler
     * @param ackSender            the ack sender forwarded to every sub-handler
     * @param passkeyAuthenticator the passkey authenticator forwarded to the Meta Exchange handler for
     *                             answering integrity checkpoints, or {@code null} when none is configured
     * @param wamService           the wam service
     */
    public NotificationBusinessDispatcher(LinkedWhatsAppClient whatsapp, LidMigrationService lidMigrationService, AckSender ackSender, LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator, WamService wamService) {
        this.businessHandler = new NotificationBusinessStreamHandler(whatsapp, ackSender, wamService);
        this.mexHandler = new NotificationMexStreamHandler(whatsapp, lidMigrationService, ackSender, passkeyAuthenticator, wamService);
        this.paymentHandler = new NotificationPaymentStreamHandler(whatsapp, ackSender);
    }

    /**
     * Forwards {@code stanza} to the sub-handler whose category matches the stanza's {@code type} attribute.
     *
     * <p>Stanzas with no {@code type} and stanzas whose {@code type} this dispatcher does not own are dropped; the
     * outer stream pipeline owns the NACK decision for stanzas this dispatcher does not match. The three
     * business-flavoured types {@code business}, {@code digital_commerce_subscription}, and {@code fb:update} all route
     * to {@link NotificationBusinessStreamHandler}; {@code mex} routes to {@code NotificationMexStreamHandler} and
     * {@code pay} routes to {@code NotificationPaymentStreamHandler}.
     *
     * @param stanza the incoming {@code <notification>} stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebCommsHandleLoggedInStanza", exports = "handleLoggedInStanza", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void handle(Stanza stanza) {
        var type = stanza.getAttributeAsString("type", null);
        if (type == null) {
            return;
        }

        switch (type) {
            case "business", "digital_commerce_subscription", "fb:update" -> businessHandler.handle(stanza);
            case "mex" -> mexHandler.handle(stanza);
            case "pay" -> paymentHandler.handle(stanza);
            default -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping business notification with unrecognized type {0}", type);
            }
        }
    }

    /**
     * Propagates {@link SocketStreamHandler#reset()} to every sub-handler.
     *
     * <p>Invoked by the parent notification pipeline when the stream reset signal fires.
     */
    @Override
    public void reset() {
        businessHandler.reset();
        mexHandler.reset();
        paymentHandler.reset();
    }
}
