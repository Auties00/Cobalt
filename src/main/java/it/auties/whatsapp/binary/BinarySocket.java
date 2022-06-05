package it.auties.whatsapp.binary;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.*;
import it.auties.whatsapp.api.SerializationStrategy.Event;
import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.controller.WhatsappStore;
import it.auties.whatsapp.crypto.*;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.chat.ChatMute;
import it.auties.whatsapp.model.chat.GroupMetadata;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.device.DeviceSentMessage;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.setting.EphemeralSetting;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.auth.ClientPayload.ClientPayloadBuilder;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.model.sync.RecordSync.Operation;
import it.auties.whatsapp.util.*;
import jakarta.websocket.*;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.SerializationStrategy.Event.*;
import static it.auties.whatsapp.binary.BinarySocket.ErrorLocation.*;
import static it.auties.whatsapp.model.request.Node.*;
import static jakarta.websocket.ContainerProvider.getWebSocketContainer;
import static java.lang.Long.parseLong;
import static java.lang.Runtime.getRuntime;
import static java.util.Map.of;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Collectors.*;

@Accessors(fluent = true)
@ClientEndpoint(configurator = BinarySocket.OriginPatcher.class)
@Log
public class BinarySocket implements JacksonProvider, SignalSpecification{
    private Session session;

    @NonNull
    private ScheduledExecutorService pingService;

    @NonNull
    private final AtomicBoolean loggedIn;

    @NonNull
    private final AtomicBoolean reconnecting;

    @NonNull
    private final WhatsappOptions options;

    @NonNull
    private final AuthHandler authHandler;

    @NonNull
    private final StreamHandler streamHandler;

    @NonNull
    private final MessageHandler messageHandler;

    @NonNull
    private final AppStateHandler appStateHandler;
    
    @NonNull
    private final ErrorHandler errorHandler;

    @Getter
    @NonNull
    private WhatsappKeys keys;

    @Getter
    @NonNull
    private WhatsappStore store;

    private Handshake handshake;

    private CompletableFuture<Void> loginFuture;

    static {
        getWebSocketContainer().setDefaultMaxSessionIdleTimeout(0);
    }

    public BinarySocket(@NonNull WhatsappOptions options, @NonNull WhatsappStore store, @NonNull WhatsappKeys keys) {
        this.options = options;
        this.store = store;
        this.keys = keys;
        this.authHandler = new AuthHandler();
        this.streamHandler = new StreamHandler();
        this.messageHandler = new MessageHandler();
        this.appStateHandler = new AppStateHandler();
        this.errorHandler = new ErrorHandler();
        this.pingService = newSingleThreadScheduledExecutor();
        this.loggedIn = new AtomicBoolean();
        this.reconnecting = new AtomicBoolean();
        getRuntime().addShutdownHook(new Thread(() -> serialize(ON_CLOSE)));
        serialize(PERIODICALLY, this::schedulePeriodicSerialization);
        serialize(CUSTOM);
    }

    private void schedulePeriodicSerialization(SerializationStrategy strategy) {
        store.requestsService()
                .scheduleAtFixedRate(() -> strategy.serialize(store, keys),
                        0, strategy.period(), strategy.unit());
    }

    private void serialize(Event event) {
        serialize(event, strategy -> strategy.serialize(store, keys));
    }

    private void serialize(Event event, Consumer<SerializationStrategy> strategyConsumer) {
        if(!options.serialization()){
            return;
        }

        options.serializationStrategies()
                .stream()
                .filter(strategy -> strategy.trigger() == event)
                .forEach(strategyConsumer);
    }

