package it.auties.whatsapp.api;

import it.auties.whatsapp.binary.BinarySocket;
import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.controller.WhatsappStore;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.response.ContactStatusResponse;
import it.auties.whatsapp.model.response.HasWhatsappResponse;
import it.auties.whatsapp.util.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static it.auties.bytes.Bytes.ofRandom;
import static it.auties.whatsapp.api.WhatsappOptions.defaultOptions;
import static it.auties.whatsapp.controller.WhatsappController.knownIds;
import static it.auties.whatsapp.model.request.Node.*;
import static java.util.Map.of;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A class used to interface a user to WhatsappWeb's WebSocket.
 * It provides various functionalities, including the possibility to query, set and modify data associated with the loaded session of whatsapp.
 * It can be configured using a default configuration or a custom one.
 * Multiple instances of this class can be initialized, though it is not advisable as; is a singleton and cannot distinguish between the data associated with each session.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused")
public class Whatsapp {
    /**
     * The socket associated with this session
     */
    private final BinarySocket socket;

    /**
     * Constructs a new instance of the API.
     * If the id is not associated with any session, a new one will be created.
     *
     * @param id the id of the session
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection(int id){
        return newConnection(WhatsappOptions.defaultOptions().withId(id));
    }

    /**
     * Constructs a new instance of the API
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection(){
        return newConnection(defaultOptions().withId(Keys.registrationId()));
    }

    /**
     * Constructs a new instance of the API.
     * If the id is not associated with any session, a new one will be created.
     *
     * @param options the non-null options used to create this session
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection(@NonNull WhatsappOptions options){
        return new Whatsapp(options);
    }

    /**
     * Constructs a new instance of the API from a fresh connection using a random id
     *
     * @param options the non-null options used to create this session
     * @param store   the non-null store used to create this session
     * @param keys    the non-null keys used to create this session
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection(@NonNull WhatsappOptions options, @NonNull WhatsappStore store, @NonNull WhatsappKeys keys){
        return new Whatsapp(options, store, keys);
    }

    /**
     * Constructs a new instance of the API from the first session opened.
     * If no sessions are available, a new one will be created.
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp firstConnection(){
        return newConnection(requireNonNullElseGet(knownIds().peekFirst(), Keys::registrationId));
    }

    /**
     * Constructs a new instance of the API from the last session opened.
     * If no sessions are available, a new one will be created.
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp lastConnection(){
        return newConnection(requireNonNullElseGet(knownIds().peekLast(), Keys::registrationId));
    }

    /**
     * Returns a list of all known connections
     *
     * @return a non-null List
     */
    public static List<Whatsapp> listConnections(){
        return streamConnections().toList();
    }

    /**
     * Returns a stream of all known connections
     *
     * @return a non-null Stream
     */
    public static Stream<Whatsapp> streamConnections(){
        return knownIds()
                .stream()
                .map(Whatsapp::newConnection);
    }

    private Whatsapp(WhatsappOptions options){
        this(options, WhatsappStore.of(options.id()), WhatsappKeys.of(options.id()));
    }

    private Whatsapp(WhatsappOptions options, WhatsappStore store, WhatsappKeys keys){
        this(new BinarySocket(options, store, keys));
        ListenerScanner.scan(this)
                .forEach(this::registerListener);
    }

    /**
     * Returns the store associated with this session
     *
     * @return a non-null WhatsappStore
     */
    public WhatsappStore store(){
        return socket.store();
    }

    /**
     * Returns the keys associated with this session
     *
     * @return a non-null WhatsappKeys
     */
    public WhatsappKeys keys(){
        return socket.keys();
    }

