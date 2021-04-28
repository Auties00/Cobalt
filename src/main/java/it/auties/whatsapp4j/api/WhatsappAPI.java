package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.listener.MissingConstructorException;
import it.auties.whatsapp4j.listener.RegisterListenerProcessor;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.request.impl.SubscribeUserPresenceRequest;
import it.auties.whatsapp4j.request.impl.UserQueryRequest;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.response.impl.*;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import it.auties.whatsapp4j.utils.Validate;
import jakarta.validation.constraints.NotNull;
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
    private final @NotNull WhatsappWebSocket socket;
    private final @NotNull WhatsappConfiguration configuration;
    private final @Getter @NotNull WhatsappDataManager manager;

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
    public WhatsappAPI(@NotNull WhatsappConfiguration configuration) {
        this.configuration = configuration;
        this.manager = WhatsappDataManager.singletonInstance();
        this.socket = new WhatsappWebSocket(configuration);
    }

    /**
     * Returns the encryption keys linked to this object
     *
     * @return a non null instance of {@link WhatsappKeysManager}
     */
    public @NotNull WhatsappKeysManager keys() {
        return socket.whatsappKeys();
    }

    /**
     * Opens a connection with Whatsapp Web's WebSocket if a previous connection doesn't exist
     *
     * @throws IllegalStateException if a previous connection already exists
     * @return the same instance
     */
    public @NotNull WhatsappAPI connect() {
        socket.connect();
        return this;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     * @throws IllegalStateException if a previous connection doesn't exist
     * @return the same instance
     */
    public @NotNull WhatsappAPI disconnect() {
        socket.disconnect(null, false, false);
        return this;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket and logs out of WhatsappWeb invalidating the previous saved credentials
     * The next time the API is used, the QR code will need to be scanned again
     *
     * @throws IllegalStateException if a previous connection doesn't exist
     * @return the same instance
     */
    public @NotNull WhatsappAPI logout() {
        socket.disconnect(null, true, false);
        return this;
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     * @throws IllegalStateException if a previous connection doesn't exist
     * @return the same instance
     */
    public @NotNull WhatsappAPI reconnect() {
        socket.disconnect(null, false, true);
        return this;
    }

    /**
     * Registers all listeners annotated with {@code @RegisterListener} and with a no arguments constructor
     *
     * @throws IllegalArgumentException    if the listeners aren't added correctly
     * @throws MissingConstructorException if a listener doesn't provide a no arguments constructor
     * @return the same instance
     */
    public @NotNull WhatsappAPI autodetectListeners() {
        Validate.isTrue(manager.listeners().addAll(RegisterListenerProcessor.queryAllListeners()), "WhatsappAPI: Cannot autodetect listeners");
        return this;
    }

    /**
     * Registers a listener manually
     *
     * @param listener the listener to register
     * @throws IllegalArgumentException if the {@code listener} cannot be added
     * @return the same instance
     */
    public @NotNull WhatsappAPI registerListener(@NotNull WhatsappListener listener) {
        Validate.isTrue(manager.listeners().add(listener), "WhatsappAPI: Cannot add listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Removes a listener manually
     *
     * @param listener the listener to remove
     * @throws IllegalArgumentException if the {@code listener} cannot be added
     * @return the same instance
     */
    public @NotNull WhatsappAPI removeListener(@NotNull WhatsappListener listener) {
        Validate.isTrue(manager.listeners().remove(listener), "WhatsappAPI: Cannot remove listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the time the contact was last seen.
     * To listen to these updates implement {@link WhatsappListener#onContactPresenceUpdate(WhatsappChat, WhatsappContact)}.
     *
     * @param contact the contact whose status the api should receive updates on
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> subscribeToContactPresence(@NotNull WhatsappContact contact) {
        return new SubscribeUserPresenceRequest<SimpleStatusResponse>(configuration, contact.jid()) {}.send(socket.session());
    }

    /**
     * Sends a text message to a chat
     *
     * @param request the message request, specifies the type of message to send, the recipient of the message and the metadata of the message
     * @return a CompletableFuture that resolves in a MessageResponse wrapping the status of the message request and, if the status == 200, the time in seconds the message was registered on the server
     */
    public @NotNull CompletableFuture<MessageResponse> sendMessage(@NotNull WhatsappUserMessage request) {
        return sendMessage(request.info());
    }

    /**
     * Sends a message to a chat
     *
     * @param message the raw Protobuf message to send
     * @return a CompletableFuture that resolves in a MessageResponse wrapping the status of the message request and, if the status == 200, the time in seconds the message was registered on the server
     */
    public @NotNull CompletableFuture<MessageResponse> sendMessage(@NotNull WhatsappProtobuf.WebMessageInfo message) {
        var node = new WhatsappNode("action", attributes(attr("type", "relay"), attr("epoch", manager.tagAndIncrement())), List.of(new WhatsappNode("message", attributes(), message)));
        return new BinaryRequest<MessageResponse>(configuration, message.getKey().getId(), node, BinaryFlag.IGNORE, BinaryMetric.MESSAGE) {}.send(socket.session());
    }

    /**
     * Queries the written whatsapp status of a WhatsappContact
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a UserStatusResponse wrapping the text status of the target contact if the request was successful
     */
    public @NotNull CompletableFuture<UserStatusResponse> queryUserStatus(@NotNull WhatsappContact contact) {
        return new UserQueryRequest<UserStatusResponse>(configuration, contact.jid(), UserQueryRequest.QueryType.USER_STATUS) {}.send(socket.session());
    }

    /**
     * Queries the profile picture of a WhatsappChat
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a ChatPictureResponse wrapping the status of the request and, if the status == 200, a link to the requested picture
     */
    public @NotNull CompletableFuture<ChatPictureResponse> queryChatPicture(@NotNull WhatsappChat chat) {
        return new UserQueryRequest<ChatPictureResponse>(configuration, chat.jid(), UserQueryRequest.QueryType.CHAT_PICTURE) {}.send(socket.session());
    }

    /**
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a GroupMetadataResponse wrapping the status of the request and, if the status == 200, the requested data
     */
    public @NotNull CompletableFuture<GroupMetadataResponse> queryGroupMetadata(@NotNull WhatsappChat chat) {
        Validate.isTrue(chat.isGroup(), "WhatsappAPI: Cannot query metadata for %s as it's not a group", chat.jid());
        return new UserQueryRequest<GroupMetadataResponse>(configuration, chat.jid(), UserQueryRequest.QueryType.GROUP_METADATA) {}.send(socket.session());
    }

    /**
     * Queries the groups in common with a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a CommonGroupsResponse wrapping the status of the request and, if the status == 200, a list of groups in common with the specified contact
     */
    public @NotNull CompletableFuture<CommonGroupsResponse> queryGroupsInCommon(@NotNull WhatsappContact contact) {
        return new UserQueryRequest<CommonGroupsResponse>(configuration, contact.jid(), UserQueryRequest.QueryType.GROUPS_IN_COMMON) {}.send(socket.session());
    }

    /**
     * Queries a chat that is not in memory associated with a contact.
     * This method does not add said chat to {@link WhatsappDataManager#chats()} automatically.
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a ChatResponse wrapping the chat that was requested
     */
    public @NotNull CompletableFuture<ChatResponse> queryChat(@NotNull WhatsappContact contact) {
        return queryChat(contact.jid());
    }

    /**
     * Queries a chat that is not in memory associated by its jid.
     * This method does not add said chat to {@link WhatsappDataManager#chats()} automatically.
     *
     * @param jid the target jid
     * @return a CompletableFuture that resolves in a ChatResponse wrapping the chat that was requested
     */
    public @NotNull CompletableFuture<ChatResponse> queryChat(@NotNull String jid) {
        return socket.queryChat(jid);
    }

    /**
     * Queries a specified amount of starred/favourite messages in a chat, including ones not in memory
     *
     * @param chat  the target chat
     * @param count the amount of messages
     * @return a CompletableFuture that resolves in a MessagesResponse wrapping the requested messages
     */
    public @NotNull CompletableFuture<MessagesResponse> queryFavouriteMessagesInChat(@NotNull WhatsappChat chat, int count) {
        var node = new WhatsappNode("query", attributes(attr("chat", chat.jid()), attr("count", count), attr("epoch", manager.tagAndIncrement()), attr("type", "star")), null);
        return new BinaryRequest<MessagesResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}.send(socket.session());
    }

    /**
     * Loads in memory twenty messages before the last message in memory for a chat in chronological terms
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in said chat, using {@code chat} is the same thing
     */
    public @NotNull CompletableFuture<WhatsappChat> loadConversation(@NotNull WhatsappChat chat) {
        return loadConversation(chat, 20);
    }

    /**
     * Loads in memory a provided number of messages before the last message in memory for a chat in chronological terms
     *
     * @param chat the target chat
     * @param messageCount the number of messages to load
     * @return a CompletableFuture that resolves in said chat, using {@code chat} is the same thing
     */
    public @NotNull CompletableFuture<WhatsappChat> loadConversation(@NotNull WhatsappChat chat, int messageCount) {
        return chat.firstUserMessage()
                .map(userMessage -> loadConversation(chat, userMessage, messageCount))
                .orElseGet(() -> queryChat(chat.jid()).thenApplyAsync(res -> {
                    chat.messages().addAll(res.data().orElseThrow().messages());
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
    public @NotNull CompletableFuture<WhatsappChat> loadConversation(@NotNull WhatsappChat chat, @NotNull WhatsappUserMessage lastMessage, int messageCount) {
        var node = new WhatsappNode("query", attributes(attr("owner", lastMessage.sentByMe()), attr("index", lastMessage.id()), attr("type", "message"), attr("epoch", manager.tagAndIncrement()), attr("jid", chat.jid()), attr("kind", "before"), attr("count", messageCount)), null);
        return new BinaryRequest<MessagesResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}
                .send(socket.session())
                .thenApplyAsync(res -> {
                    chat.messages().addAll(res.data().orElseThrow());
                    return chat;
                });
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param presence the new status
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changePresence(@NotNull WhatsappContactStatus presence) {
        var node = new WhatsappNode("action", attributes(attr("type", "set"), attr("epoch", manager.tagAndIncrement())), List.of(new WhatsappNode("presence", attributes(attr("type", presence.data())), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, presence.flag(), BinaryMetric.PRESENCE) {}
                .noResponse(presence != WhatsappContactStatus.AVAILABLE)
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
    public @NotNull CompletableFuture<SimpleStatusResponse> changePresence(@NotNull WhatsappChat chat, @NotNull WhatsappContactStatus presence) {
        var node = new WhatsappNode("action", attributes(attr("type", "set"), attr("epoch", manager.tagAndIncrement())), List.of(new WhatsappNode("presence", attributes(attr("type", presence.data()), attr("to", chat.jid())), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, presence.flag(), BinaryMetric.PRESENCE) {}
                .noResponse(presence != WhatsappContactStatus.AVAILABLE)
                .send(socket.session())
                .thenApplyAsync(res -> Optional.ofNullable(res).orElse(new SimpleStatusResponse(200)));
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     */
    public @NotNull CompletableFuture<GroupModificationResponse> promote(@NotNull WhatsappChat group, @NotNull WhatsappContact... contacts) {
        return executeActionOnGroupParticipant(group, WhatsappGroupAction.PROMOTE, jidsToParticipantNodes(contacts));
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     */
    public @NotNull CompletableFuture<GroupModificationResponse> demote(@NotNull WhatsappChat group, @NotNull WhatsappContact... contacts) {
        return executeActionOnGroupParticipant(group, WhatsappGroupAction.DEMOTE, jidsToParticipantNodes(contacts));
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     */
    public @NotNull CompletableFuture<GroupModificationResponse> add(@NotNull WhatsappChat group, @NotNull WhatsappContact... contacts) {
        return executeActionOnGroupParticipant(group, WhatsappGroupAction.ADD, jidsToParticipantNodes(contacts));
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     */
    public @NotNull CompletableFuture<GroupModificationResponse> remove(@NotNull WhatsappChat group, @NotNull WhatsappContact... contacts) {
        return executeActionOnGroupParticipant(group, WhatsappGroupAction.REMOVE, jidsToParticipantNodes(contacts));
    }

    /**
     * Executes an action on any number of contacts represented by a raw list of WhatsappNodes
     *
     * @param group  the target group
     * @param action the action to execute
     * @param jids   the raw WhatsappNodes representing the contacts the action should be executed on
     * @throws IllegalArgumentException if the provided chat is not a group
     * @throws IllegalArgumentException if no jids are provided
     * @return a CompletableFuture that resolves in a GroupModificationResponse wrapping the status of the request and the status for the action executed on each contact
     */
    public @NotNull CompletableFuture<GroupModificationResponse> executeActionOnGroupParticipant(@NotNull WhatsappChat group, @NotNull WhatsappGroupAction action, @NotNull List<WhatsappNode> jids) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot execute action on group's participant, %s is not a group", group.jid());
        Validate.isTrue(!jids.isEmpty(), "WhatsappAPI: Cannot execute action on group's participant, expected at least one participant node");

        var tag = buildRequestTag(configuration);
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("group", attributes(attr("jid", group.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", action.data())), jids)));
        return new BinaryRequest<GroupModificationResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Changes the name of a group
     *
     * @param group   the target group
     * @param newName the new name for the group
     * @throws IllegalArgumentException if the provided chat is not a group
     * @throws IllegalArgumentException if the provided new name is empty or blank
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupName(@NotNull WhatsappChat group, @NotNull String newName) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's name: %s is not a group", group.jid());
        Validate.isTrue(!newName.isBlank(), "WhatsappAPI: Cannot change group's name: the new name cannot be empty or blank");

        var tag = buildRequestTag(configuration);
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("group", attributes(attr("jid", group.jid()), attr("subject", newName), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "subject")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }
    
    /**
     * Changes the description of a group
     *
     * @param group          the target group
     * @param newDescription the new name for the group
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupDescription(@NotNull WhatsappChat group, @NotNull String newDescription) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's description: %s is not a group", group.jid());
       
        return queryGroupMetadata(group)
                .thenApplyAsync(GroupMetadataResponse::descriptionMessageId)
                .thenComposeAsync(previousId -> {
                    var tag = buildRequestTag(configuration);
                    var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("group", attributes(attr("jid", group.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "description")), List.of(new WhatsappNode("description", attributes(attr("id", randomId()), attr("prev", Objects.requireNonNullElse(previousId, "none"))), newDescription)))));
                    return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
                });
    }

    /**
     * Changes which category of users can send messages in a group
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeWhoCanSendMessagesInGroup(@NotNull WhatsappChat group, @NotNull WhatsappGroupPolicy policy) {
        return changeGroupSetting(group, WhatsappGroupSetting.SEND_MESSAGES, policy);
    }

    /**
     * Changes which category of users can edit the group's settings
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeWhoCanEditGroupInfo(@NotNull WhatsappChat group, @NotNull WhatsappGroupPolicy policy) {
        return changeGroupSetting(group, WhatsappGroupSetting.EDIT_GROUP_INFO, policy);
    }

    /**
     * Enforces a new policy for a setting in a group
     *
     * @param group   the target group
     * @param setting the target setting
     * @param policy  the new policy to enforce
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupSetting(@NotNull WhatsappChat group, @NotNull WhatsappGroupSetting setting, @NotNull WhatsappGroupPolicy policy) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's setting: %s is not a group", group.jid());
        var tag = buildRequestTag(configuration);
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("group", attributes(attr("jid", group.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "prop")), List.of(new WhatsappNode(setting.data(), attributes(attr("value", policy.data())), null)))));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Changes the picture of a group
     * This is still in beta
     *
     * @param group the target group
     * @param image the new image
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupPicture(@NotNull WhatsappChat group, byte @NotNull [] image) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's picture: %s is not a group", group.jid());
        var tag = buildRequestTag(configuration);
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("picture", attributes(attr("jid", group.jid()), attr("id", tag), attr("type", "set")), List.of(new WhatsappNode("image", attributes(), image)))));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.PICTURE) {}.send(socket.session());
    }

    /**
     * Removes the picture of a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> removeGroupPicture(@NotNull WhatsappChat group) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot remove group's picture: %s is not a group", group.jid());
        var tag = buildRequestTag(configuration);
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("picture", attributes(attr("jid", group.jid()), attr("id", tag), attr("type", "delete")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.PICTURE) {}.send(socket.session());
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> leave(@NotNull WhatsappChat group) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot leave group: %s is not a group", group.jid());
        var tag = buildRequestTag(configuration);
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("group", attributes(attr("jid", group.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "leave")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Mutes a chat indefinitely
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappChat chat) {
        return mute(chat, -1);
    }

    /**
     * Mutes a chat until a specific date
     *
     * @param chat  the target chat
     * @param until the date the mute ends
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappChat chat, @NotNull ZonedDateTime until) {
        return mute(chat, until.toEpochSecond());
    }

    /**
     * Mutes a chat until a specific date expressed in seconds since the epoch
     *
     * @param chat           the target chat
     * @param untilInSeconds the date the mute ends expressed in seconds since the epoch
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappChat chat, long untilInSeconds) {
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("chat", attributes(attr("jid", chat.jid()), attr("mute", untilInSeconds), attr("type", "mute")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session())
                .thenApplyAsync(res -> {
                    if(res.status() == 200) chat.mute(new WhatsappMute(untilInSeconds));
                    return res;
                });
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> unmute(@NotNull WhatsappChat chat) {
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("chat", attributes(attr("jid", chat.jid()), attr("previous", chat.mute().muteEndDate().map(ChronoZonedDateTime::toEpochSecond).map(String::valueOf).orElseThrow()), attr("type", "mute")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session())
                .thenApplyAsync(res -> {
                    if(res.status() == 200) chat.mute(new WhatsappMute(0));
                    return res;
                });
    }

    /**
     * Blocks a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> block(@NotNull WhatsappContact contact) {
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("block", attributes(attr("jid", contact.jid())), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.BLOCK) {}.send(socket.session());
    }

    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled in said chat after a week
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> enableEphemeralMessages(@NotNull WhatsappChat chat) {
        return changeEphemeralStatus(chat, 604800);
    }

    /**
     * Disables ephemeral messages in a chat, this means that messages sent in said chat will never be cancelled
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> disableEphemeralMessages(@NotNull WhatsappChat chat) {
        return changeEphemeralStatus(chat, 0);
    }

    /**
     * Changes the ephemeral status of a chat, this means that messages will be automatically cancelled in said chat after the provided time
     *
     * @param chat the target chat
     * @param time the time to live for a message expressed in seconds
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeEphemeralStatus(@NotNull WhatsappChat chat, int time) {
        var tag = buildRequestTag(configuration);
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("group", attributes(attr("jid", chat.jid()), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "prop")), List.of(new WhatsappNode("ephemeral", attributes(attr("value", time)), null)))));
        return new BinaryRequest<SimpleStatusResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session());
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> markAsUnread(@NotNull WhatsappChat chat) {
        return markChat(chat, -2);
    }

    /**
     * Marks a chat as read
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> markAsRead(@NotNull WhatsappChat chat) {
        return markChat(chat, -1);
    }

    /**
     * Marks a chat with a flag represented by an integer
     *
     * @param chat the target chat
     * @param flag the flag represented by an int
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> markChat(@NotNull WhatsappChat chat, int flag) {
        var lastMessage = chat.lastMessage().orElseThrow();
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("read", attributes(attr("owner", lastMessage.sentByMe()), attr("jid", chat.jid()), attr("count", flag), attr("index", lastMessage.id())), null)));
        System.out.println(node);
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.READ) {}.send(socket.session());
    }

    /**
     * Pins a chat to the top
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> pin(@NotNull WhatsappChat chat) {
        var now = ZonedDateTime.now().toEpochSecond();
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("chat", attributes(attr("jid", chat.jid()), attr("pin", String.valueOf(now)), attr("type", "pin")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session())
                .thenApplyAsync(res -> {
                    if(res.status() == 200) chat.pinned(now);
                    return res;
                });
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> unpin(@NotNull WhatsappChat chat) {
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("chat", attributes(attr("jid", chat.jid()), attr("previous", chat.pinned().map(ChronoZonedDateTime::toEpochSecond).map(String::valueOf).orElse("")), attr("type", "pin")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session())
                .thenApplyAsync(res -> {
                    if(res.status() == 200) chat.pinned(0);
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
    public @NotNull CompletableFuture<SimpleStatusResponse> archive(@NotNull WhatsappChat chat) {
        var lastMessage = chat.lastMessage().orElseThrow();
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("chat", attributes(attr("owner", lastMessage.sentByMe()), attr("jid", chat.jid()), attr("index", lastMessage.id()), attr("type", "archive")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session())
                .thenApplyAsync(res -> {
                    if(res.status() == 200){
                        chat.pinned(0);
                        chat.isArchived(true);
                    }

                    return res;
                });
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> unarchive(@NotNull WhatsappChat chat) {
        var lastMessage = chat.lastMessage().orElseThrow();
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("chat", attributes(attr("owner", lastMessage.sentByMe()), attr("jid", chat.jid()), attr("index", lastMessage.id()), attr("type", "unarchive")), null)));
        return new BinaryRequest<SimpleStatusResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.CHAT) {}.send(socket.session())
                .thenApplyAsync(res -> {
                    if(res.status() == 200) chat.isArchived(false);
                    return res;
                });
    }

    /**
     * Creates a new group with the provided name and with at least one contact
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @throws IllegalArgumentException if the name is blank
     * @throws IllegalArgumentException if at least one contact isn't provided
     * @throws IllegalArgumentException if {@code contacts} contains your jid
     * @throws IllegalStateException inside the CompletableFuture if the contact cannot be created
     * @return a CompletableFuture that resolves in a SimpleStatusResponse wrapping the status of the request
     */
    public @NotNull CompletableFuture<WhatsappChat> createGroup(@NotNull String subject, @NotNull WhatsappContact... contacts) {
        Validate.isTrue(!subject.isBlank(), "WhatsappAPI: Cannot create a group with a blank name");

        var jids = jidsToParticipantNodes(contacts);
        Validate.isTrue(jids.stream().noneMatch(node -> Objects.equals(node.attrs().get("jid"), manager.phoneNumberJid())), "WhatsappAPI: Cannot create a group with name %s with yourself as a participant", subject);

        var tag = buildRequestTag(configuration);
        var node = new WhatsappNode("action", attributes(attr("epoch", manager.tagAndIncrement()), attr("type", "set")), List.of(new WhatsappNode("group", attributes(attr("subject", subject), attr("author", manager.phoneNumberJid()), attr("id", tag), attr("type", "create")), jidsToParticipantNodes(contacts))));
        return new BinaryRequest<GroupModificationResponse>(configuration, tag, node, BinaryFlag.IGNORE, BinaryMetric.GROUP) {}.send(socket.session())
                .thenApplyAsync(res -> {
                    Validate.isTrue(res.status() == 200, "WhatsappAPI: Cannot create group with name %s, error code %s", subject, res.status(), IllegalStateException.class);
                    var group = WhatsappChat.builder()
                            .timestamp(ZonedDateTime.now().toEpochSecond())
                            .jid(res.jid())
                            .mute(new WhatsappMute(0))
                            .name(subject)
                            .messages(new WhatsappMessages())
                            .presences(new HashMap<>())
                            .build();
                    manager.chats().add(group);
                    return group;
                }).thenComposeAsync(this::loadConversation);
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
    public @NotNull CompletableFuture<MessagesResponse> search(@NotNull String search, int count, int page) {
        var node = new WhatsappNode("query", attributes(attr("search", search), attr("count", count), attr("epoch", manager.tagAndIncrement()), attr("page", String.valueOf(page)), attr("type", "search")),  null);
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
    public @NotNull CompletableFuture<MessagesResponse> searchInChat(@NotNull String search, @NotNull WhatsappChat chat, int count, int page) {
        var node = new WhatsappNode("query", attributes(attr("search", search), attr("jid", chat.jid()), attr("count", count), attr("epoch", manager.tagAndIncrement()), attr("page", page), attr("type", "search")),  null);
        return new BinaryRequest<MessagesResponse>(configuration, node, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES) {}.send(socket.session());
    }
}
