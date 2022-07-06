package it.auties.whatsapp.socket;

import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.SocketEvent;
import it.auties.whatsapp.binary.Sync;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.GroupMetadata;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.signal.auth.DeviceIdentity;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentity;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentityHMAC;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.util.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static it.auties.whatsapp.model.request.Node.*;
import static java.util.Map.of;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

@RequiredArgsConstructor
@Accessors(fluent = true)
class StreamHandler {
    private static final byte[] MESSAGE_HEADER = {6, 0};
    private static final byte[] SIGNATURE_HEADER = {6, 1};

    private final Socket socket;

    @Getter(AccessLevel.PROTECTED)
    private ScheduledExecutorService pingService;

    protected void digest(@NonNull Node node) {
        switch (node.description()) {
            case "ack" -> digestAck(node);
            case "call" -> digestCall(node);
            case "failure" -> digestFailure(node);
            case "ib" -> digestIb(node);
            case "iq" -> digestIq(node);
            case "receipt" -> digestReceipt(node);
            case "stream:error" -> digestError(node);
            case "success" -> digestSuccess();
            case "message" -> socket.readMessage(node);
            case "notification" -> digestNotification(node);
            case "presence", "chatstate" -> digestChatState(node);
        }
    }

    private void digestFailure(Node node) {
        var location = node.attributes()
                .getOptionalString("location")
                .orElse("unknown");
        var reason = node.attributes()
                .getInt("reason");
        if (reason == 401) {
            socket.errorHandler()
                    .handleFailure(LOGGED_OUT, new RuntimeException(location));
            return;
        }


        socket.errorHandler()
                .handleNodeFailure(new ErroneousNodeException("Stream error", node));
    }

    private void digestChatState(Node node) {
        var chatJid = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing from in chat state update"));
        var participantJid = node.attributes()
                .getJid("participant")
                .orElse(chatJid);
        var updateType = node.attributes()
                .getOptionalString("type")
                .orElseGet(() -> node.children()
                        .getFirst()
                        .description());
        var status = ContactStatus.forValue(updateType);
        socket.store()
                .findContactByJid(participantJid)
                .ifPresent(contact -> updateContactPresence(chatJid, status, contact));
    }

    private void updateContactPresence(ContactJid chatJid, ContactStatus status, Contact contact) {
        contact.lastKnownPresence(status);
        contact.lastSeen(ZonedDateTime.now());
        socket.store()
                .findChatByJid(chatJid)
                .ifPresent(chat -> updateChatPresence(status, contact, chat));
    }

    private void updateChatPresence(ContactStatus status, Contact contact, Chat chat) {
        chat.presences()
                .put(contact, status);
        socket.onUpdateChatPresence(status, contact, chat);
    }

    private void digestReceipt(Node node) {
        var type = node.attributes()
                .getNullableString("type");
        var status = MessageStatus.forValue(type);
        if (status != null) {
            updateMessageStatus(node, status);
        }

        var attributes = Attributes.empty()
                .put("class", "receipt")
                .put("type", type, Objects::nonNull);
        socket.sendMessageAck(node, attributes.map());
    }

    private void updateMessageStatus(Node node, MessageStatus status) {
        node.attributes()
                .getJid("from")
                .flatMap(socket.store()::findChatByJid)
                .ifPresent(chat -> updateMessageStatus(node, status, chat));
    }

    private void updateMessageStatus(Node node, MessageStatus status, Chat chat) {
        var participant = node.attributes()
                .getJid("participant")
                .flatMap(socket.store()::findContactByJid)
                .orElse(null);
        var messageIds = Stream.ofNullable(node.findNode("list"))
                .flatMap(Optional::stream)
                .map(list -> list.findNodes("item"))
                .flatMap(Collection::stream)
                .map(item -> item.attributes()
                        .getOptionalString("id"))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        messageIds.add(node.attributes()
                .getRequiredString("id"));
        messageIds.stream()
                .map(messageId -> socket.store()
                        .findMessageById(chat, messageId))
                .flatMap(Optional::stream)
                .forEach(message -> updateMessageStatus(status, participant, message));
    }

    private void updateMessageStatus(MessageStatus status, Contact participant, MessageInfo message) {
        var chat = message.chat()
                .orElseGet(() -> socket.createChat(message.chatJid()));
        message.status(status);
        if (participant != null) {
            message.individualStatus()
                    .put(participant, status);
        }

        socket.onMessageStatus(status, participant, message, chat);
    }

    private void digestCall(Node node) {
        var call = node.children()
                .peekFirst();
        if (call == null) {
            return;
        }

        socket.sendMessageAck(node, of("class", "call", "type", call.description()));
    }

    private void digestAck(Node node) {
        var clazz = node.attributes()
                .getString("class");
        if (!Objects.equals(clazz, "message")) {
            return;
        }

        var from = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Cannot digest ack: missing from"));
        var receipt = withAttributes("ack", of("class", "receipt", "id", node.id(), "from", from));
        socket.sendWithNoResponse(receipt);
    }

