package it.auties.whatsapp.binary;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.*;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.*;
import it.auties.whatsapp.listener.Listener;
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
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.setting.EphemeralSetting;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.setting.UnarchiveChatsSetting;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
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
@ClientEndpoint(configurator = Socket.OriginPatcher.class)
public class Socket implements JacksonProvider, SignalSpecification {
    static {
        getWebSocketContainer().setDefaultMaxSessionIdleTimeout(0);
    }

    @NonNull
    private final Whatsapp whatsapp;

    @NonNull
    private final Whatsapp.Options options;

    @NonNull
    private final AuthHandler authHandler;

    @NonNull
    private final StreamHandler streamHandler;

    @NonNull
    private final MessageHandler messageHandler;

    @NonNull
    private final AppStateHandler appStateHandler;

    @NonNull
    private final Socket.FailureHandler errorHandler;

    private Session session;

    @NonNull
    @Setter
    private State state;

    @Getter
    @NonNull
    private Keys keys;

    @Getter
    @NonNull
    private Store store;

    private Handshake handshake;

    private ScheduledExecutorService pingService;

    private CompletableFuture<Void> loginFuture;

    private CompletableFuture<Void> mediaConnectionFuture;

    public Socket(@NonNull Whatsapp whatsapp, @NonNull Whatsapp.Options options, @NonNull Store store,
                  @NonNull Keys keys) {
        this.whatsapp = whatsapp;
        this.options = options;
        this.store = store;
        this.keys = keys;
        this.authHandler = new AuthHandler();
        this.state = State.WAITING;
        this.streamHandler = new StreamHandler();
        this.messageHandler = new MessageHandler();
        this.appStateHandler = new AppStateHandler();
        this.errorHandler = new FailureHandler();
        getRuntime().addShutdownHook(new Thread(() -> onSocketEvent(SocketEvent.CLOSE)));
    }

    private void onSocketEvent(SocketEvent event) {
        store.invokeListeners(listener -> {
            listener.onSocketEvent(whatsapp, event);
            listener.onSocketEvent(event);
        });
    }

