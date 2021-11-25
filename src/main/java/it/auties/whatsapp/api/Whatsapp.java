package it.auties.whatsapp.api;

import it.auties.whatsapp.api.RegisterListener.Scanner;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.chat.GroupAction;
import it.auties.whatsapp.protobuf.chat.GroupPolicy;
import it.auties.whatsapp.protobuf.chat.GroupSetting;
import it.auties.whatsapp.protobuf.contact.Contact;
import it.auties.whatsapp.protobuf.contact.ContactStatus;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp.protobuf.message.model.Message;
import it.auties.whatsapp.protobuf.model.Node;
import it.auties.whatsapp.utils.Validate;
import lombok.NonNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A class used to interface a user to WhatsappWeb's WebSocket.
 * It provides various functionalities, including the possibility to query, set and modify data associated with the loaded session of whatsapp.
 * It can be configured using a default configuration or a custom one.
 * Multiple instances of this class can be initialized, though it is not advisable as; is a singleton and cannot distinguish between the data associated with each session.
 */
public record Whatsapp(WhatsappStore store) {
    public Whatsapp(){
        this(new WhatsappStore());
        Scanner.scan(this)
                .forEach(this::registerListener);
    }

    /**
     * Registers a listener manually
     *
     * @param listener the listener to register
     * @return the same instance
     * @throws IllegalArgumentException if the {@code listener} cannot be added
     */
    public Whatsapp registerListener(@NonNull WhatsappListener listener) {
        Validate.isTrue(store.listeners().add(listener),
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
        Validate.isTrue(store.listeners().remove(listener),
                "WhatsappAPI: Cannot remove listener %s", listener.getClass().getName());
        return this;
    }

    /**
     * Opens a connection with Whatsapp Web's WebSocket if a previous connection doesn't exist
     *
     * @return the same instance
     * @throws IllegalStateException if a previous connection already exists
     */
    public Whatsapp connect() {
        return null;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance
     * @throws IllegalStateException if a previous connection doesn't exist
     */
    public Whatsapp disconnect() {
        return null;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket and logs out of WhatsappWeb invalidating the previous saved credentials
     * The next time the API is used, the QR code will need to be scanned again
     *
     * @return the same instance
     * @throws IllegalStateException if a previous connection doesn't exist
     */
    public Whatsapp logout() {
        return null;
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance
     * @throws IllegalStateException if a previous connection doesn't exist
     */
    public Whatsapp reconnect() {
        return null;
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the time the contact was last seen.
     * To listen to these updates implement;.
     *
     * @param contact the contact whose status the api should receive updates on
     * @return a CompletableFuture    
     */
    public CompletableFuture<?> subscribeToContactPresence(@NonNull Contact contact) {
        return null;
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> sendMessage(@NonNull Chat chat, @NonNull String message) {
        return null;
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
        return null;
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> sendMessage(@NonNull Chat chat, @NonNull Message message) {
        return null;
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
        return null;
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
        return null;
    }

    /**
     * Sends a message info to a chat
     *
     * @param message the message to send
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> sendMessage(@NonNull MessageInfo message) {
        return null;
    }

    /**
     * Executes a query to determine whether a Whatsapp account linked
     * to the supplied phone number exists.
     *
     * @param phoneNumber the phone number to check
     * @return a CompletableFuture 
     */
    public CompletableFuture<Boolean> hasWhatsapp(@NonNull String phoneNumber) {
        return null;
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param contact the target contact
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> queryUserStatus(@NonNull Contact contact) {
        return null;
    }

    /**
     * Queries the profile picture of a Chat
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> queryChatPicture(@NonNull Chat chat) {
        return null;
    }

    /**
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> queryGroupMetadata(@NonNull Chat chat) {
        return null;
    }

    /**
     * Queries the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture     
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<?> queryGroupInviteCode(@NonNull Chat chat) {
        return null;
    }

    /**
     * Queries the groups in common with a contact
     *
     * @param contact the target contact
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> queryGroupsInCommon(@NonNull Contact contact) {
        return null;
    }

    /**
     * Queries a chat that is not in memory associated with a contact.
     * This method does not add said chat to; automatically.
     *
     * @param contact the target contact
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> queryChat(@NonNull Contact contact) {
        return null;
    }

    /**
     * Queries a chat that is not in memory associated by its jid.
     * This method does not add said chat to; automatically.
     *
     * @param jid the target jid
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> queryChat(@NonNull String jid) {
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
        return null;
    }

    /**
     * Tries to load in chat the entire chat history for a chat.
     * This process might take several minutes for chats that contain thousands of messages.
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> loadEntireChatHistory(@NonNull Chat chat) {
        return null;
    }


    /**
     * Loads in memory twenty messages before the last message in memory for a chat in chronological terms
     *
     * @param chat the target chat
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> loadChatHistory(@NonNull Chat chat) {
        return null;
    }

    /**
     * Loads in memory a provided number of messages before the last message in memory for a chat in chronological terms
     *
     * @param chat         the target chat
     * @param messageCount the number of messages to load
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> loadChatHistory(@NonNull Chat chat, int messageCount) {
        return null;
    }

    /**
     * Loads in memory a provided number of messages before a provided message in memory for a chat in chronological terms
     *
     * @param chat         the target chat
     * @param lastMessage  the last message that should be queried chronologically, exclusive
     * @param messageCount the amount of messages to load
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> loadChatHistory(@NonNull Chat chat, @NonNull MessageInfo lastMessage, int messageCount) {
        return null;
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param presence the new status
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> changePresence(@NonNull ContactStatus presence) {
        return null;
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chat     the target chat
     * @param presence the new status
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> changePresence(@NonNull Chat chat, @NonNull ContactStatus presence) {
        return null;
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
     * Executes an action on any number of contacts represented by a raw list of WhatsappNodes
     *
     * @param group  the target group
     * @param action the action to execute
     * @param jids   the raw WhatsappNodes representing the contacts the action should be executed on
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
     * Changes the ephemeral status of a chat, this means that messages will be automatically cancelled in said chat after the provided time
     *
     * @param chat the target chat
     * @param time the time to live for a message expressed in seconds
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
     * @throws IllegalArgumentException if the name is blank
     * @throws IllegalArgumentException if at least one contact isn't provided
     * @throws IllegalArgumentException if; contains your jid
     * @throws IllegalStateException    inside the CompletableFuture if the contact cannot be created
     */
    public CompletableFuture<?> createGroup(@NonNull String subject, @NonNull Contact... contacts) {
        return null;
    }

    /**
     * Searches for a specific amount of messages globally, including data that is not in memory.
     * If there are too many result the {@code message} parameter should be specified in order to view the next pages.
     *
     * @param search the keyword to search
     * @param count  the number of messages to query
     * @param page   the page to query
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> search(@NonNull String search, int count, int page) {
        return null;
    }

    /**
     * Searches for a specific amount of messages in a specific chat's messages, including ones that are not in memory
     * If there are too many result the {@code message} parameter should be specified in order to view the next pages
     *
     * @param search the keyword to search
     * @param chat   the target chat
     * @param count  the number of messages to query
     * @param page   the page to query
     * @return a CompletableFuture 
     */
    public CompletableFuture<?> searchInChat(@NonNull String search, @NonNull Chat chat, int count, int page) {
        return null;
    }
}