    private void digestNotification(Node node) {
        var type = node.attributes()
                .getString("type", null);
        socket.sendMessageAck(node, of("class", "notification", "type", type));
        handleMessageNotification(node);
        if (!Objects.equals(type, "server_sync")) {
            return;
        }

        var update = node.findNode("collection");
        if (update.isEmpty()) {
            return;
        }

        var patchName = Sync.forName(update.get()
                .attributes()
                .getRequiredString("name"));
        socket.pullPatch(patchName);
    }

    private void handleMessageNotification(Node node) {
        //  693202: (e,t,n)=>{
        //        "use strict";
        //        var a = n(595318);
        //        Object.defineProperty(t, "__esModule", {
        //            value: !0
        //        }),
        //        t.default = void 0;
        //        var r = a(n(145095))
        //          , i = n(535470)
        //          , o = n(88555)
        //          , s = n(438184)
        //          , l = n(408139)
        //          , u = a(n(465212))
        //          , d = n(978820)
        //          , c = a(n(389867))
        //          , f = n(627558)
        //          , h = n(421224);
        //        var p = {
        //            parseWebMessageInfo: function(e, t) {
        //                var n, a, s;
        //                const h = e.key
        //                  , p = this.decodeJid(h.remoteJid)
        //                  , m = (0,
        //                d.getMaybeMeUser)()
        //                  , g = h.fromMe ? p : m
        //                  , _ = h.fromMe ? m : p;
        //                let v = this.decodeJid(h.participant)
        //                  , b = this.decodeJid(e.participant);
        //                (0,
        //                o.isMDBackend)() && (c.default.isGroup(p) || c.default.isStatusV3(p)) && (null == v && (null != e.participant ? v = this.decodeJid(e.participant) : h.fromMe && (v = m)),
        //                b = b || v);
        //                let y, C = "in";
        //                if (m.equals(p) && (C = h.fromMe ? "out" : "in"),
        //                "broadcast" === g)
        //                    return void __LOG__(3)`drop: broadcast`;
        //                try {
        //                    y = new u.default({
        //                        fromMe: (0,
        //                        r.default)(h.fromMe, "key.fromMe"),
        //                        remote: p,
        //                        id: (0,
        //                        r.default)(h.id, "key.id"),
        //                        participant: v
        //                    })
        //                } catch (e) {
        //                    return void __LOG__(3)`drop: cannot create MsgKey: ${e.stack}`
        //                }
        //                let S = !1;
        //                (0,
        //                l.parseWMIReaction)() && (S = e.reactions.some((e=>null != e.text)));
        //                const E = {
        //                    id: y,
        //                    from: _,
        //                    to: g,
        //                    self: C,
        //                    participant: v,
        //                    type: "unknown",
        //                    t: e.messageTimestamp || 0,
        //                    ack: "fresh" === t ? i.ACK.SENT : e.status - 1,
        //                    author: b,
        //                    invis: !!e.ignore,
        //                    star: !!e.starred,
        //                    broadcast: h.fromMe && e.broadcast,
        //                    notifyName: e.pushName || "",
        //                    encFilehash: (0,
        //                    f.decodeBytes)(e.mediaCiphertextSha256),
        //                    shareDuration: e.duration,
        //                    labels: e.labels,
        //                    ephemeralStartTimestamp: e.ephemeralStartTimestamp,
        //                    ephemeralOutOfSync: e.ephemeralOutOfSync,
        //                    bizPrivacyStatus: e.bizPrivacyStatus,
        //                    verifiedBizName: e.verifiedBizName,
        //                    reactions: e.reactions,
        //                    hasReaction: S,
        //                    agentId: e.agentId,
        //                    kicUser: null === (n = e.keepInChat) || void 0 === n ? void 0 : n.deviceJid,
        //                    keepType: null === (a = e.keepInChat) || void 0 === a ? void 0 : a.keepType,
        //                    kicTimestamp: null === (s = e.keepInChat) || void 0 === s ? void 0 : s.serverTimestamp
        //                };
        //                return e.message ? this.parseMsgProto(e.message, E, t, e.paymentInfo, e.finalLiveLocation, e.quotedPaymentInfo) : this.parseMsgStubProto(e, E)
        //            },
        //            parseMsgStubProto: function(e, t) {
        //                if (null == e.messageStubType)
        //                    return;
        //                const n = h.WebMessageInfoStubType;
        //                switch (e.messageStubType) {
        //                case n.REVOKE:
        //                    return t.type = "revoked",
        //                    t.subtype = "sender",
        //                    t;
        //                case n.CIPHERTEXT:
        //                    return t.type = "ciphertext",
        //                    t;
        //                case n.OVERSIZED:
        //                    return t.type = "oversized",
        //                    t;
        //                case n.FUTUREPROOF:
        //                    return t.subtype = "phone",
        //                    t;
        //                default:
        //                    return this.parseMsgStubTemplate(e, t)
        //                }
        //            },
        //            parseMsgStubTemplate: function(e, t) {
        //                if (null == e.messageStubType)
        //                    return;
        //                const n = h.WebMessageInfoStubType;
        //                switch (t.type = "notification_template",
        //                t.templateParams = Array.isArray(e.messageStubParameters) ? e.messageStubParameters.map((t=>e.messageStubType === n.GROUP_CREATE || e.messageStubType === n.GROUP_CHANGE_SUBJECT ? t : this.decodeJid(t))) : void 0,
        //                e.messageStubType) {
        //                case n.NON_VERIFIED_TRANSITION:
        //                    t.subtype = "non_verified_transition";
        //                    break;
        //                case n.UNVERIFIED_TRANSITION:
        //                    t.subtype = "unverified_transition";
        //                    break;
        //                case n.VERIFIED_TRANSITION:
        //                    t.subtype = "verified_transition";
        //                    break;
        //                case n.VERIFIED_LOW_UNKNOWN:
        //                    t.subtype = "verified_low_unknown";
        //                    break;
        //                case n.VERIFIED_HIGH:
        //                    t.subtype = "verified_high";
        //                    break;
        //                case n.VERIFIED_INITIAL_UNKNOWN:
        //                    t.subtype = "verified_initial_unknown";
        //                    break;
        //                case n.VERIFIED_INITIAL_LOW:
        //                    t.subtype = "verified_initial_low";
        //                    break;
        //                case n.VERIFIED_INITIAL_HIGH:
        //                    t.subtype = "verified_initial_high";
        //                    break;
        //                case n.VERIFIED_TRANSITION_ANY_TO_NONE:
        //                    t.subtype = "verified_transition_any_to_none";
        //                    break;
        //                case n.VERIFIED_TRANSITION_ANY_TO_HIGH:
        //                    t.subtype = "verified_transition_any_to_high";
        //                    break;
        //                case n.VERIFIED_TRANSITION_HIGH_TO_LOW:
        //                    t.subtype = "verified_transition_high_to_low";
        //                    break;
        //                case n.VERIFIED_TRANSITION_HIGH_TO_UNKNOWN:
        //                    t.subtype = "verified_transition_high_to_unknown";
        //                    break;
        //                case n.VERIFIED_TRANSITION_UNKNOWN_TO_LOW:
        //                    t.subtype = "verified_transition_unknown_to_low";
        //                    break;
        //                case n.VERIFIED_TRANSITION_LOW_TO_UNKNOWN:
        //                    t.subtype = "verified_transition_low_to_unknown";
        //                    break;
        //                case n.VERIFIED_TRANSITION_NONE_TO_LOW:
        //                    t.subtype = "verified_transition_none_to_low";
        //                    break;
        //                case n.VERIFIED_TRANSITION_NONE_TO_UNKNOWN:
        //                    t.subtype = "verified_transition_none_to_unknown";
        //                    break;
        //                case n.GROUP_CREATE:
        //                    t.type = "gp2",
        //                    t.subtype = "create",
        //                    t.body = t.templateParams[0],
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_DELETE:
        //                    t.type = "gp2",
        //                    t.subtype = "delete",
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_CHANGE_SUBJECT:
        //                    t.type = "gp2",
        //                    t.subtype = "subject",
        //                    t.body = t.templateParams[0],
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_CHANGE_ICON:
        //                    t.type = "gp2",
        //                    t.subtype = "picture",
        //                    t.body = t.templateParams[0],
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_CHANGE_INVITE_LINK:
        //                    t.type = "gp2",
        //                    t.subtype = "revoke_invite";
        //                    break;
        //                case n.GROUP_CHANGE_DESCRIPTION:
        //                    t.type = "gp2",
        //                    t.subtype = "description";
        //                    break;
        //                case n.GROUP_CHANGE_RESTRICT:
        //                    t.type = "gp2",
        //                    t.subtype = "restrict",
        //                    t.body = t.templateParams[0];
        //                    break;
        //                case n.GROUP_CHANGE_ANNOUNCE:
        //                    t.type = "gp2",
        //                    t.subtype = "announce",
        //                    t.body = t.templateParams[0];
        //                    break;
        //                case n.GROUP_CHANGE_NO_FREQUENTLY_FORWARDED:
        //                    t.type = "gp2",
        //                    t.subtype = "no_frequently_forwarded",
        //                    t.body = t.templateParams[0];
        //                    break;
        //                case n.GROUP_ANNOUNCE_MODE_MESSAGE_BOUNCE:
        //                    t.type = "gp2",
        //                    t.subtype = "announce_msg_bounce",
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_PARTICIPANT_ADD:
        //                    t.type = "gp2",
        //                    t.subtype = "add",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_PARTICIPANT_REMOVE:
        //                    t.type = "gp2",
        //                    t.subtype = "remove",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_PARTICIPANT_PROMOTE:
        //                    t.type = "gp2",
        //                    t.subtype = "promote",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_PARTICIPANT_DEMOTE:
        //                    t.type = "gp2",
        //                    t.subtype = "demote",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_PARTICIPANT_INVITE:
        //                    t.type = "gp2",
        //                    t.subtype = "invite",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_PARTICIPANT_LEAVE:
        //                    t.type = "gp2",
        //                    t.subtype = "leave",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_PARTICIPANT_CHANGE_NUMBER:
        //                    t.type = "gp2",
        //                    t.subtype = "modify",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_V4_ADD_INVITE_SENT:
        //                    t.type = "gp2",
        //                    t.subtype = "v4_add_invite_sent",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_PARTICIPANT_ADD_REQUEST_JOIN:
        //                    t.type = "gp2",
        //                    t.subtype = "v4_add_invite_join",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_INVITE_LINK_GROWTH_LOCKED:
        //                    t.type = "gp2",
        //                    t.subtype = "true" === t.templateParams[0] ? "growth_locked" : "growth_unlocked",
        //                    t.body = "invite",
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GROUP_PARTICIPANT_LINKED_GROUP_JOIN:
        //                    t.type = "gp2",
        //                    t.subtype = "linked_group_join",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.BROADCAST_CREATE:
        //                    t.type = "broadcast_notification",
        //                    t.subtype = "create",
        //                    t.body = t.templateParams[0] || "0",
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.BROADCAST_ADD:
        //                    t.type = "broadcast_notification",
        //                    t.subtype = "add",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.BROADCAST_REMOVE:
        //                    t.type = "broadcast_notification",
        //                    t.subtype = "remove",
        //                    t.recipients = t.templateParams,
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.GENERIC_NOTIFICATION:
        //                    t.type = "notification",
        //                    t.body = t.templateParams[0],
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.E2E_IDENTITY_CHANGED:
        //                    t.type = "e2e_notification",
        //                    t.subtype = "identity",
        //                    t.body = t.templateParams[0]instanceof c.default ? t.templateParams[0].toString() : t.templateParams[0],
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.E2E_IDENTITY_UNAVAILABLE:
        //                    t.type = "e2e_notification",
        //                    t.subtype = "e2e_identity_unavailable";
        //                    break;
        //                case n.E2E_DEVICE_CHANGED:
        //                    t.type = "e2e_notification",
        //                    t.subtype = "device",
        //                    t.body = t.templateParams[0]instanceof c.default ? t.templateParams[0].toString() : t.templateParams[0],
        //                    t.devicesAdded = parseInt(t.templateParams[1], 10),
        //                    t.devicesRemoved = parseInt(t.templateParams[2], 10),
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.E2E_ENCRYPTED:
        //                    t.type = "e2e_notification",
        //                    t.subtype = "encrypt";
        //                    break;
        //                case n.E2E_ENCRYPTED_NOW:
        //                    t.type = "e2e_notification",
        //                    t.subtype = "encrypt_now";
        //                    break;
        //                case n.CALL_MISSED_VOICE:
        //                    t.type = "call_log",
        //                    t.subtype = "miss";
        //                    break;
        //                case n.CALL_MISSED_VIDEO:
        //                    t.type = "call_log",
        //                    t.subtype = "miss_video";
        //                    break;
        //                case n.CALL_MISSED_GROUP_VOICE:
        //                    t.type = "call_log",
        //                    t.subtype = "miss_group";
        //                    break;
        //                case n.CALL_MISSED_GROUP_VIDEO:
        //                    t.type = "call_log",
        //                    t.subtype = "miss_group_video";
        //                    break;
        //                case n.INDIVIDUAL_CHANGE_NUMBER:
        //                    t.subtype = "change_number";
        //                    break;
        //                case n.CHANGE_EPHEMERAL_SETTING:
        //                    t.type = "gp2",
        //                    t.subtype = "ephemeral",
        //                    t.author = t.templateParams[1];
        //                    break;
        //                case n.PAYMENT_CIPHERTEXT:
        //                    t.type = "payment",
        //                    t.subtype = "ciphertext",
        //                    this.callFeatureFlagFun(s.LegacyPhoneFeatures.F.PAYMENTS, "parsePaymentInfo", t, e.paymentInfo);
        //                    break;
        //                case n.PAYMENT_FUTUREPROOF:
        //                    t.type = "payment",
        //                    t.subtype = "futureproof",
        //                    this.callFeatureFlagFun(s.LegacyPhoneFeatures.F.PAYMENTS, "parsePaymentInfo", t, e.paymentInfo);
        //                    break;
        //                case n.PAYMENT_ACTION_REQUEST_CANCELLED:
        //                    t.subtype = "payment_transaction_request_cancelled";
        //                    break;
        //                case n.PAYMENT_TRANSACTION_STATUS_UPDATE_FAILED:
        //                    t.subtype = "payment_transaction_status_update_failed";
        //                    break;
        //                case n.PAYMENT_TRANSACTION_STATUS_UPDATE_REFUNDED:
        //                    t.subtype = "payment_transaction_status_update_refunded";
        //                    break;
        //                case n.PAYMENT_TRANSACTION_STATUS_UPDATE_REFUND_FAILED:
        //                    t.subtype = "payment_transaction_status_update_refund_failed";
        //                    break;
        //                case n.PAYMENT_TRANSACTION_STATUS_RECEIVER_PENDING_SETUP:
        //                    t.subtype = "payment_transaction_status_receiver_pending_setup";
        //                    break;
        //                case n.PAYMENT_TRANSACTION_STATUS_RECEIVER_SUCCESS_AFTER_HICCUP:
        //                    t.subtype = "payment_transaction_status_receiver_success_after_hiccup";
        //                    break;
        //                case n.PAYMENT_ACTION_ACCOUNT_SETUP_REMINDER:
        //                    t.subtype = "payment_action_account_setup_reminder";
        //                    break;
        //                case n.PAYMENT_ACTION_SEND_PAYMENT_REMINDER:
        //                    t.subtype = "payment_action_send_payment_reminder";
        //                    break;
        //                case n.PAYMENT_ACTION_SEND_PAYMENT_INVITATION:
        //                    t.subtype = "payment_action_send_payment_invitation";
        //                    break;
        //                case n.PAYMENT_ACTION_REQUEST_DECLINED:
        //                    t.subtype = "payment_action_request_declined";
        //                    break;
        //                case n.PAYMENT_ACTION_REQUEST_EXPIRED:
        //                    t.subtype = "payment_action_request_expired";
        //                    break;
        //                case n.BIZ_VERIFIED_TRANSITION_TOP_TO_BOTTOM:
        //                    t.subtype = "biz_verified_transition_top_to_bottom";
        //                    break;
        //                case n.BIZ_VERIFIED_TRANSITION_BOTTOM_TO_TOP:
        //                    t.subtype = "biz_verified_transition_bottom_to_top";
        //                    break;
        //                case n.BIZ_INTRO_TOP:
        //                    t.subtype = "biz_intro_top";
        //                    break;
        //                case n.BIZ_INTRO_BOTTOM:
        //                    t.subtype = "biz_intro_bottom";
        //                    break;
        //                case n.BIZ_NAME_CHANGE:
        //                    t.subtype = "biz_name_change";
        //                    break;
        //                case n.BIZ_MOVE_TO_CONSUMER_APP:
        //                    t.subtype = "biz_move_to_consumer_app";
        //                    break;
        //                case n.BIZ_TWO_TIER_MIGRATION_TOP:
        //                    t.subtype = "biz_two_tier_migration_top";
        //                    break;
        //                case n.BIZ_TWO_TIER_MIGRATION_BOTTOM:
        //                    t.subtype = "biz_two_tier_migration_bottom";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_TO_BSP_PREMISE:
        //                    t.subtype = "blue_msg_bsp_fb_to_bsp_premise";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_TO_SELF_FB:
        //                    t.subtype = "blue_msg_bsp_fb_to_self_fb";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_TO_SELF_PREMISE:
        //                    t.subtype = "blue_msg_bsp_fb_to_self_premise";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_UNVERIFIED:
        //                    t.subtype = "blue_msg_bsp_fb_unverified";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED:
        //                    t.subtype = "blue_msg_bsp_fb_unverified_to_bsp_premise_verified";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_FB_VERIFIED:
        //                    t.subtype = "blue_msg_bsp_fb_unverified_to_self_fb_verified";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED:
        //                    t.subtype = "blue_msg_bsp_fb_unverified_to_self_premise_verified";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_VERIFIED:
        //                    t.subtype = "blue_msg_bsp_fb_verified";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_bsp_fb_verified_to_bsp_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_FB_UNVERIFIED:
        //                    t.subtype = "blue_msg_bsp_fb_verified_to_self_fb_unverified";
        //                    break;
        //                case n.BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_bsp_fb_verified_to_self_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_BSP_PREMISE_TO_SELF_PREMISE:
        //                    t.subtype = "blue_msg_bsp_premise_to_self_premise";
        //                    break;
        //                case n.BLUE_MSG_BSP_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_bsp_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_BSP_PREMISE_UNVERIFIED_TO_SELF_PREMISE_VERIFIED:
        //                    t.subtype = "blue_msg_bsp_premise_unverified_to_self_premise_verified";
        //                    break;
        //                case n.BLUE_MSG_BSP_PREMISE_VERIFIED:
        //                    t.subtype = "blue_msg_bsp_premise_verified";
        //                    break;
        //                case n.BLUE_MSG_BSP_PREMISE_VERIFIED_TO_SELF_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_bsp_premise_verified_to_self_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_CONSUMER_TO_BSP_FB_UNVERIFIED:
        //                    t.subtype = "blue_msg_consumer_to_bsp_fb_unverified";
        //                    break;
        //                case n.BLUE_MSG_CONSUMER_TO_BSP_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_consumer_to_bsp_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_CONSUMER_TO_SELF_FB_UNVERIFIED:
        //                    t.subtype = "blue_msg_consumer_to_self_fb_unverified";
        //                    break;
        //                case n.BLUE_MSG_CONSUMER_TO_SELF_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_consumer_to_self_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_SELF_FB_TO_BSP_PREMISE:
        //                    t.subtype = "blue_msg_self_fb_to_bsp_premise";
        //                    break;
        //                case n.BLUE_MSG_SELF_FB_TO_SELF_PREMISE:
        //                    t.subtype = "blue_msg_self_fb_to_self_premise";
        //                    break;
        //                case n.BLUE_MSG_SELF_FB_UNVERIFIED:
        //                    t.subtype = "blue_msg_self_fb_unverified";
        //                    break;
        //                case n.BLUE_MSG_SELF_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED:
        //                    t.subtype = "blue_msg_self_fb_unverified_to_bsp_premise_verified";
        //                    break;
        //                case n.BLUE_MSG_SELF_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED:
        //                    t.subtype = "blue_msg_self_fb_unverified_to_self_premise_verified";
        //                    break;
        //                case n.BLUE_MSG_SELF_FB_VERIFIED:
        //                    t.subtype = "blue_msg_self_fb_verified";
        //                    break;
        //                case n.BLUE_MSG_SELF_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_self_fb_verified_to_bsp_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_SELF_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_self_fb_verified_to_self_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_SELF_PREMISE_TO_BSP_PREMISE:
        //                    t.subtype = "blue_msg_self_premise_to_bsp_premise";
        //                    break;
        //                case n.BLUE_MSG_SELF_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_self_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_SELF_PREMISE_VERIFIED:
        //                    t.subtype = "blue_msg_self_premise_verified";
        //                    break;
        //                case n.BLUE_MSG_TO_BSP_FB:
        //                    t.subtype = "blue_msg_to_bsp_fb";
        //                    break;
        //                case n.BLUE_MSG_TO_CONSUMER:
        //                    t.subtype = "blue_msg_to_consumer";
        //                    break;
        //                case n.BLUE_MSG_TO_SELF_FB:
        //                    t.subtype = "blue_msg_to_self_fb";
        //                    break;
        //                case n.BLUE_MSG_UNVERIFIED_TO_BSP_FB_VERIFIED:
        //                    t.subtype = "blue_msg_unverified_to_bsp_fb_verified";
        //                    break;
        //                case n.BLUE_MSG_UNVERIFIED_TO_BSP_PREMISE_VERIFIED:
        //                    t.subtype = "blue_msg_unverified_to_bsp_premise_verified";
        //                    break;
        //                case n.BLUE_MSG_UNVERIFIED_TO_SELF_FB_VERIFIED:
        //                    t.subtype = "blue_msg_unverified_to_self_fb_verified";
        //                    break;
        //                case n.BLUE_MSG_UNVERIFIED_TO_VERIFIED:
        //                    t.subtype = "blue_msg_unverified_to_verified";
        //                    break;
        //                case n.BLUE_MSG_VERIFIED_TO_BSP_FB_UNVERIFIED:
        //                    t.subtype = "blue_msg_verified_to_bsp_fb_unverified";
        //                    break;
        //                case n.BLUE_MSG_VERIFIED_TO_BSP_PREMISE_UNVERIFIED:
        //                    t.subtype = "blue_msg_verified_to_bsp_premise_unverified";
        //                    break;
        //                case n.BLUE_MSG_VERIFIED_TO_SELF_FB_UNVERIFIED:
        //                    t.subtype = "blue_msg_verified_to_self_fb_unverified";
        //                    break;
        //                case n.BLUE_MSG_VERIFIED_TO_UNVERIFIED:
        //                    t.subtype = "blue_msg_verified_to_unverified";
        //                    break;
        //                case n.BIZ_PRIVACY_MODE_INIT_FB:
        //                    t.subtype = "biz_privacy_mode_init_fb";
        //                    break;
        //                case n.BIZ_PRIVACY_MODE_INIT_BSP:
        //                    t.subtype = "biz_privacy_mode_init_bsp";
        //                    break;
        //                case n.BIZ_PRIVACY_MODE_TO_FB:
        //                    t.subtype = "biz_privacy_mode_to_fb";
        //                    break;
        //                case n.BIZ_PRIVACY_MODE_TO_BSP:
        //                    t.subtype = "biz_privacy_mode_to_bsp";
        //                    break;
        //                case n.DISAPPEARING_MODE:
        //                    t.subtype = "disappearing_mode";
        //                    break;
        //                case n.BLOCK_CONTACT:
        //                    t.subtype = "block_contact";
        //                    break;
        //                case n.ADMIN_REVOKE:
        //                    (0,
        //                    l.adminRevokeHistorySyncConsumerEnabled)() && (t.type = "revoked",
        //                    t.subtype = "admin",
        //                    t.revokeSender = t.templateParams[1]instanceof c.default ? t.templateParams[1] : void 0,
        //                    t.revokeSender || __LOG__(2)`ProtocolParser: admin revoke came without admin JID`),
        //                    t.templateParams = void 0;
        //                    break;
        //                case n.COMMUNITY_LINK_PARENT_GROUP:
        //                    t.type = "gp2",
        //                    t.subtype = "parent_group_link";
        //                    break;
        //                case n.COMMUNITY_LINK_SIBLING_GROUP:
        //                    t.type = "gp2",
        //                    t.subtype = "sibling_group_link";
        //                    break;
        //                case n.COMMUNITY_LINK_SUB_GROUP:
        //                    t.type = "gp2",
        //                    t.subtype = "sub_group_link";
        //                    break;
        //                case n.COMMUNITY_CREATE:
        //                    t.type = "gp2",
        //                    t.subtype = "community_create";
        //                    break;
        //                case n.COMMUNITY_UNLINK_PARENT_GROUP:
        //                    t.type = "gp2",
        //                    t.subtype = "parent_group_unlink";
        //                    break;
        //                case n.COMMUNITY_UNLINK_SIBLING_GROUP:
        //                    t.type = "gp2",
        //                    t.subtype = "sibling_group_unlink";
        //                    break;
        //                case n.COMMUNITY_UNLINK_SUB_GROUP:
        //                    t.type = "gp2",
        //                    t.subtype = "sub_group_unlink";
        //                    break;
        //                default:
        //                    t.templateParams = void 0
        //                }
        //                return t
        //            }
        //        };
        //        t.default = p
        //    }
    }

