package com.github.auties00.cobalt.stream.notification;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientPasskeyAuthenticator;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.pairing.CompanionPairingService;
import com.github.auties00.cobalt.pairing.ShortcakePairingService;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.stream.control.OfflineNotificationsReporter;
import com.github.auties00.cobalt.stream.notification.account.NotificationAccountDispatcher;
import com.github.auties00.cobalt.stream.notification.business.NotificationBusinessDispatcher;
import com.github.auties00.cobalt.stream.notification.device.NotificationDeviceDispatcher;
import com.github.auties00.cobalt.stream.notification.group.NotificationGroupStreamHandler;
import com.github.auties00.cobalt.stream.NodeStreamService;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;

/**
 * Dispatches inbound {@code <notification>} stanzas to the appropriate
 * category-specific handler based on their {@code type} attribute.
 *
 * <p>Every server-to-client out-of-band event (contact sync, profile
 * picture changes, device registration churn, payments, business profile
 * updates, group membership churn, app-state replication, and so on)
 * arrives as a {@code <notification>} stanza carrying a {@code type}
 * attribute. This handler reads that attribute and forwards the stanza to
 * one of four functional families, each owned by a dedicated dispatcher:
 * <ul>
 *   <li>account-scoped events ({@code account_sync}, {@code contacts},
 *       {@code disappearing_mode}, {@code picture},
 *       {@code privacy_token}, {@code status}) go to
 *       {@link NotificationAccountDispatcher};</li>
 *   <li>business-scoped events ({@code business},
 *       {@code digital_commerce_subscription}, {@code fb:update},
 *       {@code mex}, {@code pay}) go to
 *       {@link NotificationBusinessDispatcher};</li>
 *   <li>device-scoped events ({@code companion_reg_refresh},
 *       {@code devices}, {@code encrypt}, {@code hosted},
 *       {@code link_code_companion_reg}, {@code mediaretry},
 *       {@code newsletter}, {@code psa}, {@code registration},
 *       {@code server}, {@code server_sync}, {@code w:growth},
 *       {@code waffle}) go to
 *       {@link NotificationDeviceDispatcher};</li>
 *   <li>group-scoped events ({@code w:gp2}) go to
 *       {@link NotificationGroupStreamHandler}.</li>
 * </ul>
 *
 * <p>Stanzas with no {@code type} attribute or an unrecognised value are
 * silently dropped.
 *
 * @implNote
 * This implementation collapses WA Web's flat 21-arm {@code type} switch
 * into a four-family tree to keep each sub-handler at a manageable size
 * and to share dependencies across thematically related notification
 * types. Unknown {@code type} values are dropped here without emitting a
 * NACK; WA Web instead routes the unknown stanza through a generic NACK
 * path, which Cobalt centralises in {@link NodeStreamService}'s error model.
 */
@WhatsAppWebModule(moduleName = "WAWebCommsHandleLoggedInStanza")
public final class NotificationStreamHandler extends SocketStreamHandler.Concurrent {
    /** The logger for {@link NotificationStreamHandler}. */
    private static final System.Logger LOGGER = Log.get(NotificationStreamHandler.class);

    /**
     * Holds the sub-dispatcher for notifications that affect the current
     * account's contact list, privacy settings, profile picture, or status
     * broadcasts.
     */
    private final NotificationAccountDispatcher accountHandler;

    /**
     * Holds the sub-dispatcher for notifications that target the business
     * profile, MEX payloads, Facebook-side updates, and payment events.
     */
    private final NotificationBusinessDispatcher businessHandler;

    /**
     * Holds the sub-dispatcher for notifications that affect device-level
     * state: companion registration, prekey churn, server sync, newsletter
     * membership, waffle account-linking, and the residual miscellaneous
     * WA Web events.
     */
    private final NotificationDeviceDispatcher deviceHandler;

    /**
     * Holds the sub-dispatcher for {@code w:gp2} notifications that report
     * group membership, subject, description, and settings changes.
     */
    private final NotificationGroupStreamHandler groupHandler;

