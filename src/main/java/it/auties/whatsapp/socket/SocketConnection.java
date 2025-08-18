package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.*;
import it.auties.whatsapp.api.WhatsappErrorHandler.Location;
import it.auties.whatsapp.api.WhatsappVerificationHandler.Web.PairingCode;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.PairingCodeSession;
import it.auties.whatsapp.socket.io.NodeDecoder;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.business.*;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactBuilder;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.mobile.CountryLocale;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterViewerRole;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingEntryBuilder;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;
import it.auties.whatsapp.model.request.CommunityRequests;
import it.auties.whatsapp.model.request.MessageRequest;
import it.auties.whatsapp.model.request.NewsletterRequests;
import it.auties.whatsapp.model.response.CommunityLinkedGroupsResponse;
import it.auties.whatsapp.model.response.NewsletterResponse;
import it.auties.whatsapp.model.response.SubscribedNewslettersResponse;
import it.auties.whatsapp.model.response.UserAboutResponse;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.model.sync.PatchRequest;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.model.sync.PrimaryFeature;
import it.auties.whatsapp.socket.message.MessageComponent;
import it.auties.whatsapp.socket.state.AppStateComponent;
import it.auties.whatsapp.socket.stream.StreamComponent;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Streams;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.*;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class SocketConnection {
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final int DEFAULT_NEWSLETTER_MESSAGES = 100;
    private static final byte[] KEY_BUNDLE_TYPE = new byte[]{5};

    private SocketSession session;
    private final Whatsapp whatsapp;
    private final SocketEncryption socketEncryption;
    private final StreamComponent streamComponent;
    private final MessageComponent messageComponent;
    private final AppStateComponent appStateHandler;
    private final WhatsappVerificationHandler.Web webVerificationHandler;
    private final WhatsappErrorHandler errorHandler;
    private volatile ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, Request> pendingRequests;
    private final ConcurrentMap<String, CompletableFuture<MessageInfo>> pendingMessages;
    private final Map<Jid, GroupOrCommunityMetadata> chatMetadataCache;
    private final AtomicBoolean serializable;
    private final AtomicReference<State> state;
    private final PairingCodeSession pairingCodeSession;
    private final Keys keys;
    private final Store store;
    private Thread shutdownHook;

    public SocketConnection(Whatsapp whatsapp, Store store, Keys keys, WhatsappErrorHandler errorHandler, WhatsappVerificationHandler.Web webVerificationHandler) {
        this.whatsapp = whatsapp;
        this.store = store;
        this.keys = keys;
        this.state = new AtomicReference<>(State.DISCONNECTED);
        this.serializable = new AtomicBoolean(true);
        this.socketEncryption = new SocketEncryption(this);
        this.streamComponent = new StreamComponent(this);
        this.messageComponent = new MessageComponent(this);
        this.appStateHandler = new AppStateComponent(this);
        this.webVerificationHandler = webVerificationHandler;
        this.errorHandler = errorHandler;
        this.chatMetadataCache = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.pendingMessages = new ConcurrentHashMap<>();
        this.pairingCodeSession = webVerificationHandler instanceof PairingCode ? new PairingCodeSession() : null;
    }

    public void connect(WhatsappDisconnectReason reason)  {
        if(!state.compareAndSet(State.DISCONNECTED, State.HANDSHAKING)) {
            return;
        }

        try {
            this.session = SocketSession.of(store.proxy().orElse(null));
            session.connect(this::onMessage);
        } catch (Throwable throwable) {
            if (reason == WhatsappDisconnectReason.RECONNECTING) {
                state.set(State.DISCONNECTED);
                handleFailure(RECONNECT, throwable);
            }else {
                handleFailure(LOGIN, throwable);
            }
            return;
        }

        if (shutdownHook == null) {
            this.shutdownHook = Thread.ofPlatform()
                    .name("CobaltShutdownHandler")
                    .unstarted(this::onShutdown);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        socketEncryption.startHandshake(keys.ephemeralKeyPair().publicKey());
    }

    public void disconnect(WhatsappDisconnectReason reason)  {
        if(state.getAndSet(State.DISCONNECTED) == State.DISCONNECTED) {
            return;
        }

        if (session != null) {
            session.disconnect();
        }

        socketEncryption.reset();
        pendingRequests.forEach((ignored, request) -> request.complete(Node.empty()));
        pendingRequests.clear();
        if (reason == WhatsappDisconnectReason.LOGGED_OUT || reason == WhatsappDisconnectReason.BANNED) {
            store.deleteSession();
            serializable.set(false);
        }
        if(reason != WhatsappDisconnectReason.RECONNECTING) {
            if(shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
                shutdownHook = null;
            }
            onShutdown();
        }
        callListenersSync(listener -> {
            listener.onDisconnected(whatsapp, reason);
            listener.onDisconnected(reason);
        });
        if(reason == WhatsappDisconnectReason.RECONNECTING) {
            connect(reason);
        }
    }

    private void onShutdown() {
        if (!serializable.getAcquire()) {
            keys.dispose();
            store.dispose();
        }

        if (scheduler != null) {
            scheduler.shutdownNow();
            this.scheduler = null;
        }

        dispose();
    }

    public void onMessage(ByteBuffer message) {
        switch (state.getAcquire()) {
            case HANDSHAKING -> handleHandshake(message);
            case CONNECTED -> handleMessage(message);
            case DISCONNECTED -> {}
        }
    }

    private void handleHandshake(ByteBuffer message)  {
        try {
            socketEncryption.finishHandshake(message);
            state.compareAndSet(State.HANDSHAKING, State.CONNECTED);
        } catch (Throwable throwable) {
            handleFailure(LOGIN, throwable);
        }
    }

    private void handleMessage(ByteBuffer message)  {
        try {
            message = socketEncryption.receiveDeciphered(message);
        }catch (Throwable throwable) {
            handleFailure(CRYPTOGRAPHY, throwable);
            return;
        }

        try (var stream = Streams.newInputStream(message)) {
            while (stream.available() > 0) {
                var node = NodeDecoder.decode(stream);
                onNodeReceived(node);
                resolvePendingRequest(node);
                streamComponent.digest(node);
            }
        }catch (Throwable throwable) {
            handleFailure(STREAM, throwable);
        }
    }

    private void onNodeReceived(Node node) {
        callListenersAsync(listener -> {
            listener.onNodeReceived(whatsapp, node);
            listener.onNodeReceived(node);
        });
    }

    public void sendNodeWithNoResponse(Node node) {
        if(socketEncryption.sendCiphered(node)) {
            onNodeSent(node);
        }
    }

    public Node sendNode(Node node) {
        return sendNode(node, null);
    }

    public Node sendNode(Node node, Function<Node, Boolean> filter) {
        if (node.id() == null) {
            node.attributes().put("id", Bytes.randomHex(10));
        }

        if(!socketEncryption.sendCiphered(node)) {
            return Node.empty();
        }

        onNodeSent(node);
        var request = new Request(node, filter);
        pendingRequests.put(node.id(), request);
        try {
            return request.waitForResponse();
        }catch (Throwable ignored) {
            return Node.empty();
        }
    }

    public void sendBinary(byte[] binary) {
        if (state.getAcquire() == State.DISCONNECTED) {
            throw new IllegalStateException("Instance is not connected");
        }

        session.sendBinary(ByteBuffer.wrap(binary));
    }

    public void pushPatch(PatchRequest request) {
        var jid = store.jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"));
        appStateHandler.push(jid, List.of(request));
    }

    public void pullPatch(PatchType... patchTypes) {
        appStateHandler.pull(patchTypes);
    }

    public void pullInitialPatches() {
        appStateHandler.pullInitial();
    }

    public void decodeMessage(Node node, JidProvider chatOverride, boolean notify) {
        messageComponent.decode(node, chatOverride, notify);
    }

    public void sendMessage(MessageRequest request) {
        messageComponent.encode(request);
    }

    @SuppressWarnings("UnusedReturnValue")
    public void sendQueryWithNoResponse(String method, String category, Node... body) {
        sendQueryWithNoResponse(null, JidServer.user().toJid(), method, category, null, body);
    }

    public void sendQueryWithNoResponse(String id, Jid to, String method, String category, Map<String, Object> metadata, Node... body) {
        var attributes = Attributes.ofNullable(metadata)
                .put("id", id, Objects::nonNull)
                .put("type", method)
                .put("to", to)
                .put("xmlns", category, Objects::nonNull)
                .toMap();
        sendNodeWithNoResponse(Node.of("iq", attributes, body));
    }

    private void callListenersAsync(Consumer<WhatsappListener> consumer) {
        for (var listener : store.listeners()) {
            Thread.startVirtualThread(() -> invokeListenerSafe(consumer, listener));
        }
    }

    private void onNodeSent(Node node) {
        callListenersAsync(listener -> {
            listener.onNodeSent(whatsapp, node);
            listener.onNodeSent(node);
        });
    }

    public Optional<UserAboutResponse> queryAbout(JidProvider chat) {
        var query = Node.of("status");
        var body = Node.of("user", Map.of("jid", chat.toJid()));
        var result = sendInteractiveQuery(List.of(query), List.of(body), List.of());
        if(result == null) {
            return Optional.empty();
        }
        return result.stream()
                .map(entry -> entry.findChild("status"))
                .flatMap(Optional::stream)
                .findFirst()
                .map(UserAboutResponse::of);
    }

    public List<Node> sendInteractiveQuery(Collection<Node> queries, Collection<Node> listData, Collection<Node> sideListData) {
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
        var result = sendQuery("get", "usync", sync);
        if(result == null) {
            return List.of();
        }
        return result.listChildren("usync")
                .stream()
                .map(node -> node.findChild("list"))
                .flatMap(Optional::stream)
                .map(node -> node.listChildren("user"))
                .flatMap(Collection::stream)
                .toList();
    }

    public static String randomSid() {
        return Clock.nowSeconds() + "-" + ThreadLocalRandom.current().nextLong(1_000_000_000, 9_999_999_999L) + "-" + ThreadLocalRandom.current().nextInt(0, 1000);
    }

    public Node sendQuery(String method, String category, Node... body) {
        return sendQuery(null, JidServer.user().toJid(), method, category, null, body);
    }

    public Node sendQuery(String id, Jid to, String method, String category, Map<String, Object> metadata, Node... body) {
        var attributes = Attributes.ofNullable(metadata)
                .put("xmlns", category, Objects::nonNull)
                .put("id", id, Objects::nonNull)
                .put("to", to)
                .put("type", method)
                .toMap();
        return sendNode(Node.of("iq", attributes, body));
    }

    public Optional<URI> queryPicture(JidProvider chat) {
        var body = Node.of("picture", Map.of("query", "url", "type", "image"));
        var community = chat.toJid().hasServer(JidServer.groupOrCommunity())
                && queryGroupOrCommunityMetadata(chat.toJid()).isCommunity();
        return sendQuery("get", "w:profile:picture", Map.of(community ? "parent_group_jid" : "target", chat.toJid()), body)
                .findChild("picture")
                .flatMap(picture -> picture.attributes().getOptionalString("url"))
                .map(URI::create);
    }

    public Node sendQuery(String method, String category, Map<String, Object> metadata, Node... body) {
        return sendQuery(null, JidServer.user().toJid(), method, category, metadata, body);
    }

    public List<Jid> queryBlockList() {
        var result = sendQuery("get", "blocklist", (Node) null);
        return result.findChild("list")
                .stream()
                .flatMap(node -> node.listChildren("item").stream())
                .map(item -> item.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    public void subscribeToPresence(JidProvider jid) {
        var node = Node.of("presence", Map.of("to", jid.toJid(), "type", "subscribe"));
        sendNodeWithNoResponse(node);
    }

    public OptionalLong subscribeToNewsletterReactions(JidProvider channel) {
        var result = sendQuery(channel.toJid(), "set", "newsletter", Node.of("live_updates"));
        return result.findChild("live_updates")
                .stream()
                .map(node -> node.attributes().getOptionalLong("duration"))
                .flatMapToLong(OptionalLong::stream)
                .findFirst();
    }

    public void queryNewsletterMessages(JidProvider newsletterJid, int count) {
        var newsletter = store.findNewsletterByJid(newsletterJid)
                .or(() -> queryNewsletter(newsletterJid.toJid(), NewsletterViewerRole.GUEST))
                .orElseThrow(() -> new NoSuchElementException("No newsletter found for jid: " + newsletterJid));
        var result = sendQuery("get", "newsletter", Node.of("messages", Map.of("count", count, "type", "jid", "jid", newsletterJid)));
        onNewsletterMessages(newsletter, result);
    }

    private void onNewsletterMessages(Newsletter newsletter, Node result) {
        result.findChild("messages")
                .stream()
                .map(messages -> messages.listChildren("message"))
                .flatMap(Collection::stream)
                .forEach(messages -> decodeMessage(messages, newsletter, false));
    }

    public Optional<GroupOrCommunityMetadata> getChatMetadata(JidProvider chat) {
        return Optional.ofNullable(chatMetadataCache.get(chat.toJid()));
    }

    public GroupOrCommunityMetadata queryGroupOrCommunityMetadata(JidProvider chat) {
        var metadata = chatMetadataCache.get(chat.toJid());
        if (metadata != null) {
            return metadata;
        }

        var body = Node.of("query", Map.of("request", "interactive"));
        var response = sendQuery(chat.toJid(), "get", "w:g2", body);
        var result = handleGroupMetadata(response);
        chatMetadataCache.put(chat.toJid(), result);
        return result;
    }

    public void queryNewsletters() {
        try {
            var request = NewsletterRequests.subscribedNewsletters();
            var result = sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6388546374527196"), request));
            if (!store().webHistorySetting().hasNewsletters()) {
                return;
            }

            var newslettersPayload = result.findChild("result")
                    .flatMap(Node::contentAsString);
            if (newslettersPayload.isEmpty()) {
                return;
            }

            SubscribedNewslettersResponse.ofJson(newslettersPayload.get()).ifPresent(response -> {
                var noMessages = store().webHistorySetting().isZero();
                var data = response.newsletters();
                for (var newsletter : data) {
                    store().addNewsletter(newsletter);
                    if (!noMessages) {
                        try {
                            queryNewsletterMessages(newsletter, DEFAULT_NEWSLETTER_MESSAGES);
                        } catch (Throwable throwable) {
                            handleFailure(MESSAGE, throwable);
                        }
                    }
                }

                onNewsletters();
            });
        }catch (Throwable throwable) {
            handleFailure(HISTORY_SYNC, throwable);
        }
    }

    public void queryGroups() {
        try {
            var result = sendQuery(JidServer.groupOrCommunity().toJid(), "get", "w:g2", Node.of("participating", Node.of("participants"), Node.of("description")));
            var groups = result.findChild("groups");
            if (groups.isEmpty()) {
                return;
            }

            groups.get()
                    .listChildren("group")
                    .forEach(this::handleGroupMetadata);
        }catch (Throwable throwable) {
            handleFailure(HISTORY_SYNC, throwable);
        }
    }

    public GroupOrCommunityMetadata handleGroupMetadata(Node response) {
        var metadataNode = Optional.of(response)
                .filter(entry -> entry.hasDescription("group"))
                .or(() -> response.findChild("group"))
                .orElseThrow(() -> new NoSuchElementException("Erroneous response: %s".formatted(response)));
        var metadata = parseGroupMetadata(metadataNode);
        var chat = store.findChatByJid(metadata.jid())
                .orElseGet(() -> store().addNewChat(metadata.jid()));
        chat.setName(metadata.subject());
        return metadata;
    }

    private GroupOrCommunityMetadata parseGroupMetadata(Node node) {
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
        long ephemeral = node.findChild("ephemeral")
                .map(Node::attributes)
                .map(attributes -> attributes.getLong("expiration"))
                .orElse(0L);
        var communityNode = node.findChild("parent")
                .orElse(null);
        var policies = new HashMap<Integer, ChatSettingPolicy>();
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
            return new GroupOrCommunityMetadataBuilder()
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
                    .ephemeralExpirationSeconds(ephemeral)
                    .isCommunity(false)
                    .build();
        } else {
            policies.put(CommunitySetting.MODIFY_GROUPS.index(), ChatSettingPolicy.of(communityNode.hasNode("allow_non_admin_sub_group_creation")));
            var addParticipantsMode = node.findChild("member_add_mode")
                    .flatMap(Node::contentAsString)
                    .orElse(null);
            policies.put(CommunitySetting.ADD_PARTICIPANTS.index(), ChatSettingPolicy.of(Objects.equals(addParticipantsMode, "admin_add")));
            var participants = sendQuery(groupId, "get", "w:g2", Node.of("linked_groups_participants"))
                    .findChild("linked_groups_participants")
                    .stream()
                    .flatMap(participantsNodeBody -> participantsNodeBody.streamChildren("participant"))
                    .flatMap(participantNode -> participantNode.attributes().getOptionalJid("jid").stream())
                    .map(ChatParticipant::ofCommunity)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            var request = CommunityRequests.linkedGroups(groupId, "INTERACTIVE");
            var communityResponse = sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7353258338095347"), request));
            var linkedGroups = parseLinkedGroups(communityResponse);
            return new GroupOrCommunityMetadataBuilder()
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
                    .ephemeralExpirationSeconds(ephemeral)
                    .isCommunity(true)
                    .communityGroups(linkedGroups)
                    .build();
        }
    }

    @SuppressWarnings("OptionalIsPresent")
    private SequencedSet<CommunityLinkedGroup> parseLinkedGroups(Node communityResponse) {
        var result = communityResponse.findChild("result");
        if (result.isEmpty()) {
            return null;
        }

        var content = result.get()
                .contentAsBytes();
        if (content.isEmpty()) {
            return null;
        }

        return CommunityLinkedGroupsResponse.ofJson(content.get())
                .map(CommunityLinkedGroupsResponse::linkedGroups)
                .orElse(null);
    }

    private Optional<ChatParticipant> parseGroupParticipant(Node node) {
        if (node.attributes().hasKey("error")) {
            return Optional.empty();
        }

        var id = node.attributes().getRequiredJid("jid");
        var role = ChatRole.of(node.attributes().getString("type", null));
        return Optional.of(ChatParticipant.ofGroup(id, role));
    }

    public Node sendQuery(Jid to, String method, String category, Node... body) {
        return sendQuery(null, to, method, category, null, body);
    }

    public void sendReceipt(Jid jid, Jid participant, List<String> messages, String type) {
        if (messages.isEmpty()) {
            return;
        }

        if(jid.hasServer(JidServer.bot())
                || (participant != null && participant.hasServer(JidServer.bot()))) {
            // TODO: Implement BOT
            return;
        }

        var attributes = Attributes.of()
                .put("id", messages.getFirst())
                .put("t", Clock.nowMilliseconds(), () -> Objects.equals(type, "read") || Objects.equals(type, "read-self"))
                .put("to", jid.withAgent(0))
                .put("type", type, Objects::nonNull);
        if (Objects.equals(type, "sender") && jid.hasServer(JidServer.user())) {
            Objects.requireNonNull(participant);
            attributes.put("recipient", jid.withAgent(0));
            attributes.put("to", participant.withAgent(0));
        }

        var receipt = Node.of("receipt", attributes.toMap(), toMessagesNode(messages));
        sendNodeWithNoResponse(receipt);
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

    public void sendMessageAck(Jid from, Node node) {
        var attrs = node.attributes();
        var type = attrs.getOptionalString("type")
                .filter(entry -> !Objects.equals(entry, "message"))
                .orElse(null);
        var participant = attrs.getOptionalJid("participant")
                .orElse(null);
        var recipient = attrs.getOptionalJid("recipient")
                .orElse(null);
        if(from.hasServer(JidServer.bot())
                || (participant != null && participant.hasServer(JidServer.bot()))
                || (recipient != null && recipient.hasServer(JidServer.bot()))) {
            // TODO: Implement BOT
            return;
        }

        var attributes = Attributes.of()
                .put("id", node.id())
                .put("to", from.withAgent(0))
                .put("class", node.description())
                .put("participant", participant != null ? participant.withAgent(0) : null, Objects::nonNull)
                .put("recipient", recipient != null ? recipient.withAgent(0) : null, Objects::nonNull)
                .put("type", type, Objects::nonNull)
                .toMap();
        sendNodeWithNoResponse(Node.of("ack", attributes));
    }

    public void onRegistrationCode(long code) {
        callListenersAsync(listener -> {
            listener.onRegistrationCode(whatsapp, code);
            listener.onRegistrationCode(code);
        });
    }

    public void onMessageStatus(MessageInfo message) {
        callListenersAsync(listener -> {
            listener.onMessageStatus(whatsapp, message);
            listener.onMessageStatus(message);
        });
    }

    public void onUpdateChatPresence(ContactStatus status, Jid jid, Chat chat) {
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

    public void onNewMessage(MessageInfo info) {
        callListenersAsync(listener -> {
            listener.onNewMessage(whatsapp, info);
            listener.onNewMessage(info);
        });
    }

    public void onNewStatus(ChatMessageInfo info) {
        callListenersAsync(listener -> {
            listener.onNewStatus(whatsapp, info);
            listener.onNewStatus(info);
        });
    }

    public void onChatRecentMessages(Chat chat, boolean last) {
        callListenersAsync(listener -> {
            listener.onWebHistorySyncMessages(whatsapp, chat, last);
            listener.onWebHistorySyncMessages(chat, last);
        });
    }

    public void onFeatures(PrimaryFeature features) {
        callListenersAsync(listener -> {
            listener.onWebAppPrimaryFeatures(whatsapp, features.flags());
            listener.onWebAppPrimaryFeatures(features.flags());
        });
    }

    public void onSetting(Setting setting) {
        callListenersAsync(listener -> {
            listener.onWebAppStateSetting(whatsapp, setting);
            listener.onWebAppStateSetting(setting);
        });
    }

    public void onMessageDeleted(MessageInfo message, boolean everyone) {
        callListenersAsync(listener -> {
            listener.onMessageDeleted(whatsapp, message, everyone);
            listener.onMessageDeleted(message, everyone);
        });
    }

    public void onAction(Action action, MessageIndexInfo indexInfo) {
        callListenersAsync(listener -> {
            listener.onWebAppStateAction(whatsapp, action, indexInfo);
            listener.onWebAppStateAction(action, indexInfo);
        });
    }

    public void onLoggedIn() {
        callListenersAsync(listener -> {
            listener.onLoggedIn(whatsapp);
            listener.onLoggedIn();
        });
    }

    public void callListenersSync(Consumer<WhatsappListener> consumer)  {
        for (var listener : store.listeners()) {
            invokeListenerSafe(consumer, listener);
        }
    }

    private void invokeListenerSafe(Consumer<WhatsappListener> consumer, WhatsappListener listener)  {
        try {
            consumer.accept(listener);
        } catch (Throwable throwable) {
            handleFailure(UNKNOWN, throwable);
        }
    }

    public void onChats() {
        callListenersAsync(listener -> {
            listener.onChats(whatsapp, store().chats());
            listener.onChats(store().chats());
        });
    }

    public void onNewsletters() {
        callListenersAsync(listener -> {
            listener.onNewsletters(whatsapp, store().newsletters());
            listener.onNewsletters(store().newsletters());
        });
    }

    public void onStatus() {
        callListenersAsync(listener -> {
            listener.onStatus(whatsapp, store().status());
            listener.onStatus(store().status());
        });
    }

    public void onContacts() {
        callListenersAsync(listener -> {
            listener.onContacts(whatsapp, store().contacts());
            listener.onContacts(store().contacts());
        });
    }

    public void onHistorySyncProgress(Integer progress, boolean recent) {
        callListenersAsync(listener -> {
            listener.onWebHistorySyncProgress(whatsapp, progress, recent);
            listener.onWebHistorySyncProgress(progress, recent);
        });
    }

    public void onReply(MessageInfo info) {
        var quoted = info.quotedMessage()
                .orElse(null);
        if (quoted == null) {
            return;
        }
        var pendingMessageFuture = pendingMessages.remove(quoted.id());
        if(pendingMessageFuture != null) {
            pendingMessageFuture.complete(quoted);
        }
        callListenersAsync(listener -> {
            listener.onMessageReply(whatsapp, info, quoted);
            listener.onMessageReply(info, quoted);
        });
    }

    public void onContactPictureChanged(Contact fromContact) {
        callListenersAsync(listener -> {
            listener.onProfilePictureChanged(whatsapp, fromContact);
            listener.onProfilePictureChanged(fromContact);
        });
    }

    public void onUserChanged(String newName, String oldName) {
        if (oldName != null && !Objects.equals(newName, oldName)) {
            onUserNameChanged(newName, oldName);
        }

        var self = store.jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"))
                .withoutData();
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

    public void onContactBlocked(Contact contact) {
        callListenersAsync(listener -> {
            listener.onContactBlocked(whatsapp, contact);
            listener.onContactBlocked(contact);
        });
    }

    public void onNewContact(Contact contact) {
        callListenersAsync(listener -> {
            listener.onNewContact(whatsapp, contact);
            listener.onNewContact(contact);
        });
    }

    public void onDevices(LinkedHashMap<Jid, Integer> devices) {
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

    public void querySessionsForcefully(Jid jid) {
        messageComponent.querySessions(List.of(jid), true);
    }

    private void dispose() {
        streamComponent.dispose();
        messageComponent.dispose();
        appStateHandler.dispose();
    }

    public void handleFailure(Location location, Throwable throwable)  {
        var result = errorHandler.handleError(whatsapp, location, throwable);
        switch (result) {
            case LOG_OUT -> disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            case DISCONNECT -> disconnect(WhatsappDisconnectReason.DISCONNECTED);
            case RECONNECT -> disconnect(WhatsappDisconnectReason.RECONNECTING);
        }
    }

    public void querySessions(List<Jid> jid) {
        messageComponent.querySessions(jid, true);
        messageComponent.queryDevices(jid, false);
    }

    public List<BusinessCategory> queryBusinessCategories() {
        var result = sendQuery("get", "fb:thrift_iq", Node.of("request", Map.of("op", "profile_typeahead", "type", "catkit", "v", "1"), Node.of("query", List.of())));
        return result.findChild("response")
                .flatMap(entry -> entry.findChild("categories"))
                .stream()
                .map(entry -> entry.listChildren("category"))
                .flatMap(Collection::stream)
                .map(BusinessCategory::of)
                .toList();
    }

    public boolean isConnected() {
        return state.getAcquire() != State.DISCONNECTED;
    }

    public Keys keys() {
        return this.keys;
    }

    public Store store() {
        return this.store;
    }

    public void changeAbout(String newAbout) {
        sendQuery("set", "status", Node.of("status", newAbout.getBytes(StandardCharsets.UTF_8)));
        store.setAbout(newAbout);
    }

    @SuppressWarnings("SameParameterValue")
    public void scheduleAtFixedInterval(Runnable command, long initialDelay, long period) {
        if (state.getAcquire() == State.CONNECTED) {
            createScheduler();
            scheduler.scheduleAtFixedRate(command, initialDelay, period, SECONDS);
        }
    }

    public ScheduledFuture<?> scheduleDelayed(Runnable command, long delay) {
        if (state.getAcquire() == State.CONNECTED) {
            createScheduler();
            return scheduler.schedule(command, delay, SECONDS);
        } else {
            return null;
        }
    }

    private void createScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            synchronized (this) {
                if (scheduler == null || scheduler.isShutdown()) {
                    this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
                }
            }
        }
    }

    public Node sendPing()  {
        try {
            var attributes = Attributes.of()
                    .put("xmlns", "w:p")
                    .put("to", JidServer.user().toJid())
                    .put("type", "get")
                    .put("id", HexFormat.of().formatHex(Bytes.random(10)))
                    .toMap();
            var node = Node.of("iq", attributes, Node.of("ping"));
            return sendNode(node);
        }catch (Throwable throwable) {
            return Node.empty();
        }
    }

    public void updateBusinessCertificate(String newName) {
        var details = new BusinessVerifiedNameDetailsBuilder()
                .name(Objects.requireNonNullElse(newName, store.name()))
                .issuer("smb:wa")
                .serial(Math.abs(ThreadLocalRandom.current().nextLong()))
                .build();
        var encodedDetails = BusinessVerifiedNameDetailsSpec.encode(details);
        var certificate = new BusinessVerifiedNameCertificateBuilder()
                .encodedDetails(encodedDetails)
                .signature(Curve25519.sign(keys().identityKeyPair().privateKey(), encodedDetails))
                .build();
        var result = sendQuery("set", "w:biz", Node.of("verified_name", Map.of("v", 2), BusinessVerifiedNameCertificateSpec.encode(certificate)));
        var verifiedName = result.findChild("verified_name")
                .map(node -> node.attributes().getString("id"))
                .orElse("");
        store.setVerifiedName(verifiedName);
    }

    public Optional<Newsletter> queryNewsletter(Jid newsletterJid, NewsletterViewerRole role) {
        var request = NewsletterRequests.queryNewsletter(newsletterJid, "JID", role, true, false, true);
        var response = sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6620195908089573"), request));
        if(response == Node.empty()) {
            return Optional.empty();
        }

        var result = response.findChild("result");
        return result.flatMap(Node::contentAsBytes)
                .flatMap(NewsletterResponse::ofJson)
                .map(NewsletterResponse::newsletter);
    }

    public void resolvePendingRequest(Node node) {
        var id = node.id();
        if(id == null) {
            return;
        }

        var request = pendingRequests.get(id);
        if (request == null) {
            return;
        }

        var completed = request.complete(node);
        if (!completed) {
            return;
        }

        pendingRequests.remove(id);
    }

    public MessageInfo waitForMessageReply(String id) {
        var future = new CompletableFuture<MessageInfo>();
        pendingMessages.put(id, future);
        return future.join();
    }

    public Node createCall(JidProvider jid) {
        return messageComponent.createCall(jid);
    }

    public void serializeAsync() {
        Thread.startVirtualThread(store::serialize);
        Thread.startVirtualThread(keys::serialize);
    }

    public void addMe(Jid companion) {
        var contact = new ContactBuilder()
                .jid(companion)
                .chosenName(store.name())
                .lastKnownPresence(ContactStatus.AVAILABLE)
                .lastSeenSeconds(Clock.nowSeconds())
                .blocked(false)
                .build();
        store.addContact(contact);
    }

    public void sendPreKeys(int size) {
        var startId = keys.lastPreKeyId() + 1;
        var preKeys = IntStream.range(startId, startId + size)
                .mapToObj(SignalPreKeyPair::random)
                .peek(keys::addPreKey)
                .map(SignalPreKeyPair::toNode)
                .toList();
        sendQuery(
                "set",
                "encrypt",
                Node.of("registration", keys.encodedRegistrationId()),
                Node.of("type", KEY_BUNDLE_TYPE),
                Node.of("identity", keys.identityKeyPair().publicKey()),
                Node.of("list", preKeys),
                keys.signedKeyPair().toNode()
        );
    }

    public void addPrivacySetting(Node node, boolean update) {
        var privacySettingName = node.attributes().getString("name");
        var privacyType = PrivacySettingType.of(privacySettingName);
        if(privacyType.isEmpty()) {
            return;
        }

        var privacyValueName = node.attributes().getString("value");
        var privacyValue = PrivacySettingValue.of(privacyValueName);
        if(privacyValue.isEmpty()) {
            return;
        }

        if (!update) {
            var response = queryPrivacyExcludedContacts(privacyType.get(), privacyValue.get());
            var newEntry = new PrivacySettingEntryBuilder()
                    .type(privacyType.get())
                    .value(privacyValue.get())
                    .excluded(response)
                    .build();
            store.addPrivacySetting(privacyType.get(), newEntry);
        }else {
            var oldEntry = store.findPrivacySetting(privacyType.get());
            var newValues = getUpdatedBlockedList(node, oldEntry, privacyValue.get());
            var newEntry = new PrivacySettingEntryBuilder()
                    .type(privacyType.get())
                    .value(privacyValue.get())
                    .excluded(newValues)
                    .build();
            store.addPrivacySetting(privacyType.get(), newEntry);
            onPrivacySettingChanged(oldEntry, newEntry);
        }
    }

    private List<Jid> queryPrivacyExcludedContacts(PrivacySettingType type, PrivacySettingValue value) {
        if (value != PrivacySettingValue.CONTACTS_EXCEPT) {
            return List.of();
        }

        var result = sendQuery("get", "privacy", Node.of("privacy", Node.of("list", Map.of("name", type.data(), "value", value.data()))));
        return result.findChild("privacy")
                .flatMap(node -> node.findChild("list"))
                .map(node -> node.listChildren("user"))
                .stream()
                .flatMap(Collection::stream)
                .map(user -> user.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    private List<Jid> getUpdatedBlockedList(Node node, PrivacySettingEntry privacyEntry, PrivacySettingValue privacyValue) {
        if (privacyValue != PrivacySettingValue.CONTACTS_EXCEPT) {
            return List.of();
        }

        var newValues = new ArrayList<>(privacyEntry.excluded());
        for (var entry : node.listChildren("user")) {
            var jid = entry.attributes()
                    .getRequiredJid("jid");
            if (entry.attributes().hasValue("action", "add")) {
                newValues.add(jid);
                continue;
            }

            newValues.remove(jid);
        }
        return newValues;
    }

    public void updateUserAbout(boolean update) {
        var user = store.jid()
                .orElse(null);
        if(user == null) {
            return;
        }
        
        var response = queryAbout(user.withoutData())
                .orElse(null);
        if(response == null) {
            return;
        }

        var oldAbout = store.about()
                .orElse(null);
        var newAbout = response.about()
                .orElse(null);
        store.setAbout(newAbout);
        if (update) {
            callListenersAsync(listener -> {
                listener.onAboutChanged(whatsapp, oldAbout, newAbout);
                listener.onAboutChanged(oldAbout, newAbout);
            });
        }
    }

    public void updateUserPicture(boolean update) {
        var user = store.jid()
                .orElse(null);
        if(user == null) {
            return;
        }
        
        var result = queryPicture(user.withoutData());
        store.setProfilePicture(result.orElse(null));
        if (update) {
            callListenersAsync(listener -> {
                listener.onProfilePictureChanged(whatsapp, user.withoutData());
                listener.onProfilePictureChanged(user.withoutData());
            });
        }
    }

    public byte[] encryptPairingKey() {
        return pairingCodeSession.encrypt(keys.companionKeyPair().publicKey());
    }

    public byte[] decryptPairingKey(byte[] primaryEphemeralPublicKeyWrapped) {
        return pairingCodeSession.decrypt(primaryEphemeralPublicKeyWrapped);
    }

    public WhatsappVerificationHandler.Web webVerificationHandler() {
        return webVerificationHandler;
    }

    public void handle(WhatsappVerificationHandler.Web.PairingCode webHandler) {
        pairingCodeSession.accept(webHandler);
    }

    public void onPastParticipants(Jid chatJid, List<ChatPastParticipant> chatPastParticipants) {
        callListenersAsync(listener -> {
            listener.onWebHistorySyncPastParticipants(whatsapp, chatJid, chatPastParticipants);
            listener.onWebHistorySyncPastParticipants(chatJid, chatPastParticipants);
        });
    }

    private enum State {
        DISCONNECTED,
        HANDSHAKING,
        CONNECTED
    }

    private static final class Request {
        private final Node body;
        private final Function<Node, Boolean> filter;
        private volatile Node response;

        private Request(Node body, Function<Node, Boolean> filter) {
            this.body = body;
            this.filter = filter;
        }

        private boolean complete(Node response) {
            Objects.requireNonNull(response, "Response cannot be null");
            var acceptable = response == Node.empty()
                    || filter == null
                    || filter.apply(response);
            if (acceptable) {
                synchronized (this) {
                    this.response = response;
                    notifyAll();
                }
            }
            return acceptable;
        }

        private Node waitForResponse() {
            if (response == null) {
                synchronized (this) {
                    if (response == null) {
                        try {
                            wait(SocketConnection.TIMEOUT.toMillis());
                        }catch (InterruptedException exception) {
                            throw new RuntimeException("Cannot wait for response", exception);
                        }
                    }
                    if(response == null) {
                        throw new RuntimeException("The timeout of " + SocketConnection.TIMEOUT + " has expired for " + body);
                    }
                }
            }
            return response;
        }
    }
}
