package it.auties.whatsapp.api;

import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatPastParticipant;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.info.QuotedMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.mobile.CountryLocale;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.setting.Setting;

import java.util.Collection;
import java.util.List;

/**
 * This interface can be used to listen for events fired when new information is sent by
 * WhatsApp. A listener can be registered using {@link Whatsapp#addListener(WhatsappListener)}.
 */
public interface WhatsappListener {
    /**
     * Called when the socket sends a node to WhatsApp
     *
     * @param whatsapp an instance to the calling api
     * @param outgoing the non-null node that was just sent
     */
    default void onNodeSent(Whatsapp whatsapp, Node outgoing) {
    }

    /**
     * Called when the socket sends a node to WhatsApp
     *
     * @param outgoing the non-null node that was just sent
     */
    default void onNodeSent(Node outgoing) {
    }

    /**
     * Called when the socket receives a node from WhatsApp
     *
     * @param whatsapp an instance to the calling api
     * @param incoming the non-null node that was just received
     */
    default void onNodeReceived(Whatsapp whatsapp, Node incoming) {
    }

    /**
     * Called when the socket receives a node from WhatsApp
     *
     * @param incoming the non-null node that was just received
     */
    default void onNodeReceived(Node incoming) {
    }

    /**
     * Called when the socket successfully establishes a connection and logs in into an account. When
     * this event is called, any data, including chats and contact, is not guaranteed to be already in
     * memory. Prefer using the event that corresponds to that specific type of data: for example, for chats
     * use {@link WhatsappListener#onChats(Whatsapp, Collection)}. 
     *
     * @param whatsapp an instance to the calling api
     */
    default void onLoggedIn(Whatsapp whatsapp) {
    }

    /**
     * Called when the socket successfully establishes a connection and logs in into an account. When
     * this event is called, any data, including chats and contact, is not guaranteed to be already in
     * memory. Prefer using the event that corresponds to that specific type of data: for example, for chats
     * use {@link WhatsappListener#onChats(Whatsapp, Collection)}. 
     */
    default void onLoggedIn() {
    }

    /**
     * Called when the socket successfully disconnects from WhatsApp
     *
     * @param whatsapp an instance to the calling api
     * @param reason   the errorReason why the session was disconnected
     */
    default void onDisconnected(Whatsapp whatsapp, WhatsappDisconnectReason reason) {
    }


    /**
     * Called when the socket successfully disconnects from WhatsApp
     *
     * @param reason the errorReason why the session was disconnected
     */
    default void onDisconnected(WhatsappDisconnectReason reason) {
    }

    /**
     * Called when the socket receives an action from WhatsAppWeb.
     * This listener only works for web clients.
     *
     * @param whatsapp         an instance to the calling api
     * @param action           the sync that was executed
     * @param messageIndexInfo the data about this action
     */
    default void onWebAppStateAction(Whatsapp whatsapp, Action action, MessageIndexInfo messageIndexInfo) {
    }

    /**
     * Called when the socket receives an action from WhatsAppWeb.
     * This listener only works for web clients.
     *
     * @param action           the sync that was executed
     * @param messageIndexInfo the data about this action
     */
    default void onWebAppStateAction(Action action, MessageIndexInfo messageIndexInfo) {
    }

    /**
     * Called when the socket receives a setting from WhatsAppWeb.
     * This listener only works for web clients.
     *
     * @param whatsapp an instance to the calling api
     * @param setting  the setting that was toggled
     */
    default void onWebAppStateSetting(Whatsapp whatsapp, Setting setting) {
    }

    /**
     * Called when the socket receives a setting from WhatsAppWeb.
     * This listener only works for web clients.
     *
     * @param setting the setting that was toggled
     */
    default void onWebAppStateSetting(Setting setting) {
    }

