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
import it.auties.whatsapp.model.chat.ChatPastParticipant;
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
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Json;

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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static it.auties.whatsapp.util.SignalConstants.KEY_BUNDLE_TYPE;

class StreamHandler {
    private static final byte[] DEVICE_WEB_SIGNATURE_HEADER = {6, 1};
    private static final int PRE_KEYS_UPLOAD_CHUNK = 10;
    private static final int PING_INTERVAL = 20;
    private static final int MAX_MESSAGE_RETRIES = 5;
    private static final int DEFAULT_NEWSLETTER_MESSAGES = 100;
    private static final byte[][] CALL_RELAY = new byte[][]{
            new byte[]{-105, 99, -47, -29, 13, -106},
            new byte[]{-99, -16, -53, 62, 13, -106},
            new byte[]{-99, -16, -25, 62, 13, -106},
            new byte[]{-99, -16, -5, 62, 13, -106},
            new byte[]{-71, 60, -37, 62, 13, -106}
    };
    private static final byte[] ACCOUNT_SIGNATURE_HEADER = {6, 0};

    private final SocketHandler socketHandler;
    private final WebVerificationHandler webVerificationHandler;
    private final Map<String, Integer> retries;
    private final AtomicReference<String> lastLinkCodeKey;
    private final AtomicBoolean retryConnection;

    protected StreamHandler(SocketHandler socketHandler, WebVerificationHandler webVerificationHandler) {
        this.socketHandler = socketHandler;
        this.webVerificationHandler = webVerificationHandler;
        this.retries = new ConcurrentHashMap<>();
        this.lastLinkCodeKey = new AtomicReference<>();
        this.retryConnection = new AtomicBoolean(false);
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
        var metadata = node.findChild();
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
        if (!Objects.equals(type.orElse(null), "retry")) {
            return;
        }

        acceptMessageRetry(message);
    }

    private void acceptMessageRetry(ChatMessageInfo message) {
        if (!message.fromMe()) {
            return;
        }

        var attempts = retries.getOrDefault(message.id(), 0);
        if (attempts > MAX_MESSAGE_RETRIES) {
            return;
        }

        socketHandler.querySessionsForcefully(message.senderJid()).whenCompleteAsync((result, error) -> {
            if(error != null) {
                return;
            }

            var all = message.senderJid().device() == 0;
            var recipients = all ? null : Set.of(message.senderJid());
            var request = new MessageSendRequest.Chat(message, recipients, !all, false, null);
            socketHandler.sendMessage(request);
            retries.put(message.id(), attempts + 1);
        });
    }

    private void updateReceipt(MessageStatus status, Chat chat, Contact participant, ChatMessageInfo message) {
        var container = status == MessageStatus.READ ? message.receipt().readJids() : message.receipt().deliveredJids();
        container.add(participant != null ? participant.jid() : message.senderJid());
        if(chat == null) {
            return;
        }

        socketHandler.queryGroupMetadata(chat.jid()).thenAcceptAsync(metadata -> {
            if (participant != null && metadata.participants().size() != container.size()) {
                return;
            }

            switch (status) {
                case READ -> message.receipt().readTimestampSeconds(Clock.nowSeconds());
                case PLAYED -> message.receipt().playedTimestampSeconds(Clock.nowSeconds());
            }
        });
    }