    @NonNull
    public Session session(){
        return session;
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(@NonNull Session session) {
        this.session = session;
        if(loggedIn.get()){
            return;
        }

        this.handshake = new Handshake(keys);
        handshake.updateHash(keys.ephemeralKeyPair().publicKey());
        var clientHello = new ClientHello(keys.ephemeralKeyPair().publicKey());
        var handshakeMessage = new HandshakeMessage(clientHello);
        Request.with(handshakeMessage)
                .sendWithPrologue(session, keys, store);
    }

    @OnMessage
    public void onBinary(byte @NonNull [] raw) {
        var message = new BinaryMessage(raw);
        if(message.decoded().isEmpty()){
            return;
        }

        var header = message.decoded().getFirst();
        if(!loggedIn.get()){
            authHandler.sendUserPayload(header.toByteArray());
            return;
        }

        message.toNodes(keys)
                .forEach(this::handleNode);
    }

    private void handleNode(Node deciphered) {
        store.resolvePendingRequest(deciphered, false);
        streamHandler.digest(deciphered);
        store.callListeners(listener -> listener.onNodeReceived(deciphered));
    }

    @SneakyThrows
    public CompletableFuture<Void> connect() {
        if(loginFuture == null || loginFuture.isDone()){
            this.loginFuture = new CompletableFuture<>();
        }

        getWebSocketContainer().connectToServer(this, URI.create(options.url()));
        return loginFuture;
    }

    @SneakyThrows
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void await(){
        pingService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    public CompletableFuture<Void> reconnect(){
        reconnecting.set(true);
        return disconnect()
                .thenComposeAsync(ignored -> connect());
    }

    @SneakyThrows
    public CompletableFuture<Void> disconnect(){
        changeState(false);
        session.close();
        return completedFuture(null); // session#close is a synchronous operation
    }

    public CompletableFuture<Void> logout(){
        if (keys.hasCompanion()) {
            var metadata = of("jid", keys.companion(), "reason", "user_initiated");
            var device = withAttributes("remove-companion-device", metadata);
            sendQuery("set", "md", device);
        }

        return disconnect()
                .thenRunAsync(this::changeKeys);
    }

    private void changeState(boolean loggedIn){
        this.loggedIn.set(loggedIn);
        keys.clear();
    }

    @OnClose
    public void onClose(){
        if(loginFuture != null && !loginFuture.isDone() && !reconnecting.get()){
            loginFuture.complete(null);
        }

        if(loggedIn.get()) {
            store.invokeListeners(listener -> listener.onDisconnected(true));
            reconnect();
            return;
        }

        store.invokeListeners(listener -> listener.onDisconnected(false));
        store.dispose();
        serialize(ON_CLOSE);
        pingService.shutdownNow();
        reconnecting.set(false);
    }

    @OnError
    public void onError(Throwable throwable){
        serialize(ON_ERROR);
        errorHandler.handleFailure(503, throwable.getMessage(), UNKNOWN, throwable);
    }

    public CompletableFuture<Node> send(Node node) {
        store.callListeners(listener -> listener.onNodeSent(node));
        return node.toRequest(node.id() == null ? store.nextTag() : null)
                .send(session, keys, store)
                .exceptionallyAsync(throwable -> errorHandler.handleFailure(503, throwable.getMessage(), ERRONEOUS_NODE, throwable));
    }

    public CompletableFuture<Void> sendWithNoResponse(Node node) {
        store.callListeners(listener -> listener.onNodeSent(node));
        return node.toRequest(node.id() == null ? store.nextTag() : null)
                .sendWithNoResponse(session, keys, store)
                .exceptionallyAsync(throwable -> errorHandler.handleFailure(503, throwable.getMessage(), UNKNOWN, throwable));
    }

    public CompletableFuture<Void> push(PatchRequest request){
        return appStateHandler.pullAndPush(request);
    }

    @SafeVarargs
    public final CompletableFuture<Node> sendMessage(MessageInfo info, Entry<String, Object>... metadata){
        return messageHandler.encode(info, metadata);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Node... body){
        return sendQuery(null, ContactJid.WHATSAPP,
                method, category, null, body);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Map<String, Object> metadata, Node... body){
        return sendQuery(null, ContactJid.WHATSAPP,
                method, category, metadata, body);
    }

    public CompletableFuture<Node> sendQuery(ContactJid to, String method, String category, Node... body){
        return sendQuery(null, to,
                method, category, null, body);
    }

    public CompletableFuture<Node> sendQuery(String id, ContactJid to, String method, String category, Map<String, Object> metadata, Node... body){
        var attributes = Attributes.of(metadata)
                .put("id", id, Objects::nonNull)
                .put("type", method)
                .put("to", to)
                .put("xmlns", category, Objects::nonNull)
                .map();
        return send(withChildren("iq", attributes, body));
    }

    public CompletableFuture<List<Node>> sendInteractiveQuery(Node queryNode, Node... queryBody) {
        var query = withChildren("query", queryNode);
        var list = withChildren("list", queryBody);
        var sync = withChildren("usync",
                of("sid", store.nextTag(), "mode", "query", "last", "true", "index", "0", "context", "interactive"),
                query, list);
        return sendQuery("get", "usync", sync)
                .thenApplyAsync(this::parseQueryResult);
    }

    private List<Node> parseQueryResult(Node result) {
        return result.findNodes("usync")
                .stream()
                .map(node -> node.findNode("list"))
                .flatMap(Optional::stream)
                .map(node -> node.findNodes("user"))
                .flatMap(Collection::stream)
                .toList();
    }

    @SneakyThrows
    public CompletableFuture<GroupMetadata> queryGroupMetadata(ContactJid group){
        var body = withAttributes("query", of("request", "interactive"));
        return sendQuery(group, "get", "w:g2", body)
                .thenApplyAsync(node -> node.findNode("group")
                        .orElseThrow(() -> new NoSuchElementException("Missing group node")))
                .thenApplyAsync(GroupMetadata::of);
    }

    private void sendSyncReceipt(MessageInfo info, String type){
        var receipt = withAttributes("receipt",
                of("to",  ContactJid.of(keys.companion().user(), ContactJid.Server.USER), "type", type, "id", info.key().id()));
        sendWithNoResponse(receipt);
    }

    private void sendReceipt(ContactJid jid, ContactJid participant, List<String> messages, String type) {
        if(messages.isEmpty()){
            return;
        }

        var attributes = Attributes.empty()
                .put("id", messages.get(0))
                .put("t", Clock.now() / 1000)
                .put("to", jid)
                .put("type", type, Objects::nonNull)
                .put("participant", participant, Objects::nonNull, value -> !Objects.equals(jid, value));
        var receipt = withChildren("receipt",
                attributes.map(), toMessagesNode(messages));
        sendWithNoResponse(receipt);
    }

    private List<Node> toMessagesNode(List<String> messages) {
        if (messages.size() <= 1) {
            return null;
        }

        return messages.subList(1, messages.size())
                .stream()
                .map(id -> withAttributes("item", of("id", id)))
                .toList();
    }

    private void sendMessageAck(Node node, Map<String, Object> metadata){
        var to = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing from in message ack"));
        var attributes = Attributes.of(metadata)
                .put("id", node.id())
                .put("to", to)
                .put("participant", node.attributes().getNullableString("participant"), Objects::nonNull)
                .map();
        var receipt = withAttributes("ack", attributes);
        sendWithNoResponse(receipt);
    }

    private void changeKeys() {
        keys.delete();
        var newId = KeyHelper.registrationId();
        this.keys = WhatsappKeys.random(newId);
        var newStore = WhatsappStore.random(newId);
        newStore.listeners().addAll(store.listeners());
        this.store = newStore;
    }

    public Contact createContact(ContactJid jid) {
        var newContact = Contact.ofJid(jid);
        store.addContact(newContact);
        return newContact;
    }

    public Chat createChat(ContactJid jid){
        var newChat = Chat.ofJid(jid);
        store.addChat(newChat);
        return newChat;
    }

    public static class OriginPatcher extends Configurator{
        @Override
        public void beforeRequest(@NonNull Map<String, List<String>> headers) {
            headers.put("Origin", List.of("https://web.whatsapp.com"));
            headers.put("Host", List.of("web.whatsapp.com"));
        }
    }

    private class AuthHandler {
        @SneakyThrows
        private void sendUserPayload(byte[] message) {
            var serverHello = PROTOBUF.readMessage(message, HandshakeMessage.class)
                    .serverHello();
            handshake.updateHash(serverHello.ephemeral());
            var sharedEphemeral = Curve25519.sharedKey(serverHello.ephemeral(), keys.ephemeralKeyPair().privateKey());
            handshake.mixIntoKey(sharedEphemeral);

            var decodedStaticText = handshake.cipher(serverHello.staticText(), false);
            var sharedStatic = Curve25519.sharedKey(decodedStaticText, keys.ephemeralKeyPair().privateKey());
            handshake.mixIntoKey(sharedStatic);
            handshake.cipher(serverHello.payload(), false);

            var encodedKey = handshake.cipher(keys.noiseKeyPair().publicKey(), true);
            var sharedPrivate = Curve25519.sharedKey(serverHello.ephemeral(), keys.noiseKeyPair().privateKey());
            handshake.mixIntoKey(sharedPrivate);

            var encodedPayload = handshake.cipher(createUserPayload(), true);
            var clientFinish = new ClientFinish(encodedKey, encodedPayload);
            var handshakeMessage = new HandshakeMessage(clientFinish);
            Request.with(handshakeMessage)
                    .sendWithNoResponse(session, keys, store)
                    .thenRunAsync(() -> changeState(true))
                    .thenRunAsync(handshake::finish);
        }

        @SneakyThrows
        private byte[] createUserPayload() {
            var builder = ClientPayload.builder()
                    .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                    .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                    .userAgent(createUserAgent())
                    .passive(true)
                    .webInfo(new WebInfo(WebInfo.WebInfoWebSubPlatform.WEB_BROWSER));
            return PROTOBUF.writeValueAsBytes(finishUserPayload(builder));
        }

        private ClientPayload finishUserPayload(ClientPayloadBuilder builder) {
            if(keys.hasCompanion()){
                return builder.username(parseLong(keys.companion().user()))
                        .device(keys.companion().device())
                        .build();
            }

            return builder.regData(createRegisterData())
                    .build();
        }

        private UserAgent createUserAgent() {
            return UserAgent.builder()
                    .appVersion(options.version())
                    .platform(UserAgent.UserAgentPlatform.WEB)
                    .releaseChannel(UserAgent.UserAgentReleaseChannel.RELEASE)
                    .build();
        }

        @SneakyThrows
        private CompanionData createRegisterData() {
            return CompanionData.builder()
                    .buildHash(options.version().toHash())
                    .companion(PROTOBUF.writeValueAsBytes(createCompanionProps()))
                    .id(BytesHelper.intToBytes(keys.id(), 4))
                    .keyType(BytesHelper.intToBytes(KEY_TYPE, 1))
                    .identifier(keys.identityKeyPair().publicKey())
                    .signatureId(keys.signedKeyPair().encodedId())
                    .signaturePublicKey(keys.signedKeyPair().keyPair().publicKey())
                    .signature(keys.signedKeyPair().signature())
                    .build();
        }

        private Companion createCompanionProps() {
            return Companion.builder()
                    .os(options.description())
                    .platformType(Companion.CompanionPropsPlatformType.DESKTOP)
                    .requireFullSync(options.historyLength() == HistoryLength.ONE_YEAR)
                    .build();
        }
    }

    private class StreamHandler {
        private static final byte[] MESSAGE_HEADER = {6, 0};
        private static final byte[] SIGNATURE_HEADER = {6, 1};

        private void digest(@NonNull Node node) {
            switch (node.description()) {
                case "ack" -> digestAck(node);
                case "call" -> digestCall(node);
                case "failure" -> digestFailure(node);
                case "ib" -> digestIb(node);
                case "iq" -> digestIq(node);
                case "receipt" -> digestReceipt(node);
                case "stream:error" -> digestError(node);
                case "success" -> digestSuccess();
                case "message" -> digestMessage(node);
                case "notification" -> digestNotification(node);
                case "presence", "chatstate" -> digestChatState(node);
            }
        }

        private void digestFailure(Node node) {
            var statusCode = node.attributes().getLong("reason");
            var reason = node.attributes().getString("location");
            errorHandler.handleFailure(statusCode, reason, ERRONEOUS_NODE);
        }

        private void digestMessage(Node node) {
            messageHandler.decode(node);
            serialize(ON_MESSAGE);
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
                    .orElseGet(() -> node.children().getFirst().description());
            var status = ContactStatus.forValue(updateType);
            store.findContactByJid(participantJid)
                    .ifPresent(contact -> updateContactPresence(chatJid, status, contact));
        }

        private void updateContactPresence(ContactJid chatJid, ContactStatus status, Contact contact) {
            contact.lastKnownPresence(status);
            contact.lastSeen(ZonedDateTime.now());
            store.findChatByJid(chatJid)
                    .ifPresent(chat -> updateChatPresence(status, contact, chat));
        }

        private void updateChatPresence(ContactStatus status, Contact contact, Chat chat) {
            chat.presences().put(contact, status);
            store.callListeners(listener -> {
                listener.onContactPresence(chat, contact, status);
                if (status != ContactStatus.PAUSED) {
                    return;
                }

                listener.onContactPresence(chat, contact, ContactStatus.AVAILABLE);
            });
        }

        private void digestReceipt(Node node) {
            var type = node.attributes().getNullableString("type");
            var status = MessageStatus.forValue(type);
            if(status != null) {
                updateMessageStatus(node, status);
            }

            var attributes = Attributes.empty()
                    .put("class", "receipt")
                    .put("type", type, Objects::nonNull);
            sendMessageAck(node, attributes.map());
        }

        private void updateMessageStatus(Node node, MessageStatus status) {
            node.attributes().getJid("from")
                    .flatMap(store::findChatByJid)
                    .ifPresent(chat -> updateMessageStatus(node, status, chat));
        }

        private void updateMessageStatus(Node node, MessageStatus status, Chat chat) {
            var participant = node.attributes().getJid("participant")
                    .flatMap(store::findContactByJid)
                    .orElse(null);
            var messageIds = Stream.ofNullable(node.findNode("list"))
                    .flatMap(Optional::stream)
                    .map(list -> list.findNodes("item"))
                    .flatMap(Collection::stream)
                    .map(item -> item.attributes().getOptionalString("id"))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
            messageIds.add(node.attributes().getRequiredString("id"));
            messageIds.stream()
                    .map(messageId -> store.findMessageById(chat, messageId))
                    .flatMap(Optional::stream)
                    .forEach(message -> updateMessageStatus(status, participant, message));
        }

        private void updateMessageStatus(MessageStatus status, Contact participant, MessageInfo message) {
            var chat = message.chat()
                    .orElseGet(() -> createChat(message.chatJid()));
            message.status(status);
            if(participant != null){
                message.individualStatus().put(participant, status);
            }

            store.callListeners(listener -> {
                if(participant == null) {
                    listener.onMessageStatus(message, status);
                }

                listener.onMessageStatus(chat, participant, message, status);
            });
        }

        private void digestCall(Node node) {
            var call = node.children().peekFirst();
            if(call == null){
                return;
            }

            sendMessageAck(node, of("class", "call", "type", call.description()));
        }

        private void digestAck(Node node) {
            var clazz = node.attributes().getString("class");
            if (!Objects.equals(clazz, "message")) {
                return;
            }

            var from = node.attributes().getJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Cannot digest ack: missing from"));
            var receipt = withAttributes("ack",
                    of("class", "receipt", "id", node.id(), "from", from));
            sendWithNoResponse(receipt);
        }

        private void digestNotification(Node node) {
            var type = node.attributes().getString("type", null);
            sendMessageAck(node, of("class", "notification", "type", type));
            if (!Objects.equals(type, "server_sync")) {
                return;
            }

            var update = node.findNode("collection");
            if (update.isEmpty()) {
                return;
            }

            var patchName = BinarySync.forName(update.get().attributes().getRequiredString("name"));
            appStateHandler.pull(patchName);
        }

        private void digestIb(Node node) {
            var dirty = node.findNode("dirty");
            if(dirty.isEmpty()){
                Validate.isTrue(!node.hasNode("downgrade_webclient"),
                        "Multi device beta is not enabled. Please enable it from Whatsapp");
                return;
            }

            var type = dirty.get().attributes().getString("type");
            if(!Objects.equals(type, "account_sync")){
                return;
            }

            var timestamp = dirty.get().attributes().getString("timestamp");
            sendQuery("set", "urn:xmpp:whatsapp:dirty",
                    withAttributes("clean", of("type", type, "timestamp", timestamp)));
        }

        private void digestError(Node node) {
            var statusCode = node.attributes().getInt("code");
            switch (statusCode) {
                case 515 -> reconnect();
                case 401 -> handleStreamError(node, statusCode);
                default -> node.children().forEach(error -> store.resolvePendingRequest(error, true));
            }
        }

        private void handleStreamError(Node node, int statusCode) {
            var child = node.children().getFirst();
            var type = child.attributes().getString("type");
            var reason = child.attributes().getString("reason", null);
            errorHandler.handleFailure(statusCode, requireNonNullElse(reason, type), STREAM);
        }

        private void digestSuccess() {
            confirmConnection();
            sendPreKeys();
            createPingTask();
            sendStatusUpdate();
            store.invokeListeners(WhatsappListener::onLoggedIn);
            loginFuture.complete(null);
            if (!store.hasSnapshot()) {
                return;
            }

            store.invokeListeners(WhatsappListener::onChats);
            store.invokeListeners(WhatsappListener::onContacts);
        }

        private void createPingTask() {
            if(pingService.isShutdown()){
                pingService = newSingleThreadScheduledExecutor();
            }

            pingService.scheduleAtFixedRate(this::sendPing, 20L, 20L, TimeUnit.SECONDS);
        }

        private void sendStatusUpdate() {
            var presence = withAttributes("presence", of("type", "available"));
            sendWithNoResponse(presence);
            sendQuery("get", "blocklist");
            sendQuery("get", "privacy", with("privacy"));
            sendQuery("get", "abt", withAttributes("props", of("protocol", "1")));
            sendQuery("get", "w", with("props"))
                    .thenAcceptAsync(this::parseProps);
        }

        private void parseProps(Node result) {
            var properties = result.findNode("props")
                    .orElseThrow(() -> new NoSuchElementException("Missing props"))
                    .findNodes("prop")
                    .stream()
                    .map(node -> Map.entry(node.attributes().getString("name"), node.attributes().getString("value")))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            store.callListeners(listener -> listener.onMetadata(properties));
        }

        private void sendPing() {
            if(!loggedIn.get()){
                pingService.shutdownNow();
                return;
            }

            sendQuery("get", "w:p", with("ping"));
        }

        @SneakyThrows
        private void createMediaConnection(){
            if(!loggedIn.get()){
                return;
            }

            sendQuery("set", "w:m", with("media_conn"))
                    .thenApplyAsync(MediaConnection::of)
                    .thenApplyAsync(this::scheduleMediaConnection)
                    .thenApplyAsync(store::mediaConnection)
                    .exceptionallyAsync(throwable -> errorHandler.handleFailure(503, throwable.getMessage(), MEDIA_CONNECTION));
        }

        private MediaConnection scheduleMediaConnection(MediaConnection connection) {
            CompletableFuture.delayedExecutor(connection.ttl(), TimeUnit.SECONDS)
                    .execute(this::createMediaConnection);
            return connection;
        }

        private void digestIq(Node node) {
            var container = node.children().peekFirst();
            if(container == null){
                return;
            }

            if (container.description().equals("pair-device")) {
                generateQrCode(node, container);
                return;
            }

            if (!container.description().equals("pair-success")) {
                return;
            }

            confirmQrCode(node, container);
        }

        private void confirmConnection() {
            sendQuery("set", "passive", with("active"))
                    .thenRunAsync(this::createMediaConnection);
        }

        private void sendPreKeys() {
            if(keys.hasPreKeys()){
                return;
            }

            var preKeys = IntStream.range(1, 31)
                    .mapToObj(SignalPreKeyPair::random)
                    .peek(keys.preKeys()::add)
                    .map(SignalPreKeyPair::toNode)
                    .toList();
            sendQuery(
                    "set",
                    "encrypt",
                    with("registration", BytesHelper.intToBytes(keys.id(), 4)),
                    with("type", KEY_BUNDLE_TYPE),
                    with("identity", keys.identityKeyPair().publicKey()),
                    withChildren("list", preKeys),
                    keys.signedKeyPair().toNode()
            );
        }

        private void generateQrCode(Node node, Node container) {
            printQrCode(container);
            sendConfirmNode(node, null);
        }

        private void printQrCode(Node container) {
            var ref = container.findNode("ref")
                    .orElseThrow(() -> new NoSuchElementException("Missing ref"));
            var qr = "%s,%s,%s,%s".formatted(
                    new String(ref.bytes(), StandardCharsets.UTF_8),
                    Bytes.of(keys.noiseKeyPair().publicKey()).toBase64(),
                    Bytes.of(keys.identityKeyPair().publicKey()).toBase64(),
                    Bytes.of(keys.companionKey()).toBase64()
            );

            if (!store.listeners().isEmpty()) {
                store.callListeners(listener -> requireNonNull(listener.onQRCode(), "Invalid QR handler").accept(qr));
                return;
            }

            QrHandler.toTerminal().accept(qr);
        }

        @SneakyThrows
        private void confirmQrCode(Node node, Node container) {
            saveCompanion(container);

            var deviceIdentity = container.findNode("device-identity")
                    .orElseThrow(() -> new NoSuchElementException("Missing device identity"));
            var advIdentity = PROTOBUF.readMessage(deviceIdentity.bytes(), SignedDeviceIdentityHMAC.class);
            var advSign = Hmac.calculateSha256(advIdentity.details(), keys.companionKey());
            if(!Arrays.equals(advIdentity.hmac(), advSign)) {
                errorHandler.handleFailure(503, "hmac_validation", LOGIN);
                return;
            }

            var account = PROTOBUF.readMessage(advIdentity.details(), SignedDeviceIdentity.class);
            var message = Bytes.of(MESSAGE_HEADER)
                    .append(account.details())
                    .append(keys.identityKeyPair().publicKey())
                    .toByteArray();
            if(!Curve25519.verifySignature(account.accountSignatureKey(), message, account.accountSignature())) {
                errorHandler.handleFailure(503, "hmac_validation", LOGIN);
                return;
            }

            var deviceSignatureMessage = Bytes.of(SIGNATURE_HEADER)
                    .append(account.details())
                    .append(keys.identityKeyPair().publicKey())
                    .append(account.accountSignatureKey())
                    .toByteArray();
            account.deviceSignature(Curve25519.sign(keys.identityKeyPair().privateKey(), deviceSignatureMessage, true));

            var keyIndex = PROTOBUF.readMessage(account.details(), DeviceIdentity.class)
                    .keyIndex();
            var devicePairNode = withChildren("pair-device-sign",
                    with("device-identity",
                            of("key-index", keyIndex),
                            PROTOBUF.writeValueAsBytes(account.withoutKey())));

            keys.companionIdentity(account);
            sendConfirmNode(node, devicePairNode);
        }

        private void sendConfirmNode(Node node, Node content) {
            var attributes = Attributes.empty()
                    .put("id", node.id())
                    .put("type", "result")
                    .put("to", ContactJid.WHATSAPP)
                    .map();
            var request = withChildren("iq", attributes, content);
            sendWithNoResponse(request);
        }

        private void saveCompanion(Node container) {
            var node = container.findNode("device")
                    .orElseThrow(() -> new NoSuchElementException("Missing device"));
            var companion = node.attributes().getJid("jid")
                    .orElseThrow(() -> new NoSuchElementException("Missing companion"));
            keys.companion(companion);
        }
    }

    private class MessageHandler {
        private static final int CACHE_EXPIRATION = 5;
        private static final Semaphore LOCK = new Semaphore(1);

        private final Cache<ContactJid, GroupMetadata> groupsCache;
        private final Cache<String, List<ContactJid>> devicesCache;
        public MessageHandler() {
            this.groupsCache = createCache();
            this.devicesCache = createCache();
        }

        private <K, V> Cache<K, V> createCache() {
            return Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRATION))
                    .build();
        }

        @SafeVarargs
        @SneakyThrows
        public final CompletableFuture<Node> encode(MessageInfo info, Entry<String, Object>... attributes) {
            LOCK.acquire();
            var encodedMessage = BytesHelper.messageToBytes(info.message());
            if (isConversation(info)) {
                var deviceMessage = DeviceSentMessage.newDeviceSentMessage(info.chatJid().toString(), info.message(), null);
                var encodedDeviceMessage = BytesHelper.messageToBytes(deviceMessage);
                var knownDevices = List.of(keys.companion().toUserJid(), info.chatJid());
                return getDevices(knownDevices, true)
                        .thenComposeAsync(allDevices -> createConversationNodes(allDevices, encodedMessage, encodedDeviceMessage))
                        .thenApplyAsync(sessions -> createEncodedMessageNode(info, sessions, null, attributes))
                        .thenComposeAsync(BinarySocket.this::send)
                        .thenApplyAsync(this::releaseMessageLock)
                        .exceptionallyAsync(this::handleMessageFailure);
            }

            var senderName = new SenderKeyName(info.chatJid().toString(), keys.companion().toSignalAddress());
            var groupBuilder = new GroupBuilder(keys);
            var signalMessage = groupBuilder.createOutgoing(senderName);
            var groupCipher = new GroupCipher(senderName, keys);
            var groupMessage = groupCipher.encrypt(encodedMessage);
            return Optional.ofNullable(groupsCache.getIfPresent(info.chatJid()))
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(() -> queryGroupMetadata(info.chatJid()))
                    .thenComposeAsync(this::getDevices)
                    .thenComposeAsync(allDevices -> createGroupNodes(info, signalMessage, allDevices))
                    .thenApplyAsync(preKeys -> createEncodedMessageNode(info, preKeys, groupMessage, attributes))
                    .thenComposeAsync(BinarySocket.this::send)
                    .thenApplyAsync(this::releaseMessageLock)
                    .exceptionallyAsync(this::handleMessageFailure);
        }

        private Node handleMessageFailure(Throwable throwable) {
            LOCK.release();
            return errorHandler.handleFailure(503, throwable.getMessage(), LOGIN, throwable);
        }

        private Node releaseMessageLock(Node node) {
            LOCK.release();
            return node;
        }

        private boolean isConversation(MessageInfo info) {
            return info.chatJid().type() == ContactJid.Type.USER
                    || info.chatJid().type() == ContactJid.Type.STATUS;
        }

        @SafeVarargs
        @SneakyThrows
        private Node createEncodedMessageNode(MessageInfo info, List<Node> preKeys, Node descriptor, Entry<String, Object>... metadata) {
            var body = new ArrayList<Node>();
            if(!preKeys.isEmpty()){
                body.add(withChildren("participants", preKeys));
            }

            if(descriptor != null){
                body.add(descriptor);
            }

            if(hasPreKeyMessage(preKeys)) {
                var identity = PROTOBUF.writeValueAsBytes(keys.companionIdentity());
                body.add(with("device-identity", identity));
            }

            var attributes = Attributes.of(metadata)
                    .put("id", info.id())
                    .put("type", "text")
                    .put("to", info.chatJid())
                    .map();
            return withChildren("message", attributes, body);
        }

        private boolean hasPreKeyMessage(List<Node> participants) {
            return participants.stream()
                    .map(Node::children)
                    .flatMap(Collection::stream)
                    .map(node -> node.attributes().getOptionalString("type"))
                    .flatMap(Optional::stream)
                    .anyMatch("pkmsg"::equals);
        }

        private CompletableFuture<List<Node>> createConversationNodes(List<ContactJid> contacts, byte[] message, byte[] deviceMessage) {
            var partitioned = contacts.stream()
                    .collect(partitioningBy(contact -> Objects.equals(contact.user(), keys.companion().user())));
            var companions = querySessions(partitioned.get(true))
                    .thenApplyAsync(ignored -> createMessageNodes(partitioned.get(true), deviceMessage));
            var others = querySessions(partitioned.get(false))
                    .thenApplyAsync(ignored -> createMessageNodes(partitioned.get(false), message));
            return companions.thenCombineAsync(others, (first, second) -> append(first, second));
        }

        @SneakyThrows
        private CompletableFuture<List<Node>> createGroupNodes(MessageInfo info, byte[] distributionMessage, List<ContactJid> participants) {
            var chat = info.chat()
                    .orElseGet(() -> createChat(info.chatJid()));
            Validate.isTrue(chat.isGroup(), "Cannot send group message to non-group");

            var missingParticipants= participants.stream()
                    .filter(participant -> !chat.participantsPreKeys().contains(participant))
                    .toList();
            if(missingParticipants.isEmpty()){
                return completedFuture(List.of());
            }

            var whatsappMessage = new SenderKeyDistributionMessage(info.chatJid().toString(), distributionMessage);
            var paddedMessage = BytesHelper.messageToBytes(whatsappMessage);
            return querySessions(missingParticipants)
                    .thenApplyAsync(ignored -> createMessageNodes(missingParticipants, paddedMessage))
                    .thenApplyAsync(results -> savePreKeys(chat, missingParticipants, results));
        }

        private List<Node> savePreKeys(Chat group, List<ContactJid> missingParticipants, List<Node> results) {
            group.participantsPreKeys().addAll(missingParticipants);
            return results;
        }

        private CompletableFuture<Void> querySessions(List<ContactJid> contacts){
            var missingSessions = contacts.stream()
                    .filter(contact -> !keys.hasSession(contact.toSignalAddress()))
                    .map(contact -> withAttributes("user", of("jid", contact, "reason", "identity")))
                    .toList();
            if(missingSessions.isEmpty()){
                return completedFuture(null);
            }

            return sendQuery("get", "encrypt", withChildren("key", missingSessions))
                    .thenAcceptAsync(this::parseSessions);
        }

        private List<Node> createMessageNodes(List<ContactJid> contacts, byte[] message) {
            return contacts.stream()
                    .map(contact -> createMessageNode(contact, message))
                    .toList();
        }

        private Node createMessageNode(ContactJid contact, byte[] message) {
            var cipher = new SessionCipher(contact.toSignalAddress(), keys);
            var encrypted = cipher.encrypt(message);
            return withChildren("to", of("jid", contact), encrypted);
        }

        private CompletableFuture<List<ContactJid>> getDevices(GroupMetadata metadata) {
            groupsCache.put(metadata.jid(), metadata);
            return getDevices(metadata.participantsJids(), false);
        }

        private CompletableFuture<List<ContactJid>> getDevices(List<ContactJid> contacts, boolean excludeSelf) {
            var partitioned = contacts.stream()
                    .collect(partitioningBy(contact -> devicesCache.asMap().containsKey(contact.user()), toUnmodifiableList()));
            var cached = partitioned.get(true)
                    .stream()
                    .map(ContactJid::user)
                    .map(devicesCache::getIfPresent)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .toList();
            var missing = partitioned.get(false);
            if (missing.isEmpty()) {
                return completedFuture(excludeSelf ? append(contacts, cached) : cached);
            }

            return queryDevices(missing, excludeSelf)
                    .thenApplyAsync(missingDevices -> excludeSelf ? append(contacts, cached, missingDevices) : append(cached, missingDevices));
        }

        @SneakyThrows
        private CompletableFuture<List<ContactJid>> queryDevices(List<ContactJid> contacts, boolean excludeSelf) {
            var contactNodes = contacts.stream()
                    .map(contact -> withAttributes("user", of("jid", contact)))
                    .toList();
            var body = withChildren("usync",
                    of("sid", store.nextTag(), "mode", "query", "last", "true", "index", "0", "context", "message"),
                    withChildren("query", withAttributes("devices", of("version", "2"))),
                    withChildren("list", contactNodes));
            return sendQuery("get", "usync", body)
                    .thenApplyAsync(result -> parseDevices(result, excludeSelf));
        }

        private List<ContactJid> parseDevices(Node node, boolean excludeSelf) {
            var results = node.children()
                    .stream()
                    .map(child -> child.findNode("list"))
                    .flatMap(Optional::stream)
                    .map(Node::children)
                    .flatMap(Collection::stream)
                    .map(entry -> parseDevice(entry, excludeSelf))
                    .flatMap(Collection::stream)
                    .toList();
            devicesCache.putAll(results.stream().collect(groupingBy(ContactJid::user)));
            return results;
        }

        private List<ContactJid> parseDevice(Node wrapper, boolean excludeSelf) {
            var jid = wrapper.attributes().getJid("jid")
                    .orElseThrow(() -> new NoSuchElementException("Missing jid for sync device"));
            return wrapper.findNode("devices")
                    .orElseThrow(() -> new NoSuchElementException("Missing devices"))
                    .findNode("device-list")
                    .orElseThrow(() -> new NoSuchElementException("Missing device list"))
                    .children()
                    .stream()
                    .map(child -> parseDeviceId(child, jid, excludeSelf))
                    .flatMap(Optional::stream)
                    .map(id -> ContactJid.ofDevice(jid.user(), id))
                    .toList();
        }

        private Optional<Integer> parseDeviceId(Node child, ContactJid jid, boolean excludeSelf) {
            var deviceId = child.attributes().getInt("id");
            return child.description().equals("device")
                    && (!excludeSelf || deviceId != 0)
                    && (!jid.user().equals(keys.companion().user()) || keys.companion().device() != deviceId)
                    && (deviceId == 0 || child.attributes().hasKey("key-index")) ? Optional.of(deviceId) : Optional.empty();
        }

        private void parseSessions(Node node) {
            node.findNode("list")
                    .orElseThrow(() -> new NoSuchElementException("Missing list: %s".formatted(node)))
                    .findNodes("user")
                    .forEach(this::parseSession);
        }

        private void parseSession(Node node) {
            Validate.isTrue(!node.hasNode("error"),
                    "Erroneous session node",
                    SecurityException.class);
            var jid = node.attributes().getJid("jid")
                    .orElseThrow(() -> new NoSuchElementException("Missing jid for session"));
            var registrationId = node.findNode("registration")
                    .map(id -> BytesHelper.bytesToInt(id.bytes(), 4))
                    .orElseThrow(() -> new NoSuchElementException("Missing id"));
            var identity = node.findNode("identity")
                    .map(Node::bytes)
                    .map(KeyHelper::withHeader)
                    .orElseThrow(() -> new NoSuchElementException("Missing identity"));
            var signedKey = node.findNode("skey")
                    .flatMap(SignalSignedKeyPair::of)
                    .orElseThrow(() -> new NoSuchElementException("Missing signed key"));
            var key = node.findNode("key")
                    .flatMap(SignalSignedKeyPair::of)
                    .orElse(null);
            var builder = new SessionBuilder(jid.toSignalAddress(), keys);
            builder.createOutgoing(
                    registrationId,
                    identity,
                    signedKey,
                    key
            );
        }

        public void decode(Node node) {
            var pushName = node.attributes().getString("notify");
            var timestamp = node.attributes().getLong("t");
            var id = node.attributes().getRequiredString("id");
            var from = node.attributes().getJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var recipient = node.attributes().getJid("recipient")
                    .orElse(from);
            var participant = node.attributes().getJid("participant")
                    .orElse(null);
            var messageBuilder = MessageInfo.newMessageInfo();
            var keyBuilder = MessageKey.newMessageKey();
            switch (from.type()){
                case USER, OFFICIAL_BUSINESS_ACCOUNT, STATUS, ANNOUNCEMENT, COMPANION -> {
                    keyBuilder.chatJid(recipient);
                    messageBuilder.senderJid(from);
                }

                case GROUP, GROUP_CALL, BROADCAST -> {
                    keyBuilder.chatJid(from);
                    messageBuilder.senderJid(requireNonNull(participant, "Missing participant in group message"));
                }

                default -> throw new IllegalArgumentException("Cannot decode message, unsupported type: %s".formatted(from.type().name()));
            }

            var key = keyBuilder.id(id).create();
            var info = messageBuilder.storeId(store.id())
                    .key(key)
                    .pushName(pushName)
                    .timestamp(timestamp)
                    .create();

            node.findNodes("enc")
                    .forEach(messageNode -> decodeMessage(info, node, messageNode));
        }

        private void decodeMessage(MessageInfo info, Node container, Node messageNode) {
            try {
                sendMessageAck(container, of("class", "receipt"));
                var encodedMessage = messageNode.bytes();
                var messageType = messageNode.attributes().getString("type");
                var buffer = decodeCipheredMessage(info, encodedMessage, messageType);
                if(buffer.isEmpty()){
                    return;
                }

                var messageContainer = BytesHelper.bytesToMessage(buffer.get());
                var message = messageContainer.content() instanceof DeviceSentMessage deviceSentMessage
                        ? MessageContainer.of(deviceSentMessage.message().content()) : messageContainer;
                info.message(message);
                handleStubMessage(info);
                Message content = info.message().content();
                if (content instanceof SenderKeyDistributionMessage distributionMessage) {
                    handleDistributionMessage(distributionMessage, info.senderJid());
                }

                if (content instanceof ProtocolMessage protocolMessage) {
                    handleProtocolMessage(info, protocolMessage, Objects.equals(container.attributes().getString("category"), "peer"));
                }

                saveMessage(info);
                sendReceipt(info.chatJid(), info.senderJid(), List.of(info.key().id()), null);
            }catch (Throwable throwable){
                errorHandler.handleFailure(503, throwable.getMessage(), MESSAGE, throwable);
            }
        }

        private void handleStubMessage(MessageInfo info) {
            if(!info.hasStub()) {
                return;
            }

            log.warning("Received stub %s with %s: unsupported!".formatted(info.stubType(), info.stubParameters()));
        }

        private Optional<byte[]> decodeCipheredMessage(MessageInfo info, byte[] message, String type) {
            try {
                return Optional.of(switch (type) {
                    case "skmsg" -> {
                        var senderName = new SenderKeyName(info.chatJid().toString(), info.senderJid().toSignalAddress());
                        var signalGroup = new GroupCipher(senderName, keys);
                        yield signalGroup.decrypt(message);
                    }

                    case "pkmsg" -> {
                        var session = new SessionCipher(info.chatJid().toSignalAddress(), keys);
                        var preKey = SignalPreKeyMessage.ofSerialized(message);
                        yield session.decrypt(preKey);
                    }

                    case "msg" -> {
                        var session = new SessionCipher(info.chatJid().toSignalAddress(), keys);
                        var signalMessage = SignalMessage.ofSerialized(message);
                        yield session.decrypt(signalMessage);
                    }

                    default -> throw new IllegalArgumentException("Unsupported encoded message type: %s".formatted(type));
                });
            }catch (Throwable throwable){
                var description = throwable instanceof SecurityException ? "hmac_validation"
                        : "%s: %s".formatted(throwable.getClass().getSimpleName(), throwable.getMessage());
                errorHandler.handleFailure(400, description, MESSAGE, throwable);
                return Optional.empty();
            }
        }

        private void saveMessage(MessageInfo info) {
            if(info.message().content() instanceof MediaMessage mediaMessage){
                mediaMessage.storeId(info.storeId());
            }

            if(info.chatJid().equals(ContactJid.STATUS_ACCOUNT)){
                store.status().add(info);
                store.callListeners(listener -> listener.onNewStatus(info));
                return;
            }

            var chat = info.chat()
                    .orElseGet(() -> createChat(info.chatJid()));
            chat.messages().add(info);
            if(info.timestamp() <= store.initializationTimeStamp()){
                return;
            }

            if(!info.ignore()) {
                chat.unreadMessages(requireNonNullElse(chat.unreadMessages(), 0) + 1);
            }

            store.callListeners(listener -> listener.onNewMessage(info));
        }

        private void handleDistributionMessage(SenderKeyDistributionMessage distributionMessage, ContactJid from) {
            var groupName = new SenderKeyName(distributionMessage.groupId(), from.toSignalAddress());
            var builder = new GroupBuilder(keys);
            var message = SignalDistributionMessage.ofSerialized(distributionMessage.data());
            builder.createIncoming(groupName, message);
        }

        @SneakyThrows
        private void handleProtocolMessage(MessageInfo info, ProtocolMessage protocolMessage, boolean peer){
            switch(protocolMessage.type()) {
                case HISTORY_SYNC_NOTIFICATION -> {
                    var compressed = Medias.download(protocolMessage.historySyncNotification(), store);
                    var decompressed = BytesHelper.deflate(compressed);
                    var history = PROTOBUF.readMessage(decompressed, HistorySync.class);

                    switch(history.syncType()) {
                        case INITIAL_BOOTSTRAP -> {
                            history.conversations().forEach(store::addChat);
                            store.hasSnapshot(true);
                            store.invokeListeners(WhatsappListener::onChats);
                        }

                        case FULL -> history.conversations().forEach(store::addChat);

                        case INITIAL_STATUS_V3 -> {
                            history.statusV3Messages()
                                    .stream()
                                    .peek(message -> message.storeId(store.id()))
                                    .forEach(store.status()::add);
                            store.invokeListeners(WhatsappListener::onStatus);
                        }

                        case RECENT -> history.conversations()
                                .forEach(this::handleRecentMessage);

                        case PUSH_NAME -> {
                            history.pushNames()
                                    .forEach(this::handNewPushName);
                            store.invokeListeners(WhatsappListener::onContacts);
                        }
                    }

                    sendSyncReceipt(info, "hist_sync");
                }

                case APP_STATE_SYNC_KEY_SHARE -> {
                    keys.addAppKeys(protocolMessage.appStateSyncKeyShare().keys());
                    appStateHandler.pull(BinarySync.values());
                }

                case REVOKE -> {
                    var chat = info.chat()
                            .orElseGet(() -> createChat(info.chatJid()));
                    store.findMessageById(chat, protocolMessage.key().id())
                            .ifPresent(message -> {
                                chat.messages().remove(message);
                                store.callListeners(listener -> listener.onMessageDeleted(message, true));
                            });
                }

                case EPHEMERAL_SETTING -> {
                    var chat = info.chat()
                            .orElseGet(() -> createChat(info.chatJid()));
                    chat.ephemeralMessagesToggleTime(info.timestamp())
                            .ephemeralMessageDuration(ChatEphemeralTimer.forSeconds(protocolMessage.ephemeralExpiration()));
                    var setting = new EphemeralSetting(info.ephemeralDuration(), info.timestamp());
                    store.callListeners(listener -> listener.onSetting(setting));
                }
            }

            if (!peer) {
                return;
            }

            sendSyncReceipt(info, "peer_msg");
        }

        private void handNewPushName(PushName pushName) {
            var jid = ContactJid.of(pushName.id());
            store.findContactByJid(jid)
                    .orElseGet(() -> createContact(jid))
                    .chosenName(pushName.name());
            var action = new ContactAction(pushName.name(), null);
            store.callListeners(listener -> listener.onAction(action));
        }

        private void handleRecentMessage(Chat recent) {
            var oldChat = store.findChatByJid(recent.jid());
            if (oldChat.isEmpty()) {
                store.addChat(recent);
                return;
            }

            recent.messages()
                    .stream()
                    .peek(message -> message.storeId(store.id()))
                    .forEach(oldChat.get().messages()::add);
            store.callListeners(listener -> listener.onChatRecentMessages(oldChat.get()));
        }

        @SafeVarargs
        private <T> List<T> append(List<T>... all) {
            return Stream.of(all)
                    .flatMap(Collection::stream)
                    .toList();
        }
    }

