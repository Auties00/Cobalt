package com.github.auties00.cobalt.api;

import com.github.auties00.cobalt.io.core.node.Node;
import com.github.auties00.cobalt.model.action.Action;
import com.github.auties00.cobalt.model.call.Call;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.chat.ChatPastParticipant;
import com.github.auties00.cobalt.model.contact.Contact;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.MessageIndexInfo;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.info.QuotedMessageInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.newsletter.Newsletter;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntry;
import com.github.auties00.cobalt.model.setting.Setting;

import java.util.Collection;
import java.util.List;

/**
 * An event listener interface for WhatsApp communication events.
 * <p>
 * This interface provides callback methods for various events that occur during
 * a WhatsApp session, such as message reception, connection state changes, and data updates.
 * <p>
 * Register a listener using {@link Whatsapp#addListener(WhatsappListener)} to receive these events.
 * All methods have empty default implementations, allowing you to override only the ones you need.
 *
 * @see Whatsapp#addListener(WhatsappListener)
 * @see Whatsapp#removeListener(WhatsappListener)
 */
public interface WhatsappListener {
    /**
     * Called when a node is sent to the WhatsApp server.
     *
     * @param whatsapp an instance of the calling API
     * @param outgoing the non-null node that was sent
     */
    default void onNodeSent(Whatsapp whatsapp, Node outgoing) {
    }

    /**
     * Called when a node is received from the WhatsApp server.
     *
     * @param whatsapp an instance of the calling API
     * @param incoming the non-null node that was received
     */
    default void onNodeReceived(Whatsapp whatsapp, Node incoming) {
    }

    /**
     * Called when a successful connection and login to a WhatsApp account is established.
     * <p>
     * Note: When this event is fired, data such as chats and contacts may not yet be loaded
     * into memory. For specific data types, use the corresponding event handlers:
     * {@link #onChats(Whatsapp, Collection)}, {@link #onContacts(Whatsapp, Collection)}, etc.
     *
     * @param whatsapp an instance of the calling API
     */
    default void onLoggedIn(Whatsapp whatsapp) {
    }

    /**
     * Called when the connection to WhatsApp is terminated.
     *
     * @param whatsapp an instance of the calling API
     * @param reason   the reason for disconnection, indicating why the session was terminated
     * @see WhatsappDisconnectReason
     */
    default void onDisconnected(Whatsapp whatsapp, WhatsappDisconnectReason reason) {
    }

    /**
     * Called when an action is received from WhatsApp Web.
     * <p>
     * This event is only triggered for web client connections.
     *
     * @param whatsapp         an instance of the calling API
     * @param action           the action that was executed
     * @param messageIndexInfo the data associated with this action
     */
    default void onWebAppStateAction(Whatsapp whatsapp, Action action, MessageIndexInfo messageIndexInfo) {
    }

    /**
     * Called when a setting is received from WhatsApp Web.
     * <p>
     * This event is only triggered for web client connections.
     *
     * @param whatsapp an instance of the calling API
     * @param setting  the setting that was toggled
     */
    default void onWebAppStateSetting(Whatsapp whatsapp, Setting setting) {
    }

    /**
     * Called when primary features are received from WhatsApp Web.
     * <p>
     * This event is only triggered for web client connections.
     *
     * @param whatsapp an instance of the calling API
     * @param features the non-null collection of features that were sent
     */
    default void onWebAppPrimaryFeatures(Whatsapp whatsapp, List<String> features) {
    }

    /**
     * Called when all contacts are received from WhatsApp.
     *
     * @param whatsapp an instance of the calling API
     * @param contacts the collection of contacts
     */
    default void onContacts(Whatsapp whatsapp, Collection<Contact> contacts) {
    }

    /**
     * Called when a contact's presence status is updated.
     *
     * @param whatsapp     an instance of the calling API
     * @param conversation the chat related to this presence update
     * @param participant  the contact whose presence status changed
     */
    default void onContactPresence(Whatsapp whatsapp, Jid conversation, Jid participant) {
    }

    /**
     * Called when all chats are received from WhatsApp.
     * <p>
     * When this event is fired, all chat metadata is available, excluding message children.
     * For message children, refer to {@link #onWebHistorySyncMessages(Whatsapp, Chat, boolean)}.
     * Note that particularly old chats may be loaded later through the history sync process.
     *
     * @param whatsapp an instance of the calling API
     * @param chats    the collection of chats
     */
    default void onChats(Whatsapp whatsapp, Collection<Chat> chats) {
    }

    /**
     * Called when all newsletters are received from WhatsApp.
     *
     * @param whatsapp    an instance of the calling API
     * @param newsletters the collection of newsletters
     */
    default void onNewsletters(Whatsapp whatsapp, Collection<Newsletter> newsletters) {
    }

    /**
     * Called when messages for a chat are received during history synchronization.
     * <p>
     * This event is only triggered during initial QR code scanning and history syncing.
     * In subsequent connections, messages will already be loaded in the chats.
     *
     * @param whatsapp an instance of the calling API
     * @param chat     the chat being synchronized
     * @param last     true if these are the final messages for this chat, false if more are coming
     */
    default void onWebHistorySyncMessages(Whatsapp whatsapp, Chat chat, boolean last) {
    }

