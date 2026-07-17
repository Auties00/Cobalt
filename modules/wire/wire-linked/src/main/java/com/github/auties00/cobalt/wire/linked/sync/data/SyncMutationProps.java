package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Catalogue of sync action types that can appear inside an app-state
 * mutation.
 *
 * <p>Every app-state change, from starring a chat to toggling a privacy
 * setting or recording a newsletter interest, is mapped to one of the values
 * in this enum. Each constant identifies the kind of action carried by a
 * {@code SyncActionData} payload and is used to dispatch the decoded action
 * to the right handler in the store.
 */
@ProtobufEnum(name = "MutationProps")
public enum SyncMutationProps {
    /**
     * Star or unstar a message.
     */
    STAR_ACTION(2),
    /**
     * Create, update or delete a contact.
     */
    CONTACT_ACTION(3),
    /**
     * Mute or unmute a chat.
     */
    MUTE_ACTION(4),
    /**
     * Pin or unpin a chat.
     */
    PIN_ACTION(5),
    /**
     * Preference controlling security notifications.
     */
    SECURITY_NOTIFICATION_SETTING(6),
    /**
     * Push name (display name) change.
     */
    PUSH_NAME_SETTING(7),
    /**
     * Create, update or delete a quick reply.
     */
    QUICK_REPLY_ACTION(8),
    /**
     * Update of recently used emoji weights.
     */
    RECENT_EMOJI_WEIGHTS_ACTION(11),
    /**
     * Associate or dissociate a label with a message.
     */
    LABEL_MESSAGE_ACTION(13),
    /**
     * Edit the metadata of an existing label.
     */
    LABEL_EDIT_ACTION(14),
    /**
     * Bulk label-association changes.
     */
    LABEL_ASSOCIATION_ACTION(15),
    /**
     * User locale preference.
     */
    LOCALE_SETTING(16),
    /**
     * Archive or unarchive a chat.
     */
    ARCHIVE_CHAT_ACTION(17),
    /**
     * Delete a single message for the current user.
     */
    DELETE_MESSAGE_FOR_ME_ACTION(18),
    /**
     * Sync-key rotation/expiration event.
     */
    KEY_EXPIRATION(19),
    /**
     * Mark a chat as read or unread.
     */
    MARK_CHAT_AS_READ_ACTION(20),
    /**
     * Clear the messages of a chat.
     */
    CLEAR_CHAT_ACTION(21),
    /**
     * Delete an entire chat.
     */
    DELETE_CHAT_ACTION(22),
    /**
     * Preference controlling whether new chats are automatically unarchived.
     */
    UNARCHIVE_CHATS_SETTING(23),
    /**
     * Identifies the account's primary device.
     */
    PRIMARY_FEATURE(24),
    /**
     * Actions that are unsupported on Android but echoed for other clients.
     */
    ANDROID_UNSUPPORTED_ACTIONS(26),
    /**
     * Business agent state change.
     */
    AGENT_ACTION(27),
    /**
     * Newsletter subscription change.
     */
    SUBSCRIPTION_ACTION(28),
    /**
     * Mute or unmute a contact's status updates.
     */
    USER_STATUS_MUTE_ACTION(29),
    /**
     * User preference for 12/24 hour time format.
     */
    TIME_FORMAT_ACTION(30),
    /**
     * Dismissal of a new-user experience (NUX) element.
     */
    NUX_ACTION(31),
    /**
     * Advertised version of the primary device.
     */
    PRIMARY_VERSION_ACTION(32),
    /**
     * Sticker add/remove action.
     */
    STICKER_ACTION(33),
    /**
     * Remove a sticker from the recently used list.
     */
    REMOVE_RECENT_STICKER_ACTION(34),
    /**
     * Assign a chat to a business agent.
     */
    CHAT_ASSIGNMENT(35),
    /**
     * Open/closed status of a chat assignment.
     */
    CHAT_ASSIGNMENT_OPENED_STATUS(36),
    /**
     * Phone-number-for-LID chat action.
     */
    PN_FOR_LID_CHAT_ACTION(37),
    /**
     * Marketing message changes for business accounts.
     */
    MARKETING_MESSAGE_ACTION(38),
    /**
     * Broadcast variant of a marketing message action.
     */
    MARKETING_MESSAGE_BROADCAST_ACTION(39),
    /**
     * External-web beta program opt-in toggle.
     */
    EXTERNAL_WEB_BETA_ACTION(40),
    /**
     * Relay-all-calls privacy preference.
     */
    PRIVACY_SETTING_RELAY_ALL_CALLS(41),
    /**
     * Add/remove a call log entry.
     */
    CALL_LOG_ACTION(42),
    /**
     * User-generated-content bot action.
     */
    UGC_BOT(43),
    /**
     * Privacy setting controlling who can see status updates.
     */
    STATUS_PRIVACY(44),
    /**
     * Bot welcome-request acknowledgement.
     */
    BOT_WELCOME_REQUEST_ACTION(45),
    /**
     * Remove a single entry from a call log.
     */
    DELETE_INDIVIDUAL_CALL_LOG(46),
    /**
     * Reorder labels.
     */
    LABEL_REORDERING_ACTION(47),
    /**
     * Payment information update.
     */
    PAYMENT_INFO_ACTION(48),
    /**
     * Custom payment methods update.
     */
    CUSTOM_PAYMENT_METHODS_ACTION(49),
    /**
     * Lock or unlock a chat with the user's lock credentials.
     */
    LOCK_CHAT_ACTION(50),
    /**
     * Global chat-lock settings.
     */
    CHAT_LOCK_SETTINGS(51),
    /**
     * Wamo (merchant) user identifier update.
     */
    WAMO_USER_IDENTIFIER_ACTION(52),
    /**
     * Per-chat setting that disables link previews.
     */
    PRIVACY_SETTING_DISABLE_LINK_PREVIEWS_ACTION(53),
    /**
     * Advertised device capabilities.
     */
    DEVICE_CAPABILITIES(54),
    /**
     * Edit the contents of a note.
     */
    NOTE_EDIT_ACTION(55),
    /**
     * Mark or unmark a contact as a favorite.
     */
    FAVORITES_ACTION(56),
    /**
     * Merchant payment partner change.
     */
    MERCHANT_PAYMENT_PARTNER_ACTION(57),
    /**
     * Waffle (account-link) state change.
     */
    WAFFLE_ACCOUNT_LINK_STATE_ACTION(58),
    /**
     * Preference controlling whether usernames can initiate chats.
     */
    USERNAME_CHAT_START_MODE(59),
    /**
     * Notification-activity preference.
     */
    NOTIFICATION_ACTIVITY_SETTING_ACTION(60),
    /**
     * LID-based contact action.
     */
    LID_CONTACT_ACTION(61),
    /**
     * Per-customer data-sharing preference for click-to-WhatsApp.
     */
    CTWA_PER_CUSTOMER_DATA_SHARING_ACTION(62),
    /**
     * Payment terms-of-service acceptance.
     */
    PAYMENT_TOS_ACTION(63),
    /**
     * Personalised channel recommendation opt-out.
     */
    PRIVACY_SETTING_CHANNELS_PERSONALISED_RECOMMENDATION_ACTION(64),
    /**
     * Association of a business with a broadcast.
     */
    BUSINESS_BROADCAST_ASSOCIATION_ACTION(65),
    /**
     * Detected-outcomes status update.
     */
    DETECTED_OUTCOMES_STATUS_ACTION(66),
    /**
     * Maiba AI feature-control toggle.
     */
    MAIBA_AI_FEATURES_CONTROL_ACTION(68),
    /**
     * Create, update or delete a business broadcast list.
     */
    BUSINESS_BROADCAST_LIST_ACTION(69),
    /**
     * Music-related user identifier update.
     */
    MUSIC_USER_ID_ACTION(70),
    /**
     * Post-status opt-in notification preferences.
     */
    STATUS_POST_OPT_IN_NOTIFICATION_PREFERENCES_ACTION(71),
    /**
     * Profile avatar update event.
     */
    AVATAR_UPDATED_ACTION(72),
    /**
     * Galaxy-flow onboarding action.
     */
    GALAXY_FLOW_ACTION(73),
    /**
     * Private-processing preferences for message handling.
     */
    PRIVATE_PROCESSING_SETTING_ACTION(74),
    /**
     * Save or remove a newsletter topic of interest.
     */
    NEWSLETTER_SAVED_INTERESTS_ACTION(75),
    /**
     * Rename an AI thread.
     */
    AI_THREAD_RENAME_ACTION(76),
    /**
     * Interactive message action for bot responses.
     */
    INTERACTIVE_MESSAGE_ACTION(77),
    /**
     * Settings-sync bulk action.
     */
    SETTINGS_SYNC_ACTION(78),
    /**
     * Action to share the account's own phone number.
     */
    SHARE_OWN_PN(10001),
    /**
     * Business broadcast send action.
     */
    BUSINESS_BROADCAST_ACTION(10002),
    /**
     * Delete an AI thread.
     */
    AI_THREAD_DELETE_ACTION(10003);

    /**
     * Constructs a mutation-props constant with the given protobuf index.
     *
     * @param index the protobuf wire index
     */
    SyncMutationProps(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf wire index assigned to this action kind.
     */
    final int index;

    /**
     * Returns the protobuf wire index of this action kind.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return this.index;
    }
}
