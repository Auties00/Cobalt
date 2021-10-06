package it.auties.whatsapp4j.socket;

import it.auties.whatsapp4j.common.listener.IWhatsappListener;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.protobuf.chat.Chat;
import it.auties.whatsapp4j.common.protobuf.chat.ChatMute;
import it.auties.whatsapp4j.common.protobuf.contact.Contact;
import it.auties.whatsapp4j.common.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.common.protobuf.message.server.ProtocolMessage;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.response.BinaryResponseModel;
import it.auties.whatsapp4j.common.response.JsonResponse;
import it.auties.whatsapp4j.response.PhoneBatteryResponse;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a singleton and holds all the data regarding a session with WhatsappWeb's WebSocket.
 * It also provides various methods to query this data.
 * It should not be used by multiple sessions as, being a singleton, it cannot determine and divide data coming from different sessions.
 * It should not be initialized manually.
 */
@UtilityClass
public class NodeDigester {
    /**
     * Singleton Instance for WhatsappDataManager
     */
    private static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    /**
     * Digests a {@code node} adding the data it contains to the data this singleton holds
     *
     * @param socket the WebSocket associated with the WhatsappWeb's session
     * @param node   the WhatsappNode to digest
     */
    public void digestWhatsappNode(@NonNull WhatsappSocket socket, @NonNull Node node) {
        var duplicate = node.attributes().getBoolean("duplicate");
        if (duplicate) {
            return;
        }
        
        switch (node.description()) {
            case "response" -> parseResponse(socket, node);
            case "action" -> parseAction(socket, node);
        }
    }