    private class AppStateHandler {
        private static final Semaphore SEMAPHORE = new Semaphore(1);
        private static final int MAX_SYNC_ATTEMPTS = 5;

        public CompletableFuture<Void> pullAndPush(@NonNull PatchRequest patch) {
            return pull(patch.type())
                    .thenComposeAsync(ignored -> push(patch));
        }

        @SneakyThrows
        public CompletableFuture<Void> push(PatchRequest patch){
            try {
                SEMAPHORE.acquire();
                var type = patch.type().toString();
                var index = patch.index().getBytes(StandardCharsets.UTF_8);
                var key = requireNonNull(keys.appStateKeys().peekLast(), "No keys available for push");
                var oldState = keys.findHashStateByName(type);
                var newState = oldState.copy();
                var actionData = ActionDataSync.builder()
                        .index(index)
                        .value(patch.sync())
                        .padding(new byte[0])
                        .version(patch.version())
                        .build();
                var encoded = PROTOBUF.writeValueAsBytes(actionData);
                var mutationKeys = MutationKeys.of(key.keyData().keyData());
                var encrypted = AesCbc.encryptAndPrefix(encoded, mutationKeys.encKey());
                var valueMac = generateMac(patch.operation(), encrypted, key.keyId().keyId(), mutationKeys.macKey());
                var indexMac = Hmac.calculateSha256(index, mutationKeys.indexKey());

                var generator = new LTHash(newState);
                generator.mix(indexMac, valueMac, patch.operation());
                var result = generator.finish();
                newState.hash(result.hash());
                newState.indexValueMap(result.indexValueMap());
                newState.version(newState.version() + 1);

                var snapshotMac = generateSnapshotMac(newState.hash(), newState.version(),
                        type, mutationKeys.snapshotMacKey());
                var syncId = new KeyId(key.keyId().keyId());
                var patchMac = generatePatchMac(snapshotMac, Bytes.of(valueMac),
                        newState.version(), type, mutationKeys.patchMacKey());
                var record = RecordSync.builder()
                        .index(new IndexSync(indexMac))
                        .value(new ValueSync(Bytes.of(encrypted, valueMac).toByteArray()))
                        .keyId(syncId)
                        .build();
                var mutation = MutationSync.builder()
                        .operation(patch.operation())
                        .record(record)
                        .build();
                var sync = PatchSync.builder()
                        .patchMac(patchMac)
                        .snapshotMac(snapshotMac)
                        .keyId(syncId)
                        .mutations(List.of(mutation))
                        .build();
                newState.indexValueMap().put(Bytes.of(indexMac).toBase64(), valueMac);

                var body = withChildren("collection",
                        of("name", patch.type(), "version", newState.version() - 1, "return_snapshot", false),
                        with("patch", PROTOBUF.writeValueAsBytes(sync)));
                return sendQuery("set", "w:sync:app:state", withChildren("sync", body))
                        .thenRunAsync(() -> keys.hashStates().put(type, newState))
                        .thenRunAsync(() -> sync.version(new VersionSync(newState.version())))
                        .thenRunAsync(() -> parseSyncRequest(type, oldState.copy(), sync))
                        .thenRunAsync(SEMAPHORE::release)
                        .exceptionallyAsync(this::handleSyncError);
            }catch (Throwable throwable) {
                SEMAPHORE.release();
                throw new RuntimeException("Cannot push patch", throwable);
            }
        }

