package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses every incoming {@code <message>} stanza into the structured
 * {@link MessageReceiveStanza} the rest of the receive pipeline consumes.
 *
 * <p>The single entry point ({@link #parse}) is a stateless adapter. The caller
 * is the inbound message-dispatch loop: it hands every {@code <message>} stanza to
 * this class before attempting decryption, so the dedup layer, the receipt
 * emitter, and the per-payload decryptor all operate on the same structured
 * snapshot rather than re-scanning the raw stanza.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgParser")
public final class MessageReceiveStanzaParser {
    /**
     * The logger for {@link MessageReceiveStanzaParser}.
     */
    private static final System.Logger LOGGER = Log.get(MessageReceiveStanzaParser.class);

    /**
     * Prevents instantiation of this utility class.
     *
     * <p>All entry points are {@code static}, so instances would carry no state.
     *
     * @throws AssertionError always
     */
    private MessageReceiveStanzaParser() {
        throw new AssertionError();
    }

    /**
     * Parses a raw {@code <message>} stanza into a structured
     * {@link MessageReceiveStanza}.
     *
     * <p>The local account's PN and LID let the message-type classifier
     * recognize self-originated broadcasts and status posts; either argument may
     * be {@code null}, in which case the matching is-me-account branch is skipped
     * and ambiguous broadcasts are treated as non-self.
     *
     * @implNote
     * This implementation aggregates every parsed field into a single
     * allocation, delegating missing-required-attribute handling to
     * {@link Stanza#getRequiredAttributeAsString} and
     * {@link Stanza#getRequiredAttributeAsLong}.
     *
     * @param stanza       the inbound {@code <message>} stanza
     * @param selfPnJid  the local account's PN JID, or {@code null}
     * @param selfLidJid the local account's LID, or {@code null}
     * @return the parsed stanza
     * @throws NullPointerException     if {@code stanza} is {@code null}
     * @throws IllegalArgumentException if required attributes are missing or malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static MessageReceiveStanza parse(Stanza stanza, Jid selfPnJid, Jid selfLidJid) {
        Objects.requireNonNull(stanza, "stanza cannot be null");

        var id = stanza.getRequiredAttributeAsString("id");
        var timestampSeconds = stanza.getRequiredAttributeAsLong("t");
        var timestamp = Instant.ofEpochSecond(timestampSeconds);
        var fromJid = stanza.getRequiredAttributeAsJid("from");

        var stanzaType = stanza.getRequiredAttributeAsString("type");
        var editAttribute = stanza.getAttributeAsInt("edit", 0);
        var pushName = stanza.getAttributeAsString("notify", null);
        var category = stanza.getAttributeAsString("category", null);
        var offline = stanza.getAttributeAsString("offline", null);
        var addressingMode = stanza.getAttributeAsString("addressing_mode", null);

        var participant = stanza.getAttributeAsJid("participant", null);

        var senderPn = stanza.getAttributeAsJid("sender_pn", null);
        var senderLid = stanza.getAttributeAsJid("sender_lid", null);
        var recipient = stanza.getAttributeAsJid("recipient", null);
        var recipientPn = stanza.getAttributeAsJid("recipient_pn", null);
        var recipientLid = stanza.getAttributeAsJid("recipient_lid", null);
        var peerRecipientPn = stanza.getAttributeAsJid("peer_recipient_pn", null);
        var peerRecipientLid = stanza.getAttributeAsJid("peer_recipient_lid", null);
        var peerRecipientUsername = stanza.getAttributeAsString("peer_recipient_username", null);
        var recipientLatestLid = stanza.getAttributeAsJid("recipient_latest_lid", null);
        var recipientUsername = stanza.getAttributeAsString("recipient_username", null);
        var participantPn = stanza.getAttributeAsJid("participant_pn", null);
        var participantLid = stanza.getAttributeAsJid("participant_lid", null);
        var participantUsername = stanza.getAttributeAsString("participant_username", null);
        var username = stanza.getAttributeAsString("username", null);
        var displayName = stanza.getAttributeAsString("display_name", null);

        var count = stanza.getAttributeAsInt("count", null);

        var isHsm = stanza.getChild("hsm").isPresent();

        var senderJid = resolveSender(fromJid, participant);

        var encs = parseEncryptedPayloads(stanza);

        var messageType = resolveMessageType(fromJid, participant, selfPnJid, selfLidJid, encs, category);

        var deviceIdentity = stanza.getChild("device-identity")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);

        var unavailableNode = stanza.getChild("unavailable", null);
        var unavailable = unavailableNode != null;
        var hostedUnavailable = unavailable
                && "true".equals(unavailableNode.getAttributeAsString("hosted").orElse(null));
        var viewOnceUnavailable = unavailable
                && "view_once".equals(unavailableNode.getAttributeAsString("type").orElse(null));

        var metaNode = stanza.getChild("meta", null);
        String pollType = null;
        String eventType = null;
        String origin = null;
        var statusMentioned = false;
        String appdata = null;
        String bizSource = null;
        String threadMsgId = null;
        Jid threadMsgSenderJid = null;
        String targetId = null;
        Jid targetSenderJid = null;
        Jid targetChatJid = null;
        Jid targetChatJidLid = null;
        var capi = false;
        String contextSource = null;
        String senderCountryCode = null;
        if (metaNode != null) {
            if ("poll".equals(stanzaType)) {
                pollType = metaNode.getAttributeAsString("polltype", null);
            }

            if ("event".equals(stanzaType)) {
                eventType = metaNode.getAttributeAsString("event_type", null);
            }
            origin = metaNode.getAttributeAsString("origin", null);
            statusMentioned = "true".equals(
                    metaNode.getAttributeAsString("status_mentioned").orElse(null));
            appdata = metaNode.getAttributeAsString("appdata", null);
            bizSource = metaNode.getAttributeAsString("biz_source", null);
            threadMsgId = metaNode.getAttributeAsString("thread_msg_id", null);
            threadMsgSenderJid = metaNode.getAttributeAsJid("thread_msg_sender_jid", null);
            targetId = metaNode.getAttributeAsString("target_id", null);
            targetSenderJid = metaNode.getAttributeAsJid("target_sender_jid", null);
            targetChatJid = metaNode.getAttributeAsJid("target_chat_jid", null);
            targetChatJidLid = metaNode.getAttributeAsJid("target_chat_jid_lid", null);
            capi = "true".equals(
                    metaNode.getAttributeAsString("capi").orElse(null));
            contextSource = metaNode.getAttributeAsString("context_source", null);
            senderCountryCode = metaNode.getAttributeAsString("sender_country_code", null);
        }

        var urlNumber = stanza.getChild("url_number").isPresent();
        var urlText = stanza.getChild("url_text").isPresent();

        var botInfo = parseBotInfo(stanza);
        var bizInfo = parseBizInfo(stanza);
        var reportingInfo = parseReportingInfo(stanza);
        var bclParticipants = parseBroadcastParticipants(stanza);
        var paymentInfo = parsePaymentInfo(stanza);

        var ephSetting = stanza.getAttributeAsString("eph_setting", null);

        var rcat = stanza.getChild("rcat")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);

        var hsmNode = stanza.getChild("hsm", null);
        String hsmTag = null;
        String hsmCategory = null;
        if (hsmNode != null) {
            hsmTag = hsmNode.getAttributeAsString("tag", null);
            hsmCategory = hsmNode.getAttributeAsString("category", null);
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "parsed message stanza id={0} type={1} from={2} participant={3}",
                    id, messageType, fromJid, participant);
        }

        return new MessageReceiveStanza(
                id,
                timestamp,
                fromJid,
                senderJid,
                participant,
                messageType,
                editAttribute,
                pushName,
                category,
                offline,
                addressingMode,
                isHsm,
                count,
                senderPn,
                senderLid,
                recipient,
                recipientPn,
                recipientLid,
                peerRecipientPn,
                peerRecipientLid,
                peerRecipientUsername,
                recipientLatestLid,
                recipientUsername,
                participantPn,
                participantLid,
                participantUsername,
                username,
                displayName,
                stanzaType,
                unavailable,
                hostedUnavailable,
                viewOnceUnavailable,
                pollType,
                eventType,
                origin,
                urlNumber,
                urlText,
                statusMentioned,
                appdata,
                bizSource,
                threadMsgId,
                threadMsgSenderJid,
                targetId,
                targetSenderJid,
                targetChatJid,
                targetChatJidLid,
                capi,
                contextSource,
                senderCountryCode,
                encs,
                deviceIdentity,
                botInfo,
                bizInfo,
                reportingInfo,
                bclParticipants,
                paymentInfo,
                ephSetting,
                rcat,
                hsmTag,
                hsmCategory
        );
    }

    /**
     * Returns the actual sender's device JID derived from the stanza's
     * addressing.
     *
     * <p>For 1:1 messages the sender equals the {@code from} JID; for group,
     * broadcast, and status messages it is the {@code participant} JID.
     *
     * @param fromJid     the {@code from} attribute JID
     * @param participant the {@code participant} attribute JID, or {@code null}
     * @return the resolved sender JID
     * @throws IllegalArgumentException when a group, broadcast, or status
     *                                  stanza arrives without a participant
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static Jid resolveSender(Jid fromJid, Jid participant) {
        if (fromJid.hasGroupOrCommunityServer() || fromJid.hasBroadcastServer()) {
            if (participant == null) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "group/broadcast/status message from {0} missing participant attribute", fromJid);
                }
                throw new IllegalArgumentException(
                        "Group/broadcast/status message from " + fromJid
                                + " missing participant attribute");
            }
            return participant;
        }
        return fromJid;
    }

    /**
     * Classifies the addressing shape of an incoming stanza into a
     * {@link MessageType}.
     *
     * <p>The result drives every later branching decision: which Signal cipher to
     * use, which receipt to emit, whether to attach the broadcast contact list.
     * See {@link MessageType} for the meaning of each value.
     *
     * @implNote
     * This implementation collapses the upstream two-axis check (a chat message
     * combined with the peer message category) into the single
     * {@link MessageType#PEER_CHAT} value, and collapses the direct
     * sub-classification of {@link MessageType#OTHER_STATUS} into the boolean
     * {@link MessageReceiveStanza#isDirect()} accessor.
     *
     * @param fromJid     the {@code from} attribute JID
     * @param participant the {@code participant} attribute JID, or {@code null}
     * @param selfPnJid   the local account's PN JID, or {@code null}
     * @param selfLidJid  the local account's LID, or {@code null}
     * @param encs        the parsed encrypted payloads
     * @param category    the {@code category} attribute, or {@code null}
     * @return the classified message type
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static MessageType resolveMessageType(
            Jid fromJid,
            Jid participant,
            Jid selfPnJid,
            Jid selfLidJid,
            List<MessageReceiveEncryptedPayload> encs,
            String category) {
        if (fromJid.hasUserServer() || fromJid.hasLidServer() || fromJid.hasBotServer()) {
            if ("peer".equals(category)) {
                return MessageType.PEER_CHAT;
            } else {
                return MessageType.CHAT;
            }
        }

        if (fromJid.hasGroupOrCommunityServer()) {
            return MessageType.GROUP;
        }

        if (fromJid.hasBroadcastServer()) {
            var isStatus = fromJid.isStatusBroadcastAccount();
            var isSelf = isMeAccount(participant, selfPnJid, selfLidJid);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "broadcast message classification: status={0} self={1}", isStatus, isSelf);
            }
            if (!isStatus) {
                return isSelf ? MessageType.PEER_BROADCAST : MessageType.OTHER_BROADCAST;
            }

            var isDirect = encs.stream().noneMatch(enc ->
                    enc.e2eType().isSenderKeyMessage());
            if (isSelf && isDirect) {
                return MessageType.DIRECT_PEER_STATUS;
            }
            return MessageType.OTHER_STATUS;
        }

        return MessageType.CHAT;
    }

    /**
     * Returns whether the given participant identifies the locally logged-in
     * account at user level.
     *
     * <p>The user-level comparison strips the device suffix so any companion
     * device is treated as the same account as the primary; both the PN and LID
     * branches are checked so a LID-addressed status broadcast from the local
     * account is still recognised as self.
     *
     * @param participant the participant JID, or {@code null}
     * @param selfPnJid   the local account's PN JID, or {@code null}
     * @param selfLidJid  the local account's LID, or {@code null}
     * @return {@code true} when {@code participant} matches either identity
     */
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsMeUser", exports = "isMeAccount",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isMeAccount(Jid participant, Jid selfPnJid, Jid selfLidJid) {
        if (participant == null) {
            return false;
        }
        var participantUser = participant.toUserJid();
        if (selfPnJid != null && participantUser.equals(selfPnJid.toUserJid())) {
            return true;
        }
        return selfLidJid != null && participantUser.equals(selfLidJid.toUserJid());
    }

    /**
     * Parses every {@code <enc>} child of the message stanza into a typed payload
     * list.
     *
     * <p>The list iteration order matches the wire order; downstream code relies
     * on this when picking the first payload's retry count or when preferring an
     * {@code skmsg} envelope over the per-device retry one.
     *
     * @implNote
     * This implementation drops {@code <enc>} nodes whose content is empty to
     * avoid producing payloads that no Signal cipher can act on.
     *
     * @param stanza the parent {@code <message>} stanza
     * @return the parsed encrypted payloads
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static List<MessageReceiveEncryptedPayload> parseEncryptedPayloads(Stanza stanza) {
        var encNodes = stanza.getChildren("enc");
        var payloads = new ArrayList<MessageReceiveEncryptedPayload>(encNodes.size());
        for (var encNode : encNodes) {
            var typeStr = encNode.getRequiredAttributeAsString("type");
            var e2eType = MessageEncryptionType.fromProtocolValue(typeStr);
            var encMediaType = encNode.getAttributeAsString("mediatype", null);
            var ciphertext = encNode.toContentBytes().orElse(null);

            if (ciphertext == null || ciphertext.length == 0) {
                continue;
            }
            var retryCount = encNode.getAttributeAsInt("count", 0);
            var hideFail = "hide".equals(
                    encNode.getAttributeAsString("decrypt-fail").orElse(null));
            payloads.add(new MessageReceiveEncryptedPayload(
                    e2eType, encMediaType, ciphertext, retryCount, hideFail));
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "parsed {0} encrypted payload(s) from {1} enc node(s)",
                    payloads.size(), encNodes.size());
        }
        return payloads;
    }

    /**
     * Parses the {@code <bot>} child into a {@link MessageReceiveBotInfo}, or
     * returns {@code null} when no bot child is present.
     *
     * <p>Populated for Meta AI and 1P/3P business bot replies; the downstream
     * rich-response stitcher consumes the result to thread streaming chunks back
     * together.
     *
     * @param stanza the parent {@code <message>} stanza
     * @return the parsed bot info, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static MessageReceiveBotInfo parseBotInfo(Stanza stanza) {
        var botNode = stanza.getChild("bot", null);
        if (botNode == null) {
            return null;
        }

        var senderTimestampMs = botNode.getAttributeAsString("sender_timestamp_ms", null);
        var editTargetId = botNode.getAttributeAsString("edit_target_id", null);
        var editType = botNode.getAttributeAsString("edit", null);
        var bodyType = botNode.getAttributeAsString("type", null);
        var bizBotType = botNode.getAttributeAsString("biz_bot", null);
        return new MessageReceiveBotInfo(
                senderTimestampMs,
                editTargetId,
                editType,
                bodyType,
                bizBotType
        );
    }

    /**
     * Parses the business metadata into a {@link MessageReceiveBizInfo},
     * combining stanza attributes with the {@code <biz>} child, or returns
     * {@code null} when no business attribute or child is present.
     *
     * <p>The stanza-level {@code verified_name} attribute, the
     * {@code verified_level} attribute, the {@code <verified_name>} child (cert
     * bytes), and the {@code <biz>} child all flow into one record so the
     * rendering pipeline has a single object to consult.
     *
     * @implNote
     * This implementation reads the verified-buttons and verified-list envelopes
     * from the {@code <biz>} child directly, but reads the verified-HSM envelope
     * from the parent {@code <message>} stanza because the {@code <hsm>} child sits
     * at the message level rather than inside {@code <biz>}.
     *
     * @param stanza the parent {@code <message>} stanza
     * @return the parsed biz info, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static MessageReceiveBizInfo parseBizInfo(Stanza stanza) {
        var verifiedNameSerial = stanza.getAttributeAsInt("verified_name", null);
        var verifiedLevel = stanza.getAttributeAsString("verified_level", null);
        var verifiedNameCert = stanza.getChild("verified_name")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);

        var bizNode = stanza.getChild("biz", null);
        if (verifiedNameSerial == null && verifiedLevel == null
                && verifiedNameCert == null && bizNode == null) {
            return null;
        }

        Integer actualActors = null;
        Integer hostStorage = null;
        Integer privacyModeTs = null;
        String nativeFlowName = null;
        String campaignId = null;
        var verifiedButtonsEnvelope = false;
        var verifiedListEnvelope = false;
        var verifiedHsmEnvelope = false;

        if (bizNode != null) {
            actualActors = bizNode.getAttributeAsInt("actual_actors", null);
            hostStorage = bizNode.getAttributeAsInt("host_storage", null);
            privacyModeTs = bizNode.getAttributeAsInt("privacy_mode_ts", null);
            campaignId = bizNode.getAttributeAsString("campaign_id", null);
            nativeFlowName = resolveNativeFlowName(bizNode);
            verifiedButtonsEnvelope = bizNode.getChild("buttons").isPresent();
            verifiedListEnvelope = bizNode.getChild("list").isPresent();

            verifiedHsmEnvelope = stanza.getChild("hsm").isPresent();
        }

        return new MessageReceiveBizInfo(
                verifiedNameCert,
                verifiedNameSerial != null ? verifiedNameSerial : -1,
                verifiedLevel,
                nativeFlowName,
                campaignId,
                actualActors,
                hostStorage,
                privacyModeTs,
                verifiedButtonsEnvelope,
                verifiedListEnvelope,
                verifiedHsmEnvelope
        );
    }

    /**
     * Returns the native-flow name from the {@code <biz>} stanza.
     *
     * <p>Prefers the nested
     * {@code <interactive><native_flow name="..."/></interactive>} shape and
     * falls back to a direct {@code native_flow_name} attribute on the
     * {@code <biz>} stanza when the nested structure is absent.
     *
     * @param bizStanza the {@code <biz>} child stanza
     * @return the native-flow name, or {@code null} when neither path resolves it
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static String resolveNativeFlowName(Stanza bizStanza) {
        var interactiveNode = bizStanza.getChild("interactive", null);
        if (interactiveNode != null) {
            var nativeFlowNode = interactiveNode.getChild("native_flow", null);
            if (nativeFlowNode != null) {
                var name = nativeFlowNode.getAttributeAsString("name", null);
                if (name != null) {
                    return name;
                }
            }
        }

        return bizStanza.getAttributeAsString("native_flow_name", null);
    }

    /**
     * Parses the {@code <reporting>} child into a
     * {@link MessageReceiveReportingInfo}, or returns {@code null} when no
     * reporting child is present.
     *
     * <p>Extracts the reporting-token bytes and version from the
     * {@code <reporting_token>} child and the reporting-tag bytes from the
     * {@code <reporting_tag>} child; the stanza's own {@code t} attribute is
     * captured so a later abuse report can prove the token was bound to this
     * delivery.
     *
     * @implNote
     * This implementation does not check the reporting-token-receive feature gate
     * before parsing; absent gating, the absence of a {@code <reporting>} child
     * is the only short-circuit.
     *
     * @param stanza the parent {@code <message>} stanza
     * @return the parsed reporting info, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static MessageReceiveReportingInfo parseReportingInfo(Stanza stanza) {
        var reportingNode = stanza.getChild("reporting", null);
        if (reportingNode == null) {
            return null;
        }

        var stanzaTs = Instant.ofEpochSecond(stanza.getRequiredAttributeAsLong("t"));

        var tokenNode = reportingNode.getChild("reporting_token", null);
        byte[] reportingToken = null;
        var version = 0;
        if (tokenNode != null) {
            reportingToken = tokenNode.toContentBytes().orElse(null);
            version = tokenNode.getAttributeAsInt("v", 0);
        }

        var reportingTag = reportingNode.getChild("reporting_tag")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);

        return new MessageReceiveReportingInfo(
                stanzaTs,
                reportingToken,
                version,
                reportingTag
        );
    }

    /**
     * Parses the {@code <pay>} and {@code <transaction>} sibling children into a
     * {@link MessageReceivePaymentInfo}, or returns {@code null} when neither
     * child is present.
     *
     * <p>When both children are present the {@code <transaction>} fields take
     * precedence because {@code <transaction>} is the newer Novi/WhatsApp Pay
     * envelope.
     *
     * @implNote
     * This implementation handles the legacy {@code <pay type="send">} shape by
     * falling back to the message's own {@code recipient} attribute when the
     * {@code <pay>} child lacks a {@code receiver} attribute. The {@code request}
     * and {@code invite} pay types carry no usable payment data and yield
     * {@code null}.
     *
     * @param stanza the parent {@code <message>} stanza
     * @return the parsed payment info, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static MessageReceivePaymentInfo parsePaymentInfo(Stanza stanza) {
        var payNode = stanza.getChild("pay", null);
        var transactionNode = stanza.getChild("transaction", null);

        if (payNode == null && transactionNode == null) {
            return null;
        }

        if (transactionNode != null) {
            var currency = transactionNode.getAttributeAsString("currency", null);
            var amount1000 = transactionNode.getAttributeAsLong("amount_1000", null);
            var status = transactionNode.getAttributeAsString("status", null);
            var ts = transactionNode.getAttributeAsLong("t", null);
            var receiver = transactionNode.getAttributeAsString("receiver", null);
            return new MessageReceivePaymentInfo(
                    false,
                    receiver,
                    currency,
                    amount1000,
                    status,
                    ts
            );
        }

        var payType = payNode.getAttributeAsString("type", null);
        if ("send".equals(payType)) {
            var currency = payNode.getAttributeAsString("currency", null);
            var amount1000 = payNode.getAttributeAsLong("amount_1000", null);
            var receiver = payNode.getAttributeAsString("receiver", null);
            if (receiver == null) {
                receiver = stanza.getAttributeAsString("recipient", null);
            }
            var ts = stanza.getAttributeAsLong("t", null);
            return new MessageReceivePaymentInfo(
                    false,
                    receiver,
                    currency,
                    amount1000,
                    null,
                    ts
            );
        }

        return null;
    }

    /**
     * Parses the {@code <participants>} child into the broadcast contact list.
     *
     * <p>Returns an empty list (not {@code null}) when no participants child is
     * present so the caller can iterate unconditionally.
     *
     * @param stanza the parent {@code <message>} stanza
     * @return the broadcast contact list
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static List<MessageReceiveBroadcastParticipant> parseBroadcastParticipants(Stanza stanza) {
        var participantsNode = stanza.getChild("participants", null);
        if (participantsNode == null) {
            return List.of();
        }

        var toNodes = participantsNode.getChildren("to");
        var participants = new ArrayList<MessageReceiveBroadcastParticipant>(toNodes.size());
        for (var toNode : toNodes) {
            var jid = toNode.getRequiredAttributeAsJid("jid");
            var ephSetting = toNode.getAttributeAsString("eph_setting", null);
            var peerRecipientLid = toNode.getAttributeAsJid("peer_recipient_lid", null);
            var peerRecipientPn = toNode.getAttributeAsJid("peer_recipient_pn", null);
            var peerRecipientUsername = toNode.getAttributeAsString("peer_recipient_username", null);
            var recipientLatestLid = toNode.getAttributeAsJid("recipient_latest_lid", null);
            participants.add(new MessageReceiveBroadcastParticipant(
                    jid,
                    ephSetting,
                    peerRecipientLid,
                    peerRecipientPn,
                    peerRecipientUsername,
                    recipientLatestLid
            ));
        }
        return participants;
    }
}
