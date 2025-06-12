package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.*;
import it.auties.whatsapp.api.ErrorHandler.Location;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.io.BinaryDecoder;
import it.auties.whatsapp.io.BinaryEncoder;
import it.auties.whatsapp.io.BinaryLength;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.ChatMessageInfoBuilder;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import it.auties.whatsapp.model.info.MessageInfo;
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
import it.auties.whatsapp.model.newsletter.NewsletterViewerRole;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.request.CommunityRequests;
import it.auties.whatsapp.model.request.MessageRequest;
import it.auties.whatsapp.model.request.NewsletterRequests;
import it.auties.whatsapp.model.response.CommunityLinkedGroupsResponse;
import it.auties.whatsapp.model.response.NewsletterResponse;
import it.auties.whatsapp.model.response.UserAboutResponse;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.signal.auth.ClientHelloBuilder;
import it.auties.whatsapp.model.signal.auth.HandshakeMessageBuilder;
import it.auties.whatsapp.model.signal.auth.HandshakeMessageSpec;
import it.auties.whatsapp.model.sync.PatchRequest;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.model.sync.PrimaryFeature;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Streams;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static java.util.concurrent.TimeUnit.SECONDS;

@SuppressWarnings("unused")
public class SocketHandler implements SocketSession.Listener {
    private static final Set<UUID> connectedUuids = ConcurrentHashMap.newKeySet();
    private static final Set<Long> connectedPhoneNumbers = ConcurrentHashMap.newKeySet();
    private static final Set<String> connectedAlias = ConcurrentHashMap.newKeySet();
    private static final int PING_TIMEOUT = 20;

    public static boolean isConnected(UUID uuid) {
        return connectedUuids.contains(uuid);
    }

    public static boolean isConnected(long phoneNumber) {
        return connectedPhoneNumbers.contains(phoneNumber);
    }

    public static boolean isConnected(String id) {
        return connectedAlias.contains(id);
    }

    private SocketSession session;
    private final Whatsapp whatsapp;
    private final AuthHandler authHandler;
    private final StreamHandler streamHandler;
    private final MessageHandler messageHandler;
    private final AppStateHandler appStateHandler;
    private final ErrorHandler errorHandler;
    private final AtomicLong requestsCounter;
    private volatile ScheduledExecutorService scheduler;
    private final ConcurrentMap<Jid, SequencedSet<ChatPastParticipant>> pastParticipants;
    private final Semaphore writeSemaphore;
    private final Map<Jid, ChatMetadata> chatMetadataCache;
    private final AtomicBoolean serializable;
    private final AtomicReference<SocketState> state;
    private final Cipher readCipher, writeCipher;
    private final Keys keys;
    private final Store store;
    private boolean readCipherFragmented;
    private Thread shutdownHook;
    public SocketHandler(Whatsapp whatsapp, Store store, Keys keys, ErrorHandler errorHandler, WebVerificationHandler webVerificationHandler) {
        this.whatsapp = whatsapp;
        this.store = store;
        this.keys = keys;
        this.state = new AtomicReference<>(SocketState.DISCONNECTED);
        this.serializable = new AtomicBoolean(true);
        this.authHandler = new AuthHandler(this);
        this.streamHandler = new StreamHandler(this, webVerificationHandler);
        this.messageHandler = new MessageHandler(this);
        this.appStateHandler = new AppStateHandler(this);
        this.errorHandler = Objects.requireNonNullElse(errorHandler, ErrorHandler.toTerminal());
        this.requestsCounter = new AtomicLong();
        this.writeSemaphore = new Semaphore(1, true);
        this.pastParticipants = new ConcurrentHashMap<>();
        this.chatMetadataCache = new ConcurrentHashMap<>();
        try {
            this.readCipher = Cipher.getInstance("AES/GCM/NoPadding");
            this.writeCipher = Cipher.getInstance("AES/GCM/NoPadding");
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Unsupported AES cipher");
        }
    }

    private void onShutdown() {
        if (!serializable.getAcquire()) {
            keys.dispose();
            store.dispose();
        }

        if(scheduler != null) {
            scheduler.shutdownNow();
            this.scheduler = null;
        }

        dispose();
    }

    private void callListenersAsync(Consumer<Listener> consumer) {
        store.listeners()
                .forEach(listener -> Thread.startVirtualThread(() -> invokeListenerSafe(consumer, listener)));
    }

