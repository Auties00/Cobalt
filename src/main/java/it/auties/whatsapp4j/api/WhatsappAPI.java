package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.listener.MissingConstructorException;
import it.auties.whatsapp4j.listener.RegisterListenerProcessor;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.protobuf.chat.*;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.protobuf.contact.ContactStatus;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp4j.protobuf.message.model.Message;
import it.auties.whatsapp4j.protobuf.message.model.MessageContainer;
import it.auties.whatsapp4j.protobuf.message.model.MessageKey;
import it.auties.whatsapp4j.protobuf.model.Messages;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.request.impl.SubscribeUserPresenceRequest;
import it.auties.whatsapp4j.request.impl.UserQueryRequest;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.response.impl.binary.ChatResponse;
import it.auties.whatsapp4j.response.impl.binary.MessagesResponse;
import it.auties.whatsapp4j.response.impl.json.*;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.NonNull;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static it.auties.whatsapp4j.utils.WhatsappUtils.*;

/**
 * A class used to interface a user to WhatsappWeb's WebSocket.
 * It provides various functionalities, including the possibility to query, set and modify data associated with the loaded session of whatsapp.
 * It can be configured using a default configuration or a custom one.
 * Multiple instances of this class can be initialized, though it is not advisable as {@link WhatsappDataManager} is a singleton and cannot distinguish between the data associated with each session.
 */
@Accessors(fluent = true)
public class WhatsappAPI {
    private final @NonNull WhatsappWebSocket socket;
    private final @NonNull WhatsappConfiguration configuration;
    private final @Getter @NonNull WhatsappDataManager manager;

    /**
     * Creates a new WhatsappAPI with default configuration
     */
    public WhatsappAPI() {
        this(WhatsappConfiguration.defaultOptions());
    }

    /**
     * Creates a new WhatsappAPI with the {@code configuration} provided
     *
     * @param configuration the configuration
     */
    public WhatsappAPI(@NonNull WhatsappConfiguration configuration) {
        this.configuration = configuration;
        this.manager = WhatsappDataManager.singletonInstance();
        this.socket = new WhatsappWebSocket(configuration);
    }

    /**
     * Returns the encryption keys linked to this object
     *
     * @return a non null instance of {@link WhatsappKeysManager}
     */
    public @NonNull WhatsappKeysManager keys() {
        return socket.whatsappKeys();
    }

