package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.api.SocketEvent;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.setting.Setting;

import java.util.List;
import java.util.Map;

/**
 * This interface can be used to listen for events fired when new information is sent by WhatsappWeb's socket.
 * A listener can be registered manually using {@link Whatsapp#addListener(Listener)}.
 * Otherwise, it can be registered by annotating it with the {@link RegisterListener} annotation.
 * To disable the latter, check out {@link it.auties.whatsapp.api.Whatsapp.Options#autodetectListeners()}.
 */
@SuppressWarnings("unused")
public interface Listener {
    /**
     * Called when the socket sends a node to Whatsapp
     *
     * @param whatsapp an instance to the calling api
     * @param outgoing the non-null node that was just sent
     */
    default void onNodeSent(Whatsapp whatsapp, Node outgoing) {

    }

    /**
     * Called when the socket sends a node to Whatsapp
     *
     * @param outgoing the non-null node that was just sent
     */
    default void onNodeSent(Node outgoing) {

    }

    /**
     * Called when the socket receives a node from Whatsapp
     *
     * @param whatsapp an instance to the calling api
     * @param incoming the non-null node that was just received
     */
    default void onNodeReceived(Whatsapp whatsapp, Node incoming) {

    }

    /**
     * Called when the socket receives a node from Whatsapp
     *
     * @param incoming the non-null node that was just received
     */
    default void onNodeReceived(Node incoming) {

    }

    /**
     * Called when the socket successfully establishes a connection and logs in into an account.
     * When this event is called, any data, including chats and contact, is not guaranteed to be already in memory.
     * Instead, {@link Listener#onChats()} and {@link Listener#onContacts()} should be used.
     */
    default void onLoggedIn(Whatsapp whatsapp) {

    }

    /**
     * Called when the socket successfully establishes a connection and logs in into an account.
     * When this event is called, any data, including chats and contact, is not guaranteed to be already in memory.
     * Instead, {@link Listener#onChats()} and {@link Listener#onContacts()} should be used.
     */
    default void onLoggedIn() {

    }

    /**
     * Called when an updated list of properties is received.
     * This method is called both when a connection is established with WhatsappWeb and when new props are available.
     * In the latter case though, this object should be considered as partial and is guaranteed to contain only updated entries.
     *
     * @param whatsapp an instance to the calling api
     * @param metadata the updated list of properties
     */
    default void onMetadata(Whatsapp whatsapp, Map<String, String> metadata) {

    }

    /**
     * Called when an updated list of properties is received.
     * This method is called both when a connection is established with WhatsappWeb and when new props are available.
     * In the latter case though, this object should be considered as partial and is guaranteed to contain only updated entries.
     *
     * @param metadata the updated list of properties
     */
    default void onMetadata(Map<String, String> metadata) {

    }

    /**
     * Called when the socket successfully disconnects from WhatsappWeb's WebSocket
     *
     * @param whatsapp an instance to the calling api
     * @param reason   the reason why the session was disconnected
     */
    default void onDisconnected(Whatsapp whatsapp, DisconnectReason reason) {

    }


    /**
     * Called when the socket successfully disconnects from WhatsappWeb's WebSocket
     *
     * @param reason the reason why the session was disconnected
     */
    default void onDisconnected(DisconnectReason reason) {

    }

    /**
     * Called when the socket receives an sync from Whatsapp.
     *
     * @param whatsapp an instance to the calling api
     * @param action   the sync that was executed
     */
    default void onAction(Whatsapp whatsapp, Action action) {

    }

    /**
     * Called when the socket receives an sync from Whatsapp.
     *
     * @param action the sync that was executed
     */
    default void onAction(Action action) {

    }

    /**
     * Called when the socket receives a setting change from Whatsapp.
     *
     * @param whatsapp an instance to the calling api
     * @param setting  the setting that was toggled
     */
    default void onSetting(Whatsapp whatsapp, Setting setting) {

    }

    /**
     * Called when the socket receives a setting change from Whatsapp.
     *
     * @param setting the setting that was toggled
     */
    default void onSetting(Setting setting) {

    }

    /**
     * Called when the socket receives new features from Whatsapp.
     *
     * @param whatsapp an instance to the calling api
     * @param features the non-null features that were sent
     */
    default void onFeatures(Whatsapp whatsapp, List<String> features) {

    }


