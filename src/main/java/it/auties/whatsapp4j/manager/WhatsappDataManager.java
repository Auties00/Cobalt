package it.auties.whatsapp4j.manager;

import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.media.MediaConnection;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.chat.ChatMute;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.message.server.ProtocolMessage;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.request.model.Request;
import it.auties.whatsapp4j.response.impl.json.PhoneBatteryResponse;
import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import it.auties.whatsapp4j.response.model.common.Response;
import it.auties.whatsapp4j.response.model.json.JsonResponse;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import it.auties.whatsapp4j.whatsapp.internal.WhatsappWebSocket;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a singleton and holds all of the data regarding a session with WhatsappWeb's WebSocket.
 * It also provides various methods to query this data.
 * It should not be used by multiple sessions as, being a singleton, it cannot determine and divide data coming from different sessions.
 * It should not be initialized manually.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Accessors(fluent = true)
public class WhatsappDataManager {
    private static final @Getter WhatsappDataManager singletonInstance = new WhatsappDataManager(Executors.newSingleThreadExecutor(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Instant.now().getEpochSecond());
    private final @NonNull ExecutorService requestsService;
    private final @NonNull List<Chat> chats;
    private final @NonNull List<Contact> contacts;
    private final @NonNull List<Request<?, ?>> pendingRequests;
    private final @NonNull List<WhatsappListener> listeners;
    private final long initializationTimeStamp;
    private String phoneNumberJid;
    private MediaConnection mediaConnection;
    private long tag;

    /**
     * Queries the first contact whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Contact> findContactByJid(@NonNull String jid) {
        return Collections.synchronizedList(contacts)
                .stream()
                .filter(e -> Objects.equals(e.jid(), WhatsappUtils.parseJid(jid)))
                .findAny();
    }

    /**
     * Queries the first contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Contact> findContactByName(@NonNull String name) {
        return Collections.synchronizedList(contacts)
                .stream()
                .filter(e -> Objects.equals(e.bestName().orElse(null), name))
                .findAny();
    }

    /**
     * Queries every contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
     */
    public @NonNull Set<Contact> findContactsByName(@NonNull String name) {
        return Collections.synchronizedList(contacts)
                .stream()
                .filter(e -> Objects.equals(e.bestName().orElse(null), name))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first chat whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Chat> findChatByJid(@NonNull String jid) {
        return Collections.synchronizedList(chats)
                .stream()
                .filter(e -> Objects.equals(e.jid(), WhatsappUtils.parseJid(jid)))
                .findAny();
    }

    /**
     * Queries the message in {@code chat} whose jid is equal to {@code jid}
     *
     * @param chat the chat to search in
     * @param id   the jid to search
     * @return a non empty Optional containing the result if it is found otherwise an empty Optional empty
     */
    public @NonNull Optional<MessageInfo> findMessageById(@NonNull Chat chat, @NonNull String id) {
        return chat.messages().stream().filter(e -> Objects.equals(e.key().id(), id)).findAny();
    }

    /**
     * Queries the chat associated with {@code message}
     *
     * @param message the message to use as context
     * @return a non empty Optional containing the result if it is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Chat> findChatByMessage(@NonNull MessageInfo message) {
        return findChatByJid(message.key().chatJid());
    }

    /**
     * Queries the first chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Chat> findChatByName(@NonNull String name) {
        return Collections.synchronizedList(chats)
                .stream()
                .filter(e -> Objects.equals(e.displayName(), name))
                .findAny();
    }

    /**
     * Queries every chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
     */
    public @NonNull Set<Chat> findChatsByName(@NonNull String name) {
        return Collections.synchronizedList(chats)
                .stream()
                .filter(e -> Objects.equals(e.displayName(), name))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first Request whose tag is equal to {@code tag}
     *
     * @param tag the tag to search
     * @return a non empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Request<?, ?>> findPendingRequest(@NonNull String tag) {
        return Collections.synchronizedList(pendingRequests)
                .stream()
                .filter(req -> req.tag().equals(tag))
                .findAny();
    }

    /**
     * Queries the first Request whose tag is equal to {@code messageTag} and, if any is found, resolves the request using {@code response}
     *
     * @param messageTag the tag to search
     * @param response   the response to complete the request with
     * @return true if any request matching {@code messageTag} is found
     */
    public boolean resolvePendingRequest(@NonNull String messageTag, @NonNull Response<?> response) {
        var req = findPendingRequest(messageTag);
        if (req.isEmpty()) {
            return false;
        }

        var request = req.get();
        request.complete(response);
        pendingRequests.remove(request);
        return true;
    }

    /**
     * Adds a chat in memory
     *
     * @param chat the chat to add
     * @return the input chat
     */
    public @NonNull Chat addChat(@NonNull Chat chat) {
        chats.add(chat);
        return chat;
    }

    /**
     * Returns the number of pinned chats
     *
     * @return an unsigned int between zero and three(both inclusive)
     */
    public long pinnedChats(){
        return chats
                .stream()
                .filter(Chat::isPinned)
                .count();
    }

    /**
     * Clears all data associated with the WhatsappWeb's WebSocket session
     */
    public void clear() {
        chats.clear();
        contacts.clear();
        pendingRequests.clear();
    }

    /**
     * Returns the incremental tag and then increments it
     *
     * @return the tag
     */
    public long tagAndIncrement() {
        return tag++;
    }

    /**
     * Returns the phone number
     *
     * @return the phone number
     * @throws NullPointerException if the phone number is null
     */
    public @NonNull String phoneNumberJid() {
        return Objects.requireNonNull(phoneNumberJid, "WhatsappAPI: Phone number is missing");
    }

    /**
     * Returns the media connection
     *
     * @return the media connection
     * @throws NullPointerException if the media connection is null
     */
    public @NonNull MediaConnection mediaConnection() {
        return Objects.requireNonNull(mediaConnection, "WhatsappAPI: Media connection is missing");
    }

    /**
     * Executes a runnable on a single threaded ExecutorService.
     * This should be used to be sure that when a listener should be called it's called on a thread that is not the WebSocket's.
     * If this condition isn't met, if the thread is put on hold to wait for a response for a pending request, the WebSocket will freeze.
     */
    public void callOnListenerThread(@NonNull Runnable runnable) {
        requestsService.execute(runnable);
    }

    /**
     * Digests a {@code node} adding the data it contains to the data this singleton holds
     *
     * @param socket the WebSocket associated with the WhatsappWeb's session
     * @param node   the WhatsappNode to digest
     */
    public void digestWhatsappNode(@NonNull WhatsappWebSocket socket, @NonNull Node node) {
        var description = node.description();
        var attrs = node.attrs();
        var content = node.content();
        var duplicate = Boolean.parseBoolean(attrs.getOrDefault("duplicate", "false"));
        if (duplicate) {
            return;
        }

        switch (description) {
            case "response" -> parseResponse(socket, node, content);
            case "action" -> parseAction(socket, node, content);
        }
    }

    private void parseAction(@NonNull WhatsappWebSocket socket, @NonNull Node node, Object content) {
        if (!(content instanceof List<?> listContent)) {
            return;
        }

        var nodes = Node.fromGenericList(listContent);
        if (nodes.isEmpty()) {
            return;
        }

        var firstChildNode = nodes.get(0);
        switch (firstChildNode.description()) {
            case "chat" -> parseChatAction(firstChildNode);
            case "user" -> parseContact(node);
            case "battery" -> parseBattery(node);
            case "read" -> parseReadStatus(firstChildNode);
            case "received" -> parseReceivedStatus(firstChildNode);
            case "contacts", "broadcast" -> {} // Recent contacts and broadcast lists
            case "message" -> processMessages(socket, node, nodes);
        }
    }

    private void parseChatAction(@NonNull Node node) {
        var jid = node.attrs().get("jid");
        if (jid == null) {
            return;
        }

        var chat = findChatByJid(jid).orElse(null);
        if (chat == null) {
            return;
        }

        var type = node.attrs().get("type");
        if (type == null) {
            return;
        }

        switch (type) {
            case "archive" -> archiveChat(chat, true);
            case "unarchive" -> archiveChat(chat, false);
            case "mute" -> muteChat(node, chat);
            case "star" -> starMessage(node, chat);
            case "unstar" -> unstarMessage(node, chat);
            case "clear" -> deleteMessage(node, chat);
            case "delete" -> chats.remove(chat);
        }
    }

    private void parseContact(@NonNull Node node) {
        if (!(node.content() instanceof List<?> content)) {
            return;
        }

        Node.fromGenericList(content).forEach(childNode -> addOrReplaceContact(Contact.fromAttributes(childNode.attrs())));
    }

    private void addOrReplaceContact(@NonNull Contact contact) {
        if (findContactByJid(contact.jid()).isPresent()) {
            contacts.remove(contact);
            contacts.add(contact);
            callListeners(listener -> listener.onContactUpdate(contact));
            return;
        }

        contacts.add(contact);
        callListeners(listener -> listener.onNewContact(contact));
    }

    private void parseBattery(@NonNull Node node) {
        if (!(node.content() instanceof List<?> content)) {
            return;
        }

        var nodes = Node.fromGenericList(content);
        nodes.forEach(childNode -> callListeners(listener -> parseBattery(childNode, listener)));
    }

    private void parseBattery(@NonNull Node childNode, @NonNull WhatsappListener listener) {
        var json = JsonResponse.fromMap(childNode.attrs());
        var battery = json.toModel(PhoneBatteryResponse.class);
        listener.onPhoneBatteryStatusUpdate(battery);
    }

    private void muteChat(@NonNull Node node, @NonNull Chat chat) {
        chat.mute(new ChatMute(Long.parseLong(node.attrs().get("mute"))));
        callListeners(listener -> listener.onChatMuteChange(chat));
    }

    private void archiveChat(@NonNull Chat chat, boolean archive) {
        chat.isArchived(archive);
        callListeners(listener -> archiveChat(chat, listener, archive));
    }

    private void archiveChat(@NonNull Chat chat, @NonNull WhatsappListener listener, boolean archive) {
        if (archive) {
            listener.onChatArchived(chat);
            return;
        }

        listener.onChatUnarchived(chat);
    }

    private void deleteMessage(@NonNull Node node, @NonNull Chat chat) {
        if (node.content() == null) {
            chat.messages().clear();
            return;
        }

        if (!(node.content() instanceof List<?> content)) {
            return;
        }

        var childNodes = Node.fromGenericList(content);
        if (childNodes.isEmpty()) {
            return;
        }

        findMessagesFromNode(chat, childNodes).forEach(message -> {
            chat.messages().remove(message);
            callListeners(listener -> listener.onMessageDeleted(chat, message, false));
        });
    }

    private void unstarMessage(@NonNull Node node, @NonNull Chat chat) {
        if (!(node.content() instanceof List<?> content)) {
            return;
        }

        var childNodes = Node.fromGenericList(content);
        if (childNodes.isEmpty()) {
            return;
        }

        findMessagesFromNode(chat, childNodes)
                .forEach(message -> unstarMessage(chat, message));
    }

    private @NonNull Stream<MessageInfo> findMessagesFromNode(@NonNull Chat chat, @NonNull List<Node> childNodes) {
        return childNodes.stream()
                .map(Node::attrs)
                .map(entry -> entry.get("index"))
                .filter(Objects::nonNull)
                .map(id -> findMessageById(chat, id))
                .map(Optional::orElseThrow);
    }

    private void unstarMessage(@NonNull Chat chat, @NonNull MessageInfo message) {
        message.starred(false);
        callListeners(listener -> listener.onMessageUnstarred(chat, message));
    }

    private void starMessage(@NonNull Node node, @NonNull Chat chat) {
        if (!(node.content() instanceof List<?> content)) {
            return;
        }

        var childNodes = Node.fromGenericList(content);
        if (childNodes.isEmpty()) {
            return;
        }

        findMessagesFromNodes(childNodes)
                .forEach(message -> starMessage(chat, message));
    }

    private void starMessage(Chat chat, MessageInfo message) {
        chat.messages().addOrReplace(message);
        callListeners(listener -> listener.onMessageStarred(chat, message));
    }

    private @NonNull Stream<MessageInfo> findMessagesFromNodes(@NonNull List<Node> childNodes) {
        return childNodes.stream()
                .map(Node::content)
                .filter(entry -> entry instanceof MessageInfo)
                .map(entry -> (MessageInfo) entry);
    }

    private void parseResponse(@NonNull WhatsappWebSocket socket, @NonNull Node node, Object content) {
        var type = node.attrs().get("type");
        if (type == null) {
            return;
        }

        if (!(content instanceof List<?> listContent)) {
            return;
        }

        var nodes = Node.fromGenericList(listContent);
        if (nodes.isEmpty()) {
            return;
        }

        switch (type) {
            case "contacts" -> parseContacts(nodes);
            case "chat" -> parseChats(nodes);
            case "message" -> processMessages(socket, node, nodes); // TODO: is this right????
        }
    }

    private void processMessages(@NonNull WhatsappWebSocket socket, @NonNull Node node, List<Node> nodes) {
        var action = node.attrs().get("add");
        if(action == null) {
            return;
        }

        var last = Boolean.parseBoolean(node.attrs().getOrDefault("last", "false"));
        var chats = processMessagesFromNodes(socket, nodes);
        if (!action.equals("last") && !last) {
            return;
        }

        chats.forEach(this::processMessages);
    }

    private void processMessages(@NonNull CompletableFuture<Chat> future) {
        callListeners(listener -> processMessages(future, listener));
    }

    private void processMessages(@NonNull CompletableFuture<Chat> future, @NonNull WhatsappListener listener) {
        future.thenAcceptAsync(listener::onChatRecentMessages);
    }

    private void parseReceivedStatus(@NonNull Node firstChildNode) {
        var chatOpt = findChatByJid(firstChildNode.attrs().get("jid"));
        if (chatOpt.isEmpty()) {
            return;
        }

        var chat = chatOpt.get();
        var messageOpt = findMessageById(chat, firstChildNode.attrs().get("index"));
        if (messageOpt.isEmpty()) {
            return;
        }

        var message = messageOpt.get();
        var statusName = firstChildNode.attrs().get("type");
        var status = switch (statusName) {
            case "read" -> MessageInfo.MessageInfoStatus.READ;
            case "message" -> MessageInfo.MessageInfoStatus.DELIVERY_ACK;
            case "error" -> MessageInfo.MessageInfoStatus.ERROR;
            default -> throw new IllegalStateException("Cannot process read status, unexpected value: %s".formatted(statusName));
        };

        if (status.index() <= message.globalStatus().index() && status != MessageInfo.MessageInfoStatus.ERROR) {
            return;
        }

        message.globalStatus(status);
        callListeners(listener -> listener.onMessageGlobalReadStatusUpdate(chat, message));
    }

    private void parseReadStatus(@NonNull Node firstChildNode) {
        var jid = firstChildNode.attrs().get("jid");
        if (jid == null) {
            return;
        }

        var type = Boolean.parseBoolean(firstChildNode.attrs().getOrDefault("type", "true"));
        var chatOpt = findChatByJid(jid);
        if (chatOpt.isEmpty()) {
            return;
        }

        var chat = chatOpt.get();
        chat.unreadMessages(type ? 0 : -1);
        callListeners(listener -> listener.onChatReadStatusChange(chat));
    }

    private void parseChats(@NonNull List<Node> nodes) {
        nodes.stream()
                .map(Node::attrs)
                .map(Chat::fromAttributes)
                .forEach(this::addChat);
        callListeners(WhatsappListener::onChats);
    }

    private void parseContacts(@NonNull List<Node> nodes) {
        nodes.stream().map(Node::attrs).map(Contact::fromAttributes).forEach(contacts::add);
        callListeners(WhatsappListener::onContacts);
    }

    private Set<CompletableFuture<Chat>> processMessagesFromNodes(@NonNull WhatsappWebSocket socket, @NonNull List<Node> nodes) {
        return nodes.stream()
                .filter(node -> node.content() instanceof MessageInfo)
                .map(node -> (MessageInfo) node.content())
                .map(messageInfo -> processMessageFromNode(socket, messageInfo))
                .collect(Collectors.toUnmodifiableSet());
    }

    private @NonNull CompletableFuture<Chat> processMessageFromNode(@NonNull WhatsappWebSocket socket, @NonNull MessageInfo messageInfo) {
        return findChatByMessage(messageInfo)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> queryMissingChat(socket, messageInfo.key().chatJid()))
                .thenApplyAsync(chat -> processMessageFromNode(messageInfo, chat));
    }

    private @NonNull Chat processMessageFromNode(@NonNull MessageInfo messageInfo, @NonNull Chat chat) {
        processMessage(chat, messageInfo);
        return chat;
    }

    private void processMessage(@NonNull Chat chat, @NonNull MessageInfo message) {
        processServerMessage(chat, message);
        commitMessage(chat, message);
        broadcastMessage(chat, message);
    }

    private void broadcastMessage(@NonNull Chat chat, @NonNull MessageInfo message) {
        if (initializationTimeStamp > message.timestamp()) {
            return;
        }

        updateUnreadMessages(message, chat);
        callListeners(listener -> listener.onNewMessage(chat, message));
    }

    private void commitMessage(@NonNull Chat chat, @NonNull MessageInfo message) {
        if (!chat.messages().addOrReplace(message)) {
            return;
        }

        callListeners(listener -> listener.onMessageUpdate(chat, message));
    }

    private void processServerMessage(@NonNull Chat chat, @NonNull MessageInfo message) {
        if (!message.container().isServerMessage()) {
            return;
        }

        var protocolMessage = message.container().protocolMessage();
        switch (protocolMessage.type()){
            case REVOKE -> processRevokeMessage(chat, protocolMessage);
            case EPHEMERAL_SETTING, EPHEMERAL_SYNC_RESPONSE -> processEphemeralUpdate(chat, protocolMessage);
            case HISTORY_SYNC_NOTIFICATION -> throw new UnsupportedOperationException("WhatsappWeb4j cannot handle history syncs as of now");
        }
    }

    private void processEphemeralUpdate(@NonNull Chat chat, @NonNull ProtocolMessage protocolMessage) {
        chat.ephemeralMessagesToggleTime(protocolMessage.ephemeralSettingTimestamp());
        chat.ephemeralMessageDuration(protocolMessage.ephemeralExpiration());
    }

    private void processRevokeMessage(@NonNull Chat chat, @NonNull ProtocolMessage message) {
        var id = message.key().id();
        findMessageById(chat, id).ifPresent(oldMessage -> processRevokeMessage(chat, oldMessage));
    }

    private void processRevokeMessage(@NonNull Chat chat, @NonNull MessageInfo oldMessage) {
        chat.messages().remove(oldMessage);
        callListeners(listener -> listener.onMessageDeleted(chat, oldMessage, true));
    }

    private void updateUnreadMessages(@NonNull MessageInfo message, Chat chat) {
        if (message.key().fromMe() || message.globalStatus() == MessageInfo.MessageInfoStatus.READ || message.ignore()) {
            return;
        }

        chat.unreadMessages(chat.unreadMessages() + 1);
    }

    private @NonNull CompletableFuture<Chat> queryMissingChat(@NonNull WhatsappWebSocket socket, @NonNull String jid) {
        return socket.queryChat(jid)
                .thenApplyAsync(BinaryResponseModel::data)
                .thenApplyAsync(this::addChat);
    }

    private void callListeners(@NonNull Consumer<WhatsappListener> consumer){
        listeners.forEach(listener -> callOnListenerThread(() -> consumer.accept(listener)));
    }
}
