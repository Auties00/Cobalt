package com.github.auties00.cobalt.stream.notification.device;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppIntegrityChallengeException;
import com.github.auties00.cobalt.listener.linked.LinkedNewContactListener;
import com.github.auties00.cobalt.listener.WhatsAppListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.smax.coexistence.SmaxCoexistenceOffboardingNotificationResponse;
import com.github.auties00.cobalt.wire.stanza.smax.coexistence.SmaxCoexistenceOnboardingStatusNotificationResponse;
import com.github.auties00.cobalt.wire.stanza.smax.newsletters.SmaxNewslettersLiveUpdatesNotificationResponse;
import com.github.auties00.cobalt.wire.stanza.smax.newsletters.SmaxNewslettersLiveUpdatesNotificationResponse.NewsletterMessage;
import com.github.auties00.cobalt.pairing.CompanionPairingService;
import com.github.auties00.cobalt.pairing.ShortcakePairingService;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.wire.linked.newsletter.Newsletter;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfoBuilder;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterPollVote;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterReaction;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.wire.linked.sync.action.device.WaffleAccountLinkStateAction;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.ChatMessageCountsEventBuilder;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Handles the companion-linking and per-feature push notification family dispatched by
 * {@link NotificationDeviceDispatcher}.
 *
 * <p>The {@code type} attribute selects the branch. The handler covers the companion pairing-code
 * refresh ({@code link_code_companion_reg}, {@code companion_reg_refresh}), the Meta account-linking
 * {@code waffle} event, the {@code hosted} CTWA coexistence notification, the {@code w:growth}
 * invite notification, the {@code psa} announcement notification, and the {@code newsletter}
 * live-update notification. Every supported stanza is acknowledged at the end of {@link #handle(Stanza)}
 * even when its mutation throws; unsupported types return without side-effects.
 *
 * @implNote This implementation collapses ten WA Web modules into one Cobalt class because they
 * share the same per-stanza ack pattern (read {@code type}, branch, ACK in {@code finally}) and so
 * live more comfortably under one type than under ten near-empty ones.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleCompanionReqRefreshNotification")
@WhatsAppWebModule(moduleName = "WAWebAltDeviceLinkingHandleNotification")
@WhatsAppWebModule(moduleName = "WAWebAccountLinkingNotificationHandler")
@WhatsAppWebModule(moduleName = "WAWebHandleHostedNotification")
@WhatsAppWebModule(moduleName = "WAWebHandleGrowthNotification")
@WhatsAppWebModule(moduleName = "WAWebHandlePsa")
@WhatsAppWebModule(moduleName = "WAWebHandleQPSurfacesNotification")
@WhatsAppWebModule(moduleName = "WAWebHandleQPPrefetchTimestampNotification")
@WhatsAppWebModule(moduleName = "WAWebHandleWaChat")
@WhatsAppWebModule(moduleName = "WAWebHandleNewsletterNotification")
@WhatsAppWebModule(moduleName = "WAWebNewsletterHandleLiveUpdatesNotification")
@WhatsAppWebModule(moduleName = "WAWebNewsletterBackendAddOnsUtils")
@WhatsAppWebModule(moduleName = "WAWebNewsletterMapMsgAndAddOns")
@WhatsAppWebModule(moduleName = "WAWebNewsletterMsgUtils")
@WhatsAppWebModule(moduleName = "WAWebNewsletterExtendedGatingUtils")
final class NotificationLinkingStreamHandler extends SocketStreamHandler.Concurrent {

    /**
     * The logger for {@link NotificationLinkingStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(NotificationLinkingStreamHandler.class);

    /**
     * Holds the notification {@code type} values routed to this handler by the dispatcher.
     *
     * <p>Consulted by {@link #handle(Stanza)} as the first-line filter; any stanza whose type is
     * outside this set returns without side-effects.
     */
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "link_code_companion_reg",
            "companion_reg_refresh",
            "crsc_continuation",
            "passkey_prologue_request",
            "waffle",
            "hosted",
            "w:growth",
            "psa",
            "newsletter"
    );

    /**
     * Holds the {@code notification_metadata.event} integer that WA Web's
     * {@code AccountLinkingNotificationEvent} enum names {@code ACCOUNT_LINKED}.
     */
    @WhatsAppWebExport(moduleName = "WAWebAccountLinkingConstants", exports = "AccountLinkingNotificationEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private static final int WAFFLE_EVENT_ACCOUNT_LINKED = 1;

    /**
     * Holds the {@code notification_metadata.event} integer that WA Web's
     * {@code AccountLinkingNotificationEvent} enum names {@code ACCOUNT_UNLINKED}.
     */
    @WhatsAppWebExport(moduleName = "WAWebAccountLinkingConstants", exports = "AccountLinkingNotificationEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private static final int WAFFLE_EVENT_ACCOUNT_UNLINKED = 2;

    /**
     * Holds the {@code notification_metadata.event} integer that WA Web's
     * {@code AccountLinkingNotificationEvent} enum names {@code STATE_DELETED}.
     */
    @WhatsAppWebExport(moduleName = "WAWebAccountLinkingConstants", exports = "AccountLinkingNotificationEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private static final int WAFFLE_EVENT_STATE_DELETED = 4;

    /**
     * Holds the {@code notification_metadata.event} integer that WA Web's
     * {@code AccountLinkingNotificationEvent} enum names {@code STATE_SUSPENDED}.
     */
    @WhatsAppWebExport(moduleName = "WAWebAccountLinkingConstants", exports = "AccountLinkingNotificationEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private static final int WAFFLE_EVENT_STATE_SUSPENDED = 5;

    /**
     * Holds the {@code notification_metadata.event} integer that WA Web's
     * {@code AccountLinkingNotificationEvent} enum names {@code CLIENT_RESYNC}.
     */
    @WhatsAppWebExport(moduleName = "WAWebAccountLinkingConstants", exports = "AccountLinkingNotificationEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private static final int WAFFLE_EVENT_CLIENT_RESYNC = 6;

    /**
     * Provides store reads, newsletter queries, message queries, and ack sends.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Drives the {@code primary_hello} and {@code refresh_code} steps of the pairing-code handshake
     * on the companion side.
     *
     * <p>May be {@code null} when the local account is not a pairing companion, in which case
     * {@link #handleLinkCodeRefresh(Stanza)} short-circuits the pairing-code branch.
     */
    private final CompanionPairingService deviceLinkingService;

    /**
     * Drives the Shortcake passkey-linking ceremony when the primary device replies.
     *
     * <p>May be {@code null} when the local account is not a passkey-linking companion, in which case
     * {@link #handleShortcakeContinuation(Stanza)} and {@link #handlePasskeyPrologueRequest(Stanza)}
     * short-circuit.
     */
    private final ShortcakePairingService shortcakePairingService;

    /**
     * Commits the {@code ChatMessageCounts} event after a successful invite-driven new-chat
     * creation in {@link #handleGrowth(Stanza)}.
     */
    private final WamService wamService;

    /**
     * Gates the newsletter {@code live_updates} apply behind the
     * {@link ABProp#CHANNEL_REACTIONS_ENABLED} feature flag in {@link #handleNewsletter(Stanza)}.
     */
    private final ABPropsService abPropsService;

    /**
     * Ships the post-processing {@code <ack class="notification">} stanza, reflecting the inbound
     * stanza's {@code type} attribute because this handler covers several notification types.
     */
    private final AckSender ackSender;

    /**
     * Constructs the handler with shared dependencies.
     *
     * <p>Called once by {@link NotificationDeviceDispatcher}.
     *
     * @param whatsapp                the {@link LinkedWhatsAppClient}
     * @param deviceLinkingService    the {@link CompanionPairingService}, may be {@code null} when the local account is not a pairing companion
     * @param shortcakePairingService the {@link ShortcakePairingService}, may be {@code null} when the local account is not a passkey-linking companion
     * @param wamService              the {@link WamService}
     * @param abPropsService          the {@link ABPropsService} used to gate the newsletter {@code live_updates} apply
     * @param ackSender               the {@link AckSender}
     */
    NotificationLinkingStreamHandler(
            LinkedWhatsAppClient whatsapp,
            CompanionPairingService deviceLinkingService,
            ShortcakePairingService shortcakePairingService,
            WamService wamService,
            ABPropsService abPropsService,
            AckSender ackSender
    ) {
        this.whatsapp = whatsapp;
        this.deviceLinkingService = deviceLinkingService;
        this.shortcakePairingService = shortcakePairingService;
        this.wamService = wamService;
        this.abPropsService = abPropsService;
        this.ackSender = ackSender;
    }

    /**
     * Routes the notification to its per-type branch and always sends the protocol-level ACK.
     *
     * <p>Stanzas whose type is outside {@link #SUPPORTED_TYPES} return without side-effects. For a
     * supported stanza the matching branch handler runs inside a {@code try} block, any failure is
     * caught and warning-logged, and the ACK is sent in the {@code finally} block so a valid stanza
     * is always acknowledged even when its mutation throws.
     *
     * @param stanza the incoming {@code <notification>} stanza
     */
    @Override
    public void handle(Stanza stanza) {
        var type = stanza.getAttributeAsString("type", null);
        if (!SUPPORTED_TYPES.contains(type)) {
            return;
        }

        try {
            switch (type) {
                case "link_code_companion_reg", "companion_reg_refresh" -> handleLinkCodeRefresh(stanza);
                case "crsc_continuation" -> handleShortcakeContinuation(stanza);
                case "passkey_prologue_request" -> handlePasskeyPrologueRequest(stanza);
                case "waffle" -> handleWaffle(stanza);
                case "hosted" -> handleHosted(stanza);
                case "w:growth" -> handleGrowth(stanza);
                case "psa" -> handlePsa(stanza);
                case "newsletter" -> handleNewsletter(stanza);
                default -> {
                }
            }
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "cannot handle notification type=" + type + " id=" + stanza.getAttributeAsString("id", "<missing>"),
                        throwable);
            }
        } finally {
            sendNotificationAck(stanza);
        }
    }

    /**
     * Handles the companion-registration refresh and pairing-code handshake notifications.
     *
     * <p>The {@code companion_reg_refresh} branch generates a fresh 32-byte ADV secret on the
     * companion side and returns. The {@code link_code_companion_reg} branch reads the {@code stage}
     * attribute of the {@code <link_code_companion_reg>} child and drives the pairing-code handshake
     * step: {@code primary_hello} ships the finish IQ via
     * {@link CompanionPairingService#handlePrimaryHello(byte[], byte[], byte[])} once the wrapped
     * primary ephemeral public key, primary identity public key, and pairing ref are all present;
     * {@code refresh_code} updates the cached ref via
     * {@link CompanionPairingService#handleRefreshCode(byte[])}. Any other stage is debug-logged and
     * ignored, and the branch returns early when no pairing service, no child, or no stage is
     * available.
     *
     * @param stanza the notification stanza
     */
    private void handleLinkCodeRefresh(Stanza stanza) {
        if (stanza.hasAttribute("type", "companion_reg_refresh")) {
            whatsapp.store().signalStore().setAdvSecretKey(DataUtils.randomByteArray(32));
            return;
        }

        if (deviceLinkingService == null) {
            return;
        }

        var child = stanza.getChild("link_code_companion_reg").orElse(null);
        if (child == null) {
            return;
        }

        var stage = child.getAttributeAsString("stage", null);
        if (stage == null) {
            return;
        }

        var ref = child.getChild("link_code_pairing_ref")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);

        switch (stage) {
            case "primary_hello" -> {
                var wrappedPrimaryEphemeralPub = child.getChild("link_code_pairing_wrapped_primary_ephemeral_pub")
                        .flatMap(Stanza::toContentBytes)
                        .orElse(null);
                var primaryIdentityPublic = child.getChild("primary_identity_pub")
                        .flatMap(Stanza::toContentBytes)
                        .orElse(null);
                if (wrappedPrimaryEphemeralPub == null || primaryIdentityPublic == null || ref == null) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "rejecting primary_hello notification, missing required fields");
                    return;
                }
                try {
                    deviceLinkingService.handlePrimaryHello(wrappedPrimaryEphemeralPub, primaryIdentityPublic, ref);
                } catch (Throwable throwable) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot complete alt-device-linking handshake", throwable);
                }
            }
            case "refresh_code" -> deviceLinkingService.handleRefreshCode(ref);
            default -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring link_code_companion_reg notification, stage={0}", stage);
            }
        }
    }

    /**
     * Continues the Shortcake passkey ceremony with the primary device's ephemeral identity.
     *
     * <p>Reads the {@code <primary_ephemeral_identity>} child and hands its bytes to
     * {@link ShortcakePairingService#handlePrimaryEphemeralIdentity(byte[])}. Returns early when no
     * passkey-linking companion is configured or the child is absent.
     *
     * @param stanza the notification stanza
     */
    private void handleShortcakeContinuation(Stanza stanza) {
        if (shortcakePairingService == null) {
            return;
        }
        var primaryEphemeralIdentity = stanza.getChild("primary_ephemeral_identity")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (primaryEphemeralIdentity == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "rejecting crsc_continuation notification, missing primary_ephemeral_identity");
            return;
        }
        try {
            shortcakePairingService.handlePrimaryEphemeralIdentity(primaryEphemeralIdentity);
        } catch (Throwable throwable) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot complete shortcake passkey linking", throwable);
        }
    }

    /**
     * Starts the Shortcake passkey ceremony when the server prompts for it.
     *
     * <p>The ceremony is idempotent, so this is a no-op when it has already been started from the
     * {@code pair-device} IQ. When no passkey authenticator is configured the prompt cannot be
     * answered, so the failure is routed to {@link LinkedWhatsAppClient#handleFailure} as a
     * {@link WhatsAppIntegrityChallengeException}; its default recovery logs the session out, which an
     * embedder that wants to keep an in-progress QR or pairing-code link can override to
     * {@link com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult#DISCARD}.
     *
     * @param stanza the notification stanza
     */
    private void handlePasskeyPrologueRequest(Stanza stanza) {
        if (shortcakePairingService == null || !shortcakePairingService.isEnabled()) {
            whatsapp.handleFailure(new WhatsAppIntegrityChallengeException(
                    "Server requested passkey companion linking but no passkey authenticator is configured"));
            return;
        }
        try {
            shortcakePairingService.start();
        } catch (Throwable throwable) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "cannot start shortcake passkey linking", throwable);
        }
    }

    /**
     * Applies the parsed {@code waffle} Meta account-linking event to the local linked-account
     * state.
     *
     * <p>Reads the {@code event} integer and the {@code client_resync} flag from the
     * {@code <notification_metadata>} child and transitions
     * {@link WaffleAccountLinkStateAction.AccountLinkState} accordingly:
     * {@link #WAFFLE_EVENT_STATE_SUSPENDED} maps to {@code PAUSED},
     * {@link #WAFFLE_EVENT_STATE_DELETED} maps to {@code UNLINKED}, and
     * {@link #WAFFLE_EVENT_CLIENT_RESYNC} maps to {@code ACTIVE}.
     * {@link #WAFFLE_EVENT_ACCOUNT_LINKED} and {@link #WAFFLE_EVENT_ACCOUNT_UNLINKED} transition to
     * {@code ACTIVE} only when the {@code client_resync} attribute is {@code "true"}, matching WA
     * Web which only re-runs the resync-state handler on the resync hint. A stanza with no
     * {@code <notification_metadata>} child, and any unhandled event integer, is debug-logged and
     * ignored.
     *
     * @param stanza the notification stanza
     */
    private void handleWaffle(Stanza stanza) {
        var metadata = stanza.getChild("notification_metadata").orElse(null);
        if (metadata == null) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "ignoring waffle notification, no notification_metadata id={0}",
                        stanza.getAttributeAsString("id", "<missing>"));
            }
            return;
        }

        var event = metadata.getAttributeAsInt("event", -1);
        var clientResync = "true".equals(metadata.getAttributeAsString("client_resync", null));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "waffle notification event={0} clientResync={1}", event, clientResync);

        switch (event) {
            case WAFFLE_EVENT_STATE_SUSPENDED ->
                    whatsapp.store().accountStore().setLinkedMetaAccountState(WaffleAccountLinkStateAction.AccountLinkState.PAUSED);
            case WAFFLE_EVENT_STATE_DELETED ->
                    whatsapp.store().accountStore().setLinkedMetaAccountState(WaffleAccountLinkStateAction.AccountLinkState.UNLINKED);
            case WAFFLE_EVENT_CLIENT_RESYNC ->
                    whatsapp.store().accountStore().setLinkedMetaAccountState(WaffleAccountLinkStateAction.AccountLinkState.ACTIVE);
            case WAFFLE_EVENT_ACCOUNT_UNLINKED -> {
                if (clientResync) {
                    whatsapp.store().accountStore().setLinkedMetaAccountState(WaffleAccountLinkStateAction.AccountLinkState.ACTIVE);
                }
            }
            case WAFFLE_EVENT_ACCOUNT_LINKED -> {
                if (clientResync) {
                    whatsapp.store().accountStore().setLinkedMetaAccountState(WaffleAccountLinkStateAction.AccountLinkState.ACTIVE);
                }
            }
            default -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring unhandled waffle notification event {0}", event);
            }
        }
    }

    /**
     * Applies the parsed {@code hosted} CTWA coexistence notification to the local hosted-automation
     * flags.
     *
     * <p>An {@code <onboarding_status status="completed" product_surface="automation"/>} child
     * marks hosted automation as onboarded and enables detected outcomes; an
     * {@code <offboarding product_surface="automation"/>} child clears both flags. Any other child
     * is debug-logged and ignored.
     *
     * @implNote This implementation routes both child cases through the typed SMAX parsers
     * ({@link SmaxCoexistenceOnboardingStatusNotificationResponse},
     * {@link SmaxCoexistenceOffboardingNotificationResponse}) so the SMAX exports remain the single
     * source of truth for envelope and field validation. WA Web throws {@code SmaxParsingFailure} on
     * an unsupported child; Cobalt debug-logs and lets the outer ACK fire.
     *
     * @param stanza the notification stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleHostedNotification",
            exports = "handleHostedNotification",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleHosted(Stanza stanza) {
        if (stanza.hasChild("onboarding_status")) {
            var onboardingStatus = SmaxCoexistenceOnboardingStatusNotificationResponse.of(stanza).orElse(null);
            if (onboardingStatus != null
                    && "completed".equals(onboardingStatus.onboardingStatusStatus())
                    && "automation".equals(onboardingStatus.onboardingStatusProductSurface())) {
                whatsapp.store().businessStore().setHostedAutomationOnboarded(true);
                whatsapp.store().businessStore().setDetectedOutcomesEnabled(true);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "hosted automation onboarding completed");
            }
            return;
        }

        if (stanza.hasChild("offboarding")) {
            var offboarding = SmaxCoexistenceOffboardingNotificationResponse.of(stanza).orElse(null);
            if (offboarding != null && "automation".equals(offboarding.offboardingProductSurface())) {
                whatsapp.store().businessStore().setHostedAutomationOnboarded(false);
                whatsapp.store().businessStore().setDetectedOutcomesEnabled(false);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "hosted automation offboarded");
            }
            return;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "ignoring unsupported hosted notification child={0}",
                    firstChildDescription(stanza));
        }
    }

    /**
     * Materialises a contact and a chat for the invite recipient carried by a {@code w:growth}
     * notification.
     *
     * <p>Resolves the recipient JID from {@code <invite><receiver user="..."/></invite>} and
     * returns early when it is absent. An existing contact and chat are reused; otherwise a new
     * contact and chat are created. The contact's chosen name is refreshed from a name query (best
     * effort; failures are debug-logged), and {@link LinkedNewContactListener#onNewContact} fires when
     * the contact did not previously exist.
     *
     * @implNote This implementation commits a {@code ChatMessageCounts} WAM event with
     * {@code isInviteCreatedThread=true} when a new chat was created, matching WA Web's commit after
     * its {@code handleSingleMsg} call. Unlike WA Web, Cobalt does not synthesise the
     * {@code sender_invite} stub message because Cobalt's message-generation pipeline runs from a
     * different stream.
     *
     * @param stanza the notification stanza
     */
    private void handleGrowth(Stanza stanza) {
        var receiver = stanza.getChild("invite")
                .flatMap(invite -> invite.getChild("receiver"))
                .flatMap(receiverNode -> receiverNode.getAttributeAsJid("user"))
                .map(Jid::toUserJid)
                .orElse(null);
        if (receiver == null) {
            return;
        }

        var existed = whatsapp.store().contactStore().findContactByJid(receiver).isPresent();
        var contact = whatsapp.store().contactStore().findContactByJid(receiver)
                .orElseGet(() -> whatsapp.store().contactStore().addNewContact(receiver));
        var chatCreated = whatsapp.store().chatStore().findChatByJid(receiver).isEmpty();
        whatsapp.store().chatStore().findChatByJid(receiver)
                .orElseGet(() -> whatsapp.store().chatStore().addNewChat(receiver));
        try {
            whatsapp.queryName(receiver).ifPresent(contact::setChosenName);
        } catch (Throwable throwable) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "cannot refresh invited contact metadata for " + new LogRedactable.User(String.valueOf(receiver)),
                        throwable);
            }
        }
        whatsapp.store().contactStore().addContact(contact);

        if (!existed) {
            fireListeners(LinkedNewContactListener.class, listener -> listener.onNewContact(whatsapp, contact));
        }

        if (chatCreated) {
            wamService.commit(new ChatMessageCountsEventBuilder()
                    .isInviteCreatedThread(true)
                    .build());
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "growth invite processed receiver={0} newContact={1} newChat={2}",
                    receiver, !existed, chatCreated);
        }
    }

    /**
     * Logs and drops {@code psa} notifications because Cobalt has no local quick-promotion or in-app
     * PSA campaign pipeline.
     *
     * <p>When the {@code from} attribute is the announcements account, the first child tag selects a
     * sub-case ({@code surfaces} for QP surfaces, {@code reset_smb_last_qp_prefetch_timestamp} for
     * the QP prefetch timestamp reset, or any other tag for the PSA campaign and wa_chat path); each
     * sub-case is debug-logged and dropped. Stanzas from any other sender are also debug-logged and
     * dropped.
     *
     * @implNote This implementation never produces a side-effect because none of the three sub-cases
     * (QP surfaces, QP prefetch timestamp, wa_chat) have a Cobalt store equivalent.
     *
     * @param stanza the notification stanza
     */
    private void handlePsa(Stanza stanza) {
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null || !from.equals(Jid.announcementsAccount())) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "ignoring psa notification, unexpected sender child={0}",
                        firstChildDescription(stanza));
            }
        }

        var firstChild = stanza.getChild().orElse(null);
        var firstChildTag = firstChild == null ? null : firstChild.description();
        switch (firstChildTag) {
            case "surfaces" -> {
                // TODO: implement the quick-promotion surfaces pipeline once Cobalt has the in-app QP model.
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "ignoring qp surfaces psa notification {0}",
                            stanza.getAttributeAsString("id", "<missing>"));
                }
            }
            case "reset_smb_last_qp_prefetch_timestamp" -> {
                // TODO: implement the QP prefetch timestamp reset once Cobalt has the in-app QP model.
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "ignoring qp prefetch timestamp psa notification {0}",
                            stanza.getAttributeAsString("id", "<missing>"));
                }
            }
            case null, default -> {
                // TODO: implement the in-app PSA campaign / wa_chat message pipeline.
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring wa_chat/psa notification from psa_jid, child={0}", firstChildTag);
            }
        }
    }

    /**
     * Applies a newsletter {@code live_updates} delta to the local store when a {@code newsletter}
     * notification carries the {@code <live_updates>} child.
     *
     * <p>Returns early when the {@code from} JID is absent or is not a newsletter-server JID, or when
     * the notification carries no {@code <live_updates>} child. The whole delta is dropped (and only
     * the protocol ACK fires) when the {@link ABProp#CHANNEL_REACTIONS_ENABLED} feature flag is off,
     * matching WA Web's {@code isNewsletterReactionEnabled} gate. Otherwise the
     * {@code <live_updates><messages>} envelope is parsed through
     * {@link SmaxNewslettersLiveUpdatesNotificationResponse} and every {@code <message>} is projected
     * onto the newsletter's stored messages: content posts are inserted or updated, admin edits mutate
     * an existing message only when strictly newer, admin revokes remove the stored message, and the
     * add-on children (reactions, poll votes, view/forward/response counts) are reconciled against the
     * resolved message. The notification's own {@code from} and {@code t} attributes drive the apply,
     * not the {@code <messages>} echo. The protocol ACK is always emitted by {@link #handle(Stanza)};
     * this method never re-queries the newsletter.
     *
     * @implNote This implementation folds WA Web's two write paths (the {@code updateAddOnDbRecords}
     * IndexedDB write and the {@code frontendFireAndForget("updateNewsletterMessages")} model push)
     * into a single pass over Cobalt's unified newsletter store. It processes synchronously on the
     * calling virtual thread, whereas WA Web serialises through {@code WAWebNewsletterNotificationQueue};
     * the per-newsletter store merge is serial and idempotent, so the queue is not required. Message
     * resolution keys on {@code server_id}, matching WA Web's {@code getMessageByServerId}.
     *
     * @param stanza the notification stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterHandleLiveUpdatesNotification",
            exports = "handleNewsletterLiveUpdatesNotification",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleNewsletter(Stanza stanza) {
        var newsletterJid = stanza.getAttributeAsJid("from").orElse(null);
        if (newsletterJid == null || !newsletterJid.hasNewsletterServer()) {
            return;
        }

        if (!stanza.hasChild("live_updates")) {
            return;
        }

        if (!abPropsService.getBool(ABProp.CHANNEL_REACTIONS_ENABLED)) {
            return;
        }

        var response = SmaxNewslettersLiveUpdatesNotificationResponse.of(stanza).orElse(null);
        if (response == null) {
            return;
        }

        var notificationTimestamp = stanza.getAttributeAsLong("t");
        if (notificationTimestamp.isEmpty() || notificationTimestamp.getAsLong() < 0) {
            return;
        }
        var updateTimestamp = Instant.ofEpochSecond(notificationTimestamp.getAsLong());

        var newsletter = ensureNewsletter(newsletterJid);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "applying newsletter live_updates, newsletter={0} messages={1}",
                    newsletterJid, response.messages().size());
        }
        for (var message : response.messages()) {
            applyLiveUpdate(newsletter, newsletterJid, message, updateTimestamp);
        }
    }

    /**
     * Projects a single parsed {@link NewsletterMessage} onto the newsletter's stored messages.
     *
     * <p>Content resolution follows WA Web: an admin revoke removes the stored message (and drops its
     * add-ons); an admin edit mutates an existing message only, keyed by {@code server_id}; a content
     * post is inserted or updated; and an add-on-only update resolves the existing message and drops
     * the add-ons when the message is unknown. When a message is resolved, its add-ons are reconciled
     * and the mutated message is re-stored.
     *
     * @implNote This implementation never synthesises a stub for an unresolvable add-on-only update or
     * revoke, matching WA Web which only writes add-ons against a message it can resolve.
     *
     * @param newsletter    the owning newsletter
     * @param newsletterJid the newsletter JID used for the message key
     * @param message       the parsed message delta
     * @param updateTimestamp the notification timestamp driving the apply
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterMapMsgAndAddOns", exports = "mapMsgAndAddOns",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void applyLiveUpdate(Newsletter newsletter, Jid newsletterJid, NewsletterMessage message, Instant updateTimestamp) {
        var serverIdKey = String.valueOf(message.serverId());

        if (message.isRevoke()) {
            if (newsletter.getMessageById(serverIdKey).isPresent()) {
                newsletter.removeMessage(serverIdKey);
            }
            return;
        }

        NewsletterMessageInfo info;
        if (message.isEdit()) {
            info = newsletter.getMessageById(serverIdKey).orElse(null);
            if (info == null) {
                return;
            }
            applyEdit(info, message);
        } else if (message.hasContent()) {
            info = applyContent(newsletter, newsletterJid, message, serverIdKey);
            if (info == null) {
                info = newsletter.getMessageById(serverIdKey).orElse(null);
                if (info == null) {
                    return;
                }
            }
        } else {
            info = newsletter.getMessageById(serverIdKey).orElse(null);
            if (info == null) {
                return;
            }
        }

        applyAddOns(info, message, updateTimestamp);
        newsletter.addMessage(info);
    }

    /**
     * Inserts or updates a content post's {@link NewsletterMessageInfo}.
     *
     * <p>Decodes the {@code <plaintext>} payload into a {@link LinkedMessageContainer}; returns {@code null}
     * when the payload is absent (a WAMO placeholder) or fails to decode, so the caller can fall back
     * to add-on-only resolution. An existing message with the same {@code server_id} has its content
     * and timestamp refreshed in place, preserving its stored add-ons; otherwise a fresh
     * {@link NewsletterMessageInfo} is built with {@link MessageStatus#DELIVERED}, mirroring the
     * newsletter receive path.
     *
     * @param newsletter    the owning newsletter
     * @param newsletterJid the newsletter JID used for the message key
     * @param message       the parsed content message
     * @param serverIdKey   the stringified {@code server_id} used for store lookup
     * @return the inserted or updated message, or {@code null} when the payload is absent or undecodable
     */
    private NewsletterMessageInfo applyContent(Newsletter newsletter, Jid newsletterJid, NewsletterMessage message, String serverIdKey) {
        var plaintext = message.plaintext().orElse(null);
        if (plaintext == null) {
            return null;
        }
        var container = decodeContainer(plaintext);
        if (container == null) {
            return null;
        }

        var existing = newsletter.getMessageById(serverIdKey).orElse(null);
        if (existing != null) {
            existing.setMessage(container);
            message.timestamp().ifPresent(seconds -> existing.setTimestamp(Instant.ofEpochSecond(seconds)));
            return existing;
        }

        var key = new MessageKeyBuilder()
                .id(message.stanzaId().orElse(null))
                .parentJid(newsletterJid)
                .fromMe(message.fromSelf())
                .build();
        return new NewsletterMessageInfoBuilder()
                .key(key)
                .serverId((int) message.serverId())
                .timestamp(message.timestamp().map(Instant::ofEpochSecond).orElse(null))
                .message(container)
                .status(MessageStatus.DELIVERED)
                .build();
    }

    /**
     * Applies an admin edit to an existing message.
     *
     * <p>The edit lands only when its effective timestamp ({@code original_msg_t}, falling back to the
     * message {@code t}) is strictly greater than the stored message's timestamp, replicating WA Web's
     * last-writer-wins guard. When it lands, the decoded payload replaces the message content, the
     * sender-side edit timestamp is recorded in milliseconds, and the original send timestamp is set
     * to the effective timestamp.
     *
     * @param info    the stored message to edit
     * @param message the parsed edit message
     */
    private void applyEdit(NewsletterMessageInfo info, NewsletterMessage message) {
        var original = message.originalMessageTimestamp();
        Long incomingSeconds;
        if (original.isPresent()) {
            incomingSeconds = original.getAsLong();
        } else {
            incomingSeconds = message.timestamp().orElse(null);
        }
        if (incomingSeconds == null) {
            return;
        }
        var existingSeconds = info.timestamp().map(Instant::getEpochSecond).orElse(Long.MIN_VALUE);
        if (incomingSeconds <= existingSeconds) {
            return;
        }

        var container = message.plaintext().map(this::decodeContainer).orElse(null);
        if (container != null) {
            info.setMessage(container);
        }
        message.editTimestampMs().ifPresent(editMs -> info.setLatestEditSenderTimestampMs(editMs));
        info.setOriginalTimestamp(Instant.ofEpochSecond(incomingSeconds));
    }

    /**
     * Reconciles a resolved message's add-on state against a parsed delta.
     *
     * <p>Reactions are replaced from the {@code emoji -> count} tally when the delta carries a
     * non-empty {@code <reactions>} element and the notification timestamp is newer than the stored
     * server-update timestamp; an absent or empty element clears them unconditionally. Poll votes are
     * replaced only when the delta carries them. The stored server-update timestamp is advanced to the
     * notification timestamp; the view count is overwritten only when present (preserved otherwise),
     * while the forward and response counts are overwritten to zero when absent, replicating WA Web's
     * count asymmetry.
     *
     * @implNote This implementation uses the message's prior
     * {@link NewsletterMessageInfo#lastUpdateFromServerTimestamp()} as the reaction last-writer-wins
     * clock, captured before it is advanced by the count step, because Cobalt stores reactions on the
     * message rather than in WA Web's separate reaction table.
     *
     * @param info            the resolved message to reconcile
     * @param message         the parsed message delta
     * @param updateTimestamp the notification timestamp driving the apply
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterBackendAddOnsUtils", exports = "updateAddOnDbRecords",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void applyAddOns(NewsletterMessageInfo info, NewsletterMessage message, Instant updateTimestamp) {
        var previousUpdate = info.lastUpdateFromServerTimestamp().orElse(null);

        var reactions = message.reactions().orElse(null);
        if (reactions != null && !reactions.isEmpty()) {
            if (previousUpdate == null || updateTimestamp.isAfter(previousUpdate)) {
                var tallies = reactions.entrySet()
                        .stream()
                        .map(entry -> new NewsletterReaction(entry.getKey(), entry.getValue(), false))
                        .toList();
                info.setReactions(tallies);
            }
        } else {
            info.setReactions(List.of());
        }

        var votes = message.pollVotes();
        if (!votes.isEmpty()) {
            var tallies = votes.stream()
                    .map(vote -> new NewsletterPollVote(vote.hash(), vote.count()))
                    .toList();
            info.setPollVotes(tallies);
        }

        info.setLastUpdateFromServerTimestamp(updateTimestamp);
        message.viewsCount().ifPresent(views -> info.setViews(views));
        info.setForwardsCount(message.forwardsCount().orElse(0));
        info.setQuestionResponsesCount(message.responsesCount().orElse(0));
    }

    /**
     * Decodes a newsletter {@code <plaintext>} payload into a {@link LinkedMessageContainer}.
     *
     * <p>Returns {@code null} rather than throwing when the payload cannot be parsed, matching the
     * newsletter receive path which treats an undecodable payload as a silent drop.
     *
     * @param plaintext the raw protobuf payload bytes
     * @return the decoded container, or {@code null} when the payload cannot be parsed
     */
    private LinkedMessageContainer decodeContainer(byte[] plaintext) {
        try {
            return LinkedMessageContainerSpec.decode(plaintext);
        } catch (Throwable throwable) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cannot decode newsletter live-update payload", throwable);
            return null;
        }
    }

    /**
     * Returns the description (tag name) of the first child, or {@code null} when the stanza has no
     * children.
     *
     * <p>Used by {@link #handleHosted(Stanza)} and {@link #handlePsa(Stanza)} to log the unsupported
     * child without pulling its full content.
     *
     * @param stanza the parent stanza
     * @return the first child's description, or {@code null}
     */
    private String firstChildDescription(Stanza stanza) {
        return stanza.getChild().map(Stanza::description).orElse(null);
    }

    /**
     * Returns the newsletter for the given JID, creating a blank record when none exists.
     *
     * <p>Used only by {@link #handleNewsletter(Stanza)} to obtain the target of a {@code live_updates}
     * apply.
     *
     * @param newsletterJid the newsletter JID
     * @return the matching {@link Newsletter}
     */
    private Newsletter ensureNewsletter(Jid newsletterJid) {
        return whatsapp.store().chatStore().findNewsletterByJid(newsletterJid)
                .orElseGet(() -> whatsapp.store().chatStore().addNewNewsletter(newsletterJid));
    }

    /**
     * Fans the given callback out to every registered listener of the given
     * type on its own virtual thread.
     *
     * <p>Used only by {@link #handleGrowth(Stanza)} for the {@code onNewContact} listener fan-out.
     *
     * @param type     the per-event listener interface to dispatch against
     * @param consumer the callback to invoke against each matching listener
     * @param <L>      the per-event listener interface
     */
    private <L extends WhatsAppListener> void fireListeners(Class<L> type, Consumer<L> consumer) {
        for (var listener : whatsapp.store().listeners()) {
            if (type.isInstance(listener)) {
                var typed = type.cast(listener);
                Thread.startVirtualThread(() -> consumer.accept(typed));
            }
        }
    }

    /**
     * Sends the protocol-level ACK for the processed notification.
     *
     * <p>The ACK reflects the {@code type} attribute from the original stanza because this handler
     * covers several different notification types.
     *
     * @param stanza the original {@code <notification>} stanza
     */
    private void sendNotificationAck(Stanza stanza) {
        ackSender.sendAck(AckClass.NOTIFICATION, stanza);
    }
}