        private void parseSyncRequest(String type, LTHashState state, PatchSync sync) {
            decodePatch(type, state.version(), state, sync)
                    .stream()
                    .map(MutationsRecord::records)
                    .flatMap(Collection::stream)
                    .forEach(this::processSyncActions);
        }

        @SneakyThrows
        private CompletableFuture<Void> pull(BinarySync... syncs) {
            try {
                SEMAPHORE.acquire();
                var states = Arrays.stream(syncs)
                        .map(LTHashState::new)
                        .peek(state -> keys.hashStates().put(state.name(), state))
                        .toList();

                var nodes = states.stream()
                        .map(LTHashState::toNode)
                        .toList();

                var request = withChildren("iq",
                        of("id", store.nextTag(), "to", ContactJid.WHATSAPP, "xmlns", "w:sync:app:state", "type", "set"),
                        withChildren("sync", nodes));
                return send(request)
                        .thenApplyAsync(this::parseSyncRequest)
                        .thenApplyAsync(this::parsePatches)
                        .thenAcceptAsync(actions -> actions.forEach(this::processSyncActions))
                        .thenRunAsync(SEMAPHORE::release)
                        .exceptionallyAsync(this::handleSyncError);
            }catch (Throwable throwable) {
                SEMAPHORE.release();
                throw new RuntimeException("Cannot pull patches", throwable);
            }
        }