    /**
     * Called when the socket receives primary features from WhatsAppWeb.
     * This listener only works for web clients.
     *
     * @param whatsapp an instance to the calling api
     * @param features the non-null features that were sent
     */
    default void onWebAppPrimaryFeatures(Whatsapp whatsapp, List<String> features) {
    }


    /**
     * Called when the socket receives primary features from WhatsAppWeb.
     * This listener only works for web clients.
     *
     * @param features the non-null features that were sent
     */
    default void onWebAppPrimaryFeatures(Collection<String> features) {
    }

    /**
     * Called when the socket receives all the contacts from Whatsapp.
     *
     * @param whatsapp an instance to the calling api
     * @param contacts the contacts
     */
    default void onContacts(Whatsapp whatsapp, Collection<Contact> contacts) {
    }

    /**
     * Called when the socket receives all the contacts from Whatsapp.
     *
     * @param contacts the contacts
     */
    default void onContacts(Collection<Contact> contacts) {
    }

    /**
     * Called when the socket receives an update regarding the presence of a contact
     *
     * @param whatsapp an instance to the calling api
     * @param chat     the chat that this update regards
     * @param jid      the contact that this update regards
     */
    default void onContactPresence(Whatsapp whatsapp, Chat chat, JidProvider jid) {
    }

    /**
     * Called when the socket receives an update regarding the presence of a contact
     *
     * @param chat   the chat that this update regards
     * @param jid    the contact that this update regards
     */
    default void onContactPresence(Chat chat, JidProvider jid) {
    }

    /**
     * Called when the socket receives all the chats from WhatsApp. When this event is
     * fired, it is guaranteed that all metadata excluding messages will be present. If you also need
     * the messages to be loaded, please refer to {@link WhatsappListener#onWebHistorySyncMessages(Chat, boolean)}.
     * Particularly old chats may come later through
     * {@link WhatsappListener#onWebHistorySyncMessages(Chat, boolean)}
     *
     * @param whatsapp an instance to the calling api
     * @param chats    the chats
     */
    default void onChats(Whatsapp whatsapp, Collection<Chat> chats) {
    }

    /**
     * Called when the socket receives all the chats from WhatsApp.
     * When this event is fired, it is guaranteed that all metadata excluding messages will be present. if you are using a web client
     * If you also need the messages to be loaded, please refer to {@link WhatsappListener#onWebHistorySyncMessages(Chat, boolean)}.
     * Particularly old chats may come later through {@link WhatsappListener#onWebHistorySyncMessages(Chat, boolean)}.
     *
     * @param chats the chats
     */
    default void onChats(Collection<Chat> chats) {
    }


    /**
     * Called when the socket receives all the newsletters from WhatsApp
     *
     * @param whatsapp    an instance to the calling api
     * @param newsletters the newsletters
     */
    default void onNewsletters(Whatsapp whatsapp, Collection<Newsletter> newsletters) {
    }

    /**
     * Called when the socket receives all the newsletters from WhatsApp
     *
     * @param newsletters the newsletters
     */
    default void onNewsletters(Collection<Newsletter> newsletters) {
    }

    /**
     * Called when the socket receives the messages for a chat. This method is only called when the QR
     * is first scanned and history is being synced. From all subsequent runs, the messages will
     * already in the chat on startup.
     *
     * @param whatsapp an instance to the calling api
     * @param chat     the chat
     * @param last     whether the messages in this chat are complete or there are more coming
     */
    default void onWebHistorySyncMessages(Whatsapp whatsapp, Chat chat, boolean last) {
    }

    /**
     * Called when the socket receives the message for a chat This method is only called when the QR
     * is first scanned and history is being synced. From all subsequent runs, the messages will
     * already in the chat on startup.
     *
     * @param chat the chat
     * @param last whether the messages in this chat are complete or there are more coming
     */
    default void onWebHistorySyncMessages(Chat chat, boolean last) {
    }

