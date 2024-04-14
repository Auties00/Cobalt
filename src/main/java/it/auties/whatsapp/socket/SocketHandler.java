package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.*;
import it.auties.whatsapp.api.ErrorHandler.Location;
import it.auties.whatsapp.binary.BinaryDecoder;
import it.auties.whatsapp.binary.BinaryEncoder;
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
import it.auties.whatsapp.model.mobile.CountryLocale;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.request.MessageSendRequest;
import it.auties.whatsapp.model.response.ContactAboutResponse;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.signal.auth.ClientHelloBuilder;
import it.auties.whatsapp.model.signal.auth.HandshakeMessageBuilder;
import it.auties.whatsapp.model.signal.auth.HandshakeMessageSpec;
import it.auties.whatsapp.model.sync.PatchRequest;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.model.sync.PrimaryFeature;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Exceptions;
import it.auties.whatsapp.util.Specification;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static it.auties.whatsapp.model.chat.GroupSetting.*;

@SuppressWarnings("unused")
public class SocketHandler implements SocketListener {
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

    private final AtomicLong requestsCounter;
    private final ScheduledExecutorService scheduler;
    private final Semaphore writeSemaphore;

    private volatile SocketState state;

    private volatile CompletableFuture<Void> loginFuture;

    private Keys keys;

    private Store store;

    private Thread shutdownHook;

    public static boolean isConnected(UUID uuid) {
        return connectedUuids.contains(uuid);
    }

    public static boolean isConnected(long phoneNumber) {
        return connectedPhoneNumbers.contains(phoneNumber);
    }

    public static boolean isConnected(String id) {
        return connectedAlias.contains(id);
    }