    /**
     * Called when the socket receives new features from Whatsapp.
     *
     * @param features the non-null features that were sent
     */
    default void onFeatures(List<String> features) {

    }

    /**
     * Called when the socket receives all the contacts from WhatsappWeb's WebSocket.
     * To access this data use {@link Store#contacts()}.
     *
     * @param whatsapp an instance to the calling api
     */
    default void onContacts(Whatsapp whatsapp) {

    }

    /**
     * Called when the socket receives all the contacts from WhatsappWeb's WebSocket.
     * To access this data use {@link Store#contacts()}.
     */
    default void onContacts() {

    }

    /**
     * Called when the socket receives a new contact
     *
     * @param whatsapp an instance to the calling api
     * @param contact  the new contact
     */
    default void onNewContact(Whatsapp whatsapp, Contact contact) {

    }

    /**
     * Called when the socket receives a new contact
     *
     * @param contact the new contact
     */
    default void onNewContact(Contact contact) {

    }

    /**
     * Called when the socket receives an update regarding the presence of a contact
     *
     * @param whatsapp an instance to the calling api
     * @param chat     the chat that this update regards
     * @param contact  the contact that this update regards
     * @param status   the new status of the contact
     */
    default void onContactPresence(Whatsapp whatsapp, Chat chat, Contact contact, ContactStatus status) {

    }

    /**
     * Called when the socket receives an update regarding the presence of a contact
     *
     * @param chat    the chat that this update regards
     * @param contact the contact that this update regards
     * @param status  the new status of the contact
     */
    default void onContactPresence(Chat chat, Contact contact, ContactStatus status) {

    }

    /**
     * Called when the socket receives all the chats from WhatsappWeb's WebSocket.
     * When this event is fired, it is guaranteed that all metadata excluding messages will be present.
     * To access this data use {@link Store#chats()}.
     * If you also need the messages to be loaded, please refer to {@link Listener#onChatMessagesSync(Chat, boolean)}.
     * Particularly old chats may come later through {@link Listener#onChatMessagesSync(Chat, boolean)}
     *
     * @param whatsapp an instance to the calling api
     */
    default void onChats(Whatsapp whatsapp) {

    }

    /**
     * Called when the socket receives all the chats from WhatsappWeb's WebSocket.
     * When this event is fired, it is guaranteed that all metadata excluding messages will be present.
     * To access this data use {@link Store#chats()}.
     * If you also need the messages to be loaded, please refer to {@link Listener#onChatMessagesSync(Chat, boolean)}.
     * Particularly old chats may come later through {@link Listener#onChatMessagesSync(Chat, boolean)}
     */
    default void onChats() {

    }

    /**
     * Called when the socket receives the messages for a chat.
     * This method is only called when the QR is first scanned and history is being synced.
     * From all subsequent runs, the messages will already in the chat on startup.
     *
     * @param whatsapp an instance to the calling api
     * @param chat     the chat
     * @param last     whether the messages in this chat are complete or there are more coming
     */
    default void onChatMessagesSync(Whatsapp whatsapp, Chat chat, boolean last) {

    }

    /**
     * Called when the socket receives the message for a chat
     * This method is only called when the QR is first scanned and history is being synced.
     * From all subsequent runs, the messages will already in the chat on startup.
     *
     * @param chat the chat
     * @param last whether the messages in this chat are complete or there are more coming
     */
    default void onChatMessagesSync(Chat chat, boolean last) {

    }

    /**
     * Called when the socket receives the sync percentage for the full or recent chunk of messages.
     * This method is only called when the QR is first scanned and history is being synced.
     *
     * @param percentage the percentage synced up to now
     * @param recent whether the sync is about the recent messages or older messages
     */
    default void onHistorySyncProgress(int percentage, boolean recent){

    }

    /**
     * Called when the socket receives the sync percentage for the full or recent chunk of messages.
     * This method is only called when the QR is first scanned and history is being synced.
     *
     * @param whatsapp an instance to the calling api
     * @param percentage the percentage synced up to now
     * @param recent whether the sync is about the recent messages or older messages
     */
    default void onHistorySyncProgress(Whatsapp whatsapp, int percentage, boolean recent){

    }

    /**
     * Called when a new message is received in a chat
     *
     * @param whatsapp an instance to the calling api
     * @param info     the message that was sent
     */
    default void onNewMessage(Whatsapp whatsapp, MessageInfo info) {

    }

