package it.auties.whatsapp.socket;

import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.SocketEvent;
import it.auties.whatsapp.binary.Sync;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaConnection;
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
        socket.errorHandler().handleFailure(reason == 401 ?
                DISCONNECTED :
                ERRONEOUS_NODE, new RuntimeException(location));
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
                        DISCONNECTED :
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
        if (!socket.state().isConnected()) {
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
                .thenRunAsync(() -> runAsyncDelayed(this::createMediaConnection, socket.store().mediaConnection().ttl()));
    }

    private void runAsyncDelayed(Runnable runnable, int seconds){
        var mediaService = CompletableFuture.delayedExecutor(seconds, TimeUnit.SECONDS);
        CompletableFuture.runAsync(runnable, mediaService);
    }

    private <T> T handleMediaConnectionError(Throwable throwable) {
        socket.store()
                .mediaConnectionLock()
                .release();
        return socket.errorHandler().handleFailure(MEDIA_CONNECTION, throwable);
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
        if (socket.keys().hasPreKeys()) {
            return;
        }

        var preKeys = IntStream.range(1, 31)
                .mapToObj(SignalPreKeyPair::random)
                .peek(socket.keys()::addPreKey)
                .map(SignalPreKeyPair::toNode)
                .toList();
        socket.sendQuery("set", "encrypt", with("registration", BytesHelper.intToBytes(socket.keys().id(), 4)),
                with("type", SignalSpecification.KEY_BUNDLE_TYPE), with("identity", socket.keys().identityKeyPair()
                        .publicKey()), withChildren("list", preKeys), socket.keys().signedKeyPair()
                        .toNode());
    }

    private void generateQrCode(Node node, Node container) {
        printQrCode(container);
        sendConfirmNode(node, null);
    }

    private void printQrCode(Node container) {
        var ref = container.findNode("ref")
                .orElseThrow(() -> new NoSuchElementException("Missing ref"));
        var qr = "%s,%s,%s,%s".formatted(new String(ref.bytes(), StandardCharsets.UTF_8), Bytes.of(
                        socket.keys().noiseKeyPair()
                                .publicKey())
                .toBase64(), Bytes.of(socket.keys().identityKeyPair()
                        .publicKey())
                .toBase64(), Bytes.of(socket.keys().companionKey())
                .toBase64());
        socket.options().qrHandler()
                .accept(qr);
    }

    @SneakyThrows
    private void confirmQrCode(Node node, Node container) {
        saveCompanion(container);

        var deviceIdentity = container.findNode("device-identity")
                .orElseThrow(() -> new NoSuchElementException("Missing device identity"));
        var advIdentity = JacksonProvider.PROTOBUF.readMessage(deviceIdentity.bytes(), SignedDeviceIdentityHMAC.class);
        var advSign = Hmac.calculateSha256(advIdentity.details(), socket.keys().companionKey());
        if (!Arrays.equals(advIdentity.hmac(), advSign)) {
            socket.errorHandler().handleFailure(LOGIN, new HmacValidationException("adv_sign"));
            return;
        }

        var account = JacksonProvider.PROTOBUF.readMessage(advIdentity.details(), SignedDeviceIdentity.class);
        var message = Bytes.of(MESSAGE_HEADER)
                .append(account.details())
                .append(socket.keys().identityKeyPair()
                        .publicKey())
                .toByteArray();
        if (!Curve25519.verifySignature(account.accountSignatureKey(), message, account.accountSignature())) {
            socket.errorHandler().handleFailure(LOGIN, new HmacValidationException("message_header"));
            return;
        }

        var deviceSignatureMessage = Bytes.of(SIGNATURE_HEADER)
                .append(account.details())
                .append(socket.keys().identityKeyPair()
                        .publicKey())
                .append(account.accountSignatureKey())
                .toByteArray();
        account.deviceSignature(Curve25519.sign(socket.keys().identityKeyPair()
                .privateKey(), deviceSignatureMessage, true));

        var keyIndex = JacksonProvider.PROTOBUF.readMessage(account.details(), DeviceIdentity.class)
                .keyIndex();
        var devicePairNode = withChildren("pair-device-sign", with("device-identity", of("key-index", keyIndex),
                JacksonProvider.PROTOBUF.writeValueAsBytes(account.withoutKey())));

        socket.keys().companionIdentity(account);
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
        socket.keys().companion(companion);
    }
}
