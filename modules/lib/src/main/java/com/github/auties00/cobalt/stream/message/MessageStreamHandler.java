package com.github.auties00.cobalt.stream.message;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.sync.LiveWebHistorySyncService;

import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.ack.NackReason;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedMessageReplyListener;
import com.github.auties00.cobalt.listener.MessageStatusListener;
import com.github.auties00.cobalt.listener.NewMessageListener;
import com.github.auties00.cobalt.listener.linked.LinkedNewStatusListener;
import com.github.auties00.cobalt.util.BufferedProtobufInputStream;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.media.MediaConnectionService;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.MessageService;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.message.receipt.MessageReceiptHandler;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveStanza;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveStanzaParser;
import com.github.auties00.cobalt.wire.linked.business.BusinessHostStorageType;
import com.github.auties00.cobalt.wire.linked.contact.Contact;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.wire.linked.tos.TosNotice;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.tos.TosService;
import com.github.auties00.cobalt.wire.wam.event.GatedMessageReceivedEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ChatGatedReason;
import com.github.auties00.cobalt.quarantine.QuarantineService;
import com.github.auties00.cobalt.message.send.id.MessageIdGenerator;
import com.github.auties00.cobalt.message.send.id.MessageIdVersion;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncPayload;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncPayloadSpec;
import com.github.auties00.cobalt.wire.linked.message.*;
import com.github.auties00.cobalt.wire.core.message.*;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.*;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.system.peer.PeerDataOperationRequestType;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.text.CommentMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.commerce.OrderMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.payment.PaymentInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.payment.RequestPaymentMessage;
import com.github.auties00.cobalt.wire.linked.message.payment.SendPaymentMessage;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.wire.linked.payment.OrphanPaymentNotificationBuilder;
import com.github.auties00.cobalt.wire.linked.payment.PaymentInfo;
import com.github.auties00.cobalt.wire.linked.payment.PaymentInfoBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdSnapshotRecovery;
import com.github.auties00.cobalt.sync.SnapshotRecoveryService;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.sync.WebHistorySyncService;
import com.github.auties00.cobalt.sync.key.SyncKeyRotationService;
import com.github.auties00.cobalt.wam.WamMsgUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.IncomingMessageDropEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MdBadDeviceSentMessageEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MessageHighRetryCountEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MessageReceiveEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.NonMessagePeerDataOperationResponseEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.OfflineCountTooHighEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.BusinessTemplateRichOrderStatusEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PlaceholderActivityEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsRichOrderStatusMessageInconsistentPayloadReceivedEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StructuredMessageBuyerReceiveEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StructuredMessageReceiveEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.UnknownStanzaEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcMessageProcessingPerfEventBuilder;
import com.github.auties00.cobalt.wam.threadlogging.ThreadLoggingActivity;
import com.github.auties00.cobalt.wam.threadlogging.ThreadLoggingMessages;
import com.github.auties00.cobalt.wire.wam.type.BizPlatform;
import com.github.auties00.cobalt.wire.wam.type.ChatsFolderType;
import com.github.auties00.cobalt.wire.wam.type.ContactType;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderAction;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderChatType;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderReasonType;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderType;
import com.github.auties00.cobalt.wire.wam.type.StructuredMessageClass;
import com.github.auties00.cobalt.wire.wam.type.PeerDataRequestType;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.BotType;
import com.github.auties00.cobalt.wire.wam.type.ChatOriginsType;
import com.github.auties00.cobalt.wire.wam.type.DeviceType;
import com.github.auties00.cobalt.wire.wam.type.DisappearingChatInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.DsmError;
import com.github.auties00.cobalt.wire.wam.type.E2eCiphertextType;
import com.github.auties00.cobalt.wire.wam.type.E2eDestination;
import com.github.auties00.cobalt.wire.wam.type.EditType;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityTriggerActionType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageDropReasonType;
import com.github.auties00.cobalt.wire.wam.type.RevokeType;
import com.github.auties00.cobalt.wire.wam.type.StanzaType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatDisappearingMode;

