package it.auties.whatsapp4j.manager;

import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.media.MediaConnection;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.chat.ChatMute;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.request.model.Request;
import it.auties.whatsapp4j.response.impl.json.PhoneBatteryResponse;
import it.auties.whatsapp4j.response.model.common.Response;
import it.auties.whatsapp4j.response.model.json.JsonResponse;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    private final @NotNull ExecutorService requestsService;
    private final @NotNull List<Chat> chats;
    private final @NotNull List<Contact> contacts;
    private final @NotNull List<Request<?, ?>> pendingRequests;
    private final @NotNull List<WhatsappListener> listeners;
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
    public @NotNull Optional<Contact> findContactByJid(@NotNull String jid) {
        return contacts.stream().filter(e -> Objects.equals(e.jid(), WhatsappUtils.parseJid(jid))).findFirst();
    }

    /**
     * Queries the first contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NotNull Optional<Contact> findContactByName(@NotNull String name) {
        return contacts.stream().filter(e -> Objects.equals(e.bestName().orElse(null), name)).findFirst();
    }

    /**
     * Queries every contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
     */
    public @NotNull Set<Contact> findContactsByName(@NotNull String name) {
        return contacts.stream().filter(e -> Objects.equals(e.bestName().orElse(null), name)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first chat whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NotNull Optional<Chat> findChatByJid(@NotNull String jid) {
        return chats.stream().filter(e -> Objects.equals(e.jid(), WhatsappUtils.parseJid(jid))).findFirst();
    }

    /**
     * Queries the message in {@code chat} whose jid is equal to {@code jid}
     *
     * @param chat the chat to search in
     * @param id   the jid to search
     * @return a non empty Optional containing the result if it is found otherwise an empty Optional empty
     */
    public @NotNull Optional<MessageInfo> findMessageById(@NotNull Chat chat, @NotNull String id) {
        return chat.messages().stream().filter(e -> Objects.equals(e.key().id(), id)).findAny();
    }

    /**
     * Queries the chat associated with {@code message}
     *
     * @param message the message to use as context
     * @return a non empty Optional containing the result if it is found otherwise an empty Optional empty
     */
    public @NotNull Optional<Chat> findChatByMessage(@NotNull MessageInfo message) {
        return findChatByJid(message.key().chatJid());
    }

    /**
     * Queries the first chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NotNull Optional<Chat> findChatByName(@NotNull String name) {
        return chats.stream().filter(e -> Objects.equals(e.displayName(), name)).findFirst();
    }

    /**
     * Queries every chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
     */
    public @NotNull Set<Chat> findChatsByName(@NotNull String name) {
        return chats.stream().filter(e -> Objects.equals(e.displayName(), name)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first Request whose tag is equal to {@code tag}
     *
     * @param tag the tag to search
     * @return a non empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NotNull Optional<Request<?, ?>> findPendingRequest(@NotNull String tag) {
        return pendingRequests.stream().filter(req -> req.tag().equals(tag)).findAny();
    }

    /**
     * Queries the first Request whose tag is equal to {@code messageTag} and, if any is found, resolves the request using {@code response}
     *
     * @param messageTag the tag to search
     * @param response   the response to complete the request with
     * @return true if any request matching {@code messageTag} is found
     */
    public boolean resolvePendingRequest(@NotNull String messageTag, @NotNull Response<?> response) {
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
    public @NotNull String phoneNumberJid() {
        return Objects.requireNonNull(phoneNumberJid, "WhatsappAPI: Phone number is missing");
    }

    /**
     * Returns the media connection
     *
     * @return the media connection
     * @throws NullPointerException if the media connection is null
     */
    public @NotNull MediaConnection mediaConnection() {
        return Objects.requireNonNull(mediaConnection, "WhatsappAPI: Media connection is missing");
    }

    /**
     * Executes a runnable on a single threaded ExecutorService.
     * This should be used to be sure that when a listener should be called it's called on a thread that is not the WebSocket's.
     * If this condition isn't met, if the thread is put on hold to wait for a response for a pending request, the WebSocket will freeze.
     */
    public void callOnListenerThread(@NotNull Runnable runnable) {
        requestsService.execute(runnable);
    }

    /**
     * Digests a {@code node} adding the data it contains to the data this singleton holds
     *
     * @param socket the WebSocket associated with the WhatsappWeb's session
     * @param node   the WhatsappNode to digest
     */
    public void digestWhatsappNode(@NotNull WhatsappWebSocket socket, @NotNull Node node) {
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

    private void parseAction(@NotNull WhatsappWebSocket socket, @NotNull Node node, Object content) {
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

    private void parseChatAction(@NotNull Node node) {
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

    private void parseContact(@NotNull Node node) {
        if (!(node.content() instanceof List<?> content)) {
            return;
        }

        Node.fromGenericList(content).forEach(childNode -> addOrReplaceContact(Contact.fromAttributes(childNode.attrs())));
    }

    private void addOrReplaceContact(@NotNull Contact contact) {
        if (findContactByJid(contact.jid()).isPresent()) {
            contacts.remove(contact);
            contacts.add(contact);
            listeners.forEach(listener -> callOnListenerThread(() -> listener.onContactUpdate(contact)));
            return;
        }

        contacts.add(contact);
        listeners.forEach(listener -> callOnListenerThread(() -> listener.onNewContact(contact)));
    }

    private void parseBattery(@NotNull Node node) {
        if (!(node.content() instanceof List<?> content)) {
            return;
        }

        Node.fromGenericList(content).forEach(childNode -> listeners.forEach(listener -> callOnListenerThread(() -> listener.onPhoneBatteryStatusUpdate(new JsonResponse("", "", childNode.attrs()).toModel(PhoneBatteryResponse.class)))));
    }

    private void muteChat(@NotNull Node node, @NotNull Chat chat) {
        chat.mute(new ChatMute(Long.parseLong(node.attrs().get("mute"))));
        listeners.forEach(listener -> callOnListenerThread(() -> listener.onChatMuteChange(chat)));
    }

    private void archiveChat(@NotNull Chat chat, boolean archive) {
        chat.isArchived(archive);
        listeners.forEach(listener -> callOnListenerThread(() -> {
            if (archive) {
                listener.onChatArchived(chat);
                return;
            }

            listener.onChatUnarchived(chat);
        }));
    }

    private void deleteMessage(@NotNull Node node, @NotNull Chat chat) {
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

        childNodes.stream().map(Node::attrs).map(entry -> entry.get("index")).filter(Objects::nonNull).map(id -> findMessageById(chat, id)).map(Optional::orElseThrow).forEach(message -> {
            chat.messages().remove(message);
            listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageDeleted(chat, message, false)));
        });
    }

    private void unstarMessage(@NotNull Node node, @NotNull Chat chat) {
        if (!(node.content() instanceof List<?> content)) {
            return;
        }

        var childNodes = Node.fromGenericList(content);
        if (childNodes.isEmpty()) {
            return;
        }

        childNodes.stream().map(Node::attrs).map(entry -> entry.get("index")).filter(Objects::nonNull).map(id -> findMessageById(chat, id)).map(Optional::orElseThrow).forEach(message -> {
            message.starred(false);
            listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageUnstarred(chat, message)));
        });
    }

    private void starMessage(@NotNull Node node, @NotNull Chat chat) {
        if (!(node.content() instanceof List<?> content)) {
            return;
        }

        var childNodes = Node.fromGenericList(content);
        if (childNodes.isEmpty()) {
            return;
        }

        childNodes.stream().map(Node::content).filter(entry -> entry instanceof MessageInfo).map(MessageInfo.class::cast).forEach(message -> {
            chat.messages().addOrReplace(message);
            listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageStarred(chat, message)));
        });
    }

    private void parseResponse(@NotNull WhatsappWebSocket socket, @NotNull Node node, Object content) {
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
            case "contacts" -> parseContacts(listeners, nodes);
            case "chat" -> parseChats(nodes);
            case "message" -> processMessagesFromNodes(socket, nodes);
        }
    }

    private void processMessages(@NotNull WhatsappWebSocket socket, @NotNull Node node, List<Node> nodes) {
        var action = node.attrs().getOrDefault("add", "unknown");
        if(action.equals("unknown")){
            System.out.println(nodes);
        }

        var last = Boolean.parseBoolean(node.attrs().getOrDefault("last", "false"));
        var chat = processMessagesFromNodes(socket, nodes);
        if (!action.equals("last") && !last) {
            return;
        }

        listeners.forEach(listener -> callOnListenerThread(() -> listener.onChatRecentMessages(chat)));
    }

    private void parseReceivedStatus(@NotNull Node firstChildNode) {
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
        var status = switch (firstChildNode.attrs().get("type")) {
            case "read" -> MessageInfo.MessageInfoStatus.READ;
            case "message" -> MessageInfo.MessageInfoStatus.DELIVERY_ACK;
            case "error" -> MessageInfo.MessageInfoStatus.ERROR;
            default -> throw new IllegalStateException("Unexpected value");
        };

        if (status.index() <= message.globalStatus().index() && status != MessageInfo.MessageInfoStatus.ERROR) {
            return;
        }

        message.globalStatus(status);
        listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageGlobalReadStatusUpdate(chat, message)));
    }

    private void parseReadStatus(@NotNull Node firstChildNode) {
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
        listeners.forEach(listener -> callOnListenerThread(() -> listener.onChatReadStatusChange(chat)));
    }

    private void parseChats(@NotNull List<Node> nodes) {
        nodes.stream()
                .map(Node::attrs)
                .map(Chat::fromAttributes)
                .forEach(chats::add);
        listeners.forEach(listener -> callOnListenerThread(listener::onChats));
    }

    private void parseContacts(@NotNull List<WhatsappListener> listeners, @NotNull List<Node> nodes) {
        nodes.stream().map(Node::attrs).map(Contact::fromAttributes).forEach(contacts::add);
        listeners.forEach(listener -> callOnListenerThread(listener::onContacts));
    }

    private Chat processMessagesFromNodes(@NotNull WhatsappWebSocket socket, @NotNull List<Node> nodes) {
        var jid = firstMessageInfo(nodes).key().chatJid();
        var chat = findChatByJid(jid).orElseGet(() -> queryMissingChat(socket, jid));
        nodes.stream().map(Node::content).map(MessageInfo.class::cast).filter(Objects::nonNull).forEach(message -> processMessage(chat, message));
        return chat;
    }

    private MessageInfo firstMessageInfo(List<Node> nodes) {
        return (MessageInfo) nodes.get(0).content();
    }

    private void processMessage(@NotNull Chat chat, @NotNull MessageInfo message) {
        if (message.container().isServerMessage()) {
            // TODO: This message could also be an history sync, handle this
            findMessageById(chat, message.container().protocolMessage().key().id()).ifPresent(oldMessage -> {
                chat.messages().remove(oldMessage);
                listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageDeleted(chat, oldMessage, true)));
            });
        }

        if (chat.messages().addOrReplace(message)) {
            listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageUpdate(chat, message)));
        }

        if (initializationTimeStamp > message.timestamp()) {
            return;
        }

        updateUnreadMessages(message, chat);
        listeners.forEach(listener -> callOnListenerThread(() -> listener.onNewMessage(chat, message)));
    }

    private void updateUnreadMessages(@NotNull MessageInfo message, Chat chat) {
        if (message.key().fromMe() || (message.globalStatus() != null && message.globalStatus() == MessageInfo.MessageInfoStatus.READ) || message.ignore()) {
            return;
        }

        chat.unreadMessages(chat.unreadMessages() + 1);
    }

    private @NotNull Chat queryMissingChat(@NotNull WhatsappWebSocket socket, @NotNull String jid) {
        try {
            var chatTemp = socket.queryChat(jid).get().data();
            chats.add(chatTemp);
            listeners.forEach(listener -> callOnListenerThread(() -> listener.onNewChat(chatTemp)));
            return chatTemp;
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("WhatsappAPI: Cannot query chat to build unknown chat with jid %s".formatted(jid));
        }
    }
}
