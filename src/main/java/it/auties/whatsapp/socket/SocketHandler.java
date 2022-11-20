package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.api.SocketEvent;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.MessageWrapper;
import it.auties.whatsapp.binary.PatchType;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.exception.ErroneousNodeRequestException;
import it.auties.whatsapp.listener.OnNewContact;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.GroupMetadata;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.response.ContactStatusResponse;
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

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static it.auties.whatsapp.model.request.Node.ofAttributes;
import static it.auties.whatsapp.model.request.Node.ofChildren;
import static jakarta.websocket.ContainerProvider.getWebSocketContainer;
import static java.lang.Runtime.getRuntime;
import static java.util.Map.of;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Accessors(fluent = true)
@ClientEndpoint(configurator = SocketHandler.OriginPatcher.class)
@SuppressWarnings("unused")
public class SocketHandler implements JacksonProvider, SignalSpecification {
    private static final String WHATSAPP_URL = "wss://web.whatsapp.com/ws/chat";

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

    private final CountDownLatch latch;

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

    public SocketHandler(@NonNull Whatsapp whatsapp, @NonNull Whatsapp.Options options, @NonNull Store store,
                         @NonNull Keys keys) {
        this.whatsapp = whatsapp;
        this.options = options;
        this.store = store;
        this.keys = keys;
        this.state = SocketState.WAITING;
        this.latch = new CountDownLatch(1);
        this.authHandler = new AuthHandler(this);
        this.streamHandler = new StreamHandler(this);
        this.messageHandler = new MessageHandler(this);
        this.appStateHandler = new AppStateHandler(this);
        this.errorHandler = new FailureHandler(this);
        store().listeners().add((OnNewContact) whatsapp::subscribeToPresence);
        getRuntime().addShutdownHook(new Thread(() -> onShutdown(false)));
    }

    private void onShutdown(boolean reconnect) {
        keys.dispose();
        store.dispose();
        streamHandler.dispose();
        if(reconnect){
            return;
        }

        onSocketEvent(SocketEvent.CLOSE);
        latch.countDown();
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
        Request.of(handshakeMessage)
                .sendWithPrologue(session, keys, store)
                .exceptionallyAsync(throwable -> errorHandler.handleFailure(CRYPTOGRAPHY, throwable));
    }

    @OnMessage
    public void onBinary(byte @NonNull [] raw) {
        var message = new MessageWrapper(raw);
        if (message.decoded()
                .isEmpty()) {
            return;
        }

        if (state != SocketState.CONNECTED) {
            var header = message.decoded()
                    .getFirst()
                    .toByteArray();
            authHandler.login(session(), header)
                    .thenRunAsync(() -> state(SocketState.CONNECTED));
            return;
        }

        message.toNodes(keys)
                .forEach(this::handleNode);
    }

    private void handleNode(Node deciphered) {
        onNodeReceived(deciphered);
        store.resolvePendingRequest(deciphered, false);
        streamHandler.digest(deciphered);
    }

    private void onNodeReceived(Node deciphered) {
        store.callListeners(listener -> {
            listener.onNodeReceived(whatsapp, deciphered);
            listener.onNodeReceived(deciphered);
        });
    }

    public CompletableFuture<Void> connect() {
        try {
            if (authHandler.future() == null || authHandler.future()
                    .isDone()) {
                authHandler.createFuture();
            }

            getWebSocketContainer().connectToServer(this, URI.create(WHATSAPP_URL));
            return authHandler.future();
        }catch (IOException | DeploymentException exception){
            throw new RuntimeException("Cannot connect to socket", exception);
        }
    }

    public void await() {
        try {
            latch.await();
        }catch (InterruptedException exception){
            throw new RuntimeException("Cannot await socket", exception);
        }
    }

    public CompletableFuture<Void> disconnect(boolean reconnect) {
        try {
            state(reconnect ? SocketState.RECONNECTING : SocketState.DISCONNECTED);
            keys.clear();
            session.close();
            return reconnect ? connect() : completedFuture(null);
        }catch (IOException exception){
            throw new RuntimeException("Cannot disconnect socket", exception);
        }
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
        onShutdown(state == SocketState.RECONNECTING);
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
                        .exceptionallyAsync(throwable -> errorHandler.handleFailure(SOCKET, throwable));
    }