        private Void handleSyncError(Throwable exception) {
            SEMAPHORE.release();
            return errorHandler.handleFailure(503, exception.getMessage(), APP_STATE_SYNC, exception);
        }

        private List<ActionDataSync> parsePatches(List<SnapshotSyncRecord> patches) {
            return patches.stream()
                    .map(patch -> parsePatch(patch, 0, null))
                    .flatMap(Collection::stream)
                    .toList();
        }

        private List<ActionDataSync> parsePatch(SnapshotSyncRecord patch, int tries, Throwable previousException) {
            try {
                var results = new ArrayList<ActionDataSync>();
                if (patch.hasSnapshot()) {
                    var decodedSnapshot = decodeSnapshot(patch.name(), patch.snapshot());
                    results.addAll(decodedSnapshot.records());
                }

                if (patch.hasPatches()) {
                    decodePatches(patch.name(), patch.patches())
                            .stream()
                            .map(MutationsRecord::records)
                            .forEach(results::addAll);
                }

                return results;
            } catch (Throwable throwable) {
                var actualThrowable = createPatchException(throwable, previousException);
                keys.hashStates().put(patch.name(), new LTHashState(BinarySync.forName(patch.name())));
                if (tries > MAX_SYNC_ATTEMPTS) {
                    throw new RuntimeException("Cannot parse patch", actualThrowable);
                }

                return parsePatch(patch, tries + 1, actualThrowable);
            }
        }