    /**
     * Constructs the dispatcher and eagerly wires its four sub-dispatchers
     * with their respective dependencies.
     *
     * <p>The socket-stream wiring invokes this constructor once at client
     * construction time; downstream code never instantiates the handler
     * directly. Each sub-dispatcher is created up front so that routing in
     * {@link #handle(Stanza)} reduces to a single switch-and-delegate, and so
     * that all per-family dependency wiring lives in one place.
     *
     * @param whatsapp                     the {@link LinkedWhatsAppClient} shared
     *                                     by every sub-dispatcher
     * @param deviceLinkingService         the {@link CompanionPairingService}
     *                                     that owns the pairing-code
     *                                     handshake state, forwarded to the
     *                                     device sub-dispatcher
     * @param shortcakePairingService      the {@link ShortcakePairingService}
     *                                     that owns the passkey-linking
     *                                     handshake state, forwarded to the
     *                                     device sub-dispatcher
     * @param lidMigrationService          the {@link LidMigrationService}
     *                                     used to reconcile LID and PN
     *                                     addressing during business
     *                                     notifications
     * @param abPropsService               the {@link ABPropsService} used to
     *                                     retrieve feature flags from the
     *                                     device sub-dispatcher
     * @param deviceService                the {@link DeviceService} used to
     *                                     reconcile linked-device state on
     *                                     the device and account
     *                                     sub-dispatchers
     * @param offlineNotificationsReporter the shared
     *                                     {@link OfflineNotificationsReporter}
     *                                     that accumulates per-collection
     *                                     offline {@code server_sync} counts,
     *                                     forwarded to the device
     *                                     sub-dispatcher
     * @param wamService                   the {@link WamService} telemetry
     *                                     sink forwarded to sub-dispatchers
     *                                     that commit notification-driven
     *                                     events
     * @param ackSender                    the {@link AckSender} forwarded to
     *                                     every sub-dispatcher for emitting
     *                                     the per-notification outbound
     *                                     {@code <ack>} stanza
     * @param passkeyAuthenticator         the {@link LinkedWhatsAppClientPasskeyAuthenticator}
     *                                     forwarded to the business
     *                                     sub-dispatcher for answering
     *                                     integrity checkpoints, or
     *                                     {@code null} when none is configured
     */
    public NotificationStreamHandler(
            LinkedWhatsAppClient whatsapp,
            CompanionPairingService deviceLinkingService,
            ShortcakePairingService shortcakePairingService,
            LidMigrationService lidMigrationService,
            ABPropsService abPropsService,
            DeviceService deviceService,
            OfflineNotificationsReporter offlineNotificationsReporter,
            WamService wamService,
            AckSender ackSender,
            LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator
    ) {
        this.accountHandler = new NotificationAccountDispatcher(whatsapp, deviceService, ackSender, wamService);
        this.businessHandler = new NotificationBusinessDispatcher(whatsapp, lidMigrationService, ackSender, passkeyAuthenticator, wamService);
        this.deviceHandler = new NotificationDeviceDispatcher(whatsapp, deviceLinkingService, shortcakePairingService, abPropsService, deviceService, offlineNotificationsReporter, wamService, ackSender);
        this.groupHandler = new NotificationGroupStreamHandler(whatsapp, wamService, ackSender);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the {@code type} attribute of the {@code <notification>}
     * stanza and forwards the stanza to the sub-dispatcher whose family
     * includes that value. Stanzas lacking a {@code type} attribute or
     * carrying an unrecognised value are silently dropped.
     *
     * @implNote
     * This implementation routes {@code w:gp2} through the same switch even
     * though WA Web wires that path through a separate handler; Cobalt
     * consolidates all notification routing here. Unknown {@code type}
     * values are dropped without a NACK because the unrecognised-stanza
     * NACK policy is enforced centrally by {@link NodeStreamService}'s error
     * model.
     */
    @Override
    @WhatsAppWebExport(
            moduleName = "WAWebCommsHandleLoggedInStanza",
            exports = "handleLoggedInStanza",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public void handle(Stanza stanza) {
        var type = stanza.getAttributeAsString("type", null);
        if (type == null) {
            return;
        }

        switch (type) {
            case "account_sync", "contacts", "disappearing_mode", "picture", "privacy_token", "status" -> {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "routing notification type {0} to account dispatcher", type);
                accountHandler.handle(stanza);
            }
            case "business", "digital_commerce_subscription", "fb:update", "mex", "pay" -> {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "routing notification type {0} to business dispatcher", type);
                businessHandler.handle(stanza);
            }
            case "companion_reg_refresh", "crsc_continuation", "devices", "encrypt", "hosted",
                    "link_code_companion_reg", "mediaretry", "newsletter", "passkey_prologue_request",
                    "psa", "registration", "server", "server_sync", "w:growth", "waffle" -> {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "routing notification type {0} to device dispatcher", type);
                deviceHandler.handle(stanza);
            }
            case "w:gp2" -> {
                if (Log.TRACE) LOGGER.log(Level.TRACE, "routing notification type {0} to group handler", type);
                groupHandler.handle(stanza);
            }
            default -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping notification with unrecognized type {0}", type);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fans the reset out to every sub-dispatcher in a fixed order so
     * that their per-connection caches and pending operations are cleared
     * before the next connection starts. Sub-dispatchers are responsible
     * for tolerating being reset while a stanza-handling call is still
     * mid-flight.
     */
    @Override
    public void reset() {
        accountHandler.reset();
        businessHandler.reset();
        deviceHandler.reset();
        groupHandler.reset();
    }
}
