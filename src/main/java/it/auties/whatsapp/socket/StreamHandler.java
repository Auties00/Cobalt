package it.auties.whatsapp.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.*;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsSpec;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.call.CallStatus;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.chat.GroupRole;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.ChatMessageInfoBuilder;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.ChatMessageKeyBuilder;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.newsletter.NewsletterMetadata;
import it.auties.whatsapp.model.newsletter.NewsletterReaction;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;
import it.auties.whatsapp.model.request.MessageSendRequest;
import it.auties.whatsapp.model.request.SubscribedNewslettersRequest;
import it.auties.whatsapp.model.response.*;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.util.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static it.auties.whatsapp.util.Specification.Signal.KEY_BUNDLE_TYPE;
import static it.auties.whatsapp.util.Specification.Whatsapp.ACCOUNT_SIGNATURE_HEADER;
import static it.auties.whatsapp.util.Specification.Whatsapp.DEVICE_WEB_SIGNATURE_HEADER;

class StreamHandler {
    private static final int REQUIRED_PRE_KEYS_SIZE = 5;
    private static final int WEB_PRE_KEYS_UPLOAD_CHUNK = 30;
    private static final int MOBILE_PRE_KEYS_UPLOAD_CHUNK = 811;
    private static final int PING_INTERVAL = 30;
    private static final int MAX_ATTEMPTS = 5;
    private static final int DEFAULT_NEWSLETTER_MESSAGES = 100;

    private final SocketHandler socketHandler;
    private final WebVerificationHandler webVerificationHandler;
    private final Map<String, Integer> retries;
    private final AtomicReference<String> lastLinkCodeKey;
    private volatile ScheduledFuture<?> pingFuture;
    private volatile ScheduledFuture<?> mediaConnectionFuture;

    protected StreamHandler(SocketHandler socketHandler, WebVerificationHandler webVerificationHandler) {
        this.socketHandler = socketHandler;
        this.webVerificationHandler = webVerificationHandler;
        this.retries = new ConcurrentHashMap<>();
        this.lastLinkCodeKey = new AtomicReference<>();
    }

    protected void digest(Node node) {
        switch (node.description()) {
            case "ack" -> digestAck(node);
            case "call" -> digestCall(node);
            case "failure" -> digestFailure(node);
            case "ib" -> digestIb(node);
            case "iq" -> digestIq(node);
            case "receipt" -> digestReceipt(node);
            case "stream:error" -> digestError(node);
            case "success" -> digestSuccess(node);
            case "message" -> socketHandler.decodeMessage(node, null, true);
            case "notification" -> digestNotification(node);
            case "presence", "chatstate" -> digestChatState(node);
            case "xmlstreamend" -> digestStreamEnd();
        }
    }

    private void digestStreamEnd() {
        if(socketHandler.state() == SocketState.CONNECTED) {
            socketHandler.disconnect(DisconnectReason.RECONNECTING);
        }
    }

    private void digestFailure(Node node) {
        var reason = node.attributes().getInt("reason");
        switch (reason) {
            case 503, 403 -> socketHandler.disconnect(DisconnectReason.BANNED);
            case 401, 405 -> socketHandler.disconnect(DisconnectReason.LOGGED_OUT);
            default -> socketHandler.disconnect(DisconnectReason.RECONNECTING);
        }
    }

    private void digestChatState(Node node) {
        var chatJid = node.attributes()
                .getRequiredJid("from");
        var participantJid = node.attributes()
                .getOptionalJid("participant")
                .orElse(chatJid);
        updateContactPresence(chatJid, getUpdateType(node), participantJid);
    }

    private ContactStatus getUpdateType(Node node) {
        var metadata = node.findNode();
        var recording = metadata.map(entry -> entry.attributes().getString("media"))
                .filter(entry -> entry.equals("audio"))
                .isPresent();
        if (recording) {
            return ContactStatus.RECORDING;
        }

        return node.attributes()
                .getOptionalString("type")
                .or(() -> metadata.map(Node::description))
                .flatMap(ContactStatus::of)
                .orElse(ContactStatus.AVAILABLE);
    }

    private void updateContactPresence(Jid chatJid, ContactStatus status, Jid contact) {
        socketHandler.store()
                .findChatByJid(chatJid)
                .ifPresent(chat -> socketHandler.onUpdateChatPresence(status, contact, chat));
    }

    private void digestReceipt(Node node) {
        var senderJid = node.attributes()
                .getRequiredJid("from");
        for (var messageId : getReceiptsMessageIds(node)) {
            var message = socketHandler.store().findMessageById(senderJid, messageId);
            if (message.isEmpty()) {
                continue;
            }

            switch (message.get()) {
                case ChatMessageInfo chatMessageInfo -> onChatReceipt(node, senderJid, chatMessageInfo);
                case NewsletterMessageInfo newsletterMessageInfo -> onNewsletterReceipt(node, newsletterMessageInfo);
                default -> throw new IllegalStateException("Unexpected value: " + message.get());
            }
        }

        socketHandler.sendMessageAck(senderJid, node);
    }

    private void onNewsletterReceipt(Node node, NewsletterMessageInfo message) {
        var messageStatus = node.attributes()
                .getOptionalString("type")
                .flatMap(MessageStatus::of);
        if (messageStatus.isEmpty()) {
            return;
        }

        message.setStatus(messageStatus.get());
        socketHandler.onMessageStatus(message);
    }

    private void onChatReceipt(Node node, Jid chatJid, ChatMessageInfo message) {
        var type = node.attributes().getOptionalString("type");
        var status = type.flatMap(MessageStatus::of)
                .orElse(MessageStatus.DELIVERED);
        socketHandler.store().findChatByJid(chatJid).ifPresent(chat -> {
            var newCount = chat.unreadMessagesCount() - 1;
            chat.setUnreadMessagesCount(newCount);
            var participant = node.attributes()
                    .getOptionalJid("participant")
                    .flatMap(socketHandler.store()::findContactByJid)
                    .orElse(null);
            updateReceipt(status, chat, participant, message);
            socketHandler.onMessageStatus(message);
        });

        message.setStatus(status);
        if (Objects.equals(type.orElse(null), "retry")) {
            sendMessageRetry(message);
        }
    }

    private void sendMessageRetry(ChatMessageInfo message) {
        if (!message.fromMe()) {
            return;
        }

        var attempts = retries.getOrDefault(message.id(), 0);
        if (attempts > MAX_ATTEMPTS) {
            return;
        }

        try {
            var all = message.senderJid().device() == 0;
            socketHandler.querySessionsForcefully(message.senderJid());
            message.chat().ifPresent(Chat::clearParticipantsPreKeys);
            var recipients = all ? null : Set.of(message.senderJid());
            var request = new MessageSendRequest.Chat(message, recipients, !all, false, null);
            socketHandler.sendMessage(request);
        } finally {
            retries.put(message.id(), attempts + 1);
        }
    }

    private void updateReceipt(MessageStatus status, Chat chat, Contact participant, ChatMessageInfo message) {
        var container = status == MessageStatus.READ ? message.receipt().readJids() : message.receipt().deliveredJids();
        container.add(participant != null ? participant.jid() : message.senderJid());
        if (chat != null && participant != null && chat.participants().size() != container.size()) {
            return;
        }

        switch (status) {
            case READ -> message.receipt().readTimestampSeconds(Clock.nowSeconds());
            case PLAYED -> message.receipt().playedTimestampSeconds(Clock.nowSeconds());
        }
    }

