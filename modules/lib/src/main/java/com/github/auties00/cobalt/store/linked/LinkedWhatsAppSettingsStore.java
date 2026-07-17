package com.github.auties00.cobalt.store.linked;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.linked.preference.Label;
import com.github.auties00.cobalt.wire.linked.preference.OnboardingHintState;
import com.github.auties00.cobalt.wire.linked.preference.QuickReply;
import com.github.auties00.cobalt.wire.linked.preference.Sticker;
import com.github.auties00.cobalt.wire.linked.privacy.AccountDisappearingMode;
import com.github.auties00.cobalt.wire.linked.privacy.PrivacySettingValue;
import com.github.auties00.cobalt.wire.linked.privacy.PrivacySettingType;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacySetting;
import com.github.auties00.cobalt.wire.linked.setting.AppTheme;
import com.github.auties00.cobalt.wire.linked.setting.ChatLockSettings;
import com.github.auties00.cobalt.wire.linked.setting.privacy.OptOutEntry;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.UsernameChatStartModeAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeight;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.SettingsSyncAction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedCollection;
import java.util.Set;

/**
 * The user-preference and settings state of a WhatsApp client session.
 *
 * <p>This is the sub-store that owns the user's configuration: the privacy settings, the
 * status-privacy and default-disappearing-message settings, the chat-lock configuration, the
 * archived-chat / time-format / link-preview / call-relay toggles, the desktop and notification
 * preferences (themes, tones, autodownload, banners), the recent and favourite stickers, the
 * business labels and quick replies, the recent-emoji weights, the accepted Terms-of-Service
 * notices, the marketing opt-out and contact-blacklist tables, the onboarding-hint state and the
 * newsletter/channel preferences.
 *
 * @apiNote
 * Embedders reach this through {@link LinkedWhatsAppStore#settingsStore()}.
 *
 * @see LinkedWhatsAppStore
 */
