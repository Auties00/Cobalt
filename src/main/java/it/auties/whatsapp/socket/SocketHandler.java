package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.api.SocketEvent;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappOptions;
import it.auties.whatsapp.api.WhatsappOptions.MobileOptions;
import it.auties.whatsapp.binary.MessageWrapper;
import it.auties.whatsapp.binary.PatchType;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.GroupMetadata;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJid.Server;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.model.request.MessageSendRequest;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.response.ContactStatusResponse;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.signal.auth.ClientHello;
import it.auties.whatsapp.model.signal.auth.HandshakeMessage;
import it.auties.whatsapp.model.sync.ActionValueSync;
import it.auties.whatsapp.model.sync.PatchRequest;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.KeyHelper;
import it.auties.whatsapp.util.LocalFileSystem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static it.auties.whatsapp.model.request.Node.ofAttributes;
import static it.auties.whatsapp.model.request.Node.ofChildren;
import static jakarta.websocket.ContainerProvider.getWebSocketContainer;
import static java.lang.Runtime.getRuntime;
import static java.util.Map.of;
import static java.util.concurrent.CompletableFuture.runAsync;

@Accessors(fluent = true)
@SuppressWarnings("unused")
public class SocketHandler extends Handler implements SocketListener, JacksonProvider {
    private static final int MANUAL_INITIAL_PULL_TIMEOUT = 5;

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
    private final WhatsappOptions options;

    @NonNull
    @Getter(AccessLevel.PROTECTED)
    private final FailureHandler errorHandler;

    private SocketSession session;

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

    private CompletableFuture<Void> authFuture;

    public SocketHandler(@NonNull Whatsapp whatsapp, @NonNull WhatsappOptions options, @NonNull Store store, @NonNull Keys keys) {
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
        getRuntime().addShutdownHook(new Thread(() -> onShutdown(false)));
    }

    private void onShutdown(boolean reconnect) {
        if (state != SocketState.LOGGED_OUT && state != SocketState.RESTORE) {
            keys.dispose();
            store.dispose();
        }
        if (!reconnect) {
            dispose();
        }
    }

    @Override
    protected void dispose() {
        onSocketEvent(SocketEvent.CLOSE);
        authHandler.dispose();
        streamHandler.dispose();
        messageHandler.dispose();
        appStateHandler.dispose();
        errorHandler.dispose();
        super.dispose();
    }

    protected void onSocketEvent(SocketEvent event) {
        callListenersAsync(listener -> {
            listener.onSocketEvent(whatsapp, event);
            listener.onSocketEvent(event);
        });
    }

    private void callListenersAsync(Consumer<Listener> consumer) {
        if (state == SocketState.DISCONNECTED || state == SocketState.LOGGED_OUT) {
            return;
        }
        var service = getOrCreateService();
        store.listeners().forEach(listener -> service.execute(() -> consumer.accept(listener)));
    }

    @Override
    protected ExecutorService createService() {
        return Executors.newCachedThreadPool();
    }

    @Override
    public void onOpen(SocketSession session) {
        this.session = session;
        if (state == SocketState.CONNECTED) {
            return;
        }
        this.state = SocketState.WAITING;
        onSocketEvent(SocketEvent.OPEN);
        authHandler.createHandshake();
        var clientHello = new ClientHello(keys.ephemeralKeyPair().publicKey());
        var handshakeMessage = new HandshakeMessage(clientHello);
        Request.of(handshakeMessage)
                .sendWithPrologue(session, keys, store)
                .exceptionallyAsync(throwable -> errorHandler.handleFailure(CRYPTOGRAPHY, throwable));
    }

    @Override
    public void onMessage(byte[] raw) {
        var message = new MessageWrapper(raw);
        if (message.decoded().isEmpty()) {
            return;
        }
        if (state != SocketState.CONNECTED && state != SocketState.RESTORE) {
            var header = message.decoded().getFirst().toByteArray();
            authHandler.loginSocket(session, header).thenRunAsync(() -> state(SocketState.CONNECTED));
            return;
        }
        message.toNodes(keys).forEach(this::handleNode);
    }

    private void handleNode(Node deciphered) {
        onNodeReceived(deciphered);
        store.resolvePendingRequest(deciphered, false);
        streamHandler.digest(deciphered);
    }