    private List<String> getReceiptsMessageIds(Node node) {
        var messageIds = Stream.ofNullable(node.findNode("list"))
                .flatMap(Optional::stream)
                .map(list -> list.findNodes("item"))
                .flatMap(Collection::stream)
                .map(item -> item.attributes().getOptionalString("id"))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        messageIds.add(node.attributes().getRequiredString("id"));
        return messageIds;
    }

    private CallStatus getCallStatus(Node node) {
        return switch (node.description()) {
            case "terminate" ->
                    node.attributes().hasValue("reason", "timeout") ? CallStatus.TIMED_OUT : CallStatus.REJECTED;
            case "reject" -> CallStatus.REJECTED;
            case "accept" -> CallStatus.ACCEPTED;
            default -> CallStatus.RINGING;
        };
    }

    private void digestCall(Node node) {
        var from = node.attributes()
                .getRequiredJid("from");
        socketHandler.sendMessageAck(from, node);
        var callNode = node.children().peekFirst();
        if (callNode == null) {
            return;
        }

        var callId = callNode.attributes()
                .getString("call-id");
        var caller = callNode.attributes()
                .getOptionalJid("call-creator")
                .orElse(from);
        var status = getCallStatus(callNode);
        var timestampSeconds = callNode.attributes()
                .getOptionalLong("t")
                .orElseGet(Clock::nowSeconds);
        var isOffline = callNode.attributes().hasKey("offline");
        var hasVideo = callNode.hasNode("video");
        var call = new Call(from, caller, callId, timestampSeconds, hasVideo, status, isOffline);
        socketHandler.store().addCall(call);
        socketHandler.onCall(call);
    }

    private void digestAck(Node node) {
        var ackClass = node.attributes().getString("class");
        switch (ackClass) {
            case "call" -> digestCallAck(node);
            case "message" -> digestMessageAck(node);
        }
    }

    private void digestMessageAck(Node node) {
        var error = node.attributes().getInt("error");
        var messageId = node.id();
        var from = node.attributes()
                .getRequiredJid("from");
        var match = socketHandler.store()
                .findMessageById(from, messageId)
                .orElse(null);
        if (match == null) {
            return;
        }

        if (error != 0) {
            match.setStatus(MessageStatus.ERROR);
            return;
        }

        if (match.status().index() >= MessageStatus.SERVER_ACK.index()) {
            return;
        }

        match.setStatus(MessageStatus.SERVER_ACK);
    }