    /**
     * Called when past participants for a group are received during history synchronization.
     *
     * @param whatsapp             an instance of the calling API
     * @param chatJid              the non-null group chat JID
     * @param chatPastParticipants the non-null collection of past participants
     */
    default void onWebHistorySyncPastParticipants(Whatsapp whatsapp, Jid chatJid, Collection<ChatPastParticipant> chatPastParticipants) {
    }

    /**
     * Called with the progress of the history synchronization process.
     * <p>
     * This event is only triggered during initial QR code scanning and history syncing.
     *
     * @param whatsapp   an instance of the calling API
     * @param percentage the percentage of synchronization completed
     * @param recent     true if syncing recent messages, false if syncing older messages
     */
    default void onWebHistorySyncProgress(Whatsapp whatsapp, int percentage, boolean recent) {
    }

    /**
     * Called when a new message is received.
     *
     * @param whatsapp an instance of the calling API
     * @param info     the message that was received
     */
    default void onNewMessage(Whatsapp whatsapp, MessageInfo info) {
    }

    /**
     * Called when a message is deleted.
     *
     * @param whatsapp an instance of the calling API
     * @param info     the message that was deleted
     * @param everyone true if the message was deleted for everyone, false if deleted only for the user
     */
    default void onMessageDeleted(Whatsapp whatsapp, MessageInfo info, boolean everyone) {
    }

    /**
     * Called when a message's status changes (e.g., sent, delivered, read).
     *
     * @param whatsapp an instance of the calling API
     * @param info     the message whose status changed
     */
    default void onMessageStatus(Whatsapp whatsapp, MessageInfo info) {
    }

    /**
     * Called when all status updates are received from WhatsApp.
     *
     * @param whatsapp an instance of the calling API
     * @param status   the collection of status updates
     */
    default void onStatus(Whatsapp whatsapp, Collection<ChatMessageInfo> status) {
    }

    /**
     * Called when a new status update is received.
     *
     * @param whatsapp an instance of the calling API
     * @param status   the new status message
     */
    default void onNewStatus(Whatsapp whatsapp, ChatMessageInfo status) {
    }

    /**
     * Called when a message is sent in reply to a previous message.
     *
     * @param whatsapp an instance of the calling API
     * @param response the reply message
     * @param quoted   the message being replied to
     */
    default void onMessageReply(Whatsapp whatsapp, MessageInfo response, QuotedMessageInfo quoted) {
    }

    /**
     * Called when a contact's profile picture changes.
     *
     * @param whatsapp an instance of the calling API
     * @param jid      the contact whose profile picture changed
     */
    default void onProfilePictureChanged(Whatsapp whatsapp, Jid jid) {
    }

    /**
     * Called when the user's display name changes.
     *
     * @param whatsapp an instance of the calling API
     * @param oldName  the non-null previous name
     * @param newName  the non-null new name
     */
    default void onNameChanged(Whatsapp whatsapp, String oldName, String newName) {
    }

    /**
     * Called when the user's about/status text changes.
     *
     * @param whatsapp an instance of the calling API
     * @param oldAbout the non-null previous about text
     * @param newAbout the non-null new about text
     */
    default void onAboutChanged(Whatsapp whatsapp, String oldAbout, String newAbout) {
    }

    /**
     * Called when the user's locale settings change.
     *
     * @param whatsapp  an instance of the calling API
     * @param oldLocale the non-null previous locale
     * @param newLocale the non-null new locale
     */
    default void onLocaleChanged(Whatsapp whatsapp, String oldLocale, String newLocale) {
    }

    /**
     * Called when a contact is blocked or unblocked.
     *
     * @param whatsapp an instance of the calling API
     * @param contact  the non-null contact that was blocked or unblocked
     */
    default void onContactBlocked(Whatsapp whatsapp, Jid contact) {
    }

    /**
     * Called when a new contact is added to the contact list.
     *
     * @param whatsapp an instance of the calling API
     * @param contact  the new contact
     */
    default void onNewContact(Whatsapp whatsapp, Contact contact) {
    }

    /**
     * Called when a privacy setting is changed.
     *
     * @param whatsapp        an instance of the calling API
     * @param newPrivacyEntry the new privacy setting
     */
    default void onPrivacySettingChanged(Whatsapp whatsapp, PrivacySettingEntry newPrivacyEntry) {
    }

    /**
     * Called when a registration code (OTP) is requested from a new device.
     * <p>
     * Note: This event is only triggered for the mobile API.
     *
     * @param whatsapp an instance of the calling API
     * @param code     the registration code
     */
    default void onRegistrationCode(Whatsapp whatsapp, long code) {
    }

    /**
     * Called when a phone call is received.
     *
     * @param whatsapp an instance of the calling API
     * @param call     the non-null phone call information
     */
    default void onCall(Whatsapp whatsapp, Call call) {
    }
}