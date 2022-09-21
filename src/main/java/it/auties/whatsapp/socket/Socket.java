package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.api.SocketEvent;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.MessageWrapper;
import it.auties.whatsapp.binary.PatchType;
import it.auties.whatsapp.controller.Controller;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.exception.ErroneousNodeException;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.GroupMetadata;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.signal.auth.ClientHello;
import it.auties.whatsapp.model.signal.auth.HandshakeMessage;
import it.auties.whatsapp.model.sync.ActionValueSync;
import it.auties.whatsapp.model.sync.PatchRequest;
import it.auties.whatsapp.util.*;
import jakarta.websocket.*;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import lombok.*;
import lombok.experimental.Accessors;

import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static it.auties.whatsapp.api.ErrorHandler.Location.UNKNOWN;
import static it.auties.whatsapp.model.request.Node.ofAttributes;
import static it.auties.whatsapp.model.request.Node.ofChildren;
import static jakarta.websocket.ContainerProvider.getWebSocketContainer;
import static java.lang.Runtime.getRuntime;
import static java.util.Map.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

@Accessors(fluent = true)
@ClientEndpoint(configurator = Socket.OriginPatcher.class)
public class Socket implements JacksonProvider, SignalSpecification {
    static {
        getWebSocketContainer().setDefaultMaxSessionIdleTimeout(0);
    }

    @NonNull
    private final Whatsapp whatsapp;

    @NonNull
    private final AuthHandler authHandler;

    @NonNull
    private final StreamHandler streamHandler;

    @NonNull
    private final MessageHandler messageHandler;

    @NonNull
    private final AppStateHandler appStateHandler;

    @NonNull
    @Getter
    private final Whatsapp.Options options;

    @NonNull
    @Getter(AccessLevel.PROTECTED)
    private final FailureHandler errorHandler;

    private Session session;

    @NonNull
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private SocketState state;

    @Getter
    @NonNull
    private Keys keys;

    @Getter
    @NonNull
    private Store store;

    public Socket(@NonNull Whatsapp whatsapp, @NonNull Whatsapp.Options options, @NonNull Store store,
                  @NonNull Keys keys) {
        this.whatsapp = whatsapp;
        this.options = options;
        this.store = store;
        this.keys = keys;
        this.state = SocketState.WAITING;
        this.authHandler = new AuthHandler(this);
        this.streamHandler = new StreamHandler(this);
        this.messageHandler = new MessageHandler(this);
        this.appStateHandler = new AppStateHandler(this);
        this.errorHandler = new FailureHandler(this);
        getRuntime().addShutdownHook(new Thread(this::onShutdown));
    }