@WhatsAppWebModule(moduleName = "WAWebUserPrefsBase")
@WhatsAppWebModule(moduleName = "WAWebUserPrefsStore")
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface LinkedWhatsAppSettingsStore {
    /**
     * Returns the value of every configured privacy setting.
     *
     * @return an unmodifiable copy of the privacy setting values
     */
    Collection<PrivacySettingValue> privacySettings();

    /**
     * Looks up the configured value of a privacy setting by type.
     *
     * @param <V>  the value sub-interface accepted by the setting
     * @param type the setting to look up, or {@code null}
     * @return the configured value, or empty if the setting is unset or {@code type} is
     *         {@code null}
     */
    <V extends PrivacySettingValue> Optional<V> findPrivacySetting(PrivacySettingType<V> type);

    /**
     * Stores the value of a privacy setting, replacing any previously stored value for the same
     * {@linkplain PrivacySettingValue#type() setting}.
     *
     * @param value the privacy setting value, never {@code null}
     */
    void addPrivacySetting(PrivacySettingValue value);

    /**
     * Returns the status-privacy setting.
     *
     * @return the status-privacy setting, or empty if unset
     */
    Optional<StatusPrivacySetting> statusPrivacy();

    /**
     * Sets the status-privacy setting.
     *
     * @param statusPrivacy the setting, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setStatusPrivacy(StatusPrivacySetting statusPrivacy);

    /**
     * Returns the account default-disappearing-message mode.
     *
     * @return the disappearing mode, or empty if unset
     */
    Optional<AccountDisappearingMode> disappearingMode();

    /**
     * Sets the account default-disappearing-message mode.
     *
     * @param disappearingMode the mode, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setDisappearingMode(AccountDisappearingMode disappearingMode);

    /**
     * Returns whether archived chats auto-unarchive on new messages.
     *
     * @return {@code true} if archived chats auto-unarchive
     */
    boolean unarchiveChats();

    /**
     * Sets the auto-unarchive setting.
     *
     * @param unarchiveChats whether archived chats auto-unarchive
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setUnarchiveChats(boolean unarchiveChats);

    /**
     * Returns whether timestamps are rendered in 24-hour form.
     *
     * @return {@code true} for 24-hour format
     */
    boolean twentyFourHourFormat();

    /**
     * Sets the 24-hour-format preference.
     *
     * @param twentyFourHourFormat whether to use 24-hour format
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setTwentyFourHourFormat(boolean twentyFourHourFormat);

    /**
     * Returns the default disappearing-message timer applied to newly created chats.
     *
     * @return the default ephemeral timer, never {@code null}
     */
    ChatEphemeralTimer newChatsEphemeralTimer();

    /**
     * Sets the default disappearing-message timer for new chats.
     *
     * @param newChatsEphemeralTimer the timer, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setNewChatsEphemeralTimer(ChatEphemeralTimer newChatsEphemeralTimer);

    /**
     * Returns whether security-code-change notifications are displayed.
     *
     * @return {@code true} if security notifications are shown
     */
    boolean showSecurityNotifications();

    /**
     * Sets whether security-code-change notifications are displayed.
     *
     * @param showSecurityNotifications whether to show security notifications
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setShowSecurityNotifications(boolean showSecurityNotifications);

    /**
     * Returns whether outbound link previews are suppressed.
     *
     * @return {@code true} if link previews are disabled
     */
    boolean disableLinkPreviews();

    /**
     * Sets whether outbound link previews are suppressed.
     *
     * @param disableLinkPreviews whether to disable link previews
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setDisableLinkPreviews(boolean disableLinkPreviews);

    /**
     * Returns whether every VoIP call is forced through Meta TURN servers.
     *
     * @return {@code true} if all calls are relayed
     */
    boolean relayAllCalls();

    /**
     * Sets whether every VoIP call is forced through Meta TURN servers.
     *
     * @param relayAllCalls whether to relay all calls
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setRelayAllCalls(boolean relayAllCalls);

    /**
     * Returns the "Chat Lock" feature configuration.
     *
     * @return the chat-lock settings, or empty if unset
     */
    Optional<ChatLockSettings> chatLockSettings();

    /**
     * Sets the "Chat Lock" feature configuration.
     *
     * @param chatLockSettings the settings, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setChatLockSettings(ChatLockSettings chatLockSettings);

    /**
     * Looks up a recently used sticker by hash.
     *
     * @param stickerHash the sticker hash
     * @return the sticker, or empty if not present
     */
    Optional<Sticker> findRecentSticker(String stickerHash);

    /**
     * Records a recently used sticker.
     *
     * @param stickerHash the sticker hash, never {@code null}
     * @param sticker     the sticker, never {@code null}
     */
    void addRecentSticker(String stickerHash, Sticker sticker);

    /**
     * Removes a recently used sticker by hash.
     *
     * @param stickerHash the sticker hash
     * @return the removed sticker, or empty if not present
     */
    Optional<Sticker> removeRecentSticker(String stickerHash);

    /**
     * Returns the recently used stickers.
     *
     * @return an unmodifiable copy of the recent stickers
     */
    SequencedCollection<Sticker> recentStickers();

    /**
     * Removes every recent sticker flagged as an avatar sticker.
     *
     * @return the number of stickers removed
     */
    int removeAllRecentAvatarStickers();

    /**
     * Looks up a favourite sticker by hash.
     *
     * @param stickerHash the sticker hash
     * @return the sticker, or empty if not present
     */
    Optional<Sticker> findFavouriteSticker(String stickerHash);

    /**
     * Records a favourite sticker.
     *
     * @param stickerHash the sticker hash, never {@code null}
     * @param sticker     the sticker, never {@code null}
     */
    void addFavouriteSticker(String stickerHash, Sticker sticker);

    /**
     * Removes a favourite sticker by hash.
     *
     * @param stickerHash the sticker hash
     * @return the removed sticker, or empty if not present
     */
    Optional<Sticker> removeFavouriteSticker(String stickerHash);

    /**
     * Returns the favourite stickers.
     *
     * @return an unmodifiable copy of the favourite stickers
     */
    SequencedCollection<Sticker> favouriteStickers();

    /**
     * Looks up a business quick reply by id.
     *
     * @param id the quick-reply id, or {@code null}
     * @return the quick reply, or empty if not present
     */
    Optional<QuickReply> findQuickReply(String id);

    /**
     * Stores a business quick reply.
     *
     * @param quickReply the quick reply, never {@code null}
     */
    void addQuickReply(QuickReply quickReply);

    /**
     * Removes a business quick reply by id.
     *
     * @param id the quick-reply id, or {@code null}
     * @return the removed quick reply, or empty if not present
     */
    Optional<QuickReply> removeQuickReply(String id);

    /**
     * Returns the business quick replies.
     *
     * @return an unmodifiable copy of the quick replies
     */
    List<QuickReply> quickReplies();

    /**
     * Stores a business label.
     *
     * @param label the label, never {@code null}
     */
    void addLabel(Label label);

    /**
     * Returns the business labels.
     *
     * @return an unmodifiable copy of the labels
     */
    Collection<Label> labels();

    /**
     * Removes a business label by id.
     *
     * @param labelId the label id
     * @return the removed label, or empty if not present
     */
    Optional<Label> removeLabel(String labelId);

    /**
     * Looks up a business label by id.
     *
     * @param labelId the label id
     * @return the label, or empty if not present
     */
    Optional<Label> findLabel(String labelId);

    /**
     * Returns the frequency weights for recently used emoji.
     *
     * @return an unmodifiable copy of the recent-emoji weights
     */
    List<RecentEmojiWeight> recentEmojiWeights();

    /**
     * Sets the recent-emoji weight list.
     *
     * @param weights the weights, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setRecentEmojiWeights(List<RecentEmojiWeight> weights);

    /**
     * Returns the identifiers of Terms-of-Service notices the user has acknowledged.
     *
     * @return an unmodifiable copy of the acknowledged notice ids
     */
    Set<String> acknowledgedTosNotices();

    /**
     * Replaces the acknowledged Terms-of-Service notice set.
     *
     * @param noticeIds the notice ids, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setAcknowledgedTosNotices(Set<String> noticeIds);

    /**
     * Returns the content hash of a marketing opt-out list.
     *
     * @param category the opt-out list category, never {@code null}
     * @return the list hash, or empty if none is stored
     */
    Optional<String> optOutListHash(String category);

    /**
     * Returns the entries of a marketing opt-out list.
     *
     * @param category the opt-out list category, never {@code null}
     * @return an unmodifiable copy of the entries
     */
    List<OptOutEntry> optOutListEntries(String category);

    /**
     * Replaces a marketing opt-out list and its hash.
     *
     * @param category the opt-out list category, never {@code null}
     * @param hash     the list hash, or {@code null} to clear the hash
     * @param entries  the list entries, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setOptOutList(String category, String hash, List<OptOutEntry> entries);

    /**
     * Returns the content hash of a contact blacklist.
     *
     * @param category the blacklist category, never {@code null}
     * @return the list hash, or empty if none is stored
     */
    Optional<String> contactBlacklistHash(String category);

    /**
     * Returns the entries of a contact blacklist.
     *
     * @param category the blacklist category, never {@code null}
     * @return an unmodifiable copy of the entries
     */
    List<Jid> contactBlacklistEntries(String category);

    /**
     * Replaces a contact blacklist and its hash.
     *
     * @param category the blacklist category, never {@code null}
     * @param hash     the list hash, or {@code null} to clear the hash
     * @param entries  the blacklisted JIDs, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setContactBlacklist(String category, String hash, List<Jid> entries);

    /**
     * Returns the onboarding-hint state records.
     *
     * @return an unmodifiable copy of the onboarding-hint states
     */
    Collection<OnboardingHintState> onboardingHintStates();

    /**
     * Looks up an onboarding-hint state by hint id.
     *
     * @param hintId the hint id, or {@code null}
     * @return the state, or empty if not present
     */
    Optional<OnboardingHintState> findOnboardingHintState(String hintId);

    /**
     * Stores an onboarding-hint state.
     *
     * @param state the state, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore putOnboardingHintState(OnboardingHintState state);

    /**
     * Removes an onboarding-hint state by hint id.
     *
     * @param hintId the hint id, or {@code null}
     * @return the removed state, or empty if not present
     */
    Optional<OnboardingHintState> removeOnboardingHintState(String hintId);

    /**
     * Clears all onboarding-hint states.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore clearOnboardingHintStates();

    /**
     * Returns the notification-activity setting.
     *
     * @return the notification-activity setting, or empty if unset
     */
    Optional<NotificationActivitySettingAction.NotificationActivitySetting> notificationActivitySetting();

    /**
     * Sets the notification-activity setting.
     *
     * @param setting the setting, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setNotificationActivitySetting(NotificationActivitySettingAction.NotificationActivitySetting setting);

    /**
     * Returns the private-processing (privacy) setting status.
     *
     * @return the private-processing status, or empty if unset
     */
    Optional<PrivateProcessingSettingAction.PrivateProcessingStatus> privateProcessingStatus();

    /**
     * Sets the private-processing setting status.
     *
     * @param status the status, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setPrivateProcessingStatus(PrivateProcessingSettingAction.PrivateProcessingStatus status);

    /**
     * Returns the user identifier used for newsletter subscription tracking.
     *
     * @return the identifier, or empty if unset
     */
    Optional<String> newsletterSubscriptionUserIdentifier();

    /**
     * Sets the newsletter subscription user identifier.
     *
     * @param identifier the identifier, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setNewsletterSubscriptionUserIdentifier(String identifier);

    /**
     * Returns the saved interests used for channel recommendations.
     *
     * @return the saved interests, or empty if unset
     */
    Optional<String> newsletterSavedInterests();

    /**
     * Sets the saved channel-recommendation interests.
     *
     * @param interests the interests, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setNewsletterSavedInterests(String interests);

    /**
     * Returns whether status-post opt-in notification preferences are enabled.
     *
     * @return the preference, or empty if unset
     */
    Optional<Boolean> statusPostOptInNotificationPreferencesEnabled();

    /**
     * Sets the status-post opt-in notification preference.
     *
     * @param enabled the preference, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setStatusPostOptInNotificationPreferencesEnabled(Boolean enabled);

    /**
     * Returns whether the user opted out of personalised channel recommendations.
     *
     * @return the preference, or empty if unset
     */
    Optional<Boolean> channelsPersonalisedRecommendationOptOut();

    /**
     * Sets the personalised-channel-recommendation opt-out preference.
     *
     * @param optOut the preference, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setChannelsPersonalisedRecommendationOptOut(Boolean optOut);

    // Desktop / notification / media preferences

    /**
     * Returns whether the desktop client launches at OS login.
     *
     * @return {@code true} if start-at-login is enabled
     */
    boolean startAtLogin();

    /**
     * Sets the start-at-login preference.
     *
     * @param startAtLogin whether to start at login
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setStartAtLogin(boolean startAtLogin);

    /**
     * Returns whether closing the window minimises to the system tray.
     *
     * @return {@code true} if minimise-to-tray is enabled
     */
    boolean minimizeToTray();

    /**
     * Sets the minimise-to-tray preference.
     *
     * @param minimizeToTray whether to minimise to tray
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setMinimizeToTray(boolean minimizeToTray);

    /**
     * Returns whether typed emoticons are auto-replaced with emoji.
     *
     * @return {@code true} if emoji replacement is enabled
     */
    boolean replaceTextWithEmoji();

    /**
     * Sets the auto-emoji-replacement preference.
     *
     * @param replaceTextWithEmoji whether to auto-replace text with emoji
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setReplaceTextWithEmoji(boolean replaceTextWithEmoji);

    /**
     * Returns when banner notifications are shown.
     *
     * @return the banner display mode, or empty if unset
     */
    Optional<SettingsSyncAction.DisplayMode> bannerNotificationDisplayMode();

    /**
     * Sets the banner-notification display mode.
     *
     * @param mode the display mode, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setBannerNotificationDisplayMode(SettingsSyncAction.DisplayMode mode);

    /**
     * Returns when the unread counter badge is shown.
     *
     * @return the badge display mode, or empty if unset
     */
    Optional<SettingsSyncAction.DisplayMode> unreadCounterBadgeDisplayMode();

    /**
     * Sets the unread-counter-badge display mode.
     *
     * @param mode the display mode, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setUnreadCounterBadgeDisplayMode(SettingsSyncAction.DisplayMode mode);

    /**
     * Returns whether incoming message notifications are delivered.
     *
     * @return {@code true} if message notifications are enabled
     */
    boolean messagesNotificationEnabled();

    /**
     * Sets the message-notification preference.
     *
     * @param enabled whether message notifications are enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setMessagesNotificationEnabled(boolean enabled);

    /**
     * Returns whether incoming call notifications are delivered.
     *
     * @return {@code true} if call notifications are enabled
     */
    boolean callsNotificationEnabled();

    /**
     * Sets the call-notification preference.
     *
     * @param enabled whether call notifications are enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setCallsNotificationEnabled(boolean enabled);

    /**
     * Returns whether reaction notifications are delivered for one-to-one chats.
     *
     * @return {@code true} if reaction notifications are enabled
     */
    boolean reactionsNotificationEnabled();

    /**
     * Sets the reaction-notification preference.
     *
     * @param enabled whether reaction notifications are enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setReactionsNotificationEnabled(boolean enabled);

    /**
     * Returns whether status-reaction notifications are delivered.
     *
     * @return {@code true} if status-reaction notifications are enabled
     */
    boolean statusReactionsNotificationEnabled();

    /**
     * Sets the status-reaction-notification preference.
     *
     * @param enabled whether status-reaction notifications are enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setStatusReactionsNotificationEnabled(boolean enabled);

    /**
     * Returns whether notification banners include a message text preview.
     *
     * @return {@code true} if text previews are enabled
     */
    boolean textPreviewForNotificationEnabled();

    /**
     * Sets the notification text-preview preference.
     *
     * @param enabled whether text previews are enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setTextPreviewForNotificationEnabled(boolean enabled);

    /**
     * Returns the default one-to-one notification tone id.
     *
     * @return the tone id, or empty if unset
     */
    OptionalInt defaultNotificationToneId();

    /**
     * Sets the default one-to-one notification tone id.
     *
     * @param toneId the tone id, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setDefaultNotificationToneId(Integer toneId);

    /**
     * Returns the default group notification tone id.
     *
     * @return the tone id, or empty if unset
     */
    OptionalInt groupDefaultNotificationToneId();

    /**
     * Sets the default group notification tone id.
     *
     * @param toneId the tone id, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setGroupDefaultNotificationToneId(Integer toneId);

    /**
     * Returns the selected application theme.
     *
     * @return the theme, or empty if unset
     */
    Optional<AppTheme> appTheme();

    /**
     * Sets the application theme.
     *
     * @param appTheme the theme, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setAppTheme(AppTheme appTheme);

    /**
     * Returns the selected chat wallpaper id.
     *
     * @return the wallpaper id, or empty if unset
     */
    OptionalInt wallpaperId();

    /**
     * Sets the chat wallpaper id.
     *
     * @param wallpaperId the wallpaper id, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setWallpaperId(Integer wallpaperId);

    /**
     * Returns whether the doodle wallpaper overlay is enabled.
     *
     * @return {@code true} if the doodle overlay is enabled
     */
    boolean doodleWallpaperEnabled();

    /**
     * Sets the doodle-wallpaper preference.
     *
     * @param enabled whether the doodle overlay is enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setDoodleWallpaperEnabled(boolean enabled);

    /**
     * Returns the selected chat font size preset.
     *
     * @return the font size, or empty if unset
     */
    OptionalInt fontSize();

    /**
     * Sets the chat font size preset.
     *
     * @param fontSize the font size, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setFontSize(Integer fontSize);

    /**
     * Returns whether incoming photos are auto-downloaded.
     *
     * @return {@code true} if photo autodownload is enabled
     */
    boolean photosAutodownloadEnabled();

    /**
     * Sets the photo-autodownload preference.
     *
     * @param enabled whether photo autodownload is enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setPhotosAutodownloadEnabled(boolean enabled);

    /**
     * Returns whether incoming audio is auto-downloaded.
     *
     * @return {@code true} if audio autodownload is enabled
     */
    boolean audiosAutodownloadEnabled();

    /**
     * Sets the audio-autodownload preference.
     *
     * @param enabled whether audio autodownload is enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setAudiosAutodownloadEnabled(boolean enabled);

    /**
     * Returns whether incoming videos are auto-downloaded.
     *
     * @return {@code true} if video autodownload is enabled
     */
    boolean videosAutodownloadEnabled();

    /**
     * Sets the video-autodownload preference.
     *
     * @param enabled whether video autodownload is enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setVideosAutodownloadEnabled(boolean enabled);

    /**
     * Returns whether incoming documents are auto-downloaded.
     *
     * @return {@code true} if document autodownload is enabled
     */
    boolean documentsAutodownloadEnabled();

    /**
     * Sets the document-autodownload preference.
     *
     * @param enabled whether document autodownload is enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setDocumentsAutodownloadEnabled(boolean enabled);

    /**
     * Returns the chat notification tone override id.
     *
     * @return the tone id, or empty if unset
     */
    OptionalInt notificationToneId();

    /**
     * Sets the chat notification tone override id.
     *
     * @param toneId the tone id, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setNotificationToneId(Integer toneId);

    /**
     * Returns the media upload quality preset.
     *
     * @return the upload quality, or empty if unset
     */
    Optional<SettingsSyncAction.MediaQualitySetting> mediaUploadQuality();

    /**
     * Sets the media upload quality preset.
     *
     * @param quality the quality, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setMediaUploadQuality(SettingsSyncAction.MediaQualitySetting quality);

    /**
     * Returns whether spell check is enabled in the composer.
     *
     * @return {@code true} if spell check is enabled
     */
    boolean spellCheckEnabled();

    /**
     * Sets the spell-check preference.
     *
     * @param enabled whether spell check is enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setSpellCheckEnabled(boolean enabled);

    /**
     * Returns whether pressing Enter sends the message.
     *
     * @return {@code true} if enter-to-send is enabled
     */
    boolean enterToSendEnabled();

    /**
     * Sets the enter-to-send preference.
     *
     * @param enabled whether enter-to-send is enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setEnterToSendEnabled(boolean enabled);

    /**
     * Returns whether group message notifications are delivered.
     *
     * @return {@code true} if group message notifications are enabled
     */
    boolean groupMessageNotificationEnabled();

    /**
     * Sets the group-message-notification preference.
     *
     * @param enabled whether group message notifications are enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setGroupMessageNotificationEnabled(boolean enabled);

    /**
     * Returns whether group reaction notifications are delivered.
     *
     * @return {@code true} if group reaction notifications are enabled
     */
    boolean groupReactionsNotificationEnabled();

    /**
     * Sets the group-reaction-notification preference.
     *
     * @param enabled whether group reaction notifications are enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setGroupReactionsNotificationEnabled(boolean enabled);

    /**
     * Returns whether status update notifications are delivered.
     *
     * @return {@code true} if status notifications are enabled
     */
    boolean statusNotificationEnabled();

    /**
     * Sets the status-notification preference.
     *
     * @param enabled whether status notifications are enabled
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setStatusNotificationEnabled(boolean enabled);

    /**
     * Returns the status notification tone id.
     *
     * @return the tone id, or empty if unset
     */
    OptionalInt statusNotificationToneId();

    /**
     * Sets the status notification tone id.
     *
     * @param toneId the tone id, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setStatusNotificationToneId(Integer toneId);

    /**
     * Returns whether call notifications play a ringtone.
     *
     * @return {@code true} if call-notification sound is enabled
     */
    boolean playSoundForCallNotification();

    /**
     * Sets the call-notification-sound preference.
     *
     * @param enabled whether call notifications play a ringtone
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setPlaySoundForCallNotification(boolean enabled);

    /**
     * Returns the username chat-start mode.
     *
     * @return the chat-start mode, or empty if unset
     */
    Optional<UsernameChatStartModeAction.ChatStartMode> usernameChatStartMode();

    /**
     * Sets the username chat-start mode.
     *
     * @param mode the mode, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSettingsStore setUsernameChatStartMode(UsernameChatStartModeAction.ChatStartMode mode);
}
