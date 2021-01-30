package it.auties.whatsapp4j.manager;

import it.auties.whatsapp4j.constant.MuteType;
import it.auties.whatsapp4j.constant.ProtoBuf;
import it.auties.whatsapp4j.model.WhatsappListener;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Accessors(fluent = true)
public class WhatsappDataManager {
    private final @NotNull List<WhatsappChat> chats;
    private final @NotNull List<WhatsappContact> contacts;
    private final @NotNull List<WhatsappPendingMessage> pendingMessages;
    private final long initializationTimeStamp;
    private @Nullable String phoneNumber;
    private static final @NotNull @Getter WhatsappDataManager singletonInstance = new WhatsappDataManager(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Instant.now().getEpochSecond());

    public Optional<WhatsappContact> findContactByJid(@NotNull String jid) {
        return contacts.stream().filter(e -> Objects.equals(e.jid(), WhatsappUtils.parseJid(jid))).findFirst();
    }

    public Optional<WhatsappContact> findContactByName(@NotNull String name) {
        return contacts.stream().filter(e -> Objects.equals(e.name(), name)).findFirst();
    }

    public Optional<WhatsappChat> findChatByJid(@NotNull String jid) {
        return chats.stream().filter(e -> Objects.equals(e.jid(), WhatsappUtils.parseJid(jid))).findFirst();
    }

    public Optional<WhatsappMessage> findMessageById(@NotNull WhatsappChat chat, @NotNull String id){
        return chat.messages().stream().filter(e -> Objects.equals(e.info().getKey().getId(), id)).findAny();
    }

    public @NotNull Optional<WhatsappChat> findChatByMessage(@NotNull WhatsappMessage message){
        return findChatByJid(message.info().getKey().getRemoteJid());
    }

    public Optional<WhatsappChat> findChatByName(@NotNull String name) {
        return chats.stream().filter(e -> Objects.equals(e.name(), name)).findFirst();
    }

    public void addContact(@NotNull WhatsappContact contact) {
        contacts.add(contact);
    }

    public void addChat(@NotNull WhatsappChat chat) {
        chats.add(chat);
    }

    public boolean isPendingMessageId(@NotNull String id){
        return pendingMessages.stream().anyMatch(e -> Objects.equals(e.message().info().getKey().getId(), id));
    }

    public void addPendingMessage(@NotNull WhatsappPendingMessage pendingMessage){
        pendingMessages.add(pendingMessage);
    }

    public void resolvePendingMessage(@NotNull String id, int statusCode){
        var pendingMessage = pendingMessages.stream().filter(e -> Objects.equals(e.message().info().getKey().getId(), id)).findAny().orElseThrow();
        findChatByMessage(pendingMessage.message()).ifPresent(chat -> {
            chat.messages().add(pendingMessage.message());
            pendingMessage.callback().accept(pendingMessage.message(), statusCode);
            pendingMessages.remove(pendingMessage);
        });
    }

    public void clear() {
        chats.clear();
        contacts.clear();
    }

