package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.listener.MissingConstructorException;
import it.auties.whatsapp4j.listener.RegisterListenerProcessor;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.request.impl.NodeRequest;
import it.auties.whatsapp4j.request.impl.SubscribeUserPresenceRequest;
import it.auties.whatsapp4j.request.impl.UserQueryRequest;
import it.auties.whatsapp4j.response.impl.*;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import it.auties.whatsapp4j.utils.Validate;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.glassfish.tyrus.core.Beta;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
    private final @Getter @NotNull WhatsappKeysManager keys;

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
        this.keys = WhatsappKeysManager.fromPreferences();
        this.socket = new WhatsappWebSocket(configuration, keys);
    }

    /**
     * Opens a connection with Whatsapp Web's WebSocket if a previous connection doesn't exist
     *
     * @throws IllegalStateException if a previous connection already exists
     */
    public @NotNull WhatsappAPI connect() {
        socket.connect();
        return this;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     * @throws IllegalStateException if a previous connection doesn't exist
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
     */
    public @NotNull WhatsappAPI logout() {
        socket.disconnect(null, true, false);
        return this;
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     * @throws IllegalStateException if a previous connection doesn't exist
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
     * @throws RuntimeException            if the {@code listener} cannot be added
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
     */
    public @NotNull WhatsappAPI removeListener(@NotNull WhatsappListener listener) {
        Validate.isTrue(manager.listeners().remove(listener), "WhatsappAPI: Cannot remove listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes
     * These changes include the last known presence and the time the contact was last seen
     * To listen to these updates implement the method WhatsappListener#onMessageStatusUpdate
     *
     * @param contact the contact whose status the api should receive updates on
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> subscribeToUserPresence(@NotNull WhatsappContact contact) {
        return new SubscribeUserPresenceRequest<SimpleStatusResponse>(configuration, contact.jid()) {}.send(socket.session());
    }

    /**
     * Sends a message to a chat
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
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("type", "relay", "epoch", String.valueOf(manager.tagAndIncrement())))
                .content(List.of(new WhatsappNode("message", Map.of(), message)))
                .build();

        return new NodeRequest<MessageResponse>(message.getKey().getId(), configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.MESSAGE);
    }

    /**
     * Queries the written whatsapp status of a WhatsappContact
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a UserStatusResponse wrapping the text status of the target contact if the request is successful
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
     * @return a CompletableFuture that resolves in a GroupMetadataResponse wrapping the status of the request and, if the status == 200, the requested data
     * @throws IllegalArgumentException if the provided chat is not a group
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
     * Queries a chat that is not in memory associated with a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture that resolves in a ChatResponse wrapping the chat that was requested
     */
    public @NotNull CompletableFuture<ChatResponse> queryChat(@NotNull WhatsappContact contact) {
        return queryChat(contact.jid());
    }

    /**
     * Queries a chat that is not in memory associated by its jid
     *
     * @param jid the target jid
     * @return a CompletableFuture that resolves in a ChatResponse wrapping the chat that was requested
     */
    public @NotNull CompletableFuture<ChatResponse> queryChat(@NotNull String jid) {
        return socket.queryChat(jid);
    }


    /**
     * Queries a specified amount of starred messages in a chat, including ones not in memory
     *
     * @param chat  the target chat
     * @param count the amount of messages
     * @return a CompletableFuture that resolves in a ChatResponse wrapping the chat that was requested
     */
    public @NotNull CompletableFuture<MessagesResponse> queryFavouriteMessagesInChat(@NotNull WhatsappChat chat, int count) {
        var node = WhatsappNode.builder()
                .description("query")
                .attrs(Map.of("chat", chat.jid(), "count", String.valueOf(count), "epoch", String.valueOf(manager.tagAndIncrement()), "type", "star"))
                .content(null)
                .build();

        return new NodeRequest<MessagesResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES);
    }

    /**
     * Loads twenty messages in memory chronologically before the last message in memory for a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture that resolves in the original WhatsappChat
     */
    public @NotNull CompletableFuture<WhatsappChat> loadConversation(@NotNull WhatsappChat chat) {
        return chat.firstMessage()
                .map(userMessage -> loadConversation(chat, userMessage, 20))
                .orElseGet(() -> queryChat(chat.jid()).thenApply(res -> {
                    chat.messages().addAll(res.data().orElseThrow().messages());
                    return chat;
                }));
    }


    /**
     * Loads a specified amount of messages in memory chronologically before a specific in memory for a chat
     *
     * @param chat         the target chat
     * @param lastMessage  the last message that should be queried chronologically, exclusive
     * @param messageCount the amount of messages to load
     * @return a CompletableFuture that resolves in the original WhatsappChat
     */
    public @NotNull CompletableFuture<WhatsappChat> loadConversation(@NotNull WhatsappChat chat, @NotNull WhatsappUserMessage lastMessage, int messageCount) {
        var node = WhatsappNode.builder()
                .description("query")
                .attrs(Map.of("owner", String.valueOf(lastMessage.sentByMe()), "index", lastMessage.id(), "type", "message", "epoch", String.valueOf(manager.tagAndIncrement()), "jid", chat.jid(), "kind", "before", "count", String.valueOf(messageCount)))
                .build();

        return new NodeRequest<MessagesResponse>(configuration, node) {}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES)
                .thenApply(res -> {
                    chat.messages().addAll(res.data().orElseThrow());
                    return chat;
                });
    }

    /**
     * Changes your global presence for everyone on Whatsapp
     *
     * @param presence the new status
     */
    public @NotNull CompletableFuture<DiscardResponse> changePresence(@NotNull WhatsappContactStatus presence) {
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("type", "set", "epoch", String.valueOf(manager.tagAndIncrement())))
                .content(List.of(new WhatsappNode("presence", Map.of("type", presence.data()), null)))
                .build();

        return new NodeRequest<DiscardResponse>(configuration, node, false) {}.send(socket.session(), keys, presence.flag(), BinaryMetric.PRESENCE);
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param presence the new status
     * @param chat     the target chat
     */
    public @NotNull CompletableFuture<DiscardResponse> changePresence(@NotNull WhatsappContactStatus presence, @NotNull WhatsappChat chat) {
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("type", "set", "epoch", String.valueOf(manager.tagAndIncrement())))
                .content(List.of(new WhatsappNode("presence", Map.of("type", presence.data(), "to", chat.jid()), null)))
                .build();

        return new NodeRequest<DiscardResponse>(configuration, node, false) {}.send(socket.session(), keys, presence.flag(), BinaryMetric.PRESENCE);
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NotNull CompletableFuture<GroupModificationResponse> promote(@NotNull WhatsappChat group, @NotNull WhatsappContact... contacts) {
        return executeActionOnGroupParticipant(group, WhatsappGroupAction.PROMOTE, WhatsappUtils.jidsToParticipantNodes(contacts));
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NotNull CompletableFuture<GroupModificationResponse> demote(@NotNull WhatsappChat group, @NotNull WhatsappContact... contacts) {
        return executeActionOnGroupParticipant(group, WhatsappGroupAction.DEMOTE, WhatsappUtils.jidsToParticipantNodes(contacts));
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NotNull CompletableFuture<GroupModificationResponse> add(@NotNull WhatsappChat group, @NotNull WhatsappContact... contacts) {
        return executeActionOnGroupParticipant(group, WhatsappGroupAction.ADD, WhatsappUtils.jidsToParticipantNodes(contacts));
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NotNull CompletableFuture<GroupModificationResponse> remove(@NotNull WhatsappChat group, @NotNull WhatsappContact... contacts) {
        return executeActionOnGroupParticipant(group, WhatsappGroupAction.REMOVE, WhatsappUtils.jidsToParticipantNodes(contacts));
    }

    /**
     * Executes an action on any number of contacts represented by a raw list of WhatsappNodes
     *
     * @param group  the target group
     * @param action the action to execute
     * @param jids   the raw WhatsappNodes representing the contacts the action should be executed on
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NotNull CompletableFuture<GroupModificationResponse> executeActionOnGroupParticipant(@NotNull WhatsappChat group, @NotNull WhatsappGroupAction action, @NotNull List<WhatsappNode> jids) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's name: %s is not a group", group.jid());
        Validate.isTrue(!jids.isEmpty(), "WhatsappAPI: Cannot change group's name: expected at least one participant node");

        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", group.jid(), "author", manager.phoneNumberJid(), "id", tag, "type", action.data()), jids)))
                .build();

        return new NodeRequest<GroupModificationResponse>(tag, configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP);
    }

    /**
     * Changes the name of a group
     *
     * @param group   the target group
     * @param newName the new name for the group
     * @throws IllegalArgumentException if the provided chat is not a group
     * @throws IllegalArgumentException if the provided new name is empty or blank
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupName(@NotNull WhatsappChat group, @NotNull String newName) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's name: %s is not a group", group.jid());
        Validate.isTrue(!newName.isBlank(), "WhatsappAPI: Cannot change group's name: the new name cannot be empty or blank");

        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", group.jid(), "subject", newName, "author", manager.phoneNumberJid(), "id", tag, "type", "subject"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP);
    }


    /**
     * Changes the description of a group
     *
     * @param group          the target group
     * @param newDescription the new name for the group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupDescription(@NotNull WhatsappChat group, @NotNull String newDescription) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's description: %s is not a group", group.jid());

        return queryGroupMetadata(group)
                .thenApplyAsync(GroupMetadataResponse::descriptionMessageId)
                .thenComposeAsync(previousId -> {
                    var tag = WhatsappUtils.buildRequestTag(configuration);
                    var node = WhatsappNode.builder()
                            .description("action")
                            .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                            .content(List.of(new WhatsappNode("group", Map.of("jid", group.jid(), "author", manager.phoneNumberJid(), "id", tag, "type", "description"), List.of(new WhatsappNode("description", Map.of("id", WhatsappUtils.randomId(), "prev", Objects.requireNonNullElse(previousId, "none")), newDescription)))))
                            .build();

                    return new NodeRequest<SimpleStatusResponse>(tag, configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP);
                });
    }

    /**
     * Changes which category of users can send messages in a group
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @throws IllegalArgumentException if the provided chat is not a group
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
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupSetting(@NotNull WhatsappChat group, @NotNull WhatsappGroupSetting setting, @NotNull WhatsappGroupPolicy policy) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's setting: %s is not a group", group.jid());
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", group.jid(), "author", manager.phoneNumberJid(), "id", tag, "type", "prop"), List.of(new WhatsappNode(setting.data(), Map.of("value", policy.data()), null)))))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP);
    }

    /**
     * Changes the picture of a group
     * This is still in beta
     *
     * @param group the target group
     * @param image the new image
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    @Beta
    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupPicture(@NotNull WhatsappChat group, byte @NotNull [] image) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot change group's picture: %s is not a group", group.jid());

        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("picture", Map.of("jid", group.jid(), "id", tag, "type", "set"), List.of(new WhatsappNode("image", Map.of(), "ASAS")))))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.PICTURE);
    }

    /**
     * Removes the picture of a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> removeGroupPicture(@NotNull WhatsappChat group) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot remove group's picture: %s is not a group", group.jid());
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("picture", Map.of("jid", group.jid(), "id", tag, "type", "delete"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.PICTURE);
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> leave(@NotNull WhatsappChat group) {
        Validate.isTrue(group.isGroup(), "WhatsappAPI: Cannot leave group: %s is not a group", group.jid());
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", group.jid(), "author", manager.phoneNumberJid(), "id", tag, "type", "leave"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP);
    }

    /**
     * Mute a chat indefinitely
     *
     * @param chat the target chat
     * @throws IllegalStateException if the chat is already muted
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappChat chat) {
        return mute(chat, -1);
    }

    /**
     * Mute a chat until a specific date
     *
     * @param chat  the target chat
     * @param until the date the mute ends
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappChat chat, @NotNull ZonedDateTime until) {
        return mute(chat, until.toEpochSecond());
    }

    /**
     * Mutes a chat until a specific date expressed in seconds since the epoch
     *
     * @param chat           the target chat
     * @param untilInSeconds the date the mute ends expressed in seconds since the epoch
     * @throws IllegalStateException if the chat is already muted
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappChat chat, long untilInSeconds) {
        Validate.isTrue(!chat.mute().isMuted(), "WhatsappAPI: Cannot mute chat with jid %s: chat is already muted", IllegalStateException.class, chat.jid());
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("jid", chat.jid(), "mute", String.valueOf(untilInSeconds), "type", "mute"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT);
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @throws IllegalStateException if the chat is not muted
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> unmute(@NotNull WhatsappChat chat) {
        Validate.isTrue(chat.mute().isMuted(), "WhatsappAPI: Cannot unmute chat with jid %s: chat is not muted", chat.jid());

        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("jid", chat.jid(), "previous", chat.mute().muteEndDate().map(ChronoZonedDateTime::toEpochSecond).map(String::valueOf).orElseThrow(), "type", "mute"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT);
    }

    /**
     * Blocks a contact
     *
     * @param contact the target contact
     * @throws IllegalStateException if the contact is already muted
     */
    @Beta
    public @NotNull CompletableFuture<SimpleStatusResponse> block(@NotNull WhatsappContact contact) {
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("block", Map.of("jid", contact.jid()), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.BLOCK);
    }

    /**
     * Enables ephemeral messages, this means that messages will be automatically cancelled in the specified chat after one week
     *
     * @param chat the target chat
     * @throws IllegalStateException if ephemereal messages are already enabled
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> enableEphemeralMessages(@NotNull WhatsappChat chat) {
        Validate.isTrue(!chat.isEphemeralChat(), "WhatsappAPI: Cannot enable ephemeral messages for chat with jid %s: ephemeral messages are already enabled", IllegalStateException.class, chat.jid());
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", chat.jid(), "author", manager.phoneNumberJid(), "id", tag, "type", "prop"), List.of(new WhatsappNode("ephemeral", Map.of("value", "604800"), null)))))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP);
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> markAsUnread(@NotNull WhatsappChat chat) {
        return markChat(chat, -2);
    }

    /**
     * Marks a chat as read
     *
     * @param chat the target chat
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> markAsRead(@NotNull WhatsappChat chat) {
        return markChat(chat, -1);
    }

    /**
     * Marks a chat with a flag represented by an integer
     *
     * @param chat the target chat
     * @param flag the flag represented by an int
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> markChat(@NotNull WhatsappChat chat, int flag) {
        var lastMessage = chat.lastMessage().orElseThrow();
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("read", Map.of("owner", String.valueOf(lastMessage.sentByMe()), "jid", chat.jid(), "count", String.valueOf(flag), "index", lastMessage.info().getKey().getId()), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.READ);
    }

    /**
     * Pins a chat to the top
     *
     * @param chat the target chat
     * @throws IllegalStateException if the chat is already pinned
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> pin(@NotNull WhatsappChat chat) {
        Validate.isTrue(!chat.isPinned(), "WhatsappAPI: Cannot pin chat with jid %s as it's already pinned", IllegalStateException.class, chat.jid());
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("jid", chat.jid(), "pin", String.valueOf(ZonedDateTime.now().toEpochSecond()), "type", "pin"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT);
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @throws IllegalStateException if the chat is not pinned
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> unpin(@NotNull WhatsappChat chat) {
        var lastPin = chat.pinned().map(ChronoZonedDateTime::toEpochSecond).map(String::valueOf);
        Validate.isTrue(lastPin.isPresent(), "WhatsappAPI: Cannot unpin chat with jid %s as it's not pinned", IllegalStateException.class, chat.jid());

        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("jid", chat.jid(), "previous", lastPin.get(), "type", "pin"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT);
    }

    /**
     * Archives a chat
     *
     * @param chat the target chat
     * @throws IllegalStateException if the chat is already archived
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> archive(@NotNull WhatsappChat chat) {
        Validate.isTrue(!chat.isArchived(), "WhatsappAPI: Cannot archive chat with jid %s as it's already archived", IllegalStateException.class, chat.jid());

        var lastMessage = chat.lastMessage().orElseThrow();
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("owner", String.valueOf(lastMessage.sentByMe()), "jid", chat.jid(), "index", lastMessage.info().getKey().getId(), "type", "archive"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT);
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     * @throws IllegalStateException if the chat is not archived
     */
    public @NotNull CompletableFuture<SimpleStatusResponse> unarchive(@NotNull WhatsappChat chat) {
        Validate.isTrue(chat.isArchived(), "WhatsappAPI: Cannot unarchive chat with jid %s as it's not archived", IllegalStateException.class, chat.jid());
        var lastMessage = chat.lastMessage().orElseThrow();
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("owner", String.valueOf(lastMessage.sentByMe()), "jid", chat.jid(), "index", lastMessage.info().getKey().getId(), "type", "unarchive"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT);
    }

    /**
     * Creates a new group with the provided name and with at least one contact
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @throws IllegalArgumentException if the name is blank
     * @throws IllegalArgumentException if at least one contact isn't provided
     */
    public @NotNull CompletableFuture<GroupModificationResponse> createGroup(@NotNull String subject, @NotNull WhatsappContact... contacts) {
        Validate.isTrue(!subject.isBlank(), "WhatsappAPI: Cannot create a group with a blank name");
        Validate.isTrue(contacts.length > 0, "WhatsappAPI: Cannot create a group with name %s with no participants", subject);

        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNode.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("subject", subject, "author", manager.phoneNumberJid(), "id", tag, "type", "create"), WhatsappUtils.jidsToParticipantNodes(contacts))))
                .build();

        return new NodeRequest<GroupModificationResponse>(tag, configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP);
    }

    /**
     * Searches for a specific amount of messages globally, including data that is not in memory
     * If there are too many result the {@code attribute} parameter should be specified in order to view the next pages
     *
     * @param search the keyword to search
     * @param count  the number of messages to query
     * @param page   the page to query
     */
    public @NotNull CompletableFuture<MessagesResponse> search(@NotNull String search, int count, int page) {
        var node = WhatsappNode.builder()
                .description("query")
                .attrs(Map.of("search", search, "count", String.valueOf(count), "epoch", String.valueOf(manager.tagAndIncrement()), "page", String.valueOf(page), "type", "search"))
                .content(null)
                .build();

        return new NodeRequest<MessagesResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES);
    }

    /**
     * Searches for a specific amount of messages in a specific chat's messages, including ones that are not in memory
     * If there are too many result the {@code attribute} parameter should be specified in order to view the next pages
     *
     * @param search the keyword to search
     * @param chat   the target chat
     * @param count  the number of messages to query
     * @param page   the page to query
     */
    public @NotNull CompletableFuture<MessagesResponse> searchInChat(@NotNull String search, @NotNull WhatsappChat chat, int count, int page) {
        var node = WhatsappNode.builder()
                .description("query")
                .attrs(Map.of("search", search, "jid", chat.jid(), "count", String.valueOf(count), "epoch", String.valueOf(manager.tagAndIncrement()), "page", String.valueOf(page), "type", "search"))
                .content(null)
                .build();

        return new NodeRequest<MessagesResponse>(configuration, node) {}.send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES);
    }

}