import java.io.ByteArrayInputStream;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Dispatches inbound {@code <message>} stanzas pushed by the WhatsApp
 * server to the parse, decrypt, persist, notify, and receipt pipeline.
 *
 * <p>Each stanza is routed along one of three branches: {@code medianotify}
 * stanzas carry no end-to-end payload and return after the transport ack;
 * stanzas whose {@code from} JID lives on a newsletter server are forwarded
 * to {@link #handleNewsletterMessage(Stanza)} for plaintext processing without
 * a delivery receipt; every other stanza is parsed by
 * {@link MessageReceiveStanzaParser}, processed by {@link MessageService},
 * then optionally enriched with a delivery receipt or bot-invoke ack through
 * the {@link MessageReceiptHandler}.
 *
 * <p>Every inbound {@code <message>} receives an unconditional
 * {@code <ack class="message">} via {@link AckSender#sendAck} before any
 * branch decision is made; that ack is the transport-level acknowledgement
 * the WhatsApp relay requires for every delivered stanza. Delivery, retry,
 * NACK, and bot-invoke receipts layered on top are higher-level state, not
 * transport acks.
 *
 * <p>Successfully processed stanzas additionally trigger protocol-message
 * handling (key share, key request, snapshot recovery, LID migration
 * mapping sync, history sync, security-notification setting sync),
 * orphan-payment reconciliation, and a fan-out to every registered
 * listener. The handler is wired into the socket stream at client
 * construction and receives stanzas through {@link #handle(Stanza)}; it is
 * not invoked directly by application code.
 *
 * @implNote
 * This implementation collapses WA Web's split between {@code WAWebHandleMsg},
 * {@code WAWebCommsHandleMessagingStanza}, and
 * {@code WAWebCommsHandleWorkerCompatibleStanza} into a single class because
 * Cobalt has no equivalent of WA Web's worker-vs-main split. WAM emission
 * helpers are colocated with the dispatch path that triggers each event so
 * that the receipt and metric are committed atomically with the processing
 * outcome.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsg")
@WhatsAppWebModule(moduleName = "WAWebCommsHandleMessagingStanza")
@WhatsAppWebModule(moduleName = "WAWebCommsHandleWorkerCompatibleStanza")
public final class MessageStreamHandler extends SocketStreamHandler.Ordered {
    /** The logger for {@link MessageStreamHandler}. */
    private static final System.Logger LOGGER = Log.get(MessageStreamHandler.class);

    /**
     * Retry-count threshold (inclusive) that triggers the
     * {@code MessageHighRetryCount} WAM metric.
     *
     * <p>The metric is committed only when the post-increment retry attempt
     * is greater than or equal to this value, so it fires on the fifth retry
     * attempt and beyond, never on the first four.
     *
     * @implNote
     * This implementation matches the private module-level constant
     * {@code WAWebPostMessageHighRetryCountMetric.MAX_RETRY = 5} bit-for-bit.
     */
    @WhatsAppWebExport(moduleName = "WAWebPostMessageHighRetryCountMetric",
            exports = "MAX_RETRY", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int MAX_MESSAGE_RETRY_COUNT = 5;

    /**
     * Threshold (inclusive) on the parsed stanza {@code offline} attribute
     * above which an {@code OfflineCountTooHigh} WAM metric is committed.
     *
     * <p>The metric is committed when the integer-parsed {@code offline}
     * value is greater than or equal to this threshold.
     *
     * @implNote
     * This implementation matches the private module-level constant
     * {@code WAWebMaybePostOfflineCountTooHighMetric.OFFLINE_COUNT_TOO_HIGH_THRESHOLD = 11}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMaybePostOfflineCountTooHighMetric",
            exports = "OFFLINE_COUNT_TOO_HIGH_THRESHOLD", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int OFFLINE_COUNT_TOO_HIGH_THRESHOLD = 11;

    /**
     * The native-flow button name marking an interactive order-details card.
     *
     * @implNote
     * This implementation matches
     * {@code WAWebInteractiveMessagesNativeFlowName.ORDER_DETAILS}.
     */
    @WhatsAppWebExport(moduleName = "WAWebInteractiveMessagesNativeFlowName",
            exports = "ORDER_DETAILS", adaptation = WhatsAppAdaptation.DIRECT)
    private static final String ORDER_DETAILS_FLOW = "order_details";

    /**
     * The native-flow button name marking an interactive rich-order-status card.
     *
     * @implNote
     * This implementation matches
     * {@code WAWebInteractiveMessagesNativeFlowName.ORDER_STATUS}.
     */
    @WhatsAppWebExport(moduleName = "WAWebInteractiveMessagesNativeFlowName",
            exports = "ORDER_STATUS", adaptation = WhatsAppAdaptation.DIRECT)
    private static final String ORDER_STATUS_FLOW = "order_status";

    /**
     * Owning {@link LinkedWhatsAppClient} used to send acknowledgments and
     * receipts, access the store, dispatch listener callbacks, and ship peer
     * messages from protocol-message key-request handling.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Parses and decrypts the inbound stanza into the typed
     * {@link LinkedMessageInfo} consumed by Cobalt downstream.
     */
    private final MessageService messageService;

    /**
     * Ships delivery, retry, NACK, and bot-ack receipts back to the server
     * based on the outcome of {@link MessageService#process(Stanza)}.
     */
    private final MessageReceiptHandler receiptHandler;

    /**
     * Consulted from {@link #resolveSnapshotRecovery} when a
     * peer-data-operation response carries a
     * {@code COMPANION_SYNCD_SNAPSHOT_FATAL_RECOVERY} payload.
     */
    private final SnapshotRecoveryService snapshotRecoveryService;

    /**
     * Consulted from {@link #processAppStateSyncKeyShare} and
     * {@link #processAppStateSyncKeyRequest} for app-state-sync key material
     * exchange.
     */
    private final SyncKeyRotationService syncKeyRotationService;

    /**
     * Consulted from {@link #handleProtocolMessage(ChatMessageInfo)} for
     * inbound LID migration mapping sync payloads.
     */
    private final LidMigrationService lidMigrationService;

    /**
     * Consulted from {@link #handleProtocolMessage(ChatMessageInfo)} for
     * inbound {@code HistorySyncNotification} payloads carried as protocol
     * messages.
     *
     * <p>Downloads, decrypts, and decodes the announced history chunk and
     * fans the decoded chunks out to the registered listeners on a dedicated
     * virtual thread.
     */
    private final WebHistorySyncService webHistorySyncService;

    /**
     * Telemetry sink used to commit every inbound-message WAM event:
     * receive, drop, high-retry, offline-count-too-high, MD bad
     * device-sent, structured-message receive, and non-message peer-data
     * response.
     */
    private final WamService wamService;

    /**
     * Ships the {@code <ack class="message">} stanza for {@code medianotify}
     * stanzas and the {@code <ack class="message" error=...>} NACK for parse
     * and runtime failures.
     */
    private final AckSender ackSender;

    /**
     * Resolves the AB-prop feature flags and the branded-number exemption list
     * consulted by the inbound-message country and Terms-of-Service gating
     * checks.
     */
    private final ABPropsService abPropsService;

    /**
     * Reads Terms-of-Service acceptance state for the inbound-message
     * Terms-of-Service gating check.
     */
    private final TosService tosService;

    /**
     * Classifies inbound messages against the Defense Mode quarantine policy.
     */
    private final QuarantineService quarantineService;

    /**
     * Per-chat reference payload of the last received rich-order-status
     * interactive message, keyed by chat JID.
     *
     * <p>Holds the {@code order_status} native-flow button JSON of the most
     * recently received rich-order-status message in each chat. When a newer
     * rich-order-status message arrives the stored payload is the referenced
     * version against which
     * {@link #emitRichOrderStatusInconsistency(MessageReceiveStanza, String)}
     * computes the per-field changed flags.
     *
     * @implNote
     * This implementation replaces WA Web's {@code WAWebOrderStatus.getMergedOrderStatus}
     * lookup of the merged order-status {@code firstMessage}: Cobalt does not
     * maintain a merged per-order state machine, so the last-seen order-status
     * payload for the chat serves as the reference version. The map is bounded
     * only by the number of distinct chats that ever send a rich-order-status
     * message and is never persisted.
     */
    private final Map<Jid, String> lastOrderStatusPayloadByChat = new ConcurrentHashMap<>();

    /**
     * Constructs a handler bound to the given collaborators.
     *
     * <p>Invoked by the socket-stream wiring at client construction; user
     * code does not instantiate this handler directly.
     *
     * @implNote
     * This implementation derives the receipt handler from the supplied
     * {@link LinkedWhatsAppClient} and constructs the history-sync service in place
     * because both are owned solely by the message handler. The
     * {@link SyncKeyRotationService} is pulled off the supplied
     * {@link WebAppStateService} so that the two services share their
     * underlying state.
     *
     * @param whatsapp                the owning {@link LinkedWhatsAppClient}
     * @param messageService          the {@link MessageService} that drives
     *                                parsing and decryption
     * @param snapshotRecoveryService the {@link SnapshotRecoveryService}
     *                                consulted for syncd snapshot fatal
     *                                recovery responses
     * @param webAppStateService      the {@link WebAppStateService} from which
     *                                the {@link SyncKeyRotationService} is
     *                                obtained
     * @param lidMigrationService     the {@link LidMigrationService} consulted
     *                                for LID migration mapping sync protocol
     *                                messages
     * @param wamService              the {@link WamService} telemetry sink for
     *                                inbound-message WAM events
     * @param ackSender               the {@link AckSender} used for the
     *                                {@code <ack class="message">} and NACK
     *                                paths
     * @param mediaConnectionService  the {@link MediaConnectionService} threaded
     *                                into the history-sync media download
     *                                pipeline
     * @param abPropsService          the {@link ABPropsService} consulted for the
     *                                gating feature flags and branded-number list
     * @param tosService              the {@link TosService} consulted for the
     *                                Terms-of-Service acceptance gating check
     * @param quarantineService       the {@link QuarantineService} consulted for the
     *                                inbound-message Defense Mode quarantine check
     */
    public MessageStreamHandler(
            LinkedWhatsAppClient whatsapp,
            MessageService messageService,
            SnapshotRecoveryService snapshotRecoveryService,
            WebAppStateService webAppStateService,
            LidMigrationService lidMigrationService,
            WamService wamService,
            AckSender ackSender,
            MediaConnectionService mediaConnectionService,
            ABPropsService abPropsService,
            TosService tosService,
            QuarantineService quarantineService
    ) {
        this.whatsapp = whatsapp;
        this.messageService = Objects.requireNonNull(messageService, "messageService cannot be null");
        this.receiptHandler = new MessageReceiptHandler(whatsapp);
        this.snapshotRecoveryService = Objects.requireNonNull(snapshotRecoveryService, "snapshotRecoveryService cannot be null");
        this.syncKeyRotationService = Objects.requireNonNull(webAppStateService, "webAppStateService cannot be null").syncKeyRotationService();
        this.lidMigrationService = Objects.requireNonNull(lidMigrationService, "lidMigrationService cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        Objects.requireNonNull(mediaConnectionService, "mediaConnectionService cannot be null");
        this.webHistorySyncService = new LiveWebHistorySyncService(whatsapp, lidMigrationService, wamService, mediaConnectionService);
        this.ackSender = Objects.requireNonNull(ackSender, "ackSender cannot be null");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
        this.tosService = Objects.requireNonNull(tosService, "tosService cannot be null");
        this.quarantineService = Objects.requireNonNull(quarantineService, "quarantineService cannot be null");
    }

    /**
     * Applies the privacy-mode update carried on a received message and records a
     * {@code GatedMessageReceived} metric when the message lands in a gated chat.
     *
     * <p>The chat contact's {@linkplain Contact#privacyModeHostStorage() privacy-mode
     * host storage} is refreshed from the message {@code <biz>} envelope (the newer
     * {@code privacy_mode_ts} wins); then, for a message that was not sent by the
     * local user, the country and Terms-of-Service gating checks run and the
     * matching {@link ChatGatedReason} is committed. A message whose chat has no
     * known contact (for example a group or newsletter) is never gated.
     *
     * @param info   the parsed inbound chat message
     * @param stanza the inbound message stanza carrying the {@code <biz>} envelope
     */
    @WhatsAppWebExport(moduleName = "WAWebLogReceivedMessages", exports = "logReceivedMessagesInWAM", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitGatedMessageReceivedIfApplicable(ChatMessageInfo info, MessageReceiveStanza stanza) {
        var contact = whatsapp.store().contactStore().findContactByJid(stanza.chatJid()).orElse(null);
        if (contact == null) {
            return;
        }
        stanza.bizInfo().ifPresent(biz -> {
            var hostStorage = biz.hostStorage().orElse(null);
            var timestamp = biz.privacyModeTs().orElse(null);
            if (hostStorage != null && timestamp != null) {
                BusinessHostStorageType.ofIndex(hostStorage)
                        .ifPresent(type -> contact.setPrivacyMode(type, Instant.ofEpochSecond(timestamp)));
            }
        });
        if (info.key().fromMe()) {
            return;
        }
        gatedReason(contact).ifPresent(reason -> wamService.commit(new GatedMessageReceivedEventBuilder()
                .chatGatedReason(reason)
                .build()));
    }

    /**
     * Returns the reason the chat with the given contact is gated for an inbound
     * message, with country gating taking precedence over Terms-of-Service gating.
     *
     * @param contact the chat contact
     * @return the {@link ChatGatedReason}, or empty when the chat is not gated
     */
    private Optional<ChatGatedReason> gatedReason(Contact contact) {
        if (shouldBlockByCountry(contact)) {
            return Optional.of(ChatGatedReason.COUNTRY);
        }
        if (shouldBlockByTos(contact)) {
            return Optional.of(ChatGatedReason.TOS3);
        }
        return Optional.empty();
    }

    /**
     * Returns whether the chat with the given contact is gated because the
     * recipient's region is not yet cleared for cross-Meta interoperable
     * messaging.
     *
     * @param contact the chat contact
     * @return {@code true} when country gating applies, {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebTosCountryGating", exports = "shouldBlockByCountry", adaptation = WhatsAppAdaptation.DIRECT)
    private boolean shouldBlockByCountry(Contact contact) {
        return abPropsService.getBool(ABProp.COUNTRY_CLIENT_GATING_ENABLED)
                && contact.hostedOnFacebook()
                && !isFbBrandedNumber(contact.jid());
    }

    /**
     * Returns whether the chat with the given contact is gated because the local
     * user has not accepted the interoperability Terms-of-Service notice.
     *
     * @implNote
     * This implementation also gates on
     * {@link ABProp#TOS_CLIENT_STATE_FETCH_ENABLED} as the proxy for WA Web's
     * {@code TosManager.getState(TOS_3) === "NOT_ACCEPTED"}: Cobalt persists only
     * acknowledged notices, so a notice counts as not accepted exactly when
     * fetching is enabled (which drives the login-time pull) yet the notice is
     * absent from the acknowledged set; without the fetch flag the state is
     * unknown and never gates, matching WA Web.
     *
     * @param contact the chat contact
     * @return {@code true} when Terms-of-Service gating applies, {@code false}
     *         otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebTosGating", exports = "shouldBlockByTos", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean shouldBlockByTos(Contact contact) {
        return abPropsService.getBool(ABProp.TOS_3_CLIENT_GATING_ENABLED)
                && abPropsService.getBool(ABProp.TOS_CLIENT_STATE_FETCH_ENABLED)
                && !tosService.isAcknowledged(TosNotice.TOS_3)
                && contact.hostedOnFacebook()
                && !isFbBrandedNumber(contact.jid());
    }

    /**
     * Returns whether the given JID's user part is a Facebook-branded system
     * message number, which is exempt from both gating checks.
     *
     * @param jid the chat contact JID
     * @return {@code true} when the number is on the
     *         {@link ABProp#SYSTEM_MSG_NUMBERS_FB_BRANDED} list, {@code false}
     *         otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsInternalNumber", exports = "getFbBrandedNumber", adaptation = WhatsAppAdaptation.DIRECT)
    private boolean isFbBrandedNumber(Jid jid) {
        var raw = abPropsService.getString(ABProp.SYSTEM_MSG_NUMBERS_FB_BRANDED);
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        var user = jid.user();
        for (var number : raw.split(",")) {
            if (number.equals(user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation keys ordering on the {@code from} JID so every message in a chat is
     * processed in arrival order, ensuring a group sender-key distribution message is applied before
     * the sender-key messages that depend on it. A message with no {@code from} attribute (which
     * {@link #handle(Stanza)} drops) maps to the empty key.
     */
    @Override
    protected String orderingKey(Stanza stanza) {
        return stanza.getAttributeAsString("from", "");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Routes the inbound {@code <message>} stanza through the
     * media-notify, newsletter, or normal-E2E branch (see the class-level
     * documentation for the full table) and sends the matching server-side
     * receipt after processing completes. The receipt type is determined by
     * the processing outcome: a parse failure yields a {@code 487} NACK; a
     * successful decrypt yields a delivery receipt (or a bot-invoke ack for
     * bot senders); a decrypt failure yields a retry, NACK, or delivery
     * receipt depending on the exception subtype; an unhandled runtime
     * exception yields a {@code 500} NACK. The transport
     * {@code <ack class="message">} is sent unconditionally up front; when
     * the relay is muting non-essential pushes the embedder uses
     * {@link LinkedWhatsAppClient#enablePassiveMode()} rather than gating the ack
     * itself.
     *
     * @implNote
     * This implementation commits the
     * {@link #maybePostOfflineCountTooHigh(MessageReceiveStanza)} metric
     * immediately after a successful parse, mirroring WA Web's ordering so
     * the metric fires before any decryption work begins.
     */
    @Override
    public void handle(Stanza node) {
        var from = node.getAttributeAsJid("from").orElse(null);
        if (from == null) {
            return;
        }

        var handleStartNanos = System.nanoTime();
        ackSender.sendAck(AckClass.MESSAGE, node);

        if ("medianotify".equals(node.getAttributeAsString("type", null))) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "medianotify stanza from {0}, skipping", from);
            return;
        }

        if (from.hasNewsletterServer()) {
            handleNewsletterMessage(node);
            return;
        }

        var parsingStartNanos = System.nanoTime();
        MessageReceiveStanza stanza;
        try {
            stanza = MessageReceiveStanzaParser.parse(
                    node,
                    whatsapp.store().accountStore().jid().orElse(null),
                    whatsapp.store().accountStore().lid().orElse(null));
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "failed to parse incoming message stanza from " + new LogRedactable.User(String.valueOf(from)), exception);
            }
            emitUnknownStanzaMetric(node);
            emitIncomingMessageDropFromNode(node, MessageDropReasonType.INVALID_STANZA);
            sendNack(node, "487");
            return;
        }
        var parsingNanos = System.nanoTime() - parsingStartNanos;
        var preProcessingNanos = parsingStartNanos - handleStartNanos;

        maybePostOfflineCountTooHigh(stanza);

        try {
            var processingStartNanos = System.nanoTime();
            var info = messageService.process(node);
            var processingNanos = System.nanoTime() - processingStartNanos;
            if (info != null) {
                var quarantined = info instanceof ChatMessageInfo quarantineCandidate
                        && quarantineService.quarantine(quarantineCandidate);
                var dbStoringStartNanos = System.nanoTime();
                try {
                    storeIncomingMessage(info);
                } catch (RuntimeException storeFailure) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "failed to persist incoming message id=" + stanza.id(), storeFailure);
                    }
                    emitIncomingMessageDropForDbFailure(stanza);
                    return;
                }
                var dbStoringNanos = System.nanoTime() - dbStoringStartNanos;
                var postProcessingStartNanos = System.nanoTime();
                if (info instanceof ChatMessageInfo chatInfo) {
                    handleProtocolMessage(chatInfo);
                    emitMessageReceiveForChatMessage(chatInfo, stanza);
                    emitGatedMessageReceivedIfApplicable(chatInfo, stanza);
                    emitStructuredMessageReceiveIfApplicable(stanza);
                    emitCommerceStructuredReceiveIfApplicable(chatInfo, stanza);
                    emitRichOrderStatusIfApplicable(chatInfo, stanza);
                }
                resolveOrphanPayment(info);
                if (!quarantined) {
                    var quoted = whatsapp.store().chatStore().findQuotedMessage(info);
                    notifyMessageReceived(info, quoted);
                }
                var postProcessingNanos = System.nanoTime() - postProcessingStartNanos;
                maybeEmitMessageProcessingPerf(stanza, preProcessingNanos, parsingNanos,
                        processingNanos, dbStoringNanos, postProcessingNanos);
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "processed incoming message {0} chat={1} quarantined={2}",
                            stanza.id(), stanza.chatJid(), quarantined);
                }
            }

            if (info == null) {
                if (receiptHandler.isBotSender(stanza)) {
                    receiptHandler.sendBotInvokeResponseAck(stanza);
                }
                return;
            }

            if (receiptHandler.isBotSender(stanza)) {
                receiptHandler.sendBotInvokeResponseAck(stanza);
            } else {
                receiptHandler.sendDeliveryReceipt(stanza, info);
            }
        } catch (WhatsAppMessageException.Receive exception) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "incoming message id=" + stanza.id() + " failed", exception);
            }
            emitIncomingMessageDropFromStanza(stanza, exception);
            emitMdBadDeviceSentMessageIfApplicable(stanza, exception);
            handleReceiveFailure(stanza, exception);
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "incoming message id=" + stanza.id() + " failed unexpectedly", exception);
            }
            emitIncomingMessageDropFromStanza(stanza, null);
            sendNack(node, "500");
        }
    }

    /**
     * Processes an inbound newsletter message stanza routed here by
     * {@link #handle(Stanza)} because its {@code from} JID lives on a
     * newsletter server.
     *
     * <p>The payload is plaintext and no end-to-end decryption runs. The
     * decoded message is persisted, reconciled against any orphan payment,
     * broadcast to the registered listeners, and surfaced as a WAM receive
     * event; no delivery receipt is sent because newsletter messages are
     * server-fanned-out plaintext content.
     *
     * @implNote
     * This implementation converts a runtime exception during processing
     * into an
     * {@link com.github.auties00.cobalt.wire.wam.event.IncomingMessageDropEvent}
     * with {@link MessageDropReasonType#INVALID_PROTOBUF} and
     * {@link E2eDestination#CHANNEL}, matching the WA Web emission for a
     * {@code MessageValidationError} on the channel pipeline.
     *
     * @param stanza the inbound newsletter {@code <message>} stanza
     */
    private void handleNewsletterMessage(Stanza stanza) {
        try {
            var info = messageService.process(stanza);
            if (info == null) {
                return;
            }

            storeIncomingMessage(info);
            resolveOrphanPayment(info);
            var quoted = whatsapp.store().chatStore().findQuotedMessage(info);
            notifyMessageReceived(info, quoted);
            if (info instanceof NewsletterMessageInfo newsletterInfo) {
                emitMessageReceiveForNewsletterMessage(newsletterInfo);
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "processed newsletter message {0}", newsletterInfo.key().id().orElse(null));
                }
            }
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                var from = stanza.getAttributeAsJid("from").orElse(null);
                LOGGER.log(Level.WARNING, "failed to handle newsletter message stanza from " + new LogRedactable.User(String.valueOf(from)), exception);
            }
            wamService.commit(new IncomingMessageDropEventBuilder()
                    .messageDropReason(MessageDropReasonType.INVALID_PROTOBUF)
                    .e2eDestination(E2eDestination.CHANNEL)
                    .build());
        }
    }

    /**
     * Selects and dispatches the server-side receipt for a stanza whose
     * decryption raised a {@link WhatsAppMessageException.Receive}.
     *
     * <p>Picks among four outcomes: no receipt for an
     * {@link WhatsAppMessageException.Receive.HsmMismatch} (matching WA Web's
     * silent drop); a NACK when
     * {@link WhatsAppMessageException.Receive#errorCode()} is non-empty; a
     * retry receipt when
     * {@link WhatsAppMessageException.Receive#shouldSendRetryReceipt()} is
     * {@code true} (which also fires the high-retry-count WAM metric); and a
     * fall-back delivery receipt or bot-invoke ack otherwise.
     *
     * @implNote
     * This implementation increments the retry count locally because
     * Cobalt's stanza model does not mutate the parsed
     * {@link MessageReceiveStanza} itself.
     *
     * @param stanza    the parsed inbound stanza whose decryption failed
     * @param exception the decryption failure
     */
    private void handleReceiveFailure(
            MessageReceiveStanza stanza,
            WhatsAppMessageException.Receive exception
    ) {
        if (exception instanceof WhatsAppMessageException.Receive.HsmMismatch) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "hsm mismatch for message {0}, no receipt sent", stanza.id());
            return;
        }

        var errorCode = exception.errorCode().orElse(null);
        if (errorCode != null) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "nack (" + errorCode + ") for message " + stanza.id()
                        + " from " + new LogRedactable.User(String.valueOf(stanza.senderJid())), exception);
            }
            receiptHandler.sendNackReceipt(stanza, parseErrorCode(errorCode));
            return;
        }

        if (exception.shouldSendRetryReceipt()) {
            var nextRetryCount = stanza.retryCount().orElse(0) + 1;
            receiptHandler.sendRetryReceipt(
                    stanza,
                    exception.retryReason(),
                    nextRetryCount
            );
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "sent retry receipt for message {0}, attempt {1}", stanza.id(), nextRetryCount);
            }
            maybeEmitMessageHighRetryCount(stanza, nextRetryCount);
            surfaceUndecryptableMessage(stanza, exception, nextRetryCount);
            return;
        }

        if (receiptHandler.isBotSender(stanza)) {
            receiptHandler.sendBotInvokeResponseAck(stanza);
        } else {
            receiptHandler.sendDeliveryReceipt(stanza, null);
        }
    }

    /**
     * Surfaces an inbound message that could not be decrypted so the failure is not silent while
     * the Signal retry-receipt ceremony runs.
     *
     * <p>A per-message decryption failure is deliberately non-fatal: it is converted into a retry
     * receipt rather than propagated, so without this method the only trace is a {@code DEBUG} log
     * and a WAM metric, and a message that never recovers (for example a sender that stays offline)
     * is lost with no visible signal. Three escalations run here:
     * <ul>
     *   <li>On the first failed delivery a {@link ChatMessageInfo.StubType#CIPHERTEXT} placeholder
     *       (WhatsApp Web's "Waiting for this message" stub) is stored and broadcast to the
     *       listeners, keyed by the real message id so a later successful retry replaces it.</li>
     *   <li>Once the retry count reaches {@link #MAX_MESSAGE_RETRY_COUNT} the message is effectively
     *       stuck: the failure is logged at {@code WARNING} so it is visible at the default level.</li>
     *   <li>At the same threshold the configured {@link com.github.auties00.cobalt.client.WhatsAppClientErrorHandler}
     *       is notified through {@link LinkedWhatsAppClient#handleFailure(com.github.auties00.cobalt.exception.WhatsAppException)};
     *       the exception is non-fatal so the handler's default outcome is to discard without
     *       disconnecting.</li>
     * </ul>
     *
     * @param stanza     the parsed inbound stanza whose decryption failed
     * @param exception  the decryption failure
     * @param retryCount the post-increment retry attempt number
     */
    private void surfaceUndecryptableMessage(
            MessageReceiveStanza stanza,
            WhatsAppMessageException.Receive exception,
            int retryCount
    ) {
        if (retryCount == 1) {
            var placeholder = buildUndecryptablePlaceholder(stanza);
            storeIncomingMessage(placeholder);
            emitPlaceholderActivityAdd(stanza, exception);
            notifyMessageReceived(placeholder, Optional.empty());
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "stored ciphertext placeholder for undecryptable message {0} from {1}",
                        stanza.id(), stanza.senderJid());
            }
        }

        if (retryCount == MAX_MESSAGE_RETRY_COUNT) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "message id=" + stanza.id() + " from "
                        + new LogRedactable.User(String.valueOf(stanza.senderJid())) + " still undecryptable after "
                        + retryCount + " retries", exception);
            }
            whatsapp.handleFailure(exception);
        }
    }

    /**
     * Builds the {@link ChatMessageInfo.StubType#CIPHERTEXT} placeholder for an undecryptable
     * message.
     *
     * <p>The placeholder carries the real message key (so a recovered retry overwrites it), an
     * {@link LinkedMessageContainer#empty() empty} body, and {@link MessageStatus#ERROR} status.
     *
     * @implNote
     * This implementation derives {@code fromMe} by matching the sender's user JID against the
     * local PN and LID accounts, mirroring
     * {@code MessageReceiver.isFromMe} which is not reachable from this package.
     *
     * @param stanza the parsed inbound stanza whose decryption failed
     * @return the placeholder message info
     */
    private ChatMessageInfo buildUndecryptablePlaceholder(MessageReceiveStanza stanza) {
        var senderJid = stanza.senderJid().toUserJid();
        var selfPn = whatsapp.store().accountStore().jid().orElse(null);
        var selfLid = whatsapp.store().accountStore().lid().orElse(null);
        var fromMe = (selfPn != null && senderJid.equals(selfPn.toUserJid()))
                || (selfLid != null && senderJid.equals(selfLid.toUserJid()));

        var key = new MessageKeyBuilder()
                .id(stanza.id())
                .parentJid(stanza.chatJid())
                .fromMe(fromMe)
                .senderJid(senderJid)
                .build();

        return new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.empty())
                .timestamp(stanza.timestamp())
                .status(MessageStatus.ERROR)
                .senderJid(senderJid)
                .stubType(ChatMessageInfo.StubType.CIPHERTEXT)
                .broadcast(stanza.chatJid().hasBroadcastServer())
                .pushName(stanza.pushName().orElse(null))
                .build();
    }

    /**
     * Commits a {@code MessageHighRetryCount} WAM event when the
     * post-increment retry count reaches the
     * {@link #MAX_MESSAGE_RETRY_COUNT} threshold.
     *
     * <p>Surfaces the message-retry-storm telemetry used to flag chats where
     * Signal session negotiation keeps failing. The metric is committed
     * exactly once per retry attempt at or above the threshold.
     *
     * @implNote
     * This implementation populates the {@code retryCount},
     * {@code messageType}, {@code e2eSenderType}, and {@code encryptionType}
     * properties that are derivable from the parsed stanza alone. The
     * {@code deviceSizeBucket} (groups only) property is left absent because
     * Cobalt has no equivalent of
     * {@code WAWebWamGroupMetricCache.getGroupMetrics}; WA Web also omits the
     * property when the cached metric is unavailable, so the omission is
     * parity-preserving.
     *
     * @param stanza     the parsed inbound stanza
     * @param retryCount the post-increment retry attempt number
     */
    @WhatsAppWebExport(moduleName = "WAWebPostMessageHighRetryCountMetric",
            exports = "maybePostMessageHighRetryCountMetric",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void maybeEmitMessageHighRetryCount(MessageReceiveStanza stanza, int retryCount) {
        if (retryCount < MAX_MESSAGE_RETRY_COUNT) {
            return;
        }

        var builder = new MessageHighRetryCountEventBuilder()
                .retryCount(retryCount)
                .messageType(WamMsgUtils.getWamMessageTypeFromStanzaType(stanza.messageType()));

        var selfJid = whatsapp.store().accountStore().jid().orElse(null);
        var senderType = WamMsgUtils.getWamE2eSenderType(stanza.senderJid(), selfJid);
        if (senderType != null) {
            builder.e2eSenderType(senderType);
        }

        if (stanza.senderJid().hasHostedServer() || stanza.senderJid().hasHostedLidServer()) {
            builder.encryptionType(EncryptionTypeCode.COEX);
        }

        wamService.commit(builder.build());
    }

    /**
     * Commits an
     * {@link com.github.auties00.cobalt.wire.wam.event.OfflineCountTooHighEvent}
     * when the parsed stanza carries an {@code offline} attribute at or above
     * {@link #OFFLINE_COUNT_TOO_HIGH_THRESHOLD}.
     *
     * <p>Surfaces the queue-depth alarm WA Web posts during offline delivery:
     * every parsed stanza whose {@code offline} attribute parses to an
     * integer at or above the threshold contributes one event so the server
     * can detect clients that fell far enough behind to risk dropping events.
     *
     * @implNote
     * This implementation populates {@code offlineCount}, {@code stanzaType}
     * (hard-coded to {@link StanzaType#MESSAGE} because the helper is
     * reachable only from the message dispatcher), {@code mediaType} (via
     * {@link #mapEncMediaTypeToWamMediaType(String, String, String)}),
     * {@code messageType}, {@code e2eSenderType}, and {@code encryptionType}.
     * The four spec properties that apply only to
     * call/notification/receipt stanzas ({@code callStanzaType},
     * {@code invisibleMessageCategory}, {@code notificationStanzaType},
     * {@code receiptStanzaType}) are intentionally absent, matching WA Web's
     * emission site.
     *
     * @param stanza the parsed inbound message stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebMaybePostOfflineCountTooHighMetric",
            exports = "maybePostOfflineCountTooHigh",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void maybePostOfflineCountTooHigh(MessageReceiveStanza stanza) {
        var rawOffline = stanza.offline().orElse(null);
        if (rawOffline == null) {
            return;
        }
        int offlineCount;
        try {
            offlineCount = Integer.parseInt(rawOffline);
        } catch (NumberFormatException _) {
            return;
        }
        if (offlineCount < OFFLINE_COUNT_TOO_HIGH_THRESHOLD) {
            return;
        }

        var builder = new OfflineCountTooHighEventBuilder()
                .offlineCount(offlineCount)
                .stanzaType(StanzaType.MESSAGE);

        var encMediaType = stanza.encs().stream()
                .map(enc -> enc.encMediaType().orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        var mediaType = mapEncMediaTypeToWamMediaType(
                encMediaType, stanza.stanzaType(), stanza.pollType().orElse(null));
        if (mediaType != null) {
            builder.mediaType(mediaType);
        }

        var messageType = WamMsgUtils.getWamMessageTypeFromStanzaType(stanza.messageType());
        if (messageType != null) {
            builder.messageType(messageType);
        }

        var selfJid = whatsapp.store().accountStore().jid().orElse(null);
        var senderType = WamMsgUtils.getWamE2eSenderType(stanza.senderJid(), selfJid);
        if (senderType != null) {
            builder.e2eSenderType(senderType);
        }

        if (stanza.senderJid().hasHostedServer() || stanza.senderJid().hasHostedLidServer()) {
            builder.encryptionType(EncryptionTypeCode.COEX);
        }

        wamService.commit(builder.build());
    }

    /**
     * Maps the {@code (encMediaType, stanzaType, pollType)} triple extracted
     * from an inbound stanza onto the WAM {@link MediaType} enum.
     *
     * <p>Drives the {@code mediaType} property of every WAM metric that needs
     * to classify the inbound payload, including
     * {@link com.github.auties00.cobalt.wire.wam.event.OfflineCountTooHighEvent}.
     * Reaction and medianotify stanza types win over poll types, poll
     * creation and vote win over the enc media type, and the enc media type
     * drives the remaining cases. Any unrecognised triple returns
     * {@link MediaType#NONE}.
     *
     * @implNote
     * This implementation honours the same precedence as
     * {@code WAWebBackendJobsCommon.getMetricMediaType}, including the
     * {@link MediaType#NONE} default branch.
     *
     * @param encMediaType the first non-{@code null} {@code mediatype}
     *                     attribute among the stanza's enc payloads, or
     *                     {@code null} when none carry one
     * @param stanzaType   the stanza's top-level {@code type} attribute, or
     *                     {@code null}
     * @param pollType     the {@code polltype} attribute from the
     *                     {@code <meta>} stanza, or {@code null}
     * @return the corresponding {@link MediaType} enum value
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "getMetricMediaType",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static MediaType mapEncMediaTypeToWamMediaType(
            String encMediaType,
            String stanzaType,
            String pollType
    ) {
        if ("reaction".equals(stanzaType)) {
            return MediaType.REACTION;
        }
        if ("medianotify".equals(stanzaType)) {
            return MediaType.MEDIA_EXPRESS_NOTIFY;
        }
        if ("creation".equals(pollType)) {
            return MediaType.POLL_CREATE;
        }
        if ("vote".equals(pollType)) {
            return MediaType.POLL_VOTE;
        }
        if (encMediaType == null) {
            return MediaType.NONE;
        }
        return switch (encMediaType) {
            case "image" -> MediaType.PHOTO;
            case "video" -> MediaType.VIDEO;
            case "ptv" -> MediaType.PUSH_TO_VIDEO;
            case "audio" -> MediaType.AUDIO;
            case "ptt" -> MediaType.PTT;
            case "location" -> MediaType.LOCATION;
            case "vcard" -> MediaType.CONTACT;
            case "document" -> MediaType.DOCUMENT;
            case "url" -> MediaType.URL;
            case "call" -> MediaType.CALL;
            case "gif" -> MediaType.GIF;
            case "future" -> MediaType.FUTURE;
            case "contact_array" -> MediaType.CONTACT_ARRAY;
            case "livelocation" -> MediaType.LIVE_LOCATION;
            case "profile_pic" -> MediaType.PROFILE_PIC;
            case "sticker" -> MediaType.STICKER;
            case "sticker_pack" -> MediaType.STICKER_PACK;
            case "hsm" -> MediaType.HSM;
            case "product_image", "product" -> MediaType.PRODUCT_IMAGE;
            case "template" -> MediaType.TEMPLATE;
            case "md_app_state" -> MediaType.MD_APP_STATE;
            case "md_history_sync" -> MediaType.MD_HISTORY_SYNC;
            case "list" -> MediaType.LIST;
            case "list_response" -> MediaType.LIST_REPLY;
            case "button" -> MediaType.BUTTON_MESSAGE;
            case "button_response" -> MediaType.BUTTON_RESPONSE_MESSAGE;
            case "order" -> MediaType.ORDER;
            case "native_flow_response" -> MediaType.INTERACTIVE_RESPONSE_NFM;
            case "group_history" -> MediaType.GROUP_HISTORY;
            default -> MediaType.NONE;
        };
    }

    /**
     * Commits an
     * {@link com.github.auties00.cobalt.wire.wam.event.IncomingMessageDropEvent}
     * for a drop that occurred before the stanza could even be parsed into a
     * {@link MessageReceiveStanza}.
     *
     * <p>Surfaces the pre-parse drop emission triggered when
     * {@link MessageReceiveStanzaParser#parse} throws.
     *
     * @implNote
     * This implementation populates only the {@code offline} and
     * {@code offlineCount} properties because Cobalt has no equivalent of WA
     * Web's {@code incomingMsgParserForMetric} fall-back parser that extracts
     * best-effort metadata from a stanza that could not be fully parsed; WA
     * Web also leaves the remaining properties absent when its metric parser
     * fails.
     *
     * @param stanza              the raw inbound stanza stanza
     * @param messageDropReason the drop reason to record on the event
     */
    private void emitIncomingMessageDropFromNode(Stanza stanza, MessageDropReasonType messageDropReason) {
        var builder = new IncomingMessageDropEventBuilder()
                .messageDropReason(messageDropReason);

        var offline = stanza.getAttributeAsLong("offline", (Long) null);
        if (offline != null) {
            builder.offline(true).offlineCount(offline.intValue());
        } else {
            builder.offline(false);
        }

        wamService.commit(builder.build());
    }

    /**
     * Commits an
     * {@link com.github.auties00.cobalt.wire.wam.event.UnknownStanzaEvent} for a
     * stanza whose top-level shape did not parse.
     *
     * <p>Records the stanza tag and type so the server can detect bundles
     * that send shapes the client cannot yet understand.
     *
     * @implNote
     * This implementation leaves {@code unknownStanzaDropReason} unset,
     * matching {@code WAWebPostUnknownStanzaMetric.postUnknownStanzaMetric}
     * which never populates it at this call site.
     *
     * @param stanza the stanza that failed to parse
     */
    @WhatsAppWebExport(moduleName = "WAWebPostUnknownStanzaMetric",
            exports = "postUnknownStanzaMetric",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void emitUnknownStanzaMetric(Stanza stanza) {
        wamService.commit(new UnknownStanzaEventBuilder()
                .unknownStanzaTag(stanza.description())
                .unknownStanzaType(stanza.getAttributeAsString("type", null))
                .build());
    }

    /**
     * Commits an
     * {@link com.github.auties00.cobalt.wire.wam.event.IncomingMessageDropEvent}
     * for a drop that occurred while processing an already-parsed stanza.
     *
     * <p>Mirrors the per-decrypt-slot drop telemetry WA Web emits when an
     * inbound decrypt outcome is non-success and not a benign skip. The drop
     * reason is mapped from the exception subtype, and the event carries the
     * {@code offlineCount}, {@code retryCount}, {@code e2eCiphertextType},
     * and {@code e2eDestination} properties derivable from the stanza alone.
     * When {@code exception} is {@code null} the drop reason is
     * {@link MessageDropReasonType#INTERNAL_ERROR}.
     *
     * @implNote
     * This implementation skips emission entirely for the exception subtypes
     * WA Web ignores ({@link WhatsAppMessageException.Receive.HsmMismatch},
     * {@link WhatsAppMessageException.Receive.BroadcastEphemeralSettings},
     * {@link WhatsAppMessageException.Receive.DuplicateMessage},
     * {@link WhatsAppMessageException.Receive.UnknownDevice}, and the full set
     * of Signal-level pre/key-related faults).
     *
     * @param stanza    the parsed inbound stanza
     * @param exception the receive exception that triggered the drop, or
     *                  {@code null} for an internal unhandled error
     */
    private void emitIncomingMessageDropFromStanza(
            MessageReceiveStanza stanza,
            WhatsAppMessageException.Receive exception
    ) {
        if (exception instanceof WhatsAppMessageException.Receive.HsmMismatch
                || exception instanceof WhatsAppMessageException.Receive.BroadcastEphemeralSettings
                || exception instanceof WhatsAppMessageException.Receive.DuplicateMessage
                || exception instanceof WhatsAppMessageException.Receive.UnknownDevice
                || exception instanceof WhatsAppMessageException.Receive.NoSession
                || exception instanceof WhatsAppMessageException.Receive.InvalidKey
                || exception instanceof WhatsAppMessageException.Receive.InvalidKeyId
                || exception instanceof WhatsAppMessageException.Receive.InvalidSignedPreKey
                || exception instanceof WhatsAppMessageException.Receive.InvalidOneTimeKey
                || exception instanceof WhatsAppMessageException.Receive.NoSenderKey
                || exception instanceof WhatsAppMessageException.Receive.InvalidSenderKey
                || exception instanceof WhatsAppMessageException.Receive.BadMac
                || exception instanceof WhatsAppMessageException.Receive.FutureMessage
                || exception instanceof WhatsAppMessageException.Receive.InvalidSignature
                || exception instanceof WhatsAppMessageException.Receive.AdvFailure) {
            return;
        }

        var messageDropReason = resolveDropReason(stanza, exception);
        if (messageDropReason == null) {
            return;
        }

        var builder = new IncomingMessageDropEventBuilder()
                .messageDropReason(messageDropReason);

        builder.offline(stanza.isOffline());
        stanza.offline().ifPresent(raw -> {
            try {
                builder.offlineCount(Integer.parseInt(raw));
            } catch (NumberFormatException _) {
            }
        });

        var encs = stanza.encs();
        if (!encs.isEmpty()) {
            var firstEnc = encs.getFirst();
            builder.retryCount(firstEnc.retryCount());

            builder.e2eCiphertextType(mapCiphertextTypeForDrop(firstEnc.e2eType()));
        }

        var destination = mapDestination(stanza);
        if (destination != null) {
            builder.e2eDestination(destination);
        }

        // TODO: surface stanza.category() to WAM's invisibleMessageCategory and
        //       propagate an e2eFailureReason from the receive exception hierarchy
        //       instead of leaving both fields absent.

        wamService.commit(builder.build());
    }

    /**
     * Commits an
     * {@link com.github.auties00.cobalt.wire.wam.event.MdBadDeviceSentMessageEvent}
     * when the current receive failure is a device-sent-message validation
     * error.
     *
     * <p>Records the broken device-sent message ({@code peerType} and
     * {@code dsmError}) so the server can detect companion devices that ship
     * malformed device-sent envelopes; non-DSM failures are ignored.
     *
     * @implNote
     * This implementation derives {@code peerType} from {@link Jid#device()}:
     * zero (the default device id) maps to {@link DeviceType#PRIMARY}, any
     * other value to {@link DeviceType#COMPANION}. The remaining event-spec
     * properties ({@code editType}, {@code encryptionType}, {@code isLid},
     * {@code mediaType}, {@code messageType}, {@code revokeType}) are left
     * absent because the WA Web emission site populates only {@code peerType}
     * and {@code dsmError}.
     *
     * @param stanza    the parsed inbound stanza whose decrypt failed
     * @param exception the receive exception that triggered the drop
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgError", exports = "DeviceSentMessageError",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitMdBadDeviceSentMessageIfApplicable(
            MessageReceiveStanza stanza,
            WhatsAppMessageException.Receive exception
    ) {
        if (!(exception instanceof WhatsAppMessageException.Receive.InvalidDeviceSentMessage dsmException)) {
            return;
        }

        var peerType = stanza.senderJid().device() == 0
                ? DeviceType.PRIMARY
                : DeviceType.COMPANION;

        var dsmError = switch (dsmException.errorType()) {
            case INVALID_SENDER -> DsmError.INVALID_SENDER;
            case MISSING_DSM -> DsmError.MISSING_DSM;
            case INVALID_DSM -> DsmError.INVALID_DSM;
        };

        wamService.commit(new MdBadDeviceSentMessageEventBuilder()
                .peerType(peerType)
                .dsmError(dsmError)
                .build());
    }

    /**
     * Commits a
     * {@link com.github.auties00.cobalt.wire.wam.event.MessageReceiveEvent} for a
     * successfully decrypted E2E chat message.
     *
     * <p>Runs once per decrypted message in an incoming batch and carries the
     * typing, content, addressing, ephemerality, and timing metadata the
     * server uses to debug delivery and adoption regressions. After committing
     * the event it reports the receive to the ctlv2 thread-logging aggregator
     * through {@link LinkedWhatsAppClient#recordThreadActivity(com.github.auties00.cobalt.wire.core.jid.JidProvider, ThreadLoggingActivity)}
     * as a {@link ThreadLoggingActivity.MessageReceived}; protocol and system
     * messages are skipped.
     *
     * @implNote
     * This implementation populates the subset of properties Cobalt can
     * derive directly from the parsed {@link MessageReceiveStanza}, the
     * decoded {@link ChatMessageInfo}, and the store. The WA-Web-specific
     * properties Cobalt does not track ({@code deviceCount},
     * {@code deviceSizeBucket}, {@code oppositeVisibleIdentification},
     * {@code hasUsername}, {@code hasUsernamePin}, vcard
     * {@code received*ContactSize}, sticker
     * {@code stickerIs*}/{@code stickerMakerSourceType},
     * {@code invisibleMessageCategory}, {@code pairedMediaType},
     * {@code privateAiFeatureName}, {@code traceIdInt}, {@code appContext},
     * {@code stanzaProcessCount}, {@code processingDeferred}, {@code isPq})
     * are intentionally absent, matching WA Web's behaviour when the upstream
     * source is unavailable. The three {@code messageReceiveT*} timers are
     * encoded as elapsed milliseconds via {@link Instant#ofEpochMilli(long)}:
     * {@code T0} is the server-to-client latency
     * ({@code clientReceivedTs - serverTs}), {@code T1} and {@code T2} are
     * zeroed exactly as WA Web does at this call site.
     *
     * @param info   the decoded chat message info
     * @param stanza the parsed inbound stanza carrying timestamps,
     *               addressing, offline flag, and retry metadata
     */
    @WhatsAppWebExport(moduleName = "WAWebLogReceivedMessages", exports = "logReceivedMessagesInWAM",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitMessageReceiveForChatMessage(
            ChatMessageInfo info,
            MessageReceiveStanza stanza
    ) {
        var builder = new MessageReceiveEventBuilder();

        builder.messageType(WamMsgUtils.getWamMessageType(info));

        builder.messageMediaType(WamMsgUtils.getWamMediaType(info));

        builder.messageIsOffline(stanza.isOffline());

        stanza.offline().ifPresent(raw -> {
            try {
                builder.offlineCount(Integer.parseInt(raw));
            } catch (NumberFormatException _) {
            }
        });

        builder.isViewOnce(isViewOnceMessage(info.message()));

        var contextInfo = extractContextInfo(info.message()).orElse(null);
        if (contextInfo != null) {
            builder.isForwardedForward(contextInfo.forwardingScore().orElse(0) > 1);
            builder.isAReply(contextInfo.quotedMessageId().isPresent());

            contextInfo.disappearingMode().ifPresent(mode -> applyDisappearingMode(builder, mode));
        } else {
            builder.isForwardedForward(false);
            builder.isAReply(false);
        }

        var editType = resolveEditType(stanza, info);
        if (editType != null) {
            builder.editType(editType);
        }

        // TODO: surface BizBotType / BizBotAutomatedType so the bot-type
        //       classification is not collapsed to METABOT for every bot sender.
        if (stanza.senderJid().isBot()) {
            builder.botType(BotType.METABOT);
        }

        builder.isAComment(info.message().content() instanceof CommentMessage);

        builder.chatOrigins(stanza.chatJid().hasLidServer()
                ? ChatOriginsType.LID_CTWA
                : ChatOriginsType.OTHERS);

        builder.isLid(stanza.senderJid().hasLidServer());

        resolveRevokeType(stanza, info).ifPresent(builder::revokeType);

        info.ephemeralDuration().ifPresent(duration -> {
            if (duration > 0) {
                builder.ephemeralityDuration(duration);
            }
        });

        var clientReceivedTsMillis = Instant.now().toEpochMilli();
        var serverTsMillis = stanza.timestamp().toEpochMilli();
        builder.messageReceiveT0(Instant.ofEpochMilli(Math.max(0, clientReceivedTsMillis - serverTsMillis)));
        builder.messageReceiveT1(Instant.ofEpochMilli(0));
        builder.messageReceiveT2(Instant.ofEpochMilli(0));

        var selfJid = whatsapp.store().accountStore().jid().orElse(null);
        var senderType = WamMsgUtils.getWamE2eSenderType(stanza.senderJid(), selfJid);
        if (senderType != null) {
            builder.e2eSenderType(senderType);
        }

        if (stanza.senderJid().hasHostedServer() || stanza.senderJid().hasHostedLidServer()) {
            builder.encryptionType(EncryptionTypeCode.COEX);
        }

        // TODO: classify subgroup vs community vs plain group so typeOfGroup
        //       is more than a single GROUP bucket for every group/community chat.
        if (stanza.chatJid().hasGroupOrCommunityServer()) {
            builder.typeOfGroup(TypeOfGroupEnum.GROUP);
        }

        stanza.addressingMode()
                .flatMap(MessageStreamHandler::mapAddressingMode)
                .ifPresent(builder::serverAddressingMode);

        wamService.commit(builder.build());

        var receivedContent = info.message().content();
        if (!(receivedContent instanceof ProtocolMessage)) {
            var reaction = receivedContent instanceof ReactionMessage || receivedContent instanceof EncReactionMessage;
            var forwarded = contextInfo != null && contextInfo.forwardingScore().orElse(0) > 1;
            var commerce = ThreadLoggingMessages.isCommerceMessage(info.message());
            whatsapp.recordThreadActivity(stanza.chatJid(), new ThreadLoggingActivity.MessageReceived(
                    isViewOnceMessage(info.message()), forwarded, reaction, commerce));
        }
    }

    /**
     * Commits a
     * {@link com.github.auties00.cobalt.wire.wam.event.StructuredMessageReceiveEvent}
     * when the inbound chat message is a galaxy-flow CTA or a payment-request
     * native-flow interactive message.
     *
     * <p>Covers two WA Web sibling emissions that fire from the received-
     * message WAM logger: the CTA-flow path (for
     * {@code nativeFlowName == "galaxy_message"}) and the payment-request
     * path (for {@code nativeFlowName == "payment_request"}). Both emit the
     * same event class with {@code messageClass=BUTTON_NFM},
     * {@code bizPlatform=CLOUDAPI}, and the sender's business JID; they differ
     * only in {@code messageMediaType} ({@link MediaType#NONE} for CTA-flow,
     * {@link MediaType#INTERACTIVE_NFM} for payment request) and in the
     * {@code messageClassAttributes} JSON payload.
     *
     * @implNote
     * This implementation leaves {@code messageClassAttributes} absent
     * because the upstream helpers
     * ({@code WAWebGetGalaxyFlowCtaButton.getGalaxyFlowCtaButton},
     * {@code WAWebBrPaymentRequest.parsePaymentRequestButton},
     * {@code P2XFunnelIdGenerator.genFunnelInfo}) and the per-conversation
     * CTWA entry-point state they consume are not modelled in Cobalt. WA Web
     * also omits this field when its helpers yield {@code null}, so the
     * omission is parity-preserving rather than a divergence.
     *
     * @param stanza the parsed inbound stanza carrying the biz
     *               {@code nativeFlowName} attribute and the sender JID
     */
    @WhatsAppWebExport(moduleName = "WAWebGalaxyFlowWamLoggerUtils",
            exports = "logStructuredMessageReceivedWAMEvent",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPaymentRequestWamLogger",
            exports = "logPaymentRequestReceivedWAMEvent",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitStructuredMessageReceiveIfApplicable(
            MessageReceiveStanza stanza
    ) {
        var nativeFlowName = stanza.bizInfo()
                .flatMap(bi -> bi.nativeFlowName())
                .orElse(null);
        if (nativeFlowName == null) {
            return;
        }

        MediaType mediaType;
        switch (nativeFlowName) {
            case "galaxy_message" -> {
                mediaType = MediaType.NONE;
            }
            case "payment_request" -> {
                mediaType = MediaType.INTERACTIVE_NFM;
            }
            default -> {
                return;
            }
        }

        var businessOwnerJid = stanza.senderJid().toUserJid().user();

        var builder = new StructuredMessageReceiveEventBuilder()
                .messageClass(StructuredMessageClass.BUTTON_NFM)
                .messageMediaType(mediaType)
                .bizPlatform(BizPlatform.CLOUDAPI);
        if (businessOwnerJid != null) {
            builder.businessOwnerJid(businessOwnerJid);
        }

        wamService.commit(builder.build());
        emitStructuredMessageBuyerReceive(mediaType);
    }

    /**
     * Commits a
     * {@link com.github.auties00.cobalt.wire.wam.event.StructuredMessageReceiveEvent}
     * and its buyer-receive sibling for an inbound commerce structured message
     * that Cobalt detects from the decoded message content.
     *
     * <p>Extends {@link #emitStructuredMessageReceiveIfApplicable(MessageReceiveStanza)}
     * (which keys off the stanza-level {@code nativeFlowName}) to the three
     * content-typed structured classes WA Web logs from dedicated receive
     * loggers: order details ({@link OrderMessage} or an {@code order_details}
     * native-flow interactive message), payment info
     * ({@link SendPaymentMessage} or {@link PaymentInviteMessage}), and the
     * unified payment request ({@link RequestPaymentMessage}). Each emits the
     * receive event with {@code messageClass=BUTTON_NFM},
     * {@code bizPlatform=CLOUDAPI}, the sender's business JID, and the message's
     * WAM media type, then fans out the buyer-receive event.
     *
     * @implNote
     * This implementation classifies the message from its decoded content type
     * because Cobalt carries the order and payment payloads on typed
     * {@link LinkedMessageContainer} content rather than on the stanza envelope. The
     * {@code messageClassAttributes}, {@code entryPoint*}, {@code templateId},
     * {@code messageDepth}, and {@code threadIdHmac} properties are left absent
     * because the WA Web helpers ({@code P2XFunnelIdGenerator.genFunnelInfo},
     * the per-conversation CTWA entry-point state) that populate them are not
     * modelled in Cobalt; WA Web omits them when its helpers yield {@code null}.
     *
     * @param info   the decoded chat message info
     * @param stanza the parsed inbound stanza carrying the sender JID
     */
    @WhatsAppWebExport(moduleName = "WAWebOrderDetailsReceivedWamLogger", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPaymentInfoReceivedWamLogger", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebUprReceivedWamLogger", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitCommerceStructuredReceiveIfApplicable(ChatMessageInfo info, MessageReceiveStanza stanza) {
        if (!isCommerceStructuredMessage(info.message())) {
            return;
        }

        var mediaType = WamMsgUtils.getWamMediaType(info);
        var businessOwnerJid = stanza.senderJid().toUserJid().user();

        var builder = new StructuredMessageReceiveEventBuilder()
                .messageClass(StructuredMessageClass.BUTTON_NFM)
                .bizPlatform(BizPlatform.CLOUDAPI);
        if (mediaType != null) {
            builder.messageMediaType(mediaType);
        }
        if (businessOwnerJid != null) {
            builder.businessOwnerJid(businessOwnerJid);
        }

        wamService.commit(builder.build());
        emitStructuredMessageBuyerReceive(mediaType);
    }

    /**
     * Tests whether a decoded message container carries a commerce structured
     * payload that drives the buyer-side receive telemetry.
     *
     * <p>Returns {@code true} for an {@link OrderMessage}, a
     * {@link RequestPaymentMessage}, a {@link SendPaymentMessage}, a
     * {@link PaymentInviteMessage}, or an interactive native-flow message whose
     * button is named {@link #ORDER_DETAILS_FLOW}. The {@code order_status}
     * native flow is deliberately excluded here because it is handled by the
     * rich-order-status path
     * ({@link #emitRichOrderStatusIfApplicable(ChatMessageInfo, MessageReceiveStanza)}).
     *
     * @param container the decoded message container
     * @return {@code true} when the message is a commerce structured message
     */
    private static boolean isCommerceStructuredMessage(LinkedMessageContainer container) {
        var content = container.content();
        return content instanceof OrderMessage
                || content instanceof RequestPaymentMessage
                || content instanceof SendPaymentMessage
                || content instanceof PaymentInviteMessage
                || isNativeFlow(container, ORDER_DETAILS_FLOW);
    }

    /**
     * Tests whether a decoded message container is an interactive native-flow
     * message carrying a button with the given flow name.
     *
     * <p>Backs {@link #isCommerceStructuredMessage(LinkedMessageContainer)} and
     * {@link #orderStatusButtonJson(LinkedMessageContainer)}.
     *
     * @param container the decoded message container
     * @param flowName  the native-flow button name to match
     * @return {@code true} when a native-flow button with the name is present
     */
    private static boolean isNativeFlow(LinkedMessageContainer container, String flowName) {
        if (!(container.content() instanceof InteractiveMessage interactive)) {
            return false;
        }
        return interactive.content()
                .filter(InteractiveMessage.NativeFlowMessage.class::isInstance)
                .map(InteractiveMessage.NativeFlowMessage.class::cast)
                .map(InteractiveMessage.NativeFlowMessage::buttons)
                .orElseGet(List::of)
                .stream()
                .map(InteractiveMessage.NativeFlowMessage.NativeFlowButton::name)
                .flatMap(Optional::stream)
                .anyMatch(flowName::equals);
    }

    /**
     * Commits a
     * {@link com.github.auties00.cobalt.wire.wam.event.StructuredMessageBuyerReceiveEvent}
     * for a buyer receiving a structured commerce message.
     *
     * <p>Fired alongside every {@link StructuredMessageReceiveEventBuilder}
     * emission, gated on {@link ABProp#PAYMENTS_BR_P2M_BUYER_LOGGING_PHASE_2},
     * mirroring WA Web's paired emission from every structured-receive logger.
     *
     * @implNote
     * This implementation sets {@code messageClass=BUTTON_NFM}, the message's
     * WAM media type, and a compact {@code messageClassAttributes} JSON derived
     * from the media type. The {@code bizPlatform} and {@code messageInteraction}
     * properties are left absent because WA Web's buyer-receive call at the
     * native-flow receive site populates only class, media type, and attributes.
     *
     * @param mediaType the WAM media type of the received structured message,
     *                  or {@code null} when unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebBuyerEventLogger", exports = "submitBuyerReceiveEvent",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitStructuredMessageBuyerReceive(MediaType mediaType) {
        if (!abPropsService.getBool(ABProp.PAYMENTS_BR_P2M_BUYER_LOGGING_PHASE_2)) {
            return;
        }

        var builder = new StructuredMessageBuyerReceiveEventBuilder()
                .messageClass(StructuredMessageClass.BUTTON_NFM);
        if (mediaType != null) {
            builder.messageMediaType(mediaType);
            builder.messageClassAttributes("{\"media_type\":\"" + mediaType.name().toLowerCase() + "\"}");
        }

        wamService.commit(builder.build());
    }

    /**
     * Commits the rich-order-status WAM telemetry when the inbound message is
     * an interactive rich-order-status card.
     *
     * <p>Gated on {@link ABProp#UTILITY_ORDER_STATUS_LOGGING_ENABLED}. When the
     * message carries an {@code order_status} native-flow button this emits the
     * {@link com.github.auties00.cobalt.wire.wam.event.BusinessTemplateRichOrderStatusEvent}
     * template telemetry and, when a previously received rich-order-status
     * message for the same chat exists as a reference, the
     * {@link com.github.auties00.cobalt.wire.wam.event.PsRichOrderStatusMessageInconsistentPayloadReceivedEvent}
     * payload-consistency check.
     *
     * @param info   the decoded chat message info
     * @param stanza the parsed inbound stanza carrying the chat and sender JIDs
     */
    @WhatsAppWebExport(moduleName = "WAWebRichOrderStatusLogger", exports = "logRichOrderStatusInteraction",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebRichOrderStatusLogger", exports = "logRichOrderStatusInconsistencies",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitRichOrderStatusIfApplicable(ChatMessageInfo info, MessageReceiveStanza stanza) {
        if (!abPropsService.getBool(ABProp.UTILITY_ORDER_STATUS_LOGGING_ENABLED)) {
            return;
        }

        var orderStatusJson = orderStatusButtonJson(info.message()).orElse(null);
        if (orderStatusJson == null) {
            return;
        }

        emitBusinessTemplateRichOrderStatus(stanza);
        emitRichOrderStatusInconsistency(stanza, orderStatusJson);
    }

    /**
     * Extracts the {@code order_status} native-flow button JSON payload from a
     * decoded message container.
     *
     * <p>Backs {@link #emitRichOrderStatusIfApplicable(ChatMessageInfo, MessageReceiveStanza)}:
     * the returned JSON is both the marker that the message is a
     * rich-order-status card and the reference payload compared field-by-field
     * against the previously received card for the same chat.
     *
     * @param container the decoded message container
     * @return the {@code order_status} button JSON, or {@link Optional#empty()}
     *         when the message is not a rich-order-status card
     */
    private static Optional<String> orderStatusButtonJson(LinkedMessageContainer container) {
        if (!(container.content() instanceof InteractiveMessage interactive)) {
            return Optional.empty();
        }
        return interactive.content()
                .filter(InteractiveMessage.NativeFlowMessage.class::isInstance)
                .map(InteractiveMessage.NativeFlowMessage.class::cast)
                .map(InteractiveMessage.NativeFlowMessage::buttons)
                .orElseGet(List::of)
                .stream()
                .filter(button -> button.name().map(ORDER_STATUS_FLOW::equals).orElse(false))
                .map(InteractiveMessage.NativeFlowMessage.NativeFlowButton::buttonParamsJson)
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Commits a
     * {@link com.github.auties00.cobalt.wire.wam.event.BusinessTemplateRichOrderStatusEvent}
     * for a received rich-order-status business template message.
     *
     * <p>Carries the folder, contact, mute, read-receipt, and subscription
     * context WA Web reports when a rich-order-status template lands and its
     * interactive actions are resolved.
     *
     * @implNote
     * This implementation derives {@code businessJid} from the chat JID and
     * {@code chatsFolderType}/{@code isMuted} from the stored {@link com.github.auties00.cobalt.wire.linked.chat.Chat};
     * {@code contactType} is {@link ContactType#ENTERPRISE} for a
     * Facebook-hosted (Cloud API) business and {@link ContactType#SMB}
     * otherwise, and {@code isBizIntent} is always {@code true} because the
     * message is itself a business template. The {@code actionTypeRichOrderStatus}
     * is set to {@link #ORDER_STATUS_FLOW} because Cobalt is a headless library
     * with no rendered order-status affordance to attribute a click action to;
     * {@code readReceiptsEnabled} defaults to {@code true} (WA Web's default
     * privacy state) and {@code templateId} is omitted because the receive path
     * does not carry it.
     *
     * @param stanza the parsed inbound stanza carrying the chat JID
     */
    @WhatsAppWebExport(moduleName = "WAWebRichOrderStatusLogger", exports = "logRichOrderStatusInteraction",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitBusinessTemplateRichOrderStatus(MessageReceiveStanza stanza) {
        var chatJid = stanza.chatJid();
        var chat = whatsapp.store().chatStore().findChatByJid(chatJid).orElse(null);
        var contact = whatsapp.store().contactStore().findContactByJid(chatJid).orElse(null);
        var enterprise = contact != null && contact.hostedOnFacebook();

        var builder = new BusinessTemplateRichOrderStatusEventBuilder()
                .actionTypeRichOrderStatus(ORDER_STATUS_FLOW)
                .chatsFolderType(chat != null && chat.archived() ? ChatsFolderType.ARCHIVED : ChatsFolderType.INBOX)
                .contactType(enterprise ? ContactType.ENTERPRISE : ContactType.SMB)
                .isBizIntent(true)
                .isInsubContact(contact != null)
                .isMuted(chat != null && chat.mute().map(mute -> mute.isMuted()).orElse(false))
                .readReceiptsEnabled(true);

        var businessJid = chatJid.toUserJid().user();
        if (businessJid != null) {
            builder.businessJid(businessJid);
        }

        wamService.commit(builder.build());
    }

    /**
     * Commits a
     * {@link com.github.auties00.cobalt.wire.wam.event.PsRichOrderStatusMessageInconsistentPayloadReceivedEvent}
     * when a newer rich-order-status card disagrees field-by-field with the
     * previously received card for the same chat.
     *
     * <p>The current {@code order_status} button JSON is stored as the new
     * reference for the chat. When a prior reference exists the two payloads are
     * compared field-by-field and the per-field changed flags (currency, header
     * and item image, item name, item count, item price, item quantity, item
     * variant) are reported. When no prior reference exists nothing is emitted,
     * mirroring WA Web which needs the merged order-status {@code firstMessage}
     * to compare against.
     *
     * @implNote
     * This implementation compares the raw {@code order_status} button JSON
     * strings with a lightweight key-value extractor rather than a decoded order
     * model: {@code hasItemNumberChanged} compares the count of {@code quantity}
     * entries as a proxy for the item-count delta, {@code hasItemPriceChanged}
     * folds both the amount {@code value} and {@code offset} keys, and
     * {@code hasHeaderImageChanged} compares the header {@code header} key. Keys
     * absent from both payloads compare equal and therefore report no change,
     * which is the faithful outcome for a field the card never carried.
     *
     * @param stanza      the parsed inbound stanza carrying the chat JID
     * @param currentJson the current message's {@code order_status} button JSON
     */
    @WhatsAppWebExport(moduleName = "WAWebRichOrderStatusLogger", exports = "logRichOrderStatusInconsistencies",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitRichOrderStatusInconsistency(MessageReceiveStanza stanza, String currentJson) {
        var priorJson = lastOrderStatusPayloadByChat.put(stanza.chatJid(), currentJson);
        if (priorJson == null) {
            return;
        }

        var builder = new PsRichOrderStatusMessageInconsistentPayloadReceivedEventBuilder()
                .hasCurrencyChanged(fieldChanged(currentJson, priorJson, "currency"))
                .hasHeaderImageChanged(fieldChanged(currentJson, priorJson, "header"))
                .hasItemImageChanged(fieldChanged(currentJson, priorJson, "file_sha256"))
                .hasItemNameChanged(fieldChanged(currentJson, priorJson, "name"))
                .hasItemNumberChanged(countOccurrences(currentJson, "\"quantity\"") != countOccurrences(priorJson, "\"quantity\""))
                .hasItemPriceChanged(fieldChanged(currentJson, priorJson, "value") || fieldChanged(currentJson, priorJson, "offset"))
                .hasItemQuantityChanged(fieldChanged(currentJson, priorJson, "quantity"))
                .hasItemVariantChanged(fieldChanged(currentJson, priorJson, "variant"));

        var businessJid = stanza.chatJid().toUserJid().user();
        if (businessJid != null) {
            builder.businessJid(businessJid);
        }

        wamService.commit(builder.build());
    }

    /**
     * Returns whether the first value for the given key differs between two JSON
     * payloads.
     *
     * <p>Backs the per-field comparison in
     * {@link #emitRichOrderStatusInconsistency(MessageReceiveStanza, String)}.
     *
     * @param current the current payload JSON
     * @param prior   the reference payload JSON
     * @param key     the JSON key to compare
     * @return {@code true} when the extracted values differ
     */
    private static boolean fieldChanged(String current, String prior, String key) {
        return !Objects.equals(jsonRawValue(current, key), jsonRawValue(prior, key));
    }

    /**
     * Extracts the first scalar value for a key from a flat JSON string.
     *
     * <p>Reads the first {@code "key":value} occurrence, returning the unquoted
     * string body for a quoted value or the raw token for a numeric, boolean, or
     * {@code null} value.
     *
     * @implNote
     * This implementation is a deliberately minimal string scanner rather than a
     * full JSON parser: the rich-order-status button payload is a flat object
     * and only the first occurrence of each compared key is significant, so a
     * substring scan suffices and avoids pulling a JSON dependency into the
     * receive path.
     *
     * @param json the JSON string to scan; may be {@code null}
     * @param key  the object key to read
     * @return the extracted value, or {@code null} when the key is absent
     */
    private static String jsonRawValue(String json, String key) {
        if (json == null) {
            return null;
        }
        var needle = "\"" + key + "\"";
        var keyIndex = json.indexOf(needle);
        if (keyIndex < 0) {
            return null;
        }
        var colon = json.indexOf(':', keyIndex + needle.length());
        if (colon < 0) {
            return null;
        }
        var cursor = colon + 1;
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
            cursor++;
        }
        if (cursor >= json.length()) {
            return null;
        }
        if (json.charAt(cursor) == '"') {
            var end = json.indexOf('"', cursor + 1);
            return end < 0 ? null : json.substring(cursor + 1, end);
        }
        var start = cursor;
        while (cursor < json.length() && ",}] \t\r\n".indexOf(json.charAt(cursor)) < 0) {
            cursor++;
        }
        return json.substring(start, cursor);
    }

    /**
     * Counts the non-overlapping occurrences of a substring inside a string.
     *
     * <p>Backs the item-count delta proxy in
     * {@link #emitRichOrderStatusInconsistency(MessageReceiveStanza, String)}.
     *
     * @param haystack the string to scan; may be {@code null}
     * @param needle   the substring to count
     * @return the number of occurrences, or {@code 0} when {@code haystack} is
     *         {@code null}
     */
    private static int countOccurrences(String haystack, String needle) {
        if (haystack == null) {
            return 0;
        }
        var count = 0;
        var index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }

    /**
     * Commits a
     * {@link com.github.auties00.cobalt.wire.wam.event.PlaceholderActivityEvent} for
     * the insertion of the {@link ChatMessageInfo.StubType#CIPHERTEXT}
     * placeholder that stands in for an undecryptable message.
     *
     * <p>Runs from {@link #surfaceUndecryptableMessage(MessageReceiveStanza, WhatsAppMessageException.Receive, int)}
     * on the first failed delivery, mirroring WA Web's placeholder-add beacon.
     * The action is {@link PlaceholderAction#ADD}, the type is
     * {@link PlaceholderType#CIPHERTEXT}, and the add reason is mapped from the
     * decrypt-failure subtype.
     *
     * @implNote
     * This implementation populates the properties derivable from the parsed
     * {@link MessageReceiveStanza} alone: chat type, elapsed placeholder time
     * period, add reason, revoke flag, message type and media type, e2e sender
     * type, hosted-encryption flag, and the LID flag. The group-only
     * ({@code participantCount}, {@code deviceCount}, {@code deviceSizeBucket},
     * {@code typeOfGroup}) and {@code messageKeyHash} properties are left absent
     * because Cobalt has no equivalent of WA Web's
     * {@code WAWebWamGroupMetricCache}/{@code WAWebHandlePlaceholderMsgKeyHashUtils},
     * which WA Web also omits when those caches are unavailable.
     *
     * @param stanza    the parsed inbound stanza whose decrypt failed
     * @param exception the decrypt failure that triggered the placeholder
     */
    @WhatsAppWebExport(moduleName = "WAWebHandlePlaceholderWam", exports = "postPlaceholderActivityAddEvent",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitPlaceholderActivityAdd(MessageReceiveStanza stanza, WhatsAppMessageException.Receive exception) {
        var builder = new PlaceholderActivityEventBuilder()
                .placeholderActionInd(PlaceholderAction.ADD)
                .placeholderTypeInd(PlaceholderType.CIPHERTEXT)
                .placeholderChatTypeInd(mapPlaceholderChatType(stanza.chatJid()))
                .placeholderAddReason(mapPlaceholderReason(exception))
                .messageIsRevoke(isRevokeStanza(stanza))
                .isLid(stanza.senderJid().hasLidServer());

        var elapsedSeconds = Instant.now().getEpochSecond() - stanza.timestamp().getEpochSecond();
        builder.placeholderTimePeriod(Math.max(elapsedSeconds, 0));

        var messageType = WamMsgUtils.getWamMessageTypeFromStanzaType(stanza.messageType());
        if (messageType != null) {
            builder.messageType(messageType);
        }

        var mediaType = mapEncMediaTypeToWamMediaType(
                firstEncMediaType(stanza), stanza.stanzaType(), stanza.pollType().orElse(null));
        if (mediaType != null) {
            builder.messageMediaType(mediaType);
        }

        var selfJid = whatsapp.store().accountStore().jid().orElse(null);
        var senderType = WamMsgUtils.getWamE2eSenderType(stanza.senderJid(), selfJid);
        if (senderType != null) {
            builder.e2eSenderType(senderType);
        }

        if (stanza.senderJid().hasHostedServer() || stanza.senderJid().hasHostedLidServer()) {
            builder.encryptionType(EncryptionTypeCode.COEX);
        }

        wamService.commit(builder.build());
    }

    /**
     * Maps a chat JID onto its WAM {@link PlaceholderChatType} bucket.
     *
     * <p>Drives the {@code placeholderChatTypeInd} property on the
     * {@link com.github.auties00.cobalt.wire.wam.event.PlaceholderActivityEvent}.
     *
     * @implNote
     * This implementation checks the JID flavours in the same order as WA Web's
     * {@code WAWebHandlePlaceholderWam} chat-type resolver (status first, then
     * group/community, broadcast, newsletter, user/LID) and falls back to
     * {@link PlaceholderChatType#OTHER}.
     *
     * @param chatJid the chat JID
     * @return the matching {@link PlaceholderChatType}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandlePlaceholderWam", exports = "getPlaceholderChatType",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static PlaceholderChatType mapPlaceholderChatType(Jid chatJid) {
        if (chatJid.isStatusBroadcastAccount()) {
            return PlaceholderChatType.STATUS;
        }
        if (chatJid.hasGroupOrCommunityServer()) {
            return PlaceholderChatType.GROUP;
        }
        if (chatJid.hasBroadcastServer()) {
            return PlaceholderChatType.BROADCAST;
        }
        if (chatJid.hasNewsletterServer()) {
            return PlaceholderChatType.CHANNEL;
        }
        if (chatJid.hasUserServer() || chatJid.hasLidServer()) {
            return PlaceholderChatType.INDIVIDUAL;
        }
        return PlaceholderChatType.OTHER;
    }

    /**
     * Maps a receive-failure subtype onto its WAM {@link PlaceholderReasonType}
     * add reason.
     *
     * <p>Drives the {@code placeholderAddReason} property on the
     * {@link com.github.auties00.cobalt.wire.wam.event.PlaceholderActivityEvent}.
     *
     * @implNote
     * This implementation maps the Signal-level decrypt failures Cobalt
     * distinguishes onto their WA Web reason counterparts and falls back to
     * {@link PlaceholderReasonType#OTHER} for any subtype without a dedicated
     * reason.
     *
     * @param exception the decrypt failure
     * @return the matching {@link PlaceholderReasonType}; never {@code null}
     */
    private static PlaceholderReasonType mapPlaceholderReason(WhatsAppMessageException.Receive exception) {
        if (exception instanceof WhatsAppMessageException.Receive.NoSession) {
            return PlaceholderReasonType.SIGNAL_NO_SESSION;
        }
        if (exception instanceof WhatsAppMessageException.Receive.InvalidKey) {
            return PlaceholderReasonType.SIGNAL_INVALID_KEY;
        }
        if (exception instanceof WhatsAppMessageException.Receive.InvalidKeyId) {
            return PlaceholderReasonType.SIGNAL_INVALID_KEY_ID;
        }
        if (exception instanceof WhatsAppMessageException.Receive.InvalidMessage) {
            return PlaceholderReasonType.SIGNAL_INVALID_MESSAGE;
        }
        if (exception instanceof WhatsAppMessageException.Receive.BadMac) {
            return PlaceholderReasonType.SIGNAL_BAD_MAC;
        }
        if (exception instanceof WhatsAppMessageException.Receive.FutureMessage) {
            return PlaceholderReasonType.SIGNAL_FUTURE_MESSAGE;
        }
        if (exception instanceof WhatsAppMessageException.Receive.InvalidSignature) {
            return PlaceholderReasonType.SIGNAL_INVALID_SIGNATURE;
        }
        if (exception instanceof WhatsAppMessageException.Receive.BroadcastEphemeralSettings) {
            return PlaceholderReasonType.BAD_EPHEMERAL_SETTING;
        }
        return PlaceholderReasonType.OTHER;
    }

    /**
     * Returns whether a parsed stanza carries a message revocation edit.
     *
     * <p>Drives the {@code messageIsRevoke} property on the
     * {@link com.github.auties00.cobalt.wire.wam.event.PlaceholderActivityEvent} when
     * the underlying message body is not yet decrypted.
     *
     * @param stanza the parsed inbound stanza
     * @return {@code true} when the stanza's {@code edit} attribute is a sender
     *         or admin revoke
     */
    private static boolean isRevokeStanza(MessageReceiveStanza stanza) {
        var editAttr = stanza.editAttribute();
        return editAttr == MessageReceiveStanza.EDIT_SENDER_REVOKE
                || editAttr == MessageReceiveStanza.EDIT_ADMIN_REVOKE;
    }

    /**
     * Returns the first {@code mediatype} attribute among a stanza's
     * {@code <enc>} children.
     *
     * <p>Backs the {@code messageMediaType} derivation shared by the placeholder
     * and offline-count metrics.
     *
     * @param stanza the parsed inbound stanza
     * @return the first non-{@code null} enc media type, or {@code null} when
     *         none carry one
     */
    private static String firstEncMediaType(MessageReceiveStanza stanza) {
        return stanza.encs().stream()
                .map(enc -> enc.encMediaType().orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Commits an
     * {@link com.github.auties00.cobalt.wire.wam.event.IncomingMessageDropEvent} with
     * {@link MessageDropReasonType#DB_OPERATION_FAILED} when persisting a
     * decoded message row fails.
     *
     * <p>Emitted when {@link #storeIncomingMessage(LinkedMessageInfo)} throws while
     * writing an already-decrypted message, mirroring WA Web's per-row drop for
     * a failed message persist.
     *
     * @implNote
     * This implementation populates the {@code offline}, {@code offlineCount},
     * {@code e2eDestination}, and {@code isLid} properties derivable from the
     * parsed stanza; the ciphertext, retry, and Signal-scope properties are
     * absent because the failure occurs after decryption, past the point where
     * those per-slot signals apply.
     *
     * @param stanza the parsed inbound stanza whose persist failed
     */
    @WhatsAppWebExport(moduleName = "WAWebPostIncomingMessageDropMetric",
            exports = "postIncomingMessageDropDBOperationFailedForMsgRows",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitIncomingMessageDropForDbFailure(MessageReceiveStanza stanza) {
        var builder = new IncomingMessageDropEventBuilder()
                .messageDropReason(MessageDropReasonType.DB_OPERATION_FAILED)
                .offline(stanza.isOffline())
                .isLid(stanza.senderJid().hasLidServer());

        stanza.offline().ifPresent(raw -> {
            try {
                builder.offlineCount(Integer.parseInt(raw));
            } catch (NumberFormatException _) {
            }
        });

        var destination = mapDestination(stanza);
        if (destination != null) {
            builder.e2eDestination(destination);
        }

        wamService.commit(builder.build());
    }

    /**
     * Commits a
     * {@link com.github.auties00.cobalt.wire.wam.event.WebcMessageProcessingPerfEvent}
     * carrying the per-stage processing durations of an inbound offline message.
     *
     * <p>Only offline messages contribute; live-delivered messages return
     * without emitting. The five measured stages (pre-processing, parsing,
     * processing, DB storing, post-processing) are reported as elapsed
     * milliseconds.
     *
     * @implNote
     * This implementation emits one event per offline message rather than one
     * aggregated event per offline-resume batch: Cobalt's message handler has no
     * equivalent of WA Web's {@code WAWebEventsWaitForOfflineDeliveryEnd}
     * barrier at which {@code WAWebOfflineResumeMsgProcessReporter} flushes the
     * summed cache, so the reporter's batch cadence is unreachable from this
     * single dispatch path. The {@code decryptionT}, {@code lidProcessingT}, and
     * {@code reportTokenValidationT} sub-stages are reported as {@code 0}
     * because Cobalt folds Signal decryption into the single
     * {@link MessageService#process(Stanza)} call and does not time the LID and
     * reporting-token sub-stages separately; WA Web likewise reports {@code 0}
     * for a stage whose marker never fired.
     *
     * @param stanza             the parsed inbound stanza
     * @param preProcessingNanos elapsed nanoseconds from stanza receipt to parse
     * @param parsingNanos       elapsed nanoseconds parsing the stanza
     * @param processingNanos    elapsed nanoseconds decrypting and decoding
     * @param dbStoringNanos     elapsed nanoseconds persisting the message
     * @param postProcessingNanos elapsed nanoseconds of listener and metric fan-out
     */
    @WhatsAppWebExport(moduleName = "WAWebOfflineResumeMsgProcessReporter", exports = "msgProcessReporter",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void maybeEmitMessageProcessingPerf(
            MessageReceiveStanza stanza,
            long preProcessingNanos,
            long parsingNanos,
            long processingNanos,
            long dbStoringNanos,
            long postProcessingNanos
    ) {
        if (!stanza.isOffline()) {
            return;
        }

        wamService.commit(new WebcMessageProcessingPerfEventBuilder()
                .isOffline(true)
                .preProcessingT(toMillis(preProcessingNanos))
                .parsingT(toMillis(parsingNanos))
                .processingT(toMillis(processingNanos))
                .dbStoringT(toMillis(dbStoringNanos))
                .postProcessingT(toMillis(postProcessingNanos))
                .decryptionT(0)
                .lidProcessingT(0)
                .reportTokenValidationT(0)
                .build());
    }

    /**
     * Converts an elapsed-nanoseconds duration into non-negative whole
     * milliseconds.
     *
     * <p>Backs the timer fields of
     * {@link #maybeEmitMessageProcessingPerf(MessageReceiveStanza, long, long, long, long, long)}.
     *
     * @param nanos the elapsed nanoseconds
     * @return the duration in milliseconds, clamped at {@code 0}
     */
    private static long toMillis(long nanos) {
        return Math.max(0, nanos) / 1_000_000L;
    }

    /**
     * Commits a
     * {@link com.github.auties00.cobalt.wire.wam.event.MessageReceiveEvent} for a
     * successfully processed newsletter message.
     *
     * <p>Populates only the properties Cobalt can derive for a newsletter
     * entry: typing, content, view-once, reply/forward, chat-origins, and the
     * timer fields.
     *
     * @implNote
     * This implementation skips the addressing, LID, ephemerality,
     * hosted-encryption, and group-only branches because newsletter messages
     * never carry those signals. The three {@code messageReceiveT*} timer
     * fields are zeroed, matching the newsletter invocation of
     * {@code logReceivedMessagesInWAM} which omits
     * {@code clientReceivedTsMillis} and {@code tsMillis} for the channel
     * pipeline.
     *
     * @param info the decoded newsletter message info
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleNewsletterMsg", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebLogReceivedMessages", exports = "logReceivedMessagesInWAM",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitMessageReceiveForNewsletterMessage(NewsletterMessageInfo info) {
        var builder = new MessageReceiveEventBuilder();

        var parent = info.key().parentJid().orElse(null);
        builder.messageType(WamMsgUtils.getWamMessageType(parent));

        builder.messageMediaType(WamMsgUtils.getWamMediaType(info.message()));

        builder.messageIsOffline(false);

        builder.isViewOnce(isViewOnceMessage(info.message()));
        var contextInfo = extractContextInfo(info.message()).orElse(null);
        if (contextInfo != null) {
            builder.isForwardedForward(contextInfo.forwardingScore().orElse(0) > 1);
            builder.isAReply(contextInfo.quotedMessageId().isPresent());
        } else {
            builder.isForwardedForward(false);
            builder.isAReply(false);
        }

        builder.chatOrigins(ChatOriginsType.OTHERS);

        builder.messageReceiveT0(Instant.ofEpochMilli(0));
        builder.messageReceiveT1(Instant.ofEpochMilli(0));
        builder.messageReceiveT2(Instant.ofEpochMilli(0));

        wamService.commit(builder.build());
    }

    /**
     * Tests whether the decoded {@link LinkedMessageContainer} carries any of the
     * view-once wrappers WhatsApp ever shipped.
     *
     * <p>Drives the {@code isViewOnce} property on the WAM
     * {@link com.github.auties00.cobalt.wire.wam.event.MessageReceiveEvent}
     * builder.
     *
     * @implNote
     * This implementation delegates to
     * {@link LinkedMessageContainer#futureProofContentType()} and checks for
     * {@link FutureProofMessageType#VIEW_ONCE}, which folds the three
     * historical view-once message shapes ({@code viewOnceMessage},
     * {@code viewOnceMessageV2}, {@code viewOnceMessageV2Extension}) into one
     * branch on the Cobalt side.
     *
     * @param container the decoded message container; may be {@code null}
     * @return {@code true} when the container carries a view-once payload;
     *         {@code false} otherwise, including for {@code null} inputs
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgGetters", exports = "getIsViewOnce",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isViewOnceMessage(LinkedMessageContainer container) {
        if (container == null) {
            return false;
        }
        var type = container.futureProofContentType();
        return type == FutureProofMessageType.VIEW_ONCE;
    }

    /**
     * Extracts the {@link ContextInfo} from a {@link LinkedMessageContainer} when
     * its content is a {@link ContextualMessage}.
     *
     * <p>Powers the {@code isForwardedForward}, {@code isAReply}, and
     * ephemerality fields on the WAM
     * {@link com.github.auties00.cobalt.wire.wam.event.MessageReceiveEvent}.
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} when the container
     * is {@code null}, when the content is not a {@link ContextualMessage}, or
     * when the contextual message itself has no embedded context info,
     * mirroring the WA Web {@code getNumTimesForwarded} / {@code getIsReply}
     * accessors that silently treat absent {@code contextInfo} as a no-op.
     *
     * @param container the decoded message container; may be {@code null}
     * @return the resolved {@link ContextInfo}, or {@link Optional#empty()}
     *         when no context info is attached
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgGetters", exports = "getNumTimesForwarded",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMsgGetters", exports = "getIsReply",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static Optional<ContextInfo> extractContextInfo(LinkedMessageContainer container) {
        if (container == null) {
            return Optional.empty();
        }
        if (container.content() instanceof ContextualMessage contextual) {
            return contextual.contextInfo();
        }
        return Optional.empty();
    }

    /**
     * Copies the three WAM ephemerality fields off a
     * {@link ChatDisappearingMode} onto a {@link MessageReceiveEventBuilder}.
     *
     * <p>Sets the {@code disappearingChatInitiator},
     * {@code ephemeralityTriggerAction}, and {@code ephemeralityInitiator}
     * properties on the WAM
     * {@link com.github.auties00.cobalt.wire.wam.event.MessageReceiveEvent}.
     *
     * @implNote
     * This implementation translates each Cobalt-side enum constant
     * ({@link ChatDisappearingMode.Initiator},
     * {@link ChatDisappearingMode.Trigger}) into its WAM counterpart via an
     * exhaustive {@code switch}. The {@code initiatedByMe} boolean is
     * collapsed onto two {@link EphemeralityInitiatorType} values, matching
     * the binary choice WA Web makes at this call site.
     *
     * @param builder the event builder to populate in place
     * @param mode    the disappearing-mode descriptor carried by the inbound
     *                message's context info
     */
    @WhatsAppWebExport(moduleName = "WAWebEphemeralityWAMUtils", exports = "getWamDisappearingModeInitiator",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebEphemeralityWAMUtils", exports = "getWamDisappearingModeTrigger",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebEphemeralityWAMUtils", exports = "getWamDisappearingModeInitiatedByMe",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static void applyDisappearingMode(MessageReceiveEventBuilder builder, ChatDisappearingMode mode) {
        mode.initiator().ifPresent(initiator -> {
            var mapped = switch (initiator) {
                case CHANGED_IN_CHAT -> DisappearingChatInitiatorType.CHAT;
                case INITIATED_BY_ME -> DisappearingChatInitiatorType.INITIATED_BY_ME;
                case INITIATED_BY_OTHER -> DisappearingChatInitiatorType.INITIATED_BY_OTHER;
                case BIZ_UPGRADE_FB_HOSTING -> DisappearingChatInitiatorType.BIZ_UPGRADE_FB_HOSTING;
            };
            builder.disappearingChatInitiator(mapped);
        });

        mode.trigger().ifPresent(trigger -> {
            var mapped = switch (trigger) {
                case UNKNOWN -> EphemeralityTriggerActionType.UNKNOWN;
                case CHAT_SETTING -> EphemeralityTriggerActionType.CHAT_SETTINGS;
                case ACCOUNT_SETTING -> EphemeralityTriggerActionType.ACCOUNT_SETTINGS;
                case BULK_CHANGE -> EphemeralityTriggerActionType.BULK_CHANGE;
                case BIZ_SUPPORTS_FB_HOSTING -> EphemeralityTriggerActionType.BIZ_SUPPORTS_FB_HOSTING;
                case UNKNOWN_GROUPS -> EphemeralityTriggerActionType.UNKNOWN_GROUP;
            };
            builder.ephemeralityTriggerAction(mapped);
        });

        builder.ephemeralityInitiator(mode.initiatedByMe()
                ? EphemeralityInitiatorType.INITIATED_BY_ME
                : EphemeralityInitiatorType.INITIATED_BY_OTHER);
    }

    /**
     * Resolves the {@link EditType} for a parsed inbound stanza, combining
     * the stanza-level {@code edit} attribute with the embedded
     * {@link ProtocolMessage.Type} subtype.
     *
     * <p>Drives the {@code editType} property on the
     * {@link com.github.auties00.cobalt.wire.wam.event.MessageReceiveEvent}.
     *
     * @implNote
     * This implementation checks the stanza's {@code edit} attribute first
     * ({@code EDIT_MESSAGE}, {@code EDIT_PIN}, {@code EDIT_SENDER_REVOKE},
     * {@code EDIT_ADMIN_REVOKE}) and falls back to the protocol-message type
     * so revokes carried inside a protocol message (typical for sender
     * revokes) still surface correctly.
     *
     * @param stanza the parsed inbound stanza
     * @param info   the decoded chat message info
     * @return the resolved {@link EditType}, or {@code null} when the message
     *         is neither edited nor revoked
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgGetters", exports = "getWamEditType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static EditType resolveEditType(MessageReceiveStanza stanza, ChatMessageInfo info) {
        var editAttr = stanza.editAttribute();
        if (editAttr == MessageReceiveStanza.EDIT_MESSAGE) {
            return EditType.EDITED;
        }
        if (editAttr == MessageReceiveStanza.EDIT_PIN) {
            return EditType.PIN;
        }
        if (editAttr == MessageReceiveStanza.EDIT_SENDER_REVOKE) {
            return EditType.SENDER_REVOKE;
        }
        if (editAttr == MessageReceiveStanza.EDIT_ADMIN_REVOKE) {
            return EditType.ADMIN_REVOKE;
        }
        if (info.message().content() instanceof ProtocolMessage protocol) {
            var protocolType = protocol.type().orElse(null);
            if (protocolType == ProtocolMessage.Type.REVOKE) {
                return EditType.SENDER_REVOKE;
            }
            if (protocolType == ProtocolMessage.Type.MESSAGE_EDIT) {
                return EditType.EDITED;
            }
        }
        return null;
    }

    /**
     * Resolves the {@link RevokeType} for a parsed inbound stanza when the
     * message is a revoke.
     *
     * <p>Drives the {@code revokeType} property on the
     * {@link com.github.auties00.cobalt.wire.wam.event.MessageReceiveEvent}.
     *
     * @implNote
     * This implementation routes the stanza's {@code edit} attribute to
     * {@link RevokeType#ADMIN} for {@code admin_revoke} and
     * {@link RevokeType#SENDER} for {@code sender_revoke}, then falls back to
     * {@link RevokeType#SENDER} when the embedded protocol message itself is a
     * {@link ProtocolMessage.Type#REVOKE}.
     *
     * @param stanza the parsed inbound stanza
     * @param info   the decoded chat message info
     * @return the matching {@link RevokeType}, or {@link Optional#empty()}
     *         when the message is not a revoke
     */
    @WhatsAppWebExport(moduleName = "WAWebLogReceivedMessages", exports = "logReceivedMessagesInWAM",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static Optional<RevokeType> resolveRevokeType(MessageReceiveStanza stanza, ChatMessageInfo info) {
        var editAttr = stanza.editAttribute();
        if (editAttr == MessageReceiveStanza.EDIT_ADMIN_REVOKE) {
            return Optional.of(RevokeType.ADMIN);
        }
        if (editAttr == MessageReceiveStanza.EDIT_SENDER_REVOKE) {
            return Optional.of(RevokeType.SENDER);
        }
        if (info.message().content() instanceof ProtocolMessage protocol
                && protocol.type().orElse(null) == ProtocolMessage.Type.REVOKE) {
            return Optional.of(RevokeType.SENDER);
        }
        return Optional.empty();
    }

    /**
     * Converts the stanza-level {@code addressing_mode} attribute into its
     * WAM {@link AddressingMode} counterpart.
     *
     * <p>Drives the {@code serverAddressingMode} property on the
     * {@link com.github.auties00.cobalt.wire.wam.event.MessageReceiveEvent}.
     *
     * @implNote
     * This implementation accepts only the two values WA Web emits
     * ({@code "pn"} and {@code "lid"}) and returns {@link Optional#empty()}
     * for everything else, including {@code null} inputs.
     *
     * @param raw the raw attribute value ({@code "pn"} or {@code "lid"}); may
     *            be {@code null}
     * @return the matching enum constant, or {@link Optional#empty()} for
     *         unrecognised or {@code null} inputs
     */
    @WhatsAppWebExport(moduleName = "WAWebWamAddressingModeUtils", exports = "getWamAddressingModeFromString",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static Optional<AddressingMode> mapAddressingMode(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return switch (raw) {
            case "pn" -> Optional.of(AddressingMode.PN);
            case "lid" -> Optional.of(AddressingMode.LID);
            default -> Optional.empty();
        };
    }

    /**
     * Resolves the {@link MessageDropReasonType} for a given inbound stanza
     * and receive exception.
     *
     * <p>Backs {@link #emitIncomingMessageDropFromStanza}; the resolved reason
     * becomes the {@code messageDropReason} property of the committed
     * {@link com.github.auties00.cobalt.wire.wam.event.IncomingMessageDropEvent}.
     *
     * @implNote
     * This implementation reproduces three special cases in order:
     * status-broadcast stanzas older than 24 hours always map to
     * {@link MessageDropReasonType#EXPIRED}; a {@code null} {@code exception}
     * maps to {@link MessageDropReasonType#INTERNAL_ERROR};
     * {@link WhatsAppMessageException.Receive.InvalidProtobuf} and
     * {@link WhatsAppMessageException.Receive.InvalidDeviceSentMessage} both
     * map to {@link MessageDropReasonType#INVALID_PROTOBUF}; everything else
     * (including the {@link WhatsAppMessageException.Receive.InvalidMessage}
     * branch that WA Web's
     * {@code WAWebPostIncomingMessageDropMetric.postIncomingMessageDropInvalidHostedCompanionStanza}
     * special-cases) collapses to {@link MessageDropReasonType#INVALID_STANZA}.
     * The hosted-companion sub-case is not yet differentiated; see the TODO
     * below.
     *
     * @param stanza    the parsed inbound stanza
     * @param exception the receive exception, or {@code null} for an internal
     *                  unhandled error
     * @return the resolved {@link MessageDropReasonType}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static MessageDropReasonType resolveDropReason(
            MessageReceiveStanza stanza,
            WhatsAppMessageException.Receive exception
    ) {
        if (stanza.chatJid().isStatusBroadcastAccount()) {
            var age = ChronoUnit.HOURS.between(stanza.timestamp(), Instant.now());
            if (age > 24) {
                return MessageDropReasonType.EXPIRED;
            }
        }

        if (exception == null) {
            return MessageDropReasonType.INTERNAL_ERROR;
        }

        if (exception instanceof WhatsAppMessageException.Receive.InvalidProtobuf
                || exception instanceof WhatsAppMessageException.Receive.InvalidDeviceSentMessage) {
            return MessageDropReasonType.INVALID_PROTOBUF;
        }

        // TODO: distinguish the hosted-companion rejection inside
        //       WhatsAppMessageException.Receive.InvalidMessage so it
        //       maps to MessageDropReasonType.INVALID_HOSTED_COMPANION_STANZA
        //       instead of the generic INVALID_STANZA fallback.
        if (exception instanceof WhatsAppMessageException.Receive.InvalidMessage) {
            return MessageDropReasonType.INVALID_STANZA;
        }

        return MessageDropReasonType.INVALID_STANZA;
    }

    /**
     * Maps a Cobalt {@link MessageEncryptionType} onto its WAM
     * {@link E2eCiphertextType} counterpart.
     *
     * <p>Drives the {@code e2eCiphertextType} property on the
     * {@link com.github.auties00.cobalt.wire.wam.event.IncomingMessageDropEvent}.
     *
     * @param type the Signal-level ciphertext type lifted from the first
     *             {@code <enc>} child of the inbound stanza
     * @return the matching WAM {@link E2eCiphertextType}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "getMetricE2eCiphertextType",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static E2eCiphertextType mapCiphertextTypeForDrop(
            MessageEncryptionType type
    ) {
        return switch (type) {
            case MSG -> E2eCiphertextType.MESSAGE;
            case PKMSG -> E2eCiphertextType.PREKEY_MESSAGE;
            case SKMSG -> E2eCiphertextType.SENDER_KEY_MESSAGE;
            case MSMSG -> E2eCiphertextType.MESSAGE_SECRET_MESSAGE;
        };
    }

    /**
     * Classifies the stanza's chat JID into its WAM {@link E2eDestination}
     * bucket.
     *
     * <p>Drives the {@code e2eDestination} property on the
     * {@link com.github.auties00.cobalt.wire.wam.event.IncomingMessageDropEvent}.
     *
     * @implNote
     * This implementation checks the chat-JID flavours in the same order as
     * WA Web (status broadcast first, then group/community, then broadcast
     * list, then newsletter, then user/LID) and returns {@code null} for JIDs
     * that do not fall in any tracked bucket; the absent property mirrors WA
     * Web's {@code undefined}-fallthrough behaviour.
     *
     * @param stanza the parsed inbound stanza
     * @return the matching {@link E2eDestination}, or {@code null} when the
     *         chat JID does not match any tracked bucket
     */
    @WhatsAppWebExport(moduleName = "WAWebGetMetricE2eDestination", exports = "getMetricE2eDestination",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static E2eDestination mapDestination(MessageReceiveStanza stanza) {
        var jid = stanza.chatJid();
        if (jid.isStatusBroadcastAccount()) {
            return E2eDestination.STATUS;
        }
        if (jid.hasGroupOrCommunityServer()) {
            return E2eDestination.GROUP;
        }
        if (jid.hasBroadcastServer()) {
            return E2eDestination.LIST;
        }
        if (jid.hasNewsletterServer()) {
            return E2eDestination.CHANNEL;
        }
        if (jid.hasUserServer() || jid.hasLidServer()) {
            return E2eDestination.INDIVIDUAL;
        }
        return null;
    }

    /**
     * Sends a negative acknowledgment ({@code <ack error="...">}) for an
     * inbound message stanza.
     *
     * <p>Emitted on parse failures ({@code "487"}) and on unhandled runtime
     * failures ({@code "500"}) to signal to the server that the client cannot
     * process the stanza.
     *
     * @implNote
     * This implementation delegates the string-to-reason translation to
     * {@link #parseErrorReason(String)} so a malformed value falls back to
     * {@link NackReason#UNHANDLED_ERROR} rather than throwing.
     *
     * @param stanza      the inbound message stanza to nack
     * @param errorCode the NACK error code, in string form (matches the
     *                  integer constants on WA Web's {@code NackReason})
     */
    private void sendNack(Stanza stanza, String errorCode) {
        ackSender.sendNack(AckClass.MESSAGE, stanza, parseErrorReason(errorCode));
    }

    /**
     * Parses a NACK error code string into its typed {@link NackReason}.
     *
     * <p>Used by {@link #sendNack(Stanza, String)} for the outbound
     * {@code <ack>} path.
     *
     * @implNote
     * This implementation falls back to {@link NackReason#UNHANDLED_ERROR}
     * when the parsed integer code matches no known reason, so the calling
     * code always gets a usable reason.
     *
     * @param value the raw error code, as carried on the inbound stanza or on
     *              a receive exception
     * @return the parsed {@link NackReason}, or {@link NackReason#UNHANDLED_ERROR}
     *         when {@code value} maps to no known reason
     */
    private static NackReason parseErrorReason(String value) {
        var reason = NackReason.fromCode(parseErrorCode(value));
        return reason != null ? reason : NackReason.UNHANDLED_ERROR;
    }

    /**
     * Parses an error code carried on an inbound stanza or exception into its
     * raw integer form.
     *
     * <p>Used by {@link #parseErrorReason(String)} for the outbound
     * {@code <ack>} path and directly by the {@link MessageReceiptHandler} for
     * the outbound {@code <receipt>} nack path, which carries the raw integer
     * rather than a typed reason.
     *
     * @implNote
     * This implementation falls back to {@code 500} on a malformed value
     * rather than propagating the {@link NumberFormatException}, so the caller
     * always gets a usable integer.
     *
     * @param value the raw error code string
     * @return the parsed integer error code, or {@code 500} when {@code value}
     *         is not a valid integer
     */
    private static int parseErrorCode(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            return 500;
        }
    }

    /**
     * Persists a freshly processed inbound {@link LinkedMessageInfo} into the
     * appropriate store collection.
     *
     * <p>Routes the message to one of three buckets: newsletter messages land
     * on the per-channel newsletter entry, status broadcast messages land on
     * the status collection, and normal chat messages land on the per-chat
     * history. Chat- and newsletter-level metadata (unread count,
     * conversation timestamp, last-message timestamp, timestamp) is updated
     * in place; unread counters are bumped only for messages the current
     * account did not send.
     *
     * @implNote
     * This implementation lazily creates the parent chat or newsletter entry
     * on first contact.
     *
     * @param info the typed inbound message to persist
     */
    private void storeIncomingMessage(LinkedMessageInfo info) {
        switch (info) {
            case NewsletterMessageInfo newsletterInfo -> {
                var newsletterJid = newsletterInfo.key().parentJid().orElse(null);
                if (newsletterJid == null) {
                    return;
                }

                var newsletter = whatsapp.store().chatStore().findNewsletterByJid(newsletterJid)
                        .orElseGet(() -> whatsapp.store().chatStore().addNewNewsletter(newsletterJid));
                newsletter.setTimestamp(newsletterInfo.timestamp().orElse(null));
                if (!newsletterInfo.key().fromMe()) {
                    newsletter.setUnreadMessagesCount(newsletter.unreadMessagesCount() + 1);
                }
                newsletter.addMessage(newsletterInfo);
            }
            case ChatMessageInfo chatInfo -> {
                if (isStatusMessage(chatInfo)) {
                    whatsapp.store().chatStore().addStatus(chatInfo);
                    return;
                }

                var chatJid = chatInfo.key().parentJid().orElse(null);
                if (chatJid == null) {
                    return;
                }

                var chat = whatsapp.store().chatStore().findChatByJid(chatJid)
                        .orElseGet(() -> whatsapp.store().chatStore().addNewChat(chatJid));
                var timestamp = chatInfo.timestamp().orElse(null);
                chat.setLastMsgTimestamp(timestamp);
                chat.setConversationTimestamp(timestamp);
                if (!chatInfo.key().fromMe()) {
                    chat.setUnreadCount(chat.unreadCount().orElse(0) + 1);
                }
                chat.addMessage(chatInfo);
            }
        }
    }

    /**
     * Fans the {@code onNewMessage}, {@code onNewStatus}, and
     * {@code onMessageReply} callbacks out to every registered listener.
     *
     * <p>The single broadcast point through which user code observes inbound
     * messages. Status broadcasts additionally fire {@code onNewStatus};
     * replies additionally fire {@code onMessageReply} with the resolved
     * quoted message.
     *
     * @implNote
     * This implementation runs each listener on its own virtual thread so
     * that one slow or blocking listener cannot stall the socket-stream
     * dispatch loop or starve other listeners.
     *
     * @param info          the inbound message info
     * @param quotedMessage the message {@code info} quotes, or
     *                      {@link Optional#empty()} when none was resolved
     */
    private void notifyMessageReceived(LinkedMessageInfo info, Optional<? extends LinkedMessageInfo> quotedMessage) {
        var statusMessage = isStatusMessage(info);
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof NewMessageListener typed) {
                Thread.startVirtualThread(() -> typed.onNewMessage(whatsapp, info));
            }
            if (statusMessage && info instanceof ChatMessageInfo chatMessageInfo
                    && listener instanceof LinkedNewStatusListener typed) {
                Thread.startVirtualThread(() -> typed.onNewStatus(whatsapp, chatMessageInfo));
            }
            if (quotedMessage.isPresent() && listener instanceof LinkedMessageReplyListener typed) {
                var quoted = quotedMessage.get();
                Thread.startVirtualThread(() -> typed.onMessageReply(whatsapp, info, quoted));
            }
        }
    }

    /**
     * Tests whether the inbound message targets the status broadcast account.
     *
     * <p>Used by {@link #storeIncomingMessage} to route status broadcasts to
     * the dedicated status collection rather than to a per-chat history, and
     * by {@link #notifyMessageReceived(LinkedMessageInfo, Optional)} to decide
     * whether to fire {@code onNewStatus}.
     *
     * @param info the inbound message info
     * @return {@code true} when the {@link LinkedMessageInfo}'s parent JID is the
     *         status broadcast account; {@code false} otherwise, including
     *         when the parent JID is absent
     */
    private boolean isStatusMessage(LinkedMessageInfo info) {
        return info.key()
                .parentJid()
                .map(Jid::isStatusBroadcastAccount)
                .orElse(false);
    }

    /**
     * Resolves a previously stored orphan payment notification when the
     * message it referenced has finally arrived.
     *
     * <p>Implements the back-half of orphan-payment reconciliation: if a
     * payment {@code <transaction>} stanza arrived before its referenced
     * {@code <message>}, the notification handler buffered it; when the late
     * message lands here it is matched and the buffered payment is replayed
     * through {@link #handlePaymentTransaction(Stanza)}.
     *
     * @implNote
     * This implementation looks up the orphan by message id and rebuilds the
     * original transaction {@link Stanza} so that
     * {@link #handlePaymentTransaction(Stanza)} can run unchanged. Non-chat
     * messages are ignored because only chat payments are buffered as
     * orphans.
     *
     * @param info the inbound message info
     */
    private void resolveOrphanPayment(LinkedMessageInfo info) {
        if (!(info instanceof ChatMessageInfo chatMessageInfo)) {
            return;
        }

        var orphan = chatMessageInfo.key()
                .id()
                .flatMap(whatsapp.store().businessStore()::removeOrphanPaymentNotification)
                .orElse(null);
        if (orphan == null) {
            return;
        }

        var transactionNode = new StanzaBuilder()
                .description("transaction")
                .attribute("message-id", orphan.messageId())
                .attribute("receiver", orphan.receiverJid().orElse(null))
                .attribute("currency", orphan.currency().orElse(null))
                .attribute("amount_1000", orphan.amount1000().orElse(null))
                .attribute("transaction-type", orphan.transactionType().orElse(null))
                .attribute("status", orphan.status().orElse(null))
                .attribute("ts", orphan.transactionTimestamp().orElse(null))
                .attribute("sender", chatMessageInfo.senderJid().orElse(null))
                .build();
        handlePaymentTransaction(transactionNode);
    }

    /**
     * Processes a payment {@code <transaction>} stanza by updating the
     * payment info on its target {@link ChatMessageInfo}.
     *
     * <p>Attaches a {@link PaymentInfo} to the message the transaction
     * references and fan-broadcasts an {@code onMessageStatus} listener
     * callback. When the message cannot be located the transaction is
     * buffered as an orphan so that
     * {@link #resolveOrphanPayment(LinkedMessageInfo)} can replay it later.
     *
     * @implNote
     * This implementation derives {@code fromMe} from the sender JID compared
     * against the current account, then computes the {@code remote} JID and
     * the optional {@code participant} (set only for group conversations).
     * Payment status and txn status are resolved via
     * {@link #mapPaymentStatus(String, String, boolean)} and
     * {@link #mapTxnStatus(String, String, boolean)}, which mirror the
     * matching helpers in {@code WAWebPaymentStatusUtils}.
     *
     * @param transaction the {@code <transaction>} stanza stanza
     */
    private void handlePaymentTransaction(Stanza transaction) {
        var sender = transaction.getAttributeAsJid("sender").orElse(null);
        var receiver = transaction.getAttributeAsJid("receiver").orElse(null);
        var messageId = transaction.getAttributeAsString("message-id", null);
        if (sender == null || receiver == null || messageId == null) {
            return;
        }

        var self = whatsapp.store().accountStore().jid().orElse(null);
        var fromMe = self != null && Objects.equals(self.toUserJid(), sender.toUserJid());
        var group = transaction.getAttributeAsJid("group").orElse(null);
        var remote = group != null ? group : fromMe ? receiver : sender;
        var participant = group != null ? sender : null;

        var message = findPaymentMessage(remote, participant, messageId, fromMe);
        if (!(message instanceof ChatMessageInfo chatMessageInfo)) {
            whatsapp.store().businessStore().addOrphanPaymentNotification(new OrphanPaymentNotificationBuilder()
                    .messageId(messageId)
                    .receiverJid(receiver)
                    .currency(transaction.getAttributeAsString("currency", null))
                    .amount1000(transaction.getAttributeAsLong("amount_1000", (Long) null))
                    .transactionType(transaction.getAttributeAsString("transaction-type", null))
                    .status(transaction.getAttributeAsString("status", null))
                    .transactionTimestamp(transaction.getAttributeAsLong("ts", (Long) null))
                    .build());
            return;
        }

        var paymentInfo = chatMessageInfo.paymentInfo().orElseGet(this::newPaymentInfo);
        var type = transaction.getAttributeAsString("transaction-type", null);
        var status = transaction.getAttributeAsString("status", null);

        paymentInfo.setReceiverJid(receiver);
        paymentInfo.setAmount1000(transaction.getAttributeAsLong("amount_1000", (Long) null));
        paymentInfo.setCurrency(transaction.getAttributeAsString("currency", null));
        paymentInfo.setTransactionTimestamp(transaction.getAttributeAsLong("ts", (Long) null));
        paymentInfo.setStatus(mapPaymentStatus(type, status, fromMe));
        paymentInfo.setTxnStatus(mapTxnStatus(type, status, fromMe));

        chatMessageInfo.setPaymentInfo(paymentInfo);
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof MessageStatusListener typed) {
                Thread.startVirtualThread(() -> typed.onMessageStatus(whatsapp, chatMessageInfo));
            }
        }
        whatsapp.store().businessStore().removeOrphanPaymentNotification(messageId);
    }

    /**
     * Locates the chat message a payment transaction stanza targets.
     *
     * <p>Backs {@link #handlePaymentTransaction(Stanza)} when reconciling an
     * inbound {@code <transaction>} against the store.
     *
     * @implNote
     * This implementation first tries an exact-key lookup and falls back to a
     * by-id lookup; the two-stage search handles cases where the constructed
     * {@link MessageKey} does not match the stored key bit-for-bit (different
     * sender derivation, different remote JID flavour) but the id is still
     * unique within the chat.
     *
     * @param remote      the remote JID resolved by
     *                    {@link #handlePaymentTransaction(Stanza)}
     * @param participant the participant JID for group messages, or
     *                    {@code null} for direct chats
     * @param messageId   the message id carried on the {@code <transaction>}
     *                    stanza
     * @param fromMe      {@code true} when the transaction was initiated by
     *                    the current account
     * @return the matching message info, or {@code null} when no message is
     *         found
     */
    private LinkedMessageInfo findPaymentMessage(Jid remote, Jid participant, String messageId, boolean fromMe) {
        var direct = whatsapp.store().chatStore().findMessageByKey(new MessageKeyBuilder()
                        .id(messageId)
                        .parentJid(remote)
                        .fromMe(fromMe)
                        .senderJid(participant != null ? participant : remote)
                        .build())
                .orElse(null);
        if (direct != null) {
            return direct;
        }

        return whatsapp.store().chatStore().findMessageById(remote, messageId)
                .map(LinkedMessageInfo.class::cast)
                .orElse(null);
    }

    /**
     * Constructs a fresh {@link PaymentInfo} initialised to
     * {@link PaymentInfo.Status#UNKNOWN_STATUS} and
     * {@link PaymentInfo.TxnStatus#UNKNOWN}.
     *
     * <p>Used by {@link #handlePaymentTransaction(Stanza)} when the target
     * message does not already carry a {@link PaymentInfo}, so that the
     * per-transaction setters always have an instance to mutate.
     *
     * @return a freshly initialised {@link PaymentInfo}
     */
    private PaymentInfo newPaymentInfo() {
        return new PaymentInfoBuilder()
                .status(PaymentInfo.Status.UNKNOWN_STATUS)
                .txnStatus(PaymentInfo.TxnStatus.UNKNOWN)
                .build();
    }

    /**
     * Maps a transaction-type and server-status pair onto a
     * {@link PaymentInfo.Status} value.
     *
     * <p>Drives the high-level {@link PaymentInfo.Status} field that surfaces
     * in the WhatsApp payment UI (processing, sent, complete, refunded,
     * expired, rejected, and so on).
     *
     * @implNote
     * This implementation routes through the intermediate
     * {@link PaymentMessageStatus} returned by
     * {@link #paymentMessageStatus(String, String, boolean)} so the mapping
     * stays a single source of truth.
     *
     * @param type   the {@code transaction-type} attribute, or {@code null}
     * @param status the {@code status} attribute, or {@code null}
     * @param fromMe {@code true} when the transaction originated from the
     *               current account
     * @return the mapped {@link PaymentInfo.Status}; never {@code null}
     */
    private PaymentInfo.Status mapPaymentStatus(String type, String status, boolean fromMe) {
        return switch (paymentMessageStatus(type, status, fromMe)) {
            case SEND_PAY_INIT, SEND_PAY_PENDING, RECV_PAY_INIT, RECV_PAY_PENDING, RECV_PAY_RETRY_ON_FAILURE, REQUEST_PAY_INIT -> PaymentInfo.Status.PROCESSING;
            case SEND_PAY_PENDING_RECEIVER, SEND_PAY_FAILURE_RECEIVER -> PaymentInfo.Status.SENT;
            case REQUEST_PAY_SUCCESS -> paymentMessageTransactionType(type, fromMe) == PaymentMessageTransactionType.TYPE_P2P_REQ_SENT ? PaymentInfo.Status.WAITING_FOR_PAYER : PaymentInfo.Status.WAITING;
            case RECV_PAY_PENDING_SETUP -> PaymentInfo.Status.NEED_TO_ACCEPT;
            case SEND_PAY_SUCCESS, RECV_PAY_SUCCESS, REQUEST_PAY_FULFILLED -> PaymentInfo.Status.COMPLETE;
            case SEND_PAY_FAILURE, SEND_PAY_FAILURE_RISK, SEND_PAY_PENDING_REFUND, SEND_PAY_REFUND_PENDING, SEND_PAY_REFUND_FAILED, SEND_PAY_REFUND_FAILED_PROCESSING, RECV_PAY_FAILURE, REQUEST_PAY_FAILED, REQUEST_PAY_FAILED_RISK -> PaymentInfo.Status.COULD_NOT_COMPLETE;
            case SEND_PAY_REFUNDED -> PaymentInfo.Status.REFUNDED;
            case RECV_PAY_EXPIRED, REQUEST_PAY_EXPIRED, SEND_PAY_AUTH_CANCELED, SEND_PAY_AUTH_CANCEL_FAILED, SEND_PAY_AUTH_CANCEL_FAILED_PROCESSING -> PaymentInfo.Status.EXPIRED;
            case REQUEST_PAY_REJECTED -> PaymentInfo.Status.REJECTED;
            case REQUEST_PAY_CANCELLED -> PaymentInfo.Status.CANCELLED;
            case null, default -> PaymentInfo.Status.UNKNOWN_STATUS;
        };
    }

    /**
     * Maps a transaction-type and server-status pair onto a
     * {@link PaymentInfo.TxnStatus} value.
     *
     * <p>Drives the fine-grained {@link PaymentInfo.TxnStatus} field used by
     * the WhatsApp payment UI to distinguish between the many failure and
     * pending sub-states ({@code COLLECT_FAILED_RISK},
     * {@code REFUND_FAILED_PROCESSING}, {@code FAILED_DA_FINAL}, and so on).
     *
     * @implNote
     * This implementation routes through the intermediate
     * {@link PaymentMessageStatus} returned by
     * {@link #paymentMessageStatus(String, String, boolean)} so the
     * type/status decode happens exactly once.
     *
     * @param type   the {@code transaction-type} attribute, or {@code null}
     * @param status the {@code status} attribute, or {@code null}
     * @param fromMe {@code true} when the transaction originated from the
     *               current account
     * @return the mapped {@link PaymentInfo.TxnStatus}; never {@code null}
     */
    private PaymentInfo.TxnStatus mapTxnStatus(String type, String status, boolean fromMe) {
        return switch (paymentMessageStatus(type, status, fromMe)) {
            case RECV_PAY_EXPIRED, SEND_PAY_EXPIRED -> PaymentInfo.TxnStatus.EXPIRED_TXN;
            case RECV_PAY_FAILURE, SEND_PAY_FAILURE -> PaymentInfo.TxnStatus.FAILED;
            case RECV_PAY_INIT, SEND_PAY_INIT -> PaymentInfo.TxnStatus.INIT;
            case RECV_PAY_PENDING_SETUP -> PaymentInfo.TxnStatus.PENDING_SETUP;
            case RECV_PAY_PENDING, SEND_PAY_PENDING -> PaymentInfo.TxnStatus.FAILED_DA;
            case RECV_PAY_RETRY_ON_FAILURE -> PaymentInfo.TxnStatus.FAILED_PROCESSING;
            case RECV_PAY_SUCCESS, SEND_PAY_SUCCESS, REQUEST_PAY_FULFILLED -> PaymentInfo.TxnStatus.SUCCESS;
            case REQUEST_PAY_CANCELLED -> PaymentInfo.TxnStatus.COLLECT_CANCELED;
            case REQUEST_PAY_CANCELLING -> PaymentInfo.TxnStatus.COLLECT_CANCELLING;
            case REQUEST_PAY_EXPIRED -> PaymentInfo.TxnStatus.COLLECT_EXPIRED;
            case REQUEST_PAY_FAILED_RISK -> PaymentInfo.TxnStatus.COLLECT_FAILED_RISK;
            case REQUEST_PAY_FAILED -> PaymentInfo.TxnStatus.COLLECT_FAILED;
            case REQUEST_PAY_INIT -> PaymentInfo.TxnStatus.COLLECT_INIT;
            case REQUEST_PAY_REJECTED -> PaymentInfo.TxnStatus.COLLECT_REJECTED;
            case REQUEST_PAY_SUCCESS -> PaymentInfo.TxnStatus.COLLECT_SUCCESS;
            case SEND_PAY_AUTH_CANCELED -> PaymentInfo.TxnStatus.AUTH_CANCELED;
            case SEND_PAY_AUTH_CANCEL_FAILED_PROCESSING -> PaymentInfo.TxnStatus.AUTH_CANCEL_FAILED_PROCESSING;
            case SEND_PAY_AUTH_CANCEL_FAILED -> PaymentInfo.TxnStatus.AUTH_CANCEL_FAILED;
            case SEND_PAY_FAILURE_RECEIVER -> PaymentInfo.TxnStatus.FAILED_RECEIVER_PROCESSING;
            case SEND_PAY_FAILURE_RISK, RECV_PAY_FAILURE_RISK -> PaymentInfo.TxnStatus.FAILED_RISK;
            case SEND_PAY_PENDING_RECEIVER -> PaymentInfo.TxnStatus.PENDING_RECEIVER_SETUP;
            case SEND_PAY_PENDING_REFUND -> PaymentInfo.TxnStatus.FAILED_DA_FINAL;
            case SEND_PAY_REFUNDED -> PaymentInfo.TxnStatus.REFUNDED_TXN;
            case SEND_PAY_REFUND_FAILED_PROCESSING -> PaymentInfo.TxnStatus.REFUND_FAILED_PROCESSING;
            case SEND_PAY_REFUND_FAILED -> PaymentInfo.TxnStatus.REFUND_FAILED;
            case SEND_PAY_REFUND_PENDING -> PaymentInfo.TxnStatus.REFUND_FAILED_DA;
            case SEND_PAY_IN_REVIEW -> PaymentInfo.TxnStatus.IN_REVIEW;
            case null, default -> PaymentInfo.TxnStatus.UNKNOWN;
        };
    }

    /**
     * Resolves the internal {@link PaymentMessageStatus} for an inbound
     * {@code <transaction>} stanza.
     *
     * <p>Drives the two surface-side mappings
     * ({@link #mapPaymentStatus(String, String, boolean)} and
     * {@link #mapTxnStatus(String, String, boolean)}) so the decision lives in
     * one place.
     *
     * @implNote
     * This implementation builds the canonical
     * {@link PaymentMessageTransactionType} via
     * {@link #paymentMessageTransactionType(String, boolean)} and then
     * switches on the upper-cased server status to mirror the nested
     * {@code switch} inside {@code WAWebPaymentStatusUtils}. Any status that
     * fails to match its transaction-type branch yields
     * {@link PaymentMessageStatus#STATUS_UNSET}, matching WA Web's
     * fall-through.
     *
     * @param type   the {@code transaction-type} attribute, or {@code null}
     * @param status the {@code status} attribute, or {@code null}
     * @param fromMe {@code true} when the transaction originated from the
     *               current account
     * @return the resolved {@link PaymentMessageStatus}; never {@code null}
     */
    private PaymentMessageStatus paymentMessageStatus(String type, String status, boolean fromMe) {
        var statusValue = status == null ? "" : status.toUpperCase();
        return switch (paymentMessageTransactionType(type, fromMe)) {
            case TYPE_P2M_PAYOUT -> PaymentMessageStatus.STATUS_UNSET;
            case TYPE_P2P_SENT, TYPE_P2M_SENT, TYPE_DEPOSIT -> switch (statusValue) {
                case "PENDING_RECEIVER_SETUP" -> PaymentMessageStatus.SEND_PAY_PENDING_RECEIVER;
                case "FAILED_DA" -> PaymentMessageStatus.SEND_PAY_PENDING;
                case "REFUND_FAILED_DA" -> PaymentMessageStatus.SEND_PAY_REFUND_PENDING;
                case "FAILED_RISK" -> PaymentMessageStatus.SEND_PAY_FAILURE_RISK;
                case "INITIAL" -> PaymentMessageStatus.SEND_PAY_INIT;
                case "SUCCESS", "COMPLETED" -> PaymentMessageStatus.SEND_PAY_SUCCESS;
                case "FAILURE", "FAILED" -> PaymentMessageStatus.SEND_PAY_FAILURE;
                case "REFUNDED" -> PaymentMessageStatus.SEND_PAY_REFUNDED;
                case "REFUND_FAILED" -> PaymentMessageStatus.SEND_PAY_REFUND_FAILED;
                case "FAILED_RECEIVER_PROCESSING" -> PaymentMessageStatus.SEND_PAY_FAILURE_RECEIVER;
                case "REFUND_FAILED_PROCESSING" -> PaymentMessageStatus.SEND_PAY_REFUND_FAILED_PROCESSING;
                case "FAILED_DA_FINAL" -> PaymentMessageStatus.SEND_PAY_PENDING_REFUND;
                case "AUTH_CANCEL_FAILED_PROCESSING" -> PaymentMessageStatus.SEND_PAY_AUTH_CANCEL_FAILED_PROCESSING;
                case "AUTH_CANCEL_FAILED" -> PaymentMessageStatus.SEND_PAY_AUTH_CANCEL_FAILED;
                case "AUTH_CANCELED" -> PaymentMessageStatus.SEND_PAY_AUTH_CANCELED;
                case "CANCELLED" -> PaymentMessageStatus.SEND_PAY_USER_CANCELED;
                case "EXPIRED" -> PaymentMessageStatus.SEND_PAY_EXPIRED;
                case "IN_REVIEW" -> PaymentMessageStatus.SEND_PAY_IN_REVIEW;
                case "PENDING" -> PaymentMessageStatus.SEND_PAY_PENDING_PROCESSING;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_P2P_RCVD, TYPE_P2M_RCVD -> switch (statusValue) {
                case "PENDING_SETUP" -> PaymentMessageStatus.RECV_PAY_PENDING_SETUP;
                case "FAILED_DA" -> PaymentMessageStatus.RECV_PAY_PENDING;
                case "FAILED_PROCESSING" -> PaymentMessageStatus.RECV_PAY_RETRY_ON_FAILURE;
                case "SUCCESS", "COMPLETED" -> PaymentMessageStatus.RECV_PAY_SUCCESS;
                case "FAILURE", "FAILED" -> PaymentMessageStatus.RECV_PAY_FAILURE;
                case "EXPIRED" -> PaymentMessageStatus.RECV_PAY_EXPIRED;
                case "FAILED_RISK" -> PaymentMessageStatus.RECV_PAY_FAILURE_RISK;
                case "WITHDRAWAL_PROCESSING" -> PaymentMessageStatus.RECV_PAY_WITHDRAWAL_PROCESSING;
                case "WITHDRAWAL_FAILURE" -> PaymentMessageStatus.RECV_PAY_WITHDRAWAL_FAILURE;
                case "WITHDRAWAL_PERMANENT_FAILED" -> PaymentMessageStatus.RECV_PAY_WITHDRAWAL_PERMANENT_FAILED;
                case "CANCELLED" -> PaymentMessageStatus.RECV_PAY_SENDER_CANCELED;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_P2P_REQ_SENT, TYPE_P2P_REQ_RCVD -> switch (statusValue) {
                case "COLLECT_SUCCESS" -> PaymentMessageStatus.REQUEST_PAY_SUCCESS;
                case "COLLECT_FAILED" -> PaymentMessageStatus.REQUEST_PAY_FAILED;
                case "COLLECT_FAILED_RISK" -> PaymentMessageStatus.REQUEST_PAY_FAILED_RISK;
                case "COLLECT_REJECTED" -> PaymentMessageStatus.REQUEST_PAY_REJECTED;
                case "COLLECT_EXPIRED" -> PaymentMessageStatus.REQUEST_PAY_EXPIRED;
                case "COLLECT_CANCELED" -> PaymentMessageStatus.REQUEST_PAY_CANCELLED;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_P2P_REQ_SCHEDULED_PAYMENT_RCVD -> switch (statusValue) {
                case "COLLECT_SUCCESS" -> PaymentMessageStatus.REQUEST_PAY_SCHEDULED_PAYMENT_SUCCESS;
                case "AUTH_SUCCESS" -> PaymentMessageStatus.SEND_PAY_AUTH_SUCCESS;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_REFUND -> switch (statusValue) {
                case "SUCCESS", "COMPLETED" -> PaymentMessageStatus.RECV_PAY_SUCCESS;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_WITHDRAWAL -> switch (statusValue) {
                case "PENDING" -> PaymentMessageStatus.WITHDRAWAL_PENDING;
                case "IN_REVIEW" -> PaymentMessageStatus.WITHDRAWAL_IN_REVIEW;
                case "SUCCESS", "COMPLETED" -> PaymentMessageStatus.WITHDRAWAL_SUCCESS;
                case "FAILED", "DECLINED" -> PaymentMessageStatus.WITHDRAWAL_FAILED;
                case "CANCELLED" -> PaymentMessageStatus.WITHDRAWAL_USER_CANCELED;
                case "EXPIRED" -> PaymentMessageStatus.WITHDRAWAL_EXPIRED;
                case "WITHDRAWAL_ACTIVE" -> PaymentMessageStatus.WITHDRAWAL_ACTIVE;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_UNSET, TYPE_P2P_GRP, TYPE_P2P_NO_INFO, TYPE_FUTURE, TYPE_P2P_REQ_GRP, TYPE_MISSING_DETAILS ->
                    PaymentMessageStatus.STATUS_UNSET;
        };
    }

    /**
     * Resolves the {@link PaymentMessageTransactionType} for an inbound
     * {@code <transaction>} stanza.
     *
     * <p>Drives every subsequent payment-status decision in the stream
     * handler.
     *
     * @implNote
     * This implementation lower-cases the {@code transaction-type} attribute
     * before matching, so casing inconsistencies on the wire do not change the
     * resolved type. When the attribute is missing or unrecognised, the
     * fall-back is {@link PaymentMessageTransactionType#TYPE_P2P_SENT} or
     * {@link PaymentMessageTransactionType#TYPE_P2P_RCVD} depending on
     * {@code fromMe}, matching WA Web's default branch.
     *
     * @param type   the {@code transaction-type} attribute, or {@code null}
     * @param fromMe {@code true} when the transaction originated from the
     *               current account
     * @return the resolved {@link PaymentMessageTransactionType}; never
     *         {@code null}
     */
    private PaymentMessageTransactionType paymentMessageTransactionType(String type, boolean fromMe) {
        if (type == null) {
            return fromMe ? PaymentMessageTransactionType.TYPE_P2P_SENT : PaymentMessageTransactionType.TYPE_P2P_RCVD;
        }

        return switch (type.toLowerCase()) {
            case "p2p" -> fromMe ? PaymentMessageTransactionType.TYPE_P2P_SENT : PaymentMessageTransactionType.TYPE_P2P_RCVD;
            case "p2m" -> fromMe ? PaymentMessageTransactionType.TYPE_P2M_SENT : PaymentMessageTransactionType.TYPE_P2M_RCVD;
            case "payout" -> PaymentMessageTransactionType.TYPE_P2M_PAYOUT;
            case "deposit" -> PaymentMessageTransactionType.TYPE_DEPOSIT;
            case "refund" -> PaymentMessageTransactionType.TYPE_REFUND;
            case "withdrawal" -> PaymentMessageTransactionType.TYPE_WITHDRAWAL;
            default -> fromMe ? PaymentMessageTransactionType.TYPE_P2P_SENT : PaymentMessageTransactionType.TYPE_P2P_RCVD;
        };
    }

    /**
     * Dispatches an embedded {@link ProtocolMessage} to the service that owns
     * its payload kind.
     *
     * <p>Routes the six protocol-message slots Cobalt understands:
     * <ul>
     *   <li>{@code lidMigrationMappingSyncMessage} forwards into
     *       {@link LidMigrationService} after GZIP-decoding the payload via
     *       {@link #decodeLidMappingPayload(byte[])};</li>
     *   <li>{@code peerDataOperationRequestResponseMessage} forwards into
     *       {@link #resolveSnapshotRecovery} for syncd
     *       snapshot-fatal-recovery responses;</li>
     *   <li>{@code appStateSyncKeyShare} forwards into
     *       {@link #processAppStateSyncKeyShare};</li>
     *   <li>{@code appStateSyncKeyRequest} forwards into
     *       {@link #processAppStateSyncKeyRequest};</li>
     *   <li>{@code historySyncNotification} forwards into
     *       {@link WebHistorySyncService}, which downloads, decrypts, and
     *       decodes the announced chunk on its own virtual thread;</li>
     *   <li>{@code initialSecurityNotificationSettingSync} updates the
     *       store's show-security-notifications preference.</li>
     * </ul>
     *
     * @implNote
     * This implementation handles the LID-migration decode locally so that
     * {@link LidMigrationService} receives an already-decoded payload (or a
     * {@code null} sentinel it can escalate as
     * {@code WhatsAppLidMigrationException.FailedToParseMappings}). The
     * {@code initialSecurityNotificationSettingSync} branch replaces WA Web's
     * {@code WAWebUserPrefsNotifications.setGlobalSecurityNotifications} write
     * into {@code WAWebUserPrefsKeys.SECURITY_NOTIFICATIONS}.
     *
     * @param info the chat message info whose content is a
     *             {@link ProtocolMessage}
     */
    private void handleProtocolMessage(ChatMessageInfo info) {
        var content = info.message().content();
        if (!(content instanceof ProtocolMessage protocolMessage)) {
            return;
        }

        protocolMessage.lidMigrationMappingSyncMessage().ifPresent(message -> {
            var decoded = message.encodedMappingPayload()
                    .flatMap(this::decodeLidMappingPayload)
                    .orElse(null);
            lidMigrationService.processProtocolMessage(decoded);
        });

        protocolMessage.peerDataOperationRequestResponseMessage()
                .ifPresent(this::resolveSnapshotRecovery);

        protocolMessage.appStateSyncKeyShare()
                .ifPresent(keyShare -> processAppStateSyncKeyShare(info, keyShare));

        protocolMessage.appStateSyncKeyRequest()
                .ifPresent(request -> processAppStateSyncKeyRequest(info, request));

        protocolMessage.historySyncNotification()
                .ifPresent(webHistorySyncService::process);

        protocolMessage.initialSecurityNotificationSettingSync()
                .ifPresent(sync -> whatsapp.store().settingsStore().setShowSecurityNotifications(sync.securityNotificationEnabled()));
    }

    /**
     * Processes an inbound app-state-sync key share protocol message.
     *
     * <p>Lets a companion device receive new app-state encryption keys from
     * the primary device. Validates per-key id lengths, then forwards the
     * accepted keys to {@link SyncKeyRotationService}, which updates its
     * tracking, reschedules timeouts, and resumes any collections that were
     * blocked waiting for these keys.
     *
     * @implNote
     * This implementation logs and skips key ids whose byte-length is not
     * exactly six bytes; WA Web treats the same condition as a fatal error and
     * reports a metric, but Cobalt's error model delegates fatal escalation to
     * {@code WhatsAppClientErrorHandler} via the sealed exception hierarchy
     * rather than inline metric calls.
     *
     * @param info     the chat message info carrying the key share
     * @param keyShare the {@link AppStateSyncKeyShare} payload
     */
    private void processAppStateSyncKeyShare(ChatMessageInfo info, AppStateSyncKeyShare keyShare) {
        syncKeyRotationService.logMissingKeysReceived();
        var senderDeviceId = info.senderJid().isPresent() ? info.senderJid().get().device() : -1;

        var keys = keyShare.keys();
        var validatedKeys = new ArrayList<AppStateSyncKey>(keys.size());
        for (var key : keys) {
            var keyId = key.keyId()
                    .flatMap(AppStateSyncKeyId::keyId)
                    .orElse(null);
            if (keyId == null) {
                continue;
            }

            if (keyId.length != 6) {
                if (Log.ERROR) {
                    LOGGER.log(Level.ERROR, "syncd: fatal error: key share key id has invalid bytelength of {0}", keyId.length);
                }
                continue;
            }

            validatedKeys.add(key);
        }

        if (validatedKeys.isEmpty()) {
            return;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "handling app state sync key share from device {0} with {1} validated keys",
                    senderDeviceId, validatedKeys.size());
        }
        syncKeyRotationService.handleKeyShare(senderDeviceId, validatedKeys);
    }

    /**
     * Answers an inbound app-state-sync key request by shipping the requested
     * keys back to the requester as a peer message.
     *
     * <p>Implements the reverse direction of the syncd key-share dance: when a
     * companion device asks the primary for keys it is missing, this method
     * assembles a peer protocol message containing the subset of requested
     * keys that the local store knows about and dispatches it via
     * {@link LinkedWhatsAppClient#sendPeerMessage(com.github.auties00.cobalt.wire.core.jid.JidProvider, ChatMessageInfo)}.
     *
     * @implNote
     * This implementation packs a placeholder entry with just the key id when
     * the requested key is not locally known, mirroring WA Web's behaviour of
     * responding with an {@link AppStateSyncKey} whose key data is empty so
     * the requester can detect the miss. Failures shipping the peer message
     * are demoted to a debug log because key requests are fire-and-forget on
     * the WA Web side.
     *
     * @param info    the chat message info carrying the key request
     * @param request the {@link AppStateSyncKeyRequest} listing the requested
     *                key ids
     */
    private void processAppStateSyncKeyRequest(
            ChatMessageInfo info,
            AppStateSyncKeyRequest request
    ) {
        var sender = info.senderJid();
        if (sender.isEmpty()) {
            return;
        }

        var keysToShare = new ArrayList<AppStateSyncKey>();
        for (var requestedKeyId : request.keyIds()) {
            var rawKeyId = requestedKeyId.keyId().orElse(null);
            if (rawKeyId == null) {
                continue;
            }

            var keyToShare = whatsapp.store().syncStore().findWebAppStateKeyById(rawKeyId)
                    .orElseGet(() -> new AppStateSyncKeyBuilder()
                            .keyId(new AppStateSyncKeyIdBuilder()
                                    .keyId(rawKeyId)
                                    .build())
                            .build());
            keysToShare.add(keyToShare);
        }

        if (keysToShare.isEmpty()) {
            return;
        }

        try {
            var keyShare = new AppStateSyncKeyShareBuilder()
                    .keys(keysToShare)
                    .build();
            var protocolMessage = new ProtocolMessageBuilder()
                    .type(ProtocolMessage.Type.APP_STATE_SYNC_KEY_SHARE)
                    .appStateSyncKeyShare(keyShare)
                    .build();
            var messageContainer = new LinkedMessageContainerBuilder()
                    .protocolMessage(protocolMessage)
                    .build();
            var self = whatsapp.store().accountStore().jid().orElse(null);
            if (self == null) {
                return;
            }

            var key = new MessageKeyBuilder()
                    .id(MessageIdGenerator.generate(MessageIdVersion.V2, sender.get()))
                    .parentJid(self)
                    .fromMe(true)
                    .senderJid(self)
                    .build();
            var response = new ChatMessageInfoBuilder()
                    .key(key)
                    .message(messageContainer)
                    .timestamp(Instant.now())
                    .senderJid(self)
                    .build();
            whatsapp.sendPeerMessage(sender.get(), response);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "answered app state sync key request from {0} with {1} keys",
                        sender.orElse(null), keysToShare.size());
            }
        } catch (Throwable throwable) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "failed to answer app state sync key request from "
                        + new LogRedactable.User(String.valueOf(sender.orElse(null))), throwable);
            }
        }
    }

    /**
     * Handles a {@code COMPANION_SYNCD_SNAPSHOT_FATAL_RECOVERY} peer-data
     * response that completes a previously issued snapshot recovery request.
     *
     * <p>Used by {@link #handleProtocolMessage(ChatMessageInfo)} when a primary
     * device replies to the companion's snapshot-recovery request: the decoded
     * recovery snapshot is handed off to
     * {@link SnapshotRecoveryService#resolveRecovery(SyncPatchType, SyncdSnapshotRecovery)}
     * so the consumer blocked on the recovery promise in
     * {@link WebAppStateService} receives the result. A
     * {@link com.github.auties00.cobalt.wire.wam.event.NonMessagePeerDataOperationResponseEvent}
     * is committed in both the success and decode-failure paths. Responses for
     * other request types or responses received while recovery is disabled are
     * silently dropped.
     *
     * @implNote
     * This implementation decodes the recovery snapshot exactly once (the
     * consumer is given the already-decoded {@link SyncdSnapshotRecovery} so it
     * does not decode again). Decode failures commit the WAM event with
     * {@code peerDataErrorCount=1} while successful resolutions commit it with
     * {@code responseCount=successResponseCount=successProcessCount=1}.
     *
     * @param response the peer-data-operation response message
     */
    private void resolveSnapshotRecovery(PeerDataOperationRequestResponseMessage response) {
        if (response.peerDataOperationRequestType().orElse(null)
                != PeerDataOperationRequestType.COMPANION_SYNCD_SNAPSHOT_FATAL_RECOVERY) {
            return;
        }

        if (!snapshotRecoveryService.isRecoveryEnabled()) {
            return;
        }

        var results = response.peerDataOperationResult();
        if (results.isEmpty()) {
            return;
        }

        var recovery = results.getFirst().syncdSnapshotFatalRecoveryResponse().orElse(null);
        if (recovery == null) {
            return;
        }

        var sessionId = response.stanzaId().orElse(null);

        SyncdSnapshotRecovery decoded;
        try {
            decoded = snapshotRecoveryService.decodeRecoverySnapshot(recovery);
        } catch (RuntimeException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "failed to decode snapshot recovery payload, session=" + sessionId, exception);
            }
            wamService.commit(new NonMessagePeerDataOperationResponseEventBuilder()
                    .peerDataRequestType(PeerDataRequestType.SYNCD_SNAPSHOT_RECOVERY)
                    .peerDataRequestSessionId(sessionId)
                    .peerDataResponseCount(0)
                    .peerDataSuccessResponseCount(0)
                    .peerDataSuccessProcessCount(0)
                    .peerDataErrorCount(1)
                    .peerDataNotFoundCount(0)
                    .build());
            return;
        }

        var collectionName = decoded.collectionName().flatMap(SyncPatchType::of).orElse(null);
        if (collectionName != null) {
            snapshotRecoveryService.resolveRecovery(collectionName, decoded);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "resolved snapshot recovery for collection {0}, session={1}", collectionName, sessionId);
            }
        }

        wamService.commit(new NonMessagePeerDataOperationResponseEventBuilder()
                .peerDataRequestType(PeerDataRequestType.SYNCD_SNAPSHOT_RECOVERY)
                .peerDataRequestSessionId(sessionId)
                .peerDataResponseCount(1)
                .peerDataSuccessResponseCount(1)
                .peerDataSuccessProcessCount(1)
                .peerDataErrorCount(0)
                .peerDataNotFoundCount(0)
                .build());
    }

    /**
     * Decodes the GZIP-compressed protobuf payload that ships inside a LID
     * migration mapping sync protocol message.
     *
     * <p>Backs the {@code lidMigrationMappingSyncMessage} branch of
     * {@link #handleProtocolMessage(ChatMessageInfo)} so that
     * {@link LidMigrationService#processProtocolMessage(LIDMigrationMappingSyncPayload)}
     * receives a typed payload rather than raw GZIP bytes.
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} on every failure
     * mode (null bytes, empty bytes, GZIP failure, protobuf failure) and
     * demotes the failure to a warning log; {@link LidMigrationService} then
     * surfaces the empty payload as a
     * {@code WhatsAppLidMigrationException.FailedToParseMappings}.
     *
     * @param payload the GZIP-compressed protobuf payload bytes
     * @return the decoded {@link LIDMigrationMappingSyncPayload}, or
     *         {@link Optional#empty()} when the payload could not be decoded
     */
    private Optional<LIDMigrationMappingSyncPayload> decodeLidMappingPayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return Optional.empty();
        }

        try (var protobufStream = new BufferedProtobufInputStream(new GZIPInputStream(new ByteArrayInputStream(payload)))) {
            return Optional.of(LIDMigrationMappingSyncPayloadSpec.decode(protobufStream));
        } catch (Exception exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "failed to decode lid migration mapping payload, bytes=" + payload.length, exception);
            }
            return Optional.empty();
        }
    }
}
