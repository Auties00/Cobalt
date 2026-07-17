package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.dedup.MessageDedup;
import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.message.send.bot.BotProtobufTransform;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution;
import com.github.auties00.cobalt.message.send.stanza.*;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipantLabel;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.FutureProofMessageType;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactsArrayMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.AudioMessage;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.message.media.StickerMessage;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.media.transcode.MediaTranscoderService;
import com.github.auties00.cobalt.privacy.LiveTrustedContactTokenService;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamMsgUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.GroupMemberTagUpdateEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MessageSendEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SendDocumentEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.StatusReplyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcMessageSendEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ChatOriginsType;
import com.github.auties00.cobalt.wire.wam.type.DocumentType;
import com.github.auties00.cobalt.wire.wam.type.GroupMemberTagEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.GroupMemberTagUpdateActionType;
import com.github.auties00.cobalt.wire.wam.type.MessageDistributionEnumType;
import com.github.auties00.cobalt.wire.wam.type.MessageSendResultType;
import com.github.auties00.cobalt.wire.wam.type.ReplyEntryMethod;
import com.github.auties00.cobalt.wire.wam.type.RevokeType;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusContentType;
import com.github.auties00.cobalt.wire.wam.type.StatusPosterContactType;
import com.github.auties00.cobalt.wire.wam.type.StatusReplyMessageType;
import com.github.auties00.cobalt.wire.wam.type.StatusReplyResult;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.lang.System.Logger.Level;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Live implementation of {@link MessageSendingService} that orchestrates every outgoing-message
 * wire dispatch.
 *
 * <p>The service prepares a raw {@link LinkedMessageContainer} into a fully-populated
 * {@link LinkedMessageInfo} via {@link MessagePreparer}, dedupes concurrent sends keyed by message id
 * via {@link MessageDedup}, brackets the dispatch in a {@code WebcMessageSend} WAM event, then
 * routes by the parent JID's server kind to one of the per-chat-kind sub-senders:
 *
 * <ul>
 *   <li>1:1 PN or LID chat to {@link UserMessageSender}: per-device Signal
 *       fanout with optional phash-mismatch resend.</li>
 *   <li>group or community to {@link GroupMessageSender}: sender-key SKMSG,
 *       sender-key distribution on first contact, addressing-mode migration on
 *       stale-ack errors.</li>
 *   <li>status broadcast to {@link StatusMessageSender}: SKMSG against the
 *       audience derived from the user's status privacy preferences.</li>
 *   <li>business broadcast list to {@link BroadcastMessageSender}: SKMSG against
 *       the audience read from the local
 *       {@link com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastList}
 *       roster.</li>
 *   <li>newsletter to {@link NewsletterMessageSender}: plaintext SMAX publish,
 *       no Signal envelope.</li>
 * </ul>
 *
 * <p>Peer protocol messages (app-state sync, fatal-exception notification,
 * peer-data ops) bypass the dispatch switch and go through
 * {@link #sendPeer(Jid, ChatMessageInfo)}, which delegates straight to
 * {@link PeerMessageSender}.
 */
@WhatsAppWebModule(moduleName = "WAWebSendMsgJob")
@WhatsAppWebModule(moduleName = "WAWebEncryptAndSendStatusMsg")
@WhatsAppWebModule(moduleName = "WAWebNewsletterSendMessageQueryJob")
@WhatsAppWebModule(moduleName = "WAWebSendAppStateSyncMsgJob")
@WhatsAppWebModule(moduleName = "WAWebSendMsgRecordAction")
@WhatsAppWebModule(moduleName = "WAWebMessageSendReporter")
@WhatsAppWebModule(moduleName = "WAWebLogStatusReply")
public final class LiveMessageSendingService implements MessageSendingService {
    /**
     * The logger for {@link LiveMessageSendingService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveMessageSendingService.class);

    /**
     * Holds the fixed mapping from lower-case file extensions (without the
     * leading dot) to the corresponding WAM {@link DocumentType} bucket
     * consumed by {@link #logSendDocumentEvent(String, long)}.
     *
     * <p>It is populated verbatim from WA Web's
     * {@code WAWebProcessRawMediaLogging} extension-to-type table so the
     * emitted {@code documentType} and {@code documentExt} fields match the
     * upstream wire shape exactly. Extensions not in the table fall back to
     * {@link DocumentType#OTHER} with an empty {@code documentExt}.
     *
     * @implNote
     * The WA Web table classifies {@code wmv} as
     * {@link DocumentType#AUDIO} despite {@code wmv} being a video
     * container; the entry is kept verbatim because the WAM backend
     * expects this exact bucketing.
     */
    @WhatsAppWebExport(moduleName = "WAWebProcessRawMediaLogging", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final Map<String, DocumentType> DOCUMENT_EXT_TO_TYPE = Map.<String, DocumentType>ofEntries(
            Map.entry("ai", DocumentType.IMAGE),
            Map.entry("ico", DocumentType.IMAGE),
            Map.entry("jpeg", DocumentType.IMAGE),
            Map.entry("jpg", DocumentType.IMAGE),
            Map.entry("png", DocumentType.IMAGE),
            Map.entry("ps", DocumentType.IMAGE),
            Map.entry("psd", DocumentType.IMAGE),
            Map.entry("svg", DocumentType.IMAGE),
            Map.entry("tif", DocumentType.IMAGE),
            Map.entry("tiff", DocumentType.IMAGE),
            Map.entry("3g2", DocumentType.VIDEO),
            Map.entry("3gp", DocumentType.VIDEO),
            Map.entry("avi", DocumentType.VIDEO),
            Map.entry("flv", DocumentType.VIDEO),
            Map.entry("h264", DocumentType.VIDEO),
            Map.entry("m4v", DocumentType.VIDEO),
            Map.entry("mkv", DocumentType.VIDEO),
            Map.entry("mov", DocumentType.VIDEO),
            Map.entry("mp4", DocumentType.VIDEO),
            Map.entry("mpg", DocumentType.VIDEO),
            Map.entry("mpeg", DocumentType.VIDEO),
            Map.entry("rm", DocumentType.VIDEO),
            Map.entry("vob", DocumentType.VIDEO),
            Map.entry("wmv", DocumentType.AUDIO),
            Map.entry("aif", DocumentType.AUDIO),
            Map.entry("cda", DocumentType.AUDIO),
            Map.entry("mpa", DocumentType.AUDIO),
            Map.entry("opus", DocumentType.AUDIO),
            Map.entry("ogg", DocumentType.AUDIO),
            Map.entry("wlp", DocumentType.AUDIO),
            Map.entry("amr", DocumentType.AUDIO),
            Map.entry("mp3", DocumentType.AUDIO),
            Map.entry("m4a", DocumentType.AUDIO),
            Map.entry("aac", DocumentType.AUDIO),
            Map.entry("wav", DocumentType.AUDIO),
            Map.entry("wma", DocumentType.AUDIO),
            Map.entry("pdf", DocumentType.DOCUMENT),
            Map.entry("doc", DocumentType.DOCUMENT),
            Map.entry("docx", DocumentType.DOCUMENT),
            Map.entry("ppt", DocumentType.DOCUMENT),
            Map.entry("pptx", DocumentType.DOCUMENT),
            Map.entry("xls", DocumentType.DOCUMENT),
            Map.entry("xlsx", DocumentType.DOCUMENT),
            Map.entry("txt", DocumentType.DOCUMENT),
            Map.entry("rtf", DocumentType.DOCUMENT),
            Map.entry("tex", DocumentType.DOCUMENT),
            Map.entry("csv", DocumentType.DOCUMENT),
            Map.entry("wpd", DocumentType.DOCUMENT),
            Map.entry("7z", DocumentType.COMPRESSED_FILE),
            Map.entry("arj", DocumentType.COMPRESSED_FILE),
            Map.entry("deb", DocumentType.COMPRESSED_FILE),
            Map.entry("pkg", DocumentType.COMPRESSED_FILE),
            Map.entry("rar", DocumentType.COMPRESSED_FILE),
            Map.entry("rpm", DocumentType.COMPRESSED_FILE),
            Map.entry("gz", DocumentType.COMPRESSED_FILE),
            Map.entry("z", DocumentType.COMPRESSED_FILE),
            Map.entry("zip", DocumentType.COMPRESSED_FILE),
            Map.entry("apk", DocumentType.EXECUTABLE),
            Map.entry("bat", DocumentType.EXECUTABLE),
            Map.entry("bin", DocumentType.EXECUTABLE),
            Map.entry("cgi", DocumentType.EXECUTABLE),
            Map.entry("pl", DocumentType.EXECUTABLE),
            Map.entry("com", DocumentType.EXECUTABLE),
            Map.entry("exe", DocumentType.EXECUTABLE),
            Map.entry("gadget", DocumentType.EXECUTABLE),
            Map.entry("jar", DocumentType.EXECUTABLE),
            Map.entry("msi", DocumentType.EXECUTABLE),
            Map.entry("py", DocumentType.EXECUTABLE),
            Map.entry("wsf", DocumentType.EXECUTABLE)
    );

    /**
     * Turns a raw {@link LinkedMessageContainer} into a populated {@link LinkedMessageInfo}.
     */
    private final MessagePreparer preparer;

    /**
     * Rejects a second concurrent send carrying the same wire id.
     */
    private final MessageDedup messageDedup;

    /**
     * Handles 1:1 PN- or LID-addressed chats.
     */
    private final UserMessageSender userSender;

    /**
     * Handles group and community chats.
     */
    private final GroupMessageSender groupSender;

    /**
     * Handles status broadcasts.
     */
    private final StatusMessageSender statusSender;

    /**
     * Handles business broadcast-list sends addressed via
     * {@code <id>@broadcast}.
     */
    private final BroadcastMessageSender broadcastSender;

    /**
     * Handles plaintext SMAX newsletter publishes.
     */
    private final NewsletterMessageSender newsletterSender;

    /**
     * Handles app-state-sync and other own-device peer protocol stanzas.
     */
    private final PeerMessageSender peerSender;

    /**
     * Reaches shared services not already injected into the sub-senders.
     */
    private final LinkedWhatsAppClient client;

    /**
     * Records per-send {@code WebcMessageSend} events.
     */
    private final WamService wamService;

    /**
     * Attaches a rich preview to outgoing extended-text messages whose body
     * contains a URL.
     */
    private final MediaTranscoderService mediaTranscoderService;

    /**
     * Holds the opaque unified-session identifier stamped onto the
     * {@code StatusReply} WAM event for the lifetime of this service.
     *
     * <p>WA Web sources this from {@code UnifiedSessionManager.getSessionId()},
     * a value that is stable for a whole app session and shared across every
     * telemetry event. A headless client has no such UI session manager, so a
     * random 64-bit token is minted once at construction and reused, matching
     * the upstream stability contract without inventing per-event churn.
     *
     * @implNote
     * This implementation renders the token as lower-case hexadecimal so it is
     * indistinguishable from the compact identifiers the WAM backend already
     * receives from web sessions.
     */
    private final String unifiedSessionId = Long.toHexString(ThreadLocalRandom.current().nextLong());

    /**
     * Constructs a {@link LiveMessageSendingService} and wires up every sub-sender.
     *
     * <p>Embedders construct exactly one of these per client; the constructor
     * also instantiates the link-preview pipeline, the sender-key distribution
     * helper, and the stanza-decoration builders ({@link BotStanza},
     * {@link BizStanza}, {@link MetaStanza}, {@link ReportingStanza},
     * {@link CtwaAttributionStanza}, {@link TcTokenStanza}) that the sub-senders
     * share.
     *
     * @param client                 the {@link LinkedWhatsAppClient} used to dispatch
     *                                stanzas
     * @param encryption             the {@link MessageEncryption} service
     * @param deviceService          the {@link DeviceService} used for fanout
     *                               resolution
     * @param lidMigrationService    the {@link LidMigrationService} consulted by
     *                               the user-chat sender to gate the PN-to-LID
     *                               stanza rewrite
     * @param abPropsService         the {@link ABPropsService} consulted for
     *                               feature-gating decisions
     * @param wamService             the {@link WamService} that records per-send
     *                               events
     * @param mediaTranscoderService the {@link MediaTranscoderService} consulted
     *                               for link-preview decoration on outgoing text
     *                               messages
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgJob", exports = "encryptAndSendMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public LiveMessageSendingService(
            LinkedWhatsAppClient client,
            MessageEncryption encryption,
            DeviceService deviceService,
            LidMigrationService lidMigrationService,
            ABPropsService abPropsService,
            WamService wamService,
            MediaTranscoderService mediaTranscoderService
    ) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(encryption, "encryption");
        Objects.requireNonNull(deviceService, "deviceService");
        Objects.requireNonNull(lidMigrationService, "lidMigrationService");
        Objects.requireNonNull(abPropsService, "abPropsService");
        Objects.requireNonNull(wamService, "wamService");
        Objects.requireNonNull(mediaTranscoderService, "mediaTranscoderService");

        this.client = client;
        this.wamService = wamService;
        this.mediaTranscoderService = mediaTranscoderService;
        var store = client.store();
        this.preparer = new MessagePreparer(store, wamService);
        this.messageDedup = new MessageDedup();
        var skDistribution = new SenderKeyDistribution(encryption, deviceService, store);
        var botTransform = new BotProtobufTransform(store);
        var botStanza = new BotStanza(encryption, botTransform);
        var bizStanza = new BizStanza(store);
        var metaStanza = new MetaStanza(store);
        var reportingStanza = new ReportingStanza(abPropsService);
        var ctwaStanza = new CtwaAttributionStanza(store, abPropsService);
        var tcTokenStanza = new TcTokenStanza(store, abPropsService, new LiveTrustedContactTokenService(abPropsService));

        this.userSender = new UserMessageSender(client, encryption, deviceService, lidMigrationService, abPropsService,
                botStanza, bizStanza, metaStanza, reportingStanza, ctwaStanza, tcTokenStanza, wamService);
        this.groupSender = new GroupMessageSender(client, encryption, deviceService, abPropsService, skDistribution, botStanza, bizStanza, metaStanza, reportingStanza, wamService);
        this.statusSender = new StatusMessageSender(client, encryption, deviceService, abPropsService, skDistribution, metaStanza, reportingStanza, wamService);
        this.broadcastSender = new BroadcastMessageSender(client, encryption, deviceService, abPropsService, skDistribution, metaStanza, reportingStanza, wamService);
        this.newsletterSender = new NewsletterMessageSender(client, abPropsService, wamService);
        this.peerSender = new PeerMessageSender(client, encryption, deviceService, abPropsService, wamService);
    }

    @WhatsAppWebExport(moduleName = "WAWebSendMsgJob", exports = "encryptAndSendMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public AckResult send(Jid chatJid, LinkedMessageContainer container) {
        Objects.requireNonNull(chatJid, "chatJid");
        Objects.requireNonNull(container, "container");

        if (container.content() instanceof ExtendedTextMessage extended) {
            mediaTranscoderService.decorate(chatJid, extended);
        }

        LinkedMessageInfo prepared;
        if (chatJid.hasServer(JidServer.newsletter())) {
            prepared = preparer.prepareNewsletter(chatJid, container);
        } else {
            prepared = preparer.prepareChat(chatJid, container);
        }

        return send(prepared);
    }

    @WhatsAppWebExport(moduleName = "WAWebSendMsgJob", exports = "encryptAndSendMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public AckResult send(LinkedMessageInfo messageInfo) {
        Objects.requireNonNull(messageInfo, "messageInfo");

        var messageId = messageInfo
                .key()
                .id()
                .orElseThrow(() -> new IllegalArgumentException("messageId is required for outgoing messages"));
        var parentJid = messageInfo.key()
                .parentJid()
                .orElseThrow(() -> new IllegalArgumentException("parentJid is required for outgoing messages"));

        if (messageInfo.message() != null
                && messageInfo.message().content() instanceof DocumentMessage document) {
            logSendDocumentEvent(
                    document.fileName().orElse(null),
                    document.mediaSize().orElse(0L));
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "sending message id={0} to {1} kind={2}",
                    messageId, parentJid, messageInfo.getClass().getSimpleName());
        }

        if (messageDedup.isPending(messageId)) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "duplicate send rejected for message id={0}", messageId);
            }
            throw new WhatsAppMessageException.Send.Unknown(
                    "Duplicate send for message ID: " + messageId, null);
        }

        messageDedup.add(messageId);
        try {
            var sendEvent = buildWebcMessageSendEvent(messageInfo);
            var messageSendEvent = buildMessageSendEvent(messageInfo);
            var result = switch (messageInfo) {
                case ChatMessageInfo chatMessage when parentJid.hasUserServer() || parentJid.hasLidServer() ->
                    userSender.send(parentJid, chatMessage);
                case ChatMessageInfo chatMessage when parentJid.hasGroupOrCommunityServer() ->
                    groupSender.send(parentJid, chatMessage);
                case ChatMessageInfo chatMessage when parentJid.isStatusBroadcastAccount() ->
                    statusSender.send(parentJid, chatMessage);
                case ChatMessageInfo chatMessage when parentJid.hasBroadcastServer() ->
                    broadcastSender.send(parentJid, chatMessage);
                case NewsletterMessageInfo newsletterMessage when parentJid.hasNewsletterServer() ->
                    newsletterSender.send(parentJid, newsletterMessage);
                default -> {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "unsupported send combination {0} for {1}",
                                messageInfo.getClass().getSimpleName(), parentJid);
                    }
                    throw new WhatsAppMessageException.Send.InvalidRecipient(
                        parentJid, "Unsupported combination: " + messageInfo.getClass().getSimpleName()
                                + " with JID type " + parentJid.server());
                }
            };
            // Hand the local user's own trusted-contact token to a one-to-one peer after a non-protocol
            // message, matching WAWebSendMsgJob, so the peer keeps a current token across identity
            // rotations; the issue is gated to at most once per sender bucket. Fire-and-forget on a
            // virtual thread so the send path never blocks on the reciprocal token.
            if (messageInfo instanceof ChatMessageInfo
                    && (parentJid.hasUserServer() || parentJid.hasLidServer())
                    && !(messageInfo.message() != null
                            && messageInfo.message().content() instanceof ProtocolMessage)) {
                Thread.ofVirtual().name("tc-token-" + parentJid).start(() -> client.issueTrustedContactToken(parentJid));
            }
            if (sendEvent != null) {
                wamService.commit(sendEvent.stopMessageSendT().build());
            }
            if (messageSendEvent != null) {
                wamService.commit(messageSendEvent
                        .messageSendResult(MessageSendResultType.OK)
                        .messageSendResultIsTerminal(false)
                        .stopMessageSendT()
                        .build());
            }
            if (messageInfo instanceof ChatMessageInfo chatMessage) {
                emitStatusReplyEvent(chatMessage);
                emitGroupMemberTagUpdateEvent(parentJid, chatMessage);
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "message id={0} sent to {1}", messageId, parentJid);
            }
            return result;
        } finally {
            messageDedup.remove(messageId);
        }
    }

    /**
     * Commits the {@code SendDocumentEvent} for an outgoing document
     * send.
     *
     * <p>The document-send pipeline calls this before the upload begins; it
     * mirrors WA Web's
     * {@code WAWebProcessRawMediaLogging.logSendDocumentEvent}. The
     * filename is split on {@code .} and the last segment, lowercased, is
     * looked up in {@link #DOCUMENT_EXT_TO_TYPE} to resolve the
     * {@link DocumentType}; an absent or unknown extension yields an empty
     * {@code documentExt} property and falls back to
     * {@link DocumentType#OTHER}.
     *
     * @implNote
     * This implementation leaves the {@code documentPageSize} WAM
     * property unset because WA Web's emission site declares it on the
     * event spec but never populates it; the field is reserved for
     * future use.
     *
     * @param filename the user-visible document filename;
     *                 {@code null} resolves the extension as the empty
     *                 string (mirroring WA Web's
     *                 {@code e?.split(".").pop() ?? ""} fallback)
     * @param size     the raw decrypted document size in bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebProcessRawMediaLogging", exports = "logSendDocumentEvent",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void logSendDocumentEvent(String filename, long size) {
        String extension;
        if (filename == null || filename.isEmpty()) {
            extension = "";
        } else {
            var dotIndex = filename.lastIndexOf('.');
            var tail = dotIndex < 0 ? filename : filename.substring(dotIndex + 1);
            extension = tail.toLowerCase(Locale.ROOT);
        }
        var normalizedExt = DOCUMENT_EXT_TO_TYPE.containsKey(extension) ? extension : "";
        var documentType = DOCUMENT_EXT_TO_TYPE.getOrDefault(normalizedExt, DocumentType.OTHER);
        wamService.commit(new SendDocumentEventBuilder()
                .documentSize((double) size)
                .documentType(documentType)
                .documentExt(normalizedExt)
                .build());
    }

    /**
     * Builds the pre-configured {@link WebcMessageSendEventBuilder} for the
     * given outgoing message, or returns {@code null} when the event must be
     * suppressed.
     *
     * <p>Suppressed for newsletter sends (a different WA Web logger covers
     * those) and for protocol revoke messages. For every other chat message the
     * builder is pre-populated with the resolved {@code messageType},
     * {@code messageMediaType}, the {@code messageIsForward} flag taken from the
     * optional {@link ContextualMessage#contextInfo()}, and the latency timer is
     * started so it can be stopped before the success-branch commit on the
     * caller side.
     *
     * @param messageInfo the message about to be sent
     * @return the prepared builder, or {@code null} when no event should be
     *         emitted
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgRecordAction", exports = "sendMsgRecord",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private WebcMessageSendEventBuilder buildWebcMessageSendEvent(LinkedMessageInfo messageInfo) {
        if (!(messageInfo instanceof ChatMessageInfo chatMessage)) {
            return null;
        }
        var content = chatMessage.message() == null ? null : chatMessage.message().content();
        if (content instanceof ProtocolMessage protocol
                && protocol.type().orElse(null) == ProtocolMessage.Type.REVOKE) {
            return null;
        }
        var isForwarded = content instanceof ContextualMessage contextual
                && contextual.contextInfo().map(ctx -> ctx.isForwarded()).orElse(false);
        return new WebcMessageSendEventBuilder()
                .messageType(WamMsgUtils.getWamMessageType(chatMessage))
                .messageMediaType(WamMsgUtils.getWamMediaType(chatMessage))
                .messageIsForward(isForwarded)
                .startMessageSendT();
    }

    /**
     * Builds the pre-configured base {@code MessageSend} WAM event
     * ({@link MessageSendEventBuilder}) for the given outgoing message, or
     * returns {@code null} when the event must be suppressed.
     *
     * <p>This is Cobalt's port of WA Web's {@code MessageSendReporter}
     * constructor: it is the central per-message send beacon committed for
     * every chat send, alongside the {@code WebcMessageSend} and
     * {@code E2eMessageSend} variants. It is suppressed for non-chat sends
     * (newsletter publishes carry their own logger). The builder is populated
     * from data available at the trigger:
     *
     * <ul>
     *   <li>{@code messageType} and {@code messageMediaType} from
     *       {@link WamMsgUtils};</li>
     *   <li>{@code mediaCaptionPresent} from {@link #hasCaption(Message)};</li>
     *   <li>{@code messageIsFanout} and {@code fastForwardEnabled} pinned to
     *       {@code true}, and {@code messageDistributionType} to
     *       {@link MessageDistributionEnumType#REGULAR_MESSAGE}, matching the
     *       constants the upstream reporter hard-codes for a regular send;</li>
     *   <li>{@code messageIsForward}, {@code isAReply}, {@code isViewOnce}, and
     *       {@code messageIsRevoke} from the resolved content and its optional
     *       {@link ContextInfo};</li>
     *   <li>{@code isLid} and {@code chatOrigins} from the destination JID's
     *       server kind ({@link ChatOriginsType#LID_CTWA} for LID recipients,
     *       {@link ChatOriginsType#OTHERS} otherwise);</li>
     *   <li>{@code hasUsername} and {@code hasUsernamePin} from the bound
     *       account's own username state.</li>
     * </ul>
     *
     * <p>The {@code messageSendT} latency timer is started here so it can be
     * stopped on the success-branch commit; the terminal
     * {@code messageSendResult}/{@code messageSendResultIsTerminal} are stamped
     * by the caller once the dispatch completes.
     *
     * @implNote
     * This implementation pins {@code e2eBackfill} to {@code false} and
     * {@code isAComment} to {@code false} because Cobalt's send path has no
     * resend-backfill or comment-thread surface; a revoke send additionally
     * records {@link RevokeType#SENDER}, since Cobalt only ever revokes on
     * behalf of the local sender, never as a group admin.
     *
     * @param messageInfo the message about to be sent
     * @return the prepared builder with the send timer running, or
     *         {@code null} when no base send event should be emitted
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageSendReporter", exports = "MessageSendReporter",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private MessageSendEventBuilder buildMessageSendEvent(LinkedMessageInfo messageInfo) {
        if (!(messageInfo instanceof ChatMessageInfo chatMessage)) {
            return null;
        }
        var parentJid = chatMessage.key().parentJid().orElse(null);
        var container = chatMessage.message();
        var content = container == null ? null : container.content();
        var isRevoke = content instanceof ProtocolMessage protocol
                && protocol.type().orElse(null) == ProtocolMessage.Type.REVOKE;
        var contextInfo = content instanceof ContextualMessage contextual
                ? contextual.contextInfo().orElse(null)
                : null;
        var isForwarded = contextInfo != null && contextInfo.isForwarded();
        var isReply = contextInfo != null && contextInfo.quotedMessageId().isPresent();
        var isViewOnce = container != null
                && container.futureProofContentType() == FutureProofMessageType.VIEW_ONCE;
        var isLid = parentJid != null
                && (parentJid.hasLidServer() || parentJid.isStatusBroadcastAccount());
        var chatOrigins = parentJid != null && parentJid.hasLidServer()
                ? ChatOriginsType.LID_CTWA
                : ChatOriginsType.OTHERS;
        var account = client.store().accountStore();
        var builder = new MessageSendEventBuilder()
                .messageType(WamMsgUtils.getWamMessageType(chatMessage))
                .messageMediaType(WamMsgUtils.getWamMediaType(chatMessage))
                .mediaCaptionPresent(hasCaption(content))
                .fastForwardEnabled(true)
                .messageIsFanout(true)
                .messageIsForward(isForwarded)
                .messageIsRevoke(isRevoke)
                .isViewOnce(isViewOnce)
                .isAReply(isReply)
                .isAComment(false)
                .e2eBackfill(false)
                .messageDistributionType(MessageDistributionEnumType.REGULAR_MESSAGE)
                .chatOrigins(chatOrigins)
                .isLid(isLid)
                .hasUsername(account.username().isPresent())
                .hasUsernamePin(account.usernameHasRecoveryPin().orElse(false))
                .startMessageSendT();
        if (isRevoke) {
            builder.revokeType(RevokeType.SENDER);
        }
        return builder;
    }

    /**
     * Commits a {@code StatusReply} WAM event when the just-sent message is a
     * reply to someone's status update.
     *
     * <p>This ports WA Web's {@code logStatusReply}. A status reply is an
     * ordinary contextual message addressed to the poster whose quoted
     * {@link ContextInfo} points back at the {@code status@broadcast} account;
     * the event is skipped for every send that does not carry that quoted
     * parent. When it applies, the builder is populated with the reply's own
     * {@link #statusReplyMessageType(Message) message type}, the replied-to
     * status {@link #statusContentType(Message) content type}, the quoted
     * status id, the reply media dimensions when present, and the
     * {@link #statusPosterContactType(Jid) poster contact classification};
     * {@code replyEntryMethod} is fixed to
     * {@link ReplyEntryMethod#TAP_REPLY_BAR} as WA Web does at this call site.
     *
     * @implNote
     * This implementation mints fresh 53-bit {@code statusSessionId} /
     * {@code statusViewerSessionId} tokens (with {@code updatesTabSessionId}
     * mirroring {@code statusSessionId}, exactly as the upstream call passes
     * the same {@code sessionId} to both) because a headless client has no
     * status-viewer UI to originate them; the upstream code likewise refuses
     * to commit until a session id is known, and Cobalt always supplies one.
     * {@code statusCategory} is pinned to
     * {@link StatusCategory#REGULAR_STATUS} and {@code isMentioned} to
     * {@code false} as Cobalt does not model group-status membership or
     * status @-mentions on the reply path.
     *
     * @param chatMessage the chat message that was just sent successfully
     */
    @WhatsAppWebExport(moduleName = "WAWebLogStatusReply", exports = "logStatusReply",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitStatusReplyEvent(ChatMessageInfo chatMessage) {
        var container = chatMessage.message();
        var content = container == null ? null : container.content();
        if (!(content instanceof ContextualMessage contextual)) {
            return;
        }
        var contextInfo = contextual.contextInfo().orElse(null);
        if (contextInfo == null) {
            return;
        }
        var quotedParent = contextInfo.quotedMessageParentJid().orElse(null);
        if (quotedParent == null || !quotedParent.isStatusBroadcastAccount()) {
            return;
        }
        var poster = contextInfo.quotedMessageSenderJid().orElse(null);
        var quotedContent = contextInfo.quotedMessageContent()
                .map(LinkedMessageContainer::content)
                .orElse(null);
        var sessionId = ThreadLocalRandom.current().nextLong(1, 1L << 53);
        var viewerSessionId = ThreadLocalRandom.current().nextLong(1, 1L << 53);
        var builder = new StatusReplyEventBuilder()
                .statusReplyResult(StatusReplyResult.OK)
                .statusReplyMessageType(statusReplyMessageType(content))
                .statusContentType(statusContentType(quotedContent))
                .statusCategory(StatusCategory.REGULAR_STATUS)
                .statusPosterContactType(statusPosterContactType(poster))
                .replyEntryMethod(ReplyEntryMethod.TAP_REPLY_BAR)
                .isMentioned(false)
                .statusSessionId(sessionId)
                .statusViewerSessionId(viewerSessionId)
                .updatesTabSessionId(sessionId)
                .unifiedSessionId(unifiedSessionId);
        contextInfo.quotedMessageId().ifPresent(builder::statusId);
        applyStatusReplyMediaDimensions(builder, content);
        wamService.commit(builder.build());
    }

    /**
     * Commits a {@code GroupMemberTagUpdate} WAM event when the just-sent
     * message applies a member-tag (participant-label) change to a group.
     *
     * <p>This ports WA Web's {@code WAWebGroupMemberTagUpdateLogger}. A
     * member-tag change rides on a {@link ProtocolMessage} of type
     * {@link ProtocolMessage.Type#GROUP_MEMBER_LABEL_CHANGE} addressed to a
     * group or community; the event is skipped for every other send. The
     * {@code groupMemberTagUpdateAction} mirrors the {@code tag_reason} the
     * {@link MetaStanza} writes onto the outgoing {@code <meta>}: a non-empty
     * {@link GroupParticipantLabel#label()} records
     * {@link GroupMemberTagUpdateActionType#UPDATE} (the upstream
     * {@code logUpdateClick} branch), and an empty or absent label records
     * {@link GroupMemberTagUpdateActionType#DELETE_CONFIRM} (the upstream
     * {@code logDeleteConfirm} branch). {@code groupId} carries the destination
     * group JID, {@code userJourneyEventMs} the wall-clock millisecond stamp WA
     * Web sources from {@code WATimeUtils.unixTimeMs()}, and
     * {@code unifiedSessionId} reuses the service-wide session token; the
     * upstream logger refuses to commit when that token is {@code null}, and
     * Cobalt always supplies one.
     *
     * @implNote
     * This implementation pins {@code memberTagEntryPoint} to
     * {@link GroupMemberTagEntryPointType#OTHER} and {@code uiSurface} to
     * {@link TsSurface#GROUP_CHAT} because the headless send path has no
     * member-tag UI entry point to originate them; {@code hasMemberTagAtStart}
     * is derived as {@code true} for a deletion (a tag must have existed to be
     * removed) and {@code false} for an update, since Cobalt does not retain the
     * participant's prior label to tell an addition from an edit. The upstream
     * {@code logError} branch is not reproduced: this call site runs only after
     * the dispatch succeeds, so the action is always a confirmed update or
     * delete.
     *
     * @param parentJid   the destination chat {@link Jid}
     * @param chatMessage the chat message that was just sent successfully
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupMemberTagUpdateLogger", exports = "logUpdateClick",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebGroupMemberTagUpdateLogger", exports = "logDeleteConfirm",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitGroupMemberTagUpdateEvent(Jid parentJid, ChatMessageInfo chatMessage) {
        if (!parentJid.hasGroupOrCommunityServer()) {
            return;
        }
        var container = chatMessage.message();
        var content = container == null ? null : container.content();
        if (!(content instanceof ProtocolMessage protocol)
                || protocol.type().orElse(null) != ProtocolMessage.Type.GROUP_MEMBER_LABEL_CHANGE) {
            return;
        }
        var label = protocol.memberLabel()
                .flatMap(GroupParticipantLabel::label)
                .orElse(null);
        var isDelete = label == null || label.isEmpty();
        var action = isDelete
                ? GroupMemberTagUpdateActionType.DELETE_CONFIRM
                : GroupMemberTagUpdateActionType.UPDATE;
        wamService.commit(new GroupMemberTagUpdateEventBuilder()
                .groupId(parentJid.toString())
                .groupMemberTagUpdateAction(action)
                .hasMemberTagAtStart(isDelete)
                .memberTagEntryPoint(GroupMemberTagEntryPointType.OTHER)
                .uiSurface(TsSurface.GROUP_CHAT)
                .unifiedSessionId(unifiedSessionId)
                .userJourneyEventMs(System.currentTimeMillis())
                .build());
    }

    /**
     * Returns whether the given outgoing content carries a user-visible
     * caption, populating the {@code mediaCaptionPresent} property of the
     * {@code MessageSend} event.
     *
     * <p>Only the media kinds that expose a caption field
     * ({@link ImageMessage}, {@link VideoMessage}, {@link DocumentMessage})
     * can be captioned; every other content type, including {@code null},
     * yields {@code false}.
     *
     * @param content the resolved message content; may be {@code null}
     * @return {@code true} when the content has a non-empty caption
     */
    private static boolean hasCaption(Message content) {
        return switch (content) {
            case ImageMessage image -> image.caption().isPresent();
            case VideoMessage video -> video.caption().isPresent();
            case DocumentMessage document -> document.caption().isPresent();
            case null, default -> false;
        };
    }

    /**
     * Classifies the reply content into the WAM
     * {@link StatusReplyMessageType} recorded on the {@code StatusReply} event.
     *
     * <p>This mirrors the {@code replyType} argument WA Web passes to
     * {@code logStatusReply}, collapsing the reply's payload onto the status
     * reply enumeration.
     *
     * @implNote
     * This implementation maps {@link VideoMessage} to
     * {@link StatusReplyMessageType#GIF_VIDEO}, the only video-bearing
     * constant the enumeration exposes, and returns
     * {@link StatusReplyMessageType#UNKNOWN} for content types Cobalt does not
     * bucket (including {@code null}).
     *
     * @param content the reply content being classified; may be {@code null}
     * @return the status-reply message-type classification
     */
    @WhatsAppWebExport(moduleName = "WAWebLogStatusReply", exports = "logStatusReply",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static StatusReplyMessageType statusReplyMessageType(Message content) {
        return switch (content) {
            case ImageMessage ignored -> StatusReplyMessageType.IMAGE;
            case VideoMessage ignored -> StatusReplyMessageType.GIF_VIDEO;
            case AudioMessage audio -> audio.ptt()
                    ? StatusReplyMessageType.VOICE
                    : StatusReplyMessageType.AUDIO;
            case StickerMessage ignored -> StatusReplyMessageType.STICKER;
            case DocumentMessage ignored -> StatusReplyMessageType.DOCUMENT;
            case LocationMessage ignored -> StatusReplyMessageType.LOCATION;
            case ContactMessage ignored -> StatusReplyMessageType.CONTACT;
            case ContactsArrayMessage ignored -> StatusReplyMessageType.CONTACT_ARRAY;
            case PollCreationMessage ignored -> StatusReplyMessageType.POLL;
            case ExtendedTextMessage ignored -> StatusReplyMessageType.TEXT;
            case null, default -> StatusReplyMessageType.UNKNOWN;
        };
    }

    /**
     * Classifies the replied-to status content into the WAM
     * {@link StatusContentType} recorded on the {@code StatusReply} event.
     *
     * <p>This mirrors the {@code statusContentType} argument WA Web passes to
     * {@code logStatusReply}, derived from the quoted status payload; a
     * {@link VideoMessage} split into {@link StatusContentType#GIF} versus
     * {@link StatusContentType#VIDEO} on its GIF-playback flag.
     *
     * @param quoted the quoted status content; {@code null} when the quoted
     *               payload was not preserved
     * @return the status content-type classification, defaulting to
     *         {@link StatusContentType#PLACEHOLDER} for a missing quote and
     *         {@link StatusContentType#FUTURE} for an unrecognised type
     */
    @WhatsAppWebExport(moduleName = "WAWebLogStatusReply", exports = "logStatusReply",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static StatusContentType statusContentType(Message quoted) {
        return switch (quoted) {
            case ImageMessage ignored -> StatusContentType.PHOTO;
            case VideoMessage video -> video.gifPlayback()
                    ? StatusContentType.GIF
                    : StatusContentType.VIDEO;
            case AudioMessage ignored -> StatusContentType.VOICE;
            case ExtendedTextMessage ignored -> StatusContentType.TEXT;
            case null -> StatusContentType.PLACEHOLDER;
            default -> StatusContentType.FUTURE;
        };
    }

    /**
     * Classifies the status poster relative to the bound account into the WAM
     * {@link StatusPosterContactType} recorded on the {@code StatusReply}
     * event.
     *
     * <p>This mirrors the {@code statusPosterContactType} WA Web derives from
     * the poster contact: the local user maps to
     * {@link StatusPosterContactType#SELF}, a poster present in the local
     * contact roster to {@link StatusPosterContactType#CONTACT}, and everyone
     * else to {@link StatusPosterContactType#UNKNOWN}.
     *
     * @implNote
     * This implementation does not surface
     * {@link StatusPosterContactType#TRUSTED_GROUP_MEMBER} or
     * {@link StatusPosterContactType#CHANNEL}, which depend on group-status and
     * channel-status surfaces Cobalt's reply path does not model.
     *
     * @param poster the JID that posted the replied-to status;
     *               {@code null} yields {@link StatusPosterContactType#UNKNOWN}
     * @return the poster contact-type classification
     */
    @WhatsAppWebExport(moduleName = "WAWebLogStatusReply", exports = "logStatusReply",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private StatusPosterContactType statusPosterContactType(Jid poster) {
        if (poster == null) {
            return StatusPosterContactType.UNKNOWN;
        }
        var account = client.store().accountStore();
        var self = account.jid().map(Jid::toUserJid).orElse(null);
        var selfLid = account.lid().map(Jid::toUserJid).orElse(null);
        var posterUser = poster.toUserJid();
        if (posterUser.equals(self) || posterUser.equals(selfLid)) {
            return StatusPosterContactType.SELF;
        }
        if (client.store().contactStore().findContactByJid(poster).isPresent()) {
            return StatusPosterContactType.CONTACT;
        }
        return StatusPosterContactType.UNKNOWN;
    }

    /**
     * Copies the reply media's pixel dimensions onto the {@code StatusReply}
     * event builder when the reply is an image or video.
     *
     * <p>WA Web fills {@code mediaHeight} and {@code mediaWidth} from the reply
     * attachment; content types without intrinsic dimensions leave both
     * properties unset.
     *
     * @param builder the event builder being populated
     * @param content the reply content whose dimensions are being copied
     */
    private static void applyStatusReplyMediaDimensions(StatusReplyEventBuilder builder, Message content) {
        switch (content) {
            case ImageMessage image -> {
                image.height().ifPresent(builder::mediaHeight);
                image.width().ifPresent(builder::mediaWidth);
            }
            case VideoMessage video -> {
                video.height().ifPresent(builder::mediaHeight);
                video.width().ifPresent(builder::mediaWidth);
            }
            case null, default -> {
                // no intrinsic media dimensions to report
            }
        }
    }

    @WhatsAppWebExport(moduleName = "WAWebSendMsgJob", exports = "encryptAndSendKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSendKeyDistributionMsgAction", exports = "sendKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void sendKeyDistribution(Jid groupJid, MessageKey key) {
        Objects.requireNonNull(groupJid, "groupJid");
        Objects.requireNonNull(key, "key");

        var messageId = key.id()
                .orElseThrow(() -> new IllegalArgumentException(
                        "messageId is required for key distribution"));

        if (!groupJid.hasGroupOrCommunityServer()) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "key distribution rejected: {0} is not a group", groupJid);
            }
            throw new WhatsAppMessageException.Send.InvalidRecipient(
                    groupJid, "Key distribution is only supported for group chats");
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "dispatching key distribution id={0} for {1}", messageId, groupJid);
        }
        groupSender.sendKeyDistribution(groupJid, messageId);
    }

    @WhatsAppWebExport(moduleName = "WAWebSendAppStateSyncMsgJob", exports = "encryptAndSendKeyMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public AckResult sendPeer(Jid targetDevice, ChatMessageInfo messageInfo) {
        Objects.requireNonNull(targetDevice, "targetDevice");
        Objects.requireNonNull(messageInfo, "messageInfo");
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "sending peer message to {0}", targetDevice);
        }
        return peerSender.send(targetDevice, messageInfo);
    }
}