        @SuppressWarnings("ThrowableNotThrown")
        private Throwable createPatchException(Throwable current, Throwable previous){
            if(current == null){
                return previous;
            }

            if(previous == null){
                return current;
            }

            var innerException = createPatchException(current.getCause(), current);
            innerException.initCause(previous);
            return current;
        }

        private List<SnapshotSyncRecord> parseSyncRequest(Node node) {
            return node.findNode("sync")
                    .map(sync -> sync.findNodes("collection"))
                    .stream()
                    .flatMap(Collection::stream)
                    .map(this::parseSync)
                    .toList();
        }

        private SnapshotSyncRecord parseSync(Node sync) {
            var name = sync.attributes().getString("name");
            var more = sync.attributes().getBool("has_more_patches");
            var snapshotSync = sync.findNode("snapshot")
                    .map(this::decodeSnapshot)
                    .orElse(null);
            var patches = decodePatches(sync);
            return new SnapshotSyncRecord(name, snapshotSync, patches, more);
        }

        @SneakyThrows
        private SnapshotSync decodeSnapshot(Node snapshot)  {
            if(snapshot == null){
                return null;
            }

            var blob = PROTOBUF.readMessage(snapshot.bytes(), ExternalBlobReference.class);
            var syncedData = Medias.download(blob, store);
            return PROTOBUF.readMessage(syncedData, SnapshotSync.class);
        }

