package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.icdc.IcdcResult;
import com.github.auties00.cobalt.exception.linked.WhatsAppCorruptedStoreException;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.icdc.IcdcEnricher;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.linked.chat.ChatKeepType;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentitySpec;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.message.EmptyMessage;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactsArrayMessage;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EncEventResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EventMessage;
import com.github.auties00.cobalt.wire.linked.message.group.GroupInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateButtonReplyMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LiveLocationMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.*;
import com.github.auties00.cobalt.wire.linked.message.newsletter.NewsletterAdminInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.newsletter.NewsletterFollowerInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollResultSnapshotMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncCommentMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.security.SecretEncMessage;
import com.github.auties00.cobalt.wire.linked.message.system.*;
import com.github.auties00.cobalt.wire.linked.message.system.history.MessageHistoryNotice;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSignalStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.AndroidMessageSendPerfEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DisappearingMessageKeepInChatEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.E2eMessageSendEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.KeepInChatErrorsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.KeepInChatPerfEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PnhRequestRevealActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StickerSendEventBuilder;
import com.github.auties00.cobalt.wam.threadlogging.LiveThreadLoggingService;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.AgentEngagementEnumType;
import com.github.auties00.cobalt.wire.wam.type.ClientMessageSendStage;
import com.github.auties00.cobalt.wire.wam.type.E2eCiphertextType;
import com.github.auties00.cobalt.wire.wam.type.E2eDestination;
import com.github.auties00.cobalt.wire.wam.type.EditType;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.KicActionNameType;
import com.github.auties00.cobalt.wire.wam.type.KicActionType;
import com.github.auties00.cobalt.wire.wam.type.KicActorType;
import com.github.auties00.cobalt.wire.wam.type.KicEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.KicErrorCodeType;
import com.github.auties00.cobalt.wire.wam.type.KicRequestTypeType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageSendResultType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderReasonType;
import com.github.auties00.cobalt.wire.wam.type.PnhActionType;
import com.github.auties00.cobalt.wire.wam.type.PnhEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.PnhMessageChatParty;
import com.github.auties00.cobalt.wire.wam.type.ResponseType;
import com.github.auties00.cobalt.wire.wam.type.StickerSendMessageType;
import com.github.auties00.cobalt.wire.wam.type.StickerSendOriginType;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Provides the cross-cutting helpers shared by the per-chat-kind senders.
 *
 * <p>Concrete subclasses ({@link UserMessageSender}, {@link GroupMessageSender},
 * {@link StatusMessageSender}, {@link BroadcastMessageSender},
 * {@link NewsletterMessageSender}, {@link PeerMessageSender}) implement the
 * chat-kind-specific orchestration and reuse this class for per-device Signal
 * encryption with ICDC enrichment, the {@code type}, {@code edit},
 * {@code decrypt-fail}, {@code mediatype}, and {@code native_flow_name}
 * attribute resolvers, the {@code <device-identity>} child builder, and the WAM
 * emission helpers that record per-send Signal cipher results.
 */
@WhatsAppWebModule(moduleName = "WAWebSendMsgCommonApi")
@WhatsAppWebModule(moduleName = "WAWebE2EProtoUtils")
@WhatsAppWebModule(moduleName = "WAWebAdvSignatureApi")
@WhatsAppWebModule(moduleName = "WAWebBackendJobsCommon")
abstract sealed class MessageSender<T extends LinkedMessageInfo> permits UserMessageSender, GroupMessageSender, StatusMessageSender, BroadcastMessageSender, NewsletterMessageSender, PeerMessageSender {
    /**
     * The logger for {@link MessageSender}.
     */
    private static final System.Logger LOGGER = Log.get(MessageSender.class);

    /**
     * Dispatches wire stanzas and surfaces fatal store-persistence failures to
     * the embedder's error handler.
     */
    final LinkedWhatsAppClient client;

    /**
     * Carries Signal sessions, device-list state, identity records, sender-key
     * distribution flags, chat metadata, and receipt records consulted by every
     * subclass.
     */
    final LinkedWhatsAppStore store;

    /**
     * Supplies the resend-timeout AB prop read by the base class and the
     * feature-gating props read by subclasses.
     */
    final ABPropsService abPropsService;

    /**
     * Commits per-send WAM events.
     */
    final WamService wamService;

    /**
     * Holds the per-conversation locks that serialise the whole send pipeline
     * for one target JID.
     *
     * <p>Each lock guards the multi-step group, status, and broadcast pipeline
     * (device-partition read, key-rotation decision, sender-key distribution,
     * SKMSG ratchet, and distribution bookkeeping) so two sends to the same
     * conversation cannot interleave; sends to different conversations acquire
     * distinct locks and proceed in parallel.
     */
    private final ConcurrentMap<String, ReentrantLock> sendLocks;

    /**
     * Constructs a {@link MessageSender} bound to the supplied dependencies.
     *
     * <p>Invoked only by the sealed subclasses; the package-private visibility
     * is intentional.
     *
     * @param client         the {@link LinkedWhatsAppClient} used to dispatch
     *                       stanzas and surface failures
     * @param abPropsService the {@link ABPropsService} consulted for
     *                       feature-gating decisions
     * @param wamService     the {@link WamService} that records per-send
     *                       events
     * @throws NullPointerException if any argument is {@code null}
     */
    MessageSender(LinkedWhatsAppClient client, ABPropsService abPropsService, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client");
        this.store = client.store();
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService");
        this.wamService = Objects.requireNonNull(wamService, "wamService");
        this.sendLocks = new ConcurrentHashMap<>();
    }