    private void digestIb(Node node) {
        var dirty = node.findNode("dirty");
        if (dirty.isEmpty()) {
            Validate.isTrue(!node.hasNode("downgrade_webclient"),
                    "Multi device beta is not enabled. Please enable it from Whatsapp");
            return;
        }

        var type = dirty.get()
                .attributes()
                .getString("type");
        if (!Objects.equals(type, "account_sync")) {
            return;
        }

        var timestamp = dirty.get()
                .attributes()
                .getString("timestamp");
        socket.sendQuery("set", "urn:xmpp:whatsapp:dirty",
                withAttributes("clean", of("type", type, "timestamp", timestamp)));
    }

    private void digestError(Node node) {
        var statusCode = node.attributes()
                .getInt("code");
        switch (statusCode) {
            case 515 -> socket.reconnect();
            case 401 -> handleStreamError(node);
            default -> node.children()
                    .forEach(error -> socket.store()
                            .resolvePendingRequest(error, true));
        }
    }

    private void handleStreamError(Node node) {
        var child = node.children()
                .getFirst();
        var type = child.attributes()
                .getString("type");
        var reason = child.attributes()
                .getString("reason", type);
        socket.errorHandler()
                .handleFailure(Objects.equals(reason, "device_removed") ?
                        LOGGED_OUT :
                        STREAM, new RuntimeException(reason));
    }