    @NonNull
    public Session session() {
        return session;
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(@NonNull Session session) {
        this.session = session;
        if (state.isConnected()) {
            return;
        }

        onSocketEvent(SocketEvent.OPEN);
        this.handshake = new Handshake(keys);
        handshake.updateHash(keys.ephemeralKeyPair()
                .publicKey());
        var clientHello = new ClientHello(keys.ephemeralKeyPair()
                .publicKey());
        var handshakeMessage = new HandshakeMessage(clientHello);
        Request.with(handshakeMessage)
                .sendWithPrologue(session, keys, store);
    }

    @OnMessage
    public void onBinary(byte @NonNull [] raw) {
        var message = new Message(raw);
        if (message.decoded()
                .isEmpty()) {
            return;
        }

        var header = message.decoded()
                .getFirst();
        if (!state.isConnected()) {
            authHandler.sendUserPayload(header.toByteArray());
            return;
        }

        message.toNodes(keys)
                .forEach(this::handleNode);
    }

    private void handleNode(Node deciphered) {
        store.resolvePendingRequest(deciphered, false);
        streamHandler.digest(deciphered);
        onNodeReceived(deciphered);
    }

    private void onNodeReceived(Node deciphered) {
        store.callListeners(listener -> {
            listener.onNodeReceived(whatsapp, deciphered);
            listener.onNodeReceived(deciphered);
        });
    }

    @SneakyThrows
    public CompletableFuture<Void> connect() {
        if (loginFuture == null || loginFuture.isDone()) {
            this.loginFuture = new CompletableFuture<>();
        }

        getWebSocketContainer().connectToServer(this, URI.create(options.url()));
        return loginFuture;
    }

    @SneakyThrows
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void await() {
        if (pingService == null) {
            return;
        }

        pingService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    public CompletableFuture<Void> reconnect() {
        state(State.RECONNECTING);
        return disconnect().thenComposeAsync(ignored -> connect());
    }

    @SneakyThrows
    public CompletableFuture<Void> disconnect() {
        if (state.isConnected()) {
            state(State.DISCONNECTED);
        }

        keys.clear();
        session.close();
        return completedFuture(null); // session#close is a synchronous operation
    }

    @OnClose
    public void onClose() {
        if (loginFuture != null && !loginFuture.isDone() && state == State.DISCONNECTED) {
            loginFuture.complete(null);
        }

        if (state.isConnected()) {
            store.invokeListeners(listener -> listener.onDisconnected(true));
            reconnect();
            return;
        }

        store.invokeListeners(listener -> listener.onDisconnected(false));
        store.dispose();
        onSocketEvent(SocketEvent.CLOSE);
        if (pingService != null) {
            pingService.shutdownNow();
        }

        if (mediaConnectionFuture != null) {
            mediaConnectionFuture.cancel(true);
        }
    }

    @OnError
    public void onError(Throwable throwable) {
        onSocketEvent(SocketEvent.ERROR);
        errorHandler.handleFailure(UNKNOWN, throwable);
    }

    public CompletableFuture<Node> send(Node node) {
        onNodeSent(node);
        return node.toRequest(node.id() == null ?
                        store.nextTag() :
                        null)
                .send(session, keys, store)
                .exceptionallyAsync(throwable -> errorHandler.handleFailure(ERRONEOUS_NODE, throwable));
    }

    private void onNodeSent(Node node) {
        store.callListeners(listener -> {
            listener.onNodeSent(whatsapp, node);
            listener.onNodeSent(node);
        });
    }

    public CompletableFuture<Void> sendWithNoResponse(Node node) {
        onNodeSent(node);
        return node.toRequest(node.id() == null ?
                        store.nextTag() :
                        null)
                .sendWithNoResponse(session, keys, store)
                .exceptionallyAsync(throwable -> errorHandler.handleFailure(UNKNOWN, throwable));
    }

    public CompletableFuture<Void> push(PatchRequest request) {
        return appStateHandler.pullAndPush(request);
    }

    @SafeVarargs
    public final CompletableFuture<Node> sendMessage(MessageInfo info, Entry<String, Object>... metadata) {
        return messageHandler.encode(info, metadata);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Node... body) {
        return sendQuery(null, ContactJid.WHATSAPP, method, category, null, body);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Map<String, Object> metadata,
                                             Node... body) {
        return sendQuery(null, ContactJid.WHATSAPP, method, category, metadata, body);
    }

    public CompletableFuture<Node> sendQuery(ContactJid to, String method, String category, Node... body) {
        return sendQuery(null, to, method, category, null, body);
    }

    public CompletableFuture<Node> sendQuery(String id, ContactJid to, String method, String category,
                                             Map<String, Object> metadata, Node... body) {
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
        return sendQuery("get", "usync", sync).thenApplyAsync(this::parseQueryResult);
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
    public CompletableFuture<GroupMetadata> queryGroupMetadata(ContactJid group) {
        var body = withAttributes("query", of("request", "interactive"));
        return sendQuery(group, "get", "w:g2", body).thenApplyAsync(node -> node.findNode("group")
                        .orElseThrow(() -> new NoSuchElementException("Missing group node")))
                .thenApplyAsync(GroupMetadata::of);
    }

    private void sendSyncReceipt(MessageInfo info, String type) {
        var receipt = withAttributes("receipt", of("to", ContactJid.of(keys.companion()
                .user(), ContactJid.Server.USER), "type", type, "id", info.key()
                .id()));
        sendWithNoResponse(receipt);
    }

    private void sendReceipt(ContactJid jid, ContactJid participant, List<String> messages) {
        if (messages.isEmpty()) {
            return;
        }

        var attributes = Attributes.empty()
                .put("id", messages.get(0))
                .put("t", Clock.now() / 1000)
                .put("to", jid)
                .put("participant", participant, Objects::nonNull, value -> !Objects.equals(jid, value));
        var receipt = withChildren("receipt", attributes.map(), toMessagesNode(messages));
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

    private void sendMessageAck(Node node, Map<String, Object> metadata) {
        var to = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing from in message ack"));
        var participant = node.attributes()
                .getNullableString("participant");
        var attributes = Attributes.of(metadata)
                .put("id", node.id())
                .put("to", to)
                .put("participant", participant, Objects::nonNull)
                .map();
        var receipt = withAttributes("ack", attributes);
        sendWithNoResponse(receipt);
    }

    public void changeKeys() {
        keys.delete();
        var newId = KeyHelper.registrationId();
        this.keys = Keys.random(newId);
        var newStore = Store.random(newId);
        newStore.listeners()
                .addAll(store.listeners());
        this.store = newStore;
    }

    public Contact createContact(ContactJid jid) {
        var newContact = Contact.ofJid(jid);
        store.addContact(newContact);
        return newContact;
    }

    public Chat createChat(ContactJid jid) {
        var newChat = Chat.ofJid(jid);
        store.addChat(newChat);
        return newChat;
    }

    public static class OriginPatcher extends Configurator {
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
            var sharedEphemeral = Curve25519.sharedKey(serverHello.ephemeral(), keys.ephemeralKeyPair()
                    .privateKey());
            handshake.mixIntoKey(sharedEphemeral);

            var decodedStaticText = handshake.cipher(serverHello.staticText(), false);
            var sharedStatic = Curve25519.sharedKey(decodedStaticText, keys.ephemeralKeyPair()
                    .privateKey());
            handshake.mixIntoKey(sharedStatic);
            handshake.cipher(serverHello.payload(), false);

            var encodedKey = handshake.cipher(keys.noiseKeyPair()
                    .publicKey(), true);
            var sharedPrivate = Curve25519.sharedKey(serverHello.ephemeral(), keys.noiseKeyPair()
                    .privateKey());
            handshake.mixIntoKey(sharedPrivate);

            var encodedPayload = handshake.cipher(createUserPayload(), true);
            var clientFinish = new ClientFinish(encodedKey, encodedPayload);
            var handshakeMessage = new HandshakeMessage(clientFinish);
            Request.with(handshakeMessage)
                    .sendWithNoResponse(session, keys, store)
                    .thenRunAsync(this::changeToConnected)
                    .thenRunAsync(handshake::finish);
        }

        private void changeToConnected() {
            keys.clear();
            state(State.CONNECTED);
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
            if (keys.hasCompanion()) {
                return builder.username(parseLong(keys.companion()
                                .user()))
                        .device(keys.companion()
                                .device())
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
                    .buildHash(options.version()
                            .toHash())
                    .companion(PROTOBUF.writeValueAsBytes(createCompanionProps()))
                    .id(BytesHelper.intToBytes(keys.id(), 4))
                    .keyType(BytesHelper.intToBytes(KEY_TYPE, 1))
                    .identifier(keys.identityKeyPair()
                            .publicKey())
                    .signatureId(keys.signedKeyPair()
                            .encodedId())
                    .signaturePublicKey(keys.signedKeyPair()
                            .keyPair()
                            .publicKey())
                    .signature(keys.signedKeyPair()
                            .signature())
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
                case "message" -> messageHandler.decode(node);
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
            errorHandler.handleFailure(reason == 401 ? DISCONNECTED : ERRONEOUS_NODE, new RuntimeException(location));
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
            chat.presences()
                    .put(contact, status);
            onUpdateChatPresence(status, contact, chat);
        }

        private void onUpdateChatPresence(ContactStatus status, Contact contact, Chat chat) {
            store.callListeners(listener -> {
                listener.onContactPresence(whatsapp, chat, contact, status);
                listener.onContactPresence(chat, contact, status);
                if (status != ContactStatus.PAUSED) {
                    return;
                }

                listener.onContactPresence(whatsapp, chat, contact, ContactStatus.AVAILABLE);
                listener.onContactPresence(chat, contact, ContactStatus.AVAILABLE);
            });
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
            sendMessageAck(node, attributes.map());
        }

        private void updateMessageStatus(Node node, MessageStatus status) {
            node.attributes()
                    .getJid("from")
                    .flatMap(store::findChatByJid)
                    .ifPresent(chat -> updateMessageStatus(node, status, chat));
        }

        private void updateMessageStatus(Node node, MessageStatus status, Chat chat) {
            var participant = node.attributes()
                    .getJid("participant")
                    .flatMap(store::findContactByJid)
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
                    .map(messageId -> store.findMessageById(chat, messageId))
                    .flatMap(Optional::stream)
                    .forEach(message -> updateMessageStatus(status, participant, message));
        }

        private void updateMessageStatus(MessageStatus status, Contact participant, MessageInfo message) {
            var chat = message.chat()
                    .orElseGet(() -> createChat(message.chatJid()));
            message.status(status);
            if (participant != null) {
                message.individualStatus()
                        .put(participant, status);
            }

            onMessageStatus(status, participant, message, chat);
        }

        private void onMessageStatus(MessageStatus status, Contact participant, MessageInfo message, Chat chat) {
            store.callListeners(listener -> {
                if (participant == null) {
                    listener.onMessageStatus(whatsapp, message, status);
                    listener.onMessageStatus(message, status);
                }

                listener.onMessageStatus(whatsapp, chat, participant, message, status);
                listener.onMessageStatus(chat, participant, message, status);
            });
        }

        private void digestCall(Node node) {
            var call = node.children()
                    .peekFirst();
            if (call == null) {
                return;
            }

            sendMessageAck(node, of("class", "call", "type", call.description()));
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
            sendWithNoResponse(receipt);
        }

        private void digestNotification(Node node) {
            var type = node.attributes()
                    .getString("type", null);
            sendMessageAck(node, of("class", "notification", "type", type));
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
            appStateHandler.pull(patchName);
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
            sendQuery("set", "urn:xmpp:whatsapp:dirty",
                    withAttributes("clean", of("type", type, "timestamp", timestamp)));
        }

        private void digestError(Node node) {
            var statusCode = node.attributes()
                    .getInt("code");
            switch (statusCode) {
                case 515 -> reconnect();
                case 401 -> handleStreamError(node);
                default -> node.children()
                        .forEach(error -> store.resolvePendingRequest(error, true));
            }
        }

        private void handleStreamError(Node node) {
            var child = node.children()
                    .getFirst();
            var type = child.attributes()
                    .getString("type");
            var reason = child.attributes()
                    .getString("reason", null);
            errorHandler.handleFailure(STREAM, new RuntimeException(requireNonNullElse(reason, type)));
        }

        private void digestSuccess() {
            confirmConnection();
            sendPreKeys();
            createPingTask();
            sendStatusUpdate();
            store.invokeListeners(Listener::onLoggedIn);
            loginFuture.complete(null);
            if (!store.hasSnapshot()) {
                return;
            }

            store.invokeListeners(Listener::onChats);
            store.invokeListeners(Listener::onContacts);
        }

        private void createPingTask() {
            if (pingService != null && !pingService.isShutdown()) {
                return;
            }

            pingService = newSingleThreadScheduledExecutor();
            pingService.scheduleAtFixedRate(this::sendPing, 20L, 20L, TimeUnit.SECONDS);
        }

        private void sendStatusUpdate() {
            var presence = withAttributes("presence", of("type", "available"));
            sendWithNoResponse(presence);
            sendQuery("get", "blocklist");
            sendQuery("get", "privacy", with("privacy"));
            sendQuery("get", "abt", withAttributes("props", of("protocol", "1")));
            sendQuery("get", "w", with("props")).thenAcceptAsync(this::parseProps);
        }

        private void parseProps(Node result) {
            var properties = result.findNode("props")
                    .orElseThrow(() -> new NoSuchElementException("Missing props"))
                    .findNodes("prop")
                    .stream()
                    .map(node -> Map.entry(node.attributes()
                            .getString("name"), node.attributes()
                            .getString("value")))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            onMetadata(properties);
        }

        private void onMetadata(Map<String, String> properties) {
            store.callListeners(listener -> {
                listener.onMetadata(whatsapp, properties);
                listener.onMetadata(properties);
            });
        }

        private void sendPing() {
            if (!state.isConnected()) {
                pingService.shutdownNow();
                return;
            }

            sendQuery("get", "w:p", with("ping"));
            onSocketEvent(SocketEvent.PING);
        }

        private void createMediaConnection() {
            if (!state.isConnected()) {
                return;
            }

            sendQuery("set", "w:m", with("media_conn")).thenApplyAsync(MediaConnection::of)
                    .thenApplyAsync(store::mediaConnection)
                    .exceptionallyAsync(throwable -> errorHandler.handleFailure(MEDIA_CONNECTION, throwable))
                    .thenRunAsync(this::scheduleMediaConnection);
        }

        private void scheduleMediaConnection() {
            var mediaService = CompletableFuture.delayedExecutor(store.mediaConnection()
                    .ttl(), TimeUnit.SECONDS);
            mediaConnectionFuture = CompletableFuture.runAsync(this::createMediaConnection, mediaService);
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
            sendQuery("set", "passive", with("active")).thenRunAsync(this::createMediaConnection);
        }

        private void sendPreKeys() {
            if (keys.hasPreKeys()) {
                return;
            }

            var preKeys = IntStream.range(1, 31)
                    .mapToObj(SignalPreKeyPair::random)
                    .peek(keys::addPreKey)
                    .map(SignalPreKeyPair::toNode)
                    .toList();
            sendQuery("set", "encrypt", with("registration", BytesHelper.intToBytes(keys.id(), 4)),
                    with("type", KEY_BUNDLE_TYPE), with("identity", keys.identityKeyPair()
                            .publicKey()), withChildren("list", preKeys), keys.signedKeyPair()
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
                            keys.noiseKeyPair()
                                    .publicKey())
                    .toBase64(), Bytes.of(keys.identityKeyPair()
                            .publicKey())
                    .toBase64(), Bytes.of(keys.companionKey())
                    .toBase64());
            options.qrHandler()
                    .accept(qr);
        }

        @SneakyThrows
        private void confirmQrCode(Node node, Node container) {
            saveCompanion(container);

            var deviceIdentity = container.findNode("device-identity")
                    .orElseThrow(() -> new NoSuchElementException("Missing device identity"));
            var advIdentity = PROTOBUF.readMessage(deviceIdentity.bytes(), SignedDeviceIdentityHMAC.class);
            var advSign = Hmac.calculateSha256(advIdentity.details(), keys.companionKey());
            if (!Arrays.equals(advIdentity.hmac(), advSign)) {
                errorHandler.handleFailure(LOGIN, new HmacValidationException("adv_sign"));
                return;
            }

            var account = PROTOBUF.readMessage(advIdentity.details(), SignedDeviceIdentity.class);
            var message = Bytes.of(MESSAGE_HEADER)
                    .append(account.details())
                    .append(keys.identityKeyPair()
                            .publicKey())
                    .toByteArray();
            if (!Curve25519.verifySignature(account.accountSignatureKey(), message, account.accountSignature())) {
                errorHandler.handleFailure(LOGIN, new HmacValidationException("message_header"));
                return;
            }

            var deviceSignatureMessage = Bytes.of(SIGNATURE_HEADER)
                    .append(account.details())
                    .append(keys.identityKeyPair()
                            .publicKey())
                    .append(account.accountSignatureKey())
                    .toByteArray();
            account.deviceSignature(Curve25519.sign(keys.identityKeyPair()
                    .privateKey(), deviceSignatureMessage, true));

            var keyIndex = PROTOBUF.readMessage(account.details(), DeviceIdentity.class)
                    .keyIndex();
            var devicePairNode = withChildren("pair-device-sign", with("device-identity", of("key-index", keyIndex),
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
            var companion = node.attributes()
                    .getJid("jid")
                    .orElseThrow(() -> new NoSuchElementException("Missing companion"));
            keys.companion(companion);
        }
    }

    private class MessageHandler {
        private static final String SKMSG = "skmsg";
        private static final String PKMSG = "pkmsg";
        private static final String MSG = "msg";
        private static final int CACHE_EXPIRATION = 1;
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
                var deviceMessage = DeviceSentMessage.newDeviceSentMessage(info.chatJid()
                        .toString(), info.message(), null);
                var encodedDeviceMessage = BytesHelper.messageToBytes(deviceMessage);
                var knownDevices = List.of(keys.companion()
                        .toUserJid(), info.chatJid());
                return getDevices(knownDevices, true).thenComposeAsync(
                                allDevices -> createConversationNodes(allDevices, encodedMessage, encodedDeviceMessage))
                        .thenApplyAsync(sessions -> createEncodedMessageNode(info, sessions, null, attributes))
                        .thenComposeAsync(Socket.this::send)
                        .thenApplyAsync(this::releaseMessageLock)
                        .exceptionallyAsync(this::handleMessageFailure);
            }

            var senderName = new SenderKeyName(info.chatJid()
                    .toString(), keys.companion()
                    .toSignalAddress());
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
                    .thenComposeAsync(Socket.this::send)
                    .thenApplyAsync(this::releaseMessageLock)
                    .exceptionallyAsync(this::handleMessageFailure);
        }

        private Node handleMessageFailure(Throwable throwable) {
            LOCK.release();
            return errorHandler.handleFailure(MESSAGE, throwable);
        }

        private Node releaseMessageLock(Node node) {
            LOCK.release();
            return node;
        }

        private boolean isConversation(MessageInfo info) {
            return info.chatJid()
                    .type() == ContactJid.Type.USER || info.chatJid()
                    .type() == ContactJid.Type.STATUS;
        }

        @SafeVarargs
        @SneakyThrows
        private Node createEncodedMessageNode(MessageInfo info, List<Node> preKeys, Node descriptor,
                                              Entry<String, Object>... metadata) {
            var body = new ArrayList<Node>();
            if (!preKeys.isEmpty()) {
                body.add(withChildren("participants", preKeys));
            }

            if (descriptor != null) {
                body.add(descriptor);
            }

            if (hasPreKeyMessage(preKeys)) {
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
                    .map(node -> node.attributes()
                            .getOptionalString("type"))
                    .flatMap(Optional::stream)
                    .anyMatch("pkmsg"::equals);
        }

        private CompletableFuture<List<Node>> createConversationNodes(List<ContactJid> contacts, byte[] message,
                                                                      byte[] deviceMessage) {
            var partitioned = contacts.stream()
                    .collect(partitioningBy(contact -> Objects.equals(contact.user(), keys.companion()
                            .user())));
            var companions = querySessions(partitioned.get(true)).thenApplyAsync(
                    ignored -> createMessageNodes(partitioned.get(true), deviceMessage));
            var others = querySessions(partitioned.get(false)).thenApplyAsync(
                    ignored -> createMessageNodes(partitioned.get(false), message));
            return companions.thenCombineAsync(others, (first, second) -> append(first, second));
        }

        @SneakyThrows
        private CompletableFuture<List<Node>> createGroupNodes(MessageInfo info, byte[] distributionMessage,
                                                               List<ContactJid> participants) {
            var chat = info.chat()
                    .orElseGet(() -> createChat(info.chatJid()));
            Validate.isTrue(chat.isGroup(), "Cannot send group message to non-group");

            var missingParticipants = participants.stream()
                    .filter(participant -> !chat.participantsPreKeys()
                            .contains(participant))
                    .toList();
            if (missingParticipants.isEmpty()) {
                return completedFuture(List.of());
            }

            var whatsappMessage = new SenderKeyDistributionMessage(info.chatJid()
                    .toString(), distributionMessage);
            var paddedMessage = BytesHelper.messageToBytes(whatsappMessage);
            return querySessions(missingParticipants).thenApplyAsync(
                            ignored -> createMessageNodes(missingParticipants, paddedMessage))
                    .thenApplyAsync(results -> savePreKeys(chat, missingParticipants, results));
        }

        private List<Node> savePreKeys(Chat group, List<ContactJid> missingParticipants, List<Node> results) {
            group.participantsPreKeys()
                    .addAll(missingParticipants);
            return results;
        }

        private CompletableFuture<Void> querySessions(List<ContactJid> contacts) {
            var missingSessions = contacts.stream()
                    .filter(contact -> !keys.hasSession(contact.toSignalAddress()))
                    .map(contact -> withAttributes("user", of("jid", contact, "reason", "identity")))
                    .toList();
            if (missingSessions.isEmpty()) {
                return completedFuture(null);
            }

            return sendQuery("get", "encrypt", withChildren("key", missingSessions)).thenAcceptAsync(
                    this::parseSessions);
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
                    .collect(partitioningBy(contact -> devicesCache.asMap()
                            .containsKey(contact.user()), toUnmodifiableList()));
            var cached = partitioned.get(true)
                    .stream()
                    .map(ContactJid::user)
                    .map(devicesCache::getIfPresent)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .toList();
            var missing = partitioned.get(false);
            if (missing.isEmpty()) {
                return completedFuture(excludeSelf ?
                        append(contacts, cached) :
                        cached);
            }

            return queryDevices(missing, excludeSelf).thenApplyAsync(missingDevices -> excludeSelf ?
                    append(contacts, cached, missingDevices) :
                    append(cached, missingDevices));
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
            return sendQuery("get", "usync", body).thenApplyAsync(result -> parseDevices(result, excludeSelf));
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
            devicesCache.putAll(results.stream()
                    .collect(groupingBy(ContactJid::user)));
            return results;
        }

        private List<ContactJid> parseDevice(Node wrapper, boolean excludeSelf) {
            var jid = wrapper.attributes()
                    .getJid("jid")
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
            var deviceId = child.attributes()
                    .getInt("id");
            return child.description()
                    .equals("device") && (!excludeSelf || deviceId != 0) && (!jid.user()
                    .equals(keys.companion()
                            .user()) || keys.companion()
                    .device() != deviceId) && (deviceId == 0 || child.attributes()
                    .hasKey("key-index")) ?
                    Optional.of(deviceId) :
                    Optional.empty();
        }

        private void parseSessions(Node node) {
            node.findNode("list")
                    .orElseThrow(() -> new NoSuchElementException("Missing list: %s".formatted(node)))
                    .findNodes("user")
                    .forEach(this::parseSession);
        }

        private void parseSession(Node node) {
            Validate.isTrue(!node.hasNode("error"), "Erroneous session node", SecurityException.class);
            var jid = node.attributes()
                    .getJid("jid")
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
            builder.createOutgoing(registrationId, identity, signedKey, key);
        }

        public void decode(Node node) {
            var encrypted = node.findNodes("enc");
            encrypted.forEach(message -> decode(node, message));
        }

        private void decode(Node infoNode, Node messageNode) {
            try {
                LOCK.acquire();
                var pushName = infoNode.attributes()
                        .getString("notify");
                var timestamp = infoNode.attributes()
                        .getLong("t");
                var id = infoNode.attributes()
                        .getRequiredString("id");
                var from = infoNode.attributes()
                        .getJid("from")
                        .orElseThrow(() -> new NoSuchElementException("Missing from"));
                var recipient = infoNode.attributes()
                        .getJid("recipient")
                        .orElse(from);
                var participant = infoNode.attributes()
                        .getJid("participant")
                        .orElse(null);
                var messageBuilder = MessageInfo.newMessageInfo();
                var keyBuilder = MessageKey.newMessageKey();
                switch (from.type()) {
                    case USER, OFFICIAL_BUSINESS_ACCOUNT, STATUS, ANNOUNCEMENT, COMPANION -> {
                        keyBuilder.chatJid(recipient);
                        messageBuilder.senderJid(from);
                    }

                    case GROUP, GROUP_CALL, BROADCAST -> {
                        keyBuilder.chatJid(from);
                        messageBuilder.senderJid(requireNonNull(participant, "Missing participant in group message"));
                    }

                    default -> throw new IllegalArgumentException(
                            "Cannot decode message, unsupported type: %s".formatted(from.type()
                                    .name()));
                }

                var key = keyBuilder.id(id)
                        .create();
                var info = messageBuilder.storeId(store.id())
                        .key(key)
                        .pushName(pushName)
                        .timestamp(timestamp)
                        .create();

                sendMessageAck(infoNode, of("class", "receipt"));
                var encodedMessage = messageNode.bytes();
                var type = messageNode.attributes()
                        .getString("type");
                var buffer = decode(info, encodedMessage, type);
                if (buffer.isEmpty()) {
                    return;
                }

                var messageContainer = BytesHelper.bytesToMessage(buffer.get());
                var message = messageContainer.content() instanceof DeviceSentMessage deviceSentMessage ?
                        MessageContainer.of(deviceSentMessage.message()
                                .content()) :
                        messageContainer;
                info.message(message);
                var content = info.message()
                        .content();
                if (content instanceof SenderKeyDistributionMessage distributionMessage) {
                    handleDistributionMessage(distributionMessage, info.senderJid());
                }

                if (content instanceof ProtocolMessage protocolMessage) {
                    handleProtocolMessage(info, protocolMessage, Objects.equals(infoNode.attributes()
                            .getString("category"), "peer"));
                }

                saveMessage(info);
                sendReceipt(info.chatJid(), info.senderJid(), List.of(info.key()
                        .id()));
            } catch (Throwable throwable) {
                errorHandler.handleFailure(MESSAGE, throwable);
            } finally {
                LOCK.release();
            }
        }

        private Optional<byte[]> decode(MessageInfo info, byte[] message, String type) {
            try {
                return Optional.of(switch (type) {
                    case SKMSG -> {
                        var senderName = new SenderKeyName(info.chatJid()
                                .toString(), info.senderJid()
                                .toSignalAddress());
                        var signalGroup = new GroupCipher(senderName, keys);
                        yield signalGroup.decrypt(message);
                    }

                    case PKMSG -> {
                        var session = new SessionCipher(info.senderJid()
                                .toSignalAddress(), keys);
                        var preKey = SignalPreKeyMessage.ofSerialized(message);
                        yield session.decrypt(preKey);
                    }

                    case MSG -> {
                        var session = new SessionCipher(info.senderJid()
                                .toSignalAddress(), keys);
                        var signalMessage = SignalMessage.ofSerialized(message);
                        yield session.decrypt(signalMessage);
                    }

                    default ->
                            throw new IllegalArgumentException("Unsupported encoded message type: %s".formatted(type));
                });
            } catch (Throwable throwable) {
                errorHandler.handleFailure(MESSAGE, throwable);
                return Optional.empty();
            }
        }

        private void saveMessage(MessageInfo info) {
            if (info.message()
                    .content() instanceof MediaMessage mediaMessage) {
                mediaMessage.storeId(info.storeId());
            }

            if (info.chatJid()
                    .equals(ContactJid.STATUS_ACCOUNT)) {
                store.status()
                        .add(info);
                onNewStatus(info);
                return;
            }

            var chat = info.chat()
                    .orElseGet(() -> createChat(info.chatJid()));
            chat.messages()
                    .add(info);
            if (info.timestamp() <= store.initializationTimeStamp()) {
                return;
            }

            if (info.message()
                    .isServer()) {
                return;
            }

            if (chat.archived() && store.unarchiveChats()) {
                chat.archived(false);
            }

            chat.unreadMessages(chat.unreadMessages() + 1);
            onNewMessage(info);
        }

        private void onNewMessage(MessageInfo info) {
            store.callListeners(listener -> {
                listener.onNewMessage(whatsapp, info);
                listener.onNewMessage(info);
            });
        }

        private void onNewStatus(MessageInfo info) {
            store.callListeners(listener -> {
                listener.onNewStatus(whatsapp, info);
                listener.onNewStatus(info);
            });
        }

        private void handleDistributionMessage(SenderKeyDistributionMessage distributionMessage, ContactJid from) {
            var groupName = new SenderKeyName(distributionMessage.groupId(), from.toSignalAddress());
            var builder = new GroupBuilder(keys);
            var message = SignalDistributionMessage.ofSerialized(distributionMessage.data());
            builder.createIncoming(groupName, message);
        }

        @SneakyThrows
        private void handleProtocolMessage(MessageInfo info, ProtocolMessage protocolMessage, boolean peer) {
            switch (protocolMessage.type()) {
                case HISTORY_SYNC_NOTIFICATION -> {
                    var compressed = Medias.download(protocolMessage.historySyncNotification(), store);
                    var decompressed = BytesHelper.deflate(compressed);
                    var history = PROTOBUF.readMessage(decompressed, HistorySync.class);

                    switch (history.syncType()) {
                        case INITIAL_BOOTSTRAP -> {
                            history.conversations()
                                    .forEach(store::addChat);
                            store.hasSnapshot(true);
                            store.invokeListeners(Listener::onChats);
                        }

                        case FULL -> history.conversations()
                                .forEach(store::addChat);

                        case INITIAL_STATUS_V3 -> {
                            history.statusV3Messages()
                                    .stream()
                                    .peek(message -> message.storeId(store.id()))
                                    .forEach(store.status()::add);
                            store.invokeListeners(Listener::onStatus);
                        }

                        case RECENT -> history.conversations()
                                .forEach(this::handleRecentMessage);

                        case PUSH_NAME -> {
                            history.pushNames()
                                    .forEach(this::handNewPushName);
                            store.invokeListeners(Listener::onContacts);
                        }
                    }

                    sendSyncReceipt(info, "hist_sync");
                }

                case APP_STATE_SYNC_KEY_SHARE -> {
                    keys.addAppKeys(protocolMessage.appStateSyncKeyShare()
                            .keys());
                    appStateHandler.pull(Sync.values());
                }

                case REVOKE -> {
                    var chat = info.chat()
                            .orElseGet(() -> createChat(info.chatJid()));
                    store.findMessageById(chat, protocolMessage.key()
                                    .id())
                            .ifPresent(message -> {
                                chat.messages()
                                        .remove(message);
                                appStateHandler.onMessageDeleted(message, true);
                            });
                }

                case EPHEMERAL_SETTING -> {
                    info.chat()
                            .orElseGet(() -> createChat(info.chatJid()))
                            .ephemeralMessagesToggleTime(info.timestamp())
                            .ephemeralMessageDuration(
                                    ChatEphemeralTimer.forValue(protocolMessage.ephemeralExpiration()));
                    var setting = new EphemeralSetting(info.ephemeralDuration(), info.timestamp());
                    appStateHandler.onSetting(setting);
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
            appStateHandler.onAction(action);
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
                    .forEach(oldChat.get()
                            .messages()::add);
            onChatRecentMessages(oldChat.get());
        }

        private void onChatRecentMessages(Chat oldChat) {
            store.callListeners(listener -> {
                listener.onChatRecentMessages(whatsapp, oldChat);
                listener.onChatRecentMessages(oldChat);
            });
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
        private static final int PULL_ATTEMPTS = 5;

        public CompletableFuture<Void> pullAndPush(@NonNull PatchRequest patch) {
            return pull(patch.type()).thenComposeAsync(ignored -> push(patch));
        }

        public CompletableFuture<Void> push(PatchRequest patch) {
            try {
                SEMAPHORE.acquire();

                var oldState = keys.findHashStateByName(patch.type())
                        .orElseGet(() -> createHashState(patch.type()));
                var newState = oldState.copy();

                var key = requireNonNull(keys.appStateKeys()
                        .peekLast(), "No keys available for push");

                var index = patch.index()
                        .getBytes(StandardCharsets.UTF_8);
                var actionData = ActionDataSync.builder()
                        .index(index)
                        .value(patch.sync())
                        .padding(new byte[0])
                        .version(patch.version())
                        .build();
                var encoded = PROTOBUF.writeValueAsBytes(actionData);

                var mutationKeys = MutationKeys.of(key.keyData()
                        .keyData());
                var encrypted = AesCbc.encryptAndPrefix(encoded, mutationKeys.encKey());
                var valueMac = generateMac(patch.operation(), encrypted, key.keyId()
                        .keyId(), mutationKeys.macKey());
                var indexMac = Hmac.calculateSha256(index, mutationKeys.indexKey());

                var generator = new LTHash(newState);
                generator.mix(indexMac, valueMac, patch.operation());
                var result = generator.finish();
                newState.hash(result.hash());
                newState.indexValueMap(result.indexValueMap());
                newState.version(newState.version() + 1);

                var syncId = new KeyId(key.keyId()
                        .keyId());
                var record = RecordSync.builder()
                        .index(new IndexSync(indexMac))
                        .value(new ValueSync(Bytes.of(encrypted, valueMac)
                                .toByteArray()))
                        .keyId(syncId)
                        .build();
                var mutation = MutationSync.builder()
                        .operation(patch.operation())
                        .record(record)
                        .build();

                var snapshotMac = generateSnapshotMac(newState.hash(), newState.version(), patch.type(),
                        mutationKeys.snapshotMacKey());
                var patchMac = generatePatchMac(snapshotMac, valueMac, newState.version(), patch.type(),
                        mutationKeys.patchMacKey());
                var sync = PatchSync.builder()
                        .patchMac(patchMac)
                        .snapshotMac(snapshotMac)
                        .keyId(syncId)
                        .mutations(List.of(mutation))
                        .build();
                newState.indexValueMap()
                        .put(Bytes.of(indexMac)
                                .toBase64(), valueMac);

                var body = withChildren("collection",
                        of("name", patch.type(), "version", newState.version() - 1, "return_snapshot", false),
                        with("patch", PROTOBUF.writeValueAsBytes(sync)));
                return sendQuery("set", "w:sync:app:state", withChildren("sync", body))
                        .thenRunAsync(() -> keys.putState(patch.type(), newState))
                        .thenRunAsync(() -> sync.version(new VersionSync(newState.version())))
                        .thenRunAsync(() -> parseSyncRequest(patch.type(), oldState.copy(), sync))
                        .thenRunAsync(SEMAPHORE::release)
                        .exceptionallyAsync(this::handleSyncError);
            } catch (Throwable throwable) {
                SEMAPHORE.release();
                throw new RuntimeException("Cannot push patch", throwable);
            }
        }

        private void parseSyncRequest(Sync sync, LTHashState state, PatchSync patch) {
            decodePatch(sync, state.version(), state, patch).stream()
                    .map(MutationsRecord::records)
                    .flatMap(Collection::stream)
                    .forEach(this::processActions);
        }

        private CompletableFuture<Void> pull(Sync... syncs) {
            try {
                return pull(Arrays.asList(syncs), getInitialStateVersions(syncs));
            } catch (Throwable throwable) {
                SEMAPHORE.release();
                throw new RuntimeException("Cannot pull patches", throwable);
            }
        }

        private Map<Sync, Long> getInitialStateVersions(Sync[] syncs) {
            return Arrays.stream(syncs)
                    .collect(toMap(Function.identity(), this::getInitialStateVersion));
        }

        private long getInitialStateVersion(Sync sync) {
            return keys.findHashStateByName(sync)
                    .map(LTHashState::version)
                    .orElse(0L);
        }

        @SneakyThrows
        private CompletableFuture<Void> pull(List<Sync> syncs, Map<Sync, Long> versions) {
            SEMAPHORE.acquire();

            var states = syncs.stream()
                    .map(sync -> keys.findHashStateByName(sync)
                            .orElseGet(() -> createHashState(sync)))
                    .toList();

            var nodes = states.stream()
                    .map(LTHashState::toNode)
                    .toList();

            var request = withChildren("iq",
                   of("id", store.nextTag(), "to", ContactJid.WHATSAPP, "xmlns", "w:sync:app:state", "type",
                            "set"), withChildren("sync", nodes));
            return send(request).thenApplyAsync(this::parseSyncRequest)
                    .thenApplyAsync(records -> handleSyncRequest(records, versions))
                    .thenComposeAsync(records -> checkIncompleteRecords(records, versions))
                    .exceptionallyAsync(this::handleSyncError);
        }

        private List<SnapshotSyncRecord> handleSyncRequest(List<SnapshotSyncRecord> records, Map<Sync, Long> versions){
            parsePatches(records, versions)
                    .forEach(this::processActions);
            return records;
        }

        private CompletableFuture<Void> checkIncompleteRecords(List<SnapshotSyncRecord> records, Map<Sync, Long> versions) {
            SEMAPHORE.release();
            var remaining = records.stream()
                    .filter(SnapshotSyncRecord::hasMore)
                    .map(SnapshotSyncRecord::name)
                    .toList();
            if (remaining.isEmpty()) {
                return completedFuture(null);
            }

            return pull(remaining, versions);
        }

        private LTHashState createHashState(Sync sync) {
            var hashState = new LTHashState(sync);
            keys.putState(sync, hashState);
            return hashState;
        }

        private Void handleSyncError(Throwable exception) {
            SEMAPHORE.release();
            return errorHandler.handleFailure(APP_STATE_SYNC, exception);
        }

        private List<ActionDataSync> parsePatches(List<SnapshotSyncRecord> patches, Map<Sync, Long> versions) {
            return patches.stream()
                    .map(patch -> parsePatch(patch, versions.get(patch.name()), new ArrayList<>()))
                    .flatMap(Collection::stream)
                    .toList();
        }

        private List<ActionDataSync> parsePatch(SnapshotSyncRecord patch, long minimumVersion,
                                                List<Throwable> exceptions) {
            try {
                var results = new ArrayList<ActionDataSync>();
                if (patch.hasSnapshot()) {
                    var decodedSnapshot = decodeSnapshot(patch.name(), minimumVersion, patch.snapshot());
                    results.addAll(decodedSnapshot.records());
                }

                if (patch.hasPatches()) {
                    decodePatches(patch.name(), minimumVersion, patch.patches()).stream()
                            .map(MutationsRecord::records)
                            .forEach(results::addAll);
                }

                return results;
            } catch (Throwable throwable) {
                exceptions.add(throwable);
                createHashState(patch.name());
                if (exceptions.size() > PULL_ATTEMPTS) {
                    throw Exceptions.make(new RuntimeException("Cannot parse patch"), exceptions);
                }

                return parsePatch(patch, minimumVersion, exceptions);
            }
        }

        private List<SnapshotSyncRecord> parseSyncRequest(Node node) {
            return Optional.ofNullable(node)
                    .flatMap(sync -> sync.findNode("sync"))
                    .map(sync -> sync.findNodes("collection"))
                    .stream()
                    .flatMap(Collection::stream)
                    .map(this::parseSync)
                    .toList();
        }

        private SnapshotSyncRecord parseSync(Node sync) {
            var name = Sync.forName(sync.attributes()
                    .getString("name"));
            var more = sync.attributes()
                    .getBool("has_more_patches");
            var snapshotSync = sync.findNode("snapshot")
                    .map(this::decodeSnapshot)
                    .orElse(null);
            var patches = decodePatches(sync);
            return new SnapshotSyncRecord(name, snapshotSync, patches, more);
        }

        @SneakyThrows
        private SnapshotSync decodeSnapshot(Node snapshot) {
            if (snapshot == null) {
                return null;
            }

            var blob = PROTOBUF.readMessage(snapshot.bytes(), ExternalBlobReference.class);
            var syncedData = Medias.download(blob, store);
            return PROTOBUF.readMessage(syncedData, SnapshotSync.class);
        }

        private List<PatchSync> decodePatches(Node sync) {
            var versionCode = sync.attributes()
                    .getInt("version");
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

        private void processActions(ActionDataSync mutation) {
            var value = mutation.value();
            if (value == null) {
                return;
            }

            var action = value.action();
            if (action != null) {
                var jid = ContactJid.of(mutation.messageIndex()
                        .chatJid());
                var targetContact = store.findContactByJid(jid);
                var targetChat = store.findChatByJid(jid);
                var targetMessage = targetChat.flatMap(chat -> store.findMessageById(chat, mutation.messageIndex()
                        .messageId()));
                switch (action) {
                    case ClearChatAction ignored -> targetChat.map(Chat::messages)
                            .ifPresent(SortedMessageList::clear);
                    case ContactAction contactAction -> updateName(targetContact.orElseGet(() -> createContact(jid)),
                            targetChat.orElseGet(() -> createChat(jid)), contactAction);
                    case DeleteChatAction ignored -> targetChat.ifPresent(store.chats()::remove);
                    case DeleteMessageForMeAction ignored -> targetMessage.ifPresent(
                            message -> targetChat.ifPresent(chat -> deleteMessage(message, chat)));
                    case MarkChatAsReadAction markAction -> targetChat.ifPresent(chat -> chat.unreadMessages(
                            markAction.read() ?
                                    0 :
                                    -1));
                    case MuteAction muteAction ->
                            targetChat.ifPresent(chat -> chat.mute(ChatMute.muted(muteAction.muteEndTimestamp())));
                    case PinAction pinAction -> targetChat.ifPresent(chat -> chat.pinned(pinAction.pinned() ?
                            mutation.value()
                                    .timestamp() :
                            0));
                    case StarAction starAction ->
                            targetMessage.ifPresent(message -> message.starred(starAction.starred()));
                    case ArchiveChatAction archiveChatAction ->
                            targetChat.ifPresent(chat -> chat.archived(archiveChatAction.archived()));
                    default -> {
                    }
                }

                onAction(action);
            }

            var setting = value.setting();
            if (setting != null) {
                if (setting instanceof UnarchiveChatsSetting unarchiveChatsSetting) {
                    store.unarchiveChats(unarchiveChatsSetting.unarchiveChats());
                }

                onSetting(setting);
            }

            var features = mutation.value()
                    .primaryFeature();
            if (features != null && !features.flags()
                    .isEmpty()) {
                onFeatures(features);
            }
        }

        private void onFeatures(ActionValueSync.PrimaryFeature features) {
            store.callListeners(listener -> {
                listener.onFeatures(whatsapp, features.flags());
                listener.onFeatures(features.flags());
            });
        }

        private void onSetting(Setting setting) {
            store.callListeners(listener -> {
                listener.onSetting(whatsapp, setting);
                listener.onSetting(setting);
            });
        }

        private void onMessageDeleted(MessageInfo message, boolean everyone) {
            store.callListeners(listener -> {
                listener.onMessageDeleted(whatsapp, message, everyone);
                listener.onMessageDeleted(message, everyone);
            });
        }

        private void onAction(Action action) {
            store.callListeners(listener -> {
                listener.onAction(whatsapp, action);
                listener.onAction(action);
            });
        }

        private void updateName(Contact contact, Chat chat, ContactAction contactAction) {
            if (contact != null) {
                contact.fullName(contactAction.fullName());
                contact.shortName(contactAction.firstName());
            }

            if (chat != null) {
                var name = requireNonNullElse(contactAction.firstName(), contactAction.fullName());
                chat.name(name);
            }
        }

        private void deleteMessage(MessageInfo message, Chat chat) {
            chat.messages()
                    .remove(message);
            onMessageDeleted(message, false);
        }

        private List<MutationsRecord> decodePatches(Sync name, long minimumVersion, List<PatchSync> patches) {
            var newState = keys.findHashStateByName(name)
                    .orElseThrow()
                    .copy();
            return patches.stream()
                    .map(patch -> decodePatch(name, minimumVersion, newState, patch))
                    .flatMap(Optional::stream)
                    .toList();
        }

        @SneakyThrows
        private Optional<MutationsRecord> decodePatch(Sync sync, long minimumVersion, LTHashState newState,
                                                      PatchSync patch) {
            if (patch.hasExternalMutations()) {
                var blob = Medias.download(patch.externalMutations(), store);
                var mutationsSync = PROTOBUF.readMessage(blob, MutationsSync.class);
                patch.mutations()
                        .addAll(mutationsSync.mutations());
            }

            newState.version(patch.version());
            Validate.isTrue(Arrays.equals(calculateSyncMac(patch, sync), patch.patchMac()), "sync_mac",
                    HmacValidationException.class);
            var records = patch.mutations()
                    .stream()
                    .collect(Collectors.toMap(MutationSync::record, MutationSync::operation));
            var mutations = decodeMutations(records, newState);
            newState.hash(mutations.hash());
            newState.indexValueMap(mutations.indexValueMap());
            Validate.isTrue(Arrays.equals(generatePatchMac(sync, newState, patch), patch.snapshotMac()),
                    "patch_mac", HmacValidationException.class);

            return Optional.of(mutations)
                    .filter(ignored -> minimumVersion == 0 || newState.version() > minimumVersion);
        }

        private byte[] generatePatchMac(Sync name, LTHashState newState, PatchSync patch) {
            var mutationKeys = getMutationKeys(patch.keyId());
            return generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.snapshotMacKey());
        }

        private byte[] calculateSyncMac(PatchSync patch, Sync sync) {
            var mutationKeys = getMutationKeys(patch.keyId());
            var mutationMacs = patch.mutations()
                    .stream()
                    .map(mutation -> mutation.record()
                            .value()
                            .blob())
                    .map(Bytes::of)
                    .map(binary -> binary.slice(-KEY_LENGTH))
                    .reduce(Bytes.newBuffer(), Bytes::append)
                    .toByteArray();
            return generatePatchMac(patch.snapshotMac(), mutationMacs, patch.version(), sync,
                    mutationKeys.patchMacKey());
        }

        private MutationsRecord decodeSnapshot(Sync name, long minimumVersion, SnapshotSync snapshot) {
            var newState = new LTHashState(name, snapshot.version()
                    .version());
            var records = snapshot.records()
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), ignored -> Operation.SET));
            var mutations = decodeMutations(records, newState);
            newState.hash(mutations.hash());
            newState.indexValueMap(mutations.indexValueMap());
            var mutationKeys = getMutationKeys(snapshot.keyId());
            if (!Arrays.equals(snapshot.mac(),
                    generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.snapshotMacKey()))) {
                throw new HmacValidationException("decode_snapshot");
            }

            if (minimumVersion == 0 || newState.version() > minimumVersion) {
                mutations.records()
                        .clear();
            }

            keys.putState(name, newState);
            return mutations;
        }

        private MutationKeys getMutationKeys(KeyId snapshot) {
            var encryptedKey = keys.findAppKeyById(snapshot.id())
                    .orElseThrow(() -> new NoSuchElementException("No keys available for mutation"));
            return MutationKeys.of(encryptedKey.keyData()
                    .keyData());
        }

        private MutationsRecord decodeMutations(Map<RecordSync, Operation> syncs, LTHashState initialState) {
            var generator = new LTHash(initialState);
            var mutations = syncs.entrySet()
                    .stream()
                    .map(mutation -> decodeMutation(mutation.getValue(), mutation.getKey(), generator))
                    .collect(Collectors.toList());
            var result = generator.finish();
            return new MutationsRecord(result.hash(), result.indexValueMap(), mutations);
        }

        @SneakyThrows
        private ActionDataSync decodeMutation(Operation operation, RecordSync sync, LTHash generator) {
            var mutationKeys = getMutationKeys(sync.keyId());

            var blob = Bytes.of(sync.value()
                    .blob());
            var encryptedBlob = blob.cut(-KEY_LENGTH)
                    .toByteArray();
            var encryptedMac = blob.slice(-KEY_LENGTH)
                    .toByteArray();
            Validate.isTrue(Arrays.equals(encryptedMac, generateMac(operation, encryptedBlob, sync.keyId()
                    .id(), mutationKeys.macKey())), "decode_mutation", HmacValidationException.class);

            var result = AesCbc.decrypt(encryptedBlob, mutationKeys.encKey());
            var actionSync = PROTOBUF.readMessage(result, ActionDataSync.class);
            Validate.isTrue(Arrays.equals(sync.index()
                            .blob(), Hmac.calculateSha256(actionSync.index(), mutationKeys.indexKey())), "decode_mutation",
                    HmacValidationException.class);
            generator.mix(sync.index()
                    .blob(), encryptedMac, operation);
            return actionSync;
        }

        private byte[] generateMac(Operation operation, byte[] data, byte[] keyId, byte[] key) {
            var keyData = Bytes.of(operation.content())
                    .append(keyId)
                    .toByteArray();

            var last = Bytes.newBuffer(MAC_LENGTH - 1)
                    .append(keyData.length)
                    .toByteArray();

            var total = Bytes.of(keyData, data, last)
                    .toByteArray();
            return Bytes.of(Hmac.calculateSha512(total, key))
                    .cut(KEY_LENGTH)
                    .toByteArray();
        }

        private byte[] generateSnapshotMac(byte[] ltHash, long version, Sync sync, byte[] key) {
            var total = Bytes.of(ltHash)
                    .append(BytesHelper.longToBytes(version))
                    .append(sync.toString()
                            .getBytes(StandardCharsets.UTF_8))
                    .toByteArray();
            return Hmac.calculateSha256(total, key);
        }

        private byte[] generatePatchMac(byte[] snapshotMac, byte[] valueMac, long version, Sync sync, byte[] key) {
            var total = Bytes.of(snapshotMac)
                    .append(valueMac)
                    .append(BytesHelper.longToBytes(version))
                    .append(sync.toString()
                            .getBytes(StandardCharsets.UTF_8))
                    .toByteArray();
            return Hmac.calculateSha256(total, key);
        }
    }

    private class FailureHandler {
        private <T> T handleFailure(ErrorHandler.Location location, Throwable throwable) {
            if (state == State.RESTORING_FAILURE) {
                return null;
            }

            if (!options.errorHandler()
                    .apply(location, throwable)) {
                return null;
            }

            state(State.RESTORING_FAILURE);
            store.clear();
            changeKeys();
            reconnect();
            return null;
        }
    }

    private enum State {
        WAITING,
        CONNECTED,
        DISCONNECTED,
        RECONNECTING,
        RESTORING_FAILURE;

        public boolean isConnected() {
            return this == CONNECTED || this == RESTORING_FAILURE;
        }
    }
}