    @SuppressWarnings("unchecked")
    public void digestWhatsappNode(@NotNull WhatsappNode node, @NotNull List<WhatsappListener> listeners) {
        var duplicate = Boolean.parseBoolean(node.attrs().getOrDefault("duplicate", "false"));
        if (duplicate) {
            return;
        }

        if (node.description().equals("response")) {
            var type = node.attrs().getOrDefault("type", "");
            if (type == null) {
                return;
            }

            var nodes = (List<WhatsappNode>) node.content();
            if (nodes == null) {
                return;
            }

            switch (type) {
                case "contacts" -> {
                    nodes.stream()
                            .map(WhatsappNode::attrs)
                            .filter(attrs -> !attrs.isEmpty())
                            .map(contactAttributes -> WhatsappContact
                                    .builder()
                                    .jid(contactAttributes.get("jid"))
                                    .name(contactAttributes.get("name"))
                                    .chosenName(contactAttributes.get("notify"))
                                    .shortName(contactAttributes.get("short"))
                                    .build())
                            .forEach(this::addContact);
                    listeners.forEach(WhatsappListener::onContactsReceived);
                }

                case "chat" -> {
                    nodes.stream()
                            .map(WhatsappNode::attrs)
                            .forEach(chatAttributes -> {
                                var chatName = chatAttributes.get("name");
                                var jid = chatAttributes.get("jid");
                                var matchingContact = findContactByJid(jid);
                                var chat = WhatsappChat.builder()
                                        .jid(jid)
                                        .name(matchingContact.map(WhatsappContact::bestName).orElseGet(() -> chatName == null ? jid.split("@")[0] : chatName))
                                        .unreadMessages(Integer.parseInt(chatAttributes.get("count")))
                                        .mute(MuteType.forValue(Integer.parseInt(chatAttributes.get("mute"))))
                                        .spam(Boolean.parseBoolean(chatAttributes.get("spam")))
                                        .messages(new WhatsappMessages())
                                        .presences(new HashMap<>())
                                        .build();
                                addChat(chat);
                            });
                    listeners.forEach(WhatsappListener::onChatsReceived);
                }

                case "message" -> processMessagesFromNodes(nodes, listeners);
            }
        } else if (node.description().equals("action")) {
            var childNodes = (List<WhatsappNode>) node.content();
            if (childNodes == null || childNodes.isEmpty()) {
                return;
            }

            var firstChildNode = childNodes.get(0);
            switch (firstChildNode.description()){
                case "read" -> {
                    var jid = firstChildNode.attrs().get("jid");
                    if (jid == null) {
                        return;
                    }

                    var type = Boolean.parseBoolean(firstChildNode.attrs().getOrDefault("type", "true"));
                    var chat = findChatByJid(jid).orElseThrow();
                    chat.unreadMessages(type ? 0 : -1);
                }

                case "received" -> {
                    var chat = findChatByJid(firstChildNode.attrs().get("jid"));
                    if(chat.isEmpty()){
                        return;
                    }
                    
                    var message = findMessageById(chat.get(), firstChildNode.attrs().get("index"));
                    if(message.isEmpty()){
                        return;
                    }

                    var status = switch (firstChildNode.attrs().get("type")){
                        case "read" -> ProtoBuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.READ;
                        case "message" -> ProtoBuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.DELIVERY_ACK;
                        case "error" -> ProtoBuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.ERROR;
                        default -> throw new IllegalStateException("Unexpected value");
                    };

                    if (status.getNumber() <= message.get().info().getStatus().getNumber() && status != ProtoBuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.ERROR) {
                        return;
                    }

                    message.get().info(message.get().info().toBuilder().setStatus(status).build());
                }

                default -> {
                    var action = node.attrs().get("add");
                    if(action == null){
                        return;
                    }

                    processMessagesFromNodes(childNodes, listeners);
                }
            }
        }
    }

    private void processMessagesFromNodes(@NotNull List<WhatsappNode> nodes, @NotNull List<WhatsappListener> listeners) {
        nodes.stream()
                .map(WhatsappNode::content)
                .map(ProtoBuf.WebMessageInfo.class::cast)
                .filter(Objects::nonNull)
                .map(WhatsappMessage::new)
                .forEach(message -> {
                    var jid = message.info().getKey().getRemoteJid();
                    var chat = findChatByJid(jid).orElseGet(() -> {
                        var chatTemp = WhatsappChat.builder()
                                .jid(jid)
                                .name(jid)
                                .messages(new WhatsappMessages(message))
                                .presences(new HashMap<>())
                                .build();
                        addChat(chatTemp);
                        listeners.forEach(e -> e.onChatReceived(chatTemp));
                        return chatTemp;
                    });

                    if(message.info().hasMessage() && message.info().getMessage().hasProtocolMessage() && message.info().getMessage().getProtocolMessage().hasKey() && message.info().getMessage().getProtocolMessage().getKey().hasId()){
                        var oldMessage = findMessageById(chat, message.info().getMessage().getProtocolMessage().getKey().getId());
                        oldMessage.ifPresent(chat.messages()::remove);
                    }

                    chat.messages().remove(message);
                    chat.messages().add(message);
                    if (initializationTimeStamp > message.info().getMessageTimestamp()) {
                        return;
                    }

                    var fromMe = message.sender().isEmpty();
                    if(!fromMe && message.info().getStatus() != ProtoBuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.READ && !message.info().getIgnore()){
                        chat.unreadMessages(chat.unreadMessages() + 1);
                    }

                    listeners.forEach(listener -> listener.onNewMessageReceived(chat, message));
                });

    }
}