    private void parseAction(@NonNull WhatsappSocket socket, @NonNull Node node) {
        var nodes = node.childNodes();
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
        var jid = node.attributes().getNullableString("jid");
        if (jid == null) {
            return;
        }

        var chat = MANAGER.findChatByJid(jid).orElse(null);
        if (chat == null) {
            return;
        }

        var type = node.attributes().getNullableString("type");
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
            case "delete" -> MANAGER.chats().remove(chat);
        }
    }

    private void parseContact(@NonNull Node node) {
        node.childNodes()
                .forEach(childNode -> addOrReplaceContact(Contact.fromAttributes(childNode.attributes())));
    }

    private void addOrReplaceContact(@NonNull Contact contact) {
        MANAGER.findContactByJid(contact.jid()).ifPresentOrElse(oldContact -> {
            MANAGER.contacts().remove(oldContact);
            MANAGER.contacts().add(contact);
            MANAGER.callListeners(listener -> listener.onContactUpdate(contact));
        }, () -> {
            MANAGER.contacts().add(contact);
            MANAGER.callListeners(listener -> listener.onNewContact(contact));
        });
    }

    private void parseBattery(@NonNull Node node) {
        node.childNodes()
                .forEach(childNode -> MANAGER.callListeners(listener -> parseBattery(childNode, listener)));
    }

    private void parseBattery(@NonNull Node childNode, @NonNull IWhatsappListener listener) {
        var json = JsonResponse.fromMap(childNode.attrs());
        listener.onPhoneBatteryStatusUpdate(json.toModel(PhoneBatteryResponse.class));
    }

    private void muteChat(@NonNull Node node, @NonNull Chat chat) {
        chat.mute(new ChatMute(Long.parseLong(node.attributes().getNullableString("mute"))));
        MANAGER.callListeners(listener -> listener.onChatMuteChange(chat));
    }

    private void archiveChat(@NonNull Chat chat, boolean archive) {
        chat.isArchived(archive);
        MANAGER.callListeners(listener -> archiveChat(chat, listener, archive));
    }

    private void archiveChat(@NonNull Chat chat, @NonNull IWhatsappListener listener, boolean archive) {
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

        findMessagesFromNode(chat, node.childNodes()).forEach(message -> {
            chat.messages().remove(message);
            MANAGER.callListeners(listener -> listener.onMessageDeleted(chat, message, false));
        });
    }

    private void unstarMessage(@NonNull Node node, @NonNull Chat chat) {
        findMessagesFromNode(chat, node.childNodes())
                .forEach(message -> unstarMessage(chat, message));
    }

    private @NonNull Stream<MessageInfo> findMessagesFromNode(@NonNull Chat chat, @NonNull List<Node> childNodes) {
        return childNodes.stream()
                .map(Node::attributes)
                .map(entry -> entry.getNullableString("index"))
                .filter(Objects::nonNull)
                .map(id -> MANAGER.findMessageById(chat, id))
                .map(Optional::orElseThrow);
    }

    private void unstarMessage(@NonNull Chat chat, @NonNull MessageInfo message) {
        message.starred(false);
        MANAGER.callListeners(listener -> listener.onMessageUnstarred(chat, message));
    }

    private void starMessage(@NonNull Node node, @NonNull Chat chat) {
        findMessagesFromNodes(node.childNodes())
                .forEach(message -> starMessage(chat, message));
    }

    private void starMessage(Chat chat, MessageInfo message) {
        chat.messages().addOrReplace(message);
        MANAGER.callListeners(listener -> listener.onMessageStarred(chat, message));
    }

    private @NonNull Stream<MessageInfo> findMessagesFromNodes(@NonNull List<Node> childNodes) {
        return childNodes.stream()
                .map(Node::content)
                .filter(entry -> entry instanceof MessageInfo)
                .map(entry -> (MessageInfo) entry);
    }

    private void parseResponse(@NonNull WhatsappSocket socket, @NonNull Node node) {
        var type = node.attributes().getNullableString("type");
        if (type == null) {
            return;
        }

        var nodes = node.childNodes();
        if (nodes.isEmpty()) {
            return;
        }

        switch (type) {
            case "contacts" -> parseContacts(nodes);
            case "chat" -> parseChats(nodes);
            case "message" -> processMessages(socket, node, nodes); // TODO: is this right????
        }
    }

    private void processMessages(@NonNull WhatsappSocket socket, @NonNull Node node, List<Node> nodes) {
        var action = node.attributes().getNullableString("add");
        if(action == null) {
            return;
        }

        var last = node.attributes().getBoolean("last");
        var chats = processMessagesFromNodes(socket, nodes);
        if (!action.equals("last") && !last) {
            return;
        }

        chats.forEach(NodeDigester::processMessages);
    }

    private void processMessages(@NonNull CompletableFuture<Chat> future) {
        MANAGER.callListeners(listener -> processMessages(future, listener));
    }

    private void processMessages(@NonNull CompletableFuture<Chat> future, @NonNull IWhatsappListener listener) {
        future.thenAcceptAsync(listener::onChatRecentMessages);
    }

    private void parseReceivedStatus(@NonNull Node firstChildNode) {
        var chatOpt = MANAGER.findChatByJid(firstChildNode.attributes().getNullableString("jid"));
        if (chatOpt.isEmpty()) {
            return;
        }

        var chat = chatOpt.get();
        var messageOpt = MANAGER.findMessageById(chat, firstChildNode.attributes().getNullableString("index"));
        if (messageOpt.isEmpty()) {
            return;
        }

        var message = messageOpt.get();
        var statusName = firstChildNode.attributes().getNullableString("type");
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
        MANAGER.callListeners(listener -> listener.onMessageGlobalReadStatusUpdate(chat, message));
    }

    private void parseReadStatus(@NonNull Node firstChildNode) {
        var jid = firstChildNode.attributes().getNullableString("jid");
        if (jid == null) {
            return;
        }

        var type = Boolean.parseBoolean(firstChildNode.attributes().getString("type").orElse("true"));
        var chatOpt = MANAGER.findChatByJid(jid);
        if (chatOpt.isEmpty()) {
            return;
        }

        var chat = chatOpt.get();
        chat.unreadMessages(type ? 0 : -1);
        MANAGER.callListeners(listener -> listener.onChatReadStatusChange(chat));
    }

    private void parseChats(@NonNull List<Node> nodes) {
        nodes.stream()
                .map(Node::attributes)
                .map(Chat::fromAttributes)
                .forEach(MANAGER::addChat);
        MANAGER.callListeners(IWhatsappListener::onChats);
    }

    private void parseContacts(@NonNull List<Node> nodes) {
        nodes.stream().map(Node::attributes).map(Contact::fromAttributes).forEach(MANAGER.contacts()::add);
        MANAGER.callListeners(IWhatsappListener::onContacts);
    }

    private Set<CompletableFuture<Chat>> processMessagesFromNodes(@NonNull WhatsappSocket socket, @NonNull List<Node> nodes) {
        return nodes.stream()
                .filter(node -> node.content() instanceof MessageInfo)
                .map(node -> (MessageInfo) node.content())
                .map(messageInfo -> processMessageFromNode(socket, messageInfo))
                .collect(Collectors.toUnmodifiableSet());
    }

    private @NonNull CompletableFuture<Chat> processMessageFromNode(@NonNull WhatsappSocket socket, @NonNull MessageInfo messageInfo) {
        return MANAGER.findChatByMessage(messageInfo)
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
        if (MANAGER.initializationTimeStamp() > message.timestamp()) {
            return;
        }

        updateUnreadMessages(message, chat);
        MANAGER.callListeners(listener -> listener.onNewMessage(chat, message));
    }

    private void commitMessage(@NonNull Chat chat, @NonNull MessageInfo message) {
        if (!chat.messages().addOrReplace(message)) {
            return;
        }

        MANAGER.callListeners(listener -> listener.onMessageUpdate(chat, message));
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
        MANAGER.findMessageById(chat, id).ifPresent(oldMessage -> processRevokeMessage(chat, oldMessage));
    }

    private void processRevokeMessage(@NonNull Chat chat, @NonNull MessageInfo oldMessage) {
        chat.messages().remove(oldMessage);
        MANAGER.callListeners(listener -> listener.onMessageDeleted(chat, oldMessage, true));
    }

    private void updateUnreadMessages(@NonNull MessageInfo message, Chat chat) {
        if (message.key().fromMe() || message.globalStatus() == MessageInfo.MessageInfoStatus.READ || message.ignore()) {
            return;
        }

        chat.unreadMessages(chat.unreadMessages() + 1);
    }

    private @NonNull CompletableFuture<Chat> queryMissingChat(@NonNull WhatsappSocket socket, @NonNull String jid) {
        return socket.queryChat(jid)
                .thenApplyAsync(BinaryResponseModel::data)
                .thenApplyAsync(MANAGER::addChat);
    }
}