    /**
     * Dispatches the supplied {@link LinkedMessageInfo} to the given chat or audience
     * JID, serialised per conversation.
     *
     * <p>Waits for the offline backlog to drain, then runs {@link #doSend} while
     * holding the per-conversation lock so concurrent sends to the same target
     * cannot interleave their sender-key distribution, rotation, and ratchet
     * steps. Sends to different conversations run in parallel; the per-session
     * and per-sender-key ratchets are additionally guarded by
     * {@link com.github.auties00.cobalt.message.crypto.SignalCryptoLocks}. The
     * dispatch from {@link MessageSendingService#send(LinkedMessageInfo)} routes by JID
     * server.
     *
     * <p>The whole dispatch is timed so that, once the server ack has been
     * parsed, the base class commits the per-send WAM telemetry: the
     * {@code AndroidMessageSendPerfEvent} stage/result/duration beacon for every
     * send, plus the content-specific {@code StickerSendEvent},
     * keep-in-chat family, and {@code PnhRequestRevealActionEvent} when the
     * payload warrants them (see {@link #emitMessageSendPerfEvent} and
     * {@link #emitContentSendTelemetry}).
     *
     * @param chatJid     the target chat, group, status, newsletter, or
     *                    peer-device JID
     * @param messageInfo the prepared message info
     * @return the parsed server {@link AckResult}
     */
    final AckResult send(Jid chatJid, T messageInfo) {
        waitForOfflineDelivery();
        var perf = new AndroidMessageSendPerfEventBuilder()
                .startDurationT()
                .startDurationRelative();
        var ack = enqueue(chatJid.toString(), () -> doSend(chatJid, messageInfo));
        var container = messageInfo.message();
        emitMessageSendPerfEvent(chatJid, container, perf, ack);
        emitContentSendTelemetry(chatJid, container, ack);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "send to {0} finished, success={1}", chatJid, ack.isSuccess());
        }
        return ack;
    }

    /**
     * Builds and dispatches the chat-kind-specific wire stanza for the given
     * message.
     *
     * @implSpec
     * Each subclass implements the chat-kind-specific stanza shape and
     * encryption flow. By the time this runs the base class has already drained
     * the offline backlog and acquired the per-conversation lock, so an
     * implementation must not call {@link #waitForOfflineDelivery()} or
     * {@link #enqueue(String, Supplier)} again; it composes the stanza,
     * encrypts, dispatches, and reacts to the server ack.
     *
     * @param chatJid     the target chat, group, status, newsletter, or
     *                    peer-device JID
     * @param messageInfo the prepared message info
     * @return the parsed server {@link AckResult}
     */
    abstract AckResult doSend(Jid chatJid, T messageInfo);

    /**
     * Runs {@code task} while holding the {@link ReentrantLock} associated with
     * {@code conversationKey}, creating the lock on first use.
     *
     * <p>Serialises the whole send pipeline for one conversation so the
     * device-partition read, the key-rotation decision, the sender-key
     * distribution, and the SKMSG ratchet cannot interleave with another send to
     * the same target. The base {@link #send(Jid, LinkedMessageInfo)} routes every
     * send through this; {@link GroupMessageSender#sendKeyDistribution(Jid, String)}
     * also calls it directly for the standalone distribution flow.
     *
     * @implNote This implementation mirrors WA Web's per-conversation
     * {@code sendMsgQueueMap.enqueue}; WA Web needs no cross-thread lock because
     * its JavaScript is single-threaded, whereas Cobalt sends on virtual threads.
     *
     * @param <R>             the {@code task} result type
     * @param conversationKey the lock key, the target JID string
     * @param task            the send pipeline to run under the lock
     * @return the value returned by {@code task}
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgQueueMap", exports = "sendMsgQueueMap",
            adaptation = WhatsAppAdaptation.ADAPTED)
    <R> R enqueue(String conversationKey, Supplier<R> task) {
        Objects.requireNonNull(conversationKey, "conversationKey");
        Objects.requireNonNull(task, "task");
        var lock = sendLocks.computeIfAbsent(conversationKey, _ -> new ReentrantLock());
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "acquiring send lock for {0}", new LogRedactable.User(conversationKey));
        }
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Blocks the current virtual thread until the offline-message backlog has
     * been fully replayed.
     *
     * <p>Every subclass calls this before composing the outbound stanza so that
     * sends issued mid-reconnect are emitted only after the local store has
     * caught up with the queue the server held while the client was offline.
     */
    @WhatsAppWebExport(moduleName = "WAWebEventsWaitForOfflineDeliveryEnd", exports = "waitForOfflineDeliveryEnd",
            adaptation = WhatsAppAdaptation.DIRECT)
    void waitForOfflineDelivery() {
        store.connectionStore().waitForOfflineDeliveryEnd();
    }

    /**
     * Returns the maximum age, in seconds, a previously-sent message may still
     * be resent at after a server-driven retry signal.
     *
     * <p>Backed by the {@code WEB_E2E_BACKFILL_EXPIRE_TIME} AB prop (a minute
     * value); the resend pipeline skips messages older than this threshold to
     * avoid replaying stale backfill traffic.
     *
     * @implNote
     * This implementation falls back to {@code 5} minutes when the AB prop is
     * missing or non-positive, matching WA Web's default literal in
     * {@code getResendTimeoutInSeconds}.
     *
     * @return the resend timeout in seconds
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "getResendTimeoutInSeconds",
            adaptation = WhatsAppAdaptation.DIRECT)
    long getResendTimeoutInSeconds() {
        var minutes = abPropsService.getInt(ABProp.WEB_E2E_BACKFILL_EXPIRE_TIME);
        if (minutes <= 0) {
            minutes = 5;
        }
        return minutes * 60L;
    }

    /**
     * Flushes the {@link LinkedWhatsAppStore} to its persistent backing so the Signal
     * session ratchets and pre-key updates produced by the encryption step
     * survive a process crash immediately after the wire write.
     *
     * <p>Every subclass calls this after building the stanza and before
     * invoking {@link LinkedWhatsAppClient#sendNode(StanzaBuilder)}; a persistence
     * failure is routed through the client's
     * {@link LinkedWhatsAppClient#handleFailure(com.github.auties00.cobalt.exception.WhatsAppException)} error handler as a
     * {@link WhatsAppCorruptedStoreException}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSignalProtocolStore", exports = "flushBufferToDiskIfNotMemOnlyMode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    void flushStore() {
        try {
            store.save();
        } catch (IOException ex) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "store persistence failed after send", ex);
            }
            client.handleFailure(new WhatsAppCorruptedStoreException(ex));
        }
    }

    /**
     * Returns the {@link Jid} of the currently-paired device or fails fast.
     *
     * <p>Used whenever a send path needs the local PN JID and treats a missing
     * pairing as a programming error rather than a recoverable miss.
     *
     * @return the self PN {@link Jid}
     * @throws IllegalStateException if the client is not logged in
     */
    Jid requireSelfJid() {
        return store.accountStore().jid().orElseThrow(() ->
                new IllegalStateException("Not logged in"));
    }

    /**
     * Returns the local LID JID, or the PN JID when no LID is paired.
     *
     * <p>The group, status, and CAG paths use this to pick the addressing-mode
     * sender JID; CAG and LID-addressed groups need the LID, but legacy PN
     * groups still address the sender by PN.
     *
     * @return the self LID {@link Jid}, or the PN {@link Jid} when no LID is
     *         paired
     */
    Jid selfLidOrPn() {
        return store.accountStore().lid().orElseGet(this::requireSelfJid);
    }

    /**
     * Encrypts {@code container} for each device in {@code devices} and returns
     * the per-device payloads, populating ICDC metadata before serialisation.
     *
     * <p>Companion devices receive a {@code DeviceSentMessage}-wrapped copy
     * carrying only the sender ICDC; non-self recipient devices receive both
     * the sender and recipient ICDC. Devices whose encryption raises any
     * exception are logged and dropped from the result; the receipts recorded
     * against them via {@link LinkedWhatsAppSignalStore#updateIdentityRange} still cover the
     * full input list so identity ranges stay aligned with the dispatched
     * fanout.
     *
     * @param encryption     the {@link MessageEncryption} service to use
     * @param devices        the device {@link Jid}s to encrypt for
     * @param container      the source {@link LinkedMessageContainer}
     * @param destinationJid the chat recipient JID, written into the
     *                       {@code DeviceSentMessage} wrapper sent to self
     *                       devices
     * @param senderIcdc     the sender's ICDC, or {@code null}
     * @param recipientIcdc  the recipient's ICDC, or {@code null}
     * @return the encrypted payloads, possibly shorter than {@code devices}
     *         when per-device encryption failed
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "createFanoutMsgStanza",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebICDCMetaApi", exports = "populateICDCMeta",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebDeviceSentMessageProtoUtils", exports = "wrapDeviceSentMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    List<MessageEncryptedPayload> encryptForDevices(
            MessageEncryption encryption,
            Collection<Jid> devices,
            LinkedMessageContainer container,
            Jid destinationJid,
            IcdcResult senderIcdc,
            IcdcResult recipientIcdc
    ) {
        var selfPn = store.accountStore().jid().map(Jid::toUserJid).orElse(null);
        var selfLid = store.accountStore().lid().map(Jid::toUserJid).orElse(null);
        var results = new ArrayList<MessageEncryptedPayload>(devices.size());

        var companionContainer = IcdcEnricher.enrich(container, senderIcdc, null);
        var recipientContainer = IcdcEnricher.enrich(container, senderIcdc, recipientIcdc);

        for (var device : devices) {
            try {
                byte[] devicePlaintext;
                var deviceUserJid = device.toUserJid();
                var isSelfDevice = (selfPn != null && deviceUserJid.equals(selfPn))
                        || (selfLid != null && deviceUserJid.equals(selfLid));
                if (isSelfDevice) {
                    var innerContextInfo = companionContainer.messageContextInfo().orElse(null);
                    var innerContainer = innerContextInfo != null
                            ? companionContainer.withMessageContextInfo(null)
                            : companionContainer;
                    var deviceSentMessage = new DeviceSentMessageBuilder()
                            .destinationJid(destinationJid)
                            .messageContainer(innerContainer)
                            .build();
                    var wrapped = LinkedMessageContainer.of(deviceSentMessage);
                    if (innerContextInfo != null) {
                        wrapped = wrapped.withMessageContextInfo(innerContextInfo);
                    }
                    devicePlaintext = LinkedMessageContainerSpec.encode(wrapped);
                } else {
                    devicePlaintext = LinkedMessageContainerSpec.encode(recipientContainer);
                }
                var payload = encryption.encryptForDevice(device, devicePlaintext);
                results.add(payload);
                emitE2eMessageSendEvent(device, container, true, payload.type(), 0);
            } catch (Exception e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "device encryption failed for " + new LogRedactable.User(device.toString()), e);
                }
                emitE2eMessageSendEvent(device, container, false, null, 0);
            }
        }

        store.signalStore().updateIdentityRange(devices);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "encrypted for {0}/{1} devices for {2}", results.size(), devices.size(), destinationJid);
        }

        return results;
    }

    /**
     * Returns the wire-level {@code type} attribute value for the given
     * {@link LinkedMessageContainer}.
     *
     * <p>The result is one of {@code "text"}, {@code "media"},
     * {@code "reaction"}, {@code "poll"}, or {@code "event"} and is stamped onto
     * the outer {@code <message type="...">} attribute by every chat fanout
     * stanza builder. The classification matches WA Web's
     * {@code typeAttributeFromProtobuf} so receivers parse the wire shape
     * identically.
     *
     * @param container the outbound {@link LinkedMessageContainer}
     * @return the {@code type} attribute value; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoUtils", exports = "typeAttributeFromProtobuf",
            adaptation = WhatsAppAdaptation.DIRECT)
    String resolveStanzaType(LinkedMessageContainer container) {
        var message = container.content();
        return switch (message) {
            case ReactionMessage _ -> "reaction";
            case EncReactionMessage _ -> "reaction";

            case EventMessage _ -> "event";
            case EncEventResponseMessage _ -> "event";
            case SecretEncMessage s
                    when s.secretEncType().orElse(null) == SecretEncMessage.SecretEncType.EVENT_EDIT -> "event";

            case SecretEncMessage s
                    when s.secretEncType().orElse(null) == SecretEncMessage.SecretEncType.MESSAGE_EDIT -> "text";

            case PollCreationMessage _ -> "poll";
            case PollUpdateMessage _ -> "poll";

            case PollResultSnapshotMessage _ -> "poll";

            case ExtendedTextMessage text when text.matchedText().isPresent() -> "media";

            case ExtendedTextMessage _ -> "text";
            case TemplateButtonReplyMessage _ -> "text";
            case ProtocolMessage _ -> "text";
            case InteractiveMessage _ -> "text";
            case InteractiveResponseMessage _ -> "text";
            case KeepInChatMessage _ -> "text";
            case PinInChatMessage _ -> "text";
            case EncCommentMessage _ -> "text";
            case NewsletterAdminInviteMessage _ -> "text";
            case NewsletterFollowerInviteMessage _ -> "text";
            case MessageHistoryNotice _ -> "text";
            case RequestPhoneNumberMessage _ -> "text";
            case EmptyMessage _ -> "text";

            default -> "media";
        };
    }

    /**
     * Returns the wire {@code edit} attribute value for {@code container},
     * defaulting to the non-admin revoke classification.
     *
     * <p>Delegates to {@link #resolveEditAttribute(LinkedMessageContainer, boolean)}
     * with {@code isAdminRevoke=false}; every send path other than the group
     * admin-revoke branch uses this overload.
     *
     * @param container the outbound {@link LinkedMessageContainer}
     * @return the {@code edit} value, or {@code null} when no attribute should
     *         be written
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "editAttribute",
            adaptation = WhatsAppAdaptation.DIRECT)
    String resolveEditAttribute(LinkedMessageContainer container) {
        return resolveEditAttribute(container, false);
    }

    /**
     * Returns the wire {@code edit} attribute value for {@code container},
     * distinguishing the admin-revoke variant.
     *
     * <p>The mapping mirrors WA Web's {@code editAttribute} routing:
     * {@code "7"} for sender revoke (and undo-keep-for-all and reaction-clear),
     * {@code "8"} for admin revoke, {@code "1"} for protobuf and secret
     * message-edit, and {@code "2"} for pin-in-chat. Anything else returns
     * {@code null} and the caller drops the attribute.
     *
     * @param container     the outbound {@link LinkedMessageContainer}
     * @param isAdminRevoke {@code true} when a group admin is revoking a
     *                      participant's message
     * @return the {@code edit} value, or {@code null} when no attribute should
     *         be written
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "editAttribute",
            adaptation = WhatsAppAdaptation.DIRECT)
    String resolveEditAttribute(LinkedMessageContainer container, boolean isAdminRevoke) {
        var message = container.content();
        return switch (message) {
            case ProtocolMessage p when p.type().orElse(null) == ProtocolMessage.Type.REVOKE ->
                    isAdminRevoke ? "8" : "7";

            case ProtocolMessage p when p.type().orElse(null) == ProtocolMessage.Type.MESSAGE_EDIT -> "1";

            case SecretEncMessage s
                    when s.secretEncType().orElse(null) == SecretEncMessage.SecretEncType.EVENT_EDIT
                    || s.secretEncType().orElse(null) == SecretEncMessage.SecretEncType.MESSAGE_EDIT -> "1";

            case KeepInChatMessage keep when isUndoKeepForAll(keep) -> "7";

            case PinInChatMessage _ -> "2";

            case ReactionMessage r when r.text().orElse("").isEmpty() -> "7";

            default -> null;
        };
    }

    /**
     * Returns the wire {@code decrypt-fail} attribute value for
     * {@code container}.
     *
     * <p>{@code "hide"} silences the receiver-side fallback that surfaces a
     * decryption failure as a placeholder bubble; it is used for reactions,
     * encrypted reactions, poll updates, keep/pin-in-chat addons, encrypted
     * event responses, secret message edits, and the silent protocol subtypes
     * (revoke, message edit, ephemeral sync response, welcome request). Anything
     * else returns {@code null} so the caller drops the attribute.
     *
     * @param container the outbound {@link LinkedMessageContainer}
     * @return {@code "hide"} or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoUtils", exports = "decryptFailAttributeFromProtobuf",
            adaptation = WhatsAppAdaptation.DIRECT)
    String resolveDecryptFail(LinkedMessageContainer container) {
        var message = container.content();
        return switch (message) {
            case ReactionMessage _ -> "hide";
            case EncReactionMessage _ -> "hide";
            case PollUpdateMessage _ -> "hide";
            case KeepInChatMessage _ -> "hide";
            case PinInChatMessage _ -> "hide";
            case EncEventResponseMessage _ -> "hide";
            case SecretEncMessage s
                    when s.secretEncType().orElse(null) == SecretEncMessage.SecretEncType.EVENT_EDIT -> "hide";
            case ProtocolMessage p when p.type().isPresent() -> switch (p.type().get()) {
                case REVOKE, MESSAGE_EDIT, EPHEMERAL_SYNC_RESPONSE, REQUEST_WELCOME_MESSAGE -> "hide";
                default -> null;
            };
            default -> null;
        };
    }

    /**
     * Returns the wire {@code mediatype} attribute value written onto the inner
     * {@code <enc>} child for the given {@link LinkedMessageContainer}.
     *
     * <p>Every chat-fanout and group-skmsg stanza builder uses this. The result
     * matches WA Web's {@code mediaTypeFromProtobuf} and
     * {@code encodeMaybeMediaType} pair; {@code null} means the attribute is
     * dropped (non-media payload).
     *
     * @param container the outbound {@link LinkedMessageContainer}
     * @return the {@code mediatype} value, or {@code null} for non-media
     *         payloads
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon",
            exports = {"mediaTypeFromProtobuf", "encodeMaybeMediaType"},
            adaptation = WhatsAppAdaptation.DIRECT)
    String resolveMediaType(LinkedMessageContainer container) {
        var message = container.content();
        return switch (message) {
            case ImageMessage _ -> "image";
            case VideoMessage v -> v.gifPlayback() ? "gif" : "video";
            case AudioMessage a -> a.ptt() ? "ptt" : "audio";
            case DocumentMessage _ -> "document";
            case StickerMessage _ -> "sticker";
            case LocationMessage l -> l.isLive() ? "livelocation" : "location";
            case LiveLocationMessage _ -> "livelocation";
            case ContactMessage _ -> "vcard";
            case ContactsArrayMessage _ -> "contact_array";
            case GroupInviteMessage _ -> "url";
            case ExtendedTextMessage t when t.matchedText().isPresent() -> "url";
            case PollCreationMessage _ -> null;
            case PollUpdateMessage _ -> null;
            case ReactionMessage _ -> null;
            default -> null;
        };
    }

    /**
     * Returns the wire {@code native_flow_name} attribute value written onto
     * the inner {@code <enc>} child for interactive-response payloads.
     *
     * <p>The native flow name routes the response on the bot backend; only
     * {@link InteractiveResponseMessage} payloads with an inner
     * {@link InteractiveResponseMessage.NativeFlowResponseMessage} carry it.
     *
     * @param container the outbound {@link LinkedMessageContainer}
     * @return the native flow name, or {@code null} when the payload is not a
     *         native-flow response
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "nativeFlowNameTypeFromProtobuf",
            adaptation = WhatsAppAdaptation.DIRECT)
    String resolveNativeFlowName(LinkedMessageContainer container) {
        var message = container.content();
        if (!(message instanceof InteractiveResponseMessage irm)) {
            return null;
        }

        var content = irm.content();
        if (content.isEmpty() || !(content.get() instanceof InteractiveResponseMessage.NativeFlowResponseMessage nfr)) {
            return null;
        }

        return nfr.name()
                .orElse(null);
    }

    /**
     * Builds the {@code <device-identity>} child carrying this device's stored
     * ADV-signed identity, or {@code null} when no identity is available.
     *
     * <p>Recipients need the ADV-signed identity to verify a PKMSG envelope on
     * first contact; subclasses emit the child only when at least one per-device
     * payload is PKMSG.
     *
     * @return the {@code <device-identity>} {@link Stanza}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureApi", exports = "getADVEncodedIdentity",
            adaptation = WhatsAppAdaptation.DIRECT)
    Stanza buildIdentityNode() {
        return store.signalStore().signedDeviceIdentity()
                .map(identity -> new StanzaBuilder()
                        .description("device-identity")
                        .content(ADVSignedDeviceIdentitySpec.encode(identity))
                        .build())
                .orElse(null);
    }

    /**
     * Returns whether the given {@link KeepInChatMessage} represents an
     * undo-keep-for-all by the original sender.
     *
     * <p>Drives the
     * {@link #resolveEditAttribute(LinkedMessageContainer, boolean)} branch that maps
     * undo-keep-for-all to the sender-revoke value ({@code "7"}); the operation
     * is allowed only on messages the caller originally sent.
     *
     * @param keep the keep-in-chat payload
     * @return {@code true} when {@code keep} is a fromMe undo-keep-for-all
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "editAttribute",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean isUndoKeepForAll(KeepInChatMessage keep) {
        return keep.key().isPresent()
                && keep.key().get().fromMe()
                && keep.keepType().orElse(null) == ChatKeepType.UNDO_KEEP_FOR_ALL;
    }

    /**
     * Commits the {@code E2eMessageSendEvent} (event id 476) for a single
     * per-device Signal encryption result.
     *
     * <p>Called from the per-device loop in
     * {@link #encryptForDevices(MessageEncryption, Collection, LinkedMessageContainer, Jid, IcdcResult, IcdcResult)}
     * and from {@link PeerMessageSender#doSend(Jid, com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo)}.
     *
     * @implNote
     * This implementation collapses WA Web's
     * {@code postSuccessDirectE2eMessageSendMetric} and
     * {@code postFailureDirectE2eMessageSendMetric} helpers into one emission
     * point parameterised by {@code success}, with the per-branch field
     * population (hosted COEX flag, agent-engagement flag) handled inline.
     *
     * @param device         the recipient device {@link Jid}
     * @param container      the encrypted {@link LinkedMessageContainer}, or
     *                       {@code null} when the encryption carried no
     *                       user-visible payload
     * @param success        {@code true} for a successful encryption
     * @param ciphertextType the resolved Signal ciphertext type on success, or
     *                       {@code null} on failure
     * @param retryCount     the retry count passed through from the sender;
     *                       {@code 0} for a fresh send
     */
    @WhatsAppWebExport(moduleName = "WAWebPostE2eMessageSendMetric",
            exports = {"postSuccessDirectE2eMessageSendMetric", "postFailureDirectE2eMessageSendMetric"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    void emitE2eMessageSendEvent(Jid device, LinkedMessageContainer container, boolean success,
                                 MessageEncryptionType ciphertextType,
                                 int retryCount) {
        var builder = new E2eMessageSendEventBuilder()
                .e2eCiphertextVersion(MessageEncryption.CIPHERTEXT_VERSION)
                .isLid(device.hasLidServer())
                .retryCount(retryCount)
                .editType(EditType.NOT_EDITED)
                .e2eDestination(E2eDestination.INDIVIDUAL)
                .e2eSuccessful(success);
        if (device.hasServer(JidServer.hosted())) {
            builder.encryptionType(EncryptionTypeCode.COEX);
        }
        if (ciphertextType != null) {
            builder.e2eCiphertextType(mapCiphertextType(ciphertextType));
        }
        if (container != null) {
            var mediaType = mapMediaType(container);
            if (mediaType != null) {
                builder.messageMediaType(mediaType);
            }
            if (device.isBot()) {
                builder.agentEngagementType(AgentEngagementEnumType.INVOKED);
            }
        }
        wamService.commit(builder.build());
    }

    /**
     * Commits the {@code E2eMessageSendEvent} (event id 476) for a sender-key
     * (SKMSG) encryption result covering an entire group or status fanout.
     *
     * <p>Called once per group send and once per status broadcast; the
     * destination ({@link E2eDestination#GROUP} or {@link E2eDestination#STATUS})
     * and the addressing mode are reflected on the emitted event.
     *
     * @param groupOrStatusJid    the SKMSG target {@link Jid}
     * @param container           the {@link LinkedMessageContainer} being encrypted
     * @param destination         the semantic destination
     *                            ({@link E2eDestination#GROUP} or
     *                            {@link E2eDestination#STATUS})
     * @param isLidAddressingMode whether the fanout uses LID addressing
     * @param success             {@code true} for a successful encryption
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptMsgProtobuf", exports = "encryptMsgSenderKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    void emitE2eMessageSendSenderKeyEvent(Jid groupOrStatusJid, LinkedMessageContainer container,
                                          E2eDestination destination, boolean isLidAddressingMode,
                                          boolean success) {
        var builder = new E2eMessageSendEventBuilder()
                .e2eSuccessful(success)
                .e2eCiphertextType(E2eCiphertextType.SENDER_KEY_MESSAGE)
                .e2eCiphertextVersion(MessageEncryption.CIPHERTEXT_VERSION)
                .e2eDestination(destination)
                .retryCount(0)
                .isLid(isLidAddressingMode)
                .editType(mapEditType(container))
                .localAddressingMode(isLidAddressingMode ? AddressingMode.LID : AddressingMode.PN);
        if (container != null) {
            var mediaType = mapMediaType(container);
            if (mediaType != null) {
                builder.messageMediaType(mediaType);
            }
        }
        wamService.commit(builder.build());
    }

    /**
     * Commits the {@code AndroidMessageSendPerfEvent} (event id 1994) that
     * records the just-completed send's stage timing and terminal result.
     *
     * <p>Every send routed through {@link #send(Jid, LinkedMessageInfo)} emits one of
     * these regardless of chat kind. The {@code builder} is created and its
     * {@code durationT}/{@code durationRelative} timers started by
     * {@link #send(Jid, LinkedMessageInfo)} immediately before the dispatch, so this
     * method stops those timers to record the real wall-clock cost, stamps the
     * terminal {@link ClientMessageSendStage#CLIENT_WRITTEN_WIRE} stage, and
     * folds the parsed {@link AckResult} into the {@link MessageSendResultType}
     * ({@link MessageSendResultType#OK} on accept, {@link MessageSendResultType#SERVER_ERROR}
     * on a NACK). The remaining slots mirror WA Web's {@code MessageSendPerfReporter}
     * finalisation: media type, message type, edit type, revoke/forward/LID
     * flags, and the host processor count.
     *
     * @implNote
     * This implementation fabricates the values WA Web derives from runtime
     * state Cobalt does not track at the wire boundary ({@code fetchPrekeys},
     * {@code isE2eBackfill}, {@code sendRetryCount}, {@code sendCount}) using the
     * common-case constants a fresh single-attempt send would report, and reads
     * {@code phoneCores} from {@link Runtime#availableProcessors()}.
     *
     * @param chatJid   the send destination JID
     * @param container the outbound {@link LinkedMessageContainer}
     * @param builder   the perf builder whose timers were started before the
     *                  dispatch
     * @param ack       the parsed server {@link AckResult}
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageSendPerfReporter", exports = "MessageSendPerfReporter",
            adaptation = WhatsAppAdaptation.ADAPTED)
    void emitMessageSendPerfEvent(Jid chatJid, LinkedMessageContainer container,
                                  AndroidMessageSendPerfEventBuilder builder, AckResult ack) {
        var editType = mapEditType(container);
        builder.stopDurationT()
                .stopDurationRelative()
                .sendStage(ClientMessageSendStage.CLIENT_WRITTEN_WIRE)
                .messageSendResult(ack.isSuccess() ? MessageSendResultType.OK : MessageSendResultType.SERVER_ERROR)
                .messageType(mapMessageType(chatJid))
                .editType(editType)
                .isRevokeMessage(editType == EditType.SENDER_REVOKE)
                .isMessageForward(isForwarded(container))
                .isLid(chatJid.hasLidServer())
                .isE2eBackfill(false)
                .fetchPrekeys(false)
                .sendRetryCount(0)
                .sendCount(1)
                .phoneCores(Runtime.getRuntime().availableProcessors());
        var mediaType = mapMediaType(container);
        if (mediaType != null) {
            builder.mediaType(mediaType);
        }
        wamService.commit(builder.build());
    }

    /**
     * Dispatches the content-specific per-send WAM beacons for the payload just
     * written to the wire.
     *
     * <p>A {@link StickerMessage} commits the {@code StickerSendEvent}; a
     * {@link KeepInChatMessage} commits the keep-in-chat family
     * ({@code DisappearingMessageKeepInChatEvent}, {@code KeepInChatPerfEvent},
     * and on a NACK {@code KeepInChatErrorsEvent}); a
     * {@link RequestPhoneNumberMessage} commits the
     * {@code PnhRequestRevealActionEvent}. Every other payload emits nothing
     * here (the perf beacon in {@link #emitMessageSendPerfEvent} already covered
     * the generic send).
     *
     * @param chatJid   the send destination JID
     * @param container the outbound {@link LinkedMessageContainer}
     * @param ack       the parsed server {@link AckResult}
     */
    void emitContentSendTelemetry(Jid chatJid, LinkedMessageContainer container, AckResult ack) {
        switch (container.content()) {
            case StickerMessage sticker -> emitStickerSendEvent(sticker);
            case KeepInChatMessage keep -> emitKeepInChatEvents(chatJid, keep, ack);
            case RequestPhoneNumberMessage _ -> emitPnhRequestRevealEvent(chatJid);
            default -> {
            }
        }
    }

    /**
     * Commits the {@code StickerSendEvent} (event id 1840) describing the
     * sticker just dispatched.
     *
     * <p>The animated, avatar, AI, and Lottie flags are read straight off the
     * {@link StickerMessage} proto; the send origin is
     * {@link StickerSendOriginType#FORWARD} when the sticker carries a forwarded
     * context and {@link StickerSendOriginType#STICKER_PICKER_TAB_RECENTS}
     * otherwise.
     *
     * @implNote
     * This implementation fabricates the fields WA Web reads from its runtime
     * {@code mediaData} rather than the wire proto: the first-party flag is
     * inferred from the avatar/AI markers (both are WhatsApp-authored sticker
     * families), the message type is the common {@link StickerSendMessageType#REGULAR}
     * variant, and the sticker-maker, premium, and third-party-source flags
     * default to {@code false} for a proto-sourced send.
     *
     * @param sticker the sticker payload being sent
     */
    @WhatsAppWebExport(moduleName = "WAWebSendStickerAction", exports = "sendStickerToChat",
            adaptation = WhatsAppAdaptation.ADAPTED)
    void emitStickerSendEvent(StickerMessage sticker) {
        var forwarded = sticker.contextInfo()
                .map(ContextInfo::isForwarded)
                .orElse(false);
        var firstParty = sticker.isAvatar() || sticker.isAiSticker();
        var event = new StickerSendEventBuilder()
                .stickerSendOrigin(forwarded
                        ? StickerSendOriginType.FORWARD
                        : StickerSendOriginType.STICKER_PICKER_TAB_RECENTS)
                .stickerSendMessageType(StickerSendMessageType.REGULAR)
                .stickerIsAnimated(sticker.isAnimated())
                .stickerIsAvatar(sticker.isAvatar())
                .stickerIsAi(sticker.isAiSticker())
                .stickerIsLottie(sticker.isLottie())
                .stickerIsFirstParty(firstParty)
                .stickerIsFromStickerMaker(false)
                .stickerIsFromUserCreatedPack(false)
                .stickerIsGiphy(false)
                .stickerIsTenor(false)
                .stickerIsKlipy(false)
                .stickerIsText(false)
                .stickerIsPremium(false)
                .build();
        wamService.commit(event);
    }

    /**
     * Commits the keep-in-chat WAM family for a keep or unkeep action being
     * dispatched.
     *
     * <p>Always emits the {@code DisappearingMessageKeepInChatEvent} (event id
     * 3482) user-action beacon and the {@code KeepInChatPerfEvent} (event id
     * 3488) request/response beacon; on a server NACK it additionally emits the
     * {@code KeepInChatErrorsEvent} (event id 3698). The keep type selects the
     * {@link KicActionNameType}/{@link KicRequestTypeType}/{@link KicActionType}
     * triple, the actor is the sender when the kept message is {@code fromMe},
     * and the chat ephemeral timer and thread id are resolved from the store.
     *
     * @implNote
     * This implementation fabricates the group-membership fields WA Web resolves
     * from live group metadata that Cobalt does not carry at the send boundary:
     * the self admin flag defaults to {@code false} and {@code canEditDmSettings}
     * to {@code true} only outside groups, matching a non-admin participant. The
     * kept message's own ephemeral duration is reported as {@code 0} (WA Web's
     * own {@code ||0} fallback) because the referenced message is not resolved
     * here, and the NACK error code collapses to {@link KicErrorCodeType#UNKNOWN}.
     *
     * @param chatJid the send destination JID
     * @param keep    the keep-in-chat payload being sent
     * @param ack     the parsed server {@link AckResult}
     */
    @WhatsAppWebExport(moduleName = "WAWebKeepInChatMsgAction", exports = "logKeepInChatAction",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebEphemeralKeepInChatWamUtils",
            exports = {"getBaseErrorLog", "parseKeepTypeToMetric"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    void emitKeepInChatEvents(Jid chatJid, KeepInChatMessage keep, AckResult ack) {
        var keepType = keep.keepType().orElse(ChatKeepType.UNKNOWN);
        var isGroup = chatJid.hasGroupOrCommunityServer();
        var ephemeralSeconds = chatEphemeralityDurationSeconds(chatJid);
        var thread = threadId(chatJid);
        var actor = keep.key().isPresent() && keep.key().get().fromMe()
                ? KicActorType.SENDER
                : KicActorType.RECIPIENT;

        var action = new DisappearingMessageKeepInChatEventBuilder()
                .isAGroup(isGroup)
                .isAdmin(false)
                .canEditDmSettings(!isGroup)
                .messagesSelected(1)
                .keptCount(1)
                .chatEphemeralityDuration(ephemeralSeconds)
                .kicActor(actor)
                .kicEntryPoint(KicEntryPointType.CHAT);
        if (keepType == ChatKeepType.UNDO_KEEP_FOR_ALL) {
            action.kicActionName(KicActionNameType.UNKEEP_MESSAGE)
                    .keptDelta(0)
                    .messageExpiryTimer(0)
                    .messageExpiredOnUnkeep(false);
        } else {
            action.kicActionName(KicActionNameType.KEEP_MESSAGE);
        }
        if (!thread.isEmpty()) {
            action.threadId(thread);
        }
        wamService.commit(action.build());

        var perf = new KeepInChatPerfEventBuilder()
                .response(ack.isSuccess() ? ResponseType.SUCCESS : ResponseType.ERROR)
                .requestSendTime(Instant.now().getEpochSecond())
                .chatEphemeralityDuration(ephemeralSeconds)
                .kicMessageEphemeralityDuration(0)
                .kicRequestType(keepType == ChatKeepType.UNDO_KEEP_FOR_ALL
                        ? KicRequestTypeType.UNKEEP
                        : KicRequestTypeType.KEEP);
        if (!thread.isEmpty()) {
            perf.threadId(thread);
        }
        wamService.commit(perf.build());

        if (!ack.isSuccess()) {
            var errors = new KeepInChatErrorsEventBuilder()
                    .kicAction(keepType == ChatKeepType.UNDO_KEEP_FOR_ALL
                            ? KicActionType.UNKEEP_MESSAGE
                            : KicActionType.KEEP_MESSAGE)
                    .isAGroup(isGroup)
                    .isAdmin(false)
                    .canEditDmSettings(!isGroup)
                    .kicErrorCode(KicErrorCodeType.UNKNOWN)
                    .kicMessageEphemeralityDuration(0)
                    .build();
            wamService.commit(errors);
        }
    }

    /**
     * Commits the {@code PnhRequestRevealActionEvent} (event id 3808) for a
     * phone-number-reveal request being sent into a LID chat.
     *
     * <p>The action is fixed to {@link PnhActionType#SEND_REQUEST} from the
     * {@link PnhEntryPointType#PN_REQUEST} entry point; the thread id is the
     * store-derived HMAC when a thread-logging secret has been provisioned.
     *
     * @implNote
     * This implementation reports {@link PnhMessageChatParty#CONSUMER} and omits
     * the optional {@code pnhChatType} because Cobalt does not resolve the
     * counterpart's business classification or the CTWA origin at the send
     * boundary; WA Web reads both from the active chat's UI model.
     *
     * @param chatJid the send destination JID
     */
    @WhatsAppWebExport(moduleName = "WAWebLogRequestPhoneNumber", exports = "logPnhRequestRevealActionHelper",
            adaptation = WhatsAppAdaptation.ADAPTED)
    void emitPnhRequestRevealEvent(Jid chatJid) {
        var event = new PnhRequestRevealActionEventBuilder()
                .pnhAction(PnhActionType.SEND_REQUEST)
                .pnhChatParty(PnhMessageChatParty.CONSUMER)
                .pnhEntryPoint(PnhEntryPointType.PN_REQUEST);
        var thread = threadId(chatJid);
        if (!thread.isEmpty()) {
            event.threadId(thread);
        }
        wamService.commit(event.build());
    }

    /**
     * Classifies the destination {@link Jid} into the WAM {@link MessageType}
     * used on outbound perf events.
     *
     * <p>Status broadcasts, newsletters, groups/communities, broadcast lists,
     * and interop bridges map to their dedicated constants; every other
     * destination is an {@link MessageType#INDIVIDUAL} chat.
     *
     * @param chatJid the send destination JID
     * @return the matching {@link MessageType}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMsgUtils", exports = "getWamMessageType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static MessageType mapMessageType(Jid chatJid) {
        if (chatJid.isStatusBroadcastAccount()) {
            return MessageType.STATUS;
        }
        if (chatJid.hasNewsletterServer()) {
            return MessageType.CHANNEL;
        }
        if (chatJid.hasGroupOrCommunityServer()) {
            return MessageType.GROUP;
        }
        if (chatJid.hasBroadcastServer()) {
            return MessageType.BROADCAST;
        }
        if (chatJid.hasInteropServer()) {
            return MessageType.INTEROP;
        }
        return MessageType.INDIVIDUAL;
    }

    /**
     * Returns whether the container's payload carries a forwarded context.
     *
     * <p>Only {@link ContextualMessage} payloads expose the forwarding flag;
     * control payloads report {@code false}.
     *
     * @param container the outbound {@link LinkedMessageContainer}
     * @return {@code true} when the payload is a forward
     */
    private static boolean isForwarded(LinkedMessageContainer container) {
        return container.content() instanceof ContextualMessage contextual
                && contextual.contextInfo()
                .map(ContextInfo::isForwarded)
                .orElse(false);
    }

    /**
     * Returns the chat's ephemeral-timer duration in seconds, or {@code 0} when
     * disappearing messages are off or the chat is unknown.
     *
     * @param chatJid the chat JID
     * @return the ephemeral duration in seconds
     */
    @WhatsAppWebExport(moduleName = "WAWebChatEphemerality", exports = "getEphemeralSetting",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private long chatEphemeralityDurationSeconds(Jid chatJid) {
        return store.chatStore()
                .findChatByJid(chatJid)
                .flatMap(Chat::ephemeralExpiration)
                .map(ChatEphemeralTimer::periodSeconds)
                .orElse(0);
    }

    /**
     * Returns the per-thread HMAC thread id for the given chat, or the empty
     * string when no thread-logging secret has been provisioned.
     *
     * <p>Delegates to {@link LiveThreadLoggingService#chatThreadIdHmac(LinkedWhatsAppClient, String)}
     * so the {@code threadId} slot stamped on keep-in-chat and PNH events
     * matches the value the {@code ThreadInteractionData} uploader reports for
     * the same conversation.
     *
     * @param chatJid the chat JID
     * @return the Base64 HMAC thread id, or the empty string
     */
    @WhatsAppWebExport(moduleName = "WAWebChatThreadLogging", exports = "getChatThreadID",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private String threadId(Jid chatJid) {
        return LiveThreadLoggingService.chatThreadIdHmac(client, chatJid.toString());
    }

    /**
     * Maps a {@link MessageEncryptionType} to the matching WAM
     * {@link E2eCiphertextType}.
     *
     * <p>Used by {@link #emitE2eMessageSendEvent} to populate the
     * {@code e2eCiphertextType} slot on per-device events.
     *
     * @param type the Signal ciphertext type
     * @return the matching {@link E2eCiphertextType}
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "getMetricE2eCiphertextType",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static E2eCiphertextType mapCiphertextType(MessageEncryptionType type) {
        return switch (type) {
            case MSG -> E2eCiphertextType.MESSAGE;
            case PKMSG -> E2eCiphertextType.PREKEY_MESSAGE;
            case SKMSG -> E2eCiphertextType.SENDER_KEY_MESSAGE;
            case MSMSG -> E2eCiphertextType.MESSAGE_SECRET_MESSAGE;
        };
    }

    /**
     * Maps the content of the given {@link LinkedMessageContainer} to the matching
     * WAM {@link MediaType}.
     *
     * <p>Populates the {@code messageMediaType} slot on the WAM
     * {@code E2eMessageSendEvent}; non-classifiable payloads return
     * {@code null} so the field is omitted.
     *
     * @param container the outbound {@link LinkedMessageContainer}
     * @return the matching {@link MediaType}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMsgUtils", exports = "getWamMediaType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static MediaType mapMediaType(LinkedMessageContainer container) {
        var message = container.content();
        return switch (message) {
            case ImageMessage _ -> MediaType.PHOTO;
            case VideoMessage v -> v.gifPlayback() ? MediaType.GIF : MediaType.VIDEO;
            case AudioMessage a -> a.ptt() ? MediaType.PTT : MediaType.AUDIO;
            case DocumentMessage _ -> MediaType.DOCUMENT;
            case StickerMessage _ -> MediaType.STICKER;
            case LocationMessage l -> l.isLive() ? MediaType.LIVE_LOCATION : MediaType.LOCATION;
            case LiveLocationMessage _ -> MediaType.LIVE_LOCATION;
            case ContactMessage _ -> MediaType.CONTACT;
            case ContactsArrayMessage _ -> MediaType.CONTACT_ARRAY;
            case GroupInviteMessage _ -> MediaType.URL;
            case ExtendedTextMessage t when t.matchedText().isPresent() -> MediaType.URL;
            case ExtendedTextMessage _ -> MediaType.TEXT;
            case ReactionMessage _ -> MediaType.REACTION;
            case EncReactionMessage _ -> MediaType.REACTION;
            case PollCreationMessage _ -> MediaType.POLL_CREATE;
            case PollUpdateMessage _ -> MediaType.POLL_VOTE;
            case PollResultSnapshotMessage _ -> MediaType.POLL_RESULT_SNAPSHOT;
            case EventMessage _ -> MediaType.EVENT_CREATE;
            case EncEventResponseMessage _ -> MediaType.EVENT_RESPOND;
            case KeepInChatMessage _ -> MediaType.KEEP;
            default -> null;
        };
    }

    /**
     * Encodes the protobuf decrypt-fail flag into the wire
     * {@code decrypt-fail} attribute value.
     *
     * <p>Returns {@code "hide"} when the message wants the failure suppressed on
     * the receiver and {@code null} otherwise (the caller drops the attribute).
     * A {@code null} input is treated as the no-attribute case.
     *
     * @param hide the protobuf decrypt-fail flag, or {@code null}
     * @return {@code "hide"} or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "encodeMaybeDecryptFail",
            adaptation = WhatsAppAdaptation.DIRECT)
    static String encodeMaybeDecryptFail(Boolean hide) {
        if (hide == null) {
            return null;
        }
        return hide ? "hide" : null;
    }

    /**
     * Echoes the supplied native-flow-name string for the wire
     * {@code native_flow_name} attribute.
     *
     * <p>A pass-through helper preserved for parity with WA Web's
     * {@code encodeMaybeNativeFlowName}; the caller drops the attribute when the
     * value is {@code null}.
     *
     * @param nativeFlowName the resolved native flow name, or {@code null}
     * @return the same value
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "encodeMaybeNativeFlowName",
            adaptation = WhatsAppAdaptation.DIRECT)
    static String encodeMaybeNativeFlowName(String nativeFlowName) {
        return nativeFlowName;
    }

    /**
     * Maps the wire {@code edit} attribute value to the WAM {@link EditType}
     * classification.
     *
     * <p>The mapping mirrors WA Web's {@code getMetricEditType}: {@code "7"} is
     * {@link EditType#SENDER_REVOKE}, {@code "8"} is {@link EditType#ADMIN_REVOKE},
     * {@code "1"} is {@link EditType#EDITED}, everything else (including
     * {@code null}) is {@link EditType#NOT_EDITED}.
     *
     * @param editAttr the {@code edit} attribute value, or {@code null}
     * @return the matching {@link EditType}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "getMetricEditType",
            adaptation = WhatsAppAdaptation.DIRECT)
    static EditType getMetricEditType(String editAttr) {
        if (editAttr == null) {
            return EditType.NOT_EDITED;
        }
        return switch (editAttr) {
            case "7" -> EditType.SENDER_REVOKE;
            case "8" -> EditType.ADMIN_REVOKE;
            case "1" -> EditType.EDITED;
            default -> EditType.NOT_EDITED;
        };
    }

    /**
     * Returns the placeholder reason the receiver should record for the given
     * decryption error.
     *
     * <p>The inbound pipeline calls this when an opaque ciphertext fails to
     * decrypt; the returned {@link PlaceholderReasonType} drives the downstream
     * retry-or-give-up classification. A {@code null} return preserves WA Web's
     * implicit {@code undefined} fall-through which skips placeholder insertion
     * entirely.
     *
     * @implNote
     * This implementation extends the WA Web table with a dedicated branch for
     * {@link WhatsAppMessageException.Receive.UnknownDevice}; everything inside
     * the Signal {@link WhatsAppMessageException.Receive}-family hierarchy maps
     * to the matching {@code SIGNAL_*} constant, and unrelated errors fall
     * through to {@code null}.
     *
     * @param error the exception raised by inbound decryption
     * @return the matching {@link PlaceholderReasonType}, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "getPlaceholderAddReason",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static PlaceholderReasonType getPlaceholderAddReason(Throwable error) {
        if (error instanceof WhatsAppMessageException.Receive.UnknownDevice) {
            return PlaceholderReasonType.UNKNOWN_COMPANION_NO_PREKEY;
        }
        if (error instanceof WhatsAppMessageException.Receive) {
            return switch (error) {
                case WhatsAppMessageException.Receive.NoSession _,
                     WhatsAppMessageException.Receive.NoSenderKey _ ->
                        PlaceholderReasonType.SIGNAL_NO_SESSION;
                case WhatsAppMessageException.Receive.InvalidMessage _ ->
                        PlaceholderReasonType.SIGNAL_INVALID_MESSAGE;
                case WhatsAppMessageException.Receive.InvalidKey _,
                     WhatsAppMessageException.Receive.InvalidOneTimeKey _,
                     WhatsAppMessageException.Receive.InvalidSignedPreKey _ ->
                        PlaceholderReasonType.SIGNAL_INVALID_KEY;
                case WhatsAppMessageException.Receive.FutureMessage _ ->
                        PlaceholderReasonType.SIGNAL_FUTURE_MESSAGE;
                case WhatsAppMessageException.Receive.BadMac _ ->
                        PlaceholderReasonType.SIGNAL_BAD_MAC;
                default -> PlaceholderReasonType.OTHER;
            };
        }
        return null;
    }

    /**
     * Returns the {@code push_priority} value assigned to non-critical inbound
     * notifications.
     *
     * <p>Returns {@code "OFFLINE"} while the runtime is replaying queued traffic
     * on reconnect and {@code "LOW"} during normal online operation, matching WA
     * Web's {@code getNonCriticalNotificationPriority}; the same mapping is
     * needed for retransmission scheduling.
     *
     * @param isOffline {@code true} when the runtime is draining the offline
     *                  backlog
     * @return {@code "OFFLINE"} or {@code "LOW"}
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "getNonCriticalNotificationPriority",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static String getNonCriticalNotificationPriority(boolean isOffline) {
        return isOffline ? "OFFLINE" : "LOW";
    }

    /**
     * Maps the given {@link LinkedMessageContainer} to the WAM {@link EditType}
     * classification used on outbound metric events.
     *
     * <p>Used by {@link #emitE2eMessageSendSenderKeyEvent} to populate the
     * {@code editType} slot on SKMSG events without requiring callers to
     * round-trip through the wire {@code edit} string.
     *
     * @param container the outbound {@link LinkedMessageContainer}, possibly
     *                  {@code null}
     * @return the matching {@link EditType}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "getMetricEditTypeFromMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static EditType mapEditType(LinkedMessageContainer container) {
        if (container == null) {
            return EditType.NOT_EDITED;
        }
        var message = container.content();
        if (message instanceof ProtocolMessage p) {
            var type = p.type().orElse(null);
            if (type == ProtocolMessage.Type.REVOKE) {
                return EditType.SENDER_REVOKE;
            }
            if (type == ProtocolMessage.Type.MESSAGE_EDIT) {
                return EditType.EDITED;
            }
        }
        return EditType.NOT_EDITED;
    }
}
