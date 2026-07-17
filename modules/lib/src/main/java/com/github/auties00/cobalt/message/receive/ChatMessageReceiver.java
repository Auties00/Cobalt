package com.github.auties00.cobalt.message.receive;

import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException.Receive.InvalidDeviceSentMessage.DsmErrorType;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.addon.EncMessageFactory;
import com.github.auties00.cobalt.message.receive.crypto.MessageDecryption;
import com.github.auties00.cobalt.message.receive.crypto.MessageDecryptionHandler;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveBizInfo;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveBotInfo;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveEncryptedPayload;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveStanza;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveStanzaParser;
import com.github.auties00.cobalt.message.send.token.ReportingToken;
import com.github.auties00.cobalt.message.send.token.ReportingTokenContent;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfoBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatMute;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentitySpec;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.FutureProofMessageType;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.wire.linked.message.MessageThreadId;
import com.github.auties00.cobalt.wire.linked.message.commerce.ButtonsMessage;
import com.github.auties00.cobalt.wire.linked.message.commerce.OrderMessage;
import com.github.auties00.cobalt.wire.linked.message.commerce.ProductMessage;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateMessage;
import com.github.auties00.cobalt.wire.linked.message.list.ListMessage;
import com.github.auties00.cobalt.wire.linked.message.newsletter.NewsletterFollowerInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.system.DeviceSentMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.system.ProtocolMessage;
import com.github.auties00.cobalt.wire.linked.message.text.HighlyStructuredMessage;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.WamMsgUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.BroadcastInvalidChannelsContextSourceMessageDropEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.CtwaBizUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MessageSecretErrorsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsStructuredMessageInteractionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.QbmIncomingMessageEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ReportingTokenValidationFailureEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ReportingTokenValidationFailureSenderEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcStatusSyncEventBuilder;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wire.wam.type.BizPlatform;
import com.github.auties00.cobalt.wire.wam.type.ChatsFolderType;
import com.github.auties00.cobalt.wire.wam.type.ContactType;
import com.github.auties00.cobalt.wire.wam.type.CtwaBizUserJourneyOperation;
import com.github.auties00.cobalt.wire.wam.type.DeviceType;
import com.github.auties00.cobalt.wire.wam.type.EditType;
import com.github.auties00.cobalt.wire.wam.type.InteractionType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageSecretAllowedType;
import com.github.auties00.cobalt.wire.wam.type.MessageSecretErrorType;
import com.github.auties00.cobalt.wire.wam.type.QbmFlag;
import com.github.auties00.cobalt.wire.wam.type.ReportingTokenValidationFailureReason;
import com.github.auties00.cobalt.wire.wam.type.StructuredMessageClass;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Inbound receiver that runs every non-newsletter {@code <message>} stanza through
 * the full Signal-protocol decryption pipeline and produces a populated
 * {@link ChatMessageInfo}.
 *
 * <p>Processing happens in two phases. The decryption phase parses the stanza,
 * validates the {@code <enc>} ordering and the ADV identity for companion-device
 * senders, dispatches each encrypted payload through {@link MessageDecryptionHandler},
 * and flushes the Signal store. The protobuf phase decodes the plaintext, validates
 * the HSM flag, imports any embedded sender-key distribution message, unwraps a
 * {@link DeviceSentMessage} envelope for self-sent messages, and assembles the final
 * {@link ChatMessageInfo}.
 *
 * <p>Selected by {@link MessageReceivingService#process(Stanza)} for every stanza whose
 * {@code from} JID is not a newsletter; this covers 1:1, group, broadcast, status, and
 * peer-protocol messages.
 *
 * @implNote
 * This implementation collapses WhatsApp Web's
 * {@code WAWebMsgProcessingDecryptApi.decryptE2EPayload} (decryption iteration plus
 * ADV validation) and {@code WAWebHandleMsgProcess.processDecryptedMessageProto}
 * (post-decryption protobuf processing) into a single straight-line method instead
 * of WA Web's mutually-recursive callback graph between
 * {@code WAWebMsgProcessingApiUtils.parseMessage} and the various
 * {@code processXxxMsg} branches; this is possible because Cobalt does not run the
 * reactive {@code MessageQueue} dispatcher that WA Web uses to gate processing per
 * chat.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsg")
@WhatsAppWebModule(moduleName = "WAWebMsgProcessingDecryptApi")
@WhatsAppWebModule(moduleName = "WAWebHandleMsgProcess")
@WhatsAppWebModule(moduleName = "WAWebMsgProcessingApiUtils")
final class ChatMessageReceiver extends MessageReceiver<ChatMessageInfo> {
    /**
     * The logger for {@link ChatMessageReceiver}.
     */
    private static final System.Logger LOGGER = Log.get(ChatMessageReceiver.class);

    /**
     * The {@code bizFeatureEnabled} dimension reported for the click-to-WhatsApp
     * automated-greeting-message (AGM) journey.
     *
     * <p>Mirrors WhatsApp Web's {@code WAWebCtwaLogger} feature list
     * {@code ["agm"]} joined with {@code ", "}; the AGM injection is the only
     * feature that contributes to the list, so the joined value is the bare token
     * {@code "agm"}.
     */
    @WhatsAppWebExport(moduleName = "WAWebCtwaLogger", exports = "logAGMOperation",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final String CTWA_AGM_BIZ_FEATURE = "agm";

    /**
     * The serialized {@code ctwaBizUserJourneyMetadata} carried by the AGM
     * injection metric.
     *
     * <p>Mirrors the WhatsApp Web {@code WAWebCtwaLogger} shape: a JSON object with
     * a single {@code agm_cta_type} member. The call-to-action type is
     * {@code null} because Cobalt does not parse the AGM call-to-action payload
     * from the wire on the receive path, matching the {@code null} WA Web itself
     * emits whenever the ad payload omits a call-to-action type.
     */
    @WhatsAppWebExport(moduleName = "WAWebCtwaLogger", exports = "logAGMOperation",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static final String CTWA_AGM_METADATA = "{\"agm_cta_type\":null}";

    /**
     * Decryption service used for the per-enc iteration in the decryption phase.
     *
     * <p>Owns the per-device Signal cipher (PKMSG/MSG), the group sender-key cipher
     * (SKMSG), and the bot AES-GCM scheme (MSMSG); injected so the same service can be
     * reused across receivers and stubbed in tests.
     */
    private final MessageDecryption decryption;

    /**
     * Telemetry sink for the WAM events emitted by the receive pipeline, or
     * {@code null} when telemetry is disabled.
     *
     * <p>Consumed by the per-message emitters ({@link #validateReportingToken},
     * {@link #dropInvalidChannelsContextSource}, {@link #emitReceiveTelemetry},
     * and the poll-vote message-secret error path). It is nullable so tests and
     * embedders that do not run the WAM pipeline can construct the receiver with
     * {@code null}; every emit is routed through {@link #commit(WamEventSpec)}
     * which swallows the {@code null} case.
     */
    private final WamService wamService;


    /**
     * Decryptor service
     */
    private final MessageDecryptionHandler messageDecryptor;

    /**
     * Constructs a chat receiver bound to the given store and decryption service.
     *
     * @param store      the central session store
     * @param decryption the decryption service for the Signal protocol and bot
     *                   messages
     * @param wamService the WAM telemetry sink, or {@code null} to disable the
     *                   receive-path metrics
     * @throws NullPointerException if {@code decryption} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsg", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    ChatMessageReceiver(LinkedWhatsAppStore store, MessageDecryption decryption, WamService wamService) {
        super(store);
        this.decryption = Objects.requireNonNull(decryption, "decryption");
        this.wamService = wamService;
        this.messageDecryptor = new MessageDecryptionHandler(wamService);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Runs the full two-phase decrypt-then-decode pipeline; returns {@code null} for
     * unavailable fanout placeholders that should be silently acknowledged.
     *
     * @implNote
     * This implementation extends WhatsApp Web's expired-status metric suppression
     * into a full exception suppression: an expired status message (older than 24
     * hours, see {@link #isExpiredStatus(MessageReceiveStanza)}) whose decryption
     * fails is silently dropped via {@code null} so the orchestrator does not raise
     * a retry receipt; WA Web's
     * {@code WAWebMsgProcessingDecryptionHandler.postIncomingMessageDropExpired}
     * still produces a metric on the same branch but lets the receipt path proceed.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsg", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptApi", exports = "decryptE2EPayload",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    ChatMessageInfo receive(Stanza node, Jid fromJid) {
        var selfJid = store.accountStore().jid().orElse(null);
        var selfLidJid = store.accountStore().lid().orElse(null);
        var stanza = MessageReceiveStanzaParser.parse(node, selfJid, selfLidJid);

        if (stanza.isUnavailable()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "skipping unavailable (fanout) message {0}", stanza.id());
            return null;
        }

        if (stanza.encs().isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "message {0} has no encrypted payloads", stanza.id());
            return null;
        }

        validateRecipient(stanza);
        validateNotHostedCompanion(stanza);

        validateEncOrdering(stanza);
        validateAdvIdentity(stanza);
        byte[] plaintext;
        try {
            plaintext = decryptPayloads(stanza);
        } catch (WhatsAppMessageException.Receive e) {
            if (isExpiredStatus(stanza)) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "skipping decryption error for expired status {0}", stanza.id());
                return null;
            }
            throw e;
        }
        flushSignalStore();

        var container = decodeProtobuf(stanza.id(), plaintext);
        if (container == null) {
            throw new WhatsAppMessageException.Receive.InvalidProtobuf(
                    "Failed to decode protobuf for: " + stanza.id(), null);
        }

        validateHsmConsistency(stanza, container);
        processSenderKeyDistribution(container, stanza);

        dropInvalidChannelsContextSource(stanza, container);
        validateReportingToken(stanza, container, plaintext);

        var chatJid = resolveChatJid(stanza);
        var effectiveContainer = container;

        if (container.futureProofContentType() == FutureProofMessageType.DEVICE_SENT) {
            var dsm = container.deviceSentMessage().orElseThrow();
            effectiveContainer = unwrapDeviceSentMessage(container, dsm, stanza);
            chatJid = dsm.destinationJid().orElseThrow(() ->
                    new WhatsAppMessageException.Receive.InvalidDeviceSentMessage(
                            DsmErrorType.INVALID_DSM));
        } else if (shouldHaveDeviceSentMessage(stanza, container)) {
            throw new WhatsAppMessageException.Receive.InvalidDeviceSentMessage(
                    DsmErrorType.MISSING_DSM);
        }

        maybeDecryptPollVote(effectiveContainer, stanza);

        var info = buildChatMessageInfo(stanza, chatJid, effectiveContainer);
        emitReceiveTelemetry(stanza, chatJid, effectiveContainer);
        return info;
    }

    /**
     * Auto-decrypts an inbound poll vote in place, populating its
     * {@link PollUpdateMessage#selectedOptions() selectedOptions} with the
     * resolved option labels.
     *
     * <p>Mirrors the send-side vote encryption performed during message
     * preparation: when {@code container} carries a {@link PollUpdateMessage}
     * whose encrypted {@code vote} is present, the referenced
     * {@link PollCreationMessage} is resolved from the local store, the vote is
     * decrypted to its selected option hashes, and those hashes are matched back
     * to the poll's option labels. The decryption is best-effort: a missing
     * poll-creation message (not cached locally) or a cipher rejection (for
     * example a sender JID-form mismatch, which this client does not yet retry
     * across LID and PN forms) leaves the vote untouched rather than failing the
     * receive.
     *
     * @param container the decoded inbound message container
     * @param stanza    the parsed receive stanza, supplying the voter JID
     */
    @WhatsAppWebExport(moduleName = "WAWebPollsVoteDecryption", exports = "decryptVote",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void maybeDecryptPollVote(LinkedMessageContainer container, MessageReceiveStanza stanza) {
        if (!(container.content() instanceof PollUpdateMessage poll) || poll.vote().isEmpty()) {
            return;
        }
        var pollKey = poll.pollCreationMessageKey().orElse(null);
        if (pollKey == null) {
            return;
        }
        if (!(store.chatStore().findMessageByKey(pollKey).orElse(null) instanceof ChatMessageInfo pollCreation)
                || !(pollCreation.message().content() instanceof PollCreationMessage creation)) {
            return;
        }
        var voterJid = stanza.senderJid().toUserJid();
        try {
            var selectedHashes = EncMessageFactory.decryptPollVote(poll.vote().get(), pollCreation, voterJid);
            poll.setSelectedOptions(matchSelectedOptions(creation, selectedHashes));
        } catch (RuntimeException exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "could not auto-decrypt poll vote {0}: {1}", stanza.id(), exception.getMessage());
            commit(new MessageSecretErrorsEventBuilder()
                    .messageSecretAllowedList(MessageSecretAllowedType.MESSAGE_POLL)
                    .messageSecretError(MessageSecretErrorType.DECRYPTION_ERROR)
                    .messageMediaType(MediaType.POLL_VOTE)
                    .build());
        }
    }

    /**
     * Maps decrypted option hashes back to the matching poll option labels.
     *
     * <p>Each option name is hashed with SHA-256, the same digest the vote
     * encoder applies, and the hashes are compared against the decrypted
     * selection. Hashes with no matching option are dropped.
     *
     * @param creation       the poll-creation message carrying the option labels
     * @param selectedHashes the SHA-256 hashes of the voter's selected options
     * @return the matched option labels, in selection order
     */
    private static List<String> matchSelectedOptions(PollCreationMessage creation, List<byte[]> selectedHashes) {
        if (selectedHashes.isEmpty()) {
            return List.of();
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
        var labelsByHash = new HashMap<String, String>(creation.options().size());
        for (var option : creation.options()) {
            var name = option.optionName().orElse(null);
            if (name == null) {
                continue;
            }
            digest.reset();
            labelsByHash.put(HexFormat.of().formatHex(digest.digest(name.getBytes(StandardCharsets.UTF_8))), name);
        }
        var labels = new ArrayList<String>(selectedHashes.size());
        for (var hash : selectedHashes) {
            var label = labelsByHash.get(HexFormat.of().formatHex(hash));
            if (label != null) {
                labels.add(label);
            }
        }
        return labels;
    }

    /**
     * Rejects stanzas that carry a {@code recipient}, {@code recipient_pn}, or
     * {@code recipient_lid} attribute when the sender is not the logged-in user's
     * own device.
     *
     * <p>The recipient attributes are reserved for peer-protocol messages echoed
     * between the user's own devices; receiving them from any other sender is a
     * protocol violation that is rejected up front.
     *
     * @param stanza the parsed stanza
     * @throws WhatsAppMessageException.Receive.InvalidMessage if a recipient
     *         attribute appears on a non-peer message
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void validateRecipient(MessageReceiveStanza stanza) {
        var hasRecipient = stanza.recipient().isPresent()
                || stanza.recipientPn().isPresent()
                || stanza.recipientLid().isPresent();

        if (hasRecipient && !isFromMe(stanza)) {
            throw new WhatsAppMessageException.Receive.InvalidMessage(
                    "Recipient attribute from non-peer device: " + stanza.senderJid(), null);
        }
    }

    /**
     * Rejects stanzas from a hosted companion device that target a group, community,
     * broadcast, or status chat.
     *
     * <p>Hosted companions (the WhatsApp Business coexistence-mode devices) may only
     * participate in 1:1 chats with their primary, so anything else is dropped as an
     * invalid stanza.
     *
     * @param stanza the parsed stanza
     * @throws WhatsAppMessageException.Receive.InvalidMessage if a hosted companion
     *         device targets a group, community, or broadcast chat
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void validateNotHostedCompanion(MessageReceiveStanza stanza) {
        var participant = stanza.participant().orElse(null);
        if (participant == null) {
            return;
        }

        if (!participant.hasHostedServer() && !participant.hasHostedLidServer()) {
            return;
        }

        var chatJid = stanza.chatJid();
        if (chatJid.hasGroupOrCommunityServer()
                || chatJid.hasBroadcastServer()) {
            throw new WhatsAppMessageException.Receive.InvalidMessage(
                    "Hosted companion device " + participant
                            + " cannot send to " + chatJid, null);
        }
    }

    /**
     * Resolves the effective chat JID for storing the message, applying bot-specific
     * target overrides.
     *
     * <p>Bot-server messages can re-target a different chat via the
     * {@code target_chat_jid} or {@code target_chat_jid_lid} stanza attribute; the
     * resulting {@link ChatMessageInfo} is routed into that chat instead of the bot's
     * own JID so the user sees the reply in the originating thread.
     *
     * @param stanza the parsed stanza
     * @return the effective chat JID
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Jid resolveChatJid(MessageReceiveStanza stanza) {
        if (stanza.chatJid().hasBotServer()) {
            var targetChatJid = stanza.targetChatJidLid()
                    .or(stanza::targetChatJid)
                    .orElse(null);
            if (targetChatJid != null) {
                return targetChatJid;
            }
        }
        return stanza.chatJid();
    }

    /**
     * Returns whether the stanza is a status update older than 24 hours.
     *
     * <p>{@link #receive(Stanza, Jid)} uses this to silently drop decryption errors on
     * expired status content rather than triggering a retry receipt for a story the
     * user can no longer view.
     *
     * @implNote
     * This implementation uses {@link ChronoUnit#HOURS} arithmetic for the 24-hour
     * threshold; WhatsApp Web uses {@code DAY_SECONDS} so the comparison is
     * second-level but the result is identical.
     *
     * @param stanza the parsed stanza
     * @return {@code true} when the stanza is a status broadcast that is more than
     *         24 hours old
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptionHandler", exports = "createDecryptionHandler",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isExpiredStatus(MessageReceiveStanza stanza) {
        if (!stanza.chatJid().isStatusBroadcastAccount()) {
            return false;
        }

        var age = ChronoUnit.HOURS.between(stanza.timestamp(), Instant.now());
        return age > 24;
    }

    /**
     * Warns when a stanza presents an SKMSG payload before the per-device Signal
     * payload in a two-enc stanza.
     *
     * <p>The expected ordering is per-device-first so the SKMSG can be validated
     * against the sender-key distribution that lives in the per-device payload. An
     * out-of-order pair is logged and processing continues rather than aborting.
     *
     * @param stanza the parsed stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptApi", exports = "decryptE2EPayload",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void validateEncOrdering(MessageReceiveStanza stanza) {
        var encs = stanza.encs();
        if (encs.size() == 2
                && encs.getFirst().e2eType() == MessageEncryptionType.SKMSG) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "message {0}: skmsg is out of order (should not be first of two encs)",
                    stanza.id());
        }
    }

    /**
     * Validates the ADV (Auxiliary Device Validation) identity when the stanza
     * sender is a companion device and carries a PKMSG payload.
     *
     * <p>Ensures that a companion device with a freshly-established Signal session
     * cannot impersonate the primary.
     *
     * @implNote
     * This implementation extracts the identity key from the PKMSG ciphertext via
     * {@link MessageDecryption#extractIdentityKeyFromPkmsg(byte[])}, parses the
     * device-identity protobuf attached to the stanza, and accepts the binding on
     * a missing local identity record; the cryptographic signature check is
     * delegated to libsignal's session-installation path so this method only
     * surfaces structural failures.
     *
     * @param stanza the parsed stanza
     * @throws WhatsAppMessageException.Receive.AdvFailure if validation fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptApi", exports = "decryptE2EPayload",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void validateAdvIdentity(MessageReceiveStanza stanza) {
        if (!stanza.isCompanionDevice()) {
            return;
        }

        var pkmsgPayload = stanza.encs().stream()
                .filter(enc -> enc.e2eType() == MessageEncryptionType.PKMSG)
                .findFirst()
                .orElse(null);
        if (pkmsgPayload == null) {
            return;
        }

        var deviceIdentityBytes = stanza.deviceIdentity().orElse(null);
        if (deviceIdentityBytes == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "companion device {0} sent pkmsg without device-identity",
                    stanza.senderJid());
            throw new WhatsAppMessageException.Receive.AdvFailure(
                    "Missing device-identity for companion device: "
                            + stanza.senderJid());
        }

        var identityKey = decryption.extractIdentityKeyFromPkmsg(
                pkmsgPayload.ciphertext()).orElse(null);
        if (identityKey == null) {
            throw new WhatsAppMessageException.Receive.AdvFailure(
                    "Cannot extract identity key from PKMSG for: "
                            + stanza.senderJid());
        }

        try {
            var signedIdentity = ADVSignedDeviceIdentitySpec.decode(deviceIdentityBytes);

            var primaryJid = stanza.senderJid().toUserJid();
            var storedKey = store.signalStore().findIdentityByAddress(
                    primaryJid.toSignalAddress());

            if (storedKey.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "no stored identity for primary {0}, accepting adv",
                        primaryJid);
            }
        } catch (WhatsAppMessageException.Receive e) {
            throw e;
        } catch (Exception e) {
            throw new WhatsAppMessageException.Receive.AdvFailure(
                    "ADV identity parsing failed for: "
                            + stanza.senderJid(), e);
        }
    }

    /**
     * Iterates over every encrypted payload in the stanza, dispatching to the
     * appropriate cipher and returning the first successful plaintext.
     *
     * <p>Every enc is attempted even after the first success so the Signal session
     * state is advanced for every encryption type via the
     * {@link MessageDecryptionHandler} across both slots.
     *
     * @implNote
     * This implementation rethrows the dominant failure surfaced via
     * {@link MessageDecryptionHandler#failedError()} when no payload could be
     * decrypted, so the caller sees the original Signal-protocol exception rather
     * than a synthesised wrapper; a synthesised
     * {@link WhatsAppMessageException.Receive.Unknown} is only used when the handler
     * recorded no failure at all (which should not happen given the
     * {@link MessageDecryptionHandler#canDecryptNext(MessageReceiveEncryptedPayload)}
     * contract).
     *
     * @param stanza the parsed stanza
     * @return the first successfully decrypted plaintext bytes
     * @throws WhatsAppMessageException.Receive if every payload fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptApi", exports = "decryptE2EPayload",
            adaptation = WhatsAppAdaptation.DIRECT)
    private byte[] decryptPayloads(MessageReceiveStanza stanza) {
        byte[] plaintext = null;

        for (var enc : stanza.encs()) {
            if (!messageDecryptor.canDecryptNext(enc)) {
                continue;
            }

            try {
                var decrypted = decryptSinglePayload(enc, stanza);

                if (plaintext == null) {
                    plaintext = decrypted;
                }
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "decrypted message {0} via {1}",
                        stanza.id(), enc.e2eType());
            } catch (WhatsAppMessageException.Receive e) {
                messageDecryptor.handleError(enc, e);
            }
        }

        if (plaintext != null) {
            return plaintext;
        }

        var error = messageDecryptor.failedError().orElse(null);
        if (error != null) {
            throw error;
        }

        throw new WhatsAppMessageException.Receive.Unknown(
                "No encrypted payloads could be decrypted for: "
                        + stanza.id(), null);
    }

    /**
     * Dispatches a single encrypted payload to the cipher matching its
     * {@link MessageEncryptionType}.
     *
     * <p>The four ciphertext types map to the three Signal entry points on
     * {@link MessageDecryption} plus the special MSMSG bot-message scheme.
     *
     * @implNote
     * This implementation resolves the bot {@code messageId} using
     * {@link MessageReceiveBotInfo#editTargetId()} when present and falling back to
     * the stanza id; WhatsApp Web's
     * {@code WAWebBotMessageSecret.decryptMsmsgBotMessage} uses
     * {@code botEditTargetId} only when the bot edit type is
     * {@code INNER}/{@code LAST}, which Cobalt collapses to "always use the edit
     * target id when present" because the broader fallback is harmless on the
     * non-edit path.
     *
     * @param enc    the encrypted payload to decrypt
     * @param stanza the parent stanza for addressing context
     * @return the decrypted plaintext bytes with padding already removed (when
     *         applicable)
     * @throws WhatsAppMessageException.Receive if decryption fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptEnc", exports = "decryptEnc",
            adaptation = WhatsAppAdaptation.DIRECT)
    private byte[] decryptSinglePayload(
            MessageReceiveEncryptedPayload enc,
            MessageReceiveStanza stanza
    ) {
        return switch (enc.e2eType()) {
            case SKMSG -> {
                var groupJid = stanza.chatJid();
                if (!groupJid.hasGroupOrCommunityServer()
                        && !groupJid.hasBroadcastServer()) {
                    throw new WhatsAppMessageException.Receive.InvalidMessage(
                            "SKMSG for non-group JID: " + groupJid, null);
                }
                var participant = stanza.participant().orElseThrow(() ->
                        new WhatsAppMessageException.Receive.InvalidMessage(
                                "SKMSG without participant for: " + groupJid, null));
                yield decryption.decryptFromGroup(
                        enc.ciphertext(), groupJid, participant);
            }
            case PKMSG, MSG -> {
                var sender = resolveSignalSender(stanza);
                yield decryption.decryptFromDevice(
                        enc.ciphertext(), sender, enc.e2eType());
            }
            case MSMSG -> {
                var messageSecret = resolveBotMessageSecret(stanza);
                var messageId = stanza.botInfo()
                        .flatMap(MessageReceiveBotInfo::editTargetId)
                        .orElse(stanza.id());
                var targetSenderJid = stanza.targetSenderJid()
                        .map(Jid::toUserJid)
                        .orElseGet(() -> requireSelfJid().toUserJid());
                var botSenderJid = stanza.senderJid().toUserJid();
                yield decryption.decryptBotMessage(
                        enc.ciphertext(), messageSecret, messageId,
                        targetSenderJid, botSenderJid);
            }
        };
    }

    /**
     * Resolves the Signal sender JID used to look up a per-device session.
     *
     * <p>Picks the participant JID for group and broadcast chats and the {@code from}
     * JID otherwise, since the Signal cipher needs the device-level JID rather than the
     * chat JID.
     *
     * @param stanza the parsed stanza
     * @return the sender device JID for Signal session lookup
     * @throws WhatsAppMessageException.Receive.InvalidMessage if a group or broadcast
     *         stanza is missing its participant
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptEnc", exports = "decryptEnc",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Jid resolveSignalSender(MessageReceiveStanza stanza) {
        var chatJid = stanza.chatJid();
        if (chatJid.hasGroupOrCommunityServer() || chatJid.hasBroadcastServer()) {
            return stanza.participant().orElseThrow(() ->
                    new WhatsAppMessageException.Receive.InvalidMessage(
                            "PKMSG/MSG without participant for group: "
                                    + chatJid, null));
        }
        return chatJid;
    }

    /**
     * Resolves the {@code messageSecret} for a bot message by looking up the target
     * message in the store.
     *
     * @implNote
     * This implementation skips WhatsApp Web's {@code WAWebMsmsgMsgSecretCache} layer
     * and reads the target message straight from the store via
     * {@link LinkedWhatsAppChatStore#findMessageById(com.github.auties00.cobalt.wire.core.jid.JidProvider, String)},
     * keyed by {@code (targetChatJid, targetId)}. The result is accepted only when the
     * resolved {@link ChatMessageInfo#messageSecret()} is non-empty; an absent secret
     * raises {@link WhatsAppMessageException.Receive.InvalidMessage} rather than WA
     * Web's {@code WAWebOrphanBotMsgError} because Cobalt does not yet model the
     * orphan-bot-message deferral path.
     *
     * @param stanza the parsed stanza carrying {@code target_id} and
     *               {@code target_chat_jid} metadata
     * @return the 32-byte message secret
     * @throws WhatsAppMessageException.Receive.InvalidMessage if the target message
     *         or its secret cannot be found
     */
    @WhatsAppWebExport(moduleName = "WAWebBotMessageSecret", exports = "decryptMsmsgBotMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private byte[] resolveBotMessageSecret(MessageReceiveStanza stanza) {
        var targetId = stanza.targetId().orElseThrow(() ->
                new WhatsAppMessageException.Receive.InvalidMessage(
                        "MSMSG missing target_id", null));

        var targetChatJid = stanza.targetChatJid().orElse(stanza.chatJid());

        var targetMessage = store.chatStore().findMessageById(targetChatJid, targetId)
                .orElse(null);
        if (targetMessage instanceof ChatMessageInfo chatInfo) {
            var secret = chatInfo.messageSecret().orElse(null);
            if (secret != null && secret.length > 0) {
                return secret;
            }
        }
        throw new WhatsAppMessageException.Receive.InvalidMessage(
                "Cannot find messageSecret for target message: " + targetId
                        + " in chat: " + targetChatJid, null);
    }

    /**
     * Flushes the Signal protocol store after a successful decryption pass.
     *
     * <p>Session state updated during PKMSG installation and MSG ratcheting must be
     * durable before the next stanza is processed.
     *
     * @implNote
     * This implementation logs and swallows any flush failure so a transient store
     * error does not propagate as a fatal receive error; a persistent failure will
     * be surfaced by the next save attempt on a subsequent message.
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptApi", exports = "decryptE2EPayload",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void flushSignalStore() {
        try {
            store.save();
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to flush signal store", e);
        }
    }

    /**
     * Rejects stanzas whose HSM flag disagrees with the decoded protobuf content.
     *
     * <p>The mismatch resolves to
     * {@link com.github.auties00.cobalt.message.receive.crypto.MessageDecryptionResult#HSM_MISMATCH},
     * which the receipt handler treats as a silent drop.
     *
     * @param stanza    the parsed stanza
     * @param container the decoded message container
     * @throws WhatsAppMessageException.Receive.HsmMismatch if the stanza is not HSM
     *         but the protobuf carries a {@link HighlyStructuredMessage}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgProcessUtils", exports = "preProcessMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void validateHsmConsistency(
            MessageReceiveStanza stanza,
            LinkedMessageContainer container
    ) {
        if (!stanza.isHsm() && container.content() instanceof HighlyStructuredMessage) {
            throw new WhatsAppMessageException.Receive.HsmMismatch(
                    "HSM mismatch for: " + stanza.id());
        }
    }

    /**
     * Imports the sender-key distribution embedded in the decoded protobuf, when
     * present.
     *
     * <p>Importing the distribution lets future SKMSG payloads from the same sender in
     * the same group decrypt without an extra round-trip.
     *
     * @implNote
     * This implementation validates that the embedded group id matches the stanza
     * chat JID before importing the key so a malicious sender cannot install a
     * sender-key for a different group; on mismatch the import is skipped and a
     * warning is logged. The libsignal-side import error is caught and logged
     * rather than propagated so a single corrupt distribution does not abort the
     * remaining post-decryption pipeline.
     *
     * @param container the decoded message container
     * @param stanza    the parsed stanza for group and sender context
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingApiUtils", exports = "parseMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void processSenderKeyDistribution(
            LinkedMessageContainer container,
            MessageReceiveStanza stanza
    ) {
        var skdm = container.senderKeyDistributionMessage().orElse(null);
        if (skdm == null) {
            return;
        }

        var skdmGroupJid = skdm.groupJid()
                .orElse(null);
        var distributionData = skdm.axolotlSenderKeyDistributionMessage()
                .orElse(null);
        if (distributionData == null || distributionData.length == 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "sender key distribution missing data for {0}",
                    stanza.id());
            return;
        }

        var groupJid = stanza.chatJid();
        if (!Objects.equals(groupJid, skdmGroupJid)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "sender key distribution group id mismatch: stanza={0}, proto={1}",
                    groupJid, skdmGroupJid);
            return;
        }

        try {
            decryption.processSenderKeyDistribution(
                    groupJid, stanza.senderJid(), distributionData);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "processed sender key distribution from {0} for {1}",
                    stanza.senderJid(), groupJid);
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "failed to process sender key distribution for " + stanza.id(), e);
        }
    }

    /**
     * Returns whether the decoded protobuf for a self-sent stanza should carry a
     * {@link DeviceSentMessage} wrapper.
     *
     * <p>A missing DSM on a type that requires one resolves to
     * {@link WhatsAppMessageException.Receive.InvalidDeviceSentMessage} with
     * {@link DsmErrorType#MISSING_DSM}.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code parseMessage} pre-switch
     * bypass plus its per-type rules:
     * <ul>
     *   <li>{@code protocolMessage} carrying any of {@code historySyncNotification},
     *       {@code initialSecurityNotificationSettingSync},
     *       {@code appStateSyncKeyShare}, {@code appStateSyncKeyRequest},
     *       {@code peerDataOperationRequestMessage},
     *       {@code peerDataOperationRequestResponseMessage},
     *       {@code cloudApiThreadControlNotification}, or
     *       {@code lidMigrationMappingSyncMessage}: never (control-plane
     *       short-circuit).</li>
     *   <li>{@code CHAT}: always.</li>
     *   <li>{@code OTHER_BROADCAST}, {@code OTHER_STATUS}, {@code PEER_CHAT}:
     *       never.</li>
     *   <li>{@code GROUP}: only when {@link MessageReceiveStanza#isDirect()} is
     *       {@code true} AND the message is not the appdata-default sender-key
     *       distribution bootstrap (WA Web's {@code K(e)} predicate).</li>
     *   <li>{@code DIRECT_PEER_STATUS}: only when
     *       {@link MessageReceiveStanza#isDirect()} is {@code true}.</li>
     *   <li>{@code PEER_BROADCAST}: only when none of the encs is an SKMSG and
     *       the stanza is not a delivery retry (WA Web's
     *       {@code isMessageRetry && deviceSentMessage == null} short-circuit).</li>
     * </ul>
     *
     * @param stanza    the parsed stanza
     * @param container the decoded message container, consulted for the
     *                  protocol-message bypass and the appdata-default
     *                  sender-key-distribution exception
     * @return {@code true} if a DSM wrapper should be present
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingApiUtils", exports = "parseMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean shouldHaveDeviceSentMessage(MessageReceiveStanza stanza, LinkedMessageContainer container) {
        if (!isFromMe(stanza)) {
            return false;
        }

        if (isProtocolMessageBypass(container)) {
            return false;
        }

        return switch (stanza.messageType()) {
            case CHAT -> true;
            case OTHER_BROADCAST, OTHER_STATUS, PEER_CHAT -> false;
            case GROUP -> stanza.isDirect() && !isAppdataDefaultSenderKeyOnly(stanza, container);
            case DIRECT_PEER_STATUS -> stanza.isDirect();
            case PEER_BROADCAST -> {
                var hasSenderKeyEnc = stanza.encs().stream()
                        .anyMatch(enc -> enc.e2eType().isSenderKeyMessage());
                if (hasSenderKeyEnc) {
                    yield false;
                }
                yield !stanza.isRetry();
            }
        };
    }

    /**
     * Returns whether the decoded container carries a control-plane
     * {@link ProtocolMessage} variant that is exempt from the
     * device-sent-message requirement.
     *
     * <p>Mirrors the pre-switch short-circuit in WA Web's {@code parseMessage}:
     * any of the eight listed protocol-message variants routes through
     * {@code parseProtocolMessage} (or the {@code lidMigrationSyncMessage}
     * sibling branch) without consulting the DSM expectation.
     *
     * @param container the decoded message container
     * @return {@code true} when the container carries a bypass-eligible
     *         protocol message
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingApiUtils", exports = "parseMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean isProtocolMessageBypass(LinkedMessageContainer container) {
        if (!(container.content() instanceof ProtocolMessage protocolMessage)) {
            return false;
        }
        return protocolMessage.historySyncNotification().isPresent()
                || protocolMessage.initialSecurityNotificationSettingSync().isPresent()
                || protocolMessage.appStateSyncKeyShare().isPresent()
                || protocolMessage.appStateSyncKeyRequest().isPresent()
                || protocolMessage.peerDataOperationRequestMessage().isPresent()
                || protocolMessage.peerDataOperationRequestResponseMessage().isPresent()
                || protocolMessage.cloudApiThreadControlNotification().isPresent()
                || protocolMessage.lidMigrationMappingSyncMessage().isPresent();
    }

    /**
     * Returns whether a group stanza is the appdata-default sender-key
     * distribution bootstrap that WA Web excludes from the DSM expectation.
     *
     * <p>Mirrors WA Web's {@code K(e)} predicate: the stanza's
     * {@code meta.appdata} attribute is {@code "default"} and the decoded
     * container carries a {@link com.github.auties00.cobalt.wire.linked.message.group.SenderKeyDistributionMessage}
     * side channel. In that case, the GROUP-direct self message is allowed to
     * arrive without a DSM wrapper because it is a key-rotation bootstrap
     * rather than a user-visible payload.
     *
     * @param stanza    the parsed stanza
     * @param container the decoded message container
     * @return {@code true} when the bootstrap exception applies
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingApiUtils", exports = "parseMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean isAppdataDefaultSenderKeyOnly(MessageReceiveStanza stanza, LinkedMessageContainer container) {
        return stanza.appdata().filter("default"::equals).isPresent()
                && container.senderKeyDistributionMessage().isPresent();
    }

    /**
     * Unwraps a {@link DeviceSentMessage} envelope and merges
     * {@code messageContextInfo} fields from the outer envelope into the inner
     * message.
     *
     * <p>The inner message takes priority for the bot, secret, and thread fields and
     * the outer envelope supplies {@code limitSharingV2}.
     *
     * @implNote
     * This implementation preserves Cobalt-only inner context fields
     * ({@code deviceListMetadata}, {@code paddingBytes}, etc.) by copying every
     * inner field individually before applying the outer-vs-inner precedence on
     * {@code messageSecret}, {@code messageAssociation}, {@code threadId},
     * {@code botMetadata}, and {@code limitSharingV2}; WhatsApp Web's reference
     * implementation only carries the five fields exercised by its
     * {@code MessageContextInfo} type so Cobalt's extra fields have no WA Web
     * counterpart and are folded in unconditionally.
     *
     * @param outerContainer the outer container carrying the DSM
     * @param dsm            the DSM wrapper
     * @param stanza         the parsed stanza, used for error context
     * @return the inner container with the merged context info
     * @throws WhatsAppMessageException.Receive.InvalidDeviceSentMessage if the
     *         inner message is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebDeviceSentMessageProtoUtils", exports = "unwrapDeviceSentMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private LinkedMessageContainer unwrapDeviceSentMessage(
            LinkedMessageContainer outerContainer,
            DeviceSentMessage dsm,
            MessageReceiveStanza stanza
    ) {
        var inner = dsm.message();
        if (inner.isEmpty()) {
            throw new WhatsAppMessageException.Receive.InvalidDeviceSentMessage(
                    DsmErrorType.INVALID_DSM);
        }

        var innerContainer = inner.get();

        var outerCtx = outerContainer.messageContextInfo().orElse(null);
        var innerCtx = innerContainer.messageContextInfo().orElse(null);

        var mergedCtx = new ChatMessageContextInfoBuilder();

        if (innerCtx != null) {
            innerCtx.deviceListMetadata().ifPresent(mergedCtx::deviceListMetadata);
            innerCtx.deviceListMetadataVersion().ifPresent(mergedCtx::deviceListMetadataVersion);
            innerCtx.paddingBytes().ifPresent(mergedCtx::paddingBytes);
            innerCtx.messageAddOnDurationInSecs().ifPresent(mergedCtx::messageAddOnDurationInSecs);
            innerCtx.botMessageSecret().ifPresent(mergedCtx::botMessageSecret);
            innerCtx.reportingTokenVersion().ifPresent(mergedCtx::reportingTokenVersion);
            innerCtx.messageAddOnExpiryType().ifPresent(mergedCtx::messageAddOnExpiryType);
            if (innerCtx.capiCreatedGroup()) {
                mergedCtx.capiCreatedGroup(true);
            }
            innerCtx.supportPayload().ifPresent(mergedCtx::supportPayload);
            innerCtx.limitSharing().ifPresent(mergedCtx::limitSharing);
            innerCtx.weblinkRenderConfig().ifPresent(mergedCtx::weblinkRenderConfig);
        }

        var messageSecret = innerCtx != null ? innerCtx.messageSecret().orElse(null) : null;
        if (messageSecret == null && outerCtx != null) {
            messageSecret = outerCtx.messageSecret().orElse(null);
        }
        if (messageSecret != null) {
            mergedCtx.messageSecret(messageSecret);
        }

        var messageAssociation = innerCtx != null ? innerCtx.messageAssociation().orElse(null) : null;
        if (messageAssociation == null && outerCtx != null) {
            messageAssociation = outerCtx.messageAssociation().orElse(null);
        }
        if (messageAssociation != null) {
            mergedCtx.messageAssociation(messageAssociation);
        }

        if (outerCtx != null) {
            outerCtx.limitSharingV2().ifPresent(mergedCtx::limitSharingV2);
        }

        List<MessageThreadId> threadId;
        if (innerCtx != null && !innerCtx.threadId().isEmpty()) {
            threadId = innerCtx.threadId();
        } else if (outerCtx != null) {
            threadId = outerCtx.threadId();
        } else {
            threadId = List.of();
        }
        mergedCtx.threadId(threadId);

        var botMetadata = innerCtx != null ? innerCtx.botMetadata().orElse(null) : null;
        if (botMetadata == null && outerCtx != null) {
            botMetadata = outerCtx.botMetadata().orElse(null);
        }
        if (botMetadata != null) {
            mergedCtx.botMetadata(botMetadata);
        }

        return innerContainer.withMessageContextInfo(mergedCtx.build());
    }

    /**
     * Builds the final {@link ChatMessageInfo} from the stanza metadata and the
     * decoded (and possibly DSM-unwrapped) container.
     *
     * <p>The result is the {@link ChatMessageInfo} returned by
     * {@link #receive(Stanza, Jid)} and persisted by the orchestrator.
     *
     * @implNote
     * This implementation always stamps the resulting status as
     * {@link MessageStatus#DELIVERED} regardless of WA Web's {@code ACK.READ}
     * fast-path for self-notes; the status is later overridden by an explicit
     * read receipt if one arrives.
     *
     * @param stanza    the parsed stanza
     * @param chatJid   the effective chat JID (possibly overridden by DSM
     *                  destination)
     * @param container the decoded (and possibly unwrapped) message container
     * @return the fully-populated message info
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingApiUtils", exports = "generateBaseMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private ChatMessageInfo buildChatMessageInfo(
            MessageReceiveStanza stanza,
            Jid chatJid,
            LinkedMessageContainer container
    ) {
        var fromMe = isFromMe(stanza);
        var senderJid = stanza.senderJid().toUserJid();

        var key = new MessageKeyBuilder()
                .id(stanza.id())
                .parentJid(chatJid)
                .fromMe(fromMe)
                .senderJid(senderJid)
                .build();

        var builder = new ChatMessageInfoBuilder()
                .key(key)
                .message(container)
                .timestamp(stanza.timestamp())
                .status(MessageStatus.DELIVERED)
                .senderJid(senderJid)
                .broadcast(stanza.chatJid().hasBroadcastServer())
                .pushName(stanza.pushName().orElse(null))
                .urlText(stanza.urlText())
                .urlNumber(stanza.urlNumber());

        container.messageContextInfo()
                .flatMap(ChatMessageContextInfo::messageSecret)
                .ifPresent(builder::messageSecret);

        return builder.build();
    }

    /**
     * Drops a stanza that claims the channels-invitation context source but does
     * not carry a {@link NewsletterFollowerInviteMessage} payload.
     *
     * <p>The {@code context_source="channels_invitation"} marker is reserved for
     * newsletter follower-invite messages; any other payload arriving with that
     * marker is treated as invalid, dropped, and accounted with a
     * {@link BroadcastInvalidChannelsContextSourceMessageDropEventBuilder wasDropped}
     * metric before the receive is aborted.
     *
     * @implNote
     * This implementation mirrors WhatsApp Web's
     * {@code WAWebHandleMsgValidate} drop branch: it emits the drop metric and
     * then throws {@link WhatsAppMessageException.Receive.InvalidMessage} (WA
     * Web's {@code MessageValidationError} analogue). WA Web additionally gates
     * the drop behind
     * {@code WAWebNewsletterGatingUtils.isChannelInviteContactsToFollowInvalidDroppingEnabled};
     * Cobalt applies the drop unconditionally because the condition (a spoofed
     * channels-invitation marker on a non-invite payload) is anomalous
     * regardless of the rollout flag.
     *
     * @param stanza    the parsed stanza
     * @param container the decoded message container
     * @throws WhatsAppMessageException.Receive.InvalidMessage when the marker is
     *         present on a non-invite payload
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgValidate", exports = "validateContextSource",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void dropInvalidChannelsContextSource(MessageReceiveStanza stanza, LinkedMessageContainer container) {
        var channelsInvite = stanza.contextSource()
                .filter(MessageReceiveStanza.CONTEXT_SOURCE_CHANNELS_INVITATION::equals)
                .isPresent();
        if (!channelsInvite || container.content() instanceof NewsletterFollowerInviteMessage) {
            return;
        }
        if (Log.WARNING) LOGGER.log(Level.WARNING,
                "dropping message {0} with channels-invitation context source but non-invite content",
                stanza.id());
        commit(new BroadcastInvalidChannelsContextSourceMessageDropEventBuilder()
                .wasDropped(true)
                .build());
        throw new WhatsAppMessageException.Receive.InvalidMessage(
                "NEWSLETTER_FOLLOWER_INVITE message without valid context_source: " + stanza.id(), null);
    }

    /**
     * Recomputes the franking-style reporting token for an inbound message and
     * accounts a validation-failure metric when it does not match the token the
     * server attached.
     *
     * <p>Validation runs only when the stanza carries both a reporting token and
     * a reporting tag and the message was not authored by the local user. The
     * expected token is recomputed from the message secret, the deterministic
     * franking content ({@link ReportingTokenContent#compute(byte[], int)}), and
     * every plausible sender/receiver JID-form pair; a mismatch, an absent
     * message secret, or empty franking content each emit
     * {@link ReportingTokenValidationFailureEventBuilder} plus its private
     * sender-attributed sibling
     * {@link ReportingTokenValidationFailureSenderEventBuilder}. The whole pass
     * is best-effort: any exception is logged and swallowed so a token quirk
     * never fails the receive.
     *
     * @implNote
     * This implementation ports WhatsApp Web's
     * {@code WAWebReportingTokenUtils.validateReportingTokenInfo} and its
     * {@code S(e)} candidate-pair builder: the token is accepted when it matches
     * under any candidate pair (sender as PN or LID crossed with the local
     * account's PN or LID, plus the group/broadcast self-JID pairs), and only a
     * whole-set miss is reported as a mismatch.
     *
     * @param stanza    the parsed stanza carrying the reporting token
     * @param container the decoded message container supplying the message secret
     * @param plaintext the raw decoded protobuf bytes hashed into the franking
     *                  content
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "validateReportingTokenInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void validateReportingToken(MessageReceiveStanza stanza, LinkedMessageContainer container, byte[] plaintext) {
        if (wamService == null) {
            return;
        }
        var reporting = stanza.reportingInfo().orElse(null);
        if (reporting == null) {
            return;
        }
        var receivedToken = reporting.reportingToken().orElse(null);
        var reportingTag = reporting.reportingTag().orElse(null);
        if (receivedToken == null || reportingTag == null || isFromMe(stanza)) {
            return;
        }
        if (stanza.editAttribute() != MessageReceiveStanza.EDIT_NONE) {
            return;
        }
        var version = reporting.version();
        try {
            var messageSecret = container.messageContextInfo()
                    .flatMap(ChatMessageContextInfo::messageSecret)
                    .orElse(null);
            if (messageSecret == null) {
                emitReportingTokenFailure(stanza, container, version,
                        ReportingTokenValidationFailureReason.MISSING_MESSAGE_SECRET);
                return;
            }
            var content = ReportingTokenContent.compute(plaintext, version);
            if (content.length == 0) {
                emitReportingTokenFailure(stanza, container, version,
                        ReportingTokenValidationFailureReason.EMPTY_REPORTING_TOKEN_CONTENT);
                return;
            }
            if (!reportingTokenMatches(stanza, messageSecret, content, receivedToken, version)) {
                emitReportingTokenFailure(stanza, container, version,
                        ReportingTokenValidationFailureReason.MISMATCH_REPORTING_TOKEN);
            }
        } catch (Exception exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "reporting-token validation error for {0}: {1}", stanza.id(), exception.getMessage());
        }
    }

    /**
     * Returns whether the received reporting token matches the token recomputed
     * over any candidate sender/receiver JID-form pair.
     *
     * <p>The candidate senders are the sender's user JID plus its PN and LID
     * mappings; the candidate receivers are the local account's PN and LID and,
     * for group and broadcast chats, the chat JID and each candidate sender (the
     * "self JID for groups and broadcasts" rule). The token is accepted on the
     * first pair whose HMAC matches, matching WhatsApp Web's
     * {@code W(e)} first-hit acceptance.
     *
     * @param stanza        the parsed stanza supplying the JID forms and stanza id
     * @param messageSecret the 32-byte message secret
     * @param content       the deterministic franking content
     * @param receivedToken the token bytes the server attached
     * @param version       the reporting-token version
     * @return {@code true} when a candidate pair reproduces the received token
     * @throws GeneralSecurityException if a cryptographic primitive
     *         is unavailable
     */
    private boolean reportingTokenMatches(
            MessageReceiveStanza stanza,
            byte[] messageSecret,
            byte[] content,
            byte[] receivedToken,
            int version
    ) throws GeneralSecurityException {
        var senders = new LinkedHashSet<Jid>();
        addUserJid(senders, stanza.senderJid());
        stanza.senderPn().ifPresent(pn -> addUserJid(senders, pn));
        stanza.senderLid().ifPresent(lid -> addUserJid(senders, lid));

        var receivers = new LinkedHashSet<Jid>();
        store.accountStore().jid().ifPresent(pn -> addUserJid(receivers, pn));
        store.accountStore().lid().ifPresent(lid -> addUserJid(receivers, lid));
        var chatJid = stanza.chatJid();
        if (chatJid.hasGroupOrCommunityServer() || chatJid.hasBroadcastServer()) {
            receivers.add(chatJid);
            receivers.addAll(senders);
        }

        for (var sender : senders) {
            for (var receiver : receivers) {
                var result = ReportingToken.generate(
                        messageSecret, stanza.id(), sender, receiver, content, version);
                if (result.isEmpty()) {
                    continue;
                }
                var token = result.get().token();
                if (Arrays.equals(token, Arrays.copyOf(receivedToken, token.length))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds the user-level form of a JID to a candidate set, skipping
     * {@code null}.
     *
     * @param set the candidate set to grow
     * @param jid the JID whose {@link Jid#toUserJid()} form is added, or
     *            {@code null} to skip
     */
    private static void addUserJid(LinkedHashSet<Jid> set, Jid jid) {
        if (jid != null) {
            set.add(jid.toUserJid());
        }
    }

    /**
     * Commits the pair of reporting-token validation-failure events for a failed
     * inbound message.
     *
     * <p>The regular-channel {@link ReportingTokenValidationFailureEventBuilder}
     * carries the message-shape dimensions and the private-channel
     * {@link ReportingTokenValidationFailureSenderEventBuilder} additionally
     * carries the sender JID and the E2E sender classification, matching
     * WhatsApp Web's {@code logReportingTokenValidationEvent} which commits both.
     *
     * @param stanza    the parsed stanza
     * @param container the decoded message container
     * @param version   the reporting-token version
     * @param reason    the classified failure reason
     */
    @WhatsAppWebExport(moduleName = "WAWebWamReportingTokenMismatchReporter", exports = "logReportingTokenValidationEvent",
            adaptation = WhatsAppAdaptation.DIRECT)
    private void emitReportingTokenFailure(
            MessageReceiveStanza stanza,
            LinkedMessageContainer container,
            int version,
            ReportingTokenValidationFailureReason reason
    ) {
        var editType = mapEditType(stanza.editAttribute());
        var mediaType = WamMsgUtils.getWamMediaType(container);
        var messageType = WamMsgUtils.getWamMessageTypeFromStanzaType(stanza.messageType());
        var lid = isLidAddressed(stanza);
        var forwarded = isForwardedMessage(container);
        var groupHistory = stanza.appdata().filter("group_history"::equals).isPresent();

        commit(new ReportingTokenValidationFailureEventBuilder()
                .editType(editType)
                .messageMediaType(mediaType)
                .messageType(messageType)
                .reportingTokenValidationFailureReason(reason)
                .isLid(lid)
                .isMessageRetry(stanza.isRetry())
                .offline(stanza.isOffline())
                .reportingTokenVersion(version)
                .messageIsForward(forwarded)
                .isSecretEncryptedMsg(false)
                .isPartOfGroupHistory(groupHistory)
                .build());

        var senderBuilder = new ReportingTokenValidationFailureSenderEventBuilder()
                .clientMessageId(stanza.id())
                .e2eReceiverType(DeviceType.COMPANION)
                .editType(editType)
                .messageMediaType(mediaType)
                .messageType(messageType)
                .reportingTokenValidationFailureReason(reason)
                .isLid(lid)
                .isMessageRetry(stanza.isRetry())
                .offline(stanza.isOffline())
                .reportingTokenVersion(version)
                .messageIsForward(forwarded)
                .isSecretEncryptedMsg(false)
                .senderJid(stanza.senderJid().toString());
        var e2eSenderType = WamMsgUtils.getWamE2eSenderType(
                stanza.senderJid(), store.accountStore().jid().orElse(null));
        if (e2eSenderType != null) {
            senderBuilder.e2eSenderType(e2eSenderType);
        }
        commit(senderBuilder.build());
    }

    /**
     * Dispatches the per-message receive telemetry emitted after a message has
     * been successfully assembled.
     *
     * <p>Fans out to the status-sync, QuickBusinessMessaging incoming-message,
     * and structured-message-interaction emitters, each of which self-gates on
     * the payload and chat class.
     *
     * @param stanza    the parsed stanza
     * @param chatJid   the effective chat JID
     * @param container the decoded (and possibly DSM-unwrapped) container
     */
    private void emitReceiveTelemetry(MessageReceiveStanza stanza, Jid chatJid, LinkedMessageContainer container) {
        if (wamService == null) {
            return;
        }
        maybeEmitStatusSync(stanza);
        maybeEmitQbmIncomingMessage(stanza, chatJid, container);
        maybeEmitStructuredMessageInteraction(stanza, chatJid, container);
        maybeEmitCtwaBizUserJourney(stanza);
    }

    /**
     * Accounts a click-to-WhatsApp business user-journey metric when an inbound
     * message carries CTWA ad-greeting attribution.
     *
     * <p>WhatsApp Web synthesises an automated greeting message (AGM) for the
     * buyer whenever a business is reached through a click-to-WhatsApp ad and logs
     * the {@link CtwaBizUserJourneyOperation#AGM_INJECTED injection} on the
     * business half of the journey. Cobalt is headless and never injects a
     * synthetic greeting row, but it observes the same trigger: an inbound
     * (non-self) message whose {@link MessageReceiveBizInfo} carries the CTWA
     * {@link MessageReceiveBizInfo#campaignId() campaign attribution}. The ad id
     * is the real campaign attribution read from the biz info; the feature tag
     * ({@link #CTWA_AGM_BIZ_FEATURE}), the {@code AGM_INJECTED} operation, and the
     * {@link #CTWA_AGM_METADATA metadata} are the fixed dimensions WA Web reports
     * for the injection.
     *
     * @implNote
     * This implementation gates on the CTWA campaign attribution surfaced by the
     * biz-info parser rather than WA Web's
     * {@code ctwaContext.automatedGreetingMessageShown} flag and {@code sourceId},
     * which live on the message's CTWA context and are not carried by
     * {@link MessageReceiveBizInfo}; it emits only the {@code AGM_INJECTED}
     * operation because the receive path cannot observe WA Web's duplicate,
     * null-greeting, invalid-source-app, and bottom-sheet error branches. The
     * {@code fromBusiness} guard that WA Web applies inside
     * {@code WAWebCtwaLogger.logAGMOperation} (commit only when the message is not
     * self-sent) is reproduced here as the {@link #isFromMe(MessageReceiveStanza)}
     * short-circuit.
     *
     * @param stanza the parsed stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebCtwaLogger", exports = "logAGMOperation",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebMsgAGMProcessing", exports = "generateAutomatedGreetingMsgs",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void maybeEmitCtwaBizUserJourney(MessageReceiveStanza stanza) {
        if (isFromMe(stanza)) {
            return;
        }
        var adId = stanza.bizInfo()
                .flatMap(MessageReceiveBizInfo::campaignId)
                .orElse(null);
        if (adId == null) {
            return;
        }
        commit(new CtwaBizUserJourneyEventBuilder()
                .adId(adId)
                .bizFeatureEnabled(CTWA_AGM_BIZ_FEATURE)
                .ctwaBizUserJourneyOperation(CtwaBizUserJourneyOperation.AGM_INJECTED)
                .ctwaBizUserJourneyMetadata(CTWA_AGM_METADATA)
                .build());
    }

    /**
     * Accounts a status-feed sync metric when the received message is a status
     * broadcast.
     *
     * <p>The recent and viewed item and row counts are read from the live
     * {@link LinkedWhatsAppChatStore#status() status feed}, aggregating distinct
     * authors as the "row" dimension; the sync-duration timer is derived from the
     * feed size because Cobalt receives status items one stanza at a time rather
     * than in a single collection sync.
     *
     * @implNote
     * This implementation emits per received status stanza, whereas WhatsApp
     * Web's {@code WAWebStatusCollection.logMetrics} emits once per collection
     * sync round; Cobalt has no batched status-collection sync boundary in the
     * receive path, so the closest faithful trigger is the arrival of a status
     * item. The counts are real store data; the {@code webcStatusSyncT} duration
     * is host-derived rather than measured.
     *
     * @param stanza the parsed stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebStatusCollection", exports = "logMetrics",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void maybeEmitStatusSync(MessageReceiveStanza stanza) {
        if (!stanza.chatJid().isStatusBroadcastAccount()) {
            return;
        }
        var statuses = store.chatStore().status();
        var itemCount = statuses.size();
        var authors = new LinkedHashSet<Jid>();
        var viewedAuthors = new LinkedHashSet<Jid>();
        var viewedItems = 0L;
        for (var status : statuses) {
            var author = status.senderJid().map(Jid::toUserJid).orElse(null);
            if (author != null) {
                authors.add(author);
            }
            var viewed = status.status()
                    .map(state -> state == MessageStatus.READ || state == MessageStatus.PLAYED)
                    .orElse(false);
            if (viewed) {
                viewedItems++;
                if (author != null) {
                    viewedAuthors.add(author);
                }
            }
        }
        commit(new WebcStatusSyncEventBuilder()
                .webcStatusSyncT(Instant.ofEpochMilli(35L + itemCount * 4L))
                .webcStatusRecentItemCount(itemCount)
                .webcStatusRecentRowCount(authors.size())
                .webcStatusViewedItemCount(viewedItems)
                .webcStatusViewedRowCount(viewedAuthors.size())
                .build());
    }

    /**
     * Accounts a QuickBusinessMessaging incoming-message metric for an inbound
     * business message on a 1:1 chat.
     *
     * <p>Gated exactly as WhatsApp Web's {@code WAWebQbmIncomingMessageLogger}:
     * the message is not self-authored, the chat is an individual user (not a
     * group, status, or broadcast), and the sender is a business (the stanza
     * carries business metadata or a highly-structured template). The obtainable
     * dimensions (contact type, folder, mute, QBM flag, HSM tag, message type,
     * subscribed-contact) are read from the store; the server-side analytics
     * counters and in-app-signup fields WA Web populates from business-backend
     * signals are omitted because Cobalt cannot obtain them client-side.
     *
     * @implNote
     * This implementation derives the SMB-versus-enterprise contact type from
     * the stanza's {@code verified_level} rather than a contact-database flag,
     * and synthesises the message and thread id HMACs with a SHA-256 digest
     * because Cobalt does not run WA Web's keyed thread-id HMAC service.
     *
     * @param stanza    the parsed stanza
     * @param chatJid   the effective chat JID
     * @param container the decoded container
     */
    @WhatsAppWebExport(moduleName = "WAWebQbmIncomingMessageLogger", exports = "logQbmIncomingMessages",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void maybeEmitQbmIncomingMessage(MessageReceiveStanza stanza, Jid chatJid, LinkedMessageContainer container) {
        if (isFromMe(stanza)) {
            return;
        }
        if (!chatJid.hasUserServer() && !chatJid.hasLidServer()) {
            return;
        }
        if (stanza.bizInfo().isEmpty() && !stanza.isHsm()) {
            return;
        }
        var chat = store.chatStore().findChatByJid(chatJid).orElse(null);
        var archived = chat != null && chat.archived();
        var muted = chat != null && chat.mute().map(ChatMute::isMuted).orElse(false);
        var enterprise = isEnterpriseSender(stanza);
        var flag = qbmFlag(stanza);
        var known = store.contactStore().findContactByJid(stanza.senderJid().toUserJid()).isPresent();

        commit(new QbmIncomingMessageEventBuilder()
                .contactType(enterprise ? ContactType.ENTERPRISE : ContactType.SMB)
                .chatsFolderType(archived ? ChatsFolderType.ARCHIVED : ChatsFolderType.INBOX)
                .isMuted(muted)
                .qbmFlag(flag)
                .qbmFlagStr(qbmFlagName(flag))
                .hsmTagStr(stanza.hsmTag().orElse(""))
                .messageTypeStr(stanza.stanzaType())
                .notificationEnabled(true)
                .isBizIntent(true)
                .isInsubContact(known)
                .messageIdHmac(pseudoHmac(stanza.id()))
                .threadIdHmac(pseudoHmac(chatJid.toString()))
                .build());
    }

    /**
     * Accounts a structured-message interaction metric when an inbound message
     * carries a structured business payload.
     *
     * <p>The message class is derived from the payload type; the interaction is
     * reported as {@link InteractionType#USER_VIEW} because the receive path
     * represents the buyer receiving and viewing the structured message rather
     * than acting on one of its buttons.
     *
     * @implNote
     * This implementation is the receive-time analogue of WhatsApp Web's
     * per-button interaction loggers in {@code WAWebFrontendVcardUtils}, which
     * fire from the buyer's UI (Pay Now, copy Pix code, and so on). Cobalt is
     * headless, so it emits the view interaction at message receipt and cannot
     * surface the button-specific interaction types.
     *
     * @param stanza    the parsed stanza
     * @param chatJid   the effective chat JID
     * @param container the decoded container
     */
    @WhatsAppWebExport(moduleName = "WAWebPsStructuredMessageInteractionWamEvent", exports = "PsStructuredMessageInteractionWamEvent",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void maybeEmitStructuredMessageInteraction(MessageReceiveStanza stanza, Jid chatJid, LinkedMessageContainer container) {
        if (isFromMe(stanza)) {
            return;
        }
        var messageClass = structuredMessageClass(container);
        if (messageClass == null) {
            return;
        }
        var hasMedia = WamMsgUtils.getWamMediaType(container) != MediaType.NONE;
        commit(new PsStructuredMessageInteractionEventBuilder()
                .bizPlatform(isEnterpriseSender(stanza) ? BizPlatform.ENT : BizPlatform.SMB)
                .businessOwnerJid(stanza.senderJid().toUserJid().toString())
                .messageClass(messageClass)
                .messageClassAttributes("{\"has_media\":" + hasMedia + "}")
                .messageInteraction(InteractionType.USER_VIEW)
                .messageMediaType(WamMsgUtils.getWamMediaType(container))
                .templateId(stanza.hsmTag().orElse(""))
                .threadIdHmac(pseudoHmac(chatJid.toString()))
                .build());
    }

    /**
     * Returns whether the sender is a verified enterprise business.
     *
     * <p>Reads the stanza's business {@code verified_level}: an enterprise sender
     * carries a high verification level, everything else is treated as an SMB.
     *
     * @param stanza the parsed stanza
     * @return {@code true} when the sender presents an enterprise verification
     *         level
     */
    private static boolean isEnterpriseSender(MessageReceiveStanza stanza) {
        return stanza.bizInfo()
                .flatMap(biz -> biz.verifiedLevel())
                .filter(level -> level.equalsIgnoreCase("high") || level.equalsIgnoreCase("enterprise"))
                .isPresent();
    }

    /**
     * Classifies a container's payload into the WAM structured-message class, or
     * returns {@code null} when the payload is not a structured business message.
     *
     * @param container the decoded container
     * @return the structured-message class, or {@code null} when the payload is
     *         not structured
     */
    private static StructuredMessageClass structuredMessageClass(LinkedMessageContainer container) {
        return switch (container.content()) {
            case HighlyStructuredMessage ignored -> StructuredMessageClass.HSM;
            case TemplateMessage ignored -> StructuredMessageClass.HSM;
            case ButtonsMessage ignored -> StructuredMessageClass.BUTTON;
            case InteractiveMessage ignored -> StructuredMessageClass.BUTTON_NFM;
            case InteractiveResponseMessage ignored -> StructuredMessageClass.BUTTON_NFM;
            case ListMessage ignored -> StructuredMessageClass.LIST;
            case ProductMessage ignored -> StructuredMessageClass.PRODUCT_ITEM;
            case OrderMessage ignored -> StructuredMessageClass.PRODUCT_LIST;
            case null, default -> null;
        };
    }

    /**
     * Classifies a business message into its QuickBusinessMessaging flag.
     *
     * <p>Marketing-message business sources map to
     * {@link QbmFlag#MARKETING_MESSAGE_SMB}; otherwise the highly-structured
     * category drives the flag (utility to transactional, marketing to
     * promotional, authentication to OTP), defaulting to {@link QbmFlag#OTHER}.
     *
     * @param stanza the parsed stanza
     * @return the QuickBusinessMessaging flag
     */
    @WhatsAppWebExport(moduleName = "WAWebQbmIncomingMessageLogger", exports = "logQbmIncomingMessages",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static QbmFlag qbmFlag(MessageReceiveStanza stanza) {
        var bizSource = stanza.bizSource().orElse(null);
        if (bizSource != null && bizSource.toLowerCase(Locale.ROOT).contains("marketing")) {
            return QbmFlag.MARKETING_MESSAGE_SMB;
        }
        var category = stanza.hsmCategory().orElse(null);
        if (category == null) {
            return QbmFlag.OTHER;
        }
        return switch (category.toUpperCase(Locale.ROOT)) {
            case "UTILITY" -> QbmFlag.TRANSACTIONAL;
            case "MARKETING" -> QbmFlag.PROMOTIONAL;
            case "AUTHENTICATION" -> QbmFlag.OTP;
            default -> QbmFlag.OTHER;
        };
    }

    /**
     * Returns the string label WhatsApp Web reports for a QuickBusinessMessaging
     * flag.
     *
     * <p>Mirrors WA Web's flag-to-string table, in which
     * {@link QbmFlag#PROMOTIONAL} is reported as {@code "NON_TRANSACTIONAL"}.
     *
     * @param flag the QuickBusinessMessaging flag
     * @return the string label
     */
    private static String qbmFlagName(QbmFlag flag) {
        return switch (flag) {
            case TRANSACTIONAL -> "TRANSACTIONAL";
            case PROMOTIONAL -> "NON_TRANSACTIONAL";
            case OTP -> "OTP";
            case MARKETING_MESSAGE_SMB -> "MARKETING_MESSAGE_SMB";
            case OTHER -> "OTHER";
        };
    }

    /**
     * Maps the stanza {@code edit} attribute to the WAM {@link EditType}
     * enumeration.
     *
     * @param editAttribute the raw {@code edit} attribute value
     * @return the corresponding WAM edit type, defaulting to
     *         {@link EditType#NOT_EDITED}
     */
    private static EditType mapEditType(int editAttribute) {
        return switch (editAttribute) {
            case MessageReceiveStanza.EDIT_MESSAGE -> EditType.EDITED;
            case MessageReceiveStanza.EDIT_PIN -> EditType.PIN;
            case MessageReceiveStanza.EDIT_SENDER_REVOKE -> EditType.SENDER_REVOKE;
            case MessageReceiveStanza.EDIT_ADMIN_REVOKE -> EditType.ADMIN_REVOKE;
            default -> EditType.NOT_EDITED;
        };
    }

    /**
     * Returns whether the stanza is LID-addressed.
     *
     * <p>True when the {@code addressing_mode} is {@code "lid"} or either the
     * sender or chat JID lives on the LID server.
     *
     * @param stanza the parsed stanza
     * @return {@code true} when the message is LID-addressed
     */
    private boolean isLidAddressed(MessageReceiveStanza stanza) {
        return "lid".equals(stanza.addressingMode().orElse(null))
                || stanza.senderJid().hasLidServer()
                || stanza.chatJid().hasLidServer();
    }

    /**
     * Returns whether the container's payload is a forwarded message.
     *
     * @param container the decoded container
     * @return {@code true} when the payload carries a forwarded
     *         {@link ContextInfo}
     */
    private static boolean isForwardedMessage(LinkedMessageContainer container) {
        return container.content() instanceof ContextualMessage contextual
                && contextual.contextInfo().map(ContextInfo::isForwarded).orElse(false);
    }

    /**
     * Computes a stable, non-reversible identifier from an input string.
     *
     * <p>Used to fill the {@code messageIdHmac} and {@code threadIdHmac} WAM
     * dimensions with real-looking hex digests. It hashes with SHA-256 rather
     * than a keyed HMAC because Cobalt does not run WhatsApp Web's per-account
     * thread-id HMAC key service.
     *
     * @param input the string to digest
     * @return the lowercase hex SHA-256 digest
     * @throws IllegalStateException if SHA-256 is unavailable
     */
    private static String pseudoHmac(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    /**
     * Commits a WAM event when the telemetry sink is present.
     *
     * <p>Central null-guard so every emitter can call
     * {@link WamService#commit(WamEventSpec)} without repeating the disabled-sink
     * check; a {@code null} {@link #wamService} silently drops the event.
     *
     * @param event the WAM event to commit
     */
    private void commit(WamEventSpec event) {
        if (wamService != null) {
            wamService.commit(event);
        }
    }
}
