package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.annotation.RegisterListenerProcessor;
import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.request.impl.NodeRequest;
import it.auties.whatsapp4j.request.impl.QueryRequest;
import it.auties.whatsapp4j.request.impl.SubscribeUserPresence;
import it.auties.whatsapp4j.response.impl.binary.ChatResponse;
import it.auties.whatsapp4j.response.impl.binary.MessagesResponse;
import it.auties.whatsapp4j.response.impl.binary.NodeResponse;
import it.auties.whatsapp4j.response.impl.json.*;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import it.auties.whatsapp4j.utils.Validate;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import it.auties.whatsapp4j.model.*;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.glassfish.tyrus.core.Beta;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Accessors(fluent = true)
public class WhatsappAPI {
    private final @NotNull WhatsappWebSocket socket;
    private final @NotNull WhatsappConfiguration configuration;
    private final @Getter @NotNull WhatsappDataManager manager;
    private final @Getter @NotNull WhatsappKeysManager keys;
    public WhatsappAPI(){
        this(WhatsappConfiguration.defaultOptions());
    }

    public WhatsappAPI(@NotNull WhatsappConfiguration configuration){
        this.configuration = configuration;
        this.manager = WhatsappDataManager.singletonInstance();
        this.keys = WhatsappKeysManager.fromPreferences();
        this.socket = new WhatsappWebSocket(configuration, keys);
    }

    public @NotNull WhatsappAPI connect(){
        socket.connect();
        return this;
    }

    public @NotNull WhatsappAPI disconnect(){
        socket.disconnect(null, false, false);
        return this;
    }

    public @NotNull WhatsappAPI logout(){
        socket.disconnect(null, true, false);
        return this;
    }

    public @NotNull WhatsappAPI reconnect(){
        socket.disconnect(null, false, true);
        return this;
    }

    public @NotNull WhatsappAPI autodetectListeners(){
        Validate.isTrue(manager.listeners().addAll(RegisterListenerProcessor.queryAllListeners()), "WhatsappAPI: Cannot autodetect listeners");
        return this;
    }

    public @NotNull WhatsappAPI registerListener(@NotNull WhatsappListener listener){
        Validate.isTrue(manager.listeners().add(listener), "WhatsappAPI: Cannot add listener %s", listener.getClass().getName());
        return this;
    }

    public @NotNull WhatsappAPI removeListener(@NotNull WhatsappListener listener){
        Validate.isTrue(manager.listeners().remove(listener), "WhatsappAPI: Cannot remove listener %s", listener.getClass().getName());
        return this;
    }

    public @NotNull WhatsappAPI subscribeToUserPresence(@NotNull String jid){
        var subscribe = new SubscribeUserPresence(configuration, jid){};
        subscribe.send(socket.session());
        return this;
    }

    public @NotNull CompletableFuture<MessageResponse> sendMessage(@NotNull WhatsappMessageRequest request) {
        return sendMessage(request.buildMessage());
    }

    public @NotNull CompletableFuture<MessageResponse> sendMessage(@NotNull WhatsappProtobuf.WebMessageInfo message){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("type", "relay", "epoch", String.valueOf(manager.tagAndIncrement())))
                .content(List.of(new WhatsappNode("message", Map.of(), message)))
                .build();