    /**
     * Called when past participants for a group are received during a web history sync
     *
     * @param whatsapp an instance to the calling api
     * @param chatJid the non-null chat jid
     * @param chatPastParticipants the non-null past participants
     */
    default void onWebHistorySyncPastParticipants(Whatsapp whatsapp, Jid chatJid, Collection<ChatPastParticipant> chatPastParticipants) {
    }

    /**
     * Called when past participants for a group are received during a web history sync
     *
     * @param chatJid the non-null chat jid
     * @param chatPastParticipants the non-null past participants
     */
    default void onWebHistorySyncPastParticipants(Jid chatJid, Collection<ChatPastParticipant> chatPastParticipants) {
    }

    /**
     * Called when the socket receives the sync percentage for the full or recent chunk of messages.
     * This method is only called when the QR is first scanned and history is being synced.
     *
     * @param percentage the percentage synced up to now
     * @param recent     whether the sync is about the recent messages or older messages
     */
    default void onWebHistorySyncProgress(int percentage, boolean recent) {
    }

    /**
     * Called when the socket receives the sync percentage for the full or recent chunk of messages.
     * This method is only called when the QR is first scanned and history is being synced.
     *
     * @param whatsapp   an instance to the calling api
     * @param percentage the percentage synced up to now
     * @param recent     whether the sync is about the recent messages or older messages
     */
    default void onWebHistorySyncProgress(Whatsapp whatsapp, int percentage, boolean recent) {
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
     * @param everyone whether this message was deleted by you only for yourself or whether the
     *                 message was permanently removed
     */
    default void onMessageDeleted(Whatsapp whatsapp, MessageInfo info, boolean everyone) {
    }

    /**
     * Called when a message is deleted
     *
     * @param info     the message that was deleted
     * @param everyone whether this message was deleted by you only for yourself or whether the
     *                 message was permanently removed
     */
    default void onMessageDeleted(MessageInfo info, boolean everyone) {
    }

    /**
     * Called when the status of a message changes inside a chat
     *
     * @param whatsapp an instance to the calling api
     * @param info     the message whose status changed
     */
    default void onMessageStatus(Whatsapp whatsapp, MessageInfo info) {
    }

    /**
     * Called when the status of a message changes inside a chat
     *
     * @param info the message whose status changed
     */
    default void onMessageStatus(MessageInfo info) {
    }


    /**
     * Called when the socket receives all the status updated from WhatsApp.
     *
     * @param whatsapp an instance to the calling api
     * @param status   the status
     */
    default void onStatus(Whatsapp whatsapp, Collection<ChatMessageInfo> status) {
    }

    /**
     * Called when the socket receives all the status updated from WhatsApp.
     *
     * @param status the status
     */
    default void onStatus(Collection<ChatMessageInfo> status) {
    }

    /**
     * Called when the socket receives a new status from WhatsApp
     *
     * @param whatsapp an instance to the calling api
     * @param status   the new status message
     */
    default void onNewStatus(Whatsapp whatsapp, ChatMessageInfo status) {
    }

    /**
     * Called when the socket receives a new status from WhatsApp
     *
     * @param status the new status message
     */
    default void onNewStatus(ChatMessageInfo status) {
    }

    /**
     * Called when a message answers a previous message
     *
     * @param response the response
     * @param quoted   the quoted message
     */
    default void onMessageReply(MessageInfo response, QuotedMessageInfo quoted) {
    }

    /**
     * Called when a message answers a previous message
     *
     * @param whatsapp an instance to the calling api
     * @param response the response
     * @param quoted   the quoted message
     */
    default void onMessageReply(Whatsapp whatsapp, MessageInfo response, QuotedMessageInfo quoted) {
    }

    /**
     * Called when a contact's profile picture changes
     *
     * @param jid the contact whose pic changed
     */
    default void onProfilePictureChanged(JidProvider jid) {
    }


    /**
     * Called when a contact's profile picture changes
     *
     * @param whatsapp an instance to the calling api
     * @param jid  the contact whose pic changed
     */
    default void onProfilePictureChanged(Whatsapp whatsapp, JidProvider jid) {
    }

    /**
     * Called when the companion's name changes
     *
     * @param oldName the non-null old name
     * @param newName the non-null new name
     */
    default void onNameChanged(String oldName, String newName) {
    }

    /**
     * Called when the companion's name changes
     *
     * @param whatsapp an instance to the calling api
     * @param oldName  the non-null old name
     * @param newName  the non-null new name
     */
    default void onNameChanged(Whatsapp whatsapp, String oldName, String newName) {
    }

    /**
     * Called when the companion's about changes
     *
     * @param oldAbout the non-null old about
     * @param newAbout the non-null new about
     */
    default void onAboutChanged(String oldAbout, String newAbout) {
    }

    /**
     * Called when the companion's about changes
     *
     * @param whatsapp an instance to the calling api
     * @param oldAbout the non-null old about
     * @param newAbout the non-null new about
     */
    default void onAboutChanged(Whatsapp whatsapp, String oldAbout, String newAbout) {
    }

    /**
     * Called when the companion's locale changes
     *
     * @param oldLocale the non-null old locale
     * @param newLocale the non-null new picture
     */
    default void onLocaleChanged(CountryLocale oldLocale, CountryLocale newLocale) {
    }

    /**
     * Called when the companion's locale changes
     *
     * @param whatsapp  an instance to the calling api
     * @param oldLocale the non-null old locale
     * @param newLocale the non-null new picture
     */
    default void onLocaleChanged(Whatsapp whatsapp, CountryLocale oldLocale, CountryLocale newLocale) {
    }

    /**
     * Called when a contact is blocked or unblocked
     *
     * @param contact the non-null contact
     */
    default void onContactBlocked(Contact contact) {
    }

    /**
     * Called when a contact is blocked or unblocked
     *
     * @param whatsapp an instance to the calling api
     * @param contact  the non-null contact
     */
    default void onContactBlocked(Whatsapp whatsapp, Contact contact) {
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
     * Called when a privacy setting is modified
     *
     * @param whatsapp        an instance to the calling api
     * @param oldPrivacyEntry the old entry
     * @param newPrivacyEntry the new entry
     */
    default void onPrivacySettingChanged(Whatsapp whatsapp, PrivacySettingEntry oldPrivacyEntry, PrivacySettingEntry newPrivacyEntry) {

    }

    /**
     * Called when a privacy setting is modified
     *
     * @param oldPrivacyEntry the old entry
     * @param newPrivacyEntry the new entry
     */
    default void onPrivacySettingChanged(PrivacySettingEntry oldPrivacyEntry, PrivacySettingEntry newPrivacyEntry) {

    }

    /**
     * Called when the list of companion devices is updated
     *
     * @param whatsapp an instance to the calling api
     * @param devices  the non-null devices
     */
    default void onLinkedDevices(Whatsapp whatsapp, Collection<Jid> devices) {

    }

    /**
     * Called when the list of companion devices is updated
     *
     * @param devices the non-null devices
     */
    default void onLinkedDevices(Collection<Jid> devices) {

    }

    /**
     * Called when an OTP is requested from a new device
     * Only works on the mobile API
     *
     * @param code the registration code
     */
    default void onRegistrationCode(long code) {

    }

    /**
     * Called when an OTP is requested from a new device
     * Only works on the mobile API
     *
     * @param whatsapp an instance to the calling api
     * @param code     the registration code
     */
    default void onRegistrationCode(Whatsapp whatsapp, long code) {

    }

    /**
     * Called when a phone call arrives
     *
     * @param call the non-null phone call
     */
    default void onCall(Call call) {

    }

    /**
     * Called when a phone call arrives
     *
     * @param whatsapp an instance to the calling api
     * @param call     the non-null phone call
     */
    default void onCall(Whatsapp whatsapp, Call call) {

    }
}
