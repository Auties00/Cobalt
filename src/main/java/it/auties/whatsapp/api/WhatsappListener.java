package it.auties.whatsapp.api;

import it.auties.whatsapp.binary.BinarySocket;
import it.auties.whatsapp.controller.WhatsappStore;
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
 * A WhatsappListener can be registered manually using {@link Whatsapp#registerListener(WhatsappListener)}.
 * Otherwise, it can be registered by annotating it with the {@link RegisterListener} annotation.
 */
@SuppressWarnings("unused")
public interface WhatsappListener {
    /**
     * Called when {@link BinarySocket} sends a node to Whatsapp
     *
     * @param outgoing the non-null node that was just sent
     */
    default void onNodeSent(Node outgoing) {

    }

    /**
     * Called when {@link BinarySocket} receives a node from Whatsapp
     *
     * @param incoming the non-null node that was just received
     */
    default void onNodeReceived(Node incoming) {

    }

    /**
     * Called when {@link BinarySocket} successfully establishes a connection with new secrets.
     * By default, the QR code is printed to the console.
     *
     * @return a non-null handler to process the qr code
     */
    default QrHandler onQRCode() {
        return QrHandler.toTerminal();
    }

    /**
     * Called when {@link BinarySocket} successfully establishes a connection and logs in into an account.
     * When this event is called, any data, including chats and contact, is not guaranteed to be already in memory.
     * Instead, {@link WhatsappListener#onChats()} and {@link WhatsappListener#onContacts()} should be used.
     */
    default void onLoggedIn() {

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
     * Called when {@link BinarySocket} successfully disconnects from WhatsappWeb's WebSocket
     *
     * @param reconnect whether the connection is going to be re-established
     */
    default void onDisconnected(boolean reconnect) {

    }

    /**
     * Called when {@link BinarySocket} receives an sync from Whatsapp.
     *
     * @param action the sync that was executed
     */
    default void onAction(Action action) {

    }

    /**
     * Called when {@link BinarySocket} receives a setting change from Whatsapp.
     *
     * @param setting the setting that was toggled
     */
    default void onSetting(Setting setting) {

    }

    /**
     * Called when {@link BinarySocket} receives new features from Whatsapp.
     *
     * @param features the non-null features that were sent
     */
    default void onFeatures(List<String> features) {

    }

    /**
     * Called when {@link BinarySocket} receives all the contacts from WhatsappWeb's WebSocket.
     * To access this data use {@link WhatsappStore#contacts()}.
     */
    default void onContacts() {

    }

    /**
     * Called when {@link BinarySocket} receives a new contact
     *
     * @param contact the new contact
     */
    default void onNewContact(Contact contact) {

    }

    /**
     * Called when {@link BinarySocket} receives an update regarding the presence of a contact
     *
     * @param chat    the chat that this update regards
     * @param contact the contact that this update regards
     * @param status  the new status of the contact
     */
    default void onContactPresence(Chat chat, Contact contact, ContactStatus status) {

    }

    /**
     * Called when {@link BinarySocket} receives all the chats from WhatsappWeb's WebSocket.
     * When this event is fired, it is guaranteed that all metadata excluding messages will be present.
     * To access this data use {@link WhatsappStore#chats()}.
     * If you also need the messages to be loaded, please refer to {@link WhatsappListener#onChatRecentMessages(Chat)}.
     */
    default void onChats() {

    }

    /**
     * Called when {@link BinarySocket} receives the recent message for a chat.
     * This method may be called multiple times depending on the chat's size.
     */
    default void onChatRecentMessages(Chat chat) {

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
     * @param info     the message that was deleted
     * @param everyone whether this message was deleted by you only for yourself or whether the message was permanently removed
     */
    default void onMessageDeleted(MessageInfo info, boolean everyone) {

    }

    /**
     * Called when the status of a message changes inside a conversation.
     * This means that the status change can be considered global as the only other participant is the contact.
     * If you need updates regarding any chat, implement {@link WhatsappListener#onMessageStatus(Chat, Contact, MessageInfo, MessageStatus)}
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
     * If you only need updates regarding conversation, implement {@link WhatsappListener#onMessageStatus(MessageInfo, MessageStatus)}.
     *
     * @param chat    the chat that triggered a status change
     * @param contact the contact that triggered a status change
     * @param info    the message whose status changed
     * @param status  the new status of the message
     */
    default void onMessageStatus(Chat chat, Contact contact, MessageInfo info, MessageStatus status) {

    }

    /**
     * Called when {@link BinarySocket} receives all the status updated from WhatsappWeb's Socket.
     * To access this data use {@link WhatsappStore#status()}.
     */
    default void onStatus() {

    }

    /**
     * Called when {@link BinarySocket} receives a new status from WhatsappWeb's Socket
     *
     * @param status the new status message
     */
    default void onNewStatus(MessageInfo status) {

    }
}