    public SocketHandler(Whatsapp whatsapp, Store store, Keys keys, ErrorHandler errorHandler, WebVerificationHandler webVerificationHandler) {
        this.whatsapp = whatsapp;
        this.store = store;
        this.keys = keys;
        this.state = SocketState.WAITING;
        this.authHandler = new AuthHandler(this);
        this.streamHandler = new StreamHandler(this, webVerificationHandler);
        this.messageHandler = new MessageHandler(this);
        this.appStateHandler = new AppStateHandler(this);
        this.errorHandler = Objects.requireNonNullElse(errorHandler, ErrorHandler.toTerminal());
        this.requestsCounter = new AtomicLong();
        this.scheduler = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory());
        this.writeSemaphore = new Semaphore(1, true);
    }

    private void onShutdown(boolean reconnect) {
        if (state != SocketState.LOGGED_OUT && state != SocketState.RESTORE && state != SocketState.BANNED) {
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
        store.listeners()
                .forEach(listener -> Thread.startVirtualThread(() -> invokeListenerSafe(consumer, listener)));
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

        addToKnownConnections();
        this.state = SocketState.WAITING;
        onSocketEvent(SocketEvent.OPEN);
        var clientHello = new ClientHelloBuilder()
                .ephemeral(keys.ephemeralKeyPair().publicKey())
                .build();
        var handshakeMessage = new HandshakeMessageBuilder()
                .clientHello(clientHello)
                .build();
        sendBinaryWithNoResponse(HandshakeMessageSpec.encode(handshakeMessage), true)
                .exceptionallyAsync(throwable -> handleFailure(LOGIN, throwable));
    }

    protected void addToKnownConnections() {
        connectedUuids.add(store.uuid());
        store.phoneNumber()
                .map(PhoneNumber::number)
                .ifPresent(connectedPhoneNumbers::add);
        connectedAlias.addAll(store.alias());
    }

    @Override
    public void onMessage(byte[] message) {
        if (state != SocketState.CONNECTED && state != SocketState.RESTORE) {
            authHandler.login(message)
                    .thenAcceptAsync(this::onConnectionCreated)
                    .exceptionallyAsync(throwable -> handleFailure(LOGIN, throwable));
            return;
        }

        var readKey = keys.readKey();
        if (readKey.isEmpty()) {
            return;
        }

        var decipheredMessage = decipherMessage(message, readKey.get());
        if(decipheredMessage == null) {
            return;
        }

        try(var decoder = new BinaryDecoder(decipheredMessage)) {
            var node = decoder.decode();
            onNodeReceived(node);
            store.resolvePendingRequest(node, false);
            streamHandler.digest(node);
        } catch (Throwable throwable) {
            handleFailure(STREAM, throwable);
        }
    }

    private void onConnectionCreated(Boolean result) {
        if (result == null || !result) {
            return;
        }

        setState(SocketState.CONNECTED);
    }

    private byte[] decipherMessage(byte[] message, byte[] readKey) {
        try {
            return AesGcm.decrypt(keys.nextReadCounter(true), message, readKey);
        }  catch (Throwable throwable) {
            return null;
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

    public CompletableFuture<Node> sendNode(Node node) {
        return sendNode(node, null);
    }

    public CompletableFuture<Node> sendNode(Node node, Function<Node, Boolean> filter) {
        if (node.id() == null) {
            node.attributes().put("id", store.initializationTimeStamp() + "-" + requestsCounter.incrementAndGet());
        }

        return sendRequest(SocketRequest.of(node, filter), false, true);
    }

    public CompletableFuture<Void> sendNodeWithNoResponse(Node node) {
        return sendRequest(SocketRequest.of(node, null), false, false)
                .thenRun(() -> {});
    }

    public CompletableFuture<Void> sendBinaryWithNoResponse(byte[] binary, boolean prologue) {
        return sendRequest(SocketRequest.of(binary), prologue, false)
                .thenRun(() -> {});
    }

    private CompletableFuture<Node> sendRequest(SocketRequest request, boolean prologue, boolean response) {
        if (state() == SocketState.RESTORE) {
            return CompletableFuture.completedFuture(Node.of("error", Map.of("closed", true)));
        }

        var byteArrayOutputStream = new ByteArrayOutputStream();
        try(var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            writeSemaphore.acquire();
            var ciphered = encryptRequest(request);
            if(prologue) {
                dataOutputStream.write(switch (store.clientType()) {
                    case WEB -> Specification.Whatsapp.WEB_PROLOGUE;
                    case MOBILE -> Specification.Whatsapp.MOBILE_PROLOGUE;
                });
            }
            dataOutputStream.writeInt(ciphered.length >> 16);
            dataOutputStream.writeShort(65535 & ciphered.length);
            dataOutputStream.write(ciphered);
            session.sendBinary(byteArrayOutputStream.toByteArray()).whenComplete((result, error) -> {
                writeSemaphore.release();
                if(request.body() instanceof Node body) {
                    onNodeSent(body);
                }

                if(error != null) {
                    if(response) {
                        request.future().complete(Node.of("error", Map.of("closed", true))); // Prevent NPEs all over the place
                    }

                    return;
                }

                if (!response) {
                    request.future().complete(null);
                    return;
                }

                store.addRequest(request);
            });
            return request.future();
        }catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }
    }


    private byte[] encryptRequest(SocketRequest request) {
        var body = request.toBytes();
        var writeKey = keys.writeKey();
        if(writeKey.isEmpty()) {
            return body;
        }

        var iv = keys.nextWriteCounter(true);
        return AesGcm.encrypt(iv, body, writeKey.get());
    }

    private byte[] getBody(Object encodedBody) {
        return switch (encodedBody) {
            case byte[] bytes -> bytes;
            case Node node -> {
                try(var encoder = new BinaryEncoder()) {
                    yield encoder.encode(node);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }
            case null, default ->
                    throw new IllegalArgumentException("Cannot create request, illegal body: %s".formatted(encodedBody));
        };
    }

    public CompletableFuture<Void> connect() {
        if (state == SocketState.CONNECTED) {
            return CompletableFuture.completedFuture(null);
        }

        if(loginFuture == null || loginFuture.isDone()) {
            this.loginFuture = new CompletableFuture<>();
        }

        this.session = SocketSession.of(store.proxy().orElse(null), store.clientType() == ClientType.WEB);
        return session.connect(this).exceptionallyAsync(throwable -> {
            if(state == SocketState.CONNECTED || state == SocketState.RECONNECTING || state == SocketState.PAUSED) {
                setState(SocketState.PAUSED);
                onSocketEvent(SocketEvent.PAUSED);
                handleFailure(Location.RECONNECT, throwable);
                return null;
            }

            if(loginFuture != null && !loginFuture.isDone()) {
                loginFuture.completeExceptionally(throwable);
            }

            Exceptions.rethrow(throwable);
            return null;
        });
    }

    public CompletableFuture<Void> disconnect(DisconnectReason reason) {
        var newState = SocketState.of(reason);
        if (state == newState) {
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
                store.resolveAllPendingRequests();
                if (session != null) {
                    session.disconnect();
                }
                yield connect();
            }
            case LOGGED_OUT, BANNED -> {
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
                var result = store.serializer()
                        .newStoreKeysPair(uuid, number, store.alias(), store.clientType());
                this.keys = result.keys();
                this.store = result.store();
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
        return sendNodeWithNoResponse(Node.of("iq", attributes, body));
    }

    private SocketRequest createRequest(Node node, Function<Node, Boolean> filter, boolean response) {
        if (response && node.id() == null) {
            node.attributes().put("id", store.initializationTimeStamp() + "-" + requestsCounter.incrementAndGet());
        }

        return SocketRequest.of(node, filter);
    }

    private void onNodeSent(Node node) {
        callListenersAsync(listener -> {
            listener.onNodeSent(whatsapp, node);
            listener.onNodeSent(node);
        });
    }

    public CompletableFuture<Optional<ContactAboutResponse>> queryAbout(JidProvider chat) {
        var query = Node.of("status");
        var body = Node.of("user", Map.of("jid", chat.toJid()));
        return sendInteractiveQuery(List.of(query), List.of(body), List.of())
                .thenApplyAsync(this::parseAbout);
    }

    public CompletableFuture<List<Node>> sendInteractiveQuery(Collection<Node> queries, Collection<Node> listData, Collection<Node> sideListData) {
        var query = Node.of("query", queries);
        var list = Node.of("list", listData);
        var sideList = Node.of("side_list", sideListData);
        var sync = Node.of(
                "usync",
                Map.of("sid", ChatMessageKey.randomId(), "mode", "query", "last", "true", "index", "0", "context", "interactive"),
                query,
                list,
                sideList
        );
        return sendQuery("get", "usync", sync)
                .thenApplyAsync(this::parseQueryResult);
    }

    private Optional<ContactAboutResponse> parseAbout(List<Node> responses) {
        return responses.stream()
                .map(entry -> entry.findNode("status"))
                .flatMap(Optional::stream)
                .findFirst()
                .map(ContactAboutResponse::ofNode);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Node... body) {
        return sendQuery(null, JidServer.WHATSAPP.toJid(), method, category, null, body);
    }

    private List<Node> parseQueryResult(Node result) {
        return result == null ? List.of() : result.findNodes("usync")
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
        return sendNode(Node.of("iq", attributes, body));
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
                .stream()
                .flatMap(node -> node.findNodes("item").stream())
                .map(item -> item.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    public CompletableFuture<Void> subscribeToPresence(JidProvider jid) {
        var node = Node.of("presence", Map.of("to", jid.toJid(), "type", "subscribe"));
        return sendNodeWithNoResponse(node);
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
        var newsletterInvite = newsletter.metadata()
                .invite()
                .orElseThrow(() -> new NoSuchElementException("Missing newsletter key"));
        return sendQuery("get", "newsletter", Node.of("messages", Map.of("count", count, "type", "invite", "key", newsletterInvite)))
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
        var subjectAuthor = node.attributes().getOptionalJid("s_o");
        var subjectTimestampSeconds = node.attributes()
                .getOptionalLong("s_t")
                .orElse(0L);
        var foundationTimestampSeconds = node.attributes()
                .getOptionalLong("creation")
                .orElse(0L);
        var founder = node.attributes()
                .getOptionalJid("creator");
        var policies = new HashMap<GroupSetting, ChatSettingPolicy>();
        policies.put(SEND_MESSAGES, ChatSettingPolicy.of(node.hasNode("restrict")));
        policies.put(EDIT_GROUP_INFO, ChatSettingPolicy.of(node.hasNode("announce")));
        policies.put(APPROVE_PARTICIPANTS, ChatSettingPolicy.of(node.hasNode("membership_approval_mode")));
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
                .flatMap(Optional::stream)
                .toList();
        return new GroupMetadata(groupId, subject, subjectAuthor, Clock.parseSeconds(subjectTimestampSeconds), Clock.parseSeconds(foundationTimestampSeconds), founder, description, descriptionId, Collections.unmodifiableMap(policies), participants, ephemeral, community, openCommunity);
    }

    private Optional<GroupParticipant> parseGroupParticipant(Node node) {
        if(node.attributes().hasKey("error")) {
            return Optional.empty();
        }

        var id = node.attributes().getRequiredJid("jid");
        var role = GroupRole.of(node.attributes().getString("type", null));
        return Optional.of(new GroupParticipant(id, role));
    }

    public CompletableFuture<Node> sendQuery(Jid to, String method, String category, Node... body) {
        return sendQuery(null, to, method, category, null, body);
    }

    public CompletableFuture<Void> sendReceipt(Jid jid, Jid participant, List<String> messages, String type) {
        if (messages.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var attributes = Attributes.of()
                .put("id", messages.getFirst())
                .put("t", Clock.nowMilliseconds(), () -> Objects.equals(type, "read") || Objects.equals(type, "read-self"))
                .put("to", jid)
                .put("type", type, Objects::nonNull);
        if (Objects.equals(type, "sender") && jid.hasServer(JidServer.WHATSAPP)) {
            attributes.put("recipient", jid);
            attributes.put("to", participant);
        }

        var receipt = Node.of("receipt", attributes.toMap(), toMessagesNode(messages));
        return sendNodeWithNoResponse(receipt);
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
        return sendNodeWithNoResponse(Node.of("ack", attributes));
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

    protected void onDisconnected(DisconnectReason reason) {
        if (reason != DisconnectReason.RECONNECTING) {
            connectedUuids.remove(store.uuid());
            store.phoneNumber()
                    .map(PhoneNumber::number)
                    .ifPresent(connectedPhoneNumbers::remove);
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
            confirmConnection();
        }
        callListenersSync(listener -> {
            listener.onDisconnected(whatsapp, reason);
            listener.onDisconnected(reason);
        });
    }

    protected void onLoggedIn() {
        callListenersAsync(listener -> {
            listener.onLoggedIn(whatsapp);
            listener.onLoggedIn();
        });
    }

    public void callListenersSync(Consumer<Listener> consumer) {
        for(var listener : store.listeners()) {
            invokeListenerSafe(consumer, listener);
        }
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
            var wasOnline = store().online();
            sendNodeWithNoResponse(Node.of("presence", Map.of("name", oldName, "type", "unavailable")));
            sendNodeWithNoResponse(Node.of("presence", Map.of("name", newName, "type", "available")));
            if(!wasOnline) {
                sendNodeWithNoResponse(Node.of("presence", Map.of("name", oldName, "type", "unavailable")));
            }
            onUserNameChanged(newName, oldName);
        }
        var self = store.jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"))
                .toSimpleJid();
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

    public void updateLocale(CountryLocale newLocale, CountryLocale oldLocale) {
        if (!Objects.equals(newLocale, oldLocale)) {
            return;
        }
        if (oldLocale != null) {
            onUserLocaleChanged(newLocale, oldLocale);
        }
        store().setLocale(newLocale);
    }

    private void onUserLocaleChanged(CountryLocale newLocale, CountryLocale oldLocale) {
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
        scheduler.shutdownNow();
    }

    protected <T> T handleFailure(Location location, Throwable throwable) {
        if (state() == SocketState.RESTORE || state() == SocketState.LOGGED_OUT || state() == SocketState.BANNED) {
            return null;
        }
        var result = errorHandler.handleError(whatsapp, location, throwable);
        switch (result) {
            case RESTORE -> disconnect(DisconnectReason.RESTORE);
            case LOG_OUT -> disconnect(DisconnectReason.LOGGED_OUT);
            case DISCONNECT -> disconnect(DisconnectReason.DISCONNECTED);
            case RECONNECT -> disconnect(DisconnectReason.RECONNECTING);
        }
        return null;
    }

    public CompletableFuture<List<Jid>> querySessions(List<Jid> jid) {
        return messageHandler.querySessions(jid, true)
                .thenComposeAsync(values -> messageHandler.queryDevices(jid, false));
    }

    public void parseSessions(Node result) {
        messageHandler.parseSessions(result);
    }

    public CompletableFuture<List<BusinessCategory>> queryBusinessCategories() {
        return sendQuery("get", "fb:thrift_iq", Node.of("request", Map.of("op", "profile_typeahead", "type", "catkit", "v", "1"), Node.of("query", List.of())))
                .thenApplyAsync(this::parseBusinessCategories);
    }

    private List<BusinessCategory> parseBusinessCategories(Node result) {
        return result.findNode("response")
                .flatMap(entry -> entry.findNode("categories"))
                .stream()
                .map(entry -> entry.findNodes("category"))
                .flatMap(Collection::stream)
                .map(BusinessCategory::of)
                .toList();
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

    protected void setState(SocketState state) {
        this.state = state;
    }

    public CompletableFuture<Void> changeAbout(String newAbout) {
        return sendQuery("set", "status", Node.of("status", newAbout.getBytes(StandardCharsets.UTF_8)))
                .thenRun(() -> store().setAbout(newAbout));
    }

    void confirmConnection() {
        if (loginFuture == null || loginFuture.isDone()) {
            return;
        }

        loginFuture.complete(null);
    }

    protected ScheduledFuture<?> scheduleAtFixedInterval(Runnable command, long initialDelay, long period) {
        return scheduler.scheduleAtFixedRate(command, initialDelay, period, TimeUnit.SECONDS);
    }

    protected void sendPing() {
        sendQuery("get", "w:p", Node.of("ping"))
                .thenRunAsync(() -> onSocketEvent(SocketEvent.PING))
                .exceptionallyAsync(throwable -> handleFailure(STREAM, throwable));
    }
}