        return new NodeRequest<MessageResponse>(message.getKey().getId(), configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.MESSAGE)
                .future();
    }

    public @NotNull CompletableFuture<UserStatusResponse> queryUserStatus(@NotNull String jid){
        return queryJson(jid, QueryRequest.QueryType.USER_STATUS);
    }

    public @NotNull CompletableFuture<ChatPictureResponse> queryChatPicture(@NotNull String jid){
        return queryJson(jid, QueryRequest.QueryType.CHAT_PICTURE);
    }

    public @NotNull CompletableFuture<GroupMetadataResponse> queryGroupMetadata(@NotNull String jid){
        return queryJson(jid, QueryRequest.QueryType.GROUP_METADATA);
    }

    public @NotNull CompletableFuture<CommonGroupsResponse> queryGroupsInCommon(@NotNull String jid){
        return queryJson(jid, QueryRequest.QueryType.GROUPS_IN_COMMON);
    }

    public <T extends JsonResponseModel> @NotNull CompletableFuture<T> queryJson(@NotNull String jid, @NotNull QueryRequest.QueryType queryType){
        return new QueryRequest<T>(configuration, jid, queryType){}
                .send(socket.session())
                .future();
    }

    public @NotNull CompletableFuture<ChatResponse> queryChat(@NotNull WhatsappContact contact) {
        return queryChat(contact.jid());
    }

    public @NotNull CompletableFuture<ChatResponse> queryChat(@NotNull String jid){
        return socket.queryChat(jid);
    }

    public @NotNull CompletableFuture<MessagesResponse> queryFavouriteMessagesInChat(@NotNull WhatsappContact contact, int count){
        return queryFavouriteMessagesInChat(contact.jid(), count);
    }

    public @NotNull CompletableFuture<MessagesResponse> queryFavouriteMessagesInChat(@NotNull WhatsappChat chat, int count){
        return queryFavouriteMessagesInChat(chat.jid(), count);
    }

    public @NotNull CompletableFuture<MessagesResponse> queryFavouriteMessagesInChat(@NotNull String jid, int count){
        var node = WhatsappNodeBuilder.builder()
                .description("query")
                .attrs(Map.of("chat", jid, "count", String.valueOf(count), "epoch", String.valueOf(manager.tagAndIncrement()), "type", "star"))
                .content(null)
                .build();

        return new NodeRequest<MessagesResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES)
                .future();
    }

    public @NotNull CompletableFuture<NodeResponse> queryPreview(@NotNull String jid){
        var node = WhatsappNodeBuilder.builder()
                .description("query")
                .attrs(Map.of("jid", jid, "epoch", String.valueOf(manager.tagAndIncrement()), "type", "preview"))
                .build();

        return new NodeRequest<NodeResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.QUERY_PREVIEW)
                .future();
    }

    public @NotNull CompletableFuture<WhatsappChat> loadConversation(@NotNull WhatsappChat chat) {
        return loadConversation(chat, 20);
    }

    public @NotNull CompletableFuture<WhatsappChat> loadConversation(@NotNull WhatsappChat chat, int messageCount) {
        var node = WhatsappNodeBuilder.builder()
                .description("query")
                .attrs(Map.of("type", "message", "epoch", String.valueOf(manager.tagAndIncrement()), "jid", chat.jid(), "kind", "before", "count", String.valueOf(messageCount)))
                .build();

        return new NodeRequest<MessagesResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.QUERY_MESSAGES)
                .future()
                .thenApply(res -> {
                    chat.messages().addAll(res.messages());
                    return chat;
                });
    }

    public @NotNull CompletableFuture<DiscardResponse> changePresence(@NotNull WhatsappContactStatus presence){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("type", "set", "epoch", String.valueOf(manager.tagAndIncrement())))
                .content(List.of(new WhatsappNode("presence", Map.of("type", presence.data()), null)))
                .build();

        return new NodeRequest<DiscardResponse>(configuration, node, false){}
                .send(socket.session(), keys, presence.flag(), BinaryMetric.PRESENCE)
                .future();
    }

    public @NotNull CompletableFuture<DiscardResponse> changePresence(@NotNull WhatsappContactStatus presence, @NotNull WhatsappChat chat){
        return changePresence(presence, chat.jid());
    }

    public @NotNull CompletableFuture<DiscardResponse> changePresence(@NotNull WhatsappContactStatus presence, @NotNull WhatsappContact contact){
        return changePresence(presence, contact.jid());
    }

    public @NotNull CompletableFuture<DiscardResponse> changePresence(@NotNull WhatsappContactStatus presence, @NotNull String targetJid){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("type", "set", "epoch", String.valueOf(manager.tagAndIncrement())))
                .content(List.of(new WhatsappNode("presence", Map.of("type", presence.data(), "to", targetJid), null)))
                .build();

        return new NodeRequest<DiscardResponse>(configuration, node, false){}
                .send(socket.session(), keys, presence.flag(), BinaryMetric.PRESENCE)
                .future();
    }

    public @NotNull CompletableFuture<GroupModificationResponse> promote(@NotNull WhatsappChat group, @NotNull WhatsappContact... participants){
        return modifyGroupParticipant(group.jid(), GroupAction.PROMOTE, WhatsappUtils.jidsToParticipantNodes(participants));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> promote(@NotNull WhatsappChat group, @NotNull String... participantJids){
        return modifyGroupParticipant(group.jid(), GroupAction.PROMOTE, WhatsappUtils.jidsToParticipantNodes(participantJids));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> promote(@NotNull String groupJid, @NotNull WhatsappContact... participants){
        return modifyGroupParticipant(groupJid, GroupAction.PROMOTE, WhatsappUtils.jidsToParticipantNodes(participants));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> promote(@NotNull String groupJid, @NotNull String... participantJids){
        return modifyGroupParticipant(groupJid, GroupAction.PROMOTE, WhatsappUtils.jidsToParticipantNodes(participantJids));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> demote(@NotNull WhatsappChat group, @NotNull WhatsappContact... participants){
        return modifyGroupParticipant(group.jid(), GroupAction.DEMOTE, WhatsappUtils.jidsToParticipantNodes(participants));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> demote(@NotNull WhatsappChat group, @NotNull String... participantJids){
        return modifyGroupParticipant(group.jid(), GroupAction.DEMOTE, WhatsappUtils.jidsToParticipantNodes(participantJids));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> demote(@NotNull String groupJid, @NotNull WhatsappContact... participants){
        return modifyGroupParticipant(groupJid, GroupAction.DEMOTE, WhatsappUtils.jidsToParticipantNodes(participants));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> demote(@NotNull String groupJid, @NotNull String... participantJids){
        return modifyGroupParticipant(groupJid, GroupAction.PROMOTE, WhatsappUtils.jidsToParticipantNodes(participantJids));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> add(@NotNull WhatsappChat group, @NotNull WhatsappContact... participants){
        return modifyGroupParticipant(group.jid(), GroupAction.ADD, WhatsappUtils.jidsToParticipantNodes(participants));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> add(@NotNull WhatsappChat group, @NotNull String... participantJids){
        return modifyGroupParticipant(group.jid(), GroupAction.ADD, WhatsappUtils.jidsToParticipantNodes(participantJids));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> add(@NotNull String groupJid, @NotNull WhatsappContact... participants){
        return modifyGroupParticipant(groupJid, GroupAction.ADD, WhatsappUtils.jidsToParticipantNodes(participants));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> add(@NotNull String groupJid, @NotNull String... participantJids){
        return modifyGroupParticipant(groupJid, GroupAction.ADD, WhatsappUtils.jidsToParticipantNodes(participantJids));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> remove(@NotNull WhatsappChat group, @NotNull WhatsappContact... participants){
        return modifyGroupParticipant(group.jid(), GroupAction.REMOVE, WhatsappUtils.jidsToParticipantNodes(participants));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> remove(@NotNull WhatsappChat group, @NotNull String... participantJids){
        return modifyGroupParticipant(group.jid(), GroupAction.REMOVE, WhatsappUtils.jidsToParticipantNodes(participantJids));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> remove(@NotNull String groupJid, @NotNull WhatsappContact... participants){
        return modifyGroupParticipant(groupJid, GroupAction.REMOVE, WhatsappUtils.jidsToParticipantNodes(participants));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> remove(@NotNull String groupJid, @NotNull String... participantJids){
        return modifyGroupParticipant(groupJid, GroupAction.REMOVE, WhatsappUtils.jidsToParticipantNodes(participantJids));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> modifyGroupParticipant(@NotNull String groupJid, @NotNull GroupAction action, @NotNull List<WhatsappNode> jids){
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", groupJid, "author", manager.phoneNumber(), "id", tag, "type", action.data()), jids)))
                .build();

        return new NodeRequest<GroupModificationResponse>(tag, configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupName(@NotNull WhatsappChat chat, @NotNull String newName){
        return changeGroupName(chat.jid(), newName);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupName(@NotNull String jid, @NotNull String newName){
        Validate.isTrue(WhatsappUtils.isGroup(jid), "WhatsappAPI: Cannot change group's name: %s is not a group", jid);
        Validate.isTrue(!newName.isBlank(), "WhatsappAPI: Cannot change group's name: the new name cannot be empty or blank");

        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", jid, "subject", newName, "author", manager.phoneNumber(), "id", tag, "type", "subject"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupDescription(@NotNull WhatsappChat chat, @NotNull String newDescription){
        return changeGroupDescription(chat.jid(), newDescription);
    }

    @SneakyThrows
    @Beta
    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupDescription(@NotNull String jid, @NotNull String newDescription){
        Validate.isTrue(WhatsappUtils.isGroup(jid), "WhatsappAPI: Cannot change group's description: %s is not a group", jid);

        var previousId = queryGroupMetadata(jid).get().descriptionMessageId();
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", jid, "author", manager.phoneNumber(), "id", tag, "type", "description"), List.of(new WhatsappNode("description", Map.of("id", WhatsappUtils.randomId(), "prev", Objects.requireNonNullElse(previousId, "none")), newDescription)))))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> changeWhoCanSendMessagesInGroup(@NotNull WhatsappChat chat, @NotNull GroupPolicy policy){
        return changeWhoCanSendMessagesInGroup(chat.jid(), policy);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> changeWhoCanSendMessagesInGroup(@NotNull String jid, @NotNull GroupPolicy policy){
        return changeGroupSetting(jid, GroupSetting.SEND_MESSAGES, policy);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> changeWhoCanEditGroupInfo(@NotNull WhatsappChat chat, @NotNull GroupPolicy policy){
        return changeWhoCanEditGroupInfo(chat.jid(), policy);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> changeWhoCanEditGroupInfo(@NotNull String jid, @NotNull GroupPolicy policy){
        return changeGroupSetting(jid, GroupSetting.EDIT_GROUP_INFO, policy);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupSetting(@NotNull WhatsappChat chat, @NotNull GroupSetting setting, @NotNull GroupPolicy policy){
        return changeGroupSetting(chat.jid(), setting, policy);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> changeGroupSetting(@NotNull String jid, @NotNull GroupSetting setting, @NotNull GroupPolicy policy){
        Validate.isTrue(WhatsappUtils.isGroup(jid), "WhatsappAPI: Cannot change group's setting: %s is not a group", jid);
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", jid, "author", manager.phoneNumber(), "id", tag, "type", "prop"), List.of(new WhatsappNode(setting.data(), Map.of("value", policy.data()), null)))))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }

    public @NotNull CompletableFuture<NodeResponse> changeGroupPicture(@NotNull WhatsappChat chat){
        return changeGroupPicture(chat.jid());
    }

    @Beta
    public @NotNull CompletableFuture<NodeResponse> changeGroupPicture(@NotNull String jid){
        /*
         TODO: 25/02/2021 WhatsappAPI#changeGroupPicture: Implement
                 Validate.isTrue(WhatsappUtils.isGroup(jid), "WhatsappAPI: Cannot change group's picture: %s is not a group", jid);

        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("picture", Map.of("jid", jid, "id", tag, "type", "set"), List.of(new WhatsappNode("image", Map.of(), "!!!IMAGE!!!)]")))))
                .build();

        var request = new NodeRequest<>(node, NodeResponse.class);
        request.send(socket.session(), configuration, keys, BinaryFlag.IGNORE, BinaryMetric.PICTURE);
        return request.future();
         */
        throw new RuntimeException();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> removeGroupPicture(@NotNull WhatsappChat chat){
        return removeGroupPicture(chat.jid());
    }

    @Beta
    public @NotNull CompletableFuture<SimpleStatusResponse> removeGroupPicture(@NotNull String jid){
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("picture", Map.of("jid", jid, "id", tag, "type", "delete"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.PICTURE)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> leave(@NotNull WhatsappChat chat){
        return leave(chat.jid());
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> leave(@NotNull String jid){
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", jid, "author", manager.phoneNumber(), "id", tag, "type", "leave"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappContact contact, @NotNull ZonedDateTime duration){
        return mute(contact.jid(), duration);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappChat chat, @NotNull ZonedDateTime duration){
        return mute(chat.jid(), duration);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull String jid, @NotNull ZonedDateTime duration){
        return mute(jid, duration.toEpochSecond());
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappContact contact){
        return mute(contact.jid());
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappChat chat){
        return mute(chat.jid());
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull String jid){
        return mute(jid, -1);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappContact contact, long seconds){
        return mute(contact.jid(), seconds);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull WhatsappChat chat, long seconds){
        return mute(chat.jid(), seconds);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> mute(@NotNull String jid, long seconds){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("jid", jid, "mute", String.valueOf(seconds), "type", "mute"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> unmute(@NotNull WhatsappChat chat){
        Validate.isTrue(chat.mute().isMuted(), "WhatsappAPI: Cannot unmute chat with jid %s: chat is not muted", chat.jid());

        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("jid", chat.jid(), "previous", chat.mute().muteEndDate().map(ChronoZonedDateTime::toEpochSecond).map(String::valueOf).orElseThrow(), "type", "mute"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> block(@NotNull WhatsappChat chat){
        return block(chat.jid());
    }

    @Beta
    public @NotNull CompletableFuture<SimpleStatusResponse> block(@NotNull String jid){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("block", Map.of("jid", jid), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.BLOCK)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> enableEphemeralMessages(@NotNull WhatsappChat chat){
        return enableEphemeralMessages(chat.jid());
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> enableEphemeralMessages(@NotNull String jid){
        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", jid, "author", manager.phoneNumber(), "id", tag, "type", "prop"), List.of(new WhatsappNode("ephemeral", Map.of("value", "604800"), null)))))
                .build();

        return new NodeRequest<SimpleStatusResponse>(tag, configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> markAsUnread(@NotNull WhatsappChat chat){
        return markChat(chat, -2);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> markAsRead(@NotNull WhatsappChat chat){
        return markChat(chat, -1);
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> markChat(@NotNull WhatsappChat chat, int count){
        var lastMessage = chat.lastMessage().orElseThrow();
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("read", Map.of("owner", String.valueOf(lastMessage.sentByMe()), "jid", chat.jid(), "count", String.valueOf(count), "index", lastMessage.info().getKey().getId()), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.READ)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> pin(@NotNull WhatsappChat chat){
        return pin(chat.jid());
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> pin(@NotNull String jid){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("jid", jid, "until", String.valueOf(ZonedDateTime.now().toEpochSecond()), "type", "pin"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> unpin(@NotNull WhatsappChat chat){
        return unpin(chat.jid());
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> unpin(@NotNull String jid){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("jid", jid, "previous", "none", "type", "pin"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> archive(@NotNull WhatsappChat chat){
        var lastMessage = chat.lastMessage().orElseThrow();
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("owner", String.valueOf(lastMessage.sentByMe()), "jid", chat.jid(), "index", lastMessage.info().getKey().getId(), "type", "archive"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> unarchive(@NotNull WhatsappChat chat){
        var lastMessage = chat.lastMessage().orElseThrow();
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("chat", Map.of("owner", String.valueOf(lastMessage.sentByMe()), "jid", chat.jid(), "index", lastMessage.info().getKey().getId(), "type", "unarchive"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.CHAT)
                .future();
    }

    public @NotNull CompletableFuture<GroupModificationResponse> createGroup(@NotNull String subject, @NotNull WhatsappContact... participants){
        return createGroup(subject, WhatsappUtils.jidsToParticipantNodes(participants));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> createGroup(@NotNull String subject, @NotNull String... participantJids){
        return createGroup(subject, WhatsappUtils.jidsToParticipantNodes(participantJids));
    }

    public @NotNull CompletableFuture<GroupModificationResponse> createGroup(@NotNull String subject, @NotNull List<WhatsappNode> participants){
        Validate.isTrue(!participants.isEmpty(), "WhatsappAPI: Cannot create a group with name %s with no participants".formatted(subject));

        var tag = WhatsappUtils.buildRequestTag(configuration);
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("subject", subject, "author", manager.phoneNumber(), "id", tag, "type", "create"), participants)))
                .build();

        return new NodeRequest<GroupModificationResponse>(tag, configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }

    public @NotNull CompletableFuture<SimpleStatusResponse> deleteGroup(@NotNull String jid){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("epoch", String.valueOf(manager.tagAndIncrement()), "type", "set"))
                .content(List.of(new WhatsappNode("group", Map.of("jid", jid, "type", "delete"), null)))
                .build();

        return new NodeRequest<SimpleStatusResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }

    public @NotNull CompletableFuture<MessagesResponse> search(@NotNull String search, int count, int page){
        var node = WhatsappNodeBuilder.builder()
                .description("query")
                .attrs(Map.of("search", search, "count", String.valueOf(count), "epoch", String.valueOf(manager.tagAndIncrement()), "page", String.valueOf(page), "type", "search"))
                .content(null)
                .build();

        return new NodeRequest<MessagesResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }

    public @NotNull CompletableFuture<MessagesResponse> searchInChat(@NotNull String search, @NotNull WhatsappChat chat, int count, int page){
        return searchInChat(search, chat.jid(), count, page);
    }

    public @NotNull CompletableFuture<MessagesResponse> searchInChat(@NotNull String search, @NotNull String jid, int count, int page){
        var node = WhatsappNodeBuilder.builder()
                .description("query")
                .attrs(Map.of("search", search, "jid", jid, "count", String.valueOf(count), "epoch", String.valueOf(manager.tagAndIncrement()), "page", String.valueOf(page), "type", "search"))
                .content(null)
                .build();

        return new NodeRequest<MessagesResponse>(configuration, node){}
                .send(socket.session(), keys, BinaryFlag.IGNORE, BinaryMetric.GROUP)
                .future();
    }
}