    private void digestCallAck(Node node) {
        var relayNode = node.findNode("relay").orElse(null);
        if (relayNode == null) {
            return;
        }

        var callCreator = relayNode.attributes()
                .getRequiredJid("call-creator");
        var callId = relayNode.attributes()
                .getString("call-id");
        relayNode.findNodes("participant")
                .stream()
                .map(entry -> entry.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .forEach(to -> sendRelay(callCreator, callId, to));
    }

    private void sendRelay(Jid callCreator, String callId, Jid to) {
        for (var value : Specification.Whatsapp.CALL_RELAY) {
            var te = Node.of("te", Map.of("latency", 33554440), value);
            var relay = Node.of("relaylatency", Map.of("call-creator", callCreator, "call-id", callId), te);
            socketHandler.sendNode(Node.of("call", Map.of("to", to), relay));
        }
    }

    private void digestNotification(Node node) {
        var from = node.attributes()
                .getRequiredJid("from");
        try {
            var type = node.attributes().getString("type", null);
            switch (type) {
                case "w:gp2" -> handleGroupNotification(node);
                case "server_sync" -> handleServerSyncNotification(node);
                case "account_sync" -> handleAccountSyncNotification(node);
                case "encrypt" -> handleEncryptNotification(node);
                case "picture" -> handlePictureNotification(node);
                case "registration" -> handleRegistrationNotification(node);
                case "link_code_companion_reg" -> handleCompanionRegistration(node);
                case "newsletter" -> handleNewsletter(from, node);
                case "mex" -> handleMexNamespace(node);
            }
        } finally {
            socketHandler.sendMessageAck(from, node);
        }
    }

    private void handleNewsletter(Jid newsletterJid, Node node) {
        var newsletter = socketHandler.store()
                .findNewsletterByJid(newsletterJid);
        if (newsletter.isEmpty()) {
            return;
        }

        var liveUpdates = node.findNode("live_updates");
        if (liveUpdates.isEmpty()) {
            return;
        }


        var messages = liveUpdates.get().findNode("messages");
        if (messages.isEmpty()) {
            return;
        }

        for (var messageNode : messages.get().findNodes("message")) {
            var messageId = messageNode.attributes()
                    .getRequiredString("server_id");
            var newsletterMessage = socketHandler.store().findMessageById(newsletter.get(), messageId);
            if (newsletterMessage.isEmpty()) {
                continue;
            }

            messageNode.findNode("reactions")
                    .map(reactions -> reactions.findNodes("reaction"))
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(reaction -> onNewsletterReaction(reaction, newsletterMessage.get()));
        }
    }

    private void onNewsletterReaction(Node reaction, NewsletterMessageInfo newsletterMessage) {
        var reactionCode = reaction.attributes()
                .getRequiredString("code");
        var reactionCount = reaction.attributes()
                .getRequiredInt("count");
        var newReaction = new NewsletterReaction(reactionCode, reactionCount, false);
        newsletterMessage.addReaction(newReaction)
                .ifPresent(oldReaction -> newReaction.setFromMe(oldReaction.fromMe()));
    }

    private void handleMexNamespace(Node node) {
        var update = node.findNode("update")
                .orElse(null);
        if (update == null) {
            return;
        }

        switch (update.attributes().getString("op_name")) {
            case "NotificationNewsletterJoin" -> handleNewsletterJoin(update);
            case "NotificationNewsletterMuteChange" -> handleNewsletterMute(update);
            case "NotificationNewsletterLeave" -> handleNewsletterLeave(update);
            case "NotificationNewsletterUpdate" -> handleNewsletterMetadataUpdate(update);
            case "NotificationNewsletterStateChange" -> handleNewsletterStateUpdate(update);
        }
    }

    private void handleNewsletterStateUpdate(Node update) {
        var updatePayload = update.contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing state update payload"));
        var updateJson = NewsletterStateResponse.ofJson(updatePayload)
                .orElseThrow(() -> new NoSuchElementException("Malformed state update payload"));
        var newsletter = socketHandler.store()
                .findNewsletterByJid(updateJson.jid())
                .orElseThrow(() -> new NoSuchElementException("Missing newsletter"));
        newsletter.setState(updateJson.state());
    }

    private void handleNewsletterMetadataUpdate(Node update) {
        var updatePayload = update.contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing update payload"));
        var updateJson = NewsletterResponse.ofJson(updatePayload)
                .orElseThrow(() -> new NoSuchElementException("Malformed update payload"));
        var newsletter = socketHandler.store()
                .findNewsletterByJid(updateJson.newsletter().jid())
                .orElseThrow(() -> new NoSuchElementException("Missing newsletter"));
        var oldMetadata = newsletter.metadata();
        var updatedMetadata = updateJson.newsletter().metadata();
        var mergedMetadata = new NewsletterMetadata(
                updatedMetadata.name().or(oldMetadata::name),
                updatedMetadata.description().or(oldMetadata::description),
                updatedMetadata.picture().or(oldMetadata::picture),
                updatedMetadata.handle().or(oldMetadata::handle),
                updatedMetadata.settings().or(oldMetadata::settings),
                updatedMetadata.invite().or(oldMetadata::invite),
                updatedMetadata.verification().or(oldMetadata::verification),
                updatedMetadata.creationTimestampSeconds().isPresent() ? updatedMetadata.creationTimestampSeconds() : oldMetadata.creationTimestampSeconds()
        );
        newsletter.setMetadata(mergedMetadata);
    }

    private void handleNewsletterJoin(Node update) {
        var joinPayload = update.contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing join payload"));
        var joinJson = NewsletterResponse.ofJson(joinPayload)
                .orElseThrow(() -> new NoSuchElementException("Malformed join payload"));
        socketHandler.store().addNewsletter(joinJson.newsletter());
        if(!socketHandler.store().historyLength().isZero()) {
            socketHandler.queryNewsletterMessages(joinJson.newsletter().jid(), DEFAULT_NEWSLETTER_MESSAGES);
        }
    }

    private void handleNewsletterMute(Node update) {
        var mutePayload = update.contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing mute payload"));
        var muteJson = NewsletterMuteResponse.ofJson(mutePayload)
                .orElseThrow(() -> new NoSuchElementException("Malformed mute payload"));
        var newsletter = socketHandler.store()
                .findNewsletterByJid(muteJson.jid())
                .orElseThrow(() -> new NoSuchElementException("Missing newsletter"));
        newsletter.viewerMetadata()
                .ifPresent(viewerMetadata -> viewerMetadata.setMute(muteJson.mute()));
    }

    private void handleNewsletterLeave(Node update) {
        var leavePayload = update.contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing leave payload"));
        var leaveJson = NewsletterLeaveResponse.ofJson(leavePayload)
                .orElseThrow(() -> new NoSuchElementException("Malformed leave payload"));
        socketHandler.store().removeNewsletter(leaveJson.jid());
    }

    private void handleCompanionRegistration(Node node) {
        var phoneNumber = getPhoneNumberAsJid();
        var linkCodeCompanionReg = node.findNode("link_code_companion_reg")
                .orElseThrow(() -> new NoSuchElementException("Missing link_code_companion_reg: " + node));
        var ref = linkCodeCompanionReg.findNode("link_code_pairing_ref")
                .flatMap(Node::contentAsBytes)
                .orElseThrow(() -> new IllegalArgumentException("Missing link_code_pairing_ref: " + node));
        var primaryIdentityPublicKey = linkCodeCompanionReg.findNode("primary_identity_pub")
                .flatMap(Node::contentAsBytes)
                .orElseThrow(() -> new IllegalArgumentException("Missing primary_identity_pub: " + node));
        var primaryEphemeralPublicKeyWrapped = linkCodeCompanionReg.findNode("link_code_pairing_wrapped_primary_ephemeral_pub")
                .flatMap(Node::contentAsBytes)
                .orElseThrow(() -> new IllegalArgumentException("Missing link_code_pairing_wrapped_primary_ephemeral_pub: " + node));
        var codePairingPublicKey = decipherLinkPublicKey(primaryEphemeralPublicKeyWrapped);
        var companionSharedKey = Curve25519.sharedKey(codePairingPublicKey, socketHandler.keys().companionKeyPair().privateKey());
        var random = Bytes.random(32);
        var linkCodeSalt = Bytes.random(32);
        var linkCodePairingExpanded = Hkdf.extractAndExpand(companionSharedKey, linkCodeSalt, "link_code_pairing_key_bundle_encryption_key".getBytes(StandardCharsets.UTF_8), 32);
        var encryptPayload = Bytes.concat(socketHandler.keys().identityKeyPair().publicKey(), primaryIdentityPublicKey, random);
        var encryptIv = Bytes.random(12);
        var encrypted = AesGcm.encrypt(encryptIv, encryptPayload, linkCodePairingExpanded);
        var encryptedPayload = Bytes.concat(linkCodeSalt, encryptIv, encrypted);
        var identitySharedKey = Curve25519.sharedKey(primaryIdentityPublicKey, socketHandler.keys().identityKeyPair().privateKey());
        var identityPayload = Bytes.concat(companionSharedKey, identitySharedKey, random);
        var advSecretPublicKey = Hkdf.extractAndExpand(identityPayload, "adv_secret".getBytes(StandardCharsets.UTF_8), 32);
        socketHandler.keys().setCompanionKeyPair(new SignalKeyPair(advSecretPublicKey, socketHandler.keys().companionKeyPair().privateKey()));
        var confirmation = Node.of(
                "link_code_companion_reg",
                Map.of("jid", phoneNumber, "stage", "companion_finish"),
                Node.of("link_code_pairing_wrapped_key_bundle", encryptedPayload),
                Node.of("companion_identity_public", socketHandler.keys().identityKeyPair().publicKey()),
                Node.of("link_code_pairing_ref", ref)
        );
        socketHandler.sendQuery("set", "md", confirmation);
    }

    private byte[] decipherLinkPublicKey(byte[] primaryEphemeralPublicKeyWrapped) {
        try {
            var salt = Arrays.copyOfRange(primaryEphemeralPublicKeyWrapped, 0, 32);
            var secretKey = getSecretPairingKey(lastLinkCodeKey.get(), salt);
            var iv = Arrays.copyOfRange(primaryEphemeralPublicKeyWrapped, 32, 48);
            var payload = Arrays.copyOfRange(primaryEphemeralPublicKeyWrapped, 48, 80);
            var cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return cipher.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot decipher link code pairing key", exception);
        }
    }

    private void handleRegistrationNotification(Node node) {
        var child = node.findNode("wa_old_registration");
        if (child.isEmpty()) {
            return;
        }

        var code = child.get().attributes().getOptionalLong("code");
        if (code.isEmpty()) {
            return;
        }

        socketHandler.onRegistrationCode(code.getAsLong());
    }

    private void handlePictureNotification(Node node) {
        var fromJid = node.attributes()
                .getRequiredJid("from");
        var fromChat = socketHandler.store()
                .findChatByJid(fromJid)
                .orElseGet(() -> socketHandler.store().addNewChat(fromJid));
        var timestamp = node.attributes().getLong("t");
        if (fromChat.isGroup()) {
            addMessageForGroupStubType(fromChat, ChatMessageInfo.StubType.GROUP_CHANGE_ICON, timestamp, node);
            socketHandler.onGroupPictureChanged(fromChat);
            return;
        }
        var fromContact = socketHandler.store().findContactByJid(fromJid).orElseGet(() -> {
            var contact = socketHandler.store().addContact(fromJid);
            socketHandler.onNewContact(contact);
            return contact;
        });
        socketHandler.onContactPictureChanged(fromContact);
    }

    private void handleGroupNotification(Node node) {
        var child = node.findNode();
        if (child.isEmpty()) {
            return;
        }

        var stubType = ChatMessageInfo.StubType.of(child.get().description());
        if (stubType.isEmpty()) {
            return;
        }

        handleGroupStubNotification(node, stubType.get());
    }

    private void handleGroupStubNotification(Node node, ChatMessageInfo.StubType stubType) {
        var timestamp = node.attributes().getLong("t");
        var fromJid = node.attributes()
                .getRequiredJid("from");
        var fromChat = socketHandler.store()
                .findChatByJid(fromJid)
                .orElseGet(() -> socketHandler.store().addNewChat(fromJid));
        addMessageForGroupStubType(fromChat, stubType, timestamp, node);
    }

    private void addMessageForGroupStubType(Chat chat, ChatMessageInfo.StubType stubType, long timestamp, Node metadata) {
        var participantJid = metadata.attributes()
                .getOptionalJid("participant")
                .orElse(null);
        var parameters = getStubTypeParameters(metadata);
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId())
                .chatJid(chat.jid())
                .senderJid(participantJid)
                .build();
        var message = new ChatMessageInfoBuilder()
                .status(MessageStatus.PENDING)
                .timestampSeconds(timestamp)
                .key(key)
                .ignore(true)
                .stubType(stubType)
                .stubParameters(parameters)
                .senderJid(participantJid)
                .build();
        chat.addNewMessage(message);
        socketHandler.onNewMessage(message);
        if (participantJid == null) {
            return;
        }

        handleGroupStubType(chat, stubType, participantJid);
    }

