package it.auties.whatsapp.api;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.protobuf.chat.*;
import it.auties.whatsapp.protobuf.message.model.*;
import it.auties.whatsapp.protobuf.message.standard.TextMessage;
import it.auties.whatsapp.util.RegisterListenerScanner;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.contact.Contact;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.contact.ContactStatus;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.socket.*;
import it.auties.whatsapp.util.Nodes;
import it.auties.whatsapp.util.Validate;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static it.auties.whatsapp.api.WhatsappOptions.defaultOptions;
import static it.auties.whatsapp.socket.Node.*;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A class used to interface a user to WhatsappWeb's WebSocket.
 * It provides various functionalities, including the possibility to query, set and modify data associated with the loaded session of whatsapp.
 * It can be configured using a default configuration or a custom one.
 * Multiple instances of this class can be initialized, though it is not advisable as; is a singleton and cannot distinguish between the data associated with each session.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Whatsapp {
    /**
     * The socket associated with this session
     */
    private final Socket socket;

    /**
     * Constructs a new instance of the API from a known jid.
     * If the jid is not associated with any session, a new one will be created.
     *
     * @param id the jid of the session
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp of(int id){
        return new Whatsapp(id);
    }

    /**
     * Constructs a new instance of the API from the last session opened.
     * If no sessions are available, a new one will be created.
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp ofLast(){
        return new Whatsapp();
    }

    /**
     * Constructs a new instance of the API from a fresh connection using a random jid.
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection(){
        return new Whatsapp(SignalHelper.randomRegistrationId());
    }

    /**
     * Constructs a new instance of the API from the last session opened.
     * If no sessions are available, a new one will be created.
     */
    public Whatsapp(){
        this(requireNonNullElseGet(WhatsappUtils.knownIds().peekLast(), SignalHelper::randomRegistrationId));
    }

    /**
     * Constructs a new instance of the API from a known jid.
     * If the jid is not associated with any session, a new one will be created.
     *
     * @param id the jid of the session
     */
    public Whatsapp(int id){
        this(new Socket(defaultOptions(), WhatsappStore.fromMemory(id), WhatsappKeys.fromMemory(id)));
        RegisterListenerScanner.scan(this)
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
     * Registers a listener manually
     *
     * @param listener the listener to register
     * @return the same instance
     * @throws IllegalArgumentException if the {@code listener} cannot be added
     */
    public Whatsapp registerListener(@NonNull WhatsappListener listener) {
        Validate.isTrue(socket.store().listeners().add(listener),
                "WhatsappAPI: Cannot add listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Removes a listener manually
     *
     * @param listener the listener to remove
     * @return the same instance
     * @throws IllegalArgumentException if the {@code listener} cannot be added
     */
    public Whatsapp removeListener(@NonNull WhatsappListener listener) {
        Validate.isTrue(socket.store().listeners().remove(listener),
                "WhatsappAPI: Cannot remove listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Opens a connection with Whatsapp Web's WebSocket if a previous connection doesn't exist
     *
     * @return the same instance
     */
    public Whatsapp connect() {
        socket.connect();
        return this;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance
     */
    public Whatsapp disconnect() {
        socket.disconnect();
        return this;
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance
     */
    public Whatsapp reconnect() {
        socket.reconnect();
        return this;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket and logs out of WhatsappWeb invalidating the previous saved credentials
     * The next endTimeStamp the API is used, the QR code will need to be scanned again
     *
     * @return the same instance
     */
    public Whatsapp logout() {
        socket.logout();
        return this;
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the endTimeStamp the contact was last seen.
     * To listen to these updates implement;.
     *
     * @param contact the contact whose status the api should receive updates on
     * @return a CompletableFuture    
     */
    public CompletableFuture<?> subscribeToContactPresence(@NonNull Contact contact) {
        var node = withAttributes("presence",
                Map.of("to", contact.jid(), "type", "subscribe"));
        return socket.sendWithNoResponse(node);
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> sendMessage(@NonNull Chat chat, @NonNull String message) {
        return sendMessage(chat, new TextMessage(message));
    }

    /**
     * Builds and sends a message from a chat, a message and a quoted message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the message that; should quote
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> sendMessage(@NonNull Chat chat, @NonNull String message, @NonNull MessageInfo quotedMessage) {
        return sendMessage(chat, new TextMessage(message), quotedMessage);
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> sendMessage(@NonNull Chat chat, @NonNull Message message) {
        var key = MessageKey.newMessageKey()
                .chatJid(chat.jid())
                .fromMe(true)
                .create();
        var info = MessageInfo.newMessageInfo()
                .store(store())
                .key(key)
                .message(MessageContainer.of(message))
                .create();
        return sendMessage(info);
    }

    /**
     * Builds and sends a message from a chat, a message and a quoted message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the message that; should quote
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> sendMessage(@NonNull Chat chat, @NonNull ContextualMessage message, @NonNull MessageInfo quotedMessage) {
        var context = ContextInfo.newContextInfo()
                .quotedMessageId(quotedMessage.id())
                .quotedMessageContainer(quotedMessage.message())
                .quotedMessageSenderId(quotedMessage.senderJid())
                .create();
        return sendMessage(chat, message, context);
    }

    /**
     * Builds and sends a message from a chat, a message and a context
     *
     * @param chat        the chat where the message should be sent
     * @param message     the message to send
     * @param contextInfo the context of the message to send
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> sendMessage(@NonNull Chat chat, @NonNull ContextualMessage message, @NonNull ContextInfo contextInfo) {
        var key = MessageKey.newMessageKey()
                .chatJid(chat.jid())
                .fromMe(true)
                .create();
        var info = MessageInfo.newMessageInfo()
                .store(store())
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
    public CompletableFuture<?> sendMessage(@NonNull MessageInfo message) {
        return socket.sendMessage(message);
    }

    /**
     * Executes a query to determine whether any number of jids have an account on Whatsapp
     *
     * @param contacts the contacts to check
     * @return a CompletableFuture that wraps a non-null list of HasWhatsappResponse
     */
    public CompletableFuture<List<HasWhatsappResponse>> hasWhatsapp(@NonNull ContactJid... contacts) {
        var contactNodes = Arrays.stream(contacts)
                .map(jid -> with("contact", "+%s".formatted(jid.user())))
                .toArray(Node[]::new);
        return socket.sendQuery(with("contact"), withChildren("user", contactNodes))
                .thenApplyAsync(nodes -> nodes.stream().map(HasWhatsappResponse::new).toList());
    }

    /**
     * Queries the block list
     *
     * @return a CompletableFuture that wraps a non-null list of ContactId
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
     * @param contact the target contact
     * @return a CompletableFuture that wraps a non-null list of StatusResponse
     */
    public CompletableFuture<List<StatusResponse>> queryUserStatus(@NonNull Contact contact) {
        var query = with("status");
        var body = withAttributes("user", Map.of("jid", contact.jid()));
        return socket.sendQuery(query, body)
                .thenApplyAsync(response -> Nodes.findAll(response, "status"))
                .thenApplyAsync(nodes -> nodes.stream().map(StatusResponse::new).toList());
    }

    /**
     * Queries the profile picture of a chat.
     *
     * @param chat the chat to query
     * @return a CompletableFuture that wraps nullable jpg url hosted on Whatsapp's servers
     */
    public CompletableFuture<String> queryChatPicture(@NonNull Chat chat) {
        return queryChatPicture(chat.jid());
    }

    /**
     * Queries the profile picture of a chat.
     *
     * @param contact the contact to query
     * @return a CompletableFuture that wraps nullable jpg url hosted on Whatsapp's servers
     */
    public CompletableFuture<String> queryChatPicture(@NonNull Contact contact) {
        return queryChatPicture(contact.jid());
    }

    /**
     * Queries the profile picture of a chat.
     *
     * @param jid the jid of the chat to query
     * @return a CompletableFuture that wraps nullable jpg url hosted on Whatsapp's servers
     */
    public CompletableFuture<String> queryChatPicture(@NonNull ContactJid jid) {
        var body = withAttributes("picture", Map.of("query", "url"));
        return socket.sendQuery("get", "w:profile:picture", Map.of("target", jid), body)
                .thenApplyAsync(this::parseChatPicture);
    }

    private String parseChatPicture(Node result) {
        return Optional.ofNullable(result.findNode("picture"))
                .map(picture -> picture.attributes().getString("url", null))
                .orElse(null);
    }

    /**
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<GroupMetadata> queryGroupMetadata(@NonNull Chat chat) {
        var query = withAttributes("query", Map.of("request", "interactive"));
        return socket.sendQuery(chat.jid(), "get", "w:g2", query)
                .thenApplyAsync(result -> result.findNode("group"))
                .thenApplyAsync(GroupMetadata::of);
    }

    /**
     * Queries the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<String> queryGroupInviteCode(@NonNull Chat chat) {
        return socket.sendQuery(chat.jid(), "get", "w:g2", with("invite"))
                .thenApplyAsync(result -> result.findNode("invite").attributes().getString("code"));
    }

    /**
     * Queries the groups in common with a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> queryGroupsInCommon(@NonNull Contact contact) {
        // Unknown
        return null;
    }

    /**
     * Queries a specified amount of starred/favourite messages in a chat, including ones not in memory
     *
     * @param chat  the target chat
     * @param count the amount of messages
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> queryFavouriteMessagesInChat(@NonNull Chat chat, int count) {
        // Unknown
        return null;
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param available whether you are online or not
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> changePresence(boolean available) {
        var presence = available ? ContactStatus.AVAILABLE : ContactStatus.UNAVAILABLE;
        var node = withAttributes("presence", Map.of("type", presence.data()));
        return socket.sendWithNoResponse(node);
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chat     the target chat
     * @param presence the new status
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> changePresence(@NonNull Chat chat, @NonNull ContactStatus presence) {
        var node = withAttributes("presence", Map.of("to", chat.jid(), "type", presence.data()));
        return socket.sendWithNoResponse(node);
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> promote(@NonNull Chat group, @NonNull Contact... contacts) {
        return null;
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> demote(@NonNull Chat group, @NonNull Contact... contacts) {
        return null;
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> add(@NonNull Chat group, @NonNull Contact... contacts) {
        return null;
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> remove(@NonNull Chat group, @NonNull Contact... contacts) {
        return null;
    }

    /**
     * Executes an sync on any number of contacts represented by a raw list of WhatsappNodes
     *
     * @param group  the target group
     * @param action the sync to execute
     * @param jids   the raw WhatsappNodes representing the contacts the sync should be executed on
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     * @throws IllegalArgumentException if no jids are provided
     */
    public CompletableFuture<?> executeActionOnGroupParticipant(@NonNull Chat group, @NonNull GroupAction action, @NonNull List<Node> jids) {
        return null;
    }

    /**
     * Changes the name of a group
     *
     * @param group   the target group
     * @param newName the new name for the group
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     * @throws IllegalArgumentException if the provided new name is empty or blank
     */
    public CompletableFuture<?> changeGroupName(@NonNull Chat group, @NonNull String newName) {
        return null;
    }

    /**
     * Changes the description of a group
     *
     * @param group          the target group
     * @param newDescription the new name for the group
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> changeGroupDescription(@NonNull Chat group, @NonNull String newDescription) {
        return null;
    }

    /**
     * Changes which category of users can send messages in a group
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> changeWhoCanSendMessagesInGroup(@NonNull Chat group, @NonNull GroupPolicy policy) {
        return null;
    }

    /**
     * Changes which category of users can edit the group's settings
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> changeWhoCanEditGroupInfo(@NonNull Chat group, @NonNull GroupPolicy policy) {
        return null;
    }

    /**
     * Enforces a new policy for a setting in a group
     *
     * @param group   the target group
     * @param setting the target setting
     * @param policy  the new policy to enforce
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> changeGroupSetting(@NonNull Chat group, @NonNull GroupSetting setting, @NonNull GroupPolicy policy) {
        return null;
    }

    /**
     * Changes the picture of a group
     * This is still in beta
     *
     * @param group the target group
     * @param image the new image
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> changeGroupPicture(@NonNull Chat group, byte @NonNull [] image) {
        return null;
    }

    /**
     * Removes the picture of a group
     *
     * @param group the target group
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> removeGroupPicture(@NonNull Chat group) {
        return null;
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> leave(@NonNull Chat group) {
        return null;
    }

    /**
     * Mutes a chat indefinitely
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> mute(@NonNull Chat chat) {
        return null;
    }

    /**
     * Mutes a chat until a specific date
     *
     * @param chat  the target chat
     * @param until the date the mute ends
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> mute(@NonNull Chat chat, @NonNull ZonedDateTime until) {
        return null;
    }

    /**
     * Mutes a chat until a specific date expressed in seconds since the epoch
     *
     * @param chat           the target chat
     * @param untilInSeconds the date the mute ends expressed in seconds since the epoch
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> mute(@NonNull Chat chat, long untilInSeconds) {
        return null;
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> unmute(@NonNull Chat chat) {
        return null;
    }

    /**
     * Blocks a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> block(@NonNull Contact contact) {
        return null;
    }

    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled in said chat after a week
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> enableEphemeralMessages(@NonNull Chat chat) {
        return null;
    }

    /**
     * Disables ephemeral messages in a chat, this means that messages sent in said chat will never be cancelled
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> disableEphemeralMessages(@NonNull Chat chat) {
        return null;
    }

    /**
     * Changes the ephemeral status of a chat, this means that messages will be automatically cancelled in said chat after the provided endTimeStamp
     *
     * @param chat the target chat
     * @param time the endTimeStamp to live for a message expressed in seconds
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> changeEphemeralStatus(@NonNull Chat chat, int time) {
        return null;
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> markAsUnread(@NonNull Chat chat) {
        return null;
    }

    /**
     * Marks a chat as read
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> markAsRead(@NonNull Chat chat) {
        return null;
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
    public CompletableFuture<?> markChat(@NonNull Chat chat, int flag, int newFlag) {
        return null;
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
    public CompletableFuture<?> markChat(@NonNull Chat chat, @NonNull MessageInfo lastMessage, int flag, int newFlag) {
        return null;
    }

    /**
     * Pins a chat to the top.
     * A maximum of three chats can be pinned to the top.
     * This condition can be checked using;.
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> pin(@NonNull Chat chat) {
        return null;
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> unpin(@NonNull Chat chat) {
        return null;
    }

    /**
     * Archives a chat.
     * If said chat is pinned, it will be unpinned.
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> archive(@NonNull Chat chat) {
        return null;
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> unarchive(@NonNull Chat chat) {
        return null;
    }

    /**
     * Creates a new group with the provided name and with at least one contact
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> createGroup(@NonNull String subject, @NonNull Contact... contacts) {
        var participants = Arrays.stream(contacts)
                .map(contact -> withAttributes("participant", Map.of("jid", contact.jid())))
                .toArray(Node[]::new);
        var body = withChildren("create", Map.of("subject", subject, "key", BinaryArray.random(12).toHex()), participants);
        return socket.sendQuery(ContactJid.ofServer(ContactJid.Server.GROUP), "set", "w:g2", body)
                .thenApplyAsync(response -> response.findNode("group"))
                .thenApplyAsync(GroupMetadata::of);
    }

    private void starMessagePlaceholder() {
        // Sent Binary Message Node[description=iq, attributes={xmlns=w:dataSync:app:state, to=s.whatsapp.net, jid=54595.12796-297, type=set}, content=[Node[description=dataSync, attributes={}, content=[Node[description=collection, attributes={name=regular_high, return_snapshot=false, version=13}, content=[Node[description=patch, attributes={}, content=[B@1cd3e518]]]]]]]
        // Received Binary Message Node[description=iq, attributes={from=s.whatsapp.net, jid=54595.12796-297, type=result}, content=[Node[description=dataSync, attributes={}, content=[Node[description=collection, attributes={name=regular_high, version=14}, content=null]]]]]
    }

    private void unstarMessagePlaceholder() {
        // Sent Binary Message Node[description=iq, attributes={xmlns=w:dataSync:app:state, to=s.whatsapp.net, jid=54595.12796-301, type=set}, content=[Node[description=dataSync, attributes={}, content=[Node[description=collection, attributes={name=regular_high, return_snapshot=false, version=14}, content=[Node[description=patch, attributes={}, content=[B@73ce9a0b]]]]]]]
        // Received Binary Message Node[description=iq, attributes={from=s.whatsapp.net, jid=54595.12796-301, type=result}, content=[Node[description=dataSync, attributes={}, content=[Node[description=collection, attributes={name=regular_high, version=15}, content=null]]]]]
    }
}