    private void digestSuccess() {
        confirmConnection();
        sendPreKeys();
        createPingTask();
        createMediaConnection();
        sendStatusUpdate();
        socket.onLoggedIn();
        if (!socket.store()
                .hasSnapshot()) {
            return;
        }

        socket.store()
                .invokeListeners(Listener::onChats);
        socket.store()
                .invokeListeners(Listener::onContacts);
        socket.pullPatches();
    }

    private void createPingTask() {
        if (pingService != null && !pingService.isShutdown()) {
            return;
        }

        this.pingService = newSingleThreadScheduledExecutor();
        pingService.scheduleAtFixedRate(this::sendPing, 20L, 20L, TimeUnit.SECONDS);
    }

    private void sendStatusUpdate() {
        var presence = withAttributes("presence", of("type", "available"));
        socket.sendWithNoResponse(presence);
        socket.sendQuery("get", "blocklist");
        socket.sendQuery("get", "privacy", with("privacy"));
        socket.sendQuery("get", "abt", withAttributes("props", of("protocol", "1")));
        socket.sendQuery("get", "w", with("props"))
                .thenAcceptAsync(this::parseProps);
    }

    private void parseProps(Node result) {
        var properties = result.findNode("props")
                .orElseThrow(() -> new NoSuchElementException("Missing props"))
                .findNodes("prop")
                .stream()
                .map(node -> Map.entry(node.attributes()
                        .getString("name"), node.attributes()
                        .getString("value")))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        socket.onMetadata(properties);
    }