    private List<String> getReceiptsMessageIds(Node node) {
        var messageIds = Stream.ofNullable(node.findChild("list"))
                .flatMap(Optional::stream)
                .map(list -> list.listChildren("item"))
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

        if (!callNode.description().equals("offer")) {
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
        var relayNode = node.findChild("relay").orElse(null);
        if (relayNode == null) {
            return;
        }

        var callCreator = relayNode.attributes()
                .getRequiredJid("call-creator");
        var callId = relayNode.attributes()
                .getString("call-id");
        relayNode.listChildren("participant")
                .stream()
                .map(entry -> entry.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .forEach(to -> sendRelay(callCreator, callId, to));
    }

    private void sendRelay(Jid callCreator, String callId, Jid to) {
        for (var value : CALL_RELAY) {
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

        var liveUpdates = node.findChild("live_updates");
        if (liveUpdates.isEmpty()) {
            return;
        }


        var messages = liveUpdates.get().findChild("messages");
        if (messages.isEmpty()) {
            return;
        }

        for (var messageNode : messages.get().listChildren("message")) {
            var messageId = messageNode.attributes()
                    .getRequiredString("server_id");
            var newsletterMessage = socketHandler.store().findMessageById(newsletter.get(), messageId);
            if (newsletterMessage.isEmpty()) {
                continue;
            }

            messageNode.findChild("reactions")
                    .map(reactions -> reactions.listChildren("reaction"))
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
        var update = node.findChild("update")
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
        var updatedMetadata = updateJson.newsletter()
                .metadata()
                .orElse(null);
        var oldMetadata = newsletter.metadata()
                .orElse(null);
        if(oldMetadata == null) {
            newsletter.setMetadata(updatedMetadata);
        }else if (updatedMetadata != null) {
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
    }

    private void handleNewsletterJoin(Node update) {
        var joinPayload = update.contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing join payload"));
        var joinJson = NewsletterResponse.ofJson(joinPayload)
                .orElseThrow(() -> new NoSuchElementException("Malformed join payload"));
        socketHandler.store().addNewsletter(joinJson.newsletter());
        if(!socketHandler.store().webHistorySetting().isZero()) {
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
        var linkCodeCompanionReg = node.findChild("link_code_companion_reg")
                .orElseThrow(() -> new NoSuchElementException("Missing link_code_companion_reg: " + node));
        var ref = linkCodeCompanionReg.findChild("link_code_pairing_ref")
                .flatMap(Node::contentAsBytes)
                .orElseThrow(() -> new IllegalArgumentException("Missing link_code_pairing_ref: " + node));
        var primaryIdentityPublicKey = linkCodeCompanionReg.findChild("primary_identity_pub")
                .flatMap(Node::contentAsBytes)
                .orElseThrow(() -> new IllegalArgumentException("Missing primary_identity_pub: " + node));
        var primaryEphemeralPublicKeyWrapped = linkCodeCompanionReg.findChild("link_code_pairing_wrapped_primary_ephemeral_pub")
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
        var child = node.findChild("wa_old_registration");
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
        var child = node.findChild();
        if (child.isEmpty()) {
            return;
        }

        var stubType = ChatMessageInfo.StubType.of(child.get().description());
        if (stubType.isEmpty()) {
            return;
        }

        // TODO: Handle all cases
        if (stubType.get() == ChatMessageInfo.StubType.GROUP_CHANGE_SUBJECT) {
            onGroupSubjectChange(node);
        }

        handleGroupStubNotification(node, stubType.get());
    }

    private void onGroupSubjectChange(Node node) {
        var subject = node.findChild("subject")
                .flatMap(subjectNode -> subjectNode.attributes().getOptionalString("subject"))
                .orElse(null);
        if(subject == null) {
            return;
        }

        var fromJid = node.attributes()
                .getRequiredJid("from");
        socketHandler.store()
                .findChatByJid(fromJid)
                .ifPresent(chat -> chat.setName(subject));
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
                .id(ChatMessageKey.randomIdV2(Objects.requireNonNullElse(participantJid, chat.jid()), socketHandler.store().clientType()))
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

        handleGroupStubType(timestamp, chat, stubType, participantJid);
    }

    private void handleGroupStubType(long timestamp, Chat chat, ChatMessageInfo.StubType stubType, Jid participantJid) {
        switch (stubType) {
            case GROUP_PARTICIPANT_REMOVE, GROUP_PARTICIPANT_LEAVE -> {
                var reason = stubType == ChatMessageInfo.StubType.GROUP_PARTICIPANT_REMOVE ? ChatPastParticipant.Reason.REMOVED : ChatPastParticipant.Reason.LEFT;
                var pastParticipant = new ChatPastParticipant(participantJid, reason, timestamp);
                socketHandler.addPastParticipant(chat.jid(), pastParticipant);
            }
            case GROUP_PARTICIPANT_ADD -> {
                var pastParticipants = socketHandler.pastParticipants().get(chat.jid());
                if(pastParticipants == null) {
                    return;
                }

                pastParticipants.removeIf(entry -> Objects.equals(entry.jid(), participantJid));
            }
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
        if (!chat.isServerJid(JidServer.whatsapp())) {
            return;
        }
        var keysSize = node.findChild("count")
                .orElseThrow(() -> new NoSuchElementException("Missing count in notification"))
                .attributes()
                .getInt("value");
        sendPreKeys(keysSize);
    }

    private void handleAccountSyncNotification(Node node) {
        var child = node.findChild();
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
        var devices = child.listChildren("device")
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
        var keyIndexListNode = child.findChild("key-index-list")
                .orElseThrow(() -> new NoSuchElementException("Missing index key node from device sync"));
        var signedKeyIndexBytes = keyIndexListNode.contentAsBytes()
                .orElseThrow(() -> new NoSuchElementException("Missing index key from device sync"));
        socketHandler.keys().setSignedKeyIndex(signedKeyIndexBytes);
        var signedKeyIndexTimestamp = keyIndexListNode.attributes().getLong("ts");
        socketHandler.keys().setSignedKeyIndexTimestamp(signedKeyIndexTimestamp);
    }

    private void updateBlocklist(Node child) {
        child.listChildren("item").forEach(this::updateBlocklistEntry);
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
        var category = child.listChildren("category");
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
        for (var entry : node.listChildren("user")) {
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
        return result.findChild("privacy")
                .flatMap(node -> node.findChild("list"))
                .map(node -> node.listChildren("user"))
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

        var patches = node.listChildren("collection")
                .stream()
                .map(entry -> entry.attributes().getRequiredString("name"))
                .map(PatchType::of)
                .toArray(PatchType[]::new);
        socketHandler.pullPatch(patches);
    }

    private void digestIb(Node node) {
        var dirty = node.findChild("dirty");
        if (dirty.isEmpty()) {
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
            case 403, 503 -> socketHandler.disconnect(retryConnection.getAndSet(true) ? DisconnectReason.BANNED : DisconnectReason.RECONNECTING);
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
        finishLogin();
    }

    private CompletableFuture<Void> attributeStore() {
        return socketHandler.store()
                .serializer()
                .attributeStore(socketHandler.store())
                .exceptionallyAsync(exception -> socketHandler.handleFailure(MESSAGE, exception));
    }

    private void finishLogin() {
        switch (socketHandler.store().clientType()) {
            case WEB -> finishWebLogin();
            case MOBILE -> finishMobileLogin();
        }
    }

    private void finishWebLogin() {
        var loginFuture = CompletableFuture.allOf(
                        setActiveConnection(),
                        queryRequiredWebInfo(),
                        sendInitialPreKeys(),
                        scheduleMediaConnectionUpdate(),
                        updateSelfPresence(),
                        queryInitial2fa(),
                        queryInitialAboutPrivacy(),
                        queryInitialPrivacySettings(),
                        queryInitialDisappearingMode(),
                        queryInitialBlockList()
                )
                .thenRunAsync(this::onInitialInfo)
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(LOGIN, throwable));
        CompletableFuture.allOf(loginFuture, attributeStore())
                .thenComposeAsync(result -> socketHandler.keys().initialAppSync() ? CompletableFuture.completedFuture(null) : queryGroups())
                .thenRunAsync(this::notifyChatsAndNewsletters);
    }

    private CompletableFuture<Node> setActiveConnection() {
        return socketHandler.sendQuery("set", "passive", Node.of("active"));
    }

    private CompletableFuture<?> sendInitialPreKeys() {
        if (socketHandler.keys().hasPreKeys()) {
            return CompletableFuture.completedFuture(null);
        }
        
        return sendPreKeys(PRE_KEYS_UPLOAD_CHUNK);
    }

    private void finishMobileLogin() {
        if (!socketHandler.keys().initialAppSync()) {
            initMobileSession();
            return;
        }

        var loginFuture = CompletableFuture.allOf(
                        setupRescueToken(),
                        setActiveConnection(),
                        queryMobileSessionMex(),
                        acceptDynamicTermsOfService(),
                        setPushEndpoint(),
                        updateSelfPresence(),
                        scheduleMediaConnectionUpdate(),
                        sendWam2()
                )
                .thenRunAsync(this::onInitialInfo);
        CompletableFuture.allOf(loginFuture, attributeStore())
                .thenRunAsync(this::notifyChatsAndNewsletters)
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(LOGIN, throwable));
    }

    private CompletableFuture<Void> acceptDynamicTermsOfService() {
        return socketHandler.sendQuery("get", "tos", Node.of("get_user_disclosures", Map.of("t", 0))).thenComposeAsync(result -> {
            var notices = result.listChildren("notice")
                    .stream()
                    .map(notice -> Node.of("notice", Map.of("id", notice.attributes().getRequiredString("id"))))
                    .map(notice -> socketHandler.sendQuery("get", "tos", Node.of("request", notice)))
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(notices);
        });
    }

    private CompletableFuture<Void> queryMobileSessionMex() {
        return CompletableFuture.allOf(
                socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7561558900567547"), HexFormat.of().parseHex("7b227661726961626c6573223a7b7d7d"))),
                socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7480997188628461"), HexFormat.of().parseHex("7b227661726961626c6573223a7b22696e707574223a22454d41494c227d7d")))
        );
    }

    private void initMobileSession() {
        initMobileSessionPresence(false)
                .thenComposeAsync(result -> CompletableFuture.allOf(
                        setPushEndpoint(),
                        queryProtocolV2(),
                        queryGroups(),
                        queryLists(),
                        updateUserPicture(false),
                        updateUserAbout(false),
                        sendInitialPreKeys(),
                        setActiveConnection(),
                        setDefaultStatus(),
                        queryInitial2fa(),
                        queryInitialAboutPrivacy(),
                        queryInitialPrivacySettings(),
                        queryInitialDisappearingMode(),
                        queryInitialBlockList(),
                        acceptTermsOfService(),
                        sendWam1(),
                        queryProtocolV1(),
                        setupGoogleCrypto(),
                        resetCompanionDevices(),
                        checkBusinessStatus(),
                        setupGoogleCrypto(),
                        cleanGroups(),
                        sendWam2(),
                        initMobileSessionPresence(true),
                        getInviteSender(),
                        queryMobileSessionInitMex()
                ))
                .thenComposeAsync(ignored -> {
                    socketHandler.keys().setInitialAppSync(true);
                    return socketHandler.disconnect(DisconnectReason.RECONNECTING);
                })
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(LOGIN, throwable));
    }

    private CompletableFuture<Void> initMobileSessionPresence(boolean done) {
        if(!done) {
            return socketHandler.sendNodeWithNoResponse(Node.of("presence", Map.of("type", "unavailable")));
        }

        return socketHandler.sendNodeWithNoResponse(Node.of("presence", Map.of("type", "available", "name", socketHandler.store().name())));
    }

    private CompletableFuture<Node> queryMobileSessionInitMex() {
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7561558900567547"), HexFormat.of().parseHex("7b227661726961626c6573223a7b7d7d")));
    }

    private CompletableFuture<Node> cleanGroups() {
        return socketHandler.sendQuery("set", "urn:xmpp:whatsapp:dirty", Node.of("clean", Map.of("type", "groups")));
    }

    private CompletableFuture<Node> resetCompanionDevices() {
        return socketHandler.sendQuery("set", "md", Node.of("remove-companion-device", Map.of("all", true, "reason", "user_initiated")));
    }

    private CompletableFuture<Node> queryProtocolV1() {
        return socketHandler.sendQuery("get", "abt", Node.of("props", Map.of("protocol", 1)));
    }

    private CompletableFuture<Node> queryLists() {
        return socketHandler.sendQuery("get", "w:b", Node.of("lists"));
    }

    private CompletableFuture<Node> queryProtocolV2() {
        return socketHandler.sendQuery("get", "w", Node.of("props", Map.of("protocol", "2", "hash", "")));
    }

    private CompletableFuture<?> getInviteSender() {
        return socketHandler.sendQuery("get", "w:growth", Node.of("invite", Node.of("get_sender")));
    }

    private CompletableFuture<Void> setDefaultStatus() {
        return socketHandler.changeAbout("Hey there! I am using WhatsApp.");
    }

    private CompletableFuture<?> sendWam1() {
        var wamBinary = "57414d0501010000200b800d086950686f6e652037800f0631352e372e3380110a322e32342e31372e373810152017502f2de6d266206928830138790604387b0602186b1818a71c88911e063139483330378879240431372e3418ed3318ab3888fb3c09363335333637323531294e04760100c0efd24da66540192e0b220252017cdac7063603ff192e0b22025201a5145b033603ff502f3ce6d26629fa01360104502f4de6d266294e0476010000b2f244752940502f4ee6d2662946047202000040f14db197407203000040f14db190407204000000b8be557a4072010000409fbd4697402205160c502f54e6d26629e80412031204120a120612071208720186eb51b81e408f402609502f55e6d26629e80412031204120a12061207120872014d62105839408f402609502f56e6d26629e80412031204120a1206120712087201a69bc420b03a8f402609502f57e6d26629e80412031204120a12061207120872014a0c022b87458f402609502f58e6d26629101442018a003203023205524604de00502f59e6d26639ee10ff22023203084204d00256055f5f6f67502f5ae6d266290813720100000033888c974086031f6170702d6174746573746174696f6e5f7265672d6174746573746174696f6e502f5fe6d26629e80412031204120a1206120712087201355eba490c3e8f402609502f61e6d26629e80412031204120a1206120712087201c876be9f1a418f402609502f64e6d26629e80412031204120a1206120712087201e4a59bc420428f402609502f65e6d26629e80412031204120a1206120712087201b29defa7c63f8f402609502f66e6d26629e80412031204120a12061207120872013f355eba493e8f402609502f68e6d26629e80412031204120a1206120712087201fed478e926398f402609502f69e6d26629e80412031204120a1206120712087201dbf97e6abc478f402609502f6ae6d26629e80412031204120a1206120712087201ec51b81e853f8f402609502f6be6d26629e80412031204120a12061207120872015b643bdf4f398f402609502f6de6d26629e80412031204120a1206120712087201d7a3703d0a408f402609502f6ee6d26629e80412031204120a12061207120872016de7fba9f13f8f402609502f6fe6d26629e80412031204120a1206120712087201986e1283c03b8f402609502f72e6d26629e80412031204120a12061207120872017d3f355eba408f402609502f73e6d26629e80412031204120a120612071208720191ed7c3f35428f402609502f74e6d26629e80412031204120a1206120712087201d9cef753e3408f402609502f75e6d26629e80412031204120a1206120712087201068195438b3f8f402609502f77e6d26629e80412031204120a120612071208720139b4c876be3f8f402609502f78e6d26629e80412031204120a120612071208720174931804563f8f402609502f79e6d26629e80412031204120a1206120712087201a01a2fdd24408f402609502f7ae6d26629e80412031204120a1206120712087201490c022b873f8f402609502f7ce6d26629e80412031204120a12061207120872012fdd2406813e8f402609502f7de6d26629e80412031204120a12061207120872011804560e2d418f402609502f7ee6d26629e80412031204120a1206120712087201a4703d0ad73e8f402609502f7fe6d26629e80412031204120a120612071208720104560e2db23f8f402609502f8be6d26629e80412031204120a1206120712087201736891ed7c3f8f402609502f8ce6d26629e80412031204120a1206120712087201a245b6f3fd3f8f402609502f8ee6d26629e80412031204120a12061207120872010e2db29def3f8f402609502f91e6d26629e80412031204120a120612071208720109ac1c5a646882402609502f92e6d26629e80412031204120a1206120712084201e8032609502f95e6d26629e80412031204120a12061207120872019cc420b072408f402609502f96e6d26629e80412031204120a120612071208720145b6f3fdd43f8f402609502f97e6d26629e80412031204120a12061207120872018fc2f5285c1d85402609502f98e6d26629e80412031204120a12061207120872012cb29defa7408f402609502f9ae6d26639ec10ff520193ff010032021b220332040e1205120656075f5f6f67502f9ce6d26629e80412031204120a1206120712087201a245b6f3fdc284402609502f9de6d26629e80412031204120a1206120712087201dcf97e6abc3f8f402609502f9fe6d26629e80412031204120a12061207120872017f6abc74931d8e402609502fa1e6d26629081372010000009cc6eb5f4086031d6170702d6174746573746174696f6e5f7265672d617373657274696f6e502fa2e6d26629760882031c4d7a457a4d5451334d4455334d637a3554414a6a317a784b4b38383d82011c4d5463784f5445794f5463354e737a3554414a6a317a784b4b38383d72049a1fb74e7d1a794266055eead4a791010000298a042601502fa6e6d26629e80412031204120a1206120712087201fa7e6abc748284402609502fa9e6d26629081372010000003455de5f4086031d6170702d6174746573746174696f6e5f7265672d617373657274696f6e502faae6d26629e80412031204120a1206120712087201fed478e926417e402609502fabe6d26629e80412031204120a120612071208720122dbf97e6a667e402609502face6d26629e80412031204120a12061207120872013e0ad7a3706e86402609502faee6d26629e80412031204120a12061207120872015a643bdf4fff79402609502fafe6d26629e80412031204120a1206120712087201a4703d0ad7f579402609502fb0e6d26629e80412031204120a1206120712087201d122dbf97e0a7a402609502fb1e6d26629e80412031204120a12061207120872014260e5d022357a402609502fb4e6d26629e80412031204120a1206120712087201a8c64b37894b7a402609502fb5e6d26629e80412031204120a1206120712087201b29defa7c6f979402609502fb6e6d26629e80412031204120a12061207120872011904560e2d087a402609502fb8e6d26629e80412031204120a1206120712087201cba145b6f3117a402609502fb9e6d26629e80412031204120a1206120712087201e6d022dbf9007a402609502fbae6d26629e80412031204120a120612071208720140355eba490c7a402609502fbde6d26629e80412031204120a12061207120872012506819543ff79402609502fbee6d26629e80412031204120a1206120712087201295c8fc2f5027a402609502fbfe6d26629e80412031204120a1206120712087201931804560efb79402609502fc0e6d26629e80412031204120a12061207120872016991ed7c3f0f7a402609502fc2e6d26629e80412031204120a1206120712087201d7a3703d0a0b7a402609502fc6e6d266293805220a7206000000d9eb8c5e405202a0c4b7035601a004a20339ec10ff220132021b520301c30000320414520541000100220656075f5f6f6739ee10ff320208220352041bab010056055f5f6f67293805320a02720600000080108e5e405202a0c4b7035601a0c4a003502ff6e6d266294e04760100007498273e3f402946047202000080e5ca578f407203000000cb954f7840720400000030356f824072010000801500978e402205160c39ee10ff220232030852045569020056055f5f6f67502ff9e6d26629081372010000008081eb124086031f6170702d6174746573746174696f6e5f7265672d6174746573746174696f6e502ffee6d26629e80412031204120a12061207120872019cc420b072a685402609502f00e7d266293805320a02720600000088efcc2640520278444a0356017804c302502f01e7d266294e0476010000cd1f3e3330402946047202000000be6ea5894072030000007cddba73407204000000080ad57d407201000000c2f3c788402205160c39ee10ff220232030852048193020056055f5f6f67502f07e7d26629101432011c3203264205a3004604e500502f08e7d26629081372010000000078eb144086031f6170702d6174746573746174696f6e5f7265672d6174746573746174696f6e502f09e7d26629e80412031204120a12061207120872015eba490c02ff72402609502f10e7d26629e80412031204120a1206120712087201f6285c8fc2488f402609502f12e7d26629e80412031204120a120612071208720185eb51b81e4776402609502f13e7d26629e80412031204120a1206120712087201713d0ad7a30b89402609502f14e7d26629e80412031204120a1206120712087201295c8fc2f53f8f402609502f15e7d26629e80412031204120a12061207120872017d3f355eba6480402609502f19e7d26629e80412031204120a1206120712087201986e1283c03f8f402609502f1de7d26629081372010000001cef1f614086031d6170702d6174746573746174696f6e5f7265672d617373657274696f6e502f1ee7d26629e80412031204120a1206120712087201dbf97e6abc7277402609502f1fe7d26629e80412031204120a12061207120872016766666666418f402609502f26e7d26629e80412031204120a12061207120872017e6abc7493428f402609502f2be7d26629e80412031204120a120612071208720199999999990380402609502f2ce7d26629e80412031204120a120612071208720154e3a59bc43e8f402609502f2de7d26629e80412031204120a12061207120872011804560e2d418f402609502f2ee7d26629e80412031204120a120612071208720139b4c876be3f8f402609293805320a0272060000006686234740520278049603560178c40a03294e0476010000527bcd0d2b40502f2fe7d2662946047202000080e41a4188407203000000c9357276407204000000849d6178407201000080a6e96987402205160c39ee10ff220232030852048b46030056055f5f6f67502f30e7d26629101432014b32030c32054f4604a6002908137201000000c045df1a4086031f6170702d6174746573746174696f6e5f7265672d6174746573746174696f6e502f3ce7d26639ee10ff32026422035204217b030056055f5f6f67502f3fe7d26639ee10ff22023203645204fc86030056055f5f6f67502f42e7d26629e80412031204120a120612071208720115ae47e17a418f402609502f43e7d26629e80412031204120a12061207120872015eba490c021a84402609502f44e7d2662908137201000000345200604086031d6170702d6174746573746174696f6e5f7265672d617373657274696f6e502f45e7d26629e80412031204120a1206120712087201986e1283c0b27b4026092976088203204d5449774d6a6b304e7a63344f515057426538444b4e505a4252636668513d3d8201204d5451784f5441304f5441774d675057426538444b4e505a4252636668513d3d720456a063767d1a794266054f65d7a791010000298a042601502f46e7d26629e80412031204120a12061207120872016666666666298a402609502f47e7d26629e80412031204120a1206120712087201cccccccccc3f8f402609502f4ae7d26629e80412031204120a1206120712087201d34d6210583f8f402609502f4be7d26629e80412031204120a1206120712087201a245b6f3fd408f402609502f4ce7d26629e80412031204120a1206120712087201105839b4c83f8f402609502f4de7d26629e80412031204120a1206120712087201b0726891ed3f8f402609502f4fe7d26629e80412031204120a12061207120872014f8d976e12408f402609502f51e7d26629e80412031204120a1206120712087201022b8716d93f8f402609502f54e7d26629e80412031204120a12061207120872014f8d976e12408f402609502f55e7d26629e80412031204120a12061207120872019eefa7c64b408f402609502f56e7d26629e80412031204120a12061207120872014b37894160408f402609502f57e7d26629e80412031204120a1206120712087201be9f1a2fdd3f8f402609502f59e7d26629e80412031204120a1206120712087201f2d24d6210408f402609502f5ae7d26629e80412031204120a12061207120872016de7fba9f13f8f402609502f5be7d26629e80412031204120a1206120712087201e6d022dbf93f8f402609502f5ce7d26629e80412031204120a1206120712087201a4703d0ad73f8f402609502f5ee7d26629e80412031204120a12061207120872016766666666408f402609502f5fe7d2662908137201000000da0e61604086031d6170702d6174746573746174696f6e5f7265672d617373657274696f6e502f83e7d266293805320a0272060000008c2e7955405202a0c4b5035601a0842503502f84e7d266294e04760100000daaf1e63040294604720200008025b4a08e4072030000004b68617840720400000081e79e81407201000080a69bcf8d402205160c502f85e7d26639ee10ff220232030852041795040056055f5f6f67502f8ce7d26629e80412031204120a12061207120872018716d9cef73c8f402609502f8de7d26629e80412031204120a1206120712087201a8c64b3789408f402609502f8ee7d26629e80412031204120a12061207120872019f1a2fdd24428f402609502f91e7d26629e80412031204120a12061207120872019cc420b072408f402609502f93e7d26629e80412031204120a12061207120872011a2fdd2406408f402609502f98e7d26629e80412031204120a12061207120872016f1283c0ca3f8f402609502f9be7d26629e80412031204120a120612071208720178e9263108408f402609502f9fe7d26629e80412031204120a1206120712087201e17a14ae47408f402609502fa1e7d26629e80412031204120a12061207120872014f8d976e12438f402609502fa2e7d26629e80412031204120a12061207120872010c022b8716408f402609502fa4e7d26629e80412031204120a1206120712087201931804560e498f402609502fa5e7d26629e80412031204120a1206120712087201b4c876be9f408f402609502fa6e7d26629e80412031204120a1206120712087201ec51b81e853f8f402609502fa9e7d26629e80412031204120a12061207120872014d62105839408f402609502faae7d26629e80412031204120a1206120712087201941804560e408f402609502fabe7d266293805320a027206000000d49d434440520228040d03560150447702502face7d266294e047601000006508d5b314029460472020000800cb0878c40720300000019605f734072040000805f9901824072010000006c49b18b402205160c39ee10ff22023203085204a92f050056055f5f6f67502fb4e7d26629e80412031204120a12061207120872013108ac1c5a3c8f402609502fb5e7d26629e80412031204120a1206120712087201c3f5285c8f438f402609502fb6e7d26629e80412031204120a1206120712087201cef753e3a53f8f402609502fbae7d26629e80412031204120a1206120712084201e8032609502fbfe7d26639ee10ff320208220352043d77050056055f5f6f67293805320a027206000000c4cb073440520228840e03560128447802294e0476010000e2c4d9af3040502fc0e7d26629460472020000003831e58c4072030000007062ba7340720400008070eb2082407201000080a81cfe8b402205160c39ee10ff22023203085204097c050056055f5f6f67502fc4e7d2662908137201000000804c62124086031f6170702d6174746573746174696f6e5f7265672d6174746573746174696f6e502fd0e7d26629e80412031204120a1206120712087201105839b4c81488402609502fd1e7d26629e80412031204120a1206120712087201be9f1a2fdd0988402609502fd2e7d26629e80412031204120a12061207120872010c022b87160d88402609502fd3e7d26629e80412031204120a12061207120872011804560e2d0d88402609502fd5e7d26629e80412031204120a120612071208720175931804560d88402609502fd6e7d26629e80412031204120a120612071208720154e3a59bc40c88402609502fd7e7d26629e80412031204120a12061207120872015a643bdf4f0c88402609502fd8e7d26629e80412031204120a1206120712087201bb490c022b0d88402609502fdae7d26629e80412031204120a1206120712087201ac1c5a643b0b88402609502fdbe7d26629e80412031204120a1206120712087201d9cef753e30788402609502fdce7d26629e80412031204120a120612071208720174931804560c88402609502fdde7d26629e80412031204120a1206120712087201986e1283c00c88402609502fe0e7d26629e80412031204120a120612071208720179e92631081588402609502fe1e7d26629e80412031204120a12061207120872014a0c022b871288402609502fe4e7d26629e80412031204120a1206120712087201f6285c8fc21888402609502fe5e7d26629e80412031204120a1206120712087201c420b072681488402609502fe6e7d26629e80412031204120a1206120712087201be9f1a2fdd1288402609502fe7e7d26629e80412031204120a12061207120872014260e5d0221488402609502fe9e7d26629e80412031204120a1206120712087201c420b072681488402609502feae7d26629e80412031204120a1206120712087201105839b4c81888402609502febe7d26629e80412031204120a1206120712087201d7a3703d0a1388402609502fece7d26629e80412031204120a1206120712087201c2f5285c8f6180402609502feee7d26629e80412031204120a1206120712087201be9f1a2fdd1488402609502fefe7d26629e80412031204120a120612071208720146b6f3fdd41388402609502ff0e7d26629e80412031204120a1206120712087201be9f1a2fdd1488402609502ff1e7d26629e80412031204120a12061207120872016991ed7c3f1588402609502ff8e7d26629e80412031204120a120612071208720145b6f3fdd41488402609502ff9e7d26629e80412031204120a12061207120872016de7fba9f11488402609502ffae7d26629e80412031204120a12061207120872012b8716d9ce1488402609502ffbe7d26629e80412031204120a12061207120872019f1a2fdd241588402609502ffde7d26629e80412031204120a1206120712087201508d976e121488402609502ffee7d26629e80412031204120a1206120712087201c976be9f1a1588402609502fffe7d26629e80412031204120a12061207120872017b14ae47e11488402609502f00e8d26629e80412031204120a120612071208720145b6f3fdd41488402609502f02e8d26629e80412031204120a1206120712087201e8fba9f1d21488402609502f03e8d26629e80412031204120a120612071208720145b6f3fdd41488402609502f04e8d26629e80412031204120a1206120712087201df4f8d976e1588402609502f05e8d26629e80412031204120a1206120712087201bb490c022b1588402609502f07e8d26629e80412031204120a1206120712087201fa7e6abc741588402609502f08e8d26629e80412031204120a1206120712087201cba145b6f31488402609502f09e8d26629e80412031204120a12061207120872015839b4c8761588402609502f0ae8d26629e80412031204120a1206120712087201bf9f1a2fdd1388402609502f0ce8d26629e80412031204120a1206120712087201643bdf4f8d1588402609502f0de8d26629e80412031204120a12061207120872016bbc7493181588402609502f0ee8d26629e80412031204120a12061207120872018195438b6c1588402609502f0fe8d26629e80412031204120a1206120712087201f5285c8fc21588402609502f11e8d26629e80412031204120a1206120712087201f0a7c64b371588402609502f12e8d26629e80412031204120a1206120712087201273108ac1c1488402609502f14e8d26629e80412031204120a1206120712087201caa145b6f31a88402609502f17e8d26629e80412031204120a1206120712087201f753e3a59b1288402609502f18e8d26629e80412031204120a1206120712087201894160e5d01488402609502f19e8d26629e80412031204120a12061207120872015eba490c021588402609502f1ce8d26629e80412031204120a12061207120872018fc2f5285c1388402609502f1de8d26629e80412031204120a120612071208720195438b6ce71388402609502f1ee8d26629e80412031204120a1206120712087201d7a3703d0a1588402609502f21e8d26629e80412031204120a1206120712087201ee7c3f355e1388402609502f22e8d26629e80412031204120a1206120712087201022b8716d91488402609502f23e8d26629e80412031204120a1206120712087201ae47e17a141588402609502f27e8d26629e80412031204120a12061207120872015a643bdf4f1488402609502f2ae8d26629e80412031204120a1206120712087201736891ed7c1588402609502f2be8d26629e80412031204120a1206120712087201a69bc420b01688402609502f2ce8d26629e80412031204120a12061207120872016f1283c0ca1488402609502f2de8d26629e80412031204120a1206120712087201be9f1a2fdd1488402609502f2fe8d26629e80412031204120a1206120712087201be9f1a2fdd1488402609502f30e8d26629e80412031204120a1206120712087201d122dbf97e1488402609502f31e8d266293805320a027206000000fa29a65c4052025004500356015044a902502f32e8d266294e04760100009e33ec222a402946047202000080c324258c40720300000087494a72407204000000558f1d8240720100008018b4428b402205160c39ee10ff220232030852044d3b070056055f5f6f67502f35e8d266290813720100000000dd78234086031f6170702d6174746573746174696f6e5f7265672d6174746573746174696f6e502f43e8d266293805320a027206000000809fa93240520278845203560178c4ab02502f44e8d266294e0476010000a90d747e3040294604720200000061933d8b407203000000c226ab71407204000000180e918140720100000079a1668a402205160c39ee10ff22023203085204b381070056055f5f6f67502f46e8d266290813720100000000fce7214086031f6170702d6174746573746174696f6e5f7265672d6174746573746174696f6e502f51e8d266293805320a0272060000006081112c4052022844530356012884b702294e047601000066671fa6274029460472020000803ab42c8a40720300000075686973407204000000fad81c7f407201000080b7204389402205160c502f52e8d26639ee10ff220232030852047fb6070056055f5f6f67502f53e8d26629101432012b3203174205cb0046040d01502f54e8d266290813720100000000454a214086031f6170702d6174746573746174696f6e5f7265672d6174746573746174696f6e502f55e8d26629e80412031204120a1206120712087201c1caa145b6408f402609502f59e8d26629e80412031204120a12061207120872017f6abc74933f8f402609502f5ae8d26629e80412031204120a120612071208720138b4c876be408f402609502f5be8d26629e80412031204120a1206120712087201bd749318043f8f402609502f5ee8d26629e80412031204120a12061207120872016766666666418f402609502f5fe8d26629e80412031204120a1206120712087201448b6ce7fb3f8f402609502f60e8d26629e80412031204120a1206120712087201a4703d0ad7388f402609502f62e8d26629e80412031204120a1206120712087201c520b07268388f402609502f63e8d26629e80412031204120a1206120712087201941804560e478f402609502f64e8d26629e80412031204120a120612071208720139b4c876be428f402609502f65e8d26629e80412031204120a1206120712087201d34d621058388f402609502f67e8d26629e80412031204120a120612071208720195438b6ce73c8f402609502f68e8d26629e80412031204120a1206120712087201a245b6f3fd408f402609502f69e8d26629e80412031204120a1206120712087201ec51b81e85468f402609502f6ae8d26629e80412031204120a12061207120872018b6ce7fba93b8f402609502f6de8d26629e80412031204120a12061207120872010d022b87163f8f402609502f6ee8d26629e80412031204120a1206120712087201c0caa145b63f8f402609502f6fe8d26629e80412031204120a12061207120872014b37894160408f402609502f71e8d26629e80412031204120a120612071208720154e3a59bc43d8f402609502f72e8d26629e80412031204120a12061207120872017d3f355eba418f402609502f76e8d26629e80412031204120a1206120712087201df4f8d976e3e8f402609502f77e8d26629e80412031204120a120612071208720139b4c876be3f8f402609502f78e8d26629e80412031204120a1206120712087201b7f3fdd478408f402609502f79e8d26629e80412031204120a1206120712087201c520b07268408f402609502f7ee8d26629e80412031204120a1206120712087201a01a2fdd24408f402609502f80e8d26629e80412031204120a12061207120872015b8fc2f528408f402609502f81e8d26629e80412031204120a1206120712087201b29defa7c63d8f402609502f87e8d26629e80412031204120a1206120712087201cff753e3a50778402609502f88e8d26629e80412031204120a1206120712087201448b6ce7fb3f8f402609502f8ae8d26629e80412031204120a1206120712087201976e1283c0418f402609502f8de8d26629e80412031204120a12061207120872017493180456428f402609502f8ee8d266293805320a0272060000003835844e4052027804890356017884ec02294e0476010000f131c1433440294604720200008055e3fc86407203000000abc64972407204000000ddb5d57940720100000044be0f86402205160c502f8fe8d26639ee10ff2202320308520470a4080056055f5f6f67502f93e8d26629e80412031204120a12061207120872012b8716d9ce3b8f402609502f98e8d26629e80412031204120a1206120712087201d122dbf97e408f402609502fa6e8d26629101432015232030232055c4604b0002908137201000000808494164086031f6170702d6174746573746174696f6e5f7265672d6174746573746174696f6e502fa9e8d26629e80412031204120a1206120712087201f853e3a59b907b402609502faae8d26629e80412031204120a1206120712087201c3f5285c8f3b8f402609502fabe8d26629e80412031204120a1206120712087201cccccccccc448f402609502fb0e8d26629e80412031204120a1206120712087201726891ed7c0d80402609502fb3e8d26629e80412031204120a12061207120872017b14ae47e14b84402609502fb4e8d26629e80412031204120a1206120712087201dd240681953f8f402609502fb6e8d26629e80412031204120a120612071208720147e17a14ae3f8f402609502fbbe8d26629e80412031204120a120612071208720117d9cef7533f8f402609502fbde8d26629e80412031204120a1206120712087201c0caa145b6418f402609502fc2e8d266290813720100000064a0e55f4086031d6170702d6174746573746174696f6e5f7265672d617373657274696f6e502fc4e8d26629e80412031204120a1206120712087201ad1c5a643b408f402609502fc7e8d26629e80412031204120a1206120712087201c520b072683d8f402609502fc8e8d26629e80412031204120a1206120712087201eb51b81e85438f402609502fc9e8d26629e80412031204120a1206120712087201a8c64b3789408f402609502fcbe8d266290813720100000050f568604086031d6170702d6174746573746174696f6e5f7265672d617373657274696f6e502fcce8d26629e80412031204120a120612071208720152b81e85eb8381402609502fd2e8d26629e80412031204120a1206120712087201fca9f1d24df67a402609502fd3e8d26629e80412031204120a12061207120872013bdf4f8d97fa7a402609502fd4e8d26629e80412031204120a12061207120872010c022b8716fb7a402609502fd6e8d26629e80412031204120a1206120712087201dcf97e6abcfa7a402609502fd7e8d26629e80412031204120a12061207120872018716d9cef7e77a402609502fd8e8d26629e80412031204120a12061207120872016abc749318027b402609502fdbe8d26629e80412031204120a12061207120872016991ed7c3ff97a402609502fdce8d26629e80412031204120a12061207120872018195438b6cfb7a402609502fdde8d26629e80412031204120a1206120712087201df4f8d976e007b402609502fe0e8d26629e80412031204120a12061207120872011a2fdd2406fd7a402609502fe1e8d26629e80412031204120a120612071208720154e3a59bc4f67a402609502fe2e8d26629e80412031204120a1206120712087201115839b4c8007b402609502fe6e8d26629e80412031204120a12061207120872019cc420b072fc7a402609502fe7e8d26629e80412031204120a1206120712087201941804560e0f7b402609502feae8d26629e80412031204120a1206120712087201713d0ad7a3f67a402609502febe8d26629e80412031204120a120612071208720177be9f1a2ff77a402609502fece8d26629e80412031204120a12061207120872012db29defa7c07b402609502fefe8d26629e80412031204120a1206120712087201a01a2fdd24fa7a402609502ff0e8d26629e80412031204120a12061207120872016e1283c0caf37a402609502ff1e8d26629e80412031204120a1206120712087201c3f5285c8ff87a402609502ff4e8d26629e80412031204120a1206120712087201f2d24d6210f47a402609502ff5e8d26629e80412031204120a1206120712087201c3f5285c8ff87a402609502ff6e8d26629e80412031204120a12061207120872010c022b8716197b402609502ff7e8d26629e80412031204120a120612071208720162105839b4fc7a402609502ff9e8d26629e80412031204120a1206120712087201fca9f1d24dfa7a402609502ffae8d26629e80412031204120a1206120712087201b0726891edf87a402609502ffbe8d26629e80412031204120a1206120712087201cef753e3a5f77a402609502ffee8d26629e80412031204120a1206120712087201eb51b81e85fb7a402609502fffe8d26629e80412031204120a120612071208720179e9263108f87a402609502f00e9d26629e80412031204120a1206120712087201f853e3a59bfa7a402609502f03e9d26629e80412031204120a1206120712087201ee7c3f355efc7a402609502f04e9d26629e80412031204120a120612071208720184c0caa145fa7a402609502f05e9d26629e80412031204120a1206120712087201d7a3703d0af77a402609502f08e9d26629e80412031204120a1206120712087201a245b6f3fdfa7a402609502f09e9d26629e80412031204120a1206120712087201b91e85eb51f87a402609502f0ae9d26639ec10ff220132022252031f46381032044b520574fe080032060356075f5f6f67502f0be9d26629e80412031204120a1206120712087201be9f1a2fdd7876402609502f0de9d26629081372010000000c1d7a614086031d6170702d6174746573746174696f6e5f7265672d617373657274696f6e502f12e9d26688eb0a03636c6e18fb2e29e80412031204120a12061207120872010d022b87161b79402609192e0b220252019870620b36031e192e0b2202520158dc8302360307192e0b22025201bc1c420236035a192e0b3202025201a5145b033603ff192e0b32020252017cdac7063603ff192e0b220252014390d10e2603502f13e9d26629e80412031204120a120612071208720166666666663f8f402609502f15e9d26629e80412031204120a1206120712087201986e1283c0408f402609502f17e9d26629e80412031204120a1206120712087201653bdf4f8d3f8f402609502f18e9d26629e80412031204120a12061207120872011804560e2d408f402609502f19e9d26629e80412031204120a1206120712087201fa7e6abc743f8f402609502f1ee9d26629f601220122021607502f1fe9d2662950033201047203000000d8bc8c6a40360202502f22e9d26629500332010372030000c04b0916a140360202";
        var wamData = new String(HexFormat.of().parseHex(wamBinary))
                .replace("iPhone 7", socketHandler.store().device().model().replaceAll("_", " "))
                .replace("15.7.3", socketHandler.store().device().osVersion().toString())
                .replace("2.24.17.78", socketHandler.store().version().toString())
                .getBytes();
        var addNode = Node.of("add", Map.of("t", Clock.nowSeconds()), wamData);
        return socketHandler.sendQuery("set", "w:stats", addNode);
    }

    private CompletableFuture<?> sendWam2() {
        var wamBinary = "57414d0501010001200b800d086950686f6e652037800f0631352e372e3380110a322e32342e31372e373810152017502f23e9d266206928830138790604387b060288eb0a03636c6e186b1818a71c88911e063139483330378879240431372e3418fb2e18ed3318ab3888fb3c09363335333637323531290c1476020038dd48bab4d941";
        var wamData = new String(HexFormat.of().parseHex(wamBinary))
                .replace("iPhone 7", socketHandler.store().device().model().replaceAll("_", " "))
                .replace("15.7.3", socketHandler.store().device().osVersion().toString())
                .replace("2.24.17.78", socketHandler.store().version().toString())
                .getBytes();
        var addNode = Node.of("add", Map.of("t", Clock.nowSeconds()), wamData);
        return socketHandler.sendQuery("set", "w:stats", addNode);
    }

    private CompletableFuture<?> setPushEndpoint() {
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
                .put("voip", HexFormat.of().formatHex(Bytes.random(32)))
                .put("voip_payload_type", 2)
                .toMap();
        return socketHandler.sendQuery("set", "urn:xmpp:whatsapp:push", Node.of("config", configAttributes));
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
        var notices = Stream.of("20230901", "20240729", "20230902", "20231027")
                .map(id ->  Node.of("notice", Map.of("id", id)))
                .toList();
        return socketHandler.sendQuery("get", "tos", Node.of("request", notices))
                .thenComposeAsync(ignored -> socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.of("accept")));
    }

    private void notifyChatsAndNewsletters() {
        if(socketHandler.store().clientType() != ClientType.WEB || socketHandler.keys().initialAppSync()) {
            socketHandler.onChats();
            socketHandler.onNewsletters();
        }
    }

    protected CompletableFuture<Void> queryNewsletters() {
        var query = new SubscribedNewslettersRequest(new SubscribedNewslettersRequest.Variable());
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6388546374527196"), Json.writeValueAsBytes(query)))
                .thenAcceptAsync(result -> {
                    if(socketHandler.store().webHistorySetting().hasNewsletters()) {
                        parseNewsletters(result);
                    }
                })
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(LOGIN, throwable));
    }

    private void parseNewsletters(Node result) {
        var newslettersPayload = result.findChild("result")
                .flatMap(Node::contentAsString);
        if(newslettersPayload.isEmpty()) {
            return;
        }

        var newslettersJson = SubscribedNewslettersResponse.ofJson(newslettersPayload.get())
                .orElseThrow(() -> new NoSuchElementException("Malformed newsletters payload: " + newslettersPayload));
        onNewsletters(newslettersJson);
    }

    private void onNewsletters(SubscribedNewslettersResponse result) {
        var noMessages = socketHandler.store().webHistorySetting().isZero();
        var data = result.newsletters();
        var futures = noMessages ? null : new CompletableFuture<?>[data.size()];
        for (var index = 0; index < data.size(); index++) {
            var newsletter = data.get(index);
            socketHandler.store().addNewsletter(newsletter);
            if(!noMessages) {
                futures[index] = socketHandler.queryNewsletterMessages(newsletter, DEFAULT_NEWSLETTER_MESSAGES)
                        .exceptionally(throwable -> socketHandler.handleFailure(MESSAGE, throwable));
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
        return socketHandler.sendQuery(JidServer.groupOrCommunity().toJid(), "get", "w:g2", Node.of("participating", Node.of("participants"), Node.of("description")))
                .thenAcceptAsync(this::onGroupsQuery);
    }

    private void onGroupsQuery(Node result) {
        var groups = result.findChild("groups");
        if (groups.isEmpty()) {
            return;
        }

        groups.get()
                .listChildren("group")
                .forEach(socketHandler::handleGroupMetadata);
    }

    protected CompletableFuture<Void> updateBusinessCertificate(String name) {
        var details = new BusinessVerifiedNameDetailsBuilder()
                .name(Objects.requireNonNullElse(name, socketHandler.store().name()))
                .issuer("smb:wa")
                .serial(Math.abs(ThreadLocalRandom.current().nextLong()))
                .build();
        var encodedDetails = BusinessVerifiedNameDetailsSpec.encode(details);
        var certificate = new BusinessVerifiedNameCertificateBuilder()
                .encodedDetails(encodedDetails)
                .signature(Curve25519.sign(socketHandler.keys().identityKeyPair().privateKey(), encodedDetails, true))
                .build();
        return socketHandler.sendQuery("set", "w:biz", Node.of("verified_name", Map.of("v", 2), BusinessVerifiedNameCertificateSpec.encode(certificate))).thenAccept(result -> {
            var verifiedName = result.findChild("verified_name")
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
        if(!socketHandler.keys().registered()) {
            socketHandler.keys().setRegistered(true);
            socketHandler.store().serialize(true);
            socketHandler.keys().serialize(true);
        }

        if(socketHandler.store().clientType() == ClientType.MOBILE) {
            socketHandler.store()
                    .jid()
                    .map(Jid::toSimpleJid)
                    .ifPresent(jid -> {
                        var me = new Contact(jid, socketHandler.store().name(), null, null, ContactStatus.AVAILABLE, Clock.nowSeconds(), false);
                        socketHandler.store().addContact(me);
                    });
        }

        schedulePing();
        retryConnection.set(false);
        socketHandler.onLoggedIn();
        if (socketHandler.keys().initialAppSync()) {
            socketHandler.onContacts();
        }
    }

    private CompletableFuture<Void> queryRequiredWebInfo() {
        return socketHandler.sendQuery("get", "w", Node.of("props"))
                .thenAcceptAsync(this::parseProps)
                .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
    }

    private CompletableFuture<Void> checkBusinessStatus() {
        if (!socketHandler.store().device().platform().isBusiness() || socketHandler.keys().businessCertificate()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.allOf(updateBusinessCertificate(null), setBusinessProfile())
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
        return socketHandler.store()
                .jid()
                .map(value -> socketHandler.queryAbout(value.toSimpleJid())
                        .thenAcceptAsync(result -> parseNewAbout(result.orElse(null), update)))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
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
        return socketHandler.store()
                .jid()
                .map(value -> socketHandler.queryPicture(value.toSimpleJid())
                        .thenAcceptAsync(result -> handleUserPictureChange(result.orElse(null), update)))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
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
        var privacy = result.listChildren("privacy")
                .stream()
                .flatMap(entry -> entry.children().stream())
                .map(entry -> addPrivacySetting(entry, false))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(privacy);
    }

    private void parseProps(Node result) {
        var properties = result.findChild("props")
                .stream()
                .map(entry -> entry.listChildren("prop"))
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

        socketHandler.scheduleAtFixedInterval(() -> {
            socketHandler.sendPing();
            socketHandler.store().serialize(true);
            socketHandler.store().serializer().linkMetadata(socketHandler.store());
            socketHandler.keys().serialize(true);
        }, PING_INTERVAL / 2, PING_INTERVAL);
    }

    private CompletableFuture<Void> scheduleMediaConnectionUpdate() {
        if (socketHandler.state() != SocketState.CONNECTED) {
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.sendQuery("set", "w:m", Node.of("media_conn"))
                .thenAcceptAsync(this::onMediaConnection)
                .exceptionallyAsync(throwable -> {
                    socketHandler.store().setMediaConnection(null);
                    socketHandler.handleFailure(MEDIA_CONNECTION, throwable);
                    return null;
                });
    }

    private void onMediaConnection(Node node) {
        var mediaConnection = node.findChild("media_conn").orElse(node);
        var auth = mediaConnection.attributes().getString("auth");
        var ttl = mediaConnection.attributes().getInt("ttl");
        var maxBuckets = mediaConnection.attributes().getInt("max_buckets");
        var timestamp = System.currentTimeMillis();
        var hosts = mediaConnection.listChildren("host")
                .stream()
                .map(Node::attributes)
                .map(attributes -> attributes.getString("hostname"))
                .toList();
        var result = new MediaConnection(auth, ttl, maxBuckets, timestamp, hosts);
        socketHandler.store().setMediaConnection(result);
        socketHandler.scheduleDelayed(this::scheduleMediaConnectionUpdate, result.ttl());
    }

    private void digestIq(Node node) {
        if (node.attributes().hasValue("xmlns", "urn:xmpp:ping")) {
            socketHandler.sendQueryWithNoResponse("result", null);
            return;
        }

        var container = node.findChild().orElse(null);
        if (container == null) {
            return;
        }

        switch (container.description()) {
            case "pair-device" -> startPairing(node, container);
            case "pair-success" -> confirmPairing(node, container);
        }
    }

    private CompletableFuture<?> sendPreKeys(int size) {
        var startId = socketHandler.keys().lastPreKeyId() + 1;
        var preKeys = IntStream.range(startId, startId + size)
                .mapToObj(SignalPreKeyPair::random)
                .peek(socketHandler.keys()::addPreKey)
                .map(SignalPreKeyPair::toNode)
                .toList();
        return socketHandler.sendQuery(
                "set",
                "encrypt",
                Node.of("registration", socketHandler.keys().encodedRegistrationId()),
                Node.of("type", KEY_BUNDLE_TYPE),
                Node.of("identity", socketHandler.keys().identityKeyPair().publicKey()),
                Node.of("list", preKeys),
                socketHandler.keys().signedKeyPair().toNode()
        );
    }

    private void startPairing(Node node, Node container) {
        switch (webVerificationHandler) {
            case QrHandler qrHandler -> {
                printQrCode(qrHandler, container);
                sendConfirmNode(node, null);
                schedulePing();
            }
            case PairingCodeHandler codeHandler -> askPairingCode(codeHandler)
                    .thenRun(this::schedulePing);
            default -> throw new IllegalArgumentException("Cannot verify account: unknown verification method");
        }
    }

    private CompletableFuture<Void> askPairingCode(PairingCodeHandler codeHandler) {
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
        return socketHandler.sendQuery("set", "md", registration).thenAccept(result -> {
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
        var ref = container.findChild("ref")
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
        var deviceIdentity = container.findChild("device-identity")
                .orElseThrow(() -> new NoSuchElementException("Missing device identity"));
        var advIdentity = SignedDeviceIdentityHMACSpec.decode(deviceIdentity.contentAsBytes().orElseThrow());
        var advSign = Hmac.calculateSha256(advIdentity.details(), socketHandler.keys().companionKeyPair().publicKey());
        if (!Arrays.equals(advIdentity.hmac(), advSign)) {
            socketHandler.handleFailure(LOGIN, new HmacValidationException("adv_sign"));
            return;
        }
        var account = SignedDeviceIdentitySpec.decode(advIdentity.details());
        socketHandler.keys().setCompanionIdentity(account);
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
        var device = socketHandler.store().device();
        var platform = getWebPlatform(node);
        socketHandler.store().setDevice(device.withPlatform(platform));
        sendConfirmNode(node, devicePairNode);
    }

    private UserAgent.PlatformType getWebPlatform(Node node) {
        var name = node.findChild("platform")
                .flatMap(entry -> entry.attributes().getOptionalString("name"))
                .orElse(null);
        return switch (name) {
            case "smbi" -> UserAgent.PlatformType.IOS_BUSINESS;
            case "smba" -> UserAgent.PlatformType.ANDROID_BUSINESS;
            case "android" -> UserAgent.PlatformType.ANDROID;
            case "ios" -> UserAgent.PlatformType.IOS;
            case null, default -> null;
        };
    }

    private void sendConfirmNode(Node node, Node content) {
        var attributes = Attributes.of()
                .put("id", node.id())
                .put("type", "result")
                .put("to", JidServer.whatsapp().toJid())
                .toMap();
        var request = Node.of("iq", attributes, content);
        socketHandler.sendNodeWithNoResponse(request);
    }

    private void saveCompanion(Node container) {
        var node = container.findChild("device")
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
        retries.clear();
        lastLinkCodeKey.set(null);
    }
}
