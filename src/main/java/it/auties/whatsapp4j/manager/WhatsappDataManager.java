package it.auties.whatsapp4j.manager;

import it.auties.whatsapp4j.constant.ProtoBuf;
import it.auties.whatsapp4j.model.WhatsappListener;
import it.auties.whatsapp4j.model.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
@Data
@Accessors(fluent = true)
public class WhatsappDataManager {
    private static final WhatsappDataManager INSTANCE = new WhatsappDataManager(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Instant.now().getEpochSecond());
    private final @NotNull List<WhatsappChat> chats;
    private final @NotNull List<WhatsappContact> contacts;
    private final @NotNull List<WhatsappPendingMessage> pendingMessages;
    private final long initializationTimeStamp;
    private @Nullable String phoneNumber;

    public static WhatsappDataManager singletonInstance() {
        return INSTANCE;
    }

    public Optional<WhatsappContact> findContactByJid(@NotNull String jid) {
        return contacts.stream().filter(e -> Objects.equals(e.jid(), jid)).findFirst();
    }

    public Optional<WhatsappChat> findChatByJid(@NotNull String jid) {
        return chats.stream().filter(e -> Objects.equals(e.jid(), jid)).findFirst();
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
        var chat = findChatByName(pendingMessage.message().chatName()).orElseThrow();
        chat.messages().add(pendingMessage.message());
        pendingMessages.remove(pendingMessage);
        pendingMessage.callback().accept(statusCode);
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
                                        .mute(chatAttributes.get("mute"))
                                        .spam(Boolean.parseBoolean(chatAttributes.get("spam")))
                                        .messages(new WhatsappMessages())
                                        .build();
                                addChat(chat);
                            });
                    listeners.forEach(WhatsappListener::onChatsReceived);
                }

                case "message" -> processMessagesFromNodes(nodes, listeners);
            }
        } else if (node.description().equals("action")) {
            var action = node.attrs().get("add");
            if (action == null) {
                return;
            }

            var nodes = (List<WhatsappNode>) node.content();
            if (nodes == null) {
                return;
            }

            processMessagesFromNodes(nodes, listeners);
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
                    var chatOpt = findChatByJid(jid);
                    if (chatOpt.isEmpty()) {
                        var chat = WhatsappChat.builder()
                                .jid(jid)
                                .name(message.chatName())
                                .messages(new WhatsappMessages(message))
                                .build();
                        addChat(chat);
                        listeners.forEach(e -> e.onChatReceived(chat));
                        return;
                    }

                    var chat = chatOpt.get();
                    chat.messages().add(message);
                    if (initializationTimeStamp <= message.info().getMessageTimestamp()) {
                        listeners.forEach(listener -> listener.onNewMessageReceived(chat, message, message.sender(chat).isEmpty()));
                    }
                });
    }

    public @NotNull List<WhatsappChat> chats() {
        return chats;
    }

    public @NotNull List<WhatsappContact> contacts() {
        return contacts;
    }

    public long initializationTimeStamp() {
        return initializationTimeStamp;
    }
}
