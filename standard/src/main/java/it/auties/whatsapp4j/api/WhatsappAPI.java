package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.common.api.AbstractWhatsappAPI;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.binary.BinaryFlag;
import it.auties.whatsapp4j.common.listener.IWhatsappListener;
import it.auties.whatsapp4j.common.listener.MissingConstructorException;
import it.auties.whatsapp4j.common.listener.RegisterListenerProcessor;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.protobuf.chat.*;
import it.auties.whatsapp4j.common.protobuf.contact.Contact;
import it.auties.whatsapp4j.common.protobuf.contact.ContactStatus;
import it.auties.whatsapp4j.common.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.common.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.common.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp4j.common.protobuf.message.model.Message;
import it.auties.whatsapp4j.common.protobuf.message.model.MessageContainer;
import it.auties.whatsapp4j.common.protobuf.message.model.MessageKey;
import it.auties.whatsapp4j.common.protobuf.message.standard.TextMessage;
import it.auties.whatsapp4j.common.protobuf.model.misc.Messages;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.utils.Validate;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.request.BinaryRequest;
import it.auties.whatsapp4j.request.SubscribeUserPresenceRequest;
import it.auties.whatsapp4j.request.UserQueryRequest;
import it.auties.whatsapp4j.response.*;
import it.auties.whatsapp4j.socket.WhatsappSocket;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static it.auties.whatsapp4j.common.utils.WhatsappUtils.*;

/**
 * A class used to interface a user to WhatsappWeb's WebSocket.
 * It provides various functionalities, including the possibility to query, set and modify data associated with the loaded session of whatsapp.
 * It can be configured using a default configuration or a custom one.
 * Multiple instances of this class can be initialized, though it is not advisable as {@link WhatsappDataManager} is a singleton and cannot distinguish between the data associated with each session.
 */
