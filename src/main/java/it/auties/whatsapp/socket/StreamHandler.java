package it.auties.whatsapp.socket;

import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.SocketEvent;
import it.auties.whatsapp.binary.PatchType;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.exception.ErroneousNodeException;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.chat.Chat;
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
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

@RequiredArgsConstructor
@Accessors(fluent = true)
class StreamHandler implements JacksonProvider {
    private static final byte[] MESSAGE_HEADER = {6, 0};
    private static final byte[] SIGNATURE_HEADER = {6, 1};
    private static final int REQUIRED_PRE_KEYS_SIZE = 5;
    private static final int PRE_KEYS_UPLOAD_CHUNK = 30;
    private static final int PING_INTERVAL = 30;

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
                .or(() -> node.findNode()
                        .map(Node::description))
                .orElseThrow(() -> new NoSuchElementException("Missing type from %s".formatted(node)));
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
        message.status(status);
        if (participant != null) {
            message.individualStatus()
                    .put(participant, status);
        }

        socket.onMessageStatus(status, participant, message, message.chat());
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
        var receipt = ofAttributes("ack", of("class", "receipt", "id", node.id(), "from", from));
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

        var patchName = PatchType.forName(update.get()
                .attributes()
                .getRequiredString("name"));
        socket.pullPatch(patchName);
    }

    private void handleMessageNotification(Node node) {
        var body = node.findNode()
                .orElseThrow(() -> new NoSuchElementException("Missing body in notification"));
        if (body.description()
                .equals("encrypt")) {
            var chat = node.attributes()
                    .getJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing chat in notification"));
            if (!chat.isServerJid(ContactJid.Server.WHATSAPP)) {
                return;
            }

            var keysSize = node.findNode("count")
                    .flatMap(Node::contentAsLong)
                    .orElse(0L);
            if (keysSize >= REQUIRED_PRE_KEYS_SIZE) {
                return;
            }

            sendPreKeys();
        }

        var stubType = MessageInfo.StubType.forSymbol(body.description());
        if (stubType.isEmpty()) {
            return;
        }

        var timestamp = node.attributes()
                .getLong("t");
        var chat = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing chat in notification"));
        var key = MessageKey.newMessageKeyBuilder()
                .chatJid(chat)
                .build();
        var message = MessageInfo.newMessageInfo()
                .timestamp(timestamp)
                .key(key)
                .ignore(true)
                .stubType(stubType.get())
                .stubParameters(List.of())
                .build();
        var known = socket.store()
                .findChatByJid(chat)
                .orElseGet(() -> socket.createChat(chat));
        known.addMessage(message);
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
                ofAttributes("clean", of("type", type, "timestamp", timestamp)));
    }

    private void digestError(Node node) {
        var statusCode = node.attributes()
                .getInt("code");
        switch (statusCode) {
            case 515 -> socket.disconnect(true);
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
        if (!socket.keys()
                .hasPreKeys()) {
            sendPreKeys();
        }

        createPingTask();
        createMediaConnection();
        sendStatusUpdate();
        socket.onLoggedIn();
        if (!socket.store()
                .hasSnapshot()) {
            return;
        }

        socket.onChats();
        socket.onContacts();
        socket.pullInitialPatches();
    }

    private void createPingTask() {
        if (pingService != null && !pingService.isShutdown()) {
            return;
        }

        this.pingService = newSingleThreadScheduledExecutor();
        pingService.scheduleAtFixedRate(this::sendPing, PING_INTERVAL, PING_INTERVAL, TimeUnit.SECONDS);
    }

    private void sendStatusUpdate() {
        var presence = ofAttributes("presence", of("type", "available"));
        socket.sendWithNoResponse(presence);
        socket.sendQuery("get", "blocklist");
        socket.sendQuery("get", "privacy", Node.of("privacy"));
        socket.sendQuery("get", "abt", ofAttributes("props", of("protocol", "1")));
        socket.sendQuery("get", "w", Node.of("props"))
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
        if (socket.state() != SocketState.CONNECTED) {
            pingService.shutdownNow();
            return;
        }

        socket.store()
                .serialize(true);
        socket.sendQuery("get", "w:p", Node.of("ping"));
        socket.onSocketEvent(SocketEvent.PING);
    }

    @SneakyThrows
    private void createMediaConnection() {
        if (socket.state() != SocketState.CONNECTED) {
            return;
        }

        socket.sendQuery("set", "w:m", Node.of("media_conn"))
                .thenApplyAsync(MediaConnection::of)
                .thenApplyAsync(result -> socket.store()
                        .mediaConnection(result))
                .exceptionallyAsync(throwable -> socket.errorHandler()
                        .handleFailure(MEDIA_CONNECTION, throwable))
                .thenRunAsync(() -> runAsyncDelayed(this::createMediaConnection, socket.store()
                        .mediaConnection()
                        .ttl()));
    }

    private void runAsyncDelayed(Runnable runnable, int seconds) {
        var mediaService = CompletableFuture.delayedExecutor(seconds, TimeUnit.SECONDS);
        CompletableFuture.runAsync(runnable, mediaService);
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
        socket.sendQuery("set", "passive", Node.of("active"));
    }

    private void sendPreKeys() {
        var startId = socket.keys()
                .lastPreKeyId() + 1;
        var preKeys = IntStream.range(startId, startId + PRE_KEYS_UPLOAD_CHUNK)
                .mapToObj(SignalPreKeyPair::random)
                .peek(socket.keys()::addPreKey)
                .map(SignalPreKeyPair::toNode)
                .toList();
        socket.sendQuery("set", "encrypt", Node.of("registration", BytesHelper.intToBytes(socket.keys()
                .id(), 4)), Node.of("type", SignalSpecification.KEY_BUNDLE_TYPE), Node.of("identity", socket.keys()
                .identityKeyPair()
                .publicKey()), ofChildren("list", preKeys), socket.keys()
                .signedKeyPair()
                .toNode());
    }

    private void generateQrCode(Node node, Node container) {
        printQrCode(container);
        sendConfirmNode(node, null);
    }

    private void printQrCode(Node container) {
        var ref = container.findNode("ref")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing ref"));
        var qr = "%s,%s,%s,%s".formatted(ref, Bytes.of(socket.keys()
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
        var advIdentity = PROTOBUF.readMessage(deviceIdentity.contentAsBytes()
                .orElseThrow(), SignedDeviceIdentityHMAC.class);
        var advSign = Hmac.calculateSha256(advIdentity.details(), socket.keys()
                .companionKey());
        if (!Arrays.equals(advIdentity.hmac(), advSign)) {
            socket.errorHandler()
                    .handleFailure(LOGIN, new HmacValidationException("adv_sign"));
            return;
        }

        var account = PROTOBUF.readMessage(advIdentity.details(), SignedDeviceIdentity.class);
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

        var keyIndex = PROTOBUF.readMessage(account.details(), DeviceIdentity.class)
                .keyIndex();
        var devicePairNode = ofChildren("pair-device-sign",
                Node.of("device-identity", of("key-index", keyIndex), PROTOBUF.writeValueAsBytes(account.withoutKey())));

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
        var request = ofChildren("iq", attributes, content);
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

    public void dispose() {
        if (pingService == null) {
            return;
        }

        pingService.shutdownNow();
    }
}