    public CompletableFuture<Void> pushPatch(PatchRequest request) {
        return appStateHandler.push(request);
    }

    public void pullInitialPatches() {
        if(store.initialAppSync()){
            appStateHandler.markReady();
            return;
        }

        appStateHandler.pull(true, PatchType.values());
    }

    public void pullPatch(PatchType... patchTypes) {
        appStateHandler.pull(false, patchTypes);
    }

    public void decodeMessage(Node node) {
        messageHandler.decodeAsync(node);
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
        return sendQuery("get", "usync", sync)
                .thenApplyAsync(this::parseQueryResult);
    }

    public CompletableFuture<Optional<ContactStatusResponse>> queryStatus(@NonNull ContactJidProvider chat) {
        var query = Node.of("status");
        var body = Node.ofAttributes("user", Map.of("jid", chat.toJid()));
        return sendInteractiveQuery(query, body)
                .thenApplyAsync(this::parseStatus);
    }

    private Optional<ContactStatusResponse> parseStatus(List<Node> responses) {
        return responses.stream()
                .map(entry -> entry.findNode("status"))
                .flatMap(Optional::stream)
                .findFirst()
                .map(ContactStatusResponse::new);
    }

    public CompletableFuture<Optional<URI>> queryPicture(@NonNull ContactJidProvider chat) {
        var body = Node.ofAttributes("picture", Map.of("query", "url"));
        return sendQuery("get", "w:profile:picture", Map.of("target", chat.toJid()), body)
                .thenApplyAsync(this::parseChatPicture);
    }

    private Optional<URI> parseChatPicture(Node result) {
        return result.findNode("picture")
                .flatMap(picture -> picture.attributes()
                        .getOptionalString("url"))
                .map(URI::create);
    }

    public CompletableFuture<List<ContactJid>> queryBlockList() {
        return sendQuery("get", "blocklist", (Node) null)
                .thenApplyAsync(this::parseBlockList);
    }