@Accessors(fluent = true)
public class WhatsappAPI extends AbstractWhatsappAPI {
    private final @NonNull WhatsappSocket socket;
    private final @NonNull WhatsappConfiguration configuration;

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
        this(configuration, new WhatsappKeysManager().withPreferences());
    }

    /**
     * Creates a new WhatsappAPI with the {@code manager} provided
     *
     * @param keys the keys manager
     */
    public WhatsappAPI(@NonNull WhatsappKeysManager keys) {
        this(WhatsappConfiguration.defaultOptions(), keys);
    }

    /**
     * Creates a new WhatsappAPI with the {@code configuration} and {@code manager} provided
     *
     * @param configuration the configuration
     * @param keys the keys manager
     */
    public WhatsappAPI(@NonNull WhatsappConfiguration configuration, @NonNull WhatsappKeysManager keys) {
        super();
        this.configuration = configuration;
        this.socket = new WhatsappSocket(configuration, keys);
    }

    /**
     * Returns the encryption keys linked to this object
     *
     * @return a non-null instance of {@link WhatsappKeysManager}
     */
    @Override
    public @NonNull WhatsappKeysManager keys() {
        return socket.keys();
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
    public @NonNull WhatsappAPI registerListener(@NonNull IWhatsappListener listener) {
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
    public @NonNull WhatsappAPI removeListener(@NonNull IWhatsappListener listener) {
        Validate.isTrue(manager.listeners().remove(listener), "WhatsappAPI: Cannot remove listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the time the contact was last seen.
     * To listen to these updates implement {@link IWhatsappListener#onContactPresenceUpdate(Chat, Contact)}.
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
    public @NonNull CompletableFuture<MessageResponse> sendMessage(@NonNull Chat chat, @NonNull String message) {
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
    public @NonNull CompletableFuture<MessageResponse> sendMessage(@NonNull Chat chat, @NonNull String message, @NonNull MessageInfo quotedMessage) {
        var messageContext = new ContextInfo(quotedMessage);
        return sendMessage(chat, new TextMessage(message), messageContext);
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
        var node = new Node("action", attributes(attribute("type", "relay"), attribute("epoch", manager.tagAndIncrement())), List.of(new Node("message", attributes(), message)));
        return new BinaryRequest<MessageResponse>(message.key().id(), node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.MESSAGE) {}
                .send(socket.session())
                .thenApplyAsync(messageRes -> {
                    if(messageRes.status() == 200){
                        message.key().chat().ifPresent(chat -> chat.messages().add(message));
                    }

                    return messageRes;
                });
    }

    /**
     * Executes a query to determine whether a Whatsapp account linked
     * to the supplied phone number exists.
     *
     * @param phoneNumber the phone number to check
     * @return a CompletableFuture that resolves in a boolean that indicates the result of the query
     */
    public @NonNull CompletableFuture<Boolean> hasWhatsapp(@NonNull String phoneNumber) {
        return new UserQueryRequest<SimpleStatusResponse>(configuration, phoneNumber, UserQueryRequest.QueryType.EXISTS) {}
                .send(socket.session())
                .thenApplyAsync(status -> status.status() == 200);
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
        var node = new Node("query", attributes(attribute("chat", chat.jid()), attribute("count", count), attribute("epoch", manager.tagAndIncrement()), attribute("type", "star")), null);
        return new BinaryRequest<MessagesResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}.send(socket.session());
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
        return loadChatHistory(chat)
                .thenComposeAsync(__ -> resolveChatLoad(chat, last));
    }

    private CompletableFuture<Chat> resolveChatLoad(Chat chat, int last) {
        return chat.messages().isEmpty() || chat.messages().size() == last ? CompletableFuture.completedFuture(chat) : loadEntireChatHistory(chat);
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
                .orElseGet(() -> loadChatHistoryFallback(chat));
    }

    private CompletableFuture<Chat> loadChatHistoryFallback(Chat chat) {
        return queryChat(chat.jid())
                .thenApplyAsync(res -> resolveChatHistory(chat, res.data().messages()));
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
        var node = new Node("query", attributes(attribute("owner", lastMessage.key().fromMe()), attribute("index", lastMessage.key().id()), attribute("type", "message"), attribute("epoch", manager.tagAndIncrement()), attribute("jid", chat.jid()), attribute("kind", "before"), attribute("count", messageCount)), null);
        return new BinaryRequest<MessagesResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}
                .send(socket.session())
                .thenApplyAsync(res -> resolveChatHistory(chat, res.data()));
    }

    private Chat resolveChatHistory(@NonNull Chat chat, Messages data) {
        chat.messages().addAll(data);
        return chat;
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param status the new status
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if {@code !(presence instanceof ContactStatus}
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changePresence(@NonNull ContactStatus status) {
        var node = new Node("action", attributes(attribute("type", "set"), attribute("epoch", manager.tagAndIncrement())), List.of(new Node("presence", attributes(attribute("type", status.data())), null)));
        return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), status.flag(), BinaryMetric.PRESENCE) {}
                .noResponse(status != ContactStatus.AVAILABLE)
                .send(socket.session())
                .thenApplyAsync(this::handlePresenceResponse);
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chat   the target chat
     * @param status the new status
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     * @throws IllegalArgumentException if {@code !(presence instanceof ContactStatus}
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> changePresence(@NonNull Chat chat, @NonNull ContactStatus status) {
        var node = new Node("action", attributes(attribute("type", "set"), attribute("epoch", manager.tagAndIncrement())), List.of(new Node("presence", attributes(attribute("type", status.data()), attribute("to", chat.jid())), null)));
        return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), status.flag(), BinaryMetric.PRESENCE) {}
                .noResponse(status != ContactStatus.AVAILABLE)
                .send(socket.session())
                .thenApplyAsync(this::handlePresenceResponse);
    }

    private SimpleStatusResponse handlePresenceResponse(SimpleStatusResponse res) {
        return Objects.requireNonNullElse(res, new SimpleStatusResponse(200));
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
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("group", attributes(attribute("jid", group.jid()), attribute("author", manager.phoneNumberJid()), attribute("id", tag), attribute("type", action.data())), jids)));
        return new BinaryRequest<GroupModificationResponse>(tag, node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
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
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("group", attributes(attribute("jid", group.jid()), attribute("subject", newName), attribute("author", manager.phoneNumberJid()), attribute("id", tag), attribute("type", "subject")), null)));
        return new BinaryRequest<SimpleStatusResponse>(tag, node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
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
            var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("group", attributes(attribute("jid", group.jid()), attribute("author", manager.phoneNumberJid()), attribute("id", tag), attribute("type", "description")), List.of(new Node("description", attributes(attribute("id", randomId()), attribute("prev", Objects.requireNonNullElse(previousId, "none"))), newDescription)))));
            return new BinaryRequest<SimpleStatusResponse>(tag, node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
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
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("group", attributes(attribute("jid", group.jid()), attribute("author", manager.phoneNumberJid()), attribute("id", tag), attribute("type", "prop")), List.of(new Node(setting.data(), attributes(attribute("value", policy.data())), null)))));
        return new BinaryRequest<SimpleStatusResponse>(tag, node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
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
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("picture", attributes(attribute("jid", group.jid()), attribute("id", tag), attribute("type", "set")), List.of(new Node("image", attributes(), image)))));
        return new BinaryRequest<SimpleStatusResponse>(tag, node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.PICTURE) {}.send(socket.session());
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
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("picture", attributes(attribute("jid", group.jid()), attribute("id", tag), attribute("type", "delete")), null)));
        return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.PICTURE) {}.send(socket.session());
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
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("group", attributes(attribute("jid", group.jid()), attribute("author", manager.phoneNumberJid()), attribute("id", tag), attribute("type", "leave")), null)));
        return new BinaryRequest<SimpleStatusResponse>(tag, node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
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
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("chat", attributes(attribute("jid", chat.jid()), attribute("mute", untilInSeconds), attribute("type", "mute")), null)));
        return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.CHAT) {}
                .send(socket.session())
                .thenApplyAsync(res -> handleMuteResponse(chat, untilInSeconds, res));
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> unmute(@NonNull Chat chat) {
        var previousMute = chat.mute().muteEndDate().map(ChronoZonedDateTime::toEpochSecond).map(String::valueOf).orElse("0");
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("chat", attributes(attribute("jid", chat.jid()), attribute("previous", previousMute), attribute("type", "mute")), null)));
        return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.CHAT) {}
                .send(socket.session())
                .thenApplyAsync(res -> handleMuteResponse(chat, 0, res));
    }

    private SimpleStatusResponse handleMuteResponse(@NonNull Chat chat, long untilInSeconds, SimpleStatusResponse res) {
        if (res.status() == 200) {
            chat.mute(new ChatMute(untilInSeconds));
        }

        return res;
    }

    /**
     * Blocks a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> block(@NonNull Contact contact) {
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("block", attributes(attribute("jid", contact.jid())), null)));
        return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.BLOCK) {}.send(socket.session());
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
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("group", attributes(attribute("jid", chat.jid()), attribute("author", manager.phoneNumberJid()), attribute("id", tag), attribute("type", "prop")), List.of(new Node("ephemeral", attributes(attribute("value", time)), null)))));
        return new BinaryRequest<SimpleStatusResponse>(tag, node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> markAsUnread(@NonNull Chat chat) {
        return markChat(chat, -2, -1);
    }

    /**
     * Marks a chat as read
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> markAsRead(@NonNull Chat chat) {
        return markChat(chat, chat.unreadMessages(), 0);
    }

    /**
     * Marks a chat with a flag represented by an integer.
     * If this chat has no history, an attempt to load the chat's history is made.
     * If no messages can be found after said attempt, the request will fail automatically.
     * If the request is successful, sets the number of unread messages to {@code newFlag}.
     *
     * @param chat    the target chat
     * @param flag    the flag represented by an int
     * @param newFlag the new flag represented by an int
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> markChat(@NonNull Chat chat, int flag, int newFlag) {
        return chat.lastMessage()
                .map(lastMessage -> markChat(chat, lastMessage, flag, newFlag))
                .orElse(loadAndMarkChat(chat, flag, newFlag));
    }

    private CompletableFuture<SimpleStatusResponse> loadAndMarkChat(@NonNull Chat chat, int flag, int newFlag) {
        return loadChatHistory(chat)
                .thenApplyAsync(Chat::lastMessage)
                .thenComposeAsync(message -> loadAndMarkChat(chat, message.orElse(null), flag, newFlag));
    }

    private CompletableFuture<SimpleStatusResponse> loadAndMarkChat(@NonNull Chat chat, MessageInfo info, int flag, int newFlag) {
        return Optional.ofNullable(info)
                .map(message -> markChat(chat, message, flag, newFlag))
                .orElse(CompletableFuture.completedFuture(new SimpleStatusResponse(404)));
    }

    /**
     * Marks a chat with a flag represented by an integer.
     * If the request is successful, sets the number of unread messages to {@code newFlag}.
     *
     * @param chat        the target chat
     * @param lastMessage the real last message in this chat
     * @param flag        the flag represented by an int
     * @param newFlag     the new flag represented by an int
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public CompletableFuture<SimpleStatusResponse> markChat(@NonNull Chat chat, @NonNull MessageInfo lastMessage, int flag, int newFlag) {
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("read", attributes(attribute("owner", lastMessage.key().fromMe()), attribute("jid", chat.jid()), attribute("count", flag), attribute("index", lastMessage.key().id())), null)));
        return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.READ) {}
                .send(socket.session())
                .thenApplyAsync(response -> handleChatMarkResponse(chat, newFlag, response));
    }

    private SimpleStatusResponse handleChatMarkResponse(Chat chat, int newFlag, SimpleStatusResponse response) {
        if (response.status() == 200) chat.unreadMessages(newFlag);
        return response;
    }

    /**
     * Pins a chat to the top.
     * A maximum of three chats can be pinned to the top.
     * This condition can be checked using {@link WhatsappDataManager#pinnedChats()}.
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> pin(@NonNull Chat chat) {
        var now = ZonedDateTime.now().toEpochSecond();
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("chat", attributes(attribute("jid", chat.jid()), attribute("pin", String.valueOf(now)), attribute("type", "pin")), null)));
        return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.CHAT) {}
                .send(socket.session())
                .thenApplyAsync(res -> handlePinResponse(chat, now, res));
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NonNull CompletableFuture<SimpleStatusResponse> unpin(@NonNull Chat chat) {
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("chat", attributes(attribute("jid", chat.jid()), attribute("previous", chat.pinned().map(ChronoZonedDateTime::toEpochSecond).map(String::valueOf).orElse("")), attribute("type", "pin")), null)));
        return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.CHAT) {}
                .send(socket.session())
                .thenApplyAsync(res -> handlePinResponse(chat, 0, res));
    }

    private SimpleStatusResponse handlePinResponse(@NonNull Chat chat, long now, SimpleStatusResponse res) {
        if (res.status() == 200) chat.pinned(now);
        return res;
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
            var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("chat", attributes(attribute("owner", lastMessage.key().fromMe()), attribute("jid", chat.jid()), attribute("index", lastMessage.key().id()), attribute("type", "archive")), null)));
            return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.CHAT) {}
                    .send(socket.session())
                    .thenApplyAsync(res -> handleArchiveResponse(chat, res, true));
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
            var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("chat", attributes(attribute("owner", lastMessage.key().fromMe()), attribute("jid", chat.jid()), attribute("index", lastMessage.key().id()), attribute("type", "unarchive")), null)));
            return new BinaryRequest<SimpleStatusResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.CHAT) {}
                    .send(socket.session())
                    .thenApplyAsync(res -> handleArchiveResponse(chat, res,false));
        });
    }

    private SimpleStatusResponse handleArchiveResponse(Chat chat, SimpleStatusResponse res, boolean archived) {
        if (res.status() == 200) {
            if(archived) chat.pinned(0);
            chat.isArchived(archived);
        }

        return res;
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
        var node = new Node("action", attributes(attribute("epoch", manager.tagAndIncrement()), attribute("type", "set")), List.of(new Node("group", attributes(attribute("subject", subject), attribute("author", manager.phoneNumberJid()), attribute("id", tag), attribute("type", "create")), jidsToParticipantNodes(contacts))));
        return new BinaryRequest<GroupModificationResponse>(tag, node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.GROUP) {}
                .send(socket.session())
                .thenApplyAsync(res -> createGroup(subject, res)).thenComposeAsync(this::loadChatHistory);
    }

    private Chat createGroup(@NonNull String subject, @NonNull GroupModificationResponse res) {
        Validate.isTrue(res.status() == 200, "WhatsappAPI: Cannot create group with name %s, error code %s", subject, res.status(), IllegalStateException.class);
        var group = Chat.builder()
                .timestamp(ZonedDateTime.now().toEpochSecond())
                .jid(res.jid())
                .mute(new ChatMute(0))
                .displayName(subject)
                .messages(new Messages())
                .presences(new HashMap<>())
                .build();
        return manager.addChat(group);
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
        var node = new Node("query", attributes(attribute("search", search), attribute("count", count), attribute("epoch", manager.tagAndIncrement()), attribute("page", String.valueOf(page)), attribute("type", "search")), null);
        return new BinaryRequest<MessagesResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}.send(socket.session());
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
        var node = new Node("query", attributes(attribute("search", search), attribute("jid", chat.jid()), attribute("count", count), attribute("epoch", manager.tagAndIncrement()), attribute("page", page), attribute("type", "search")), null);
        return new BinaryRequest<MessagesResponse>(node, configuration, keys(), BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}.send(socket.session());
    }
}