    private void onNodeReceived(Node deciphered) {
        callListenersAsync(listener -> {
            listener.onNodeReceived(whatsapp, deciphered);
            listener.onNodeReceived(deciphered);
        });
    }

    @Override
    public void onClose() {
        if (state == SocketState.CONNECTED) {
            disconnect(DisconnectReason.RECONNECTING);
            return;
        }
        onDisconnected(state.toReason());
        onShutdown(state == SocketState.RECONNECTING);
        if (authFuture != null && !authFuture.isDone() && state.isDisconnected()) {
            authFuture.complete(null);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable instanceof IllegalStateException stateException && stateException.getMessage()
                .equals("The connection has been closed.")) {
            onClose();
            return;
        }
        onSocketEvent(SocketEvent.ERROR);
        errorHandler.handleFailure(UNKNOWN, throwable);
    }

    public CompletableFuture<Void> connect() {
        if (authFuture == null || authFuture.isDone()) {
            this.authFuture = new CompletableFuture<>();
        }

        if(options instanceof MobileOptions mobileOptions && !keys.registered()){
            authHandler.checkRegistrationStatus();
        }

        this.session = SocketSession.of(options.clientType());
        return session.connect(this).thenCompose(ignored -> authFuture);
    }

    public CompletableFuture<Void> disconnect(DisconnectReason reason) {
        state(SocketState.of(reason));
        keys.clearReadWriteKey();
        return switch (reason) {
            case DISCONNECTED -> {
                session.close();
                yield CompletableFuture.completedFuture(null);
            }
            case RECONNECTING -> {
                session.close();
                yield connect();
            }
            case LOGGED_OUT -> {
                store.resolveAllPendingRequests();
                session.close();
                LocalFileSystem.delete(keys().id());
                yield CompletableFuture.completedFuture(null);
            }
            case RESTORE -> {
                store.resolveAllPendingRequests();
                var oldListeners = new ArrayList<>(store.listeners());
                session.close();
                LocalFileSystem.delete(keys().id());
                options.id(KeyHelper.registrationId());
                this.keys = Keys.random(options);
                this.store = Store.random(options);
                store.listeners().addAll(oldListeners);
                yield connect();
            }
        };
    }

    public CompletableFuture<Void> pushPatch(PatchRequest request) {
        return appStateHandler.push(request);
    }

    public void pullPatch(PatchType... patchTypes) {
        appStateHandler.pull(patchTypes);
    }

    protected CompletableFuture<Void> pullInitialPatches() {
        return appStateHandler.pullInitial();
    }

    public void decodeMessage(Node node) {
        messageHandler.decode(node);
    }