    /**
     * Called when a new message is received in a chat
     *
     * @param info the message that was sent
     */
    default void onNewMessage(MessageInfo info) {

    }

    /**
     * Called when a message is deleted
     *
     * @param whatsapp an instance to the calling api
     * @param info     the message that was deleted
     * @param everyone whether this message was deleted by you only for yourself or whether the message was permanently removed
     */
    default void onMessageDeleted(Whatsapp whatsapp, MessageInfo info, boolean everyone) {

    }

    /**
     * Called when a message is deleted
     *
     * @param info     the message that was deleted
     * @param everyone whether this message was deleted by you only for yourself or whether the message was permanently removed
     */
    default void onMessageDeleted(MessageInfo info, boolean everyone) {

    }

    /**
     * Called when the status of a message changes inside a conversation.
     * This means that the status change can be considered global as the only other participant is the contact.
     * If you need updates regarding any chat, implement {@link Listener#onMessageStatus(Chat, Contact, MessageInfo, MessageStatus)}
     *
     * @param whatsapp an instance to the calling api
     * @param info     the message whose status changed
     * @param status   the new status of the message
     */
    default void onMessageStatus(Whatsapp whatsapp, MessageInfo info, MessageStatus status) {

    }

    /**
     * Called when the status of a message changes inside a conversation.
     * This means that the status change can be considered global as the only other participant is the contact.
     * If you need updates regarding any chat, implement {@link Listener#onMessageStatus(Chat, Contact, MessageInfo, MessageStatus)}
     *
     * @param info   the message whose status changed
     * @param status the new status of the message
     */
    default void onMessageStatus(MessageInfo info, MessageStatus status) {

    }

    /**
     * Called when the status of a message changes inside any type of chat.
     * If {@code chat} is a conversation with {@code contact}, the new read status can be considered valid for the message itself(global status).
     * Otherwise, it should be considered valid only for {@code contact}.
     * If you only need updates regarding conversation, implement {@link Listener#onMessageStatus(MessageInfo, MessageStatus)}.
     *
     * @param whatsapp an instance to the calling api
     * @param chat     the chat that triggered a status change
     * @param contact  the contact that triggered a status change
     * @param info     the message whose status changed
     * @param status   the new status of the message
     */
    default void onMessageStatus(Whatsapp whatsapp, Chat chat, Contact contact, MessageInfo info,
                                 MessageStatus status) {

    }

    /**
     * Called when the status of a message changes inside any type of chat.
     * If {@code chat} is a conversation with {@code contact}, the new read status can be considered valid for the message itself(global status).
     * Otherwise, it should be considered valid only for {@code contact}.
     * If you only need updates regarding conversation, implement {@link Listener#onMessageStatus(MessageInfo, MessageStatus)}.
     *
     * @param chat    the chat that triggered a status change
     * @param contact the contact that triggered a status change
     * @param info    the message whose status changed
     * @param status  the new status of the message
     */
    default void onMessageStatus(Chat chat, Contact contact, MessageInfo info, MessageStatus status) {

    }

    /**
     * Called when the socket receives all the status updated from WhatsappWeb's Socket.
     * To access this data use {@link Store#status()}.
     *
     * @param whatsapp an instance to the calling api
     */
    default void onStatus(Whatsapp whatsapp) {

    }

    /**
     * Called when the socket receives all the status updated from WhatsappWeb's Socket.
     * To access this data use {@link Store#status()}.
     */
    default void onStatus() {

    }

    /**
     * Called when the socket receives a new status from WhatsappWeb's Socket
     *
     * @param whatsapp an instance to the calling api
     * @param status   the new status message
     */
    default void onNewStatus(Whatsapp whatsapp, MessageInfo status) {

    }

    /**
     * Called when the socket receives a new status from WhatsappWeb's Socket
     *
     * @param status the new status message
     */
    default void onNewStatus(MessageInfo status) {

    }

    /**
     * Called when an event regarding the underlying is fired
     *
     * @param whatsapp an instance to the calling api
     * @param event    the event
     */
    default void onSocketEvent(Whatsapp whatsapp, SocketEvent event) {

    }

    /**
     * Called when an event regarding the underlying is fired
     *
     * @param event the event
     */
    default void onSocketEvent(SocketEvent event) {

    }
}
