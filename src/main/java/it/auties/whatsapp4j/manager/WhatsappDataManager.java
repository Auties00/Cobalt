package it.auties.whatsapp4j.manager;

import it.auties.whatsapp4j.model.WhatsappProtobuf.WebMessageInfo;
import it.auties.whatsapp4j.request.model.Request;
import it.auties.whatsapp4j.response.impl.json.PhoneBatteryResponse;
import it.auties.whatsapp4j.response.model.JsonResponse;
import it.auties.whatsapp4j.response.model.Response;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import it.auties.whatsapp4j.model.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Accessors(fluent = true)
public class WhatsappDataManager {
    private static final @Getter WhatsappDataManager singletonInstance = new WhatsappDataManager(Executors.newSingleThreadExecutor(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Instant.now().getEpochSecond());
    private final @NotNull ExecutorService listenerService;
    private final @NotNull List<WhatsappChat> chats;
    private final @NotNull List<WhatsappContact> contacts;
    private final @NotNull List<Request<?>> pendingRequests;
    private final @NotNull List<WhatsappListener> listeners;
    private final long initializationTimeStamp;
    private @Nullable String phoneNumber;
    private long tag;

    public @NotNull Optional<WhatsappContact> findContactByJid(@NotNull String jid) {
        return contacts.stream().filter(e -> Objects.equals(e.jid(), WhatsappUtils.parseJid(jid))).findFirst();
    }

    public @NotNull Optional<WhatsappContact> findContactByName(@NotNull String name) {
        return contacts.stream().filter(e -> Objects.equals(e.bestName().orElse(null), name)).findFirst();
    }

    public @NotNull Set<WhatsappContact> findContactsByName(@NotNull String name) {
        return contacts.stream().filter(e -> Objects.equals(e.bestName().orElse(null), name)).collect(Collectors.toUnmodifiableSet());
    }

    public @NotNull Optional<WhatsappChat> findChatByJid(@NotNull String jid) {
        return chats.stream().filter(e -> Objects.equals(e.jid(), WhatsappUtils.parseJid(jid))).findFirst();
    }

    public @NotNull Optional<WhatsappMessage> findMessageById(@NotNull WhatsappChat chat, @NotNull String id){
        return chat.messages().stream().filter(e -> Objects.equals(e.info().getKey().getId(), id)).findAny();
    }

    public @NotNull Optional<WhatsappMessage> findQuotedMessageInChatByContext(@NotNull WhatsappChat chat, @NotNull WhatsappProtobuf.ContextInfo context){
        System.out.println("Lookup for: " + context);
        return chat.messages().stream().filter(e -> context.getStanzaId().equals(e.info().getKey().getId())).findAny();
    }

    public @NotNull Optional<WhatsappChat> findChatByMessage(@NotNull WhatsappMessage message){
        return findChatByJid(message.info().getKey().getRemoteJid());
    }

    public @NotNull Optional<WhatsappChat> findChatByName(@NotNull String name) {
        return chats.stream().filter(e -> Objects.equals(e.name(), name)).findFirst();
    }

    public @NotNull Set<WhatsappChat> findChatsByName(@NotNull String name) {
        return chats.stream().filter(e -> Objects.equals(e.name(), name)).collect(Collectors.toUnmodifiableSet());
    }

    public @NotNull Optional<Request<?>> findPendingRequest(String tag){
        return pendingRequests.stream().filter(req -> req.tag().equals(tag)).findAny();
    }

    public boolean resolvePendingRequest(@NotNull String messageTag, @NotNull Response response) {
        var req = findPendingRequest(messageTag);
        if(req.isEmpty()){
            return false;
        }

        var request = req.get();
        request.complete(response);
        pendingRequests.remove(request);
        return true;
    }

    public void clear() {
        chats.clear();
        contacts.clear();
    }

    public void digestWhatsappNode(@NotNull WhatsappWebSocket socket, @NotNull WhatsappNode node) {
        var description = node.description();
        var attrs = node.attrs();
        var content = node.content();
        var duplicate = Boolean.parseBoolean(attrs.getOrDefault("duplicate", "false"));
        if (duplicate) {
            return;
        }

        if (description.equals("response")) {
            parseResponse(socket, node, content);
            return;
        }

        if (description.equals("action")) {
            parseAction(socket, node, content);
        }
    }

    private void parseAction(@NotNull WhatsappWebSocket socket, @NotNull WhatsappNode node, @Nullable Object content) {
        if(!(content instanceof List<?> listContent)){
            return;
        }

        var nodes = WhatsappNode.fromGenericList(listContent);
        if(nodes.isEmpty()){
            return;
        }

        var firstChildNode = nodes.get(0);
        switch (firstChildNode.description()){
            case "chat" -> parseChatAction(firstChildNode);
            case "user" -> parseContact(node);
            case "battery" -> parseBattery(node);
            case "read" -> parseReadStatus(firstChildNode);
            case "received" -> parseReceivedStatus(firstChildNode);
            default -> processMessages(socket, node, nodes);
        }
    }

    private void parseChatAction(@NotNull WhatsappNode node) {
        var jid = node.attrs().get("jid");
        if(jid == null){
            return;
        }

        var chat = findChatByJid(jid).orElse(null);
        if(chat == null) {
            return;
        }

        var type = node.attrs().get("type");
        if (type == null) {
            return;
        }

        switch (type){
            case "archive" -> archiveChat(chat, true);
            case "unarchive" -> archiveChat(chat, false);
            case "mute" -> muteChat(node, chat);
            case "star" -> starMessage(node, chat);
            case "unstar" -> unstarMessage(node, chat);
            case "clear" -> deleteMessage(node, chat);
            case "delete" -> chats.remove(chat);
        }
    }

    private void parseContact(@NotNull WhatsappNode node) {
        if(!(node.content() instanceof List<?> content)){
            return;
        }

        WhatsappNode.fromGenericList(content).forEach(childNode -> addOrReplaceContact(WhatsappContact.fromAttributes(childNode.attrs())));
    }

    private void addOrReplaceContact(@NotNull WhatsappContact contact) {
        if(findContactByJid(contact.jid()).isPresent()){
            contacts.remove(contact);
            contacts.add(contact);
            listeners.forEach(listener -> callOnListenerThread(() -> listener.onContactUpdate(contact)));
            return;
        }

        contacts.add(contact);
        listeners.forEach(listener -> callOnListenerThread(() -> listener.onContactReceived(contact)));
    }

    private void parseBattery(@NotNull WhatsappNode node) {
        if(!(node.content() instanceof List<?> content)){
            return;
        }

        WhatsappNode.fromGenericList(content).forEach(childNode -> listeners.forEach(listener -> callOnListenerThread(() -> listener.onPhoneBatteryStatusUpdate(new JsonResponse(childNode.attrs()).toModel(PhoneBatteryResponse.class)))));
    }

    private void muteChat(@NotNull WhatsappNode node, @NotNull WhatsappChat chat) {
        chat.mute(new WhatsappMute(Long.parseLong(node.attrs().get("mute"))));
        listeners.forEach(listener -> callOnListenerThread(() -> listener.onChatMuteChange(chat)));
    }

    private void archiveChat(@NotNull WhatsappChat chat, boolean archive) {
        chat.archived(archive);
        listeners.forEach(listener -> callOnListenerThread(() -> {
            if (archive) {
                listener.onChatArchived(chat);
                return;
            }

            listener.onChatUnarchived(chat);
        }));
    }

    private void deleteMessage(@NotNull WhatsappNode node, @NotNull WhatsappChat chat) {
        if(node.content() == null){
            chat.messages().clear();
            return;
        }

        if(!(node.content() instanceof List<?> content)){
            return;
        }

        var childNodes = WhatsappNode.fromGenericList(content);
        if(childNodes.isEmpty()){
            return;
        }

        childNodes.stream()
                .map(WhatsappNode::attrs)
                .map(entry -> entry.get("index"))
                .filter(Objects::nonNull)
                .map(id -> findMessageById(chat, id))
                .map(Optional::orElseThrow)
                .forEach(message -> {
                    chat.messages().remove(message);
                    listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageDeleted(chat, message, false)));
                });
    }

    private void unstarMessage(@NotNull WhatsappNode node, @NotNull WhatsappChat chat) {
        if(!(node.content() instanceof List<?> content)){
            return;
        }

        var childNodes = WhatsappNode.fromGenericList(content);
        if(childNodes.isEmpty()){
            return;
        }

        childNodes.stream()
                .map(WhatsappNode::attrs)
                .map(entry -> entry.get("index"))
                .filter(Objects::nonNull)
                .map(id -> findMessageById(chat, id))
                .map(Optional::orElseThrow)
                .forEach(message -> {
                    message.starred(false);
                    listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageUnstarred(chat, message)));
                });
    }

    private void starMessage(@NotNull WhatsappNode node, @NotNull WhatsappChat chat) {
        if(!(node.content() instanceof List<?> content)){
            return;
        }

        var childNodes = WhatsappNode.fromGenericList(content);
        if(childNodes.isEmpty()){
            return;
        }

        childNodes.stream()
                .map(WhatsappNode::content)
                .filter(entry -> entry instanceof WebMessageInfo)
                .map(WebMessageInfo.class::cast)
                .map(WhatsappMessage::new)
                .forEach(message -> {
                    chat.messages().addOrReplace(message);
                    listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageStarred(chat, message)));
                });
    }

    private void parseResponse(@NotNull WhatsappWebSocket socket, @NotNull WhatsappNode node, @Nullable Object content) {
        var type = node.attrs().get("type");
        if (type == null) {
            return;
        }

        if(!(content instanceof List<?> listContent)){
            return;
        }

        var nodes = WhatsappNode.fromGenericList(listContent);
        if(nodes.isEmpty()){
            return;
        }

        switch (type) {
            case "contacts" -> parseContacts(listeners, nodes);
            case "chat" -> parseChats(listeners, nodes);
            case "message" -> processMessagesFromNodes(socket, nodes);
        }
    }

    private void processMessages(@NotNull WhatsappWebSocket socket, @NotNull WhatsappNode node, List<WhatsappNode> nodes) {
        var action = node.attrs().get("add");
        if(action == null){
            return;
        }

        processMessagesFromNodes(socket, nodes);
    }

    private void parseReceivedStatus(@NotNull WhatsappNode firstChildNode) {
        var chatOpt = findChatByJid(firstChildNode.attrs().get("jid"));
        if(chatOpt.isEmpty()){
            return;
        }

        var chat = chatOpt.get();
        var messageOpt = findMessageById(chat, firstChildNode.attrs().get("index"));
        if(messageOpt.isEmpty()){
            return;
        }

        var message = messageOpt.get();
        var status = switch (firstChildNode.attrs().get("type")){
            case "read" -> WebMessageInfo.WEB_MESSAGE_INFO_STATUS.READ;
            case "message" -> WebMessageInfo.WEB_MESSAGE_INFO_STATUS.DELIVERY_ACK;
            case "error" -> WebMessageInfo.WEB_MESSAGE_INFO_STATUS.ERROR;
            default -> throw new IllegalStateException("Unexpected value");
        };

        if (status.getNumber() <= message.info().getStatus().getNumber() && status != WebMessageInfo.WEB_MESSAGE_INFO_STATUS.ERROR) {
            return;
        }

        message.status(status);
        listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageStatusUpdate(chat, message)));
    }

    private void parseReadStatus(@NotNull WhatsappNode firstChildNode) {
        var jid = firstChildNode.attrs().get("jid");
        if (jid == null) {
            return;
        }

        var type = Boolean.parseBoolean(firstChildNode.attrs().getOrDefault("type", "true"));
        var chatOpt = findChatByJid(jid);
        if(chatOpt.isEmpty()){
            return;
        }

        var chat = chatOpt.get();
        chat.unreadMessages(type ? 0 : -1);
        listeners.forEach(listener -> callOnListenerThread(() -> listener.onChatReadStatusChange(chat)));
    }

    private void parseChats(@NotNull List<WhatsappListener> listeners, @NotNull List<WhatsappNode> nodes) {
        nodes.stream()
                .map(WhatsappNode::attrs)
                .map(WhatsappChat::fromAttributes)
                .forEach(chats::add);
        listeners.forEach(listener -> callOnListenerThread(listener::onChatsReceived));
    }

    private void parseContacts(@NotNull List<WhatsappListener> listeners, @NotNull List<WhatsappNode> nodes) {
        nodes.stream()
                .map(WhatsappNode::attrs)
                .map(WhatsappContact::fromAttributes)
                .forEach(contacts::add);
        listeners.forEach(listener -> callOnListenerThread(listener::onContactsReceived));
    }

    private void processMessagesFromNodes(@NotNull WhatsappWebSocket socket, @NotNull List<WhatsappNode> nodes) {
        nodes.stream()
                .map(WhatsappNode::content)
                .map(WebMessageInfo.class::cast)
                .filter(Objects::nonNull)
                .map(WhatsappMessage::new)
                .forEach(message -> processMessage(socket, message));
    }

    private void processMessage(@NotNull WhatsappWebSocket socket, @NotNull WhatsappMessage message) {
        var jid = message.info().getKey().getRemoteJid();
        var chat = findChatByJid(jid).orElseGet(() -> queryMissingChat(socket, jid));
        if(message.info().getMessage().getProtocolMessage().getKey().hasId()){
            findMessageById(chat, message.info().getMessage().getProtocolMessage().getKey().getId()).ifPresent(oldMessage -> {
                chat.messages().remove(oldMessage);
                listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageDeleted(chat, oldMessage, true)));
            });
        }

        if(chat.messages().addOrReplace(message)){
            listeners.forEach(listener -> callOnListenerThread(() -> listener.onMessageUpdate(chat, message)));
        }

        if (initializationTimeStamp > message.info().getMessageTimestamp()) {
            return;
        }

        if(!message.sentByMe() && message.status() != WebMessageInfo.WEB_MESSAGE_INFO_STATUS.READ && !message.info().getIgnore()){
            chat.unreadMessages(chat.unreadMessages() + 1);
        }

        listeners.forEach(listener -> callOnListenerThread(() -> listener.onNewMessageReceived(chat, message)));
    }

    private @NotNull WhatsappChat queryMissingChat(@NotNull WhatsappWebSocket socket, @NotNull String jid) {
        try {
            var chatTemp = socket.queryChat(jid).get().chat().orElseThrow();
            chats.add(chatTemp);
            listeners.forEach(listener -> callOnListenerThread(() -> listener.onChatReceived(chatTemp)));
            return chatTemp;
        }catch (InterruptedException | ExecutionException ex){
            throw new RuntimeException("WhatsappAPI: Cannot query chat to build unknown chat with jid %s".formatted(jid));
        }
    }

    public long tagAndIncrement(){
        return tag++;
    }

    public @NotNull String phoneNumber(){
        return Objects.requireNonNull(phoneNumber, "WhatsappAPI: Phone number is missing");
    }
    
    public void callOnListenerThread(@NotNull Runnable runnable){
        listenerService.execute(runnable);
    }
}
