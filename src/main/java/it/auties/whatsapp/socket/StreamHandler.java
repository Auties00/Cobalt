package it.auties.whatsapp.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.*;
import it.auties.whatsapp.binary.BinaryPatchType;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificate;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetails;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.call.CallStatus;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.chat.GroupMetadata;
import it.auties.whatsapp.model.chat.GroupRole;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJid.Server;
import it.auties.whatsapp.model.contact.ContactJid.Type;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.info.MessageInfo.StubType;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;
import it.auties.whatsapp.model.exchange.Attributes;
import it.auties.whatsapp.model.exchange.MessageSendRequest;
import it.auties.whatsapp.model.exchange.Node;
import it.auties.whatsapp.model.exchange.ContactStatusResponse;
import it.auties.whatsapp.model.signal.auth.DeviceIdentity;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentity;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentityHMAC;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Protobuf;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import lombok.experimental.Accessors;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static it.auties.whatsapp.util.Spec.Signal.KEY_BUNDLE_TYPE;
import static it.auties.whatsapp.util.Spec.Whatsapp.ACCOUNT_SIGNATURE_HEADER;
import static it.auties.whatsapp.util.Spec.Whatsapp.DEVICE_WEB_SIGNATURE_HEADER;

@Accessors(fluent = true)
class StreamHandler {
    private static final int REQUIRED_PRE_KEYS_SIZE = 5;
    private static final int PRE_KEYS_UPLOAD_CHUNK = 30;
    private static final int PING_INTERVAL = 30;
    private static final int MEDIA_CONNECTION_DEFAULT_INTERVAL = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final SocketHandler socketHandler;
    private final WebVerificationSupport webVerificationSupport;
    private final Map<String, Integer> retries;
    private final AtomicBoolean badMac;
    private final AtomicReference<String> lastLinkCodeKey;
    private ScheduledExecutorService service;

    protected StreamHandler(SocketHandler socketHandler, WebVerificationSupport webVerificationSupport) {
        this.socketHandler = socketHandler;
        this.webVerificationSupport = webVerificationSupport;
        this.retries = new HashMap<>();
        this.badMac = new AtomicBoolean();
        this.lastLinkCodeKey = new AtomicReference<>();
    }

    protected void digest(@NonNull Node node) {
        switch (node.description()) {
            case "ack" -> digestAck(node);
            case "call" -> digestCall(node);
            case "failure" -> digestFailure(node);
            case "ib" -> digestIb(node);
            case "iq" -> digestIq(node);
            case "receipt" -> digestReceipt(node);
            case "stream:error" -> digestError(node);
            case "success" -> digestSuccess(node);
            case "message" -> socketHandler.decodeMessage(node);
            case "notification" -> digestNotification(node);
            case "presence", "chatstate" -> digestChatState(node);
            case "xmlstreamend" -> digestStreamEnd();
        }
    }