    private void sendPing() {
        if (!socket.state()
                .isConnected()) {
            pingService.shutdownNow();
            return;
        }

        socket.sendQuery("get", "w:p", with("ping"));
        socket.onSocketEvent(SocketEvent.PING);
    }

    @SneakyThrows
    private void createMediaConnection() {
        if (!socket.state()
                .isConnected()) {
            return;
        }

        socket.store()
                .mediaConnectionLock()
                .acquire();
        socket.sendQuery("set", "w:m", with("media_conn"))
                .thenApplyAsync(MediaConnection::of)
                .thenApplyAsync(socket.store()::mediaConnection)
                .thenRunAsync(socket.store()
                        .mediaConnectionLock()::release)
                .exceptionallyAsync(this::handleMediaConnectionError)
                .thenRunAsync(() -> runAsyncDelayed(this::createMediaConnection, socket.store()
                        .mediaConnection()
                        .ttl()));
    }

    private void runAsyncDelayed(Runnable runnable, int seconds) {
        var mediaService = CompletableFuture.delayedExecutor(seconds, TimeUnit.SECONDS);
        CompletableFuture.runAsync(runnable, mediaService);
    }

    private <T> T handleMediaConnectionError(Throwable throwable) {
        socket.store()
                .mediaConnectionLock()
                .release();
        return socket.errorHandler()
                .handleFailure(MEDIA_CONNECTION, throwable);
    }