    private void onShutdown() {
        keys.dispose();
        store.dispose();
        streamHandler.dispose();
        onSocketEvent(SocketEvent.CLOSE);
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

    public void changeKeys() {
        var oldListeners = new ArrayList<>(store.listeners());
        deleteAndClearKeys();

        var newId = KeyHelper.registrationId();
        this.keys = Keys.random(newId, options.defaultSerialization());
        this.store = Store.random(newId, options.defaultSerialization());
        store.listeners()
                .addAll(oldListeners);
        onDisconnected(DisconnectReason.LOGGED_OUT);
    }
    
    @NonNull
    public Session session() {
        return session;
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(@NonNull Session session) {
        this.session = session;
        if (state == SocketState.CONNECTED) {
            return;
        }

        errorHandler.failure()
                .set(false);
        onSocketEvent(SocketEvent.OPEN);
        authHandler.createHandshake();
        var clientHello = new ClientHello(keys.ephemeralKeyPair()
                .publicKey());
        var handshakeMessage = new HandshakeMessage(clientHello);
        Request.with(handshakeMessage)
                .sendWithPrologue(session, keys, store);
    }

    @OnMessage
    public void onBinary(byte @NonNull [] raw) {
        var message = new MessageWrapper(raw);
        if (message.decoded()
                .isEmpty()) {
            return;
        }

        var header = message.decoded()
                .getFirst();
        if (state != SocketState.CONNECTED) {
            authHandler.login(session(), header.toByteArray())
                    .thenRunAsync(() -> state(SocketState.CONNECTED));
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
        if (authHandler.future() == null || authHandler.future()
                .isDone()) {
            authHandler.createFuture();
        }

        getWebSocketContainer().connectToServer(this, URI.create(options.url()));
        return authHandler.future();
    }

    @SneakyThrows
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void await() {
        if (streamHandler.pingService() == null) {
            return;
        }

        streamHandler.pingService()
                .awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    @SneakyThrows
    public CompletableFuture<Void> disconnect(boolean reconnect) {
        state(reconnect ? SocketState.RECONNECTING : SocketState.DISCONNECTED);
        keys.clear();
        session.close();
        return reconnect ? connect() : completedFuture(null);
    }

    @OnClose
    public void onClose() {
        if (authHandler.future() != null && !authHandler.future()
                .isDone() && state == SocketState.DISCONNECTED) {
            authHandler.future()
                    .complete(null);
        }

        if (state == SocketState.CONNECTED) {
            onDisconnected(DisconnectReason.RECONNECTING);
            disconnect(true);
            return;
        }

        onDisconnected(DisconnectReason.DISCONNECTED);
        onShutdown();
    }

    @OnError
    public void onError(Throwable throwable) {
        onSocketEvent(SocketEvent.ERROR);
        errorHandler.handleFailure(UNKNOWN, throwable);
    }

    public CompletableFuture<Node> send(Node node) {
        onNodeSent(node);
        return errorHandler.failure()
                .get() ?
                CompletableFuture.failedFuture(new IllegalStateException("Socket is in fail safe state")) :
                node.toRequest(node.id() == null ?
                                store.nextTag() :
                                null)
                        .send(session, keys, store)
                        .exceptionallyAsync(errorHandler::handleNodeFailure);

    }

    private void onNodeSent(Node node) {
        store.callListeners(listener -> {
            listener.onNodeSent(whatsapp, node);
            listener.onNodeSent(node);
        });
    }

    public CompletableFuture<Void> sendWithNoResponse(Node node) {
        onNodeSent(node);
        return errorHandler.failure()
                .get() ?
                CompletableFuture.failedFuture(new IllegalStateException("Socket is in fail safe state")) :
                node.toRequest(node.id() == null ?
                                store.nextTag() :
                                null)
                        .sendWithNoResponse(session, keys, store)
                        .exceptionallyAsync(throwable -> errorHandler.handleFailure(UNKNOWN, throwable));
    }

    public CompletableFuture<Void> pushPatch(PatchRequest request) {
        return appStateHandler.push(request);
    }

    public void pullInitialPatches() {
        appStateHandler.pull(true, PatchType.values());
    }

    public void pullPatch(PatchType... patchTypes) {
        appStateHandler.pull(false, patchTypes);
    }

    public void readMessage(Node node) {
        messageHandler.decode(node);
    }

    @SafeVarargs
    public final CompletableFuture<Void> sendMessage(MessageInfo info, Entry<String, Object>... metadata) {
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
        return send(ofChildren("iq", attributes, body));
    }

    public CompletableFuture<List<Node>> sendInteractiveQuery(Node queryNode, Node... queryBody) {
        var query = Node.ofChildren("query", queryNode);
        var list = Node.ofChildren("list", queryBody);
        var sync = ofChildren("usync",
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
        var body = ofAttributes("query", of("request", "interactive"));
        return sendQuery(group, "get", "w:g2", body).thenApplyAsync(node -> node.findNode("group")
                        .orElseThrow(() -> new ErroneousNodeException("Missing group node", node)))
                .exceptionallyAsync(errorHandler::handleNodeFailure)
                .thenApplyAsync(GroupMetadata::of);
    }

    protected void sendSyncReceipt(MessageInfo info, String type) {
        var receipt = ofAttributes("receipt", of("to", ContactJid.of(keys.companion()
                .user(), ContactJid.Server.USER), "type", type, "id", info.key()
                .id()));
        sendWithNoResponse(receipt);
    }

    protected void sendReceipt(ContactJid jid, ContactJid participant, List<String> messages) {
        if (messages.isEmpty()) {
            return;
        }

        var attributes = Attributes.empty()
                .put("id", messages.get(0))
                .put("t", Clock.now() / 1000)
                .put("to", jid)
                .put("participant", participant, Objects::nonNull, value -> !Objects.equals(jid, value));
        var receipt = Node.ofChildren("receipt", attributes.map(), toMessagesNode(messages));
        sendWithNoResponse(receipt);
    }

    private List<Node> toMessagesNode(List<String> messages) {
        if (messages.size() <= 1) {
            return null;
        }

        return messages.subList(1, messages.size())
                .stream()
                .map(id -> ofAttributes("item", of("id", id)))
                .toList();
    }

    protected void sendMessageAck(Node node, Map<String, Object> metadata) {
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
        var receipt = ofAttributes("ack", attributes);
        sendWithNoResponse(receipt);
    }

    private void deleteAndClearKeys() {
        Controller.deleteFolder(keys().id());
        keys.clear();
        store.clear();
    }

    protected void onMetadata(Map<String, String> properties) {
        store.callListeners(listener -> {
            listener.onMetadata(whatsapp, properties);
            listener.onMetadata(properties);
        });
    }

    protected void onMessageStatus(MessageStatus status, Contact participant, MessageInfo message, Chat chat) {
        store.callListeners(listener -> {
            if (participant == null) {
                listener.onMessageStatus(whatsapp, message, status);
                listener.onMessageStatus(message, status);
            }

            listener.onMessageStatus(whatsapp, chat, participant, message, status);
            listener.onMessageStatus(chat, participant, message, status);
        });
    }

    protected void onUpdateChatPresence(ContactStatus status, Contact contact, Chat chat) {
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

    protected void onNewMessage(MessageInfo info) {
        store.callListeners(listener -> {
            listener.onNewMessage(whatsapp, info);
            listener.onNewMessage(info);
        });
    }

    protected void onNewStatus(MessageInfo info) {
        store.callListeners(listener -> {
            listener.onNewStatus(whatsapp, info);
            listener.onNewStatus(info);
        });
    }

    protected void onChatRecentMessages(Chat chat, boolean last) {
        store.callListeners(listener -> {
            listener.onChatMessages(whatsapp, chat, last);
            listener.onChatMessages(chat, last);
        });
    }

    protected void onFeatures(ActionValueSync.PrimaryFeature features) {
        store.callListeners(listener -> {
            listener.onFeatures(whatsapp, features.flags());
            listener.onFeatures(features.flags());
        });
    }

    protected void onSetting(Setting setting) {
        store.callListeners(listener -> {
            listener.onSetting(whatsapp, setting);
            listener.onSetting(setting);
        });
    }

    protected void onMessageDeleted(MessageInfo message, boolean everyone) {
        store.callListeners(listener -> {
            listener.onMessageDeleted(whatsapp, message, everyone);
            listener.onMessageDeleted(message, everyone);
        });
    }

    protected void onAction(Action action) {
        store.callListeners(listener -> {
            listener.onAction(whatsapp, action);
            listener.onAction(action);
        });
    }

    protected void onSocketEvent(SocketEvent event) {
        store.invokeListeners(listener -> {
            listener.onSocketEvent(whatsapp, event);
            listener.onSocketEvent(event);
        });
    }

    protected void onDisconnected(DisconnectReason loggedOut) {
        store.invokeListeners(listener -> {
            listener.onDisconnected(whatsapp, loggedOut);
            listener.onDisconnected(loggedOut);
        });
    }

    protected void onLoggedIn() {
        store.invokeListeners(listener -> {
            listener.onLoggedIn(whatsapp);
            listener.onLoggedIn();
        });
        authHandler.future().complete(null);
    }
    
    protected void onChats(){
        store.invokeListeners(listener -> {
            listener.onChats(whatsapp);
            listener.onChats();
        });
    }

    protected void onStatus(){
        store.invokeListeners(listener -> {
            listener.onStatus(whatsapp);
            listener.onStatus();
        });
    }

    protected void onContacts(){
        store.invokeListeners(listener -> {
            listener.onContacts(whatsapp);
            listener.onContacts();
        });
    }
    
    @SneakyThrows
    protected void awaitReadyState() {
        appStateHandler.latch()
                .await();
    }

    public static class OriginPatcher extends Configurator {
        @Override
        public void beforeRequest(@NonNull Map<String, List<String>> headers) {
            headers.put("Origin", List.of("https://web.whatsapp.com"));
            headers.put("Host", List.of("web.whatsapp.com"));
        }
    }
}
