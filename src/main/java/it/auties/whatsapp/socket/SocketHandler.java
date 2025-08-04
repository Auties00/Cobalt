package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.*;
import it.auties.whatsapp.api.WhatsappErrorHandler.Location;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.io.BinaryNodeDecoder;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.contact.Contact;
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
import it.auties.whatsapp.model.request.CommunityRequests;
import it.auties.whatsapp.model.request.MessageRequest;
import it.auties.whatsapp.model.request.NewsletterRequests;
import it.auties.whatsapp.model.response.CommunityLinkedGroupsResponse;
import it.auties.whatsapp.model.response.NewsletterResponse;
import it.auties.whatsapp.model.response.UserAboutResponse;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.sync.PatchRequest;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.model.sync.PrimaryFeature;
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

import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.*;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class SocketHandler {
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private SocketSession session;
    private final Whatsapp whatsapp;
    private final EncryptionHandler encryptionHandler;
    private final StreamHandler streamHandler;
    private final MessageHandler messageHandler;
    private final AppStateHandler appStateHandler;
    private final WhatsappErrorHandler errorHandler;
    private volatile ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, Request> requests;
    private final ConcurrentMap<Jid, SequencedSet<ChatPastParticipant>> pastParticipants;
    private final Map<Jid, ChatMetadata> chatMetadataCache;
    private final AtomicBoolean serializable;
    private final AtomicReference<State> state;
    private final Keys keys;
    private final Store store;
    private Thread shutdownHook;

    public SocketHandler(Whatsapp whatsapp, Store store, Keys keys, WhatsappErrorHandler errorHandler, WhatsappVerificationHandler.Web webVerificationHandler) {
        this.whatsapp = whatsapp;
        this.store = store;
        this.keys = keys;
        this.state = new AtomicReference<>(State.DISCONNECTED);
        this.serializable = new AtomicBoolean(true);
        this.encryptionHandler = new EncryptionHandler(this);
        this.streamHandler = new StreamHandler(this, webVerificationHandler);
        this.messageHandler = new MessageHandler(this);
        this.appStateHandler = new AppStateHandler(this);
        this.errorHandler = Objects.requireNonNullElse(errorHandler, WhatsappErrorHandler.toTerminal());
        this.pastParticipants = new ConcurrentHashMap<>();
        this.chatMetadataCache = new ConcurrentHashMap<>();
        this.requests = new ConcurrentHashMap<>();
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

    private void callListenersAsync(Consumer<WhatsappListener> consumer) {
        for (var listener : store.listeners()) {
            Thread.startVirtualThread(() -> invokeListenerSafe(consumer, listener));
        }
    }

    public void onMessage(ByteBuffer message) {
        switch (state.getAcquire()) {
            case HANDSHAKING -> handleHandshake(message);
            case CONNECTED -> handleMessage(message);
            case DISCONNECTED -> {}
        }
    }

    private void handleMessage(ByteBuffer message)  {
        try {
            message = encryptionHandler.receiveDeciphered(message);
            if (message == null) {
                return;
            }
        }catch (Throwable throwable) {
            handleFailure(CRYPTOGRAPHY, throwable);
            return;
        }

        try (var stream = Streams.newInputStream(message)) {
            while (stream.available() > 0) {
                var node = BinaryNodeDecoder.decode(stream);
                onNodeReceived(node);
                resolvePendingRequest(node);
                streamHandler.digest(node);
            }
        }catch (Throwable throwable) {
            handleFailure(STREAM, throwable);
        }
    }

    private void handleHandshake(ByteBuffer message)  {
        try {
            encryptionHandler.finishHandshake(message);
            state.compareAndSet(State.HANDSHAKING, State.CONNECTED);
        } catch (Throwable throwable) {
            handleFailure(LOGIN, throwable);
        }
    }

    private void onNodeReceived(Node node) {
        callListenersAsync(listener -> {
            listener.onNodeReceived(whatsapp, node);
            listener.onNodeReceived(node);
        });
    }

    public void sendNodeWithNoResponse(Node node) {
        if(encryptionHandler.sendCiphered(node)) {
            onNodeSent(node);
        }
    }

    public Node sendNode(Node node) {
        return sendNode(node, null);
    }

    public Node sendNode(Node node, Function<Node, Boolean> filter) {
        if (node.id() == null) {
            node.attributes().put("id", Bytes.randomHex(6));
        }

        if(!encryptionHandler.sendCiphered(node)) {
            return Node.empty();
        }

        onNodeSent(node);
        var request = new Request(node.id(), filter, node);
        requests.put(node.id(), request);
        try {
            return request.waitForResponse(TIMEOUT);
        }catch (Throwable ignored) {
            return Node.empty();
        }
    }

    public void sendBinary(byte[] binary) {
        if (state.getAcquire() == State.DISCONNECTED) {
            throw new IllegalStateException("Instance is not connected");
        }

        session.sendBinary(binary);
    }

    public void connect(WhatsappDisconnectReason reason)  {
        if(!state.compareAndSet(State.DISCONNECTED, State.HANDSHAKING)) {
            return;
        }

        try {
            this.session = SocketSession.of(store.proxy().orElse(null));
            session.connect(this::onMessage);
        } catch (Throwable throwable) {
            state.set(State.DISCONNECTED);
            if (reason == WhatsappDisconnectReason.RECONNECTING) {
                handleFailure(RECONNECT, throwable);
            }
        }

        if (shutdownHook == null) {
            this.shutdownHook = Thread.ofPlatform()
                    .name("CobaltShutdownHandler")
                    .unstarted(this::onShutdown);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        encryptionHandler.startHandshake(keys.ephemeralKeyPair().publicKey());
    }

    public void disconnect(WhatsappDisconnectReason reason)  {
        if(!state.compareAndSet(State.CONNECTED, State.DISCONNECTED)) {
            return;
        }

        if (session != null) {
            session.disconnect();
        }

        onDisconnected(reason);
    }

    private void onDisconnected(WhatsappDisconnectReason reason) {
        encryptionHandler.reset();
        requests.forEach((_, request) -> request.complete(Node.empty()));
        requests.clear();
        if (reason == WhatsappDisconnectReason.LOGGED_OUT || reason == WhatsappDisconnectReason.BANNED) {
            store.deleteSession();
            serializable.set(false);
        }
        if(reason != WhatsappDisconnectReason.RECONNECTING && shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }
        callListenersSync(listener -> {
            listener.onDisconnected(whatsapp, reason);
            listener.onDisconnected(reason);
        });
        if(reason == WhatsappDisconnectReason.RECONNECTING) {
            connect(reason);
        }
    }

    public void pushPatch(PatchRequest request) {
        var jid = store.jid().orElseThrow(() -> new IllegalStateException("The session isn't connected"));
        appStateHandler.push(jid, List.of(request));
    }

    public void pullPatch(PatchType... patchTypes) {
        appStateHandler.pull(patchTypes);
    }

    void pullInitialPatches() {
        appStateHandler.pullInitial();
    }

    public void decodeMessage(Node node, JidProvider chatOverride, boolean notify) {
        messageHandler.decode(node, chatOverride, notify);
    }

    public void sendMessage(MessageRequest request) {
        messageHandler.encode(request);
    }

    @SuppressWarnings("UnusedReturnValue")
    public void sendQueryWithNoResponse(String method, String category, Node... body) {
        sendQueryWithNoResponse(null, JidServer.whatsapp().toJid(), method, category, null, body);
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
        return sendQuery(null, JidServer.whatsapp().toJid(), method, category, null, body);
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
                && queryGroupMetadata(chat.toJid()).isCommunity();
        return sendQuery("get", "w:profile:picture", Map.of(community ? "parent_group_jid" : "target", chat.toJid()), body)
                .findChild("picture")
                .flatMap(picture -> picture.attributes().getOptionalString("url"))
                .map(URI::create);
    }

    public Node sendQuery(String method, String category, Map<String, Object> metadata, Node... body) {
        return sendQuery(null, JidServer.whatsapp().toJid(), method, category, metadata, body);
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

    public ChatMetadata queryGroupMetadata(JidProvider group) {
        var metadata = chatMetadataCache.get(group.toJid());
        if (metadata != null) {
            return metadata;
        }

        var body = Node.of("query", Map.of("request", "interactive"));
        var response = sendQuery(group.toJid(), "get", "w:g2", body);
        var result = handleGroupMetadata(response);
        chatMetadataCache.put(group.toJid(), result);
        return result;
    }

    public ChatMetadata handleGroupMetadata(Node response) {
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

    private ChatMetadata parseGroupMetadata(Node node) {
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

    public void sendRetryReceipt(long nodeTimestamp, Jid chatJid, Jid participantJid, String messageId) {
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
        sendNodeWithNoResponse(receipt);
    }

    public void sendReceipt(Jid jid, Jid participant, List<String> messages, String type) {
        if (messages.isEmpty()) {
            return;
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

    void sendMessageAck(Jid from, Node node) {
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
        sendNodeWithNoResponse(Node.of("ack", attributes));
    }

    void onRegistrationCode(long code) {
        callListenersAsync(listener -> {
            listener.onRegistrationCode(whatsapp, code);
            listener.onRegistrationCode(code);
        });
    }

    void onMetadata(Map<String, String> properties) {
        callListenersAsync(listener -> {
            listener.onMetadata(whatsapp, properties);
            listener.onMetadata(properties);
        });
    }

    void onMessageStatus(MessageInfo<?> message) {
        callListenersAsync(listener -> {
            listener.onMessageStatus(whatsapp, message);
            listener.onMessageStatus(message);
        });
    }

    void onUpdateChatPresence(ContactStatus status, Jid jid, Chat chat) {
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

    void onNewMessage(MessageInfo<?> info) {
        callListenersAsync(listener -> {
            listener.onNewMessage(whatsapp, info);
            listener.onNewMessage(info);
        });
    }

    void onNewStatus(ChatMessageInfo info) {
        callListenersAsync(listener -> {
            listener.onNewStatus(whatsapp, info);
            listener.onNewStatus(info);
        });
    }

    void onChatRecentMessages(Chat chat, boolean last) {
        callListenersAsync(listener -> {
            listener.onChatMessagesSync(whatsapp, chat, last);
            listener.onChatMessagesSync(chat, last);
        });
    }

    void onFeatures(PrimaryFeature features) {
        callListenersAsync(listener -> {
            listener.onFeatures(whatsapp, features.flags());
            listener.onFeatures(features.flags());
        });
    }

    void onSetting(Setting setting) {
        callListenersAsync(listener -> {
            listener.onSetting(whatsapp, setting);
            listener.onSetting(setting);
        });
    }

    void onMessageDeleted(MessageInfo<?> message, boolean everyone) {
        callListenersAsync(listener -> {
            listener.onMessageDeleted(whatsapp, message, everyone);
            listener.onMessageDeleted(message, everyone);
        });
    }

    void onAction(Action action, MessageIndexInfo indexInfo) {
        callListenersAsync(listener -> {
            listener.onAction(whatsapp, action, indexInfo);
            listener.onAction(action, indexInfo);
        });
    }

    void onLoggedIn() {
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

    void onChats() {
        callListenersAsync(listener -> {
            listener.onChats(whatsapp, store().chats());
            listener.onChats(store().chats());
        });
    }

    void onNewsletters() {
        callListenersAsync(listener -> {
            listener.onNewsletters(whatsapp, store().newsletters());
            listener.onNewsletters(store().newsletters());
        });
    }

    void onStatus() {
        callListenersAsync(listener -> {
            listener.onStatus(whatsapp, store().status());
            listener.onStatus(store().status());
        });
    }

    void onContacts() {
        callListenersAsync(listener -> {
            listener.onContacts(whatsapp, store().contacts());
            listener.onContacts(store().contacts());
        });
    }

    void onHistorySyncProgress(Integer progress, boolean recent) {
        callListenersAsync(listener -> {
            listener.onHistorySyncProgress(whatsapp, progress, recent);
            listener.onHistorySyncProgress(progress, recent);
        });
    }

    void onReply(ChatMessageInfo info) {
        var quoted = info.quotedMessage().orElse(null);
        if (quoted == null) {
            return;
        }
        // FIXME: store.resolvePendingReply(info);
        callListenersAsync(listener -> {
            listener.onMessageReply(whatsapp, info, quoted);
            listener.onMessageReply(info, quoted);
        });
    }

    void onGroupPictureChanged(Chat fromChat) {
        callListenersAsync(listener -> {
            listener.onGroupPictureChanged(whatsapp, fromChat);
            listener.onGroupPictureChanged(fromChat);
        });
    }

    void onContactPictureChanged(Contact fromContact) {
        callListenersAsync(listener -> {
            listener.onProfilePictureChanged(whatsapp, fromContact);
            listener.onProfilePictureChanged(fromContact);
        });
    }

    void onUserAboutChanged(String newAbout, String oldAbout) {
        callListenersAsync(listener -> {
            listener.onAboutChanged(whatsapp, oldAbout, newAbout);
            listener.onAboutChanged(oldAbout, newAbout);
        });
    }

    public void onUserPictureChanged() {
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

    void onContactBlocked(Contact contact) {
        callListenersAsync(listener -> {
            listener.onContactBlocked(whatsapp, contact);
            listener.onContactBlocked(contact);
        });
    }

    void onNewContact(Contact contact) {
        callListenersAsync(listener -> {
            listener.onNewContact(whatsapp, contact);
            listener.onNewContact(contact);
        });
    }

    void onDevices(LinkedHashMap<Jid, Integer> devices) {
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

    void querySessionsForcefully(Jid jid) {
        messageHandler.querySessions(List.of(jid), true);
    }

    private void dispose() {
        streamHandler.dispose();
        messageHandler.dispose();
        appStateHandler.dispose();
    }

    void handleFailure(Location location, Throwable throwable)  {
        var result = errorHandler.handleError(whatsapp, location, throwable);
        switch (result) {
            case LOG_OUT -> disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            case DISCONNECT -> disconnect(WhatsappDisconnectReason.DISCONNECTED);
            case RECONNECT -> disconnect(WhatsappDisconnectReason.RECONNECTING);
        }
    }

    public void querySessions(List<Jid> jid) {
        messageHandler.querySessions(jid, true);
        messageHandler.queryDevices(jid, false);
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
        return state.getAcquire() == State.CONNECTED;
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
    void scheduleAtFixedInterval(Runnable command, long initialDelay, long period) {
        if (state.getAcquire() == State.CONNECTED) {
            createScheduler();
            scheduler.scheduleAtFixedRate(command, initialDelay, period, SECONDS);
        }
    }

    ScheduledFuture<?> scheduleDelayed(Runnable command, long delay) {
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

    Node sendPing()  {
        try {
            var attributes = Attributes.of()
                    .put("xmlns", "w:p")
                    .put("to", JidServer.whatsapp().toJid())
                    .put("type", "get")
                    .put("id", HexFormat.of().formatHex(Bytes.random(6)))
                    .toMap();
            var node = Node.of("iq", attributes, Node.of("ping"));
            return sendNode(node);
        }catch (Throwable throwable) {
            return Node.empty();
        }
    }

    public void updateBusinessCertificate(String newName) {
        streamHandler.updateBusinessCertificate(newName);
    }

    public ConcurrentMap<Jid, SequencedSet<ChatPastParticipant>> pastParticipants() {
        return pastParticipants;
    }

    public void addPastParticipant(Jid jid, ChatPastParticipant pastParticipant) {
        var pastParticipants = pastParticipants().get(jid);
        if (pastParticipants != null) {
            pastParticipants.add(pastParticipant);
            this.pastParticipants.put(jid, pastParticipants);
        } else {
            var values = new LinkedHashSet<ChatPastParticipant>();
            values.add(pastParticipant);
            this.pastParticipants.put(jid, values);
        }
    }

    public void addPastParticipant(Jid jid, Collection<? extends ChatPastParticipant> pastParticipant) {
        var pastParticipants = pastParticipants().get(jid);
        if (pastParticipants != null) {
            pastParticipants.addAll(pastParticipant);
            this.pastParticipants.put(jid, pastParticipants);
        } else {
            var values = new LinkedHashSet<ChatPastParticipant>(pastParticipant);
            this.pastParticipants.put(jid, values);
        }
    }

    void queryNewsletters()  {
        try {
            streamHandler.queryNewsletters();
        }catch (Throwable throwable) {
            handleFailure(HISTORY_SYNC, throwable);
        }
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

    void resolvePendingRequest(Node node) {
        var id = node.id();
        if(id == null) {
            return;
        }

        var request = requests.get(id);
        if (request == null) {
            return;
        }

        var completed = request.complete(node);
        if (!completed) {
            return;
        }

        requests.remove(id);
    }

    private enum State {
        DISCONNECTED,
        HANDSHAKING,
        CONNECTED
    }

    private static final class Request {
        private final String id;
        private final Object body;
        private final Function<Node, Boolean> filter;
        private volatile Node response;

        Request(String id, Function<Node, Boolean> filter, Object body) {
            this.id = id;
            this.body = body;
            this.filter = filter;
        }

        public boolean complete(Node response) {
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

        public String id() {
            return id;
        }

        public Object body() {
            return body;
        }

        public Node waitForResponse(Duration timeout) {
            Objects.requireNonNull(timeout, "Timeout cannot be null");
            if (response == null) {
                synchronized (this) {
                    if (response == null) {
                        try {
                            wait(timeout.toMillis());
                        }catch (InterruptedException exception) {
                            throw new RuntimeException("Cannot wait for response", exception);
                        }
                    }
                }
            }
            if(response == null) {
                throw new RuntimeException("The timeout of " + timeout + " has expired for " + body);
            }
            return response;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this
                    || obj instanceof Request that && Objects.equals(this.id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "SocketRequest[" + "id=" + id + ']';
        }
    }
}
