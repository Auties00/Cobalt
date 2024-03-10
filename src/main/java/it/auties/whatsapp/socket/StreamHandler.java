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
import java.util.concurrent.*;
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
    private static final int MEDIA_CONNECTION_DEFAULT_INTERVAL = 60;
    private static final int MAX_ATTEMPTS = 5;
    private static final int DEFAULT_NEWSLETTER_MESSAGES = 100;

    private final SocketHandler socketHandler;
    private final WebVerificationHandler webVerificationHandler;
    private final Map<String, Integer> retries;
    private final AtomicReference<String> lastLinkCodeKey;
    private volatile CompletableFuture<Void> mediaConnectionFuture;
    private volatile CompletableFuture<Void> pingFuture;

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
        if (reason == 401 || reason == 403 || reason == 405) {
            socketHandler.disconnect(DisconnectReason.LOGGED_OUT);
            return;
        }
        socketHandler.disconnect(DisconnectReason.RECONNECTING);
    }

    private void digestChatState(Node node) {
        CompletableFuture.runAsync(() -> {
            var chatJid = node.attributes()
                    .getRequiredJid("from");
            var participantJid = node.attributes()
                    .getOptionalJid("participant")
                    .orElse(chatJid);
            updateContactPresence(chatJid, getUpdateType(node), participantJid);
        }).exceptionallyAsync(throwable -> socketHandler.handleFailure(STREAM, throwable));
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
            socketHandler.send(Node.of("call", Map.of("to", to), relay));
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
        var privacyType = PrivacySettingType.of(privacySettingName)
                .orElseThrow(() -> new NoSuchElementException("Unknown privacy option: %s".formatted(privacySettingName)));
        var privacyValueName = node.attributes().getString("value");
        var privacyValue = PrivacySettingValue.of(privacyValueName)
                .orElseThrow(() -> new NoSuchElementException("Unknown privacy value: %s".formatted(privacyValueName)));
        if (!update) {
            return queryPrivacyExcludedContacts(privacyType, privacyValue)
                    .thenAcceptAsync(response -> socketHandler.store().addPrivacySetting(privacyType, new PrivacySettingEntry(privacyType, privacyValue, response)));
        }

        var oldEntry = socketHandler.store().findPrivacySetting(privacyType);
        var newValues = getUpdatedBlockedList(node, oldEntry, privacyValue);
        var newEntry = new PrivacySettingEntry(privacyType, privacyValue, Collections.unmodifiableList(newValues));
        socketHandler.store().addPrivacySetting(privacyType, newEntry);
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
                .orElseThrow(() -> new NoSuchElementException("Missing privacy in newsletters: %s".formatted(result)))
                .findNode("list")
                .orElseThrow(() -> new NoSuchElementException("Missing list in newsletters: %s".formatted(result)))
                .findNodes("user")
                .stream()
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
            socketHandler.handleFailure(CRYPTOGRAPHY, new RuntimeException("Detected a bad mac"));
            return;
        }

        var statusCode = node.attributes().getInt("code");
        switch (statusCode) {
            case 503 -> socketHandler.disconnect(DisconnectReason.RECONNECTING);
            case 500 -> socketHandler.disconnect(DisconnectReason.LOGGED_OUT);
            case 401 -> handleStreamError(node);
            default -> node.children().forEach(error -> socketHandler.store().resolvePendingRequest(error, true));
        }
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
        if (!socketHandler.keys().initialAppSync()) {
            configureWhatsappAccount()
                    .exceptionally(throwable -> socketHandler.handleFailure(LOGIN, throwable))
                    .thenRunAsync(() -> { onRegistration(); onInitialInfo(); });
        }else {
            loggedInFuture.thenRunAsync(this::onInitialInfo);
        }

        var attributionFuture = socketHandler.store()
                .serializer()
                .attributeStore(socketHandler.store())
                .exceptionallyAsync(exception -> socketHandler.handleFailure(MESSAGE, exception));
        CompletableFuture.allOf(loggedInFuture, attributionFuture)
                .thenRunAsync(this::onAttribution);
    }

    private CompletableFuture<Void> initSession() {
        return CompletableFuture.allOf(
                createMediaConnection(0, null),
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

    private CompletableFuture<Void> configureWhatsappAccount() {
        return CompletableFuture.allOf(
                acceptTermsOfService(),
                setPushEndpoint(),
                setDefaultStatus(),
                resetMultiDevice(),
                setupGoogleCrypto(),
                sendNumberMetadata(),
                setupRescueToken(),
                queryGroups(),
                getInviteSender(),
                queryNewsletters()
        );
    }

    private CompletableFuture<Void> getInviteSender() {
        return socketHandler.sendQuery("get", "w:growth", Node.of("invite", Node.of("get_sender")))
                .thenRun(() -> {});
    }

    private CompletableFuture<Void> setDefaultStatus() {
        return socketHandler.changeAbout("Hey there! I am using WhatsApp.");
    }

    private CompletableFuture<Void> sendInitialMetadata() {
        var wamBinary = "57414d0501010000200b800d086950686f6e652058800f0631362e372e34801109322e32342e342e373810152017502f81abe465206928830138790604387b0602186b1818a71c88911e063230483234308879240431372e3018ed3318ab3888fb3c09353639363330373431294e047601000090da5ca02f40192e0b220252017cdac7063603ff192e0b22025201a5145b033603ff2946044202ac04120372040000408015ae924072010000408015ae92402605293805220a7206000000c82ec43e405202d84383025601d8c36c02294a0476010000000887236140502f82abe465293805320a02720600000018c0073f4052020084b30256010004b202502f88abe465294e0476010000404f463f1d4029460472020000806d4570834072030000006c2bc259407204000000a2c05a7f407201000080bea5e582402605502fc7abe4652976088203204d6a6b794d7a6b784d5451314d51616c4d746b434f763975423037756f513d3d82011c4d54457a4f4459784e6a637842715579325149362f3234485475366872049d3b3bf053e078426605bd023f058e010000298a042601502fc1ace46518fb2e293805220a72060000802d84af734052029005cd035601b8c5a103502fe4aee46508fb2e294e0476010000387305982b402d5802502ffcaee46529fa01360104502f11b1e46529fa01360104502f93b1e465298a04260229ee0122058202674b696c6c696e67206170702062656361757365206974206e6576657220706f7374656420616e20696e636f6d696e672063616c6c20746f207468652073797374656d20616674657220726563656976696e67206120507573684b697420566f495020707573682e1206360402502fd0b2e465294e047601000080bf9faa1140299c09120432030a8209094558435f435241534852087cb1e46552077cb1e465320506860b0753494741425254502f29b3e46529fa01360104502f4cb3e465298a04260229ee0122058202674b696c6c696e67206170702062656361757365206974206e6576657220706f7374656420616e20696e636f6d696e672063616c6c20746f207468652073797374656d20616674657220726563656976696e67206120507573684b697420566f495020707573682e1206360402502f03b5e465294e047601000020651f6e1040299c09120432030a8209094558435f4352415348520820b3e465520720b3e465320506860b0753494741425254502f1cb5e46529fa01360104502f1bb7e46529fa01360104502fc7b7e465298a042602502fc8b7e46529ee0122058202674b696c6c696e67206170702062656361757365206974206e6576657220706f7374656420616e20696e636f6d696e672063616c6c20746f207468652073797374656d20616674657220726563656976696e67206120507573684b697420566f495020707573682e1206360402502f2cb9e465294e047601000050f637021140299c09120432030a8209094558435f4352415348520894b7e465520794b7e465320506860b0753494741425254502f45b9e46529fa01360104502fa7bbe46529fa01360104502f6ebde46529fa01360104502fcdc6e46529fa01360104502fb5c8e46529fa01360104502fe5cae46529fa01360104502f2acde46529fa01360104502ff7cfe46529fa01360104502f74d3e46529fa01360104502f85d5e46529fa01360104502f3ddce46529fa01360104502f5bdfe465294a047601000000b8dc5c584029de054202840032050332035b520181abe465160629460442029b01720100000084a5ad7940360503502f5cdfe465293805220a72060000ce00fb18c34052025004cf0256015044c702502f65dfe46529fa01360104502fb9e1e46529fa01360104502fcee4e46529fa01360104502f3ae7e46529fa01360104502f45f0e465294a047601000000883bb751402946044202fa007201000000c8a8566f40360502502f47f0e46529fa01360104502f5df0e46518fb2e29760882031c4d7a63774e4459304e4467344e67524a2f454d724b7855452b4f6c5682011c4d5449784d54677a4f5467774e51524a2f454d724b7855452b4f6c567204d92ab5ae64e078426605bce94a068e010000298a042601502f11f1e4652976088203204d7a49344d5441314d7a63324f414c65642b5145624d72744245445a61773d3d8201204e4445774d5449304d446b334d774c65642b5145624d72744245445a61773d3d72044a7ebcda64e07842660507ab4d068e010000298a042601502fdef1e465293805220a72060000fc5a9359cc405202f0c45b035601a0441f03502fc7f3e46508fb2e294e04760100008074be9f1b402d5802502fdff3e46529fa01360104502fc2f4e465298a04260229ee0122058202674b696c6c696e67206170702062656361757365206974206e6576657220706f7374656420616e20696e636f6d696e672063616c6c20746f207468652073797374656d20616674657220726563656976696e67206120507573684b697420566f495020707573682e1206360402502fdaf5e465294e0476010000e0e935c71040299c09120432030a8209094558435f43524153485208c0f4e4655207c0f4e465320506860b0753494741425254502ff3f5e46529fa01360104502f89f7e465298a04260229ee0122058202674b696c6c696e67206170702062656361757365206974206e6576657220706f7374656420616e20696e636f6d696e672063616c6c20746f207468652073797374656d20616674657220726563656976696e67206120507573684b697420566f495020707573682e1206360402502fc6f7e465294e047601000030ab1cea1340299c09120432030a8209094558435f4352415348520854f7e465520754f7e465320506860b0753494741425254502fdef7e46529fa01360104502fe9f7e465298a04260229ee0122058202674b696c6c696e67206170702062656361757365206974206e6576657220706f7374656420616e20696e636f6d696e672063616c6c20746f207468652073797374656d20616674657220726563656976696e67206120507573684b697420566f495020707573682e1206360402502fb3f9e465294e047601000070f6446d1440502fb4f9e465299c09120432030a8209094558435f43524153485208ccf7e4655207ccf7e465320506860b0753494741425254502fccf9e46529fa01360104502faefbe46529fa01360104502fb6fde46529fa01360104502fa5ffe46529fa01360104502fb201e56529fa01360104502f8d18e56529fa01360104502f931ae56529fa01360104502fa61ce56529fa01360104502ffe1ee56529fa01360104502f5621e56529fa01360104502f4523e56529fa01360104502f2725e56529fa01360104502f3227e56529fa01360104502f7b29e56529fa01360104502f6a2be56529fa01360104502f682de56529fa01360104502f762fe56529fa01360104502f8231e56529fa01360104502fbc33e56529fa01360104502fae35e56529fa01360104502fa937e56529fa01360104502fe13be56529fa01360104502f1a3ee56529fa01360104502f8341e56529fa01360104502f9b43e56529fa01360104502fe645e56529fa01360104502f3f48e56529fa01360104502ff04ae56529fa01360104502f394de56529fa01360104502fa04fe56529fa01360104502ff851e56529fa01360104502f4954e56529fa01360104502fa856e56529fa01360104502fd658e56529fa01360104502fb35ae56529fa01360104502fc65ce56529fa01360104502fdc5ee56529fa01360104502f3461e56529fa01360104502f5063e56529fa01360104502f3065e56529fa01360104502f3d67e56529fa01360104502f5969e56529fa01360104502f396be56529fa01360104502f1f6de56529fa01360104502f346fe56529fa01360104502f3371e56529fa01360104502fe573e56529fa01360104502fca75e56529fa01360104502fc477e56529fa01360104502fd079e56529fa01360104502fce7be56529fa01360104502fdb7de56529fa01360104502fd17fe56529fa01360104502ff981e56529fa01360104502f0184e56529fa01360104502f3c86e56529fa01360104502f2188e56529fa01360104502f1a8ae56529fa01360104502f7e8ce56529fa01360104502fd98ee56529fa01360104502fca90e56529fa01360104502fa892e56529fa01360104502f9794e56529fa01360104502fa496e56529fa01360104502fb198e56529fa01360104502fcd9ae56529fa01360104502fcb9ce56529fa01360104502fc89ee56529fa01360104502fa9a0e56529fa01360104502fc5a2e56529fa01360104502f1ba5e56529fa01360104502f04a7e56529fa01360104502fefa8e56529fa01360104502ff9aae56529fa01360104502fdbace56529fa01360104502fc2aee56529fa01360104502fd5b0e56529fa01360104502f2db3e56529fa01360104502f85b5e56529fa01360104502fddb7e56529fa01360104502f36bae56529fa01360104502f8dbce56529fa01360104502fe5bee56529fa01360104502fc5c0e56529fa01360104502fbbc2e56529fa01360104502fc1c4e56529fa01360104502f19c7e56529fa01360104502f0ec9e56529fa01360104502ff6cae56529fa01360104502f03cde56529fa01360104502f6fcfe56529fa01360104502fdad2e56529fa01360104502f0fd5e56529fa01360104502fedd7e56529fa01360104502f72dce56529fa01360104502f9edee56529fa01360104502fbae0e56529fa01360104502fd4e2e56529fa01360104502fe5e5e56529fa01360104502f29e8e56529fa01360104502fa8ece56529fa01360104502f1df0e56529fa01360104502f9af2e56529fa01360104502f8df5e56529fa01360104502f70f7e56529fa01360104502f9ef9e56529fa01360104502f6dfbe565393008ff124752387997000032391f723afcf9f3fab068c4403253063257021252125012541251124f124e12561255122d1266124b124a124c12492248124d42430202424607024245fe0122372244125d12234203e204120112024204a602525fa09eb901320523520694bd000032600b5261a8a900003262ff120712091208120a120b120c120d520ec1b300005219f000e565125882590732342e342e3738521870afe36512121213421495004615a200502f87fbe56529fa01360104502f68fde56529fa01360104502f77ffe56529fa01360104502f6c01e66529fa01360104502fa303e66529fa01360104502f2407e66529fa01360104502f2f0ae66529fa01360104502fc91de66529fa01360104502f3b20e66529fa01360104502f3c28e66529fa01360104502fe42ee66529fa01360104502fd930e66529fa01360104502f8e35e66529fa01360104502f383ce66529fa01360104502f3a3ee66529fa01360104502fe342e66529fa01360104502ffc4ae66529fa01360104502f384de66529fa01360104502f6f53e66529fa01360104502f9e55e66529fa01360104502fc957e66529fa01360104502f6d5ce66529fa01360104502fdd61e66529fa01360104502f0269e66529fa01360104502f9c6ce66529fa01360104502f5171e66529fa01360104502ffb77e66529fa01360104502f0d7be66529fa01360104502f957fe66529fa01360104502f6786e66529fa01360104502ff48ce66529fa01360104502fe98ee66529fa01360104502f8f95e66529fa01360104502f439ae66529fa01360104502fc39de66529fa01360104502f3fa6e66529fa01360104502f08abe66529fa01360104502f0cade66529fa01360104502fb5b1e66529fa01360104502fe0b9e66529fa01360104502f1cbde66529fa01360104502f70c1e66529fa01360104502f60c8e66529fa01360104502fe4d4e66529fa01360104502fc5d8e66529fa01360104502fa0dae66529fa01360104502f1ceae865294e04760100c4c876bed73a4029de054202331232050232037e52015bdfe4652606393008ff1247523884a80000323918723a53331c6eb9473e40125312571252125012541251124f124e12561255122d1266124b124a124c12491248124d1237125d122342033203120112021204125f12051206126012613262ff120712091208120a120b120c120d520e91a900005219f0a3e765125882590732342e342e373852187052e66512121213121416152946044202c503120372040000000727278e4072010000000727278e402605502f3aeae865293805220a72060000706bb2aa99405202b003b6025601b003ad02502f3af4e865294e0476010000c0b120b0823f502f15f5e865294a047601000000407b8a4e40502f16f5e8652946044202550272010000008acaa78240360502502f1ef5e865293805220a72060000300f194eb1405202b043dc025601b083da02502f35fce865294e0476010088638207db2d40801109322e32342e352e3733502fed9de96588fb3c09353733323038313439294e047601000030d069a31540502fd3a1e96529fa01360104502f56c1e96529fa01360104502f62c8e96529fa01360104502f6fcfe96529fa01360104502fa9ebe96529fa01360104502fa7f0ea65298a04260229ee0122058202674b696c6c696e67206170702062656361757365206974206e6576657220706f7374656420616e20696e636f6d696e672063616c6c20746f207468652073797374656d20616674657220726563656976696e67206120507573684b697420566f495020707573682e1206360402502facf0ea65294e047601000040b02b201440298a042602299c09120432030a8209094558435f43524153485208a0f0ea655207a0f0ea65320506860b0753494741425254502ffcf1ea65294e0476010000f889faf71240298a04260229ee0122058202674b696c6c696e67206170702062656361757365206974206e6576657220706f7374656420616e20696e636f6d696e672063616c6c20746f207468652073797374656d20616674657220726563656976696e67206120507573684b697420566f495020707573682e1206360402299c09120432030a8209094558435f43524153485208a0f0ea655207a0f0ea65320509860b075349474b494c4c502f92f8ea65294e04760100000079e9961040299c09120432030a8209094558435f43524153485208ccf1ea655207ccf1ea65320506860b0753494741425254502facf8ea6529fa01360104502f1f0beb65298a04260229ee0122058202674b696c6c696e67206170702062656361757365206974206e6576657220706f7374656420616e20696e636f6d696e672063616c6c20746f207468652073797374656d20616674657220726563656976696e67206120507573684b697420566f495020707573682e1206360402502f483fec65294e04760100801f8941401440299c09120432030a8209094558435f435241534852081c0beb6552071c0beb65320506860b0753494741425254502fcc41ec65294e04760100e415671fde1e40502fcd41ec6529de054202020b32050232030a52011ceae865360602299c092204220382090e4558435f4241445f4143434553535208a841ec655207a841ec6532050b820b07534947534547569602210130783130206973206e6f7420696e20616e7920726567696f6e2e20204279746573206265666f726520666f6c6c6f77696e6720726567696f6e3a2036383731393437363732300a202020202020524547494f4e205459504520202020202020202020202020202020205354415254202d20454e442020202020205b205653495a455d205052542f4d4158205348524d4f442020524547494f4e2044455441494c0a202020202020554e555345442053504143452041542053544152540a2d2d2d3e20200a202020202020636f6d6d7061676520287265736572766564292020202020313030303030303030302d37303030303030303030205b3338342e30475d202d2d2d2f2d2d2d20534d3d4e554c20202e2e2e28756e616c6c6f636174656429299c092204220382090e4558435f4241445f4143434553535208a841ec655207a841ec6532050b820b07534947534547569602210130783130206973206e6f7420696e20616e7920726567696f6e2e20204279746573206265666f726520666f6c6c6f77696e6720726567696f6e3a2036383731393437363732300a202020202020524547494f4e205459504520202020202020202020202020202020205354415254202d20454e442020202020205b205653495a455d205052542f4d4158205348524d4f442020524547494f4e2044455441494c0a202020202020554e555345442053504143452041542053544152540a2d2d2d3e20200a202020202020636f6d6d7061676520287265736572766564292020202020313030303030303030302d37303030303030303030205b3338342e30475d202d2d2d2f2d2d2d20534d3d4e554c20202e2e2e28756e616c6c6f6361746564292946047202000080598ec499407203000000b31c498940720400008032ac9b894072010000c072647299402605502fe441ec6518fb2e293805220a720600000080a0c4394052024085bc03560118c5f102293805320a027206000000801ac6394052024085bc03560118c5f102502fe841ec6508fb2e294e0476010022050fe6791940502fe941ec652946047202000000b849ef82407203000000c04dba59407204000000d02bc37d407201000000a0df1882402605502ff141ec65293805220a7206000000a0204222405202f0c419035601f0041203801109322e32342e352e3734502f0842ec6588fb3c09353733353838393332294e047601008258f28be5164029460472020000402359c49e407203000040235980934072040000003dddcf854072010000c0c147689e402605502f0a42ec65293805220a7206000000006c6012405202d803a8025601d8039d02294a047601000000c0be213e40293805220a720600000030554013405202d8c3aa025601d843a902293805320a027206000000208e4313405202d8c3aa025601d8039f02502f1142ec65294e0476010036ed7c3f9517402946047202000000f1f3e080407203000000889f47534072040000008dd8f67b40720100008037606480402605502f3342ec65293805220a7206000000d09c2b4140520218054003560118053103502f0d44ec6529fa01360104502f1044ec65294a047601000000808ab15140294604420255027201000000150aab8240360502502f2644ec6529760882031c4d546b324e6a49304e4459354d67625532746c596d617343747a4e6c82011c4d7a41784e4463784d6a6b314d77625532746c596d617343747a4e6c7204765256a32ee278426605a034ea228e010000298a042601502f5444ec6518fb2e2976088203204e4449324e4463784d4463794f41586667426745754842544251444c47513d3d8201204d6a6b324e5441314e5449314e67586667426745754842544251444c47513d3d72041ec5a8ae2ee2784266057fe9ea228e010000298a042601502f7a44ec65293805220a72060000c0e29b4f834052026845cf0356016845a903502f4446ec6508fb2e294e0476010070e86d598326402d5802502f5c46ec6529fa01360104502f4948ec6529fa01360104502f1a4bec6529fa01360104502f034dec6529fa01360104502ff44eec6529fa01360104502f1f54ec6529fa01360104502f9657ec6529fa01360104502f7c59ec6529fa01360104502fc064ec6529fa01360104502f9c66ec6529fa01360104502fa468ec6529fa01360104502fc56aec6529fa01360104502fe06dec6529fa01360104502f6d70ec6529fa01360104502fac85ec6529fa01360104502f018cec6529de05420290023205063203735201cd41ec65360603294a04760100000010c3e5444029460442021f017201000000c06be57140360503502f208cec65293805220a720600000b268377d14052020004a002560100049702502f738cec65294a047601000000e07c333f40502f748cec65294604420259027201000000a09bca8240360502502fb08cec6518fb2e29760882031c4d7a6b774e6a55304f44557a4154717267415967414b4d45684a76448201204d7a4d344e5463344f44597a4e774536713441474941436a4249536277773d3d7204007c085940e278426605a88f05248e010000298a042601502fcb8cec6588eb0a0361746e29f60132010222021607192e0b220252019870620b36031e192e0b2202520158dc8302360307192e0b22025201bc1c420236035a192e0b3202025201a5145b033603ff192e0b32020252017cdac7063603ff192e0b220252014390d10e260329500332010472030000000030d9d93f360202502fcd8cec6588eb0a03636c6e2950033201037203000000ecffbb9640360202";
        var wamData = new String(HexFormat.of().parseHex(wamBinary))
                .replace("iPhone X", socketHandler.store().device().model().replaceAll("_", " "))
                .replace("2.24.2.71", socketHandler.store().version().toString())
                .replace("569630741", String.valueOf(socketHandler.store().phoneNumber().orElseThrow().numberWithoutPrefix()))
                .getBytes();
        var addNode = Node.of("add", Map.of("t", Clock.nowSeconds()), wamData);
        return socketHandler.sendQuery("set", "w:stats", addNode)
                .thenRun(() -> {});
    }

    private CompletableFuture<Void> sendNumberMetadata() {
        var wamBinary = "57414d0501010001200b800d086950686f6e652058800f0631362e372e34801109322e32342e322e373110152017502fc8a2b265206928830138790604387b060288eb0a03636c6e186b1818a71c88911e063230483234308879240431372e3018fb2e18ed3318ab3888fb3c09353537393735393734290c147602705fefb1a86cd941";
        var wamData = new String(HexFormat.of().parseHex(wamBinary))
                .replace("iPhone X", socketHandler.store().device().model().replaceAll("_", " "))
                .replace("2.24.2.71", socketHandler.store().version().toString())
                .replace("557975974", String.valueOf(socketHandler.store().phoneNumber().orElseThrow().numberWithoutPrefix()))
                .getBytes();
        var addNode = Node.of("add", Map.of("t", Clock.nowSeconds()), wamData);
        return socketHandler.sendQuery("set", "w:stats", addNode)
                .thenRun(() -> {});
    }

    private CompletableFuture<Void> setPushEndpoint() {
        if(socketHandler.store().clientType() != ClientType.MOBILE) {
            return CompletableFuture.completedFuture(null);
        }

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
    }

    private CompletableFuture<Void> resetMultiDevice() {
        if(socketHandler.store().clientType() != ClientType.MOBILE) {
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.sendQuery("set", "md", Node.of("remove-companion-device", Map.of("all", true, "reason", "user_initiated")))
                .thenComposeAsync(ignored -> socketHandler.sendQuery("set", "w:sync:app:state", Node.of("delete_all_data")))
                .thenRun(() -> {});
    }

    private CompletableFuture<Void> setupRescueToken() {
        return socketHandler.sendQuery("set", "w:auth:token", Node.of("token", HexFormat.of().parseHex("20292dbd11e06094feb1908737ca76e6")))
                .thenRun(() -> {});
    }

    private CompletableFuture<Void> setupGoogleCrypto() {
        var firstCrypto = Node.of("crypto", Map.of("action", "create"), Node.of("google", HexFormat.of().parseHex("7d7ce52cde18aa4854bf522bc72899074e06b60b1bf51864de82e8576b759d12")));
        var secondCrypto = Node.of("crypto", Map.of("action", "create"), Node.of("google", HexFormat.of().parseHex("2f39184f8feb97d57493a69bf5558507472c6bfb633b1c2d369f3409210401c6")));
        return socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", firstCrypto)
                .thenCompose(ignored -> socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", secondCrypto))
                .thenRun(() -> {});
    }

    private CompletableFuture<Void> acceptTermsOfService() {
        if(socketHandler.store().clientType() != ClientType.MOBILE) {
            return CompletableFuture.completedFuture(null);
        }


        var firstNotice = Node.of("notice", Map.of("id", "20230902"));
        var secondNotice = Node.of("notice", Map.of("id", "20230901"));
        var thirdNotice = Node.of("notice", Map.of("id", "20231027"));
        return socketHandler.sendQuery("get", "tos", Node.of("request", firstNotice, secondNotice, thirdNotice))
                .thenCompose(ignored -> socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.of("accept")))
                .thenCompose(ignored -> socketHandler.sendQuery("set", "tos", Node.of("trackable", Map.of("id", "20601216", "result", 1))))
                .thenCompose(ignored -> socketHandler.sendQuery("set", "tos", Node.of("trackable", Map.of("id", "20900727", "result", 1))))
                .thenRun(() -> {});
    }

    private void onRegistration() {
        socketHandler.store().serialize(true);
        socketHandler.keys().serialize(true);
    }

    private void onAttribution() {
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
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing newsletter payload"));
        var newslettersJson = SubscribedNewslettersResponse.ofJson(newslettersPayload)
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
                socketHandler.sendWithNoResponse(Node.of("presence", Map.of("name", socketHandler.store().name(), "type", "unavailable")));
            }
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.sendWithNoResponse(Node.of("presence", Map.of("name", socketHandler.store().name(), "type", "available")))
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
        var privacy = result.findNode("privacy")
                .orElseThrow(() -> new NoSuchElementException("Missing privacy in newsletters: %s".formatted(result)))
                .children()
                .stream()
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
        var executor = CompletableFuture.delayedExecutor(PING_INTERVAL, TimeUnit.SECONDS, Thread::startVirtualThread);
        ping(executor);
    }

    private void ping(Executor executor) {
        if (socketHandler.state() != SocketState.CONNECTED) {
            return;
        }

        socketHandler.sendQuery("get", "w:p", Node.of("ping"))
                .thenRunAsync(() -> socketHandler.onSocketEvent(SocketEvent.PING))
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(STREAM, throwable));
        socketHandler.store().serialize(true);
        socketHandler.store().serializer().linkMetadata(socketHandler.store());
        socketHandler.keys().serialize(true);
        this.pingFuture = CompletableFuture.runAsync(() -> ping(executor), executor);
    }

    private CompletableFuture<Void> createMediaConnection(int tries, Throwable error) {
        if (socketHandler.state() != SocketState.CONNECTED) {
            return CompletableFuture.completedFuture(null);
        }

        if (tries >= MAX_ATTEMPTS) {
            socketHandler.store().setMediaConnection(null);
            socketHandler.handleFailure(MEDIA_CONNECTION, error);
            scheduleMediaConnection(MEDIA_CONNECTION_DEFAULT_INTERVAL);
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.sendQuery("set", "w:m", Node.of("media_conn"))
                .thenApplyAsync(node -> {
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
                    return new MediaConnection(auth, ttl, maxBuckets, timestamp, hosts);
                })
                .thenAcceptAsync(result -> {
                    socketHandler.store().setMediaConnection(result);
                    scheduleMediaConnection(result.ttl());
                })
                .exceptionallyCompose(throwable -> createMediaConnection(tries + 1, throwable));
    }

    private void scheduleMediaConnection(int seconds) {
        var executor = CompletableFuture.delayedExecutor(seconds, TimeUnit.SECONDS);
        this.mediaConnectionFuture = CompletableFuture.runAsync(() -> createMediaConnection(0, null), executor);
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
        socketHandler.sendWithNoResponse(request);
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

        if(pingFuture != null && !pingFuture.isDone()) {
            pingFuture.cancel(true);
        }

        retries.clear();
        lastLinkCodeKey.set(null);
    }
}