        private List<PatchSync> decodePatches(Node sync) {
            var versionCode = sync.attributes().getInt("version");
            return sync.findNode("patches")
                    .orElse(sync)
                    .findNodes("patch")
                    .stream()
                    .map(patch -> decodePatch(patch, versionCode))
                    .flatMap(Optional::stream)
                    .toList();
        }

        @SneakyThrows
        private Optional<PatchSync> decodePatch(Node patch, long versionCode) {
            if (!patch.hasContent()) {
                return Optional.empty();
            }

            var patchSync = PROTOBUF.readMessage(patch.bytes(), PatchSync.class);
            if (!patchSync.hasVersion()) {
                var version = new VersionSync(versionCode + 1);
                patchSync.version(version);
            }

            return Optional.of(patchSync);
        }

        private void processSyncActions(ActionDataSync mutation) {
            var value = mutation.value();
            if(value == null){
                return;
            }

            var action = value.action();
            if (action != null){
                var jid = ContactJid.of(mutation.messageIndex().chatJid());
                var targetContact = store.findContactByJid(jid);
                var targetChat = store.findChatByJid(jid);
                var targetMessage = targetChat.flatMap(chat -> store.findMessageById(chat, mutation.messageIndex().messageId()));
                switch (action) {
                    case AndroidUnsupportedActions ignored -> {}
                    case ClearChatAction ignored -> targetChat.map(Chat::messages).ifPresent(SortedMessageList::clear);
                    case ContactAction contactAction -> updateName(targetContact.orElseGet(() -> createContact(jid)), targetChat.orElseGet(() -> createChat(jid)), contactAction);
                    case DeleteChatAction ignored -> targetChat.ifPresent(store.chats()::remove);
                    case DeleteMessageForMeAction ignored -> targetMessage.ifPresent(message -> targetChat.ifPresent(chat -> deleteMessage(message, chat)));
                    case MarkChatAsReadAction markAction -> targetChat.ifPresent(chat -> chat.unreadMessages(markAction.read() ? 0 : -1));
                    case MuteAction muteAction -> targetChat.ifPresent(chat -> chat.mute(ChatMute.muted(muteAction.muteEndTimestamp())));
                    case PinAction pinAction -> targetChat.ifPresent(chat -> chat.pinned(pinAction.pinned() ? mutation.value().timestamp() : 0));
                    case StarAction starAction -> targetMessage.ifPresent(message -> message.starred(starAction.starred()));
                    case ArchiveChatAction archiveChatAction -> targetChat.ifPresent(chat -> chat.archived(archiveChatAction.archived()));
                    default -> log.info("Unsupported sync: " + mutation.value().action());
                }

                store.callListeners(listener -> listener.onAction(action));
            }

            var setting = value.setting();
            if(setting != null){
                store.callListeners(listener -> listener.onSetting(setting));
            }

            var features = mutation.value().primaryFeature();
            if(features != null && !features.flags().isEmpty()){
                store.callListeners(listener -> listener.onFeatures(features.flags()));
            }
        }

        private void updateName(Contact contact, Chat chat, ContactAction contactAction) {
            if(contact != null) {
                contact.fullName(contactAction.fullName());
                contact.shortName(contactAction.firstName());
            }

            if(chat != null) {
                var name = requireNonNullElse(contactAction.firstName(), contactAction.fullName());
                chat.name(name);
            }
        }

        private void deleteMessage(MessageInfo message, Chat chat) {
            chat.messages().remove(message);
            store.callListeners(listener -> listener.onMessageDeleted(message, false));
        }