    private void handleGroupStubType(Chat chat, ChatMessageInfo.StubType stubType, Jid participantJid) {
        switch (stubType) {
            case GROUP_PARTICIPANT_ADD -> chat.addParticipant(participantJid, GroupRole.USER);
            case GROUP_PARTICIPANT_REMOVE, GROUP_PARTICIPANT_LEAVE -> chat.removeParticipant(participantJid);
            case GROUP_PARTICIPANT_PROMOTE ->
                    chat.findParticipant(participantJid).ifPresent(participant -> participant.setRole(GroupRole.ADMIN));
            case GROUP_PARTICIPANT_DEMOTE ->
                    chat.findParticipant(participantJid).ifPresent(participant -> participant.setRole(GroupRole.USER));
        }
    }

    private List<String> getStubTypeParameters(Node metadata) {
        try {
            var mapper = new ObjectMapper();
            var attributes = new ArrayList<String>();
            attributes.add(mapper.writeValueAsString(metadata.attributes().toMap()));
            for (var child : metadata.children()) {
                var data = child.attributes();
                if (data.isEmpty()) {
                    continue;
                }

                attributes.add(mapper.writeValueAsString(data.toMap()));
            }

            return Collections.unmodifiableList(attributes);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot encode stub parameters", exception);
        }
    }

    private void handleEncryptNotification(Node node) {
        var chat = node.attributes()
                .getRequiredJid("from");
        if (!chat.isServerJid(JidServer.WHATSAPP)) {
            return;
        }
        var keysSize = node.findNode("count")
                .orElseThrow(() -> new NoSuchElementException("Missing count in notification"))
                .attributes()
                .getLong("value");
        if (keysSize >= REQUIRED_PRE_KEYS_SIZE) {
            return;
        }
        sendPreKeys();
    }

    private void handleAccountSyncNotification(Node node) {
        var child = node.findNode();
        if (child.isEmpty()) {
            return;
        }
        switch (child.get().description()) {
            case "devices" -> handleDevices(child.get());
            case "privacy" -> changeUserPrivacySetting(child.get());
            case "disappearing_mode" -> updateUserDisappearingMode(child.get());
            case "status" -> updateUserAbout(true);
            case "picture" -> updateUserPicture(true);
            case "blocklist" -> updateBlocklist(child.orElse(null));
        }
    }

    private void handleDevices(Node child) {
        var deviceHash = child.attributes().getString("dhash");
        socketHandler.store().setDeviceHash(deviceHash);
        var devices = child.findNodes("device")
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.attributes().getRequiredJid("jid"),
                        entry -> entry.attributes().getInt("key-index"),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        var companionJid = socketHandler.store()
                .jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"))
                .toSimpleJid();
        var companionDevice = devices.remove(companionJid);
        devices.put(companionJid, companionDevice);
        socketHandler.store().setLinkedDevicesKeys(devices);
        socketHandler.onDevices(devices);
        var keyIndexListNode = child.findNode("key-index-list")
                .orElseThrow(() -> new NoSuchElementException("Missing index key node from device sync"));
        var signedKeyIndexBytes = keyIndexListNode.contentAsBytes()
                .orElseThrow(() -> new NoSuchElementException("Missing index key from device sync"));
        socketHandler.keys().setSignedKeyIndex(signedKeyIndexBytes);
        var signedKeyIndexTimestamp = keyIndexListNode.attributes().getLong("ts");
        socketHandler.keys().setSignedKeyIndexTimestamp(signedKeyIndexTimestamp);
    }

    private void updateBlocklist(Node child) {
        child.findNodes("item").forEach(this::updateBlocklistEntry);
    }

    private void updateBlocklistEntry(Node entry) {
        entry.attributes()
                .getOptionalJid("jid")
                .flatMap(socketHandler.store()::findContactByJid)
                .ifPresent(contact -> {
                    contact.setBlocked(Objects.equals(entry.attributes().getString("action"), "block"));
                    socketHandler.onContactBlocked(contact);
                });
    }

    private void changeUserPrivacySetting(Node child) {
        var category = child.findNodes("category");
        category.forEach(entry -> addPrivacySetting(entry, true));
    }

    private void updateUserDisappearingMode(Node child) {
        var timer = ChatEphemeralTimer.of(child.attributes().getInt("duration"));
        socketHandler.store().setNewChatsEphemeralTimer(timer);
    }