    private void digestIq(Node node) {
        var container = node.children()
                .peekFirst();
        if (container == null) {
            return;
        }

        if (container.description()
                .equals("pair-device")) {
            generateQrCode(node, container);
            return;
        }

        if (!container.description()
                .equals("pair-success")) {
            return;
        }

        confirmQrCode(node, container);
    }

    private void confirmConnection() {
        socket.sendQuery("set", "passive", with("active"));
    }

    private void sendPreKeys() {
        if (socket.keys()
                .hasPreKeys()) {
            return;
        }

        var preKeys = IntStream.range(1, 31)
                .mapToObj(SignalPreKeyPair::random)
                .peek(socket.keys()::addPreKey)
                .map(SignalPreKeyPair::toNode)
                .toList();
        socket.sendQuery("set", "encrypt", with("registration", BytesHelper.intToBytes(socket.keys()
                .id(), 4)), with("type", SignalSpecification.KEY_BUNDLE_TYPE), with("identity", socket.keys()
                .identityKeyPair()
                .publicKey()), withChildren("list", preKeys), socket.keys()
                .signedKeyPair()
                .toNode());
    }

    private void generateQrCode(Node node, Node container) {
        printQrCode(container);
        sendConfirmNode(node, null);
    }

    private void printQrCode(Node container) {
        var ref = container.findNode("ref")
                .orElseThrow(() -> new NoSuchElementException("Missing ref"));
        var qr = "%s,%s,%s,%s".formatted(new String(ref.bytes(), StandardCharsets.UTF_8), Bytes.of(socket.keys()
                        .noiseKeyPair()
                        .publicKey())
                .toBase64(), Bytes.of(socket.keys()
                        .identityKeyPair()
                        .publicKey())
                .toBase64(), Bytes.of(socket.keys()
                        .companionKey())
                .toBase64());
        socket.options()
                .qrHandler()
                .accept(qr);
    }

