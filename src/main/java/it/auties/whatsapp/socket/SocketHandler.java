package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.*;
import it.auties.whatsapp.api.ErrorHandler.Location;
import it.auties.whatsapp.binary.BinaryDecoder;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.*;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.ChatMessageKeyBuilder;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.request.MessageSendRequest;
import it.auties.whatsapp.model.response.ContactStatusResponse;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.signal.auth.ClientHelloBuilder;
import it.auties.whatsapp.model.signal.auth.HandshakeMessageBuilder;
import it.auties.whatsapp.model.signal.auth.HandshakeMessageSpec;
import it.auties.whatsapp.model.sync.PatchRequest;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.model.sync.PrimaryFeature;
import it.auties.whatsapp.util.Clock;

import java.net.SocketException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static it.auties.whatsapp.model.chat.GroupSetting.*;

@SuppressWarnings("unused")
public class SocketHandler implements SocketListener {
    private static final Executor DEFAULT_EXECUTOR = ForkJoinPool.getCommonPoolParallelism() > 1 ? ForkJoinPool.commonPool() : runnable -> new Thread(runnable).start();

    private static final Set<UUID> connectedUuids = ConcurrentHashMap.newKeySet();
    private static final Set<Long> connectedPhoneNumbers = ConcurrentHashMap.newKeySet();
    private static final Set<String> connectedAlias = ConcurrentHashMap.newKeySet();

    private SocketSession session;

    private final Whatsapp whatsapp;

    private final AuthHandler authHandler;

    private final StreamHandler streamHandler;

    private final MessageHandler messageHandler;

    private final AppStateHandler appStateHandler;

    private final ErrorHandler errorHandler;

    private final Executor socketExecutor;

    private volatile SocketState state;

    private Keys keys;

    private Store store;

    private Thread shutdownHook;

    private ExecutorService listenersService;

    public static boolean isConnected(UUID uuid) {
        return connectedUuids.contains(uuid);
    }

    public static boolean isConnected(long phoneNumber) {
        return connectedPhoneNumbers.contains(phoneNumber);
    }

    public static boolean isConnected(String id) {
        return connectedAlias.contains(id);
    }

