package com.github.auties00.cobalt.stream.notification.account;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;

/**
 * Routes inbound {@code <notification>} stanzas in the account category to the matching per-type handler.
 *
 * <p>The parent notification stream forwards every stanza whose {@code type} attribute names an
 * account-scoped notification (status, contacts, disappearing mode, privacy tokens, picture, about)
 * to this dispatcher, which owns one instance of each concrete sub-handler and selects the branch on
 * the {@code type} attribute. Stanzas with no {@code type} attribute and stanzas whose type is not
 * owned by this dispatcher return without side-effects; the surrounding pipeline owns the ACK or NACK
 * decision for unmatched stanzas.</p>
 *
 * @implNote This implementation collapses six WA Web notification handler modules (account_sync,
 * contacts, disappearing_mode, privacy_token, picture, status) into one Java type with five fields,
 * whereas WA Web routes each type through the central {@code WAWebCommsHandleLoggedInStanza} switch to
 * a dedicated handler module. The grouping has no protocol effect; it keeps the dependency graph in the
 * parent notification stream manageable.
 */
@WhatsAppWebModule(moduleName = "WAWebCommsHandleLoggedInStanza")
public final class NotificationAccountDispatcher extends SocketStreamHandler.Concurrent {
    /** The logger for {@link NotificationAccountDispatcher}. */
    private static final System.Logger LOGGER = Log.get(NotificationAccountDispatcher.class);

    /**
     * Handles {@code type="account_sync"} notifications.
     *
     * <p>Covers status, text status, privacy, devices, blocklist, picture, disappearing mode, TOS,
     * notice, user, and business-opt-out updates for the authenticated account.</p>
     */
    private final NotificationAccountStreamHandler accountHandler;

    /**
     * Handles {@code type="contacts"} notifications.
     *
     * <p>Covers contact updates, phone-number changes, and full contact resyncs.</p>
     */
    private final NotificationContactStreamHandler contactHandler;

    /**
     * Handles {@code type="disappearing_mode"} notifications.
     *
     * <p>Covers per-chat ephemeral-timer changes.</p>
     */
    private final NotificationDisappearingModeStreamHandler disappearingModeHandler;

    /**
     * Handles {@code type="privacy_token"} notifications.
     *
     * <p>Covers trusted-contact tokens for end-to-end identity verification.</p>
     */
    private final NotificationPrivacyStreamHandler privacyHandler;

    /**
     * Handles {@code type="picture"} and {@code type="status"} notifications.
     *
     * <p>Covers profile-picture and about-text changes.</p>
     */
    private final NotificationProfileStreamHandler profileHandler;

    /**
     * Constructs the dispatcher and eagerly instantiates every sub-handler with the shared client and
     * device service.
     *
     * <p>The {@code deviceService} is forwarded only to {@link NotificationAccountStreamHandler}, which
     * uses it when refreshing the authenticated user's own device list; the {@code whatsapp} client and
     * {@code ackSender} are forwarded to every sub-handler.</p>
     *
     * @param whatsapp      the {@link LinkedWhatsAppClient} forwarded to every sub-handler for store and stanza access
     * @param deviceService the {@link DeviceService} consumed only by {@link NotificationAccountStreamHandler}
     * @param ackSender     the {@link AckSender} forwarded to every sub-handler for the per-notification {@code <ack>} stanza
     * @param wamService    the {@link WamService}
     */
    public NotificationAccountDispatcher(LinkedWhatsAppClient whatsapp, DeviceService deviceService, AckSender ackSender, WamService wamService) {
        this.accountHandler = new NotificationAccountStreamHandler(whatsapp, deviceService, ackSender);
        this.contactHandler = new NotificationContactStreamHandler(whatsapp, ackSender);
        this.disappearingModeHandler = new NotificationDisappearingModeStreamHandler(whatsapp, wamService, ackSender);
        this.privacyHandler = new NotificationPrivacyStreamHandler(whatsapp, ackSender);
        this.profileHandler = new NotificationProfileStreamHandler(whatsapp, ackSender);
    }

    /**
     * Forwards {@code stanza} to the sub-handler whose category matches the stanza's {@code type} attribute.
     *
     * <p>Stanzas with no {@code type} attribute and stanzas whose type is unknown to this dispatcher
     * return without side-effects. The {@code "picture"} and {@code "status"} types both route to
     * {@link NotificationProfileStreamHandler}; all other owned types route to their dedicated handler.</p>
     *
     * @implNote This implementation maps {@code "picture"} and {@code "status"} to the same
     * {@link NotificationProfileStreamHandler} because WA Web's two source modules share enough structure
     * (action child, hash-vs-jid resolution, ack format) that Cobalt merges them into one handler.
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
            case "account_sync" -> accountHandler.handle(stanza);
            case "contacts" -> contactHandler.handle(stanza);
            case "disappearing_mode" -> disappearingModeHandler.handle(stanza);
            case "privacy_token" -> privacyHandler.handle(stanza);
            case "picture", "status" -> profileHandler.handle(stanza);
            default -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping account notification with unrecognized type {0}", type);
            }
        }
    }

    /**
     * Propagates the reset signal to every sub-handler so per-session caches are cleared on a socket reconnect.
     *
     * <p>Invoked by the parent notification stream when {@link SocketStreamHandler#reset()} fires, clearing
     * each sub-handler's pending acks and in-flight refresh jobs.</p>
     */
    @Override
    public void reset() {
        accountHandler.reset();
        contactHandler.reset();
        disappearingModeHandler.reset();
        privacyHandler.reset();
        profileHandler.reset();
    }
}