    private List<ContactJid> parseBlockList(Node result) {
        return result.findNode("list")
                .orElseThrow(() -> new NoSuchElementException("Missing block list in response"))
                .findNodes("item")
                .stream()
                .map(item -> item.attributes()
                        .getJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    public CompletableFuture<Void> subscribeToPresence(ContactJidProvider jid){
        var node = Node.ofAttributes("presence", Map.of("to", jid.toJid(), "type", "subscribe"));
        return sendWithNoResponse(node);
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
                        .orElseThrow(() -> new ErroneousNodeRequestException("Missing group node", node)))
                .exceptionallyAsync(errorHandler::handleNodeFailure)
                .thenApplyAsync(GroupMetadata::of);
    }

    protected void sendSyncReceipt(MessageInfo info, String type) {
        if(store.userCompanionJid() == null){
            return;
        }

        var receipt = ofAttributes("receipt", of("to", ContactJid.of(store.userCompanionJid().user(), ContactJid.Server.USER), "type", type, "id", info.key()
                .id()));
        sendWithNoResponse(receipt);
    }

    public void sendReceipt(ContactJid jid, ContactJid participant, List<String> messages, String type) {
        if (messages.isEmpty()) {
            return;
        }

        var attributes = Attributes.empty()
                .put("id", messages.get(0))
                .put("t", Clock.now() / 1000)
                .put("to", jid)
                .put("type", type, Objects::nonNull)
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
        LocalFileSystem.delete(String.valueOf(keys().id()));
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
            if(participant == null) {
                listener.onConversationMessageStatus(whatsapp, message, status);
                listener.onConversationMessageStatus(message, status);
            }

            listener.onAnyMessageStatus(whatsapp, chat, participant, message, status);
            listener.onAnyMessageStatus(chat, participant, message, status);
        });
    }

    protected void onUpdateChatPresence(ContactStatus status, Contact contact, Chat chat) {
        store.callListeners(listener -> {
            listener.onContactPresence(whatsapp, chat, contact, status);
            listener.onContactPresence(chat, contact, status);
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
            listener.onChatMessagesSync(whatsapp, chat, last);
            listener.onChatMessagesSync(chat, last);
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
    
    protected void onChats() {
        store.invokeListeners(listener -> {
            listener.onChats(whatsapp, store().chats());
            listener.onChats(store().chats());
        });
    }

    protected void onMediaStatus() {
        store.invokeListeners(listener -> {
            listener.onMediaStatus(whatsapp, store().status());
            listener.onMediaStatus(store().status());
        });
    }

    protected void onContacts() {
        store.invokeListeners(listener -> {
            listener.onContacts(whatsapp, store().contacts());
            listener.onContacts(store().contacts());
        });
    }

    protected void onPrivacySettings() {
        store.invokeListeners(listener -> {
            listener.onPrivacySettings(whatsapp, store().privacySettings());
            listener.onPrivacySettings(store().privacySettings());
        });
    }

    protected void onHistorySyncProgress(Integer progress, boolean recent) {
        store.invokeListeners(listener -> {
            listener.onHistorySyncProgress(whatsapp, progress, recent);
            listener.onHistorySyncProgress(progress, recent);
        });
    }

    protected void awaitAppReady() {
        appStateHandler.awaitReady();
    }

    protected void onReply(MessageInfo info) {
        store.resolvePendingReply(info);
        store.callListeners(listener -> {
            listener.onMessageReply(whatsapp, info, info.quotedMessage().get());
            listener.onMessageReply(info, info.quotedMessage().get());
        });
    }

    protected void onGroupPictureChange(Chat fromChat) {
        store.callListeners(listener -> {
            listener.onGroupPictureChange(whatsapp, fromChat);
            listener.onGroupPictureChange(fromChat);
        });
    }

    protected void onContactPictureChange(Contact fromContact) {
        store.callListeners(listener -> {
            listener.onContactPictureChange(whatsapp, fromContact);
            listener.onContactPictureChange(fromContact);
        });
    }

    protected void onUserStatusChange(String newStatus, String oldStatus) {
        store.callListeners(listener -> {
            listener.onUserStatusChange(whatsapp, oldStatus, newStatus);
            listener.onUserStatusChange(oldStatus, newStatus);
        });
    }

    public void onUserPictureChange(URI newPicture, URI oldPicture) {
        store.callListeners(listener -> {
            listener.onUserPictureChange(whatsapp, oldPicture, newPicture);
            listener.onUserPictureChange(oldPicture, newPicture);
        });
    }

    public void updateUserName(String newName, String oldName) {
        if(newName == null || Objects.equals(newName, oldName)){
            return;
        }

        if(oldName != null) {
            sendWithNoResponse(Node.ofAttributes("presence", Map.of("name", oldName, "type", "unavailable")));
            sendWithNoResponse(Node.ofAttributes("presence", Map.of("name", newName, "type", "available")));
            store.callListeners(listener -> {
                listener.onUserNameChange(whatsapp, oldName, newName);
                listener.onUserStatusChange(oldName, newName);
            });
        }

        var userJid = store().userCompanionJid().toUserJid();
        store().findContactByJid(userJid)
                .orElseGet(() -> store.addContact(userJid))
                .chosenName(newName);
        store().userName(newName);
    }

    public void updateLocale(String newLocale, String oldLocale) {
        if(!Objects.equals(newLocale, oldLocale)){
            return;
        }

        if(oldLocale != null){
            store.callListeners(listener -> {
                listener.onUserLocaleChange(whatsapp, oldLocale, newLocale);
                listener.onUserLocaleChange(oldLocale, newLocale);
            });
        }

        store().userLocale(newLocale);
    }

    protected void onContactBlocked(Contact contact) {
        store.callListeners(listener -> {
            listener.onContactBlocked(whatsapp, contact);
            listener.onContactBlocked(contact);
        });
    }

    public static class OriginPatcher extends Configurator {
        @Override
        public void beforeRequest(@NonNull Map<String, List<String>> headers) {
            headers.put("Origin", List.of("https://web.whatsapp.com"));
            headers.put("Host", List.of("web.whatsapp.com"));
        }
    }
}