    public SocketHandler(Whatsapp whatsapp, Store store, Keys keys, ErrorHandler errorHandler, WebVerificationSupport webVerificationSupport, Executor socketExecutor) {
        this.whatsapp = whatsapp;
        this.store = store;
        this.keys = keys;
        this.state = SocketState.WAITING;
        this.authHandler = new AuthHandler(this);
        this.streamHandler = new StreamHandler(this, webVerificationSupport);
        this.messageHandler = new MessageHandler(this);
        this.appStateHandler = new AppStateHandler(this);
        this.errorHandler = Objects.requireNonNullElse(errorHandler, ErrorHandler.toTerminal());
        this.socketExecutor = Objects.requireNonNullElse(socketExecutor, DEFAULT_EXECUTOR);
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

    protected void onSocketEvent(SocketEvent event) {
        callListenersAsync(listener -> {
            listener.onSocketEvent(whatsapp, event);
            listener.onSocketEvent(event);
        });
    }

    private void callListenersAsync(Consumer<Listener> consumer) {
        var service = getOrCreateListenersService();
        store.listeners().forEach(listener -> service.execute(() -> invokeListenerSafe(consumer, listener)));
    }

    @Override
    public void onOpen(SocketSession session) {
        this.session = session;
        if (state == SocketState.CONNECTED) {
            return;
        }

        if (shutdownHook == null) {
            this.shutdownHook = new Thread(() -> onShutdown(false));
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        markConnected();
        this.state = SocketState.WAITING;
        onSocketEvent(SocketEvent.OPEN);
        var clientHello = new ClientHelloBuilder()
                .ephemeral(keys.ephemeralKeyPair().publicKey())
                .build();
        var handshakeMessage = new HandshakeMessageBuilder()
                .clientHello(clientHello)
                .build();
        SocketRequest.of(HandshakeMessageSpec.encode(handshakeMessage))
                .sendWithPrologue(session, keys, store)
                .exceptionallyAsync(throwable -> handleFailure(LOGIN, throwable));
    }

    protected void markConnected() {
        connectedUuids.add(store.uuid());
        store.phoneNumber()
                .map(PhoneNumber::number)
                .ifPresent(connectedPhoneNumbers::add);
        connectedAlias.addAll(store.alias());
    }

    @Override
    public void onMessage(byte[] message) {
        if (state != SocketState.CONNECTED && state != SocketState.RESTORE) {
            authHandler.login(session, message)
                    .thenApplyAsync(result -> result ? setState(SocketState.CONNECTED) : null)
                    .exceptionallyAsync(throwable -> handleFailure(LOGIN, throwable));
            return;
        }

        var readKey = keys.readKey();
        if (readKey.isEmpty()) {
            return;
        }

        try {
            var plainText = AesGcm.decrypt(keys.readCounter(true), message, readKey.get());
            var decoder = new BinaryDecoder();
            var node = decoder.decode(plainText);
            onNodeReceived(node);
            store.resolvePendingRequest(node, false);
            streamHandler.digest(node);
        }catch (Throwable throwable) {
            handleFailure(CRYPTOGRAPHY, throwable);
        }
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
    }

    @Override
    public void onError(Throwable throwable) {
        if (isIgnorableSocketError(throwable)) {
            return;
        }
        onSocketEvent(SocketEvent.ERROR);
        handleFailure(UNKNOWN, throwable);
    }

    private boolean isIgnorableSocketError(Throwable throwable) {
        return throwable instanceof SocketException socketException
                && Objects.equals(socketException.getMessage(), "Socket closed")
                && (state() == SocketState.RECONNECTING || state() == SocketState.DISCONNECTED);
    }

    public CompletableFuture<Void> connect() {
        if (state == SocketState.CONNECTED) {
            return CompletableFuture.completedFuture(null);
        }

        this.session = SocketSession.of(store.proxy().orElse(null), socketExecutor, store.clientType() == ClientType.WEB);
        return session.connect(this);
    }

    public CompletableFuture<Void> disconnect(DisconnectReason reason) {
        var newState = SocketState.of(reason);
        if(state == newState) {
            return CompletableFuture.completedFuture(null);
        }

        setState(newState);
        keys.clearReadWriteKey();
        return switch (reason) {
            case DISCONNECTED -> {
                if (session != null) {
                    session.disconnect();
                }
                yield CompletableFuture.completedFuture(null);
            }
            case RECONNECTING -> {
                if (session != null) {
                    session.disconnect();
                }
                yield connect();
            }
            case LOGGED_OUT -> {
                store.deleteSession();
                store.resolveAllPendingRequests();
                if (session != null) {
                    session.disconnect();
                }
                yield CompletableFuture.completedFuture(null);
            }
            case RESTORE -> {
                store.deleteSession();
                store.resolveAllPendingRequests();
                var oldListeners = new ArrayList<>(store.listeners());
                if (session != null) {
                    session.disconnect();
                }
                var uuid = UUID.randomUUID();
                var number = store.phoneNumber()
                        .map(PhoneNumber::number)
                        .orElse(null);
                this.keys = Keys.builder()
                        .uuid(uuid)
                        .clientType(keys.clientType())
                        .build();
                this.store = Store.builder()
                        .uuid(uuid)
                        .clientType(store.clientType())
                        .device(store.device().orElse(null))
                        .build();
                store.addListeners(oldListeners);
                yield connect();
            }
        };
    }

    public CompletableFuture<Void> pushPatch(PatchRequest request) {
        var jid = store.jid().orElseThrow(() -> new IllegalStateException("The session isn't connected"));
        return appStateHandler.push(jid, List.of(request));
    }

    public CompletableFuture<Void> pushPatches(Jid jid, List<PatchRequest> requests) {
        return appStateHandler.push(jid, requests);
    }

    public void pullPatch(PatchType... patchTypes) {
        appStateHandler.pull(patchTypes);
    }

    protected CompletableFuture<Void> pullInitialPatches() {
        return appStateHandler.pullInitial();
    }

    public void decodeMessage(Node node, JidProvider chatOverride, boolean notify) {
        messageHandler.decode(node, chatOverride, notify);
    }

    public CompletableFuture<Void> sendPeerMessage(Jid companion, ProtocolMessage message) {
        if (message == null) {
            return CompletableFuture.completedFuture(null);
        }

        var jid = store.jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"));
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId())
                .chatJid(companion)
                .fromMe(true)
                .senderJid(jid)
                .build();
        var info = new ChatMessageInfoBuilder()
                .status(MessageStatus.PENDING)
                .senderJid(jid)
                .key(key)
                .message(MessageContainer.of(message))
                .timestampSeconds(Clock.nowSeconds())
                .build();
        var request = new MessageSendRequest.Chat(info, null, false, true, null);
        return sendMessage(request);
    }

    public CompletableFuture<Void> sendMessage(MessageSendRequest request) {
        return messageHandler.encode(request);
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<Void> sendQueryWithNoResponse(String method, String category, Node... body) {
        return sendQueryWithNoResponse(null, JidServer.WHATSAPP.toJid(), method, category, null, body);
    }

    public CompletableFuture<Void> sendQueryWithNoResponse(String id, Jid to, String method, String category, Map<String, Object> metadata, Node... body) {
        var attributes = Attributes.ofNullable(metadata)
                .put("id", id, Objects::nonNull)
                .put("type", method)
                .put("to", to)
                .put("xmlns", category, Objects::nonNull)
                .toMap();
        return sendWithNoResponse(Node.of("iq", attributes, body));
    }

    public CompletableFuture<Void> sendWithNoResponse(Node node) {
        if (state() == SocketState.RESTORE) {
            return CompletableFuture.completedFuture(null);
        }

        return node.toRequest(null, false)
                .sendWithNoResponse(session, keys, store)
                .exceptionallyAsync(throwable -> handleFailure(STREAM, throwable))
                .thenRunAsync(() -> onNodeSent(node));
    }

    private void onNodeSent(Node node) {
        callListenersAsync(listener -> {
            listener.onNodeSent(whatsapp, node);
            listener.onNodeSent(node);
        });
    }

    public CompletableFuture<Optional<ContactStatusResponse>> queryAbout(JidProvider chat) {
        var query = Node.of("status");
        var body = Node.of("user", Map.of("jid", chat.toJid()));
        return sendInteractiveQuery(query, body).thenApplyAsync(this::parseStatus);
    }

    public CompletableFuture<List<Node>> sendInteractiveQuery(Node queryNode, Node... queryBody) {
        var query = Node.of("query", queryNode);
        var list = Node.of("list", queryBody);
        var sync = Node.of("usync",
                Map.of("sid", ChatMessageKey.randomId(), "mode", "query", "last", "true", "index", "0", "context", "interactive"),
                query, list);
        return sendQuery("get", "usync", sync).thenApplyAsync(this::parseQueryResult);
    }

    private Optional<ContactStatusResponse> parseStatus(List<Node> responses) {
        return responses.stream()
                .map(entry -> entry.findNode("status"))
                .flatMap(Optional::stream)
                .findFirst()
                .map(ContactStatusResponse::ofNode);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Node... body) {
        return sendQuery(null, JidServer.WHATSAPP.toJid(), method, category, null, body);
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

    public CompletableFuture<Node> sendQuery(String id, Jid to, String method, String category, Map<String, Object> metadata, Node... body) {
        var attributes = Attributes.ofNullable(metadata)
                .put("xmlns", category, Objects::nonNull)
                .put("id", id, Objects::nonNull)
                .put("to", to)
                .put("type", method)
                .toMap();
        return send(Node.of("iq", attributes, body));
    }

    public CompletableFuture<Node> send(Node node) {
        return send(node, null);
    }

    public CompletableFuture<Node> send(SocketRequest request) {
        return request.send(session, keys, store);
    }

    public CompletableFuture<Node> send(Node node, Function<Node, Boolean> filter) {
        if (state() == SocketState.RESTORE) {
            return CompletableFuture.completedFuture(node);
        }

        var request = node.toRequest(filter, true);
        var result = request.send(session, keys, store);
        onNodeSent(node);
        return result;
    }

    public CompletableFuture<Optional<URI>> queryPicture(JidProvider chat) {
        var body = Node.of("picture", Map.of("query", "url", "type", "image"));
        if (chat.toJid().hasServer(JidServer.GROUP)) {
            return queryGroupMetadata(chat.toJid())
                    .thenComposeAsync(result -> sendQuery("get", "w:profile:picture", Map.of(result.isCommunity() ? "parent_group_jid" : "target", chat.toJid()), body))
                    .thenApplyAsync(this::parseChatPicture);
        }

        return sendQuery("get", "w:profile:picture", Map.of("target", chat.toJid()), body)
                .thenApplyAsync(this::parseChatPicture);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Map<String, Object> metadata, Node... body) {
        return sendQuery(null, JidServer.WHATSAPP.toJid(), method, category, metadata, body);
    }

    private Optional<URI> parseChatPicture(Node result) {
        return result.findNode("picture")
                .flatMap(picture -> picture.attributes().getOptionalString("url"))
                .map(URI::create);
    }

    public CompletableFuture<List<Jid>> queryBlockList() {
        return sendQuery("get", "blocklist", (Node) null)
                .thenApplyAsync(this::parseBlockList);
    }

    private List<Jid> parseBlockList(Node result) {
        return result.findNode("list")
                .orElseThrow(() -> new NoSuchElementException("Missing block list in newsletters"))
                .findNodes("item")
                .stream()
                .map(item -> item.attributes().getJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    public CompletableFuture<Void> subscribeToPresence(JidProvider jid) {
        var node = Node.of("presence", Map.of("to", jid.toJid(), "type", "subscribe"));
        return sendWithNoResponse(node);
    }

    public CompletableFuture<OptionalLong> subscribeToNewsletterReactions(JidProvider channel) {
        return sendQuery(channel.toJid(), "set", "newsletter", Node.of("live_updates"))
                .thenApplyAsync(this::parseNewsletterSubscription);
    }

    private OptionalLong parseNewsletterSubscription(Node result) {
        return result.findNode("live_updates")
                .stream()
                .map(node -> node.attributes().getOptionalLong("duration"))
                .flatMapToLong(OptionalLong::stream)
                .findFirst();
    }

    public CompletableFuture<Void> queryNewsletterMessages(JidProvider newsletterJid, int count) {
        var newsletter = store.findNewsletterByJid(newsletterJid)
                .orElseThrow(() -> new NoSuchElementException("Missing newsletter"));
        return sendQuery("get", "newsletter", Node.of("messages", Map.of("count", count, "type", "invite", "key", newsletter.metadata().invite())))
                .thenAcceptAsync(result -> onNewsletterMessages(newsletter, result));
    }

    private void onNewsletterMessages(Newsletter newsletter, Node result) {
        result.findNode("messages")
                .stream()
                .map(messages -> messages.findNodes("message"))
                .flatMap(Collection::stream)
                .forEach(messages -> decodeMessage(messages, newsletter, false));
    }

    public CompletableFuture<GroupMetadata> queryGroupMetadata(JidProvider group) {
        var body = Node.of("query", Map.of("request", "interactive"));
        return sendQuery(group.toJid(), "get", "w:g2", body)
                .thenApplyAsync(this::handleGroupMetadata);
    }

    protected GroupMetadata handleGroupMetadata(Node response) {
        var metadata = Optional.of(response)
                .filter(entry -> entry.hasDescription("group"))
                .or(() -> response.findNode("group"))
                .map(this::parseGroupMetadata)
                .orElseThrow(() -> new NoSuchElementException("Erroneous response: %s".formatted(response)));
        var chat = store.findChatByJid(metadata.jid())
                .orElseGet(() -> store().addNewChat(metadata.jid()));
        if (chat != null) {
            metadata.foundationTimestamp().ifPresent(timestamp -> chat.setFoundationTimestampSeconds(timestamp.toEpochSecond()));
            metadata.founder().ifPresent(chat::setFounder);
            metadata.description().ifPresent(chat::setDescription);
            chat.addParticipants(metadata.participants());
        }

        return metadata;
    }

    public GroupMetadata parseGroupMetadata(Node node) {
        var groupId = node.attributes()
                .getOptionalString("id")
                .map(id -> Jid.of(id, JidServer.GROUP))
                .orElseThrow(() -> new NoSuchElementException("Missing group jid"));
        var subject = node.attributes().getString("subject");
        var subjectAuthor = node.attributes().getJid("s_o");
        var subjectTimestampSeconds = node.attributes()
                .getOptionalLong("s_t")
                .orElse(0L);
        var foundationTimestampSeconds = node.attributes()
                .getOptionalLong("creation")
                .orElse(0L);
        var founder = node.attributes().getJid("creator");
        var policies = new HashMap<GroupSetting, GroupSettingPolicy>();
        policies.put(SEND_MESSAGES, GroupSettingPolicy.of(node.hasNode("restrict")));
        policies.put(EDIT_GROUP_INFO, GroupSettingPolicy.of(node.hasNode("announce")));
        policies.put(APPROVE_NEW_PARTICIPANTS, GroupSettingPolicy.of(node.hasNode("membership_approval_mode")));
        var description = node.findNode("description")
                .flatMap(parent -> parent.findNode("body"))
                .flatMap(Node::contentAsString);
        var descriptionId = node.findNode("description")
                .map(Node::attributes)
                .flatMap(attributes -> attributes.getOptionalString("id"));
        var community = node.findNode("parent")
                .isPresent();
        var openCommunity = node.findNode("parent")
                .filter(entry -> entry.attributes().hasValue("default_membership_approval_mode", "request_required"))
                .isEmpty();
        var ephemeral = node.findNode("ephemeral")
                .map(Node::attributes)
                .map(attributes -> attributes.getLong("expiration"))
                .flatMap(Clock::parseSeconds);
        var participants = node.findNodes("participant")
                .stream()
                .map(this::parseGroupParticipant)
                .toList();
        return new GroupMetadata(groupId, subject, subjectAuthor, Clock.parseSeconds(subjectTimestampSeconds), Clock.parseSeconds(foundationTimestampSeconds), founder, description, descriptionId, Collections.unmodifiableMap(policies), participants, ephemeral, community, openCommunity);
    }

    private GroupParticipant parseGroupParticipant(Node node) {
        var id = node.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing participant in group newsletters"));
        var role = GroupRole.of(node.attributes().getString("type", null));
        return new GroupParticipant(id, role);
    }

    public CompletableFuture<Node> sendQuery(Jid to, String method, String category, Node... body) {
        return sendQuery(null, to, method, category, null, body);
    }

    public CompletableFuture<Void> sendReceipt(Jid jid, Jid participant, List<String> messages, String type) {
        if (messages.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var attributes = Attributes.of()
                .put("id", messages.get(0))
                .put("t", Clock.nowMilliseconds(), () -> Objects.equals(type, "read") || Objects.equals(type, "read-self"))
                .put("to", jid)
                .put("type", type, Objects::nonNull);
        if (Objects.equals(type, "sender") && jid.hasServer(JidServer.WHATSAPP)) {
            attributes.put("recipient", jid);
            attributes.put("to", participant);
        } else {
            attributes.put("to", jid);
            attributes.put("participant", participant, Objects::nonNull);
        }
        var receipt = Node.of("receipt", attributes.toMap(), toMessagesNode(messages));
        return sendWithNoResponse(receipt);
    }

    private List<Node> toMessagesNode(List<String> messages) {
        if (messages.size() <= 1) {
            return null;
        }
        return messages.subList(1, messages.size())
                .stream()
                .map(id -> Node.of("item", Map.of("id", id)))
                .toList();
    }

    protected CompletableFuture<Void> sendMessageAck(Jid from, Node node) {
        var attrs = node.attributes();
        var type = attrs.getOptionalString("type")
                .filter(entry -> !Objects.equals(entry, "message"))
                .orElse(null);
        var attributes = Attributes.of()
                .put("id", node.id())
                .put("to", from)
                .put("class", node.description())
                .put("participant", attrs.getNullableString("participant"), Objects::nonNull)
                .put("recipient", attrs.getNullableString("recipient"), Objects::nonNull)
                .put("type", type, Objects::nonNull)
                .toMap();
        return sendWithNoResponse(Node.of("ack", attributes));
    }

    protected void onRegistrationCode(long code) {
        callListenersAsync(listener -> {
            listener.onRegistrationCode(whatsapp, code);
            listener.onRegistrationCode(code);
        });
    }

    protected void onMetadata(Map<String, String> properties) {
        callListenersAsync(listener -> {
            listener.onMetadata(whatsapp, properties);
            listener.onMetadata(properties);
        });
    }


    protected void onMessageStatus(MessageInfo message) {
        callListenersAsync(listener -> {
            listener.onMessageStatus(whatsapp, message);
            listener.onMessageStatus(message);
        });
    }

    protected void onUpdateChatPresence(ContactStatus status, Jid jid, Chat chat) {
        var contact = store.findContactByJid(jid);
        if (contact.isPresent()) {
            contact.get().setLastKnownPresence(status);
            if (status == contact.get().lastKnownPresence()) {
                return;
            }

            contact.get().setLastSeen(ZonedDateTime.now());
        }

        chat.presences().put(jid, status);
        callListenersAsync(listener -> {
            listener.onContactPresence(whatsapp, chat, jid, status);
            listener.onContactPresence(chat, jid, status);
        });
    }

    protected void onNewMessage(ChatMessageInfo info) {
        callListenersAsync(listener -> {
            listener.onNewMessage(whatsapp, info);
            listener.onNewMessage(info);
        });
    }

    protected void onNewStatus(ChatMessageInfo info) {
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

    protected void onFeatures(PrimaryFeature features) {
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

    protected void onMessageDeleted(ChatMessageInfo message, boolean everyone) {
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
        if (loggedOut != DisconnectReason.RECONNECTING) {
            connectedUuids.remove(store.uuid());
            store.phoneNumber()
                    .map(PhoneNumber::number)
                    .ifPresent(connectedPhoneNumbers::remove);
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        }
        callListenersSync(listener -> {
            listener.onDisconnected(whatsapp, loggedOut);
            listener.onDisconnected(loggedOut);
        });
    }

    protected void onLoggedIn() {
        callListenersAsync(listener -> {
            listener.onLoggedIn(whatsapp);
            listener.onLoggedIn();
        });
    }

    public void callListenersSync(Consumer<Listener> consumer) {
        var service = getOrCreateListenersService();
        var futures = store.listeners()
                .stream()
                .map(listener -> CompletableFuture.runAsync(() -> invokeListenerSafe(consumer, listener), service))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    private void invokeListenerSafe(Consumer<Listener> consumer, Listener listener) {
        try {
            consumer.accept(listener);
        } catch (Throwable throwable) {
            handleFailure(UNKNOWN, throwable);
        }
    }

    protected void onChats() {
        callListenersAsync(listener -> {
            listener.onChats(whatsapp, store().chats());
            listener.onChats(store().chats());
        });
    }

    protected void onNewsletters() {
        callListenersAsync(listener -> {
            listener.onNewsletters(whatsapp, store().newsletters());
            listener.onNewsletters(store().newsletters());
        });
    }

    protected void onNewsletterMessage(NewsletterMessageInfo messageInfo) {
        callListenersAsync(listener -> {
            listener.onNewMessage(whatsapp, messageInfo);
            listener.onNewMessage(messageInfo);
        });
    }

    protected void onStatus() {
        callListenersAsync(listener -> {
            listener.onStatus(whatsapp, store().status());
            listener.onStatus(store().status());
        });
    }

    protected void onContacts() {
        callListenersAsync(listener -> {
            listener.onContacts(whatsapp, store().contacts());
            listener.onContacts(store().contacts());
        });
    }

    protected void onHistorySyncProgress(Integer progress, boolean recent) {
        callListenersAsync(listener -> {
            listener.onHistorySyncProgress(whatsapp, progress, recent);
            listener.onHistorySyncProgress(progress, recent);
        });
    }

    protected void onReply(ChatMessageInfo info) {
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

    protected void onGroupPictureChanged(Chat fromChat) {
        callListenersAsync(listener -> {
            listener.onGroupPictureChanged(whatsapp, fromChat);
            listener.onGroupPictureChanged(fromChat);
        });
    }

    protected void onContactPictureChanged(Contact fromContact) {
        callListenersAsync(listener -> {
            listener.onProfilePictureChanged(whatsapp, fromContact);
            listener.onProfilePictureChanged(fromContact);
        });
    }

    protected void onUserAboutChanged(String newAbout, String oldAbout) {
        callListenersAsync(listener -> {
            listener.onAboutChanged(whatsapp, oldAbout, newAbout);
            listener.onAboutChanged(oldAbout, newAbout);
        });
    }

    public void onUserPictureChanged(URI newPicture, URI oldPicture) {
        callListenersAsync(listener -> {
            listener.onProfilePictureChanged(whatsapp, oldPicture, newPicture);
            listener.onProfilePictureChanged(oldPicture, newPicture);
        });
    }

    public void updateUserName(String newName, String oldName) {
        if (oldName != null && !Objects.equals(newName, oldName)) {
            sendWithNoResponse(Node.of("presence", Map.of("name", oldName, "type", "unavailable")));
            sendWithNoResponse(Node.of("presence", Map.of("name", newName, "type", "available")));
            onUserNameChanged(newName, oldName);
        }
        var self = store.jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"))
                .withoutDevice();
        store().findContactByJid(self)
                .orElseGet(() -> store().addContact(self))
                .setChosenName(newName);
        store().setName(newName);
    }

    private void onUserNameChanged(String newName, String oldName) {
        callListenersAsync(listener -> {
            listener.onNameChanged(whatsapp, oldName, newName);
            listener.onNameChanged(oldName, newName);
        });
    }

    public void updateLocale(String newLocale, String oldLocale) {
        if (!Objects.equals(newLocale, oldLocale)) {
            return;
        }
        if (oldLocale != null) {
            onUserLocaleChanged(newLocale, oldLocale);
        }
        store().setLocale(newLocale);
    }

    private void onUserLocaleChanged(String newLocale, String oldLocale) {
        callListenersAsync(listener -> {
            listener.onLocaleChanged(whatsapp, oldLocale, newLocale);
            listener.onLocaleChanged(oldLocale, newLocale);
        });
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

    protected void onDevices(LinkedHashMap<Jid, Integer> devices) {
        callListenersAsync(listener -> {
            listener.onLinkedDevices(whatsapp, devices.keySet());
            listener.onLinkedDevices(devices.keySet());
        });
    }

    public void onCall(Call call) {
        callListenersAsync(listener -> {
            listener.onCall(whatsapp, call);
            listener.onCall(call);
        });
    }

    public void onPrivacySettingChanged(PrivacySettingEntry oldEntry, PrivacySettingEntry newEntry) {
        callListenersAsync(listener -> {
            listener.onPrivacySettingChanged(whatsapp, oldEntry, newEntry);
            listener.onPrivacySettingChanged(oldEntry, newEntry);
        });
    }

    protected void querySessionsForcefully(Jid jid) {
        messageHandler.querySessions(List.of(jid), true);
    }

    private void dispose() {
        onSocketEvent(SocketEvent.CLOSE);
        streamHandler.dispose();
        messageHandler.dispose();
        appStateHandler.dispose();
        if (listenersService != null) {
            listenersService.shutdownNow();
        }
    }

    private ExecutorService getOrCreateListenersService() {
        if (listenersService == null || listenersService.isShutdown()) {
            listenersService = Executors.newCachedThreadPool();
        }

        return listenersService;
    }

    protected <T> T handleFailure(Location location, Throwable throwable) {
        if (state() == SocketState.RESTORE || state() == SocketState.LOGGED_OUT) {
            return null;
        }
        var result = errorHandler.handleError(store.clientType(), location, throwable);
        switch (result) {
            case RESTORE -> disconnect(DisconnectReason.RESTORE);
            case LOG_OUT -> disconnect(DisconnectReason.LOGGED_OUT);
            case DISCONNECT -> disconnect(DisconnectReason.DISCONNECTED);
            case RECONNECT -> disconnect(DisconnectReason.RECONNECTING);
        }
        return null;
    }

    public CompletableFuture<Void> querySessions(Jid jid) {
        return messageHandler.getDevices(List.of(jid), true)
                .thenCompose(values -> messageHandler.querySessions(values, false));
    }

    public void parseSessions(Node result) {
        messageHandler.parseSessions(result);
    }

    public CompletableFuture<List<BusinessCategory>> queryBusinessCategories() {
        return sendQuery("get", "fb:thrift_iq", Node.of("request", Map.of("op", "profile_typeahead", "type", "catkit", "v", "1"), Node.of("query", List.of())))
                .thenApplyAsync(this::parseBusinessCategories);
    }

    private List<BusinessCategory> parseBusinessCategories(Node result) {
        return result.findNode("result")
                .flatMap(entry -> entry.findNode("categories"))
                .stream()
                .map(entry -> entry.findNodes("category"))
                .flatMap(Collection::stream)
                .map(BusinessCategory::of)
                .toList();
    }

    public Whatsapp whatsapp() {
        return this.whatsapp;
    }

    public SocketState state() {
        return this.state;
    }

    public Keys keys() {
        return this.keys;
    }

    public Store store() {
        return this.store;
    }

    protected SocketHandler setState(SocketState state) {
        this.state = state;
        return this;
    }
}