    private CompletableFuture<Void> addPrivacySetting(Node node, boolean update) {
        var privacySettingName = node.attributes().getString("name");
        var privacyType = PrivacySettingType.of(privacySettingName);
        if(privacyType.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var privacyValueName = node.attributes().getString("value");
        var privacyValue = PrivacySettingValue.of(privacyValueName);
        if(privacyValue.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (!update) {
            return queryPrivacyExcludedContacts(privacyType.get(), privacyValue.get())
                    .thenAcceptAsync(response -> socketHandler.store().addPrivacySetting(privacyType.get(), new PrivacySettingEntry(privacyType.get(), privacyValue.get(), response)));
        }

        var oldEntry = socketHandler.store().findPrivacySetting(privacyType.get());
        var newValues = getUpdatedBlockedList(node, oldEntry, privacyValue.get());
        var newEntry = new PrivacySettingEntry(privacyType.get(), privacyValue.get(), Collections.unmodifiableList(newValues));
        socketHandler.store().addPrivacySetting(privacyType.get(), newEntry);
        socketHandler.onPrivacySettingChanged(oldEntry, newEntry);
        return CompletableFuture.completedFuture(null);
    }

    private List<Jid> getUpdatedBlockedList(Node node, PrivacySettingEntry privacyEntry, PrivacySettingValue privacyValue) {
        if (privacyValue != PrivacySettingValue.CONTACTS_EXCEPT) {
            return List.of();
        }

        var newValues = new ArrayList<>(privacyEntry.excluded());
        for (var entry : node.findNodes("user")) {
            var jid = entry.attributes()
                    .getRequiredJid("jid");
            if (entry.attributes().hasValue("action", "add")) {
                newValues.add(jid);
                continue;
            }

            newValues.remove(jid);
        }
        return newValues;
    }

    private CompletableFuture<List<Jid>> queryPrivacyExcludedContacts(PrivacySettingType type, PrivacySettingValue value) {
        if (value != PrivacySettingValue.CONTACTS_EXCEPT) {
            return CompletableFuture.completedFuture(List.of());
        }

        return socketHandler.sendQuery("get", "privacy", Node.of("privacy", Node.of("list", Map.of("name", type.data(), "value", value.data()))))
                .thenApplyAsync(this::parsePrivacyExcludedContacts);
    }

    private List<Jid> parsePrivacyExcludedContacts(Node result) {
        return result.findNode("privacy")
                .flatMap(node -> node.findNode("list"))
                .map(node -> node.findNodes("user"))
                .stream()
                .flatMap(Collection::stream)
                .map(user -> user.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    private void handleServerSyncNotification(Node node) {
        if(!socketHandler.keys().initialAppSync()) {
            return;
        }

        var patches = node.findNodes("collection")
                .stream()
                .map(entry -> entry.attributes().getRequiredString("name"))
                .map(PatchType::of)
                .toArray(PatchType[]::new);
        socketHandler.pullPatch(patches);
    }

    private void digestIb(Node node) {
        var dirty = node.findNode("dirty");
        if (dirty.isEmpty()) {
            Validate.isTrue(!node.hasNode("downgrade_webclient"), "Multi device beta is not enabled. Please enable it from Whatsapp");
            return;
        }
        var type = dirty.get().attributes().getString("type");
        if (!Objects.equals(type, "account_sync")) {
            return;
        }
        var timestamp = dirty.get().attributes().getString("timestamp");
        socketHandler.sendQuery("set", "urn:xmpp:whatsapp:dirty",
                Node.of("clean", Map.of("type", type, "timestamp", timestamp)));
    }

    private void digestError(Node node) {
        if(node.hasNode("conflict")) {
            socketHandler.disconnect(DisconnectReason.RECONNECTING);
            return;
        }

        if (node.hasNode("bad-mac")) {
            socketHandler.handleFailure(CRYPTOGRAPHY, new RuntimeException("Detected a bad mac. Unresolved nodes: " + getUnresolvedNodes()));
            return;
        }

        var statusCode = node.attributes().getInt("code");
        switch (statusCode) {
            case 403, 503 -> socketHandler.disconnect(DisconnectReason.BANNED);
            case 500 -> socketHandler.disconnect(DisconnectReason.LOGGED_OUT);
            case 401 -> handleStreamError(node);
            default -> node.children().forEach(error -> socketHandler.store().resolvePendingRequest(error, true));
        }
    }

    private String getUnresolvedNodes() {
        return socketHandler.store()
                .pendingRequests()
                .stream()
                .map(SocketRequest::body)
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));
    }

    private void handleStreamError(Node node) {
        var child = node.children().getFirst();
        var type = child.attributes().getString("type");
        var reason = child.attributes().getString("reason", type);
        if (!Objects.equals(reason, "device_removed")) {
            socketHandler.handleFailure(STREAM, new RuntimeException(reason));
            return;
        }

        socketHandler.disconnect(DisconnectReason.LOGGED_OUT);
    }

    private void digestSuccess(Node node) {
        socketHandler.confirmConnection();
        node.attributes().getOptionalJid("lid")
                .ifPresent(socketHandler.store()::setLid);
        socketHandler.sendQuery("set", "passive", Node.of("active"));
        if (!socketHandler.keys().hasPreKeys()) {
            sendPreKeys();
        }

        var loggedInFuture = queryRequiredInfo()
                .thenComposeAsync(ignored -> initSession())
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(LOGIN, throwable));
        var initialAppSync = socketHandler.keys().initialAppSync();
        if (!initialAppSync) {
            configureApi().thenRunAsync(() -> {
                onRegistration();
                onInitialInfo();
                notifyChatsAndNewsletters(true);
            }).exceptionallyAsync(throwable -> socketHandler.handleFailure(LOGIN, throwable));
        }else {
            loggedInFuture.thenRunAsync(this::onInitialInfo);
        }

        var attributionFuture = socketHandler.store()
                .serializer()
                .attributeStore(socketHandler.store())
                .exceptionallyAsync(exception -> socketHandler.handleFailure(MESSAGE, exception));
        CompletableFuture.allOf(loggedInFuture, attributionFuture)
                .thenRunAsync(() -> notifyChatsAndNewsletters(initialAppSync));
    }

    private CompletableFuture<Void> initSession() {
        return CompletableFuture.allOf(
                scheduleMediaConnectionUpdate(0, null),
                updateSelfPresence(),
                queryInitial2fa(),
                queryInitialAboutPrivacy(),
                queryInitialPrivacySettings(),
                queryInitialDisappearingMode(),
                sendInitialMetadata(),
                queryInitialBlockList(),
                updateUserAbout(false),
                updateUserPicture(false)
        );
    }

    private CompletableFuture<Void> configureApi() {
        return switch (socketHandler.store().clientType()) {
            case WEB -> CompletableFuture.allOf(
                    queryGroups(),
                    queryNewsletters()
            );
            case MOBILE -> CompletableFuture.allOf(
                    acceptTermsOfService(),
                    setPushEndpoint(),
                    setDefaultStatus(),
                    resetMultiDevice(),
                    setupGoogleCrypto(),
                    setupRescueToken(),
                    getInviteSender(),
                    preRegistrationAddRequests()
            );
        };
    }

    private CompletableFuture<?> preRegistrationAddRequests() {
        return socketHandler.sendQuery(JidServer.GROUP.toJid(), "get", "w:g2", Node.of("pre_reg_add_requests"));
    }

    private CompletableFuture<?> getInviteSender() {
        return socketHandler.sendQuery("get", "w:growth", Node.of("invite", Node.of("get_sender")));
    }

    private CompletableFuture<Void> setDefaultStatus() {
        return socketHandler.changeAbout("Hey there! I am using WhatsApp.");
    }

    private CompletableFuture<?> sendInitialMetadata() {
        var wamBinary = "57414d0501010001200b800d086950686f6e652058800f0631362e372e34801109322e32342e352e373510152017502f15a1fc65206928830138790604387b060288eb0a036e616f88a513053231363233186b1818a71c88911e063230483234308879240431372e3018fb2e18ed3318ab3888fb3c09353735323538303733290c147602dce54645287fd941";
        var wamData = new String(HexFormat.of().parseHex(wamBinary))
                .replace("iPhone X", socketHandler.store().device().model().replaceAll("_", " "))
                .replace("2.24.5.75", socketHandler.store().version().toString())
                .getBytes();
        var addNode = Node.of("add", Map.of("t", Clock.nowSeconds()), wamData);
        return socketHandler.sendQuery("set", "w:stats", addNode);
    }

    private CompletableFuture<?> setPushEndpoint() {
        /*
        FIXME: This makes the whole app hang when sending a message for some reason even though it's completed normally
        var configAttributes = Attributes.of()
                .put("background_location", 1)
                .put("call", "Opening.m4r")
                .put("default", "note.m4r")
                .put("groups", "node.m4r")
                .put("id", HexFormat.of().formatHex(Bytes.random(32)))
                .put("lc", "US")
                .put("lg", "en")
                .put("nse_call", 0)
                .put("nse_read", 0)
                .put("nse_ver", 2)
                .put("pkey", Base64.getUrlEncoder().encodeToString(SignalKeyPair.random().publicKey()))
                .put("platform", "apple")
                .put("preview", 1)
                .put("reg_push", 1)
                .put("version", 2)
                .put("voip", "35e178c41d2bd90b8db50c7a2684a38bf802e760cd1f2d7ff803d663412a9320")
                .put("voip_payload_type", 2)
                .toMap();
        return socketHandler.sendQuery("set", "urn:xmpp:whatsapp:push", Node.of("config", configAttributes))
                .thenAccept(result -> socketHandler.keys().setInitialAppSync(true));
         */
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<?> resetMultiDevice() {
        return socketHandler.sendQuery("set", "md", Node.of("remove-companion-device", Map.of("all", true, "reason", "user_initiated")))
                .thenComposeAsync(ignored -> socketHandler.sendQuery("set", "w:sync:app:state", Node.of("delete_all_data")));
    }

    private CompletableFuture<?> setupRescueToken() {
        return socketHandler.sendQuery("set", "w:auth:token", Node.of("token", HexFormat.of().parseHex("20292dbd11e06094feb1908737ca76e6")));
    }

    private CompletableFuture<?> setupGoogleCrypto() {
        var firstCrypto = Node.of("crypto", Map.of("action", "create"), Node.of("google", HexFormat.of().parseHex("7d7ce52cde18aa4854bf522bc72899074e06b60b1bf51864de82e8576b759d12")));
        var secondCrypto = Node.of("crypto", Map.of("action", "create"), Node.of("google", HexFormat.of().parseHex("2f39184f8feb97d57493a69bf5558507472c6bfb633b1c2d369f3409210401c6")));
        return socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", firstCrypto)
                .thenCompose(ignored -> socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", secondCrypto));
    }

    private CompletableFuture<?> acceptTermsOfService() {
        var firstNotice = Node.of("notice", Map.of("id", "20230902"));
        var secondNotice = Node.of("notice", Map.of("id", "20230901"));
        var thirdNotice = Node.of("notice", Map.of("id", "20231027"));
        return socketHandler.sendQuery("get", "tos", Node.of("request", firstNotice, secondNotice, thirdNotice))
                .thenComposeAsync(ignored -> socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.of("accept")))
                .thenComposeAsync(ignored -> socketHandler.sendQuery("set", "tos", Node.of("trackable", Map.of("id", "20601216", "result", 1))))
                .thenComposeAsync(ignored -> socketHandler.sendQuery("set", "tos", Node.of("trackable", Map.of("id", "20900727", "result", 1))));
    }

    private void onRegistration() {
        socketHandler.store().serialize(true);
        socketHandler.keys().serialize(true);
        if(socketHandler.store().clientType() == ClientType.MOBILE) {
            socketHandler.keys().setInitialAppSync(true);
        }
    }

    private void notifyChatsAndNewsletters(boolean notify) {
        if(!notify) {
            return;
        }

        socketHandler.onChats();
        socketHandler.onNewsletters();
    }

    private CompletableFuture<Void> queryNewsletters() {
        var query = new SubscribedNewslettersRequest(new SubscribedNewslettersRequest.Variable());
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6388546374527196"), Json.writeValueAsBytes(query)))
                .thenAcceptAsync(this::parseNewsletters)
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(LOGIN, throwable));
    }

    private void parseNewsletters(Node result) {
        var newslettersPayload = result.findNode("result")
                .flatMap(Node::contentAsString);
        if(newslettersPayload.isEmpty()) {
            return;
        }

        var newslettersJson = SubscribedNewslettersResponse.ofJson(newslettersPayload.get())
                .orElseThrow(() -> new NoSuchElementException("Malformed newsletters payload: " + newslettersPayload));
        onNewsletters(newslettersJson);
    }

    private void onNewsletters(SubscribedNewslettersResponse result) {
        var noMessages = socketHandler.store().historyLength().isZero();
        var data = result.newsletters();
        var futures = noMessages ? null : new CompletableFuture<?>[data.size()];
        for (var index = 0; index < data.size(); index++) {
            var newsletter = data.get(index);
            socketHandler.store().addNewsletter(newsletter);
            if(!noMessages) {
                futures[index] = socketHandler.queryNewsletterMessages(newsletter, DEFAULT_NEWSLETTER_MESSAGES);
            }
        }

        if(noMessages) {
            socketHandler.onNewsletters();
            return;
        }

        CompletableFuture.allOf(futures)
                .thenRun(socketHandler::onNewsletters)
                .exceptionally(throwable -> socketHandler.handleFailure(MESSAGE, throwable));
    }

    private CompletableFuture<Void> queryGroups() {
        return socketHandler.sendQuery(JidServer.GROUP.toJid(), "get", "w:g2", Node.of("participating", Node.of("participants"), Node.of("description")))
                .thenAcceptAsync(this::onGroupsQuery);
    }

    private void onGroupsQuery(Node result) {
        var groups = result.findNode("groups");
        if (groups.isEmpty()) {
            return;
        }

        groups.get()
                .findNodes("group")
                .forEach(socketHandler::handleGroupMetadata);
    }

    private CompletableFuture<Void> setBusinessCertificate() {
        var details = new BusinessVerifiedNameDetailsBuilder()
                .name("")
                .issuer("smb:wa")
                .serial(Math.abs(ThreadLocalRandom.current().nextLong()))
                .build();
        var encodedDetails = BusinessVerifiedNameDetailsSpec.encode(details);
        var certificate = new BusinessVerifiedNameCertificateBuilder()
                .encodedDetails(encodedDetails)
                .signature(Curve25519.sign(socketHandler.keys().identityKeyPair().privateKey(), encodedDetails, true))
                .build();
        return socketHandler.sendQuery("set", "w:biz", Node.of("verified_name", Map.of("v", 2), BusinessVerifiedNameCertificateSpec.encode(certificate))).thenAccept(result -> {
            var verifiedName = result.findNode("verified_name")
                    .map(node -> node.attributes().getString("id"))
                    .orElse("");
            socketHandler.store().setVerifiedName(verifiedName);
        });
    }

    private CompletableFuture<Node> setBusinessProfile() {
        var version = socketHandler.store().properties().getOrDefault("biz_profile_options", "2");
        var body = new ArrayList<Node>();
        socketHandler.store()
                .businessAddress()
                .ifPresent(value -> body.add(Node.of("address", value)));
        socketHandler.store()
                .businessLongitude()
                .ifPresent(value -> body.add(Node.of("longitude", value)));
        socketHandler.store()
                .businessLatitude()
                .ifPresent(value -> body.add(Node.of("latitude", value)));
        socketHandler.store()
                .businessDescription()
                .ifPresent(value -> body.add(Node.of("description", value)));
        socketHandler.store()
                .businessWebsite()
                .ifPresent(value -> body.add(Node.of("website", value)));
        socketHandler.store()
                .businessEmail()
                .ifPresent(value -> body.add(Node.of("email", value)));
        return getBusinessCategoryNode().thenComposeAsync(result -> {
            body.add(Node.of("categories", Node.of("category", Map.of("id", result.id()))));
            return socketHandler.sendQuery("set", "w:biz", Node.of("business_profile", Map.of("v", version), body));
        });
    }

    private CompletableFuture<Node> getBusinessCategoryNode() {
        return socketHandler.store()
                .businessCategory()
                .map(businessCategory -> CompletableFuture.completedFuture(Node.of("category", Map.of("id", businessCategory.id()))))
                .orElseGet(() -> socketHandler.queryBusinessCategories()
                        .thenApplyAsync(entries -> Node.of("category", Map.of("id", entries.getFirst().id()))));
    }

    private void onInitialInfo() {
        socketHandler.keys().setRegistered(true);
        schedulePing();
        socketHandler.onLoggedIn();
        if (!socketHandler.keys().initialAppSync()) {
            return;
        }

        socketHandler.onContacts();
    }

    private CompletableFuture<Void> queryRequiredInfo() {
        return switch (socketHandler.store().clientType()) {
            case WEB -> queryRequiredWebInfo();
            case MOBILE -> queryRequiredMobileInfo();
        };
    }

    private CompletableFuture<Void> queryRequiredMobileInfo() {
        return checkBusinessStatus()
                .thenCompose(ignored -> socketHandler.sendQuery("get", "w", Node.of("props", Map.of("protocol", "2", "hash", ""))))
                .thenAcceptAsync(this::parseProps)
                .thenComposeAsync(ignored -> queryAbProps())
                .thenRunAsync(() -> {
                    socketHandler.sendQuery("get", "w:b", Node.of("lists"))
                            .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
                    socketHandler.sendQuery("set", "urn:xmpp:whatsapp:dirty", Node.of("clean", Map.of("type", "groups")))
                            .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
                    if (socketHandler.store().device().platform().isBusiness()) {
                        socketHandler.sendQuery("get", "fb:thrift_iq", Map.of("smax_id", 42), Node.of("linked_accounts"))
                                .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
                    }
                })
                .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
    }

    private CompletableFuture<Void> queryRequiredWebInfo() {
        return socketHandler.sendQuery("get", "w", Node.of("props"))
                .thenAcceptAsync(this::parseProps)
                .thenComposeAsync(ignored -> queryAbProps())
                .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
    }

    private CompletableFuture<Void> queryAbProps() {
        return socketHandler.sendQuery("get", "abt", Node.of("props", Map.of("protocol", "1")))
                .thenAcceptAsync(result -> { /* TODO: Handle AB props */ });
    }

    private CompletableFuture<Void> checkBusinessStatus() {
        if (!socketHandler.store().device().platform().isBusiness() || socketHandler.keys().businessCertificate()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.allOf(setBusinessCertificate(), setBusinessProfile())
                .thenRunAsync(() -> socketHandler.keys().setBusinessCertificate(true));
    }

    private CompletableFuture<Void> queryInitial2fa() {
        return socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.of("2fa"))
                .thenAcceptAsync(result -> { /* TODO: Handle 2FA */ });
    }

    private CompletableFuture<Void> queryInitialAboutPrivacy() {
        return socketHandler.sendQuery("get", "status", Node.of("privacy"))
                .thenAcceptAsync(result -> { /* TODO: Handle about privacy */ });
    }

    private CompletableFuture<Void> queryInitialPrivacySettings() {
        return socketHandler.sendQuery("get", "privacy", Node.of("privacy"))
                .thenComposeAsync(this::parsePrivacySettings);
    }

    private CompletableFuture<Void> queryInitialDisappearingMode() {
        return socketHandler.sendQuery("get", "disappearing_mode")
                .thenAcceptAsync(result -> { /* TODO: Handle disappearing mode */ });
    }

    private CompletableFuture<Void> queryInitialBlockList() {
        return socketHandler.queryBlockList()
                .thenAcceptAsync(entry -> entry.forEach(this::markBlocked));
    }

    private CompletableFuture<Void> updateSelfPresence() {
        if (!socketHandler.store().automaticPresenceUpdates()) {
            if(!socketHandler.store().online()) {  // Just to be sure
                socketHandler.sendNodeWithNoResponse(Node.of("presence", Map.of("name", socketHandler.store().name(), "type", "unavailable")));
            }
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.sendNodeWithNoResponse(Node.of("presence", Map.of("name", socketHandler.store().name(), "type", "available")))
                .thenRun(this::onPresenceUpdated)
                .exceptionally(exception -> socketHandler.handleFailure(STREAM, exception));
    }

    private void onPresenceUpdated() {
        socketHandler.store().setOnline(true);
        socketHandler.store()
                .jid()
                .flatMap(socketHandler.store()::findContactByJid)
                .ifPresent(entry -> entry.setLastKnownPresence(ContactStatus.AVAILABLE).setLastSeen(ZonedDateTime.now()));
    }

    private CompletableFuture<Void> updateUserAbout(boolean update) {
        var jid = socketHandler.store().jid();
        if (jid.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.queryAbout(jid.get().toSimpleJid())
                .thenAcceptAsync(result -> parseNewAbout(result.orElse(null), update));
    }

    private void parseNewAbout(ContactAboutResponse result, boolean update) {
        if (result == null) {
            return;
        }

        result.about().ifPresent(about -> {
            socketHandler.store().setAbout(about);
            if (!update) {
                return;
            }

            var oldStatus = socketHandler.store().about();
            socketHandler.onUserAboutChanged(about, oldStatus.orElse(null));
        });
    }

    private CompletableFuture<Void> updateUserPicture(boolean update) {
        var jid = socketHandler.store().jid();
        if (jid.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.queryPicture(jid.get().toSimpleJid())
                .thenAcceptAsync(result -> handleUserPictureChange(result.orElse(null), update));
    }

    private void handleUserPictureChange(URI newPicture, boolean update) {
        var oldStatus = socketHandler.store()
                .profilePicture()
                .orElse(null);
        socketHandler.store().setProfilePicture(newPicture);
        if (!update) {
            return;
        }

        socketHandler.onUserPictureChanged(newPicture, oldStatus);
    }

    private void markBlocked(Jid entry) {
        socketHandler.store().findContactByJid(entry).orElseGet(() -> {
            var contact = socketHandler.store().addContact(entry);
            socketHandler.onNewContact(contact);
            return contact;
        }).setBlocked(true);
    }

    private CompletableFuture<Void> parsePrivacySettings(Node result) {
        var privacy = result.findNodes("privacy")
                .stream()
                .flatMap(entry -> entry.children().stream())
                .map(entry -> addPrivacySetting(entry, false))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(privacy);
    }

    private void parseProps(Node result) {
        var properties = result.findNode("props")
                .stream()
                .map(entry -> entry.findNodes("prop"))
                .flatMap(Collection::stream)
                .map(node -> Map.entry(node.attributes().getString("name"), node.attributes().getString("value")))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (first, second) -> second, ConcurrentHashMap::new));
        socketHandler.store().addProperties(properties);
        socketHandler.onMetadata(properties);
    }

    private void schedulePing() {
        if (socketHandler.state() != SocketState.CONNECTED) {
            return;
        }

        this.pingFuture = socketHandler.scheduleAtFixedInterval(() -> {
            socketHandler.sendPing();
            socketHandler.store().serialize(true);
            socketHandler.store().serializer().linkMetadata(socketHandler.store());
            socketHandler.keys().serialize(true);
        }, PING_INTERVAL, PING_INTERVAL);
    }

    private CompletableFuture<Void> scheduleMediaConnectionUpdate(int tries, Throwable error) {
        if (socketHandler.state() != SocketState.CONNECTED) {
            return CompletableFuture.completedFuture(null);
        }

        if (tries >= MAX_ATTEMPTS) {
            socketHandler.store().setMediaConnection(null);
            socketHandler.handleFailure(MEDIA_CONNECTION, error);
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.sendQuery("set", "w:m", Node.of("media_conn"))
                .thenAcceptAsync(this::onMediaConnection)
                .exceptionallyCompose(throwable -> scheduleMediaConnectionUpdate(tries + 1, throwable));
    }

    private void onMediaConnection(Node node) {
        var mediaConnection = node.findNode("media_conn").orElse(node);
        var auth = mediaConnection.attributes().getString("auth");
        var ttl = mediaConnection.attributes().getInt("ttl");
        var maxBuckets = mediaConnection.attributes().getInt("max_buckets");
        var timestamp = System.currentTimeMillis();
        var hosts = mediaConnection.findNodes("host")
                .stream()
                .map(Node::attributes)
                .map(attributes -> attributes.getString("hostname"))
                .toList();
        var result = new MediaConnection(auth, ttl, maxBuckets, timestamp, hosts);
        var alreadyScheduled = socketHandler.store().hasMediaConnection();
        socketHandler.store().setMediaConnection(result);
        if(alreadyScheduled) {
            return;
        }

       this.mediaConnectionFuture = socketHandler.scheduleAtFixedInterval(() -> scheduleMediaConnectionUpdate(0, null), result.ttl(), result.ttl());
    }

    private void digestIq(Node node) {
        var container = node.findNode().orElse(null);
        if (container == null) {
            return;
        }

        switch (container.description()) {
            case "pair-device" -> startPairing(node, container);
            case "pair-success" -> confirmPairing(node, container);
        }
    }

    private void sendPreKeys() {
        var startId = socketHandler.keys().lastPreKeyId() + 1;
        var toUpload = socketHandler.store().clientType() == ClientType.MOBILE ? MOBILE_PRE_KEYS_UPLOAD_CHUNK : WEB_PRE_KEYS_UPLOAD_CHUNK;
        var preKeys = IntStream.range(startId, startId + toUpload)
                .mapToObj(SignalPreKeyPair::random)
                .peek(socketHandler.keys()::addPreKey)
                .map(SignalPreKeyPair::toNode)
                .toList();
        socketHandler.sendQuery(
                "set",
                "encrypt",
                Node.of("registration", socketHandler.keys().encodedRegistrationId()),
                Node.of("type", KEY_BUNDLE_TYPE),
                Node.of("identity", socketHandler.keys().identityKeyPair().publicKey()),
                Node.of("list", preKeys), socketHandler.keys().signedKeyPair().toNode()
        );
    }

    private void startPairing(Node node, Node container) {
        switch (webVerificationHandler) {
            case QrHandler qrHandler -> {
                printQrCode(qrHandler, container);
                sendConfirmNode(node, null);
            }
            case PairingCodeHandler codeHandler -> askPairingCode(codeHandler);
            default -> throw new IllegalArgumentException("Cannot verify account: unknown verification method");
        }
    }

    private void askPairingCode(PairingCodeHandler codeHandler) {
        var code = Bytes.bytesToCrockford(Bytes.random(5));
        var registration = Node.of(
                "link_code_companion_reg",
                Map.of("jid", getPhoneNumberAsJid(), "stage", "companion_hello", "should_show_push_notification", true),
                Node.of("link_code_pairing_wrapped_companion_ephemeral_pub", cipherLinkPublicKey(code)),
                Node.of("companion_server_auth_key_pub", socketHandler.keys().noiseKeyPair().publicKey()),
                Node.of("companion_platform_id", 49),
                Node.of("companion_platform_display", "Chrome (Linux)".getBytes(StandardCharsets.UTF_8)),
                Node.of("link_code_pairing_nonce", 0)
        );
        socketHandler.sendQuery("set", "md", registration).thenAccept(result -> {
            lastLinkCodeKey.set(code);
            codeHandler.accept(code);
        });
    }

    private byte[] cipherLinkPublicKey(String linkCodeKey) {
        try {
            var salt = Bytes.random(32);
            var randomIv = Bytes.random(16);
            var secretKey = getSecretPairingKey(linkCodeKey, salt);
            var cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(randomIv));
            var ciphered = cipher.doFinal(socketHandler.keys().companionKeyPair().publicKey());
            return Bytes.concat(salt, randomIv, ciphered);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot cipher link code pairing key", exception);
        }
    }

    private Jid getPhoneNumberAsJid() {
        return socketHandler.store()
                .phoneNumber()
                .map(PhoneNumber::toJid)
                .orElseThrow(() -> new IllegalArgumentException("Missing phone number while registering via OTP"));
    }

    private SecretKey getSecretPairingKey(String pairingKey, byte[] salt) {
        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            var spec = new PBEKeySpec(pairingKey.toCharArray(), salt, 2 << 16, 256);
            var tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot compute pairing key", exception);
        }
    }

    private void printQrCode(QrHandler qrHandler, Node container) {
        var ref = container.findNode("ref")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing ref"));
        var qr = String.join(
                ",",
                ref,
                Base64.getEncoder().encodeToString(socketHandler.keys().noiseKeyPair().publicKey()),
                Base64.getEncoder().encodeToString(socketHandler.keys().identityKeyPair().publicKey()),
                Base64.getEncoder().encodeToString(socketHandler.keys().companionKeyPair().publicKey()),
                "1"
        );
        qrHandler.accept(qr);
    }

    private void confirmPairing(Node node, Node container) {
        saveCompanion(container);
        var deviceIdentity = container.findNode("device-identity")
                .orElseThrow(() -> new NoSuchElementException("Missing device identity"));
        var advIdentity = SignedDeviceIdentityHMACSpec.decode(deviceIdentity.contentAsBytes().orElseThrow());
        var advSign = Hmac.calculateSha256(advIdentity.details(), socketHandler.keys().companionKeyPair().publicKey());
        if (!Arrays.equals(advIdentity.hmac(), advSign)) {
            socketHandler.handleFailure(LOGIN, new HmacValidationException("adv_sign"));
            return;
        }
        var account = SignedDeviceIdentitySpec.decode(advIdentity.details());
        var message = Bytes.concat(
                ACCOUNT_SIGNATURE_HEADER,
                account.details(),
                socketHandler.keys().identityKeyPair().publicKey()
        );
        if (!Curve25519.verifySignature(account.accountSignatureKey(), message, account.accountSignature())) {
            socketHandler.handleFailure(LOGIN, new HmacValidationException("message_header"));
            return;
        }
        var deviceSignatureMessage = Bytes.concat(
                DEVICE_WEB_SIGNATURE_HEADER,
                account.details(),
                socketHandler.keys().identityKeyPair().publicKey(),
                account.accountSignatureKey()
        );
        var result = new SignedDeviceIdentityBuilder()
                .accountSignature(account.accountSignature())
                .accountSignatureKey(account.accountSignatureKey())
                .details(account.details())
                .deviceSignature(Curve25519.sign(socketHandler.keys().identityKeyPair().privateKey(), deviceSignatureMessage, true))
                .build();
        var keyIndex = DeviceIdentitySpec.decode(result.details()).keyIndex();
        var outgoingDeviceIdentity = SignedDeviceIdentitySpec.encode(new SignedDeviceIdentity(result.details(), null, result.accountSignature(), result.deviceSignature()));
        var devicePairNode = Node.of(
                "pair-device-sign",
                Node.of(
                        "device-identity",
                        Map.of("key-index", keyIndex),
                        outgoingDeviceIdentity
                )
        );
        socketHandler.keys().companionIdentity(result);
        sendConfirmNode(node, devicePairNode);
    }

    private void sendConfirmNode(Node node, Node content) {
        var attributes = Attributes.of()
                .put("id", node.id())
                .put("type", "result")
                .put("to", JidServer.WHATSAPP.toJid())
                .toMap();
        var request = Node.of("iq", attributes, content);
        socketHandler.sendNodeWithNoResponse(request);
    }

    private void saveCompanion(Node container) {
        var node = container.findNode("device")
                .orElseThrow(() -> new NoSuchElementException("Missing device"));
        var companion = node.attributes()
                .getOptionalJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing companion"));
        socketHandler.store().setJid(companion);
        socketHandler.store().setPhoneNumber(PhoneNumber.of(companion.user()));
        socketHandler.addToKnownConnections();
        var me = new Contact(companion.toSimpleJid(), socketHandler.store().name(), null, null, ContactStatus.AVAILABLE, Clock.nowSeconds(), false);
        socketHandler.store().addContact(me);
    }

    protected void dispose() {
        if(mediaConnectionFuture != null && !mediaConnectionFuture.isDone()) {
            mediaConnectionFuture.cancel(true);
        }

        if(pingFuture != null) {
            pingFuture.cancel(true);
        }

        if(mediaConnectionFuture != null) {
            mediaConnectionFuture.cancel(true);
        }

        retries.clear();
        lastLinkCodeKey.set(null);
    }
}