    /**
     * Opens a connection with Whatsapp Web's WebSocket if a previous connection doesn't exist
     *
     * @return the same instance
     * @throws IllegalStateException if a previous connection already exists
     */
    public @NonNull WhatsappAPI connect() {
        socket.connect();
        return this;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance
     * @throws IllegalStateException if a previous connection doesn't exist
     */
    public @NonNull WhatsappAPI disconnect() {
        socket.disconnect(null, false, false);
        return this;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket and logs out of WhatsappWeb invalidating the previous saved credentials
     * The next time the API is used, the QR code will need to be scanned again
     *
     * @return the same instance
     * @throws IllegalStateException if a previous connection doesn't exist
     */
    public @NonNull WhatsappAPI logout() {
        socket.disconnect(null, true, false);
        return this;
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance
     * @throws IllegalStateException if a previous connection doesn't exist
     */
    public @NonNull WhatsappAPI reconnect() {
        socket.disconnect(null, false, true);
        return this;
    }

    /**
     * Registers all listeners annotated with {@code @RegisterListener} and with a no arguments constructor
     *
     * @return the same instance
     * @throws IllegalArgumentException    if the listeners aren't added correctly
     * @throws MissingConstructorException if a listener doesn't provide a no arguments constructor
     */
    public @NonNull WhatsappAPI autodetectListeners() {
        Validate.isTrue(manager.listeners().addAll(RegisterListenerProcessor.queryAllListeners()), "WhatsappAPI: Cannot autodetect listeners");
        return this;
    }

    /**
     * Registers a listener manually
     *
     * @param listener the listener to register
     * @return the same instance
     * @throws IllegalArgumentException if the {@code listener} cannot be added
     */
    public @NonNull WhatsappAPI registerListener(@NonNull WhatsappListener listener) {
        Validate.isTrue(manager.listeners().add(listener), "WhatsappAPI: Cannot add listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Removes a listener manually
     *
     * @param listener the listener to remove
     * @return the same instance
     * @throws IllegalArgumentException if the {@code listener} cannot be added
     */
    public @NonNull WhatsappAPI removeListener(@NonNull WhatsappListener listener) {
        Validate.isTrue(manager.listeners().remove(listener), "WhatsappAPI: Cannot remove listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the time the contact was last seen.
     * To listen to these updates implement {@link WhatsappListener#onContactPresenceUpdate(Chat, Contact)}.
     *
     * @param contact the contact whose status the api should receive updates on
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> subscribeToContactPresence(@NonNull Contact contact) {
        return new SubscribeUserPresenceRequest<SimpleStatusResponse>(configuration, contact.jid()) {}.send(socket.session());
    }

    /**
     * Builds and sends a {@link MessageInfo} from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture that resolves in a MessageResponse wrapping the status of the message request and, if the status == 200, the time in seconds the message was registered on the server
     */
    public @NonNull CompletableFuture<MessageResponse> sendMessage(@NonNull Chat chat, @NonNull Message message) {
        var key = new MessageKey(chat);
        var messageContainer = new MessageContainer(message);
        return sendMessage(new MessageInfo(key, messageContainer));
    }

    /**
     * Builds and sends a {@link MessageInfo} from a chat, a message and a quoted message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the message that {@code message} should quote
     * @return a CompletableFuture that resolves in a MessageResponse wrapping the status of the message request and, if the status == 200, the time in seconds the message was registered on the server
     */
    public @NonNull CompletableFuture<MessageResponse> sendMessage(@NonNull Chat chat, @NonNull ContextualMessage message, @NonNull MessageInfo quotedMessage) {
        var messageContext = Optional.ofNullable(message.contextInfo())
                .orElse(new ContextInfo(quotedMessage))
                .quotedMessageContainer(quotedMessage.container())
                .quotedMessageId(quotedMessage.key().id())
                .quotedMessageSenderJid(quotedMessage.senderJid());
        return sendMessage(chat,  message, messageContext);
    }

    /**
     * Builds and sends a {@link MessageInfo} from a chat, a message and a context
     *
     * @param chat        the chat where the message should be sent
     * @param message     the message to send
     * @param contextInfo the context of the message to send
     * @return a CompletableFuture that resolves in a MessageResponse wrapping the status of the message request and, if the status == 200, the time in seconds the message was registered on the server
     */
    public @NonNull CompletableFuture<MessageResponse> sendMessage(@NonNull Chat chat, @NonNull ContextualMessage message, @NonNull ContextInfo contextInfo) {
        var key = new MessageKey(chat);
        var messageContainer = new MessageContainer(message.contextInfo(contextInfo));
        return sendMessage(new MessageInfo(key, messageContainer));
    }

    /**
     * Sends a message info to a chat
     *
     * @param message the message to send
     * @return a CompletableFuture that resolves in a MessageResponse wrapping the status of the message request and, if the status == 200, the time in seconds the message was registered on the server
     */
    public @NonNull CompletableFuture<MessageResponse> sendMessage(@NonNull MessageInfo message) {
        var node = new Node("action", attributes(attr("type", "relay"), attr("epoch", manager.tagAndIncrement())), List.of(new Node("message", attributes(), message)));
        return new BinaryRequest<MessageResponse>(configuration, message.key().id(), node, BinaryFlag.IGNORE, BinaryMetric.MESSAGE) {}
                .send(socket.session())
                .thenApplyAsync(messageRes -> {
                    if(messageRes.status() == 200){
                        message.key().chat().ifPresent(chat -> chat.messages().add(message));
                    }

                    return messageRes;
                });
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a UserStatusResponse wrapping the text status of the target contact if the request was successful
     */
    public @NonNull CompletableFuture<UserStatusResponse> queryUserStatus(@NonNull Contact contact) {
        return new UserQueryRequest<UserStatusResponse>(configuration, contact.jid(), UserQueryRequest.QueryType.USER_STATUS) {}.send(socket.session());
    }

    /**
     * Queries the profile picture of a Chat
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a ChatPictureResponse wrapping the status of the request and, if the status == 200, a link to the requested picture
     */
    public @NonNull CompletableFuture<ChatPictureResponse> queryChatPicture(@NonNull Chat chat) {
        return new UserQueryRequest<ChatPictureResponse>(configuration, chat.jid(), UserQueryRequest.QueryType.CHAT_PICTURE) {}.send(socket.session());
    }

    /**
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @return a CompletableFuture that resolves in a GroupMetadataResponse wrapping the status of the request and, if the status == 200, the requested data
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<GroupMetadataResponse> queryGroupMetadata(@NonNull Chat chat) {
        Validate.isTrue(chat.isGroup(), "WhatsappAPI: Cannot query metadata for %s as it's not a group", chat.jid());
        return new UserQueryRequest<GroupMetadataResponse>(configuration, chat.jid(), UserQueryRequest.QueryType.GROUP_METADATA) {}.send(socket.session());
    }

    /**
     * Queries the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture that resolves in a GroupInviteCodeResponse wrapping the status of the request and, if the status == 200, the requested data
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<GroupInviteCodeResponse> queryGroupInviteCode(@NonNull Chat chat) {
        Validate.isTrue(chat.isGroup(), "WhatsappAPI: Cannot query invite code for %s as it's not a group", chat.jid());
        return new UserQueryRequest<GroupInviteCodeResponse>(configuration, chat.jid(), UserQueryRequest.QueryType.GROUP_INVITE_CODE) {}.send(socket.session());
    }

    /**
     * Queries the groups in common with a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a CommonGroupsResponse wrapping the status of the request and, if the status == 200, a list of groups in common with the specified contact
     */
    public @NonNull CompletableFuture<CommonGroupsResponse> queryGroupsInCommon(@NonNull Contact contact) {
        return new UserQueryRequest<CommonGroupsResponse>(configuration, contact.jid(), UserQueryRequest.QueryType.GROUPS_IN_COMMON) {}.send(socket.session());
    }

    /**
     * Queries a chat that is not in memory associated with a contact.
     * This method does not add said chat to {@link WhatsappDataManager#chats()} automatically.
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a ChatResponse wrapping the chat that was requested
     */
    public @NonNull CompletableFuture<ChatResponse> queryChat(@NonNull Contact contact) {
        return queryChat(contact.jid());
    }

    /**
     * Queries a chat that is not in memory associated by its jid.
     * This method does not add said chat to {@link WhatsappDataManager#chats()} automatically.
     *
     * @param jid the target jid
     * @return a CompletableFuture that resolves in a ChatResponse wrapping the chat that was requested
     */
    public @NonNull CompletableFuture<ChatResponse> queryChat(@NonNull String jid) {
        return socket.queryChat(jid);
    }

    /**
     * Queries a specified amount of starred/favourite messages in a chat, including ones not in memory
     *
     * @param chat  the target chat
     * @param count the amount of messages
     * @return a CompletableFuture that resolves in a MessagesResponse wrapping the requested messages
     */
    public @NonNull CompletableFuture<MessagesResponse> queryFavouriteMessagesInChat(@NonNull Chat chat, int count) {
        var node = new Node("query", attributes(attr("chat", chat.jid()), attr("count", count), attr("epoch", manager.tagAndIncrement()), attr("type", "star")), null);
        return new BinaryRequest<MessagesResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}.send(socket.session());
    }

    /**
     * Tries to load in chat the entire chat history for a chat.
     * This process might take several minutes for chats that contain thousands of messages.
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in said chat, using {@code chat} is the same thing
     */
    public @NonNull CompletableFuture<Chat> loadEntireChatHistory(@NonNull Chat chat) {
        var last = chat.messages().size();
        return loadChatHistory(chat).thenComposeAsync(__ -> chat.messages().isEmpty() || chat.messages().size() == last ? CompletableFuture.completedFuture(chat) : loadEntireChatHistory(chat));
    }


    /**
     * Loads in memory twenty messages before the last message in memory for a chat in chronological terms
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in said chat, using {@code chat} is the same thing
     */
    public @NonNull CompletableFuture<Chat> loadChatHistory(@NonNull Chat chat) {
        return loadChatHistory(chat, 20);
    }

    /**
     * Loads in memory a provided number of messages before the last message in memory for a chat in chronological terms
     *
     * @param chat         the target chat
     * @param messageCount the number of messages to load
     * @return a CompletableFuture that resolves in said chat, using {@code chat} is the same thing
     */
    public @NonNull CompletableFuture<Chat> loadChatHistory(@NonNull Chat chat, int messageCount) {
        return chat.firstMessage()
                .map(userMessage -> loadChatHistory(chat, userMessage, messageCount))
                .orElseGet(() -> queryChat(chat.jid()).thenApplyAsync(res -> {
                    chat.messages().addAll(res.data().messages());
                    return chat;
                }));
    }

    /**
     * Loads in memory a provided number of messages before a provided message in memory for a chat in chronological terms
     *
     * @param chat         the target chat
     * @param lastMessage  the last message that should be queried chronologically, exclusive
     * @param messageCount the amount of messages to load
     * @return a CompletableFuture that resolves in said chat, using {@code chat} is the same thing
     */
    public @NonNull CompletableFuture<Chat> loadChatHistory(@NonNull Chat chat, @NonNull MessageInfo lastMessage, int messageCount) {
        var node = new Node("query", attributes(attr("owner", lastMessage.key().fromMe()), attr("index", lastMessage.key().id()), attr("type", "message"), attr("epoch", manager.tagAndIncrement()), attr("jid", chat.jid()), attr("kind", "before"), attr("count", messageCount)), null);
        return new BinaryRequest<MessagesResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}
                .send(socket.session())
                .thenApplyAsync(res -> {
                    chat.messages().addAll(res.data());
                    return chat;
                });
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param presence the new status
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changePresence(@NonNull ContactStatus presence) {
        var node = new Node("action", attributes(attr("type", "set"), attr("epoch", manager.tagAndIncrement())), List.of(new Node("presence", attributes(attr("type", presence.data())), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, presence.flag(), BinaryMetric.PRESENCE) {}
                .noResponse(presence != ContactStatus.AVAILABLE)
                .send(socket.session())
                .thenApplyAsync(res -> Optional.ofNullable(res).orElse(new SimpleStatusResponse(200)));
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chat     the target chat
     * @param presence the new status
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changePresence(@NonNull Chat chat, @NonNull ContactStatus presence) {
        var node = new Node("action", attributes(attr("type", "set"), attr("epoch", manager.tagAndIncrement())), List.of(new Node("presence", attributes(attr("type", presence.data()), attr("to", chat.jid())), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, presence.flag(), BinaryMetric.PRESENCE) {}
                .noResponse(presence != ContactStatus.AVAILABLE)
                .send(socket.session())
                .thenApplyAsync(res -> Optional.ofNullable(res).orElse(new SimpleStatusResponse(200)));
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<GroupModificationResponse> promote(@NonNull Chat group, @NonNull Contact... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.PROMOTE, jidsToParticipantNodes(contacts));
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<GroupModificationResponse> demote(@NonNull Chat group, @NonNull Contact... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.DEMOTE, jidsToParticipantNodes(contacts));
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<GroupModificationResponse> add(@NonNull Chat group, @NonNull Contact... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.ADD, jidsToParticipantNodes(contacts));
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<GroupModificationResponse> remove(@NonNull Chat group, @NonNull Contact... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.REMOVE, jidsToParticipantNodes(contacts));
    }

    /**
     * Executes an action on any number of contacts represented by a raw list of WhatsappNodes
     *
     * @param group  the target group
     * @param action the action to execute
     * @param jids   the raw WhatsappNodes representing the contacts the action should be executed on
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     * @throws IllegalArgumentException if the provided chat is not a group
     * @throws IllegalArgumentException if no jids are provided
     */
    public @NonNull CompletableFuture<GroupModificationResponse> executeActionOnGroupParticipant(@NonNull Chat group, @NonNull GroupAction action, @NonNull List<Node> jids) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot execute action on group's participant, %s is not a group", group.jid());
        Validate.isTrue(!jids.isEmpty(), "WhatsappAPI: Cannot execute action on group's participant, expected at least one participant node");

        var tag = buildRequestTag(configuration);
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("group", attributes(attr("jid", group.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", action.data())), jids)));
        return new BinaryRequest<GroupModificationResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Changes the name of a group
     *
     * @param group   the target group
     * @param newName the new name for the group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if the provided chat is not a group
     * @throws IllegalArgumentException if the provided new name is empty or blank
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changeGroupName(@NonNull Chat group, @NonNull String newName) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's name: %s is not a group", group.jid());
        Validate.isTrue(!newName.isBlank(), "WhatsappAPI: Cannot change group's name: the new name cannot be empty or blank");

        var tag = buildRequestTag(configuration);
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("group", attributes(attr("jid", group.jid()), attr("subject", newName), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "subject")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Changes the description of a group
     *
     * @param group          the target group
     * @param newDescription the new name for the group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changeGroupDescription(@NonNull Chat group, @NonNull String newDescription) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's description: %s is not a group", group.jid());

        return queryGroupMetadata(group).thenApplyAsync(GroupMetadataResponse::descriptionMessageId).thenComposeAsync(previousId -> {
            var tag = buildRequestTag(configuration);
            var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("group", attributes(attr("jid", group.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "description")), List.of(new Node("description", attributes(attr("id", randomId()), attr("prev", Objects.requireNonNullElse(previousId, "none"))), newDescription)))));
            return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
        });
    }

    /**
     * Changes which category of users can send messages in a group
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changeWhoCanSendMessagesInGroup(@NonNull Chat group, @NonNull GroupPolicy policy) {
        return changeGroupSetting(group, GroupSetting.SEND_MESSAGES, policy);
    }

    /**
     * Changes which category of users can edit the group's settings
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changeWhoCanEditGroupInfo(@NonNull Chat group, @NonNull GroupPolicy policy) {
        return changeGroupSetting(group, GroupSetting.EDIT_GROUP_INFO, policy);
    }

    /**
     * Enforces a new policy for a setting in a group
     *
     * @param group   the target group
     * @param setting the target setting
     * @param policy  the new policy to enforce
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changeGroupSetting(@NonNull Chat group, @NonNull GroupSetting setting, @NonNull GroupPolicy policy) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's setting: %s is not a group", group.jid());
        var tag = buildRequestTag(configuration);
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("group", attributes(attr("jid", group.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "prop")), List.of(new Node(setting.data(), attributes(attr("value", policy.data())), null)))));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Changes the picture of a group
     * This is still in beta
     *
     * @param group the target group
     * @param image the new image
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changeGroupPicture(@NonNull Chat group, byte @NonNull [] image) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's picture: %s is not a group", group.jid());
        var tag = buildRequestTag(configuration);
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("picture", attributes(attr("jid", group.jid()), attr("id", tag), attr("type", "set")), List.of(new Node("image", attributes(), image)))));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.PICTURE) {}.send(socket.session());
    }

    /**
     * Removes the picture of a group
     *
     * @param group the target group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> removeGroupPicture(@NonNull Chat group) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot remove group's picture: %s is not a group", group.jid());
        var tag = buildRequestTag(configuration);
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("picture", attributes(attr("jid", group.jid()), attr("id", tag), attr("type", "delete")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.PICTURE) {}.send(socket.session());
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> leave(@NonNull Chat group) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot leave group: %s is not a group", group.jid());
        var tag = buildRequestTag(configuration);
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("group", attributes(attr("jid", group.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "leave")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Mutes a chat indefinitely
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> mute(@NonNull Chat chat) {
        return mute(chat, -1);
    }

    /**
     * Mutes a chat until a specific date
     *
     * @param chat  the target chat
     * @param until the date the mute ends
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> mute(@NonNull Chat chat, @NonNull ZonedDateTime until) {
        return mute(chat, until.toEpochSecond());
    }

    /**
     * Mutes a chat until a specific date expressed in seconds since the epoch
     *
     * @param chat           the target chat
     * @param untilInSeconds the date the mute ends expressed in seconds since the epoch
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> mute(@NonNull Chat chat, long untilInSeconds) {
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("chat", attributes(attr("jid", chat.jid()), attr("mute", untilInSeconds), attr("type", "mute")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session()).thenApplyAsync(res -> {
            if (res.status() == 200) chat.mute(new ChatMute(untilInSeconds));
            return res;
        });
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> unmute(@NonNull Chat chat) {
        var previousMute = chat.mute().muteEndDate().map(ChronoZonedDateTime::toEpochSecond).map(String::valueOf).orElse("0");
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("chat", attributes(attr("jid", chat.jid()), attr("previous", previousMute), attr("type", "mute")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session()).thenApplyAsync(res -> {
            if (res.status() == 200) chat.mute(new ChatMute(0));
            return res;
        });
    }

    /**
     * Blocks a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> block(@NonNull Contact contact) {
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("block", attributes(attr("jid", contact.jid())), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.BLOCK) {}.send(socket.session());
    }

    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled in said chat after a week
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> enableEphemeralMessages(@NonNull Chat chat) {
        return changeEphemeralStatus(chat, 604800);
    }

    /**
     * Disables ephemeral messages in a chat, this means that messages sent in said chat will never be cancelled
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> disableEphemeralMessages(@NonNull Chat chat) {
        return changeEphemeralStatus(chat, 0);
    }

    /**
     * Changes the ephemeral status of a chat, this means that messages will be automatically cancelled in said chat after the provided time
     *
     * @param chat the target chat
     * @param time the time to live for a message expressed in seconds
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changeEphemeralStatus(@NonNull Chat chat, int time) {
        var tag = buildRequestTag(configuration);
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("group", attributes(attr("jid", chat.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "prop")), List.of(new Node("ephemeral", attributes(attr("value", time)), null)))));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> markAsUnread(@NonNull Chat chat) {
        return markChat(chat, -2);
    }

    /**
     * Marks a chat as read
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> markAsRead(@NonNull Chat chat) {
        return markChat(chat, -1);
    }

    /**
     * Marks a chat with a flag represented by an integer
     *
     * @param chat the target chat
     * @param flag the flag represented by an int
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> markChat(@NonNull Chat chat, int flag) {
        return loadChatHistory(chat).thenComposeAsync(__ -> {
            var lastMessage = chat.lastMessage().orElseThrow(() -> new IllegalArgumentException("Cannot mark chat: the chat's history is empty"));
            var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("read", attributes(attr("owner", lastMessage.key().fromMe()), attr("jid", chat.jid()), attr("count", flag), attr("index", lastMessage.key().id())), null)));
            return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.READ) {}.send(socket.session());
        });
    }

    /**
     * Pins a chat to the top
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> pin(@NonNull Chat chat) {
        var now = ZonedDateTime.now().toEpochSecond();
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("chat", attributes(attr("jid", chat.jid()), attr("pin", String.valueOf(now)), attr("type", "pin")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session()).thenApplyAsync(res -> {
            if (res.status() == 200) chat.pinned(now);
            return res;
        });
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> unpin(@NonNull Chat chat) {
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("chat", attributes(attr("jid", chat.jid()), attr("previous", chat.pinned().map(ChronoZonedDateTime::toEpochSecond).map(String::valueOf).orElse("")), attr("type", "pin")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session()).thenApplyAsync(res -> {
            if (res.status() == 200) chat.pinned(0);
            return res;
        });
    }

    /**
     * Archives a chat.
     * If said chat is pinned, it will be unpinned.
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> archive(@NonNull Chat chat) {
        return loadChatHistory(chat).thenComposeAsync(__ -> {
            var lastMessage = chat.lastMessage().orElseThrow(() -> new IllegalArgumentException("Cannot archive chat: the chat's history is empty"));
            var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("chat", attributes(attr("owner", lastMessage.key().fromMe()), attr("jid", chat.jid()), attr("index", lastMessage.key().id()), attr("type", "archive")), null)));
            return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session()).thenApplyAsync(res -> {
                if (res.status() == 200) {
                    chat.pinned(0);
                    chat.isArchived(true);
                }

                return res;
            });
        });

    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> unarchive(@NonNull Chat chat) {
        return loadChatHistory(chat).thenComposeAsync(__ -> {
            var lastMessage = chat.lastMessage().orElseThrow(() -> new IllegalArgumentException("Cannot unarchive chat: the chat's history is empty"));
            var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("chat", attributes(attr("owner", lastMessage.key().fromMe()), attr("jid", chat.jid()), attr("index", lastMessage.key().id()), attr("type", "unarchive")), null)));
            return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}
                    .send(socket.session())
                    .thenApplyAsync(res -> {
                        if (res.status() == 200) chat.isArchived(false);
                        return res;
                    });
        });
    }

    /**
     * Creates a new group with the provided name and with at least one contact
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if the name is blank
     * @throws IllegalArgumentException if at least one contact isn't provided
     * @throws IllegalArgumentException if {@code contacts} contains your jid
     * @throws IllegalStateException    inside the CompletableFuture if the contact cannot be created
     */
    public @NonNull CompletableFuture<Chat> createGroup(@NonNull String subject, @NonNull Contact... contacts) {
        Validate.isTrue(!subject.isBlank(), "WhatsappAPI: Cannot create a group with a blank name");

        var jids = jidsToParticipantNodes(contacts);
        Validate.isTrue(jids.stream().noneMatch(node -> Objects.equals(node.attrs().get("jid"), manager.phoneNumberJid())), "WhatsappAPI: Cannot create a group with name %s with yourself as a participant", subject);

        var tag = buildRequestTag(configuration);
        var node = new Node("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new Node("group", attributes(attr("subject", subject), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "create")), jidsToParticipantNodes(contacts))));
        return new BinaryRequest<GroupModificationResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session()).thenApplyAsync(res -> {
            Validate.isTrue(res.status() == 200, "WhatsappAPI: Cannot create group with name %s, error code %s", subject, res.status(), IllegalStateException.class);
            var group = Chat.builder()
                    .timestamp(ZonedDateTime.now().toEpochSecond())
                    .jid(res.jid())
                    .mute(new ChatMute(0))
                    .displayName(subject)
                    .messages(new Messages())
                    .presences(new HashMap<>())
                    .build();
            manager.chats().add(group);
            return group;
        }).thenComposeAsync(this::loadChatHistory);
    }

    /**
     * Searches for a specific amount of messages globally, including data that is not in memory.
     * If there are too many result the {@code attribute} parameter should be specified in order to view the next pages.
     *
     * @param search the keyword to search
     * @param count  the number of messages to query
     * @param page   the page to query
     * @return a CompletableFuture that resolves in a MessagesResponse wrapping the requested messages
     */
    public @NonNull CompletableFuture<MessagesResponse> search(@NonNull String search, int count, int page) {
        var node = new Node("query", attributes(attr("search", search), attr("count", count), attr("epoch", manager.tagAndIncrement()), attr("page", String.valueOf(page)), attr("type", "search")), null);
        return new BinaryRequest<MessagesResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}.send(socket.session());
    }

    /**
     * Searches for a specific amount of messages in a specific chat's messages, including ones that are not in memory
     * If there are too many result the {@code attribute} parameter should be specified in order to view the next pages
     *
     * @param search the keyword to search
     * @param chat   the target chat
     * @param count  the number of messages to query
     * @param page   the page to query
     * @return a CompletableFuture that resolves in a MessagesResponse wrapping the requested messages
     */
    public @NonNull CompletableFuture<MessagesResponse> searchInChat(@NonNull String search, @NonNull Chat chat, int count, int page) {
        var node = new Node("query", attributes(attr("search", search), attr("jid", chat.jid()), attr("count", count), attr("epoch", manager.tagAndIncrement()), attr("page", page), attr("type", "search")), null);
        return new BinaryRequest<MessagesResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}.send(socket.session());
    }
}