    private void digestStreamEnd() {
        if (socketHandler.state() != SocketState.CONNECTED || badMac.get()) {
            return;
        }
        socketHandler.disconnect(DisconnectReason.DISCONNECTED);
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
                    .getJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from in chat state update"));
            var participantJid = node.attributes()
                    .getJid("participant")
                    .orElse(chatJid);
            updateContactPresence(chatJid, getUpdateType(node), participantJid);
        });
    }

    private ContactStatus getUpdateType(Node node) {
        var metadata = node.findNode();
        var recording = metadata.map(entry -> entry.attributes().getString("media"))
                .filter(entry -> entry.equals("audio"))
                .isPresent();
        if(recording){
            return ContactStatus.RECORDING;
        }

        return node.attributes()
                .getOptionalString("type")
                .or(() -> metadata.map(Node::description))
                .flatMap(ContactStatus::of)
                .orElse(ContactStatus.AVAILABLE);
    }

    private void updateContactPresence(ContactJid chatJid, ContactStatus status, ContactJid contact) {
        socketHandler.store()
                .findChatByJid(chatJid)
                .ifPresent(chat -> socketHandler.onUpdateChatPresence(status, contact, chat));
    }

    private void digestReceipt(Node node) {
        var chat = node.attributes()
                .getJid("from")
                .filter(jid -> jid.type() != Type.STATUS)
                .flatMap(socketHandler.store()::findChatByJid)
                .orElse(null);
        getReceiptsMessageIds(node)
                .stream()
                .map(messageId -> chat == null ? socketHandler.store().findStatusById(messageId) : socketHandler.store().findMessageById(chat, messageId))
                .flatMap(Optional::stream)
                .forEach(message -> digestReceipt(node, chat, message));
        socketHandler.sendMessageAck(node);
    }

    private void digestReceipt(Node node, Chat chat, MessageInfo message) {
        var type = node.attributes().getOptionalString("type");
        var status = type.flatMap(MessageStatus::of)
                .orElse(MessageStatus.DELIVERED);
        var participant = node.attributes()
                .getJid("participant")
                .flatMap(socketHandler.store()::findContactByJid)
                .orElse(null);
        if(chat != null && chat.unreadMessagesCount() > 0) {
            chat.unreadMessagesCount(chat.unreadMessagesCount() - 1);
        }

        message.status(status);
        updateReceipt(status, chat, participant, message);
        socketHandler.onMessageStatus(status, participant, message, chat);
        if (Objects.equals(type.orElse(null), "retry")) {
            sendMessageRetry(message);
        }
    }

    private void sendMessageRetry(MessageInfo message) {
        if (!message.fromMe()) {
            return;
        }
        var attempts = retries.getOrDefault(message.id(), 0);
        if(attempts > MAX_ATTEMPTS){
            return;
        }
        try {
            var all = message.senderJid().device() == 0;
            socketHandler.querySessionsForcefully(message.senderJid());
            message.chat().participantsPreKeys().clear();
            var request = MessageSendRequest.builder()
                    .info(message)
                    .recipients(all ? null : List.of(message.senderJid()))
                    .force(!all)
                    .build();
            socketHandler.sendMessage(request);
        } finally {
            retries.put(message.id(), attempts + 1);
        }
    }

    private void updateReceipt(MessageStatus status, Chat chat, Contact participant, MessageInfo message) {
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
            case "terminate" -> node.attributes().hasValue("reason", "timeout") ? CallStatus.TIMED_OUT : CallStatus.REJECTED;
            case "reject" -> CallStatus.REJECTED;
            case "accept" -> CallStatus.ACCEPTED;
            default -> CallStatus.RINGING;
        };
    }

    private void digestCall(Node node) {
        socketHandler.sendMessageAck(node);
        var callNode = node.children().peekFirst();
        if (callNode == null) {
            return;
        }

        var from = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing call creator: " + callNode));
        var callId = callNode.attributes()
                .getString("call-id");
        var caller = callNode.attributes()
                .getJid("call-creator")
                .orElse(from);
        var status = getCallStatus(callNode);
        var timestamp = callNode.attributes()
                .getOptionalLong("t")
                .map(Clock::parseSeconds)
                .orElseGet(ZonedDateTime::now);
        var isOffline = callNode.attributes().hasKey("offline");
        var hasVideo = callNode.hasNode("video");
        var call = new Call(from, caller, callId, timestamp, hasVideo, status, isOffline);
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
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Cannot digest ack: missing from"));
        var match = socketHandler.store()
                .findMessageById(from, messageId);
        if (error != 0) {
            match.ifPresent(message -> message.status(MessageStatus.ERROR));
            return;
        }

        match.filter(message -> message.status().index() < MessageStatus.SERVER_ACK.index())
                .ifPresent(message -> message.status(MessageStatus.SERVER_ACK));
    }

    private void digestCallAck(Node node) {
        var relayNode = node.findNode("relay").orElse(null);
        if (relayNode == null) {
            return;
        }

        var callCreator = relayNode.attributes().getJid("call-creator").orElseThrow();
        var callId = relayNode.attributes().getString("call-id");
        relayNode.findNodes("participant")
                .stream()
                .map(entry -> entry.attributes().getJid("jid"))
                .flatMap(Optional::stream)
                .forEach(to -> sendRelay(callCreator, callId, to));
    }

    private void sendRelay(ContactJid callCreator, String callId, ContactJid to) {
        var values = new byte[][]{
                new byte[]{-105, 99, -47, -29, 13, -106},
                new byte[]{-99, -16, -53, 62, 13, -106},
                new byte[]{-99, -16, -25, 62, 13, -106},
                new byte[]{-99, -16, -5, 62, 13, -106},
                new byte[]{-71, 60, -37, 62, 13, -106}
        };
        for (var value : values) {
            var te = Node.of("te", Map.of("latency", 33554440), value);
            var relay = Node.of("relaylatency", Map.of("call-creator", callCreator, "call-id", callId), te);
            socketHandler.send(Node.of("call", Map.of("to", to), relay));
        }
    }

    private void digestNotification(Node node) {
        socketHandler.sendMessageAck(node);
        var type = node.attributes().getString("type", null);
        switch (type) {
            case "w:gp2" -> handleGroupNotification(node);
            case "server_sync" -> handleServerSyncNotification(node);
            case "account_sync" -> handleAccountSyncNotification(node);
            case "encrypt" -> handleEncryptNotification(node);
            case "picture" -> handlePictureNotification(node);
            case "registration" -> handleRegistrationNotification(node);
            case "link_code_companion_reg" -> confirmCompanionWebRegistration(node);
        }
    }

    private void confirmCompanionWebRegistration(Node node) {
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
        var random = BytesHelper.random(32);
        var linkCodeSalt = BytesHelper.random(32);
        var linkCodePairingExpanded = Hkdf.extractAndExpand(companionSharedKey, linkCodeSalt, "link_code_pairing_key_bundle_encryption_key".getBytes(StandardCharsets.UTF_8), 32);
        var encryptPayload = BytesHelper.concat(socketHandler.keys().identityKeyPair().publicKey(), primaryIdentityPublicKey, random);
        var encryptIv = BytesHelper.random(12);
        var encrypted = AesGcm.encrypt(encryptIv, encryptPayload, linkCodePairingExpanded);
        var encryptedPayload = BytesHelper.concat(linkCodeSalt, encryptIv, encrypted);
        var identitySharedKey = Curve25519.sharedKey(primaryIdentityPublicKey, socketHandler.keys().identityKeyPair().privateKey());
        var identityPayload = BytesHelper.concat(companionSharedKey, identitySharedKey, random);
        var advSecretPublicKey = Hkdf.extractAndExpand(identityPayload, "adv_secret".getBytes(StandardCharsets.UTF_8), 32);
        socketHandler.keys().companionKeyPair(new SignalKeyPair(advSecretPublicKey, socketHandler.keys().companionKeyPair().privateKey()));
        var confirmation = Node.of("link_code_companion_reg",
                Map.of("jid", phoneNumber, "stage", "companion_finish"),
                Node.of("link_code_pairing_wrapped_key_bundle", encryptedPayload),
                Node.of("companion_identity_public", socketHandler.keys().identityKeyPair().publicKey()),
                Node.of("link_code_pairing_ref", ref));
        socketHandler.sendQuery("set", "md", confirmation);
    }

    private byte[] decipherLinkPublicKey(byte[] primaryEphemeralPublicKeyWrapped) {
        try {
            var salt = Arrays.copyOfRange(primaryEphemeralPublicKeyWrapped, 0, 32);
            var secretKey = generatePairingKey(lastLinkCodeKey.get(), salt);
            var iv = Arrays.copyOfRange(primaryEphemeralPublicKeyWrapped, 32, 48);
            var payload = Arrays.copyOfRange(primaryEphemeralPublicKeyWrapped, 48, 80);
            var cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return cipher.doFinal(payload);
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot decipher link code pairing key", exception);
        }
    }

    private void handleRegistrationNotification(Node node) {
        var child = node.findNode("wa_old_registration");
        if(child.isEmpty()){
            return;
        }

        var code = child.get().attributes().getOptionalLong("code");
        if(code.isEmpty()){
            return;
        }

        socketHandler.onRegistrationCode(code.get());
    }

    private void handlePictureNotification(Node node) {
        var fromJid = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing from in notification"));
        var fromChat = socketHandler.store()
                .findChatByJid(fromJid)
                .orElseGet(() -> socketHandler.store().addNewChat(fromJid));
        var timestamp = node.attributes().getLong("t");
        if (fromChat.isGroup()) {
            addMessageForGroupStubType(fromChat, StubType.GROUP_CHANGE_ICON, timestamp, node);
            socketHandler.onGroupPictureChange(fromChat);
            return;
        }
        var fromContact = socketHandler.store().findContactByJid(fromJid).orElseGet(() -> {
            var contact = socketHandler.store().addContact(fromJid);
            socketHandler.onNewContact(contact);
            return contact;
        });
        socketHandler.onContactPictureChange(fromContact);
    }

    private void handleGroupNotification(Node node) {
        var child = node.findNode();
        if(child.isEmpty()){
            return;
        }

        var stubType = StubType.of(child.get().description());
        if(stubType.isEmpty()){
            return;
        }

        handleGroupStubNotification(node, stubType.get());
    }

    private void handleGroupStubNotification(Node node, StubType stubType) {
        var timestamp = node.attributes().getLong("t");
        var fromJid = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing chat in notification"));
        var fromChat = socketHandler.store()
                .findChatByJid(fromJid)
                .orElseGet(() -> socketHandler.store().addNewChat(fromJid));
        addMessageForGroupStubType(fromChat, stubType, timestamp, node);
    }

    private void addMessageForGroupStubType(Chat chat, StubType stubType, long timestamp, Node metadata) {
        var participantJid = metadata.attributes()
                .getJid("participant")
                .orElse(null);
        var parameters = getStubTypeParameters(metadata);
        var key = MessageKey.builder()
                .chatJid(chat.jid())
                .senderJid(participantJid)
                .build();
        var message = MessageInfo.builder()
                .timestampSeconds(timestamp)
                .key(key)
                .ignore(true)
                .stubType(stubType)
                .stubParameters(parameters)
                .senderJid(participantJid)
                .build();
        socketHandler.store().attribute(message);
        chat.addNewMessage(message);
        socketHandler.onNewMessage(message, false);
        if(participantJid == null){
            return;
        }

        handleGroupStubType(chat, stubType, participantJid);
    }

    private void handleGroupStubType(Chat chat, StubType stubType, ContactJid participantJid) {
        switch (stubType){
            case GROUP_PARTICIPANT_ADD -> chat.addParticipant(participantJid, GroupRole.USER);
            case GROUP_PARTICIPANT_REMOVE, GROUP_PARTICIPANT_LEAVE -> chat.removeParticipant(participantJid);
            case GROUP_PARTICIPANT_PROMOTE -> chat.findParticipant(participantJid)
                    .ifPresent(participant -> participant.role(GroupRole.ADMIN));
            case GROUP_PARTICIPANT_DEMOTE -> chat.removeParticipant(participantJid)
                    .ifPresent(participant -> participant.role(GroupRole.USER));
        }
    }

    private List<String> getStubTypeParameters(Node metadata) {
        try {
            var mapper = new ObjectMapper();
            var attributes  = new ArrayList<String>();
            attributes.add(mapper.writeValueAsString(metadata.attributes().toMap()));
            for(var child : metadata.children()){
                var data = child.attributes();
                if(data.isEmpty()){
                    continue;
                }

                attributes.add(mapper.writeValueAsString(data.toMap()));
            }

            return Collections.unmodifiableList(attributes);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot encode stub parameters", exception);
        }
    }

    private void handleEncryptNotification(Node node) {
        var chat = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing chat in notification"));
        if (!chat.isServerJid(ContactJid.Server.WHATSAPP)) {
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
            case "status" -> updateUserStatus(true);
            case "picture" -> updateUserPicture(true);
            case "blocklist" -> updateBlocklist(child.orElse(null));
        }
    }

    private void handleDevices(Node child) {
        var deviceHash = child.attributes().getString("dhash");
        socketHandler.store().deviceHash(deviceHash);
        var devices = child.findNodes("device")
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.attributes().getJid("jid").orElseThrow(),
                        entry -> entry.attributes().getInt("key-index"),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        var companionJid = socketHandler.store().jid().toWhatsappJid();
        var companionDevice = devices.remove(companionJid);
        devices.put(companionJid, companionDevice);
        socketHandler.store().linkedDevicesKeys(devices);
        socketHandler.onDevices(devices);
        var keyIndexListNode = child.findNode("key-index-list")
                .orElseThrow(() -> new NoSuchElementException("Missing index key node from device sync"));
        var signedKeyIndexBytes = keyIndexListNode.contentAsBytes()
                .orElseThrow(() -> new NoSuchElementException("Missing index key from device sync"));
        socketHandler.keys().signedKeyIndex(signedKeyIndexBytes);
        var signedKeyIndexTimestamp = keyIndexListNode.attributes().getLong("ts");
        socketHandler.keys().signedKeyIndexTimestamp(signedKeyIndexTimestamp);
    }

    private void updateBlocklist(Node child) {
        child.findNodes("item").forEach(this::updateBlocklistEntry);
    }

    private void updateBlocklistEntry(Node entry) {
        entry.attributes().getJid("jid").flatMap(socketHandler.store()::findContactByJid).ifPresent(contact -> {
            contact.blocked(Objects.equals(entry.attributes().getString("action"), "block"));
            socketHandler.onContactBlocked(contact);
        });
    }

    private void changeUserPrivacySetting(Node child) {
        var category = child.findNodes("category");
        category.forEach(entry -> addPrivacySetting(entry, true));
    }

    private void updateUserDisappearingMode(Node child) {
        var timer = ChatEphemeralTimer.of(child.attributes().getLong("duration"));
        socketHandler.store().newChatsEphemeralTimer(timer);
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

    private List<ContactJid> getUpdatedBlockedList(Node node, PrivacySettingEntry privacyEntry, PrivacySettingValue privacyValue) {
        if(privacyValue != PrivacySettingValue.CONTACTS_EXCEPT){
            return List.of();
        }

        var newValues = new ArrayList<>(privacyEntry.excluded());
        for (var entry : node.findNodes("user")) {
            var jid = entry.attributes()
                    .getJid("jid")
                    .orElseThrow(() -> new NoSuchElementException("Missing jid in response: %s".formatted(entry)));
            if (entry.attributes().hasValue("action", "add")) {
                newValues.add(jid);
                continue;
            }

            newValues.remove(jid);
        }
        return newValues;
    }

    private CompletableFuture<List<ContactJid>> queryPrivacyExcludedContacts(PrivacySettingType type, PrivacySettingValue value) {
        if(value != PrivacySettingValue.CONTACTS_EXCEPT){
            return CompletableFuture.completedFuture(List.of());
        }

        return socketHandler.sendQuery("get", "privacy", Node.of("privacy", Node.of("list", Map.of("name", type.data(), "value", value.data()))))
                .thenApplyAsync(this::parsePrivacyExcludedContacts);
    }

    private List<ContactJid> parsePrivacyExcludedContacts(Node result) {
        return result.findNode("privacy")
                .orElseThrow(() -> new NoSuchElementException("Missing privacy in result: %s".formatted(result)))
                .findNode("list")
                .orElseThrow(() -> new NoSuchElementException("Missing list in result: %s".formatted(result)))
                .findNodes("user")
                .stream()
                .map(user -> user.attributes().getJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    private void handleServerSyncNotification(Node node) {
        var patches = node.findNodes("collection")
                .stream()
                .map(entry -> entry.attributes().getRequiredString("name"))
                .map(BinaryPatchType::of)
                .toArray(BinaryPatchType[]::new);
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
        if (node.hasNode("bad-mac")) {
            badMac.set(true);
            socketHandler.handleFailure(CRYPTOGRAPHY, new RuntimeException("Detected a bad mac, last node: %s".formatted(socketHandler.lastNode())));
            return;
        }

        var statusCode = node.attributes().getInt("code");
        switch (statusCode) {
            case 515, 503 -> socketHandler.disconnect(DisconnectReason.RECONNECTING);
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
        node.attributes().getJid("lid")
                .ifPresent(socketHandler.store()::lid);
        socketHandler.sendQuery("set", "passive", Node.of("active"));
        if (!socketHandler.keys().hasPreKeys()) {
            sendPreKeys();
        }

        schedulePing();
        createMediaConnection(0, null);
        var loggedInFuture = queryInitialInfo()
                .thenRunAsync(this::onInitialInfo)
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(LOGIN, throwable));
        if (!socketHandler.keys().registered()) {
            queryGroups();
            return;
        }

        var chatsFuture = socketHandler.store().serializer()
                .attributeStore(socketHandler.store())
                .exceptionallyAsync(exception -> socketHandler.handleFailure(MESSAGE, exception));
        CompletableFuture.allOf(loggedInFuture, chatsFuture)
                .thenRunAsync(socketHandler::onChats);
    }

    private void queryGroups() {
        socketHandler.sendQuery(Server.GROUP.toJid(), "get", "w:g2", Node.of("participating", Node.of("participants"), Node.of("description")))
                .thenAcceptAsync(this::onGroupsQuery);
    }

    private void onGroupsQuery(Node result) {
        var groups = result.findNode("groups");
        if(groups.isEmpty()){
            return;
        }

        groups.get()
                .findNodes("group")
                .forEach(socketHandler::handleGroupMetadata);
    }

    private CompletableFuture<Node> setBusinessCertificate() {
        var details = BusinessVerifiedNameDetails.builder()
                .name("")
                .issuer("smb:wa")
                .serial(Math.abs(new SecureRandom().nextLong()))
                .build();
        var encodedDetails = Protobuf.writeMessage(details);
        var certificate = BusinessVerifiedNameCertificate.builder()
                .details(encodedDetails)
                .signature(Curve25519.sign(socketHandler.keys().identityKeyPair().privateKey(), encodedDetails, true))
                .build();
        return socketHandler.sendQuery("set", "w:biz", Node.of("verified_name", Map.of("v", 2), Protobuf.writeMessage(certificate)));
    }

    private CompletableFuture<Node> setBusinessProfile() {
        var version = socketHandler.store().properties().get("biz_profile_options");
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
                        .thenApplyAsync(entries -> Node.of("category", Map.of("id", entries.get(0).id()))));
    }

    private synchronized void schedulePing(){
        if (service != null && !service.isShutdown()) {
            return;
        }

        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(this::sendPing, PING_INTERVAL, PING_INTERVAL, TimeUnit.SECONDS);
    }

    private void onInitialInfo() {
        socketHandler.onLoggedIn();
        if (!socketHandler.keys().registered()) {
            if(socketHandler.store().clientType() == ClientType.WEB){
                socketHandler.keys().registered(true);
            }

            return;
        }

        socketHandler.onContacts();
    }

    private CompletableFuture<Void> queryInitialInfo() {
        return queryRequiredInfo()
                .thenComposeAsync(ignored -> CompletableFuture.allOf(updateSelfPresence(), queryInitialBlockList(), queryInitialPrivacySettings(), updateUserStatus(false), updateUserPicture(false)));
    }

    private CompletableFuture<Void> queryRequiredInfo() {
        return switch (socketHandler.store().clientType()) {
            case WEB -> {
                var requiredFuture = socketHandler.sendQuery("get", "w", Node.of("props"))
                        .thenAcceptAsync(this::parseProps)
                        .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
                socketHandler.sendQuery("get", "abt", Node.of("props", Map.of("protocol", "1")))
                        .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
                yield requiredFuture;
            }
            case MOBILE -> {
                var requiredFuture = socketHandler.sendQuery("get", "w", Node.of("props", Map.of("protocol", "2", "hash", "")))
                        .thenAcceptAsync(this::parseProps)
                        .thenComposeAsync(ignored -> checkBusinessStatus())
                        .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
                socketHandler.sendQuery("get", "urn:xmpp:whatsapp:push", Node.of("config", Map.of("version", 1)))
                        .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
                socketHandler.store().locale(Objects.requireNonNullElse(socketHandler.store().locale(), "en-US"));
                socketHandler.sendQuery("set", "urn:xmpp:whatsapp:dirty", Node.of("clean", Map.of("timestamp", 0, "type", "account_sync")))
                        .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
                if(socketHandler.store().business()){
                    socketHandler.sendQuery("get", "fb:thrift_iq", Map.of("smax_id", 42), Node.of("linked_accounts"))
                            .exceptionallyAsync(exception -> socketHandler.handleFailure(LOGIN, exception));
                }
                yield requiredFuture;
            }
        };
    }

    private CompletableFuture<Void> checkBusinessStatus() {
        if (!socketHandler.store().business() || socketHandler.keys().businessCertificate()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.allOf(setBusinessCertificate(), setBusinessProfile())
                .thenRunAsync(() -> socketHandler.keys().businessCertificate(true));
    }

    private CompletableFuture<Void> queryInitialPrivacySettings() {
        if(socketHandler.store().business()){
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.sendQuery("get", "privacy", Node.of("privacy"))
                .thenComposeAsync(this::parsePrivacySettings);
    }

    private CompletableFuture<Void> queryInitialBlockList() {
        return socketHandler.queryBlockList()
                .thenAcceptAsync(entry -> entry.forEach(this::markBlocked));
    }

    private CompletableFuture<Void> updateSelfPresence() {
        if(!socketHandler.store().automaticPresenceUpdates()){
            return CompletableFuture.completedFuture(null);
        }

        return socketHandler.sendWithNoResponse(Node.of("presence", Map.of("name", socketHandler.store().name(), "type", "available")))
                .thenRun(this::onPresenceUpdated)
                .exceptionally(exception -> socketHandler.handleFailure(STREAM, exception));
    }

    private void onPresenceUpdated() {
        socketHandler.store().online(true);
        socketHandler.store()
                .findContactByJid(socketHandler.store().jid().toWhatsappJid())
                .ifPresent(entry -> entry.lastKnownPresence(ContactStatus.AVAILABLE).lastSeen(ZonedDateTime.now()));
    }

    private CompletableFuture<Void> updateUserStatus(boolean update) {
        return socketHandler.queryAbout(socketHandler.store().jid().toWhatsappJid())
                .thenAcceptAsync(result -> parseNewStatus(result.orElse(null), update));
    }

    private void parseNewStatus(ContactStatusResponse result, boolean update) {
        if (result == null) {
            return;
        }
        var oldStatus = socketHandler.store().about();
        socketHandler.store().about(result.status());
        if (!update) {
            return;
        }
        socketHandler.onUserAboutChange(result.status(), oldStatus);
    }

    private CompletableFuture<Void> updateUserPicture(boolean update) {
        return socketHandler.queryPicture(socketHandler.store().jid().toWhatsappJid())
                .thenAcceptAsync(result -> handleUserPictureChange(result.orElse(null), update));
    }

    private void handleUserPictureChange(URI newPicture, boolean update) {
        var oldStatus = socketHandler.store().profilePicture().orElse(null);
        socketHandler.store().profilePicture(newPicture);
        if (!update) {
            return;
        }
        socketHandler.onUserPictureChange(newPicture, oldStatus);
    }

    private void markBlocked(ContactJid entry) {
        socketHandler.store().findContactByJid(entry).orElseGet(() -> {
            var contact = socketHandler.store().addContact(entry);
            socketHandler.onNewContact(contact);
            return contact;
        }).blocked(true);
    }

    private CompletableFuture<Void> parsePrivacySettings(Node result) {
        var privacy = result.findNode("privacy")
                .orElseThrow(() -> new NoSuchElementException("Missing privacy in response: %s".formatted(result)))
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
        socketHandler.store().properties(properties);
        socketHandler.onMetadata(properties);
    }

    private void sendPing() {
        if (socketHandler.state() != SocketState.CONNECTED) {
            return;
        }
        socketHandler.sendQueryWithNoResponse("get", "w:p", Node.of("ping"))
                        .exceptionallyAsync(throwable -> socketHandler.handleFailure(STREAM, throwable));
        socketHandler.onSocketEvent(SocketEvent.PING);
    }

    private void createMediaConnection(int tries, Throwable error) {
        if (socketHandler.state() != SocketState.CONNECTED) {
            return;
        }
        if (tries >= MAX_ATTEMPTS) {
            socketHandler.store().mediaConnection((MediaConnection) null);
            socketHandler.handleFailure(MEDIA_CONNECTION, error);
            scheduleMediaConnection(MEDIA_CONNECTION_DEFAULT_INTERVAL);
            return;
        }
        socketHandler.sendQuery("set", "w:m", Node.of("media_conn"))
                .thenApplyAsync(MediaConnection::of)
                .thenAcceptAsync(result -> {
                    socketHandler.store().mediaConnection(result);
                    scheduleMediaConnection(result.ttl());
                })
                .exceptionallyAsync(throwable -> {
                    createMediaConnection(tries + 1, throwable);
                    return null;
                });
    }

    private void scheduleMediaConnection(int seconds) {
        var executor = CompletableFuture.delayedExecutor(seconds, TimeUnit.SECONDS);
        CompletableFuture.runAsync(() -> createMediaConnection(0, null), executor);
    }

    private void digestIq(Node node) {
        var container = node.findNode().orElse(null);
        if (container == null) {
            return;
        }

        switch (container.description()){
            case "pair-device" -> generateQrCode(node, container);
            case "pair-success" -> confirmQrCode(node, container);
        }
    }

    private void sendPreKeys() {
        var startId = socketHandler.keys().lastPreKeyId() + 1;
        var preKeys = IntStream.range(startId, startId + PRE_KEYS_UPLOAD_CHUNK)
                .mapToObj(SignalPreKeyPair::random)
                .peek(socketHandler.keys()::addPreKey)
                .map(SignalPreKeyPair::toNode)
                .toList();
        socketHandler.sendQuery("set", "encrypt",
                Node.of("registration", socketHandler.keys().encodedRegistrationId()),
                Node.of("type", KEY_BUNDLE_TYPE),
                Node.of("identity", socketHandler.keys().identityKeyPair().publicKey()),
                Node.of("list", preKeys), socketHandler.keys().signedKeyPair().toNode());
    }

    private void generateQrCode(Node node, Node container) {
        if (webVerificationSupport instanceof QrHandler qrHandler){
            printQrCode(qrHandler, container);
            sendConfirmNode(node, null);
        }else if(webVerificationSupport instanceof PairingCodeHandler codeHandler) {
            askPairingCode(codeHandler);
        } else {
            throw new IllegalArgumentException("Cannot verify account: unknown verification method");
        }
    }

    private void askPairingCode(PairingCodeHandler codeHandler) {
        var code = BytesHelper.bytesToCrockford(BytesHelper.random(5));
        var registration = Node.of("link_code_companion_reg",
                Map.of("jid", getPhoneNumberAsJid(), "stage", "companion_hello", "should_show_push_notification", true),
                Node.of("link_code_pairing_wrapped_companion_ephemeral_pub", cipherLinkPublicKey(code)),
                Node.of("companion_server_auth_key_pub", socketHandler.keys().noiseKeyPair().publicKey()),
                Node.of("companion_platform_id", 49),
                Node.of("companion_platform_display", socketHandler.store().name()),
                Node.of("link_code_pairing_nonce", 0));
        socketHandler.sendQuery("set", "md", registration)
                .thenAcceptAsync(result -> onAskedPairingCode(codeHandler, code));
    }

    private byte[] cipherLinkPublicKey(String linkCodeKey) {
      try {
          var salt = BytesHelper.random(32);
          var randomIv = BytesHelper.random(16);
          var secretKey = generatePairingKey(linkCodeKey, salt);
          var cipher = Cipher.getInstance("AES/CTR/NoPadding");
          cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(randomIv));
          var ciphered = cipher.doFinal(socketHandler.keys().companionKeyPair().publicKey());
          return BytesHelper.concat(salt, randomIv, ciphered);
      }catch (GeneralSecurityException exception) {
          throw new RuntimeException("Cannot cipher link code pairing key", exception);
      }
    }

    private void onAskedPairingCode(PairingCodeHandler codeHandler, String code) {
        lastLinkCodeKey.set(code);
        codeHandler.accept(code);
    }

    private ContactJid getPhoneNumberAsJid() {
        return socketHandler.store()
                .phoneNumber()
                .map(PhoneNumber::toJid)
                .orElseThrow(() -> new IllegalArgumentException("Missing phone number while registering via OTP"));
    }

    private SecretKey generatePairingKey(String password, byte[] salt) {
       try {
           var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
           var spec = new PBEKeySpec(password.toCharArray(), salt, 2 << 16, 256);
           var tmp = factory.generateSecret(spec);
           return new SecretKeySpec(tmp.getEncoded(), "AES");
       }catch (GeneralSecurityException exception) {
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

    private void confirmQrCode(Node node, Node container) {
        saveCompanion(container);
        var deviceIdentity = container.findNode("device-identity")
                .orElseThrow(() -> new NoSuchElementException("Missing device identity"));
        var advIdentity = Protobuf.readMessage(deviceIdentity.contentAsBytes().orElseThrow(), SignedDeviceIdentityHMAC.class);
        var advSign = Hmac.calculateSha256(advIdentity.details(), socketHandler.keys().companionKeyPair().publicKey());
        if (!Arrays.equals(advIdentity.hmac(), advSign)) {
            socketHandler.handleFailure(LOGIN, new HmacValidationException("adv_sign"));
            return;
        }
        var account = Protobuf.readMessage(advIdentity.details(), SignedDeviceIdentity.class);
        var message = BytesHelper.concat(
                ACCOUNT_SIGNATURE_HEADER,
                account.details(),
                socketHandler.keys().identityKeyPair().publicKey()
        );
        if (!Curve25519.verifySignature(account.accountSignatureKey(), message, account.accountSignature())) {
            socketHandler.handleFailure(LOGIN, new HmacValidationException("message_header"));
            return;
        }
        var deviceSignatureMessage = BytesHelper.concat(
                DEVICE_WEB_SIGNATURE_HEADER,
                account.details(),
                socketHandler.keys().identityKeyPair().publicKey(),
                account.accountSignatureKey()
        );
        account.deviceSignature(Curve25519.sign(socketHandler.keys().identityKeyPair().privateKey(), deviceSignatureMessage, true));
        var keyIndex = Protobuf.readMessage(account.details(), DeviceIdentity.class).keyIndex();
        var outgoingDeviceIdentity = Protobuf.writeMessage(new SignedDeviceIdentity(account.details(), null, account.accountSignature(), account.deviceSignature()));
        var devicePairNode = Node.of("pair-device-sign",
                Node.of("device-identity", Map.of("key-index", keyIndex), outgoingDeviceIdentity));
        socketHandler.keys().companionIdentity(account);
        sendConfirmNode(node, devicePairNode);
    }

    private void sendConfirmNode(Node node, Node content) {
        var attributes = Attributes.of()
                .put("id", node.id())
                .put("type", "result")
                .put("to", Server.WHATSAPP.toJid())
                .toMap();
        var request = Node.of("iq", attributes, content);
        socketHandler.sendWithNoResponse(request);
    }

    private void saveCompanion(Node container) {
        var node = container.findNode("device")
                .orElseThrow(() -> new NoSuchElementException("Missing device"));
        var isBusiness = container.hasNode("business");
        var companion = node.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing companion"));
        socketHandler.store().jid(companion);
        socketHandler.store().phoneNumber(PhoneNumber.of(companion.user()));
        socketHandler.markConnected();
        var companionOs = container.findNode("platform")
                .map(entry -> entry.attributes().getNullableString("name"))
                .map(this::getCompanionOs)
                .orElseThrow(() -> new NoSuchElementException("Unknown platform: " + container));
        socketHandler.store().companionDeviceOs(companionOs);
        socketHandler.store().business(isBusiness);
        socketHandler.store().addContact(Contact.ofJid(socketHandler.store().jid().toWhatsappJid()));
    }

    private UserAgentPlatform getCompanionOs(String name) {
        return switch (name.toLowerCase()) {
            case "smba" -> UserAgentPlatform.SMB_ANDROID;
            case "smbi" -> UserAgentPlatform.SMB_IOS;
            case "android" -> UserAgentPlatform.ANDROID;
            case "iphone", "ipad", "ios" -> UserAgentPlatform.IOS;
            default -> null;
        };
    }

    protected void dispose() {
        retries.clear();
        if(service != null){
            service.shutdownNow();
        }
        badMac.set(false);
        lastLinkCodeKey.set(null);
    }
}