    @SneakyThrows
    private void confirmQrCode(Node node, Node container) {
        saveCompanion(container);

        var deviceIdentity = container.findNode("device-identity")
                .orElseThrow(() -> new NoSuchElementException("Missing device identity"));
        var advIdentity = JacksonProvider.PROTOBUF.readMessage(deviceIdentity.bytes(), SignedDeviceIdentityHMAC.class);
        var advSign = Hmac.calculateSha256(advIdentity.details(), socket.keys()
                .companionKey());
        if (!Arrays.equals(advIdentity.hmac(), advSign)) {
            socket.errorHandler()
                    .handleFailure(LOGIN, new HmacValidationException("adv_sign"));
            return;
        }

        var account = JacksonProvider.PROTOBUF.readMessage(advIdentity.details(), SignedDeviceIdentity.class);
        var message = Bytes.of(MESSAGE_HEADER)
                .append(account.details())
                .append(socket.keys()
                        .identityKeyPair()
                        .publicKey())
                .toByteArray();
        if (!Curve25519.verifySignature(account.accountSignatureKey(), message, account.accountSignature())) {
            socket.errorHandler()
                    .handleFailure(LOGIN, new HmacValidationException("message_header"));
            return;
        }

        var deviceSignatureMessage = Bytes.of(SIGNATURE_HEADER)
                .append(account.details())
                .append(socket.keys()
                        .identityKeyPair()
                        .publicKey())
                .append(account.accountSignatureKey())
                .toByteArray();
        account.deviceSignature(Curve25519.sign(socket.keys()
                .identityKeyPair()
                .privateKey(), deviceSignatureMessage, true));

        var keyIndex = JacksonProvider.PROTOBUF.readMessage(account.details(), DeviceIdentity.class)
                .keyIndex();
        var devicePairNode = withChildren("pair-device-sign", with("device-identity", of("key-index", keyIndex),
                JacksonProvider.PROTOBUF.writeValueAsBytes(account.withoutKey())));

        socket.keys()
                .companionIdentity(account);
        sendConfirmNode(node, devicePairNode);
    }

    private void sendConfirmNode(Node node, Node content) {
        var attributes = Attributes.empty()
                .put("id", node.id())
                .put("type", "result")
                .put("to", ContactJid.WHATSAPP)
                .map();
        var request = withChildren("iq", attributes, content);
        socket.sendWithNoResponse(request);
    }

    private void saveCompanion(Node container) {
        var node = container.findNode("device")
                .orElseThrow(() -> new NoSuchElementException("Missing device"));
        var companion = node.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing companion"));
        socket.keys()
                .companion(companion);
    }
}
