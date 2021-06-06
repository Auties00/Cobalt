package it.auties.whatsapp4j.listener;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.chat.GroupAction;
import it.auties.whatsapp4j.protobuf.chat.GroupPolicy;
import it.auties.whatsapp4j.protobuf.chat.GroupSetting;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.response.impl.json.BlocklistResponse;
import it.auties.whatsapp4j.response.impl.json.PhoneBatteryResponse;
import it.auties.whatsapp4j.response.impl.json.PropsResponse;
import it.auties.whatsapp4j.response.impl.json.UserInformationResponse;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * This interface can be used to listen for events fired when new information is sent by WhatsappWeb's socket.
 * A WhatsappListener can be registered manually using {@link WhatsappAPI#registerListener(WhatsappListener)}.
 * Otherwise, it can be registered by annotating it with the {@link RegisterListener} annotation.
 * If the latter option is used, auto detection of listeners by calling {@link WhatsappAPI#autodetectListeners()}.
 */
public interface WhatsappListener {
    /**
     * Called when {@link WhatsappWebSocket} successfully establishes a connection and logs in into an account.
     * When this event is called, any data, including chats and contact, is not guaranteed to be already in memory.
     * Instead, {@link WhatsappListener#onChatsReceived()} and {@link WhatsappListener#onContactsReceived()} should be used.
     *
     * @param info the information sent by WhatsappWeb's WebSocket about this session
     */
    default void onLoggedIn(@NotNull UserInformationResponse info) {
    }

    /**
     * Called when {@link WhatsappWebSocket} successfully disconnects from WhatsappWeb's WebSocket.
     * When this event is called, any data, including chats and contact, is guaranteed to not be available anymore.
     */
    default void onDisconnected() {
    }

    /**
     * Called when new information regarding this session is available.
     * Only the new data will be available in {@code info} as it's a partial object.
     *
     * @param info the partial object used to represent the new data available for this session
     */
    default void onInformationUpdate(@NotNull UserInformationResponse info) {
    }

    /**
     * Called when {@link WhatsappWebSocket} receives a plain text list.
     * This data is usually not very useful, but it may be necessary for particular use cases.
     *
     * @param response the list received as plain text by {@link WhatsappWebSocket}
     */
    default void onListResponse(@NotNull List<Object> response) {
    }

    /**
     * Called when {@link WhatsappWebSocket} receives all the contacts from WhatsappWeb's WebSocket.
     * To access this data use {@link WhatsappDataManager#contacts()}.
     */
    default void onContactsReceived() {
    }

    /**
     * Called when {@link WhatsappWebSocket} receives an update regarding a contact
     *
     * @param contact the updated contact
     */
    default void onContactUpdate(@NotNull Contact contact) {
    }

    /**
     * Called when {@link WhatsappWebSocket} receives a new contact
     *
     * @param contact the new contact
     */
    default void onContactReceived(@NotNull Contact contact) {
    }

    /**
     * Called when {@link WhatsappWebSocket} receives an update regarding the presence of a contact.
     * If {@code chat} is a conversation with {@code contact}, the new presence is available by calling {@link Contact#lastKnownPresence()}.
     * Otherwise, it should be queried using {@link Chat#presences()}.
     *
     * @param chat    the chat that this update regards
     * @param contact the contact that this update regards
     */
    default void onContactPresenceUpdate(@NotNull Chat chat, @NotNull Contact contact) {
    }

    /**
     * Called when {@link WhatsappWebSocket} receives all the chats from WhatsappWeb's WebSocket.
     * To access this data use {@link WhatsappDataManager#chats()}.
     */
    default void onChatsReceived() {
    }

    /**
     * Called when {@link WhatsappWebSocket} receives a new chat
     *
     * @param chat the new chat
     */
    default void onChatReceived(@NotNull Chat chat) {
    }

    /**
     * Called when a chat is archived
     *
     * @param chat the chat that was archived
     */
    default void onChatArchived(@NotNull Chat chat) {
    }

    /**
     * Called when a chat is unarchived
     *
     * @param chat the chat that was unarchived
     */
    default void onChatUnarchived(@NotNull Chat chat) {
    }

    /**
     * Called when a chat's mute changes
     *
     * @param chat the chat whose mute changed
     */
    default void onChatMuteChange(@NotNull Chat chat) {
    }

    /**
     * Called when a chat's read status changes
     *
     * @param chat the chat whose read status changed
     */
    default void onChatReadStatusChange(@NotNull Chat chat) {
    }

    /**
     * Called when a chat's ephemeral status changes
     *
     * @param chat the chat whose ephemeral status changed
     */
    default void onChatEphemeralStatusChange(@NotNull Chat chat) {
    }

    /**
     * Called when a group's subject changes
     *
     * @param group the group whose subject changed
     */
    default void onGroupSubjectChange(@NotNull Chat group) {
    }

    /**
     * Called when a group's description changes
     *
     * @param group         the group whose description changed
     * @param description   the new description
     * @param descriptionId the id of the new description
     */
    default void onGroupDescriptionChange(@NotNull Chat group, @NotNull String description, @NotNull String descriptionId) {
    }

    /**
     * Called when a group's settings change
     *
     * @param group   the group whose settings changed
     * @param setting the setting that changed
     * @param policy  the new policy that was set
     */
    default void onGroupSettingsChange(@NotNull Chat group, @NotNull GroupSetting setting, @NotNull GroupPolicy policy) {
    }

    /**
     * Called when an action is executed on a group's participant
     *
     * @param group       the group where the action was executed
     * @param participant the target of the action that was executed
     * @param action      the type of the action that was executed
     */
    default void onGroupAction(@NotNull Chat group, @NotNull Contact participant, @NotNull GroupAction action) {
    }

    /**
     * Called when a new message is received in a chat
     *
     * @param chat    the chat where the message was sent
     * @param message the message that was sent
     */
    default void onNewMessageReceived(@NotNull Chat chat, @NotNull MessageInfo message) {
    }

    /**
     * Called when the read status of a message changes.
     * If {@code chat} is a conversation with {@code contact}, the new read status can be considered valid for the message itself.
     * Otherwise, it should be considered valid only for {@code contact} without making assumptions about the status of the message for other participants of the group.
     *
     * @param chat    the chat where the message is
     * @param contact the contact that this update regards
     * @param message the message this update regards
     */
    default void onMessageReadStatusUpdate(@NotNull Chat chat, @NotNull Contact contact, @NotNull MessageInfo message) {
    }

    /**
     * Called when the metadata or content of a message is updated
     *
     * @param chat    the chat where the message is
     * @param message the message this update regards
     */
    default void onMessageUpdate(@NotNull Chat chat, @NotNull MessageInfo message) {
    }

    /**
     * Called when a message is deleted
     *
     * @param chat     the chat where the message is
     * @param message  the message that was deleted
     * @param everyone whether this message was deleted by you only for yourself or whether the message was permanently removed
     */
    default void onMessageDeleted(@NotNull Chat chat, @NotNull MessageInfo message, boolean everyone) {
    }

    /**
     * Called when a message is starred
     *
     * @param chat    the chat where the message is
     * @param message the message that was starred
     */
    default void onMessageStarred(@NotNull Chat chat, @NotNull MessageInfo message) {
    }

    /**
     * Called when a message is unstarred
     *
     * @param chat    the chat where the message is
     * @param message the message that was unstarred
     */
    default void onMessageUnstarred(@NotNull Chat chat, @NotNull MessageInfo message) {
    }

    /**
     * Called when the global read status of a message changes.
     * This status can be accessed calling {@link MessageInfo#globalStatus()}.
     * If {@code chat} is a conversation, {@link MessageInfo#globalStatus()} is equal to the one stored in {@link MessageInfo#individualReadStatus()} for the corresponding contact.
     * Otherwise, it is guaranteed that each value stored in {@link MessageInfo#individualReadStatus()} for each participant of the chat is equal or higher hierarchically then {@link MessageInfo#globalStatus()}.
     *
     * @param chat    the chat where the message is
     * @param message the message that was unstarred
     */
    default void onMessageGlobalReadStatusUpdate(@NotNull Chat chat, @NotNull MessageInfo message) {
    }

    /**
     * Called when an updated blocklist is received.
     * This method is called both when a connection is established with WhatsappWeb and when a contact is added or removed from the blocklist.
     *
     * @param blocklist the updated blocklist
     */
    default void onBlocklistUpdate(@NotNull BlocklistResponse blocklist) {
    }

    /**
     * Called when an updated list of properties is received.
     * This method is called both when a connection is established with WhatsappWeb and when new props are available.
     * In the latter case though, this object should be considered as partial and is guaranteed to contain only updated entries.
     *
     * @param props the updated list of properties
     */
    default void onPropsUpdate(@NotNull PropsResponse props) {
    }

    /**
     * Called when an updated object describing the status of the phone's associated with this session battery status changes
     *
     * @param battery the new battery status
     */
    default void onPhoneBatteryStatusUpdate(@NotNull PhoneBatteryResponse battery) {
    }
}