    public final CompletableFuture<Void> sendMessage(MessageSendRequest request) {
        store.attribute(request.info());
        return messageHandler.encode(request);
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<Void> sendQueryWithNoResponse(String method, String category, Node... body) {
        return sendQueryWithNoResponse(null, Server.WHATSAPP.toJid(), method, category, null, body);
    }

    public CompletableFuture<Void> sendQueryWithNoResponse(String id, ContactJid to, String method, String category, Map<String, Object> metadata, Node... body) {
        var attributes = Attributes.ofNullable(metadata)
                .put("id", id, Objects::nonNull)
                .put("type", method)
                .put("to", to)
                .put("xmlns", category, Objects::nonNull)
                .toMap();
        return sendWithNoResponse(ofChildren("iq", attributes, body));
    }

    public CompletableFuture<Void> sendWithNoResponse(Node node) {
        if (state() == SocketState.RESTORE) {
            return CompletableFuture.failedFuture(new IllegalStateException("Socket is in fail safe state"));
        }

        var request = node.toRequest(node.id() == null ? store.nextTag() : null, null);
        return request.sendWithNoResponse(session, keys, store)
                .exceptionallyAsync(throwable -> errorHandler.handleFailure(SOCKET, throwable))
                .thenRunAsync(() -> onNodeSent(node));
    }

    private void onNodeSent(Node node) {
        callListenersAsync(listener -> {
            listener.onNodeSent(whatsapp, node);
            listener.onNodeSent(node);
        });
    }

    public CompletableFuture<Optional<ContactStatusResponse>> queryStatus(@NonNull ContactJidProvider chat) {
        var query = Node.of("status");
        var body = Node.ofAttributes("user", Map.of("jid", chat.toJid()));
        return sendInteractiveQuery(query, body).thenApplyAsync(this::parseStatus);
    }

    public CompletableFuture<List<Node>> sendInteractiveQuery(Node queryNode, Node... queryBody) {
        var query = Node.ofChildren("query", queryNode);
        var list = Node.ofChildren("list", queryBody);
        var sync = ofChildren("usync", of("sid", store.nextTag(), "mode", "query", "last", "true", "index", "0", "context", "interactive"), query, list);
        return sendQuery("get", "usync", sync).thenApplyAsync(this::parseQueryResult);
    }

    private Optional<ContactStatusResponse> parseStatus(List<Node> responses) {
        return responses.stream()
                .map(entry -> entry.findNode("status"))
                .flatMap(Optional::stream)
                .findFirst()
                .map(ContactStatusResponse::new);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Node... body) {
        return sendQuery(null, Server.WHATSAPP.toJid(), method, category, null, body);
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

    public CompletableFuture<Node> sendQuery(String id, ContactJid to, String method, String category, Map<String, Object> metadata, Node... body) {
        var attributes = Attributes.ofNullable(metadata)
                .put("id", id, Objects::nonNull)
                .put("type", method)
                .put("to", to)
                .put("xmlns", category, Objects::nonNull)
                .toMap();
        return send(ofChildren("iq", attributes, body));
    }

    public CompletableFuture<Node> send(Node node) {
        return send(node, null);
    }

    public CompletableFuture<Node> send(Node node, Function<Node, Boolean> filter) {
        if (state() == SocketState.RESTORE) {
            return CompletableFuture.completedFuture(node);
        }
        onNodeSent(node);
        return node.toRequest(node.id() == null ? store.nextTag() : null, filter).send(session, keys, store);
    }

    public CompletableFuture<Optional<URI>> queryPicture(@NonNull ContactJidProvider chat) {
        var body = Node.ofAttributes("picture", Map.of("query", "url"));
        return sendQuery("get", "w:profile:picture", Map.of("target", chat.toJid()), body).thenApplyAsync(this::parseChatPicture);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Map<String, Object> metadata, Node... body) {
        return sendQuery(null, Server.WHATSAPP.toJid(), method, category, metadata, body);
    }

    private Optional<URI> parseChatPicture(Node result) {
        return result.findNode("picture")
                .flatMap(picture -> picture.attributes().getOptionalString("url"))
                .map(URI::create);
    }

    public CompletableFuture<List<ContactJid>> queryBlockList() {
        return sendQuery("get", "blocklist", (Node) null).thenApplyAsync(this::parseBlockList);
    }

    private List<ContactJid> parseBlockList(Node result) {
        return result.findNode("list")
                .orElseThrow(() -> new NoSuchElementException("Missing block list in response"))
                .findNodes("item")
                .stream()
                .map(item -> item.attributes().getJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    public CompletableFuture<Void> subscribeToPresence(ContactJidProvider jid) {
        var node = Node.ofAttributes("presence", Map.of("to", jid.toJid(), "type", "subscribe"));
        return sendWithNoResponse(node);
    }

    public CompletableFuture<GroupMetadata> queryGroupMetadata(ContactJid group) {
        var body = ofAttributes("query", of("request", "interactive"));
        return sendQuery(group, "get", "w:g2", body).thenApplyAsync(node -> node.findNode("group")
                        .orElseThrow(() -> new NoSuchElementException("Missing group node: %s".formatted(node))))
                .thenApplyAsync(GroupMetadata::of);
    }

    public CompletableFuture<Node> sendQuery(ContactJid to, String method, String category, Node... body) {
        return sendQuery(null, to, method, category, null, body);
    }

    protected void sendSyncReceipt(MessageInfo info, String type) {
        if (store.userCompanionJid() == null) {
            return;
        }
        var receipt = ofAttributes("receipt", of("to", ContactJid.of(store.userCompanionJid()
                .user(), ContactJid.Server.USER), "type", type, "id", info.key().id()));
        sendWithNoResponse(receipt);
    }

    public void sendReceipt(ContactJid jid, ContactJid participant, List<String> messages, String type) {
        if (messages.isEmpty()) {
            return;
        }
        var attributes = Attributes.of()
                .put("id", messages.get(0))
                .put("t", Clock.nowSeconds() / 1000)
                .put("to", jid)
                .put("type", type, Objects::nonNull)
                .put("participant", participant, Objects::nonNull, value -> !Objects.equals(jid, value));
        var receipt = Node.ofChildren("receipt", attributes.toMap(), toMessagesNode(messages));
        sendWithNoResponse(receipt);
    }

    private List<Node> toMessagesNode(List<String> messages) {
        if (messages.size() <= 1) {
            return null;
        }
        return messages.subList(1, messages.size()).stream().map(id -> ofAttributes("item", of("id", id))).toList();
    }

    protected void sendMessageAck(Node node, Map<String, Object> metadata) {
        var to = node.attributes()
                .getJid("from")
                .orElseThrow(() -> new NoSuchElementException("Missing from in message ack"));
        var participant = node.attributes().getNullableString("participant");
        var recipient = node.attributes().getNullableString("recipient");
        var type = node.attributes()
                .getOptionalString("type")
                .filter(ignored -> !node.hasDescription("message"))
                .orElse(null);
        var attributes = Attributes.of()
                .put("id", node.id())
                .put("to", to)
                .put("class", node.description())
                .put("participant", participant, Objects::nonNull)
                .put("recipient", recipient, Objects::nonNull)
                .put("type", type, Objects::nonNull)
                .toMap();
        var receipt = ofAttributes("ack", attributes);
        sendWithNoResponse(receipt);
    }

    protected void onMetadata(Map<String, String> properties) {
        callListenersAsync(listener -> {
            listener.onMetadata(whatsapp, properties);
            listener.onMetadata(properties);
        });
    }

    protected void onMessageStatus(MessageStatus status, Contact participant, MessageInfo message, Chat chat) {
        callListenersAsync(listener -> {
            if (participant == null) {
                listener.onConversationMessageStatus(whatsapp, message, status);
                listener.onConversationMessageStatus(message, status);
            }
            listener.onAnyMessageStatus(whatsapp, chat, participant, message, status);
            listener.onAnyMessageStatus(chat, participant, message, status);
        });
    }

    protected void onUpdateChatPresence(ContactStatus status, Contact contact, Chat chat) {
        callListenersAsync(listener -> {
            listener.onContactPresence(whatsapp, chat, contact, status);
            listener.onContactPresence(chat, contact, status);
        });
    }

    protected void onNewMessage(MessageInfo info, boolean offline) {
        callListenersAsync(listener -> {
            listener.onNewMessage(whatsapp, info);
            listener.onNewMessage(info);
            listener.onNewMessage(whatsapp, info, offline);
            listener.onNewMessage(info, offline);
        });
    }

    protected void onNewStatus(MessageInfo info) {
        callListenersAsync(listener -> {
            listener.onNewStatus(whatsapp, info);
            listener.onNewStatus(info);
        });
    }

    protected void onChatRecentMessages(Chat chat, boolean last) {
        callListenersAsync(listener -> {
            listener.onChatMessagesSync(whatsapp, chat, last);
            listener.onChatMessagesSync(chat, last);
        });
    }

    protected void onFeatures(ActionValueSync.PrimaryFeature features) {
        callListenersAsync(listener -> {
            listener.onFeatures(whatsapp, features.flags());
            listener.onFeatures(features.flags());
        });
    }

    protected void onSetting(Setting setting) {
        callListenersAsync(listener -> {
            listener.onSetting(whatsapp, setting);
            listener.onSetting(setting);
        });
    }

    protected void onMessageDeleted(MessageInfo message, boolean everyone) {
        callListenersAsync(listener -> {
            listener.onMessageDeleted(whatsapp, message, everyone);
            listener.onMessageDeleted(message, everyone);
        });
    }

    protected void onAction(Action action, MessageIndexInfo indexInfo) {
        callListenersAsync(listener -> {
            listener.onAction(whatsapp, action, indexInfo);
            listener.onAction(action, indexInfo);
        });
    }

    protected void onDisconnected(DisconnectReason loggedOut) {
        callListenersSync(listener -> {
            listener.onDisconnected(whatsapp, loggedOut);
            listener.onDisconnected(loggedOut);
        });
    }

    protected void onLoggedIn() {
        callListenersSync(listener -> {
            listener.onLoggedIn(whatsapp);
            listener.onLoggedIn();
        });
    }

    public void callListenersSync(Consumer<Listener> consumer) {
        if (state == SocketState.DISCONNECTED || state == SocketState.LOGGED_OUT) {
            return;
        }
        var service = getOrCreateService();
        var futures = store.listeners()
                .stream()
                .map(listener -> runAsync(() -> consumer.accept(listener), service))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    protected void onChats() {
        callListenersSync(listener -> {
            listener.onChats(whatsapp, store().chats());
            listener.onChats(store().chats());
        });
    }

    protected void onMediaStatus() {
        callListenersAsync(listener -> {
            listener.onMediaStatus(whatsapp, store().status());
            listener.onMediaStatus(store().status());
        });
    }

    protected void onContacts() {
        callListenersSync(listener -> {
            listener.onContacts(whatsapp, store().contacts());
            listener.onContacts(store().contacts());
        });
    }

    protected void onPrivacySettings() {
        callListenersAsync(listener -> {
            listener.onPrivacySettings(whatsapp, store().privacySettings());
            listener.onPrivacySettings(store().privacySettings());
        });
    }

    protected void onHistorySyncProgress(Integer progress, boolean recent) {
        callListenersAsync(listener -> {
            listener.onHistorySyncProgress(whatsapp, progress, recent);
            listener.onHistorySyncProgress(progress, recent);
        });
    }

    protected void onReply(MessageInfo info) {
        var quoted = info.quotedMessage().orElse(null);
        if (quoted == null) {
            return;
        }
        store.resolvePendingReply(info);
        callListenersAsync(listener -> {
            listener.onMessageReply(whatsapp, info, quoted);
            listener.onMessageReply(info, quoted);
        });
    }

    protected void onGroupPictureChange(Chat fromChat) {
        callListenersAsync(listener -> {
            listener.onGroupPictureChange(whatsapp, fromChat);
            listener.onGroupPictureChange(fromChat);
        });
    }

    protected void onContactPictureChange(Contact fromContact) {
        callListenersAsync(listener -> {
            listener.onContactPictureChange(whatsapp, fromContact);
            listener.onContactPictureChange(fromContact);
        });
    }

    protected void onUserStatusChange(String newStatus, String oldStatus) {
        callListenersAsync(listener -> {
            listener.onUserStatusChange(whatsapp, oldStatus, newStatus);
            listener.onUserStatusChange(oldStatus, newStatus);
        });
    }

    public void onUserPictureChange(URI newPicture, URI oldPicture) {
        callListenersAsync(listener -> {
            listener.onUserPictureChange(whatsapp, oldPicture, newPicture);
            listener.onUserPictureChange(oldPicture, newPicture);
        });
    }

    public void updateUserName(String newName, String oldName) {
        if (oldName != null && !Objects.equals(newName, oldName)) {
            sendWithNoResponse(Node.ofAttributes("presence", Map.of("name", oldName, "type", "unavailable")));
            sendWithNoResponse(Node.ofAttributes("presence", Map.of("name", newName, "type", "available")));
            callListenersAsync(listener -> {
                listener.onUserNameChange(whatsapp, oldName, newName);
                listener.onUserStatusChange(oldName, newName);
            });
        }
        var self = store().userCompanionJid().toUserJid();
        store().findContactByJid(self).orElseGet(() -> store().addContact(self)).chosenName(newName);
        store().userName(newName);
    }

    public void updateLocale(String newLocale, String oldLocale) {
        if (!Objects.equals(newLocale, oldLocale)) {
            return;
        }
        if (oldLocale != null) {
            callListenersAsync(listener -> {
                listener.onUserLocaleChange(whatsapp, oldLocale, newLocale);
                listener.onUserLocaleChange(oldLocale, newLocale);
            });
        }
        store().userLocale(newLocale);
    }

    protected void onContactBlocked(Contact contact) {
        callListenersAsync(listener -> {
            listener.onContactBlocked(whatsapp, contact);
            listener.onContactBlocked(contact);
        });
    }

    protected void onNewContact(Contact contact) {
        callListenersAsync(listener -> {
            listener.onNewContact(whatsapp, contact);
            listener.onNewContact(contact);
        });
    }

    protected void querySessionsForcefully(ContactJid contactJid) {
        messageHandler.querySessions(List.of(contactJid), true);
    }

    protected void disableAppStateSync() {
        appStateHandler.completeLatch();
    }
}