    /**
     * Registers a listener
     *
     * @param listener the listener to register
     * @return the same instance
     */
    public Whatsapp registerListener(@NonNull WhatsappListener listener) {
        Validate.isTrue(socket.store().listeners().add(listener),
                "WhatsappAPI: Cannot add listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Removes a listener
     *
     * @param listener the listener to remove
     * @return the same instance
     */
    public Whatsapp removeListener(@NonNull WhatsappListener listener) {
        Validate.isTrue(socket.store().listeners().remove(listener),
                "WhatsappAPI: Cannot remove listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Opens a connection with Whatsapp Web's WebSocket if a previous connection doesn't exist
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> connect() {
        return socket.connect()
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Waits for the socket to be closed on the current thread
     */
    public void await(){
        socket.await();
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> disconnect() {
        return socket.disconnect()
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> reconnect() {
        return socket.reconnect()
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket and logs out of WhatsappWeb invalidating the previous saved credentials.
     * The next time the API is used, the QR code will need to be scanned again.
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> logout() {
        return socket.logout()
                .thenApplyAsync(ignored -> this);
    }
    
    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the endTimeStamp the contact was last seen.
     *
     * @param jid the contact whose status the api should receive updates on
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> subscribeToContactPresence(@NonNull ContactJidProvider jid) {
        var node = withAttributes("presence",
                of("to", jid.toJid(), "type", "subscribe"));
        return socket.sendWithNoResponse(node);
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull String message) {
        return sendMessage(chat, MessageContainer.of(message));
    }
    
    /**
     * Builds and sends a message from a chat, a message and a quoted message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the quoted message
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull String message, @NonNull MessageInfo quotedMessage) {
        return sendMessage(chat, TextMessage.of(message), quotedMessage);
    }

    /**
     * Builds and sends a message from a chat, a message and a quoted message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the quoted message
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull ContextualMessage message, @NonNull MessageInfo quotedMessage) {
        var context = ContextInfo.newContextInfo()
                .quotedMessageId(quotedMessage.id())
                .quotedMessageContainer(quotedMessage.message())
                .quotedMessageSenderId(quotedMessage.senderJid())
                .create();
        return sendMessage(chat, message, context);
    }
    
    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture 
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull Message message) {
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull MessageContainer message) {
        var key = MessageKey.newMessageKey()
                .chatJid(chat.toJid())
                .fromMe(true)
                .create();
        var info = MessageInfo.newMessageInfo()
                .storeId(store().id())
                .key(key)
                .message(message)
                .create();
        return sendMessage(info);
    }
    
    /**
     * Builds and sends a message from a chat, a message and a context
     *
     * @param chat        the chat where the message should be sent
     * @param message     the message to send
     * @param contextInfo the context of the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull ContextualMessage message, @NonNull ContextInfo contextInfo) {
        var key = MessageKey.newMessageKey()
                .chatJid(chat.toJid())
                .fromMe(true)
                .create();
        var info = MessageInfo.newMessageInfo()
                .storeId(store().id())
                .key(key)
                .message(MessageContainer.of(message.contextInfo(contextInfo)))
                .create();
        return sendMessage(info);
    }

    /**
     * Sends a message info to a chat
     *
     * @param message the message to send
     * @return a CompletableFuture 
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull MessageInfo message) {
        if(message.message().content() instanceof MediaMessage mediaMessage){
            mediaMessage.storeId(store().id());
        }

        return socket.sendMessage(message)
                .thenApplyAsync(ignored -> message);
    }

    /**
     * Executes a query to determine whether any number of users have an account on Whatsapp
     *
     * @param chats the users to check
     * @return a CompletableFuture that wraps a non-null list of HasWhatsappResponse
     */
    public CompletableFuture<List<HasWhatsappResponse>> hasWhatsapp(@NonNull ContactJidProvider... chats) {
        var contactNodes = Arrays.stream(chats)
                .map(jid -> with("contact", "+%s".formatted(jid.toJid().user())))
                .toArray(Node[]::new);
        return socket.sendInteractiveQuery(with("contact"), withChildren("user", contactNodes))
                .thenApplyAsync(nodes -> nodes.stream().map(HasWhatsappResponse::new).toList());
    }

    /**
     * Queries the block list
     *
     * @return a CompletableFuture that wraps a non-null list of ContactJid
     */
    public CompletableFuture<List<ContactJid>> queryBlockList() {
        return socket.sendQuery("get", "blocklist", (Node) null)
                .thenApplyAsync(this::parseBlockList);
    }

    private List<ContactJid> parseBlockList(Node result) {
        return result.findNode("list")
                .findNodes("item")
                .stream()
                .map(item -> item.attributes().getJid("jid").orElseThrow())
                .toList();
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param chat the target contact
     * @return a CompletableFuture that wraps an optional contact status response
     */
    public CompletableFuture<Optional<ContactStatusResponse>> queryStatus(@NonNull ContactJidProvider chat) {
        var query = with("status");
        var body = withAttributes("user", of("jid", chat.toJid()));
        return socket.sendInteractiveQuery(query, body)
                .thenApplyAsync(response -> Nodes.findFirst(response, "status"))
                .thenApplyAsync(node -> node.map(ContactStatusResponse::new));
    }

    /**
     * Queries the profile picture of a chat.
     *
     * @param chat the chat of the chat to query
     * @return a CompletableFuture that wraps nullable jpg url hosted on Whatsapp's servers
     */
    public CompletableFuture<URI> queryChatPicture(@NonNull ContactJidProvider chat) {
        var body = withAttributes("picture", of("query", "url"));
        return socket.sendQuery("get", "w:profile:picture", of("target", chat.toJid()), body)
                .thenApplyAsync(this::parseChatPicture);
    }

    private URI parseChatPicture(Node result) {
        return Optional.ofNullable(result.findNode("picture"))
                .map(picture -> picture.attributes().getString("url", null))
                .map(URI::create)
                .orElse(null);
    }

    /**
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> queryMetadata(@NonNull ContactJidProvider chat) {
        return socket.queryGroupMetadata(chat.toJid());
    }

    /**
     * Queries the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<String> queryInviteCode(@NonNull ContactJidProvider chat) {
        return socket.sendQuery(chat.toJid(), "get", "w:g2", with("invite"))
                .thenApplyAsync(result -> result.findNode("invite").attributes().getString("code"));
    }

    /**
     * Revokes the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<?> revokeInviteCode(@NonNull ContactJidProvider chat) {
        return socket.sendQuery(chat.toJid(), "set", "w:g2", with("invite"));
    }

    /**
     * Accepts the invite for a group
     *
     * @param inviteCode the invite code
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<Chat>> acceptInvite(@NonNull String inviteCode) {
        return socket.sendQuery(ContactJid.GROUP, "set", "w:g2", withAttributes("invite", of("invite", inviteCode)))
                .thenApplyAsync(result -> result.findNode("group"))
                .thenApplyAsync(group -> group.attributes()
                        .getJid("jid")
                        .map(Chat::ofJid));
    }

    /**
     * Queries the groups in common with a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> queryGroupsInCommon(@NonNull ContactJidProvider contact) {
        //     ((0,
        //                l.participantCreateTable)(), [new s.default("groupId"), new a.default("senderKey"), new a.default("participants"), new a.default("pastParticipants"), new a.default("admins"), new a.default("rotateKey"), new a.default("version"), new i.default("participants"), new a.default("deviceSyncComplete"), new a.default("staleType")]).view((e=>e))
        //
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Queries a specified amount of starred/favourite messages in a chat, including ones not in memory
     *
     * @param chat  the target chat
     * @param count the amount of messages
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> queryFavouriteMessages(@NonNull ContactJidProvider chat, int count) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param available whether you are online or not
     * @return a CompletableFuture 
     */
    public CompletableFuture<Void> changePresence(boolean available) {
        var presence = available ? ContactStatus.AVAILABLE : ContactStatus.UNAVAILABLE;
        var node = withAttributes("presence", of("type", presence.data()));
        return socket.sendWithNoResponse(node);
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chat     the target chat
     * @param presence the new status
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> changePresence(@NonNull ContactJidProvider chat, @NonNull ContactStatus presence) {
        var node = withAttributes("presence", of("to", chat.toJid(), "type", presence.data()));
        return socket.sendWithNoResponse(node);
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> promote(@NonNull ContactJidProvider group, @NonNull ContactJidProvider... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.PROMOTE, contacts);
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> demote(@NonNull ContactJidProvider group, @NonNull ContactJidProvider... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.DEMOTE, contacts);
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> add(@NonNull ContactJidProvider group, @NonNull ContactJidProvider... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.ADD, contacts);
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> remove(@NonNull ContactJidProvider group, @NonNull ContactJidProvider... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.REMOVE, contacts);
    }

    private CompletableFuture<List<ContactJid>> executeActionOnGroupParticipant(ContactJidProvider group, GroupAction action, ContactJidProvider... jids) {
        var body = Arrays.stream(jids)
                .map(ContactJidProvider::toJid)
                .map(jid -> withChildren(action.data(), withAttributes("participant", of("jid", jid))))
                .toArray(Node[]::new);
        return socket.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenApplyAsync(result -> result.findNode(action.data()))
                .thenApplyAsync(result -> result.findNodes("participant"))
                .thenApplyAsync(participants -> participants.stream()
                        .filter(participant -> !participant.attributes().hasKey("error"))
                        .map(participant -> participant.attributes().getJid("jid").orElseThrow())
                        .toList());
    }

    /**
     * Changes the name of a group
     *
     * @param group   the target group
     * @param newName the new name for the group
     * @return a CompletableFuture
     * @throws IllegalArgumentException if the provided new name is empty or blank
     */
    public CompletableFuture<?> changeSubject(@NonNull ContactJidProvider group, @NonNull String newName) {
        var body = with("subject", newName.getBytes(StandardCharsets.UTF_8));
        return socket.sendQuery(group.toJid(), "set", "w:g2", body);
    }

    /**
     * Changes the description of a group
     *
     * @param group          the target group
     * @param newDescription the new name for the group, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<?> changeDescription(@NonNull ContactJidProvider group, String newDescription) {
        return socket.queryGroupMetadata(group.toJid())
                .thenApplyAsync(GroupMetadata::descriptionId)
                .thenComposeAsync(descriptionId -> changeDescription(group, newDescription, descriptionId));
    }

    private CompletableFuture<Node> changeDescription(ContactJidProvider group, String newDescription, String descriptionId) {
        var description = Optional.ofNullable(newDescription)
                .map(content -> with("body", content.getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
        var key = newDescription != null ? "description" : "delete";
        var value = descriptionId != null ? MessageKey.randomId() : "true";
        var body = withChildren("description", of(key, value), description);
        return socket.sendQuery(group.toJid(), "set", "w:g2", body);
    }

    /**
     * Changes which category of users can send messages in a group
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture
     */
    public CompletableFuture<?> changeWhoCanSendMessages(@NonNull ContactJidProvider group, @NonNull GroupPolicy policy) {
        var body = with(policy != GroupPolicy.ANYONE ? "not_announcement" : "announcement");
        return socket.sendQuery(group.toJid(), "set", "w:g2", body);
    }

    /**
     * Changes which category of users can edit the group's settings
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture
     */
    public CompletableFuture<?> changeWhoCanEditInfo(@NonNull ContactJidProvider group, @NonNull GroupPolicy policy) {
        var body = with(policy != GroupPolicy.ANYONE ? "locked" : "unlocked");
        return socket.sendQuery(group.toJid(), "set", "w:g2", body);
    }

    /**
     * Changes the profile picture of yourself
     *
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<?> changePicture(byte[] image) {
        return changePicture(keys().companion().toUserJid(), image);
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<?> changePicture(@NonNull ContactJidProvider group, byte[] image) {
        var profilePic = image != null ? Medias.getProfilePic(image) : null;
        var body = with("picture", of("type", "image"), profilePic);
        return socket.sendQuery(group.toJid(), "set", "w:profile:picture", body);
    }

    /**
     * Creates a new group with the provided name and with at least one contact
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> createGroup(@NonNull String subject, @NonNull ContactJidProvider... contacts) {
        var participants = Arrays.stream(contacts)
                .map(contact -> withAttributes("participant", of("jid", contact.toJid())))
                .toArray(Node[]::new);
        var body = withChildren("create", of("subject", subject, "key", ofRandom(12).toHex()), participants);
        return socket.sendQuery(ContactJid.GROUP, "set", "w:g2", body)
                .thenApplyAsync(response -> response.findNode("group"))
                .thenApplyAsync(GroupMetadata::of);
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> leave(@NonNull ContactJidProvider group) {
        var body = withChildren("leave", withAttributes("group", of("id", group.toJid())));
        return socket.sendQuery(ContactJid.GROUP, "set", "w:g2", body);
    }

    /**
     * Mutes a chat indefinitely
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> mute(@NonNull ContactJidProvider chat) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Mutes a chat until a specific date
     *
     * @param chat  the target chat
     * @param until the date the mute ends
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> mute(@NonNull ContactJidProvider chat, @NonNull ZonedDateTime until) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Mutes a chat until a specific date expressed in seconds since the epoch
     *
     * @param chat           the target chat
     * @param untilInSeconds the date the mute ends expressed in seconds since the epoch
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> mute(@NonNull ContactJidProvider chat, long untilInSeconds) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> unmute(@NonNull ContactJidProvider chat) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Blocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> block(@NonNull ContactJidProvider chat) {
        var body = withAttributes("item", of("action", "block", "jid", chat.toJid()));
        return socket.sendQuery("set", "blocklist", body);
    }

    /**
     * Unblocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<?> unblock(@NonNull ContactJidProvider chat) {
        var body = withAttributes("item", of("action", "unblock", "jid", chat.toJid()));
        return socket.sendQuery("set", "blocklist", body);
    }


    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled in said chat after a week
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> enableEphemeral(@NonNull ContactJidProvider chat) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Disables ephemeral messages in a chat, this means that messages sent in said chat will never be cancelled
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> disableEphemeral(@NonNull ContactJidProvider chat) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Changes the ephemeral status of a chat, this means that messages will be automatically cancelled in said chat after the provided endTimeStamp
     *
     * @param chat the target chat
     * @param time the endTimeStamp to live for a message expressed in seconds
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> changeEphemeralStatus(@NonNull ContactJidProvider chat, int time) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> markAsUnread(@NonNull ContactJidProvider chat) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Marks a chat as read
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> markAsRead(@NonNull ContactJidProvider chat) {
        // Node[description=ack, attributes={to=120363043753948701@g.us, id=914C55A427DEA6E754331DB7457B2FFE, type=read, class=receipt, participant=393913594579@s.whatsapp.net}];
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Marks a chat with a flag represented by an integer.
     * If this chat has no history, an attempt to load the chat's history is made.
     * If no messages can be found after said attempt, the request will fail automatically.
     * If the request is successful, sets the number of unread messages to;.
     *
     * @param chat    the target chat
     * @param flag    the flag represented by an int
     * @param newFlag the new flag represented by an int
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> markAs(@NonNull ContactJidProvider chat, int flag, int newFlag) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Marks a chat with a flag represented by an integer.
     * If the request is successful, sets the number of unread messages to;.
     *
     * @param chat        the target chat
     * @param lastMessage the real last message in this chat
     * @param flag        the flag represented by an int
     * @param newFlag     the new flag represented by an int
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> markAs(@NonNull ContactJidProvider chat, @NonNull MessageInfo lastMessage, int flag, int newFlag) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Pins a chat to the top.
     * A maximum of three chats can be pinned to the top.
     * This condition can be checked using;.
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> pin(@NonNull ContactJidProvider chat) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> unpin(@NonNull ContactJidProvider chat) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Archives a chat.
     * If said chat is pinned, it will be unpinned.
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> archive(@NonNull ContactJidProvider chat) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> unarchive(@NonNull ContactJidProvider chat) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void starMessagePlaceholder() {
        // Sent Binary Message Node[description=iq, attributes={xmlns=w:dataSync:app:state, to=s.whatsapp.net, jid=54595.12796-297, type=set}, content=[Node[description=dataSync, attributes={}, content=[Node[description=internal, attributes={name=regular_high, return_snapshot=false, version=13}, content=[Node[description=patch, attributes={}, content=[B@1cd3e518]]]]]]]
        // Received Binary Message Node[description=iq, attributes={from=s.whatsapp.net, jid=54595.12796-297, type=result}, content=[Node[description=dataSync, attributes={}, content=[Node[description=internal, attributes={name=regular_high, version=14}, content=null]]]]]
    }

    private void unstarMessagePlaceholder() {
        // Sent Binary Message Node[description=iq, attributes={xmlns=w:dataSync:app:state, to=s.whatsapp.net, jid=54595.12796-301, type=set}, content=[Node[description=dataSync, attributes={}, content=[Node[description=internal, attributes={name=regular_high, return_snapshot=false, version=14}, content=[Node[description=patch, attributes={}, content=[B@73ce9a0b]]]]]]]
        // Received Binary Message Node[description=iq, attributes={from=s.whatsapp.net, jid=54595.12796-301, type=result}, content=[Node[description=dataSync, attributes={}, content=[Node[description=internal, attributes={name=regular_high, version=15}, content=null]]]]]
    }

    private void groupRelatedStuff(){
        //         t.resetGroupInviteCode = function(e) {
        //            return f((0,
        //            i.wap)("iq", {
        //                type: "set",
        //                xmlns: "w:g2",
        //                to: (0,
        //                d.GROUP_JID)(e),
        //                id: (0,
        //                i.generateId)()
        //            }, (0,
        //            i.wap)("invite", null)))

        //        i.wap)("iq", {
        //                    type: "set",
        //                    xmlns: "w:g2",
        //                    to: (0,
        //                    c.JID)(e),
        //                    id: (0,
        //                    i.generateId)()
        //                }, (0,
        //                i.wap)("join_linked_group", {
        //                    type: n,
        //                    jid: (0,
        //                    c.JID)(t)
        //                }))
        //                  , s = yield(0,
        //                u.sendIq)(r, f);
        //                return s.success ? s.result : (__LOG__(2)`joinSubgroup failed: ${s.errorCode}:${s.errorType}`,
        //                Promise.reject(new o.ServerStatusCodeError(s.errorCode)))


        //        t.setGroupDescription = function(e, t, a, n) {
        //            const r = t ? (0,
        //            i.wap)("description", {
        //                id: (0,
        //                i.CUSTOM_STRING)(a),
        //                prev: (0,
        //                i.MAYBE_CUSTOM_STRING)(n)
        //            }, (0,
        //            i.wap)("body", null, t)) : (0,
        //            i.wap)("description", {
        //                delete: "true",
        //                prev: (0,
        //                i.MAYBE_CUSTOM_STRING)(n)
        //            });
        //            return f((0,
        //            i.wap)("iq", {
        //                to: (0,
        //                d.GROUP_JID)(e),
        //                type: "set",
        //                xmlns: "w:g2",
        //                id: (0,
        //                i.generateId)()
        //            }, r))
        //        }


        //        t.setGroupProperty = function(e, t, a) {
        //            let n;
        //            switch (t) {
        //            case l.GROUP_SETTING_TYPE.ANNOUNCEMENT:
        //                n = 1 === a ? (0,
        //                i.wap)("announcement", null) : (0,
        //                i.wap)("not_announcement", null);
        //                break;
        //            case l.GROUP_SETTING_TYPE.RESTRICT:
        //                n = 1 === a ? (0,
        //                i.wap)("locked", null) : (0,
        //                i.wap)("unlocked", null);
        //                break;
        //            case l.GROUP_SETTING_TYPE.NO_FREQUENTLY_FORWARDED:
        //                n = 1 === a ? (0,
        //                i.wap)("no_frequently_forwarded", null) : (0,
        //                i.wap)("frequently_forwarded_ok", null);
        //                break;
        //            case l.GROUP_SETTING_TYPE.EPHEMERAL:
        //                n = a > 0 ? (0,
        //                i.wap)("ephemeral", {
        //                    expiration: (0,
        //                    i.INT)(a)
        //                }) : (0,
        //                i.wap)("not_ephemeral", null);
        //                break;
        //            default:
        //                return Promise.reject(new Error(`invalid group property ${t}`))
        //            }
        //            return f((0,
        //            i.wap)("iq", {
        //                to: (0,
        //                d.GROUP_JID)(e),
        //                type: "set",
        //                xmlns: "w:g2",
        //                id: (0,
        //                i.generateId)()
        //            }, n))
        //        }


        //     t.setGroupSubject = function(e, t) {
        //            return f((0,
        //            i.wap)("iq", {
        //                to: (0,
        //                d.GROUP_JID)(e),
        //                type: "set",
        //                xmlns: "w:g2",
        //                id: (0,
        //                i.generateId)()
        //            }, (0,
        //            i.wap)("subject", null, t)))
        //        }


        //         function h() {
        //            return (h = (0,
        //            r.default)((function*(e) {
        //                const t = (0,
        //                i.wap)("iq", {
        //                    to: (0,
        //                    d.GROUP_JID)(e),
        //                    type: "get",
        //                    xmlns: "w:g2",
        //                    id: (0,
        //                    i.generateId)()
        //                }, (0,
        //                i.wap)("sub_groups", null))
        //                  , a = yield(0,
        //                l.sendIq)(t, c);
        //                return a.success ? a.result : (__LOG__(2)`queryAllSubgroups failed: ${a.errorCode}:${a.errorType}`,
        //                Promise.reject(new o.ServerStatusCodeError(a.errorCode,a.errorText)))
        //            }
        //            ))).apply(this, arguments)
        //        }
        //        function p() {
        //            return (p = (0,
        //            r.default)((function*(e, t) {
        //                const a = (0,
        //                i.wap)("iq", {
        //                    to: (0,
        //                    d.GROUP_JID)(t),
        //                    type: "get",
        //                    xmlns: "w:g2",
        //                    id: (0,
        //                    i.generateId)()
        //                }, (0,
        //                i.wap)("query_linked", {
        //                    type: "sub_group",
        //                    jid: (0,
        //                    d.GROUP_JID)(e)
        //                }))
        //                  , n = yield(0,
        //                l.sendIq)(a, f);
        //                return n.success ? n.result[0] : (__LOG__(2)`queryAllSubgroups failed: ${n.errorCode}:${n.errorType}`,
        //                Promise.reject(new o.ServerStatusCodeError(n.errorCode,n.errorText)))
        //            }


        //   function u() {
        //            return (u = (0,
        //            r.default)((function*(e) {
        //                const t = (0,
        //                i.wap)("iq", {
        //                    xmlns: "disappearing_mode",
        //                    to: i.S_WHATSAPP_NET,
        //                    type: "set",
        //                    id: (0,
        //                    i.generateId)()
        //                }, (0,
        //                i.wap)("disappearing_mode", {
        //                    duration: (0,
        //                    i.CUSTOM_STRING)(String(e))
        //                }))
        //                  , a = yield(0,
        //                o.sendIq)(t, l);
        //                if (!a.success) {
        //                    const {errorCode: e, errorText: t} = a;
        //                    throw __LOG__(3)`setDisappearingMode: failed ${e}, ${t}`,
        //                    new Error({
        //                        errorCode: e,
        //                        errorText: t
        //                    })
        //                }
        //            }
        //            ))).apply(this, arguments)
        //        }

        //    function f() {
        //            return (f = (0,
        //            r.default)((function*(e) {
        //                const t = e.map((e=>{
        //                    const {name: t, value: a, users: n, dhash: r} = e;
        //                    return (0,
        //                    i.wap)("category", {
        //                        name: (0,
        //                        i.CUSTOM_STRING)(t),
        //                        value: (0,
        //                        i.CUSTOM_STRING)(a),
        //                        dhash: null == n ? i.DROP_ATTR : (0,
        //                        i.CUSTOM_STRING)(null != r ? r : "none")
        //                    }, null == n ? null : n.map((e=>{
        //                        let {action: t, wid: a} = e;
        //                        return (0,
        //                        i.wap)("user", {
        //                            action: (0,
        //                            i.CUSTOM_STRING)(t),
        //                            jid: (0,
        //                            u.JID)(a)
        //                        })
        //                    }
        //                    )))
        //                }
        //                ))
        //                  , a = yield(0,
        //                l.sendIq)((0,
        //                i.wap)("iq", {
        //                    to: i.S_WHATSAPP_NET,
        //                    type: "set",
        //                    xmlns: "privacy",
        //                    id: (0,
        //                    i.generateId)()
        //                }, (0,
        //                i.wap)("privacy", null, t)), d);
        //                if (!0 === a.success)
        //                    return a.result;
        //                throw new o.ServerStatusCodeError(a.errorCode,a.errorText)
        //            }
        //            ))).apply(this, arguments)
        //        }
    }
}
