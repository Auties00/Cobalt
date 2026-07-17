package com.github.auties00.cobalt.message.receipt;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveStanza;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentitySpec;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import com.github.auties00.libsignal.key.SignalPreKeyPair;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Builds and dispatches the {@code <receipt>} and {@code <ack>} stanzas that respond to
 * every incoming message processed by the receive pipeline.
 * <p>
 * The client constructs this handler during session setup and the receive orchestrator
 * invokes it after the
 * {@link com.github.auties00.cobalt.message.receive.crypto.MessageDecryptionResult} has
 * been resolved. The {@link MessageReceiptType} associated with each entry point is what
 * the server uses to fan out the delivery, read, or retry status across the conversation
 * participants. Bot invoke-responses are acknowledged with an {@code <ack class="message">}
 * stanza instead of a {@code <receipt>}.
 *
 * @implNote
 * This implementation centralises addressing decisions in three private resolvers
 * ({@link #resolveFrom(MessageReceiveStanza)},
 * {@link #resolveReceiptParticipant(MessageReceiveStanza)},
 * {@link #resolveRecipientForReceipt(MessageReceiveStanza)}) so each public entry point
 * only worries about the receipt-type selection; WhatsApp Web instead threads
 * {@code from}, {@code participant}, and {@code recipient} through every call site of
 * {@code sendDeliveryReceiptsAfterDecryption} and {@code sendRetryReceipt}.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgSendReceipt")
@WhatsAppWebModule(moduleName = "WAWebSendDeliveryReceiptJob")
@WhatsAppWebModule(moduleName = "WAWebSendRetryReceiptJob")
@WhatsAppWebModule(moduleName = "WAWebHandleMsgSendAck")
@WhatsAppWebModule(moduleName = "WAWebSendReceiptJobCommon")
public final class MessageReceiptHandler {
    /**
     * The logger for {@link MessageReceiptHandler}.
     */
    private static final System.Logger LOGGER = Log.get(MessageReceiptHandler.class);

    /**
     * Holds the retry count from which the {@code <keys>} bundle is attached to the retry
     * receipt.
     * <p>
     * The first retry is sent without the bundle because the sender is expected to still
     * hold the original session; the second and subsequent retries carry the bundle so the
     * sender can re-establish.
     *
     * @implNote
     * This implementation mirrors WhatsApp Web's {@code d=2} threshold inside
     * {@code WAWebSendRetryReceiptJob.sendRetryReceipt}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendRetryReceiptJob", exports = "sendRetryReceipt",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int RETRY_KEY_BUNDLE_THRESHOLD = 2;

    /**
     * Holds the client used to dispatch the constructed receipt stanzas over the socket.
     */
    private final LinkedWhatsAppClient client;

    /**
     * Holds the central session store used to look up the local PN/LID, signed prekey pair,
     * one-time prekeys, and the device-identity blob attached to the prekey bundle.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Constructs a receipt handler bound to the given client.
     * <p>
     * The store is read from {@link LinkedWhatsAppClient#store()} so no separate store argument is
     * required.
     *
     * @param client the client used to dispatch receipt stanzas
     * @throws NullPointerException if {@code client} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendReceipt", exports = "sendReceipt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public MessageReceiptHandler(LinkedWhatsAppClient client) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.store = client.store();
    }

    /**
     * Sends a delivery receipt for a successfully processed message, assuming the message
     * is active.
     * <p>
     * Delegates to {@link #sendDeliveryReceipt(MessageReceiveStanza, LinkedMessageInfo, boolean)}
     * with the inactive-message flag set to {@code false}, which covers normal active-chat
     * deliveries where the inactive-chat fast path does not apply.
     *
     * @param stanza the parsed incoming stanza
     * @param info   the processed message info, or {@code null} when no info was produced
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendReceipt", exports = "sendReceipt",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void sendDeliveryReceipt(MessageReceiveStanza stanza, LinkedMessageInfo info) {
        sendDeliveryReceipt(stanza, info, false);
    }

    /**
     * Sends a delivery receipt for a successfully processed message.
     * <p>
     * The {@link MessageReceiptType} is selected from the stanza addressing:
     * <ul>
     *   <li>peer messages resolve to {@link MessageReceiptType#PEER}.</li>
     *   <li>self-sent messages from a companion device resolve to
     *       {@link MessageReceiptType#SENDER}.</li>
     *   <li>inactive messages where the sender is not self resolve to
     *       {@link MessageReceiptType#INACTIVE}.</li>
     *   <li>everything else resolves to {@link MessageReceiptType#DELIVERY}, where the
     *       {@code type} attribute is dropped.</li>
     * </ul>
     * The {@code participant} attribute is set only for group, community, or broadcast
     * chats, and the {@code recipient} attribute only for a non-peer sender receipt with a
     * resolved recipient.
     *
     * @param stanza         the parsed incoming stanza
     * @param info           the processed message info, or {@code null} when no info was
     *                       produced
     * @param hasInactiveMsg whether processing reported an inactive-message flag
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendReceipt", exports = "sendReceipt",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSendDeliveryReceiptJob", exports = "sendDeliveryReceiptsAfterDecryption",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void sendDeliveryReceipt(MessageReceiveStanza stanza, LinkedMessageInfo info, boolean hasInactiveMsg) {
        var from = resolveFrom(stanza);
        var participant = resolveReceiptParticipant(stanza);
        var isSender = isSenderReceipt(from, participant);
        var receiptType = resolveDeliveryReceiptType(stanza, isSender, hasInactiveMsg);

        var isPeer = stanza.isPeer();
        var recipientJid = resolveRecipientForReceipt(stanza);
        var shouldSetRecipient = !isPeer && isSender && recipientJid != null;

        var toJid = from.toUserJid();
        var receipt = new StanzaBuilder()
                .description("receipt")
                .attribute("id", stanza.id())
                .attribute("type", receiptType.protocolValue())
                .attribute("to", toJid)
                .attribute("participant",
                        (from.hasGroupOrCommunityServer() || from.hasBroadcastServer()) && participant != null
                                ? participant
                                : null)
                .attribute("recipient",
                        shouldSetRecipient ? recipientJid.toUserJid() : null);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "sending delivery receipt id={0} type={1} to={2}",
                    stanza.id(), receiptType, toJid);
        }
        client.sendNodeWithNoResponse(receipt.build());
    }

    /**
     * Sends a retry receipt that asks the sender to re-encrypt and re-send a payload that
     * could not be decrypted.
     * <p>
     * The receipt carries the failure reason and the retry count, and from the
     * {@value #RETRY_KEY_BUNDLE_THRESHOLD}th attempt onward attaches a fresh {@code <keys>}
     * bundle so the sender can re-establish the Signal session. The call is skipped when
     * the chat is a non-bot user but the participant is a bot, because the bot has no useful
     * retry semantics on that channel.
     *
     * @param stanza      the parsed incoming stanza
     * @param retryReason the failure reason carried in the {@code error} attribute
     * @param retryCount  the 1-based current retry attempt
     */
    @WhatsAppWebExport(moduleName = "WAWebSendRetryReceiptJob", exports = "sendRetryReceipt",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void sendRetryReceipt(
            MessageReceiveStanza stanza,
            WhatsAppMessageException.Receive.RetryReason retryReason,
            int retryCount
    ) {
        var from = resolveFrom(stanza);
        var participant = resolveReceiptParticipant(stanza);
        if (!from.hasBotServer() && participant != null && participant.hasBotServer()) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "skipping retry receipt for bot participant id={0}", stanza.id());
            }
            return;
        }

        var retryNode = new StanzaBuilder()
                .description("retry")
                .attribute("v", "1")
                .attribute("count", retryCount)
                .attribute("id", stanza.id())
                .attribute("t", String.valueOf(stanza.timestamp().getEpochSecond()))
                .attribute("error", retryReason.protocolValue())
                .build();

        var registrationNode = new StanzaBuilder()
                .description("registration")
                .content(DataUtils.intToBytes(store.signalStore().registrationId(), 4))
                .build();

        var keysNode = retryCount >= RETRY_KEY_BUNDLE_THRESHOLD
                ? buildKeyBundleNode()
                : null;

        Jid toJid;
        Jid participantAttr = null;
        Jid recipientAttr = null;
        String categoryAttr = null;

        if (from.hasUserServer() || from.hasLidServer()) {
            toJid = from;
            var selfJid = store.accountStore().jid().orElse(null);
            if (selfJid != null && from.toUserJid().equals(selfJid.toUserJid())) {
                if (stanza.isPeer()) {
                    categoryAttr = "peer";
                } else {
                    var recipientJid = resolveRecipientForReceipt(stanza);
                    if (recipientJid != null) {
                        recipientAttr = recipientJid.toUserJid();
                    }
                }
            }
        } else {
            toJid = from.toUserJid();
            if (participant != null) {
                participantAttr = participant;
            }
        }

        var receipt = new StanzaBuilder()
                .description("receipt")
                .attribute("id", stanza.id())
                .attribute("type", MessageReceiptType.RETRY.protocolValue())
                .attribute("to", toJid)
                .attribute("participant", participantAttr)
                .attribute("recipient", recipientAttr)
                .attribute("category", categoryAttr)
                .content(retryNode, registrationNode, keysNode);
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "sending retry receipt id={0} reason={1} count={2} keyBundle={3}",
                    stanza.id(), retryReason, retryCount, keysNode != null);
        }
        client.sendNodeWithNoResponse(receipt.build());
    }

    /**
     * Sends the specialised {@code <ack>} stanza used to acknowledge a bot invoke-response
     * message.
     * <p>
     * Bot replies use an {@code <ack class="message" type="text">} rather than a
     * {@code <receipt>}. For 1:1 chats the addressing is flipped so that {@code to} is the
     * bot author and {@code recipient} is the chat; group and broadcast bot replies use
     * {@code to = chat} and {@code participant = author}.
     *
     * @param stanza the parsed incoming stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "sendBotInvokeResponseAcks",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void sendBotInvokeResponseAck(MessageReceiveStanza stanza) {
        var chatJid = stanza.chatJid();
        Jid to;
        Jid recipient = null;
        Jid participantJid = null;

        if (!chatJid.hasGroupOrCommunityServer() && !chatJid.hasBroadcastServer()) {
            to = stanza.senderJid();
            recipient = chatJid.toUserJid();
        } else {
            to = chatJid;
            participantJid = stanza.participant()
                    .map(Jid::toUserJid)
                    .orElse(null);
        }

        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("id", stanza.id())
                .attribute("to", to)
                .attribute("recipient", recipient)
                .attribute("participant", participantJid)
                .attribute("class", "message")
                .attribute("type", "text");
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "sending bot invoke-response ack id={0} to={1}", stanza.id(), to);
        }
        client.sendNodeWithNoResponse(ack.build());
    }

    /**
     * Returns whether the message originates from a bot sender that requires a bot-specific
     * receipt rather than a normal delivery receipt.
     * <p>
     * The orchestrator uses this to choose between
     * {@link #sendDeliveryReceipt(MessageReceiveStanza, LinkedMessageInfo)} and
     * {@link #sendBotInvokeResponseAck(MessageReceiveStanza)}; the predicate holds when the
     * chat is not on the bot server but the sender is.
     *
     * @param stanza the parsed incoming stanza
     * @return {@code true} when the chat is not on the bot server but the sender is
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendReceipt", exports = "sendReceipt",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean isBotSender(MessageReceiveStanza stanza) {
        return !stanza.chatJid().hasBotServer()
                && stanza.senderJid().hasBotServer();
    }

    /**
     * Sends a NACK for a message that failed validation or protobuf parsing, without a
     * failure reason.
     * <p>
     * Delegates to {@link #sendNackReceipt(MessageReceiveStanza, int, Integer)} with a
     * {@code null} failure reason, which covers the
     * {@link com.github.auties00.cobalt.message.receive.crypto.MessageDecryptionResult#PARSE_ERROR}
     * path where the parsing error has no extra {@code failure_reason} payload.
     *
     * @param stanza    the parsed incoming stanza
     * @param errorCode the NACK error code carried in the {@code error} attribute
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendAck", exports = "sendNack",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void sendNackReceipt(MessageReceiveStanza stanza, int errorCode) {
        sendNackReceipt(stanza, errorCode, null);
    }

    /**
     * Sends a NACK for a message that failed validation or protobuf parsing.
     * <p>
     * The resulting stanza is an {@code <ack class="message">} with an {@code error}
     * attribute. For an invalid protobuf (error code 491) the stanza additionally carries a
     * {@code <meta failure_reason=...>} child when the caller supplies a reason.
     *
     * @param stanza        the parsed incoming stanza
     * @param errorCode     the NACK error code carried in the {@code error} attribute
     * @param failureReason the failure-reason payload for invalid-protobuf errors, or
     *                      {@code null} to omit the {@code <meta>} child
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendAck", exports = "sendNack",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void sendNackReceipt(MessageReceiveStanza stanza, int errorCode, Integer failureReason) {
        Stanza metaStanza = null;
        if (errorCode == 491 && failureReason != null) {
            metaStanza = new StanzaBuilder()
                    .description("meta")
                    .attribute("failure_reason", failureReason)
                    .build();
        }

        var ack = new StanzaBuilder()
                .description("ack")
                .attribute("id", stanza.id())
                .attribute("class", "message")
                .attribute("from", store.accountStore().jid().orElse(null))
                .attribute("to", resolveFrom(stanza))
                .attribute("participant",
                        resolveReceiptParticipant(stanza))
                .attribute("type", stanza.stanzaType())
                .attribute("error", errorCode)
                .content(metaStanza);
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "sending nack id={0} errorCode={1} failureReason={2}",
                    stanza.id(), errorCode, failureReason);
        }
        client.sendNodeWithNoResponse(ack.build());
    }

    /**
     * Builds the {@code <keys>} bundle attached to a retry receipt from the
     * {@value #RETRY_KEY_BUNDLE_THRESHOLD}th attempt onward.
     * <p>
     * The bundle includes the registered prekey type, the local identity key, a one-time
     * prekey, the signed prekey, and the ADV-signed device identity; the sender uses these
     * to install a fresh Signal session before re-encrypting.
     *
     * @implNote
     * This implementation lazily provisions a one-time prekey when the local store has none:
     * a random {@link SignalPreKeyPair} is generated and persisted so the next retry attempt
     * sees a stable bundle. A build failure (for example a missing identity-key pair) is
     * logged and the method returns {@code null} so the retry receipt still goes out without
     * the bundle.
     *
     * @return the {@code <keys>} stanza, or {@code null} when the bundle cannot be built
     */
    @WhatsAppWebExport(moduleName = "WAWebSendRetryReceiptJob", exports = "sendRetryReceipt",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Stanza buildKeyBundleNode() {
        try {
            var preKey = store.signalStore().hasPreKeys()
                    ? store.signalStore().preKeys().getFirst()
                    : SignalPreKeyPair.random(1);
            if (!store.signalStore().hasPreKeys()) {
                store.signalStore().addPreKey(preKey);
            }

            var typeNode = new StanzaBuilder()
                    .description("type")
                    .content(new byte[]{SignalIdentityPublicKey.type()})
                    .build();

            var identityNode = new StanzaBuilder()
                    .description("identity")
                    .content(store.signalStore().identityKeyPair().publicKey().toEncodedPoint())
                    .build();

            var preKeyIdNode = new StanzaBuilder()
                    .description("id")
                    .content(DataUtils.intToBytes(preKey.id(), 3))
                    .build();
            var preKeyValueNode = new StanzaBuilder()
                    .description("value")
                    .content(preKey.publicKey().toEncodedPoint())
                    .build();
            var preKeyNode = new StanzaBuilder()
                    .description("key")
                    .content(preKeyIdNode, preKeyValueNode)
                    .build();

            var signedKeyPair = store.signalStore().signedKeyPair();
            var skeyIdNode = new StanzaBuilder()
                    .description("id")
                    .content(DataUtils.intToBytes(signedKeyPair.id(), 3))
                    .build();
            var skeyValueNode = new StanzaBuilder()
                    .description("value")
                    .content(signedKeyPair.publicKey().toEncodedPoint())
                    .build();
            var skeySigNode = new StanzaBuilder()
                    .description("signature")
                    .content(signedKeyPair.signature())
                    .build();
            var skeyNode = new StanzaBuilder()
                    .description("skey")
                    .content(skeyIdNode, skeyValueNode, skeySigNode)
                    .build();

            var deviceIdentityNode = store.signalStore().signedDeviceIdentity()
                    .map(id -> new StanzaBuilder()
                            .description("device-identity")
                            .content(ADVSignedDeviceIdentitySpec.encode(id))
                            .build())
                    .orElse(null);

            return new StanzaBuilder()
                    .description("keys")
                    .content(typeNode, identityNode, preKeyNode, skeyNode, deviceIdentityNode)
                    .build();
        } catch (Exception e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "failed to build key bundle for retry receipt", e);
            }
            return null;
        }
    }

    /**
     * Resolves the JID that the {@code from} attribute, or for receipt stanzas the
     * {@code to} attribute, should carry.
     * <p>
     * For 1:1 chat messages the value is the sender's JID; for group, community, broadcast,
     * and status messages the value is the chat JID.
     *
     * @param stanza the parsed incoming stanza
     * @return the resolved JID
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingApiUtils", exports = "getFrom",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Jid resolveFrom(MessageReceiveStanza stanza) {
        var chatJid = stanza.chatJid();
        if (!chatJid.hasGroupOrCommunityServer()
                && !chatJid.hasBroadcastServer()
                && !chatJid.isStatusBroadcastAccount()) {
            return stanza.senderJid();
        }

        return chatJid;
    }

    /**
     * Resolves the participant attribute placed on receipt stanzas.
     * <p>
     * Group, community, broadcast, and status receipts carry the sender device JID in the
     * {@code participant} attribute so the server can route the receipt back to the
     * originating device; 1:1 chats omit the attribute because the {@code to} JID already
     * identifies the recipient.
     *
     * @param stanza the parsed incoming stanza
     * @return the participant JID, or {@code null} for 1:1 chats
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendReceipt", exports = "sendReceipt",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Jid resolveReceiptParticipant(MessageReceiveStanza stanza) {
        var chatJid = stanza.chatJid();
        if (chatJid.hasGroupOrCommunityServer()
                || chatJid.hasBroadcastServer()
                || chatJid.isStatusBroadcastAccount()) {
            return stanza.participant().orElse(null);
        }
        return null;
    }

    /**
     * Resolves the recipient attribute placed on delivery and retry receipts for
     * companion-device messages.
     * <p>
     * The recipient is only meaningful for 1:1-style chat messages sent from the user's own
     * account (echoed from a companion device); group, community, broadcast, and status
     * receipts return {@code null} so the attribute is dropped. For a self-echo the value is
     * the peer the message was addressed to: the plain {@code recipient} attribute when the
     * stanza carries one, and the chat JID otherwise.
     *
     * @implNote
     * This implementation realizes WhatsApp Web's
     * {@code originalBotRecipient ?? preMatChat ?? chat} resolution as
     * {@code recipient ?? chatJid}. Both {@code preMatChat} and {@code originalBotRecipient}
     * preserve the pre-rewrite address across a client-side chat relocation (the LID address
     * migration for {@code preMatChat}, the FBID and Maiba bot-thread relocations for
     * {@code originalBotRecipient}); Cobalt applies no such relocation, so
     * {@link MessageReceiveStanza#chatJid()} already holds that pre-rewrite address and the
     * plain {@link MessageReceiveStanza#recipient()} attribute supplies the peer for the
     * self-authored echoes whose {@code from} is the local account.
     *
     * @param stanza the parsed incoming stanza
     * @return the recipient JID, or {@code null} when the receipt should omit the attribute
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendReceipt", exports = "sendReceipt",
            adaptation = WhatsAppAdaptation.DIRECT)
    private Jid resolveRecipientForReceipt(MessageReceiveStanza stanza) {
        var chatJid = stanza.chatJid();
        if (chatJid.hasGroupOrCommunityServer()
                || chatJid.hasBroadcastServer()
                || chatJid.isStatusBroadcastAccount()) {
            return null;
        }

        var selfJid = store.accountStore().jid().orElse(null);
        if (selfJid == null) {
            return null;
        }

        if (!stanza.senderJid().toUserJid().equals(selfJid.toUserJid())) {
            return null;
        }

        return stanza.recipient().orElse(chatJid);
    }

    /**
     * Returns whether the receipt should use the {@link MessageReceiptType#SENDER} type.
     * <p>
     * The sender type is selected when the {@code from} JID is the local account's user, or
     * when the participant JID is the local account's user, so a companion-device echo loops
     * back to the primary as a sender confirmation.
     *
     * @param from        the resolved {@code from} JID
     * @param participant the resolved participant JID, or {@code null} for 1:1
     * @return {@code true} when the sender receipt type should be used
     */
    @WhatsAppWebExport(moduleName = "WAWebSendDeliveryReceiptJob", exports = "sendDeliveryReceiptsAfterDecryption",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean isSenderReceipt(Jid from, Jid participant) {
        var selfJid = store.accountStore().jid().orElse(null);
        if (selfJid == null) {
            return false;
        }

        var selfUser = selfJid.toUserJid();

        if ((from.hasUserServer() || from.hasLidServer())
                && from.toUserJid().equals(selfUser)) {
            return true;
        }

        if (participant != null && participant.toUserJid().equals(selfUser)) {
            return true;
        }

        return false;
    }

    /**
     * Resolves the {@link MessageReceiptType} used for the delivery receipt path.
     * <p>
     * Peer messages take priority over sender receipts, and sender receipts take priority
     * over the inactive-message branch; anything else falls back to
     * {@link MessageReceiptType#DELIVERY}.
     *
     * @param stanza         the parsed incoming stanza
     * @param isSender       whether the message is from the local account
     * @param hasInactiveMsg whether processing produced an inactive-message flag
     * @return the resolved delivery receipt type
     */
    @WhatsAppWebExport(moduleName = "WAWebSendDeliveryReceiptJob", exports = "sendDeliveryReceiptsAfterDecryption",
            adaptation = WhatsAppAdaptation.DIRECT)
    private MessageReceiptType resolveDeliveryReceiptType(
            MessageReceiveStanza stanza,
            boolean isSender,
            boolean hasInactiveMsg
    ) {
        if (stanza.isPeer()) {
            return MessageReceiptType.PEER;
        }

        if (isSender) {
            return MessageReceiptType.SENDER;
        }

        if (hasInactiveMsg) {
            return MessageReceiptType.INACTIVE;
        }

        return MessageReceiptType.DELIVERY;
    }
}