    @Override
    public void onOpen(SocketSession session) {
        this.session = session;
        if(!state.compareAndSet(SocketState.DISCONNECTED, SocketState.HANDSHAKING)) {
            return;
        }

        if (shutdownHook == null) {
            this.shutdownHook = Thread.ofPlatform()
                    .name("CobaltShutdownHandler")
                    .unstarted(this::onShutdown);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        addToKnownConnections();
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
    public void onMessage(ByteBuffer message, boolean last) {
        switch (state.getAcquire()) {
            case DISCONNECTED -> {
                // Ignore message
            }

            case HANDSHAKING -> authHandler.login(message).whenCompleteAsync((result, throwable) -> {
                if (throwable == null) {
                    state.compareAndSet(SocketState.HANDSHAKING, SocketState.CONNECTED);
                }else {
                    handleFailure(LOGIN, throwable);
                }
            });

            case CONNECTED -> {
                var readKey = keys.readKey();
                if (readKey.isEmpty()) {
                    return;
                }

                try {
                    if(!readCipherFragmented) {
                        readCipher.init(
                                Cipher.DECRYPT_MODE,
                                new SecretKeySpec(readKey.get(), "AES"),
                                encodeIv(keys.nextReadCounter())
                        );
                    }

                    if (last) {
                        if (readCipherFragmented) {
                            // If the message was fragmented, it's not possible to do it in place for two reasons:
                            // 1. The n - 1 fragment has already been processed and deallocated
                            // 2. Even if we saved all the fragments and then could(I say could because doFinal doesn't support outputting to an OutputStream and ByteBuffer is sealed)
                            //    decode in place to those buffers, the memory usage would be the same as deallocating those original fragments and decoding to a new buffer
                            var outputSize = readCipher.getOutputSize(message.remaining());
                            var output = ByteBuffer.allocate(outputSize);
                            readCipher.doFinal(message, output);
                            output.flip();
                            decodeNodes(output);
                        } else {
                            // In place decryption
                            var output = message.duplicate();
                            var outputPosition = output.position();
                            var length = readCipher.doFinal(message, output);
                            if(length > 0) {
                                output.limit(outputPosition + length)
                                        .position(outputPosition);
                                decodeNodes(output);
                            }
                        }
                        readCipherFragmented = false;
                    } else {
                        var output = message.duplicate();
                        var outputPosition = output.position();
                        var length = readCipher.update(message, output);
                        if(length > 0) {
                            output.limit(outputPosition + length)
                                    .position(outputPosition);
                            decodeNodes(output);
                        }
                        readCipherFragmented = true;
                    }
                } catch (Throwable throwable) {
                    handleFailure(STREAM, throwable);
                }
            }
        }
    }

    private void decodeNodes(ByteBuffer output) throws IOException {
        try(var stream = Streams.newInputStream(output)) {
            while(stream.available() > 0) {
                var node = BinaryDecoder.decode(Streams.newInputStream(output));
                onNodeReceived(node);
                store.resolvePendingRequest(node);
                streamHandler.digest(node);
            }
        }
    }

    private void onNodeReceived(Node node) {
        callListenersAsync(listener -> {
            listener.onNodeReceived(whatsapp, node);
            listener.onNodeReceived(node);
        });
    }

    @Override
    public void onClose() {
        if (state.getAcquire() != SocketState.DISCONNECTED) {
            disconnect(DisconnectReason.DISCONNECTED);
        }else {
            onDisconnected();
            onShutdown();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if(isConnected()) {
            disconnect(DisconnectReason.RECONNECTING);
        }
    }

    public CompletableFuture<Node> sendNode(Node node) {
        return sendNode(node, null);
    }

    public CompletableFuture<Node> sendNode(Node node, Function<Node, Boolean> filter) {
        if (node.id() == null) {
            node.attributes().put("id", HexFormat.of().formatHex(Bytes.random(6)));
        }

        var request = new SocketRequest(node.id(), filter, node);
        return sendRequest(request, false, true);
    }

    public CompletableFuture<Void> sendNodeWithNoResponse(Node node) {
        var request = new SocketRequest(node.id(), null, node);
        return sendRequest(request, false, false)
                .thenRun(() -> {});
    }

    public CompletableFuture<Void> sendBinaryWithNoResponse(byte[] binary, boolean prologue) {
        var request = new SocketRequest(null, null, binary);
        return sendRequest(request, prologue, false)
                .thenRun(() -> {});
    }

    private CompletableFuture<Node> sendRequest(SocketRequest request, boolean prologue, boolean response) {
        var scheduledRelease = false;
        try {
            writeSemaphore.acquire();
            if (state.getAcquire() != SocketState.CONNECTED) {
                writeSemaphore.release();
                return CompletableFuture.failedFuture(new IllegalStateException("Instance is not connected"));
            }

            var message = getRequestPayload(request, prologue);
            var future = session.sendBinary(message);
            scheduledRelease = true;
            future.whenCompleteAsync((result, error) -> {
                writeSemaphore.release();
                if(request.body() instanceof Node body) {
                    onNodeSent(body);
                }

                if(error != null) {
                    request.future().completeExceptionally(error);
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
            if(!scheduledRelease) {
                writeSemaphore.release();
            }

            return CompletableFuture.failedFuture(throwable);
        }
    }

    private byte[] getRequestPayload(SocketRequest request, boolean prologue) {
        var prologuePayload = prologue ? SocketHandshake.getPrologue(store.clientType()) : null;
        var prologuePayloadLength = prologue ? prologuePayload.length : 0;
        var writeKey = keys.writeKey();
        if (writeKey.isPresent()) {
            if(!(request.body() instanceof Node node)) {
                throw new IllegalArgumentException("Unexpected value: " + request.body());
            }

            try {
                var iv = keys.nextWriteCounter();
                writeCipher.init(
                        Cipher.ENCRYPT_MODE,
                        new SecretKeySpec(writeKey.get(), "AES"),
                        encodeIv(iv)
                );
                var requestLength = BinaryLength.sizeOf(node);
                var encryptedRequestLength = writeCipher.getOutputSize(requestLength);
                var requestPayload = new byte[getRequestPayloadLength(prologuePayloadLength, encryptedRequestLength)];
                var offset = writeRequestHeader(prologuePayload, requestPayload, prologuePayloadLength, encryptedRequestLength);
                BinaryEncoder.encode(node, requestPayload, offset);
                writeCipher.doFinal(requestPayload, offset, requestLength, requestPayload, offset);
                return requestPayload;
            } catch (Throwable throwable) {
                throw new RuntimeException("Cannot encrypt data", throwable);
            }
        }

        return switch (request.body()) {
            case byte[] bytes -> {
                var requestLength = bytes.length;
                var message = new byte[getRequestPayloadLength(prologuePayloadLength, requestLength)];
                var offset = writeRequestHeader(prologuePayload, message, prologuePayloadLength, requestLength);
                System.arraycopy(bytes, 0, message, offset, requestLength);
                yield message;
            }
            case Node node -> {
                var requestLength = BinaryLength.sizeOf(node);
                var message = new byte[getRequestPayloadLength(prologuePayloadLength, requestLength)];
                var offset = writeRequestHeader(prologuePayload, message, prologuePayloadLength, requestLength);
                BinaryEncoder.encode(node, message, offset);
                yield message;
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + request.body());
        };
    }

    private int getRequestPayloadLength(int prologuePayloadLength, int encryptedRequestLength) {
        return prologuePayloadLength
                + Integer.BYTES
                + Short.BYTES
                + encryptedRequestLength;
    }

    private int writeRequestHeader(byte[] prologuePayload, byte[] message, int prologuePayloadLength, int requestLength) {
        if(prologuePayload != null) {
            System.arraycopy(prologuePayload, 0, message, 0, prologuePayloadLength);
        }
        var a = requestLength >> 16;
        message[prologuePayloadLength++] = (byte) (a >> 24);
        message[prologuePayloadLength++] = (byte) (a >> 16);
        message[prologuePayloadLength++] = (byte) (a >> 8);
        message[prologuePayloadLength++] = (byte) a;
        var b = requestLength & 65535;
        message[prologuePayloadLength++] = (byte) (b >> 8);
        message[prologuePayloadLength++] = (byte) b;
        return prologuePayloadLength;
    }

    private GCMParameterSpec encodeIv(long value) {
        var result = new byte[12];
        result[4] = (byte) (value >> 56);
        result[5] = (byte) (value >> 48);
        result[6] = (byte) (value >> 40);
        result[7] = (byte) (value >> 32);
        result[8] = (byte) (value >> 24);
        result[9] = (byte) (value >> 16);
        result[10] = (byte) (value >> 8);
        result[11] = (byte) value;
        return new GCMParameterSpec(128, result);
    }

    public CompletableFuture<Void> connect(DisconnectReason reason) {
        if (state.getAcquire() != SocketState.DISCONNECTED) {
            return CompletableFuture.completedFuture(null);
        }

        this.session = SocketSession.of(store.proxy().orElse(null), store.clientType() == ClientType.WEB);
        return session.connect(this).exceptionallyCompose(throwable -> {
            state.set(SocketState.DISCONNECTED);
            if(reason == DisconnectReason.RECONNECTING) {
                handleFailure(RECONNECT, throwable);
            }
            return CompletableFuture.failedFuture(throwable);
        });
    }

    public CompletableFuture<Void> disconnect(DisconnectReason reason) {
        if(!state.compareAndSet(SocketState.CONNECTED, SocketState.DISCONNECTED)) {
            return CompletableFuture.completedFuture(null);
        }

        if (session != null) {
            return session.disconnect()
                    .thenRun(() -> onDisconnected(reason));
        }else {
            return onDisconnected(reason);
        }
    }

    private CompletableFuture<Void> onDisconnected(DisconnectReason reason) {
        keys.clearReadWriteKey();
        return switch (reason) {
            case DISCONNECTED -> {
                store.resolveAllPendingRequests();
                yield CompletableFuture.completedFuture(null);
            }
            case RECONNECTING -> {
                store.resolveAllPendingRequests();
                yield connect(reason);
            }
            case LOGGED_OUT, BANNED -> {
                store.deleteSession();
                store.resolveAllPendingRequests();
                store.resolveAllPendingRequests();
                serializable.set(false);
                yield CompletableFuture.completedFuture(null);
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

    protected void pullInitialPatches() {
        appStateHandler.pullInitial();
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
                .id(ChatMessageKey.randomId(store.clientType()))
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
        var request = new MessageRequest.Chat(info, null, false, true, null);
        return sendMessage(request);
    }

    public CompletableFuture<Void> sendMessage(MessageRequest request) {
        return messageHandler.encode(request);
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<Void> sendQueryWithNoResponse(String method, String category, Node... body) {
        return sendQueryWithNoResponse(null, JidServer.whatsapp().toJid(), method, category, null, body);
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

        return new SocketRequest(node.id(), filter, node);
    }

    private void onNodeSent(Node node) {
        callListenersAsync(listener -> {
            listener.onNodeSent(whatsapp, node);
            listener.onNodeSent(node);
        });
    }

    public CompletableFuture<Optional<UserAboutResponse>> queryAbout(JidProvider chat) {
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
                Map.of("sid", randomSid(), "mode", "query", "last", "true", "index", "0", "context", "interactive"),
                query,
                list,
                sideList
        );
        return sendQuery("get", "usync", sync)
                .thenApplyAsync(this::parseQueryResult);
    }

    public static String randomSid() {
        return Clock.nowSeconds() + "-" + ThreadLocalRandom.current().nextLong(1_000_000_000, 9_999_999_999L) + "-" + ThreadLocalRandom.current().nextInt(0, 1000);
    }

    private Optional<UserAboutResponse> parseAbout(List<Node> responses) {
        return responses.stream()
                .map(entry -> entry.findChild("status"))
                .flatMap(Optional::stream)
                .findFirst()
                .map(UserAboutResponse::of);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Node... body) {
        return sendQuery(null, JidServer.whatsapp().toJid(), method, category, null, body);
    }

    private List<Node> parseQueryResult(Node result) {
        return result == null ? List.of() : result.listChildren("usync")
                .stream()
                .map(node -> node.findChild("list"))
                .flatMap(Optional::stream)
                .map(node -> node.listChildren("user"))
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
        if (chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            return queryGroupMetadata(chat.toJid())
                    .thenComposeAsync(result -> sendQuery("get", "w:profile:picture", Map.of(result.isCommunity() ? "parent_group_jid" : "target", chat.toJid()), body))
                    .thenApplyAsync(this::parseChatPicture);
        }

        return sendQuery("get", "w:profile:picture", Map.of("target", chat.toJid()), body)
                .thenApplyAsync(this::parseChatPicture);
    }

    public CompletableFuture<Node> sendQuery(String method, String category, Map<String, Object> metadata, Node... body) {
        return sendQuery(null, JidServer.whatsapp().toJid(), method, category, metadata, body);
    }

    private Optional<URI> parseChatPicture(Node result) {
        return result.findChild("picture")
                .flatMap(picture -> picture.attributes().getOptionalString("url"))
                .map(URI::create);
    }

    public CompletableFuture<List<Jid>> queryBlockList() {
        return sendQuery("get", "blocklist", (Node) null)
                .thenApplyAsync(this::parseBlockList);
    }

    private List<Jid> parseBlockList(Node result) {
        return result.findChild("list")
                .stream()
                .flatMap(node -> node.listChildren("item").stream())
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
        return result.findChild("live_updates")
                .stream()
                .map(node -> node.attributes().getOptionalLong("duration"))
                .flatMapToLong(OptionalLong::stream)
                .findFirst();
    }

    @SuppressWarnings("OptionalIsPresent")
    public CompletableFuture<Void> queryNewsletterMessages(JidProvider newsletterJid, int count) {
        return store.findNewsletterByJid(newsletterJid)
                .map(entry -> CompletableFuture.completedFuture(Optional.of(entry)))
                .orElseGet(() -> queryNewsletter(newsletterJid.toJid(), NewsletterViewerRole.GUEST))
                .thenCompose(newsletter -> {
                    if(newsletter.isEmpty()) {
                        return CompletableFuture.failedFuture(new IllegalStateException("No newsletter found for jid: " + newsletterJid));
                    }

                    return sendQuery("get", "newsletter", Node.of("messages", Map.of("count", count, "type", "jid", "jid", newsletterJid)))
                            .thenAcceptAsync(result -> onNewsletterMessages(newsletter.get(), result));
                });
    }

    private void onNewsletterMessages(Newsletter newsletter, Node result) {
        result.findChild("messages")
                .stream()
                .map(messages -> messages.listChildren("message"))
                .flatMap(Collection::stream)
                .forEach(messages -> decodeMessage(messages, newsletter, false));
    }

    public CompletableFuture<ChatMetadata> queryGroupMetadata(JidProvider group) {
        var metadata = chatMetadataCache.get(group.toJid());
        if(metadata != null) {
            return CompletableFuture.completedFuture(metadata);
        }

        var body = Node.of("query", Map.of("request", "interactive"));
        return sendQuery(group.toJid(), "get", "w:g2", body)
                .thenComposeAsync(this::handleGroupMetadata)
                .thenApply(result -> {
                    chatMetadataCache.put(group.toJid(), result);
                    return result;
                });
    }

    public CompletableFuture<ChatMetadata> handleGroupMetadata(Node response) {
        var metadataNode = Optional.of(response)
                .filter(entry -> entry.hasDescription("group"))
                .or(() -> response.findChild("group"))
                .orElseThrow(() -> new NoSuchElementException("Erroneous response: %s".formatted(response)));
        return parseGroupMetadata(metadataNode).thenApplyAsync(metadata -> {
            var chat = store.findChatByJid(metadata.jid())
                    .orElseGet(() -> store().addNewChat(metadata.jid()));
            chat.setName(metadata.subject());
            return metadata;
        });
    }

    private CompletableFuture<ChatMetadata> parseGroupMetadata(Node node) {
        var groupId = node.attributes()
                .getOptionalString("id")
                .map(id -> Jid.of(id, JidServer.groupOrCommunity()))
                .orElseThrow(() -> new NoSuchElementException("Missing group jid"));
        var subject = node.attributes().getString("subject");
        var subjectAuthor = node.attributes()
                .getOptionalJid("s_o")
                .orElse(null);
        var subjectTimestampSeconds = node.attributes()
                .getOptionalLong("s_t")
                .orElse(0L);
        var foundationTimestampSeconds = node.attributes()
                .getOptionalLong("creation")
                .orElse(0L);
        var founder = node.attributes()
                .getOptionalJid("creator")
                .orElse(null);
        var description = node.findChild("description")
                .flatMap(parent -> parent.findChild("body"))
                .flatMap(Node::contentAsString)
                .orElse(null);
        var descriptionId = node.findChild("description")
                .map(Node::attributes)
                .flatMap(attributes -> attributes.getOptionalString("id"))
                .orElse(null);
        var parentCommunityJid = node.findChild("linked_parent")
                .flatMap(entry -> entry.attributes().getOptionalJid("jid"));
        long ephemeral = node.findChild("ephemeral")
                .map(Node::attributes)
                .map(attributes -> attributes.getLong("expiration"))
                .orElse(0L);
        var communityNode = node.findChild("parent")
                .orElse(null);
        var policies = new HashMap<Integer, ChatSettingPolicy>();
        var pastParticipants = Objects.requireNonNullElseGet(this.pastParticipants.get(groupId), LinkedHashSet<ChatPastParticipant>::new);
        if (communityNode == null) {
            policies.put(GroupSetting.EDIT_GROUP_INFO.index(), ChatSettingPolicy.of(node.hasNode("announce")));
            policies.put(GroupSetting.SEND_MESSAGES.index(), ChatSettingPolicy.of(node.hasNode("restrict")));
            var addParticipantsMode = node.findChild("member_add_mode")
                    .flatMap(Node::contentAsString)
                    .orElse(null);
            policies.put(GroupSetting.ADD_PARTICIPANTS.index(), ChatSettingPolicy.of(Objects.equals(addParticipantsMode, "admin_add")));
            var groupJoin = node.findChild("membership_approval_mode")
                    .flatMap(entry -> entry.findChild("group_join"))
                    .map(entry -> entry.attributes().hasValue("state", "on"))
                    .orElse(false);
            policies.put(GroupSetting.APPROVE_PARTICIPANTS.index(), ChatSettingPolicy.of(groupJoin));
            var participants = node.listChildren("participant")
                    .stream()
                    .map(this::parseGroupParticipant)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            var result = new ChatMetadataBuilder()
                    .jid(groupId)
                    .subject(subject)
                    .subjectAuthorJid(subjectAuthor)
                    .subjectTimestampSeconds(subjectTimestampSeconds)
                    .foundationTimestampSeconds(foundationTimestampSeconds)
                    .founderJid(founder)
                    .description(description)
                    .descriptionId(descriptionId)
                    .settings(policies)
                    .participants(participants)
                    .pastParticipants(pastParticipants)
                    .ephemeralExpirationSeconds(ephemeral)
                    .isCommunity(false)
                    .build();
            return CompletableFuture.completedFuture(result);
        }else {
            policies.put(CommunitySetting.MODIFY_GROUPS.index(), ChatSettingPolicy.of(communityNode.hasNode("allow_non_admin_sub_group_creation")));
            var addParticipantsMode = node.findChild("member_add_mode")
                    .flatMap(Node::contentAsString)
                    .orElse(null);
            policies.put(CommunitySetting.ADD_PARTICIPANTS.index(), ChatSettingPolicy.of(Objects.equals(addParticipantsMode, "admin_add")));
            return sendQuery(groupId, "get", "w:g2", Node.of("linked_groups_participants")).thenComposeAsync(participantsNode -> {
                var participants = participantsNode.findChild("linked_groups_participants")
                        .stream()
                        .flatMap(participantsNodeBody -> participantsNodeBody.streamChildren("participant"))
                        .flatMap(participantNode -> participantNode.attributes().getOptionalJid("jid").stream())
                        .map(ChatParticipant::ofCommunity)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                var request = CommunityRequests.linkedGroups(groupId, "INTERACTIVE");
                return sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7353258338095347"), request)).thenApplyAsync(communityResponse -> {
                    var linkedGroups = parseLinkedGroups(communityResponse);
                    return new ChatMetadataBuilder()
                            .jid(groupId)
                            .subject(subject)
                            .subjectAuthorJid(subjectAuthor)
                            .subjectTimestampSeconds(subjectTimestampSeconds)
                            .foundationTimestampSeconds(foundationTimestampSeconds)
                            .founderJid(founder)
                            .description(description)
                            .descriptionId(descriptionId)
                            .settings(policies)
                            .participants(participants)
                            .pastParticipants(pastParticipants)
                            .ephemeralExpirationSeconds(ephemeral)
                            .isCommunity(true)
                            .communityGroups(linkedGroups)
                            .build();
                });
            });
        }
    }

    @SuppressWarnings("OptionalIsPresent")
    private SequencedSet<CommunityLinkedGroup> parseLinkedGroups(Node communityResponse) {
        var result = communityResponse.findChild("result");
        if(result.isEmpty()) {
            return null;
        }

        var content = result.get()
                .contentAsBytes();
        if(content.isEmpty()) {
            return null;
        }

        return CommunityLinkedGroupsResponse.ofJson(content.get())
                .map(CommunityLinkedGroupsResponse::linkedGroups)
                .orElse(null);
    }

    private Optional<ChatParticipant> parseGroupParticipant(Node node) {
        if(node.attributes().hasKey("error")) {
            return Optional.empty();
        }

        var id = node.attributes().getRequiredJid("jid");
        var role = ChatRole.of(node.attributes().getString("type", null));
        return Optional.of(ChatParticipant.ofGroup(id, role));
    }

    public CompletableFuture<Node> sendQuery(Jid to, String method, String category, Node... body) {
        return sendQuery(null, to, method, category, null, body);
    }

    public CompletableFuture<Void> sendRetryReceipt(long nodeTimestamp, Jid chatJid, Jid participantJid, String messageId, int retryCount) {
        var retryAttributes = Attributes.of()
                .put("count", 1)
                .put("id", messageId)
                .put("t", nodeTimestamp)
                .put("v", 1)
                .toMap();
        var retryNode = Node.of("retry", retryAttributes);
        var registrationNode = Node.of("registration", keys.encodedRegistrationId());
        var receiptAttributes = Attributes.of()
                .put("id", messageId)
                .put("type", "retry")
                .put("to", chatJid.withAgent(0))
                .put("participant", participantJid == null ? null : participantJid.withAgent(0), participantJid != null)
                .toMap();
        var receipt = Node.of("receipt", receiptAttributes, retryNode, registrationNode);
        return sendNodeWithNoResponse(receipt);
    }

    public CompletableFuture<Void> sendReceipt(Jid jid, Jid participant, List<String> messages, String type) {
        if (messages.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var attributes = Attributes.of()
                .put("id", messages.getFirst())
                .put("t", Clock.nowMilliseconds(), () -> Objects.equals(type, "read") || Objects.equals(type, "read-self"))
                .put("to", jid.withAgent(0))
                .put("type", type, Objects::nonNull);
        if (Objects.equals(type, "sender") && jid.hasServer(JidServer.whatsapp())) {
            attributes.put("recipient", jid.withAgent(0));
            attributes.put("to", participant.withAgent(0));
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
        var participant = attrs.getNullableString("participant");
        var recipient = attrs.getNullableString("recipient");
        var attributes = Attributes.of()
                .put("id", node.id())
                .put("to", from)
                .put("class", node.description())
                .put("participant", participant != null ? Jid.of(participant).withAgent(0) : null)
                .put("recipient", recipient != null ? Jid.of(recipient).withAgent(0) : null)
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

    protected void onMessageStatus(MessageInfo<?> message) {
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

        var provider = contact.isPresent() ? contact.get() : jid;
        chat.addPresence(jid, status);
        callListenersAsync(listener -> {
            listener.onContactPresence(whatsapp, chat, provider);
            listener.onContactPresence(chat, provider);
        });
    }

    protected void onNewMessage(MessageInfo<?> info) {
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

    protected void onMessageDeleted(MessageInfo<?> message, boolean everyone) {
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

    protected void onDisconnected() {
        connectedUuids.remove(store.uuid());
        store.phoneNumber()
                .map(PhoneNumber::number)
                .ifPresent(connectedPhoneNumbers::remove);
        if (shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        callListenersSync(listener -> {
            listener.onDisconnected(whatsapp, DisconnectReason.DISCONNECTED);
            listener.onDisconnected(DisconnectReason.DISCONNECTED);
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
        callListenersAsync(listener -> store().jid()
                .flatMap(store()::findContactByJid)
                .ifPresent(selfJid -> {
                    listener.onProfilePictureChanged(whatsapp, selfJid);
                    listener.onProfilePictureChanged(selfJid);
                }));
    }

    public void onUserChanged(String newName, String oldName) {
        if (oldName != null && !Objects.equals(newName, oldName)) {
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

    protected CompletableFuture<Void> querySessionsForcefully(Jid jid) {
        return messageHandler.querySessions(List.of(jid), true);
    }

    private void dispose() {
        streamHandler.dispose();
        messageHandler.dispose();
        appStateHandler.dispose();
    }

    protected <T> T handleFailure(Location location, Throwable throwable) {
        var result = errorHandler.handleError(whatsapp, location, throwable);
        switch (result) {
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
        return result.findChild("response")
                .flatMap(entry -> entry.findChild("categories"))
                .stream()
                .map(entry -> entry.listChildren("category"))
                .flatMap(Collection::stream)
                .map(BusinessCategory::of)
                .toList();
    }

    public boolean isConnected() {
        return state.getAcquire() == SocketState.CONNECTED;
    }

    public Keys keys() {
        return this.keys;
    }

    public Store store() {
        return this.store;
    }

    public CompletableFuture<Void> changeAbout(String newAbout) {
        return sendQuery("set", "status", Node.of("status", newAbout.getBytes(StandardCharsets.UTF_8)))
                .thenRun(() -> store().setAbout(newAbout));
    }

    @SuppressWarnings("SameParameterValue")
    protected void scheduleAtFixedInterval(Runnable command, long initialDelay, long period) {
        if(state.getAcquire() == SocketState.CONNECTED) {
            createScheduler();
            scheduler.scheduleAtFixedRate(command, initialDelay, period, SECONDS);
        }
    }

    protected ScheduledFuture<?> scheduleDelayed(Runnable command, long delay) {
        if(state.getAcquire() == SocketState.CONNECTED) {
            createScheduler();
            return scheduler.schedule(command, delay, SECONDS);
        }else {
            return null;
        }
    }

    private void createScheduler() {
        if(scheduler == null || scheduler.isShutdown()) {
            synchronized (this) {
                if(scheduler == null || scheduler.isShutdown()) {
                    this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
                }
            }
        }
    }

    protected void sendPing() {
        var attributes = Attributes.of()
                .put("xmlns", "w:p")
                .put("to", JidServer.whatsapp().toJid())
                .put("type", "get")
                .put("id", HexFormat.of().formatHex(Bytes.random(6)))
                .toMap();
        var node = Node.of("iq", attributes, Node.of("ping"));
        var future = new CompletableFuture<Node>()
                .orTimeout(PING_TIMEOUT, SECONDS);
        var request = new SocketRequest(node.id(), node, future, null);
        sendRequest(request, false, true).exceptionally(throwable -> {
            disconnect(DisconnectReason.RECONNECTING);
            return null;
        });
    }

    public CompletableFuture<Void> updateBusinessCertificate(String newName) {
        return streamHandler.updateBusinessCertificate(newName);
    }

    public ConcurrentMap<Jid, SequencedSet<ChatPastParticipant>> pastParticipants() {
        return pastParticipants;
    }

    public void addPastParticipant(Jid jid, ChatPastParticipant pastParticipant) {
        var pastParticipants = pastParticipants().get(jid);
        if(pastParticipants != null) {
            pastParticipants.add(pastParticipant);
            this.pastParticipants.put(jid, pastParticipants);
        }else {
            var values = new LinkedHashSet<ChatPastParticipant>();
            values.add(pastParticipant);
            this.pastParticipants.put(jid, values);
        }
    }

    public void addPastParticipant(Jid jid, Collection<? extends ChatPastParticipant> pastParticipant) {
        var pastParticipants = pastParticipants().get(jid);
        if(pastParticipants != null) {
            pastParticipants.addAll(pastParticipant);
            this.pastParticipants.put(jid, pastParticipants);
        }else {
            var values = new LinkedHashSet<ChatPastParticipant>(pastParticipant);
            this.pastParticipants.put(jid, values);
        }
    }

    protected void queryNewsletters() {
        streamHandler.queryNewsletters()
                .exceptionallyAsync(throwable -> handleFailure(HISTORY_SYNC, throwable));
    }

    public CompletableFuture<Optional<Newsletter>> queryNewsletter(Jid newsletterJid, NewsletterViewerRole role) {
        var request = NewsletterRequests.queryNewsletter(newsletterJid, "JID", role, true, false, true);
        return sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6620195908089573"), request))
                .thenApplyAsync(this::parseNewsletterQuery);
    }

    @SuppressWarnings("OptionalIsPresent")
    private Optional<Newsletter> parseNewsletterQuery(Node response) {
        var result = response.findChild("result");
        if(result.isEmpty()) {
            return Optional.empty();
        }

        var content = result.get()
                .contentAsBytes();
        if(content.isEmpty()) {
            return Optional.empty();
        }

        return NewsletterResponse.ofJson(content.get())
                .map(NewsletterResponse::newsletter);
    }
}