        private List<MutationsRecord> decodePatches(String name, List<PatchSync> patches) {
            var oldState = keys.findHashStateByName(name);
            var newState = oldState.copy();
            var result = patches.stream()
                    .map(patch -> decodePatch(name, oldState.version(), newState, patch))
                    .flatMap(Optional::stream)
                    .toList();
            keys.hashStates().put(name, newState);
            return result;
        }

        @SneakyThrows
        private Optional<MutationsRecord> decodePatch(String name, long minimumVersionNumber, LTHashState newState, PatchSync patch) {
            if(patch.hasExternalMutations()) {
                var blob = Medias.download(patch.externalMutations(), store);
                var mutationsSync = PROTOBUF.readMessage(blob, MutationsSync.class);
                patch.mutations().addAll(mutationsSync.mutations());
            }

            newState.version(patch.version());
            if(!Arrays.equals(calculateSyncMac(patch, name), patch.patchMac())){
                errorHandler.handleFailure(400, "sync_mac", APP_STATE_SYNC);
                return Optional.empty();
            }

            var records = patch.mutations()
                    .stream()
                    .collect(Collectors.toMap(MutationSync::record, MutationSync::operation));
            var mutations = decodeMutations(records, newState);
            newState.hash(mutations.hash());
            newState.indexValueMap(mutations.indexValueMap());
            if(!Arrays.equals(generatePatchMac(name, newState, patch), patch.snapshotMac())){
                errorHandler.handleFailure(400, "patch_mac", APP_STATE_SYNC);
                return Optional.empty();
            }

            return Optional.of(mutations)
                    .filter(ignored ->  patch.version() == 0|| patch.version() > minimumVersionNumber);
        }

        private byte[] generatePatchMac(String name, LTHashState newState, PatchSync patch) {
            var appStateSyncKey = keys.findAppKeyById(patch.keyId().id())
                    .orElseThrow(() -> new NoSuchElementException("No keys available for mutation"));
            var mutationKeys = MutationKeys.of(appStateSyncKey.keyData().keyData());
            return generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.snapshotMacKey());
        }

        private byte[] calculateSyncMac(PatchSync sync, String name) {
            var appStateSyncKey = keys.findAppKeyById(sync.keyId().id())
                    .orElseThrow(() -> new NoSuchElementException("No keys available for mutation"));
            var mutationKeys = MutationKeys.of(appStateSyncKey.keyData().keyData());
            var mutationMacs = sync.mutations()
                    .stream()
                    .map(mutation -> mutation.record().value().blob())
                    .map(Bytes::of)
                    .map(binary -> binary.slice(-KEY_LENGTH))
                    .reduce(Bytes.newBuffer(), Bytes::append);
            return generatePatchMac(sync.snapshotMac(), mutationMacs, sync.version(), name, mutationKeys.patchMacKey());
        }

        private MutationsRecord decodeSnapshot(String name, SnapshotSync snapshot) {
            var newState = new LTHashState(BinarySync.forName(name), snapshot.version().version());
            var records = snapshot.records()
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), ignored -> Operation.SET));
            var mutations = decodeMutations(records, newState);
            newState.hash(mutations.hash());
            newState.indexValueMap(mutations.indexValueMap());
            if(!Arrays.equals(snapshot.mac(), computeSnapshotMac(name, snapshot, newState))){
                errorHandler.handleFailure(400, "decode_snapshot", APP_STATE_SYNC);
                return mutations;
            }

            var oldState = keys.findHashStateByName(name);
            var required = oldState.version() == 0 || newState.version() > oldState.version();
            if(!required){
                mutations.records().clear();
            }

            keys.hashStates().put(name, newState);
            return mutations;
        }

        private byte[] computeSnapshotMac(String name, SnapshotSync snapshot, LTHashState newState) {
            var encryptedKey = keys.findAppKeyById(snapshot.keyId().id())
                    .orElseThrow(() -> new NoSuchElementException("No keys available for mutation"));
            var mutationKeys = MutationKeys.of(encryptedKey.keyData().keyData());
            return generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.snapshotMacKey());
        }

        private MutationsRecord decodeMutations(Map<RecordSync, Operation> syncs, LTHashState initialState) {
            var generator = new LTHash(initialState);
            var mutations = syncs.keySet()
                    .stream()
                    .map(mutation -> decodeMutation(syncs.get(mutation), mutation, generator))
                    .toList();
            var result = generator.finish();
            return new MutationsRecord(result.hash(), result.indexValueMap(), mutations);
        }

        @SneakyThrows
        private ActionDataSync decodeMutation(Operation operation, RecordSync sync, LTHash generator) {
            var appStateSyncKey = keys.findAppKeyById(sync.keyId().id())
                    .orElseThrow(() -> new NoSuchElementException("No keys available for mutation"));
            var mutationKeys = MutationKeys.of(appStateSyncKey.keyData().keyData());

            var blob = Bytes.of(sync.value().blob());
            var encryptedBlob = blob.cut(-KEY_LENGTH).toByteArray();
            var encryptedMac = blob.slice(-KEY_LENGTH).toByteArray();
            if(!Arrays.equals(generateMac(operation, encryptedBlob, sync.keyId().id(), mutationKeys.macKey()), encryptedMac)){
                errorHandler.handleFailure(400, "decode_mutation", APP_STATE_SYNC);
                throw new RuntimeException("Cannot decode mutation: hmc validation failed");
            }

            var result = AesCbc.decrypt(encryptedBlob, mutationKeys.encKey());
            var actionSync = PROTOBUF.readMessage(result, ActionDataSync.class);
            if(!Arrays.equals(sync.index().blob(), Hmac.calculateSha256(actionSync.index(), mutationKeys.indexKey()))){
                errorHandler.handleFailure(400, "decode_mutation", APP_STATE_SYNC);
                throw new RuntimeException("Cannot decode mutation: hmc validation failed");
            }

            generator.mix(sync.index().blob(), encryptedMac, operation);
            return actionSync;
        }

        private byte[] generateMac(Operation operation, byte[] data, byte[] keyId, byte[] key) {
            var keyData = Bytes.of(operation.content())
                    .append(keyId)
                    .toByteArray();

            var last = Bytes.newBuffer(MAC_LENGTH - 1)
                    .append(keyData.length)
                    .toByteArray();

            var total = Bytes.of(keyData, data, last).toByteArray();
            return Bytes.of(Hmac.calculateSha512(total, key))
                    .cut(KEY_LENGTH)
                    .toByteArray();
        }

        private byte[] generateSnapshotMac(byte[] ltHash, long version, String patchName, byte[] key) {
            var total = Bytes.of(ltHash)
                    .append(BytesHelper.longToBytes(version))
                    .append(patchName.getBytes(StandardCharsets.UTF_8))
                    .toByteArray();
            return Hmac.calculateSha256(total, key);
        }

        private byte[] generatePatchMac(byte[] snapshotMac, Bytes valueMacs, long version, String type, byte[] key) {
            var total = Bytes.of(snapshotMac)
                    .append(valueMacs)
                    .append(BytesHelper.longToBytes(version))
                    .append(type.getBytes(StandardCharsets.UTF_8))
                    .toByteArray();
            return Hmac.calculateSha256(total, key);
        }
    }
    
    private class ErrorHandler {
        private <T> T handleFailure(long statusCode, String reason, ErrorLocation location) {
            return handleFailure(statusCode, reason, location, null);
        }

        private <T> T handleFailure(long statusCode, String reason, ErrorLocation location, Throwable throwable) {
            log.warning("Received status code %s at %s(%s): handling failure"
                    .formatted(statusCode, location, reason));
            (throwable != null ? throwable : new RuntimeException()).printStackTrace();
            if(location != ERRONEOUS_NODE
                    && !(location == MESSAGE && Objects.equals(reason, "hmac_validation"))
                    && !shouldHandleFailure(statusCode, reason)){
                log.warning("Ignoring failure");
                return null;
            }

            log.warning("Restoring session");
            store.clear();
            changeKeys();
            reconnect();
            return null;
        }

        private boolean shouldHandleFailure(long statusCode, String reason) {
            return store.listeners()
                    .stream()
                    .allMatch(listener -> listener.onFailure(statusCode, reason));
        }
    }

    enum ErrorLocation {
        UNKNOWN,
        ERRONEOUS_NODE,
        MEDIA_CONNECTION,
        STREAM,
        LOGIN,
        APP_STATE_SYNC,
        MESSAGE
    }
}
