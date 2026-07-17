package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.core.jid.Jid;
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
import com.github.auties00.cobalt.wire.linked.sync.action.chat.UsernameChatStartModeAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.RecentEmojiWeight;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NotificationActivitySettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.SettingsSyncAction;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

/**
 * The protobuf-backed {@link LinkedWhatsAppSettingsStore} holding this session's user-preference and settings state.
 *
 * <p>This is a nested {@code MESSAGE} sub-store of {@link ProtobufWhatsAppStore}; it owns the privacy
 * settings, chat-lock configuration, archived-chat / time-format / link-preview / call-relay toggles,
 * the sticker, label and quick-reply collections and the full desktop/notification/media preference
 * set (persisted), plus the transient status-privacy, disappearing-mode, emoji-weight, ToS-notice,
 * opt-out/blacklist, onboarding-hint and newsletter-preference state.
 *
 * @implNote
 * This implementation defaults the new-chat ephemeral timer to {@link ChatEphemeralTimer#OFF} and the
 * sticker/label/quick-reply maps to empty concurrent maps when absent. Boolean preference accessors
 * return {@code false} for an unset (null) preference; integer accessors return an empty
 * {@link OptionalInt}.
 */
@ProtobufMessage
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class ProtobufLinkedWhatsAppSettingsStore implements LinkedWhatsAppSettingsStore {
    /**
     * The logger for {@link ProtobufLinkedWhatsAppSettingsStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufLinkedWhatsAppSettingsStore.class);

    /**
     * The value of every configured privacy setting.
     *
     * <p>Each {@link PrivacySettingValue} is self-describing (it carries its
     * {@link PrivacySettingType} and refinement list), so the settings are stored as a flat
     * list rather than a type-keyed map; at most one value per setting is retained by
     * {@link #addPrivacySetting(PrivacySettingValue)}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final List<PrivacySettingValue> privacySettingsValues;

    /**
     * Whether archived chats auto-unarchive on new messages.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    private boolean unarchiveChats;

    /**
     * Whether timestamps are rendered in 24-hour form.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    private boolean twentyFourHourFormat;

    /**
     * The default disappearing-message timer applied to newly created chats.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    private ChatEphemeralTimer newChatsEphemeralTimer;

    /**
     * Whether security-code-change notifications are displayed.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    private boolean showSecurityNotifications;

    /**
     * The recently used stickers keyed by sticker hash.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentMap<String, Sticker> recentStickersMap;

    /**
     * The user-favourited stickers keyed by sticker hash.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentMap<String, Sticker> favouriteStickersMap;

    /**
     * The business quick replies keyed by shortcut id.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentMap<String, QuickReply> quickRepliesMap;

    /**
     * The business labels keyed by label id.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentMap<String, Label> labelsMap;

    /**
     * Whether outbound link previews are suppressed.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
    private boolean disableLinkPreviews;

    /**
     * Whether every VoIP call is forced through Meta TURN servers.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    private boolean relayAllCalls;

    /**
     * The "Chat Lock" feature configuration.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    private ChatLockSettings chatLockSettings;

    /**
     * Whether the desktop client launches at OS login.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    private Boolean startAtLogin;

    /**
     * Whether closing the window minimises to the system tray.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
    private Boolean minimizeToTray;

    /**
     * Whether typed emoticons are auto-replaced with emoji.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
    private Boolean replaceTextWithEmoji;

    /**
     * When banner notifications are shown.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.ENUM)
    private SettingsSyncAction.DisplayMode bannerNotificationDisplayMode;

    /**
     * When the unread counter badge is shown.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.ENUM)
    private SettingsSyncAction.DisplayMode unreadCounterBadgeDisplayMode;

    /**
     * Whether incoming message notifications are delivered.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    private Boolean messagesNotificationEnabled;

    /**
     * Whether incoming call notifications are delivered.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.BOOL)
    private Boolean callsNotificationEnabled;

    /**
     * Whether reaction notifications are delivered for one-to-one chats.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.BOOL)
    private Boolean reactionsNotificationEnabled;

    /**
     * Whether status-reaction notifications are delivered.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
    private Boolean statusReactionsNotificationEnabled;

    /**
     * Whether notification banners include a message text preview.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
    private Boolean textPreviewForNotificationEnabled;

    /**
     * The default one-to-one notification tone id.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.INT32)
    private Integer defaultNotificationToneId;

    /**
     * The default group notification tone id.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.INT32)
    private Integer groupDefaultNotificationToneId;

    /**
     * The selected application theme.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.ENUM)
    private AppTheme appTheme;

    /**
     * The selected chat wallpaper id.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.INT32)
    private Integer wallpaperId;

    /**
     * Whether the doodle wallpaper overlay is enabled.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.BOOL)
    private Boolean doodleWallpaperEnabled;

    /**
     * The selected chat font size preset.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.INT32)
    private Integer fontSize;

    /**
     * Whether incoming photos are auto-downloaded.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    private Boolean photosAutodownloadEnabled;

    /**
     * Whether incoming audio is auto-downloaded.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.BOOL)
    private Boolean audiosAutodownloadEnabled;

    /**
     * Whether incoming videos are auto-downloaded.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.BOOL)
    private Boolean videosAutodownloadEnabled;

    /**
     * Whether incoming documents are auto-downloaded.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.BOOL)
    private Boolean documentsAutodownloadEnabled;

    /**
     * The chat notification tone override id.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.INT32)
    private Integer notificationToneId;

    /**
     * The media upload quality preset.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.ENUM)
    private SettingsSyncAction.MediaQualitySetting mediaUploadQuality;

    /**
     * Whether spell check is enabled in the composer.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    private Boolean spellCheckEnabled;

    /**
     * Whether pressing Enter sends the message.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.BOOL)
    private Boolean enterToSendEnabled;

    /**
     * Whether group message notifications are delivered.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.BOOL)
    private Boolean groupMessageNotificationEnabled;

    /**
     * Whether group reaction notifications are delivered.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.BOOL)
    private Boolean groupReactionsNotificationEnabled;

    /**
     * Whether status update notifications are delivered.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.BOOL)
    private Boolean statusNotificationEnabled;

    /**
     * The status notification tone id.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.INT32)
    private Integer statusNotificationToneId;

    /**
     * Whether call notifications play a ringtone.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    private Boolean playSoundForCallNotification;

    /**
     * The status-privacy setting; not persisted.
     */
    private StatusPrivacySetting statusPrivacy;

    /**
     * The account default-disappearing-message mode; not persisted.
     */
    private AccountDisappearingMode disappearingMode;

    /**
     * The frequency weights for recently used emoji; not persisted.
     */
    private List<RecentEmojiWeight> recentEmojiWeights;

    /**
     * The acknowledged Terms-of-Service notice ids; not persisted.
     */
    private Set<String> acknowledgedTosNotices;

    /**
     * The marketing opt-out list hashes keyed by category; not persisted.
     */
    private final ConcurrentMap<String, String> optOutListHashes;

    /**
     * The marketing opt-out list entries keyed by category; not persisted.
     */
    private final ConcurrentMap<String, List<OptOutEntry>> optOutListEntries;

    /**
     * The contact blacklist hashes keyed by category; not persisted.
     */
    private final ConcurrentMap<String, String> contactBlacklistHashes;

    /**
     * The contact blacklist entries keyed by category; not persisted.
     */
    private final ConcurrentMap<String, List<Jid>> contactBlacklistEntries;

    /**
     * The onboarding-hint state records keyed by hint id; not persisted.
     */
    private final ConcurrentMap<String, OnboardingHintState> onboardingHintStates;

    /**
     * The user identifier used for newsletter subscription tracking; not persisted.
     */
    private String newsletterSubscriptionUserIdentifier;

    /**
     * The saved interests used for channel recommendations; not persisted.
     */
    private String newsletterSavedInterests;

    /**
     * Whether status-post opt-in notification preferences are enabled; not persisted.
     */
    private Boolean statusPostOptInNotificationPreferencesEnabled;

    /**
     * Whether the user opted out of personalised channel recommendations; not persisted.
     */
    private Boolean channelsPersonalisedRecommendationOptOut;

    /**
     * The private-processing privacy setting status; not persisted.
     */
    private PrivateProcessingSettingAction.PrivateProcessingStatus privateProcessingStatus;

    /**
     * The notification-activity setting; not persisted.
     */
    private NotificationActivitySettingAction.NotificationActivitySetting notificationActivitySetting;

    /**
     * The username chat-start mode setting; not persisted.
     */
    private UsernameChatStartModeAction.ChatStartMode usernameChatStartMode;

    /**
     * Constructs a settings sub-store, defaulting the ephemeral timer and the sticker/label/quick-reply maps.
     *
     * @param privacySettingsValues            the privacy-setting values, never {@code null}
     * @param unarchiveChats                   the auto-unarchive flag
     * @param twentyFourHourFormat             the 24-hour-format flag
     * @param newChatsEphemeralTimer           the default ephemeral timer, or {@code null} for OFF
     * @param showSecurityNotifications        the security-notification flag
     * @param recentStickersMap                the recent-sticker map, or {@code null} for an empty map
     * @param favouriteStickersMap             the favourite-sticker map, or {@code null} for an empty map
     * @param quickRepliesMap                  the quick-reply map, or {@code null} for an empty map
     * @param labelsMap                        the label map, or {@code null} for an empty map
     * @param disableLinkPreviews              the link-preview-disabled flag
     * @param relayAllCalls                    the relay-all-calls flag
     * @param chatLockSettings                 the chat-lock settings, or {@code null}
     * @param startAtLogin                     the start-at-login preference, or {@code null}
     * @param minimizeToTray                   the minimise-to-tray preference, or {@code null}
     * @param replaceTextWithEmoji             the emoji-replacement preference, or {@code null}
     * @param bannerNotificationDisplayMode    the banner display mode, or {@code null}
     * @param unreadCounterBadgeDisplayMode    the badge display mode, or {@code null}
     * @param messagesNotificationEnabled      the message-notification preference, or {@code null}
     * @param callsNotificationEnabled         the call-notification preference, or {@code null}
     * @param reactionsNotificationEnabled     the reaction-notification preference, or {@code null}
     * @param statusReactionsNotificationEnabled the status-reaction-notification preference, or {@code null}
     * @param textPreviewForNotificationEnabled the text-preview preference, or {@code null}
     * @param defaultNotificationToneId        the default tone id, or {@code null}
     * @param groupDefaultNotificationToneId   the default group tone id, or {@code null}
     * @param appTheme                         the theme, or {@code null}
     * @param wallpaperId                      the wallpaper id, or {@code null}
     * @param doodleWallpaperEnabled           the doodle-wallpaper preference, or {@code null}
     * @param fontSize                         the font size, or {@code null}
     * @param photosAutodownloadEnabled        the photo-autodownload preference, or {@code null}
     * @param audiosAutodownloadEnabled        the audio-autodownload preference, or {@code null}
     * @param videosAutodownloadEnabled        the video-autodownload preference, or {@code null}
     * @param documentsAutodownloadEnabled     the document-autodownload preference, or {@code null}
     * @param notificationToneId               the chat tone override id, or {@code null}
     * @param mediaUploadQuality               the upload quality, or {@code null}
     * @param spellCheckEnabled                the spell-check preference, or {@code null}
     * @param enterToSendEnabled               the enter-to-send preference, or {@code null}
     * @param groupMessageNotificationEnabled  the group-message-notification preference, or {@code null}
     * @param groupReactionsNotificationEnabled the group-reaction-notification preference, or {@code null}
     * @param statusNotificationEnabled        the status-notification preference, or {@code null}
     * @param statusNotificationToneId         the status tone id, or {@code null}
     * @param playSoundForCallNotification     the call-sound preference, or {@code null}
     */
    ProtobufLinkedWhatsAppSettingsStore(List<PrivacySettingValue> privacySettingsValues, boolean unarchiveChats, boolean twentyFourHourFormat, ChatEphemeralTimer newChatsEphemeralTimer, boolean showSecurityNotifications, ConcurrentMap<String, Sticker> recentStickersMap, ConcurrentMap<String, Sticker> favouriteStickersMap, ConcurrentMap<String, QuickReply> quickRepliesMap, ConcurrentMap<String, Label> labelsMap, boolean disableLinkPreviews, boolean relayAllCalls, ChatLockSettings chatLockSettings, Boolean startAtLogin, Boolean minimizeToTray, Boolean replaceTextWithEmoji, SettingsSyncAction.DisplayMode bannerNotificationDisplayMode, SettingsSyncAction.DisplayMode unreadCounterBadgeDisplayMode, Boolean messagesNotificationEnabled, Boolean callsNotificationEnabled, Boolean reactionsNotificationEnabled, Boolean statusReactionsNotificationEnabled, Boolean textPreviewForNotificationEnabled, Integer defaultNotificationToneId, Integer groupDefaultNotificationToneId, AppTheme appTheme, Integer wallpaperId, Boolean doodleWallpaperEnabled, Integer fontSize, Boolean photosAutodownloadEnabled, Boolean audiosAutodownloadEnabled, Boolean videosAutodownloadEnabled, Boolean documentsAutodownloadEnabled, Integer notificationToneId, SettingsSyncAction.MediaQualitySetting mediaUploadQuality, Boolean spellCheckEnabled, Boolean enterToSendEnabled, Boolean groupMessageNotificationEnabled, Boolean groupReactionsNotificationEnabled, Boolean statusNotificationEnabled, Integer statusNotificationToneId, Boolean playSoundForCallNotification) {
        this.privacySettingsValues = new CopyOnWriteArrayList<>(privacySettingsValues == null ? List.of() : privacySettingsValues);
        this.unarchiveChats = unarchiveChats;
        this.twentyFourHourFormat = twentyFourHourFormat;
        this.newChatsEphemeralTimer = requireNonNullElse(newChatsEphemeralTimer, ChatEphemeralTimer.OFF);
        this.showSecurityNotifications = showSecurityNotifications;
        this.recentStickersMap = requireNonNullElseGet(recentStickersMap, ConcurrentHashMap::new);
        this.favouriteStickersMap = requireNonNullElseGet(favouriteStickersMap, ConcurrentHashMap::new);
        this.quickRepliesMap = requireNonNullElseGet(quickRepliesMap, ConcurrentHashMap::new);
        this.labelsMap = requireNonNullElseGet(labelsMap, ConcurrentHashMap::new);
        this.disableLinkPreviews = disableLinkPreviews;
        this.relayAllCalls = relayAllCalls;
        this.chatLockSettings = chatLockSettings;
        this.startAtLogin = startAtLogin;
        this.minimizeToTray = minimizeToTray;
        this.replaceTextWithEmoji = replaceTextWithEmoji;
        this.bannerNotificationDisplayMode = bannerNotificationDisplayMode;
        this.unreadCounterBadgeDisplayMode = unreadCounterBadgeDisplayMode;
        this.messagesNotificationEnabled = messagesNotificationEnabled;
        this.callsNotificationEnabled = callsNotificationEnabled;
        this.reactionsNotificationEnabled = reactionsNotificationEnabled;
        this.statusReactionsNotificationEnabled = statusReactionsNotificationEnabled;
        this.textPreviewForNotificationEnabled = textPreviewForNotificationEnabled;
        this.defaultNotificationToneId = defaultNotificationToneId;
        this.groupDefaultNotificationToneId = groupDefaultNotificationToneId;
        this.appTheme = appTheme;
        this.wallpaperId = wallpaperId;
        this.doodleWallpaperEnabled = doodleWallpaperEnabled;
        this.fontSize = fontSize;
        this.photosAutodownloadEnabled = photosAutodownloadEnabled;
        this.audiosAutodownloadEnabled = audiosAutodownloadEnabled;
        this.videosAutodownloadEnabled = videosAutodownloadEnabled;
        this.documentsAutodownloadEnabled = documentsAutodownloadEnabled;
        this.notificationToneId = notificationToneId;
        this.mediaUploadQuality = mediaUploadQuality;
        this.spellCheckEnabled = spellCheckEnabled;
        this.enterToSendEnabled = enterToSendEnabled;
        this.groupMessageNotificationEnabled = groupMessageNotificationEnabled;
        this.groupReactionsNotificationEnabled = groupReactionsNotificationEnabled;
        this.statusNotificationEnabled = statusNotificationEnabled;
        this.statusNotificationToneId = statusNotificationToneId;
        this.playSoundForCallNotification = playSoundForCallNotification;
        this.recentEmojiWeights = new CopyOnWriteArrayList<>();
        this.acknowledgedTosNotices = ConcurrentHashMap.newKeySet();
        this.optOutListHashes = new ConcurrentHashMap<>();
        this.optOutListEntries = new ConcurrentHashMap<>();
        this.contactBlacklistHashes = new ConcurrentHashMap<>();
        this.contactBlacklistEntries = new ConcurrentHashMap<>();
        this.onboardingHintStates = new ConcurrentHashMap<>();
    }

    /**
     * Returns the live privacy-setting list backing this store.
     *
     * @return the live privacy-setting list
     */
    List<PrivacySettingValue> privacySettingsValues() {
        return privacySettingsValues;
    }

    /**
     * Returns the live recent-sticker map backing this store.
     *
     * @return the live recent-sticker map
     */
    ConcurrentMap<String, Sticker> recentStickersMap() {
        return recentStickersMap;
    }

    /**
     * Returns the live favourite-sticker map backing this store.
     *
     * @return the live favourite-sticker map
     */
    ConcurrentMap<String, Sticker> favouriteStickersMap() {
        return favouriteStickersMap;
    }

    /**
     * Returns the live quick-reply map backing this store.
     *
     * @return the live quick-reply map
     */
    ConcurrentMap<String, QuickReply> quickRepliesMap() {
        return quickRepliesMap;
    }

    /**
     * Returns the live label map backing this store.
     *
     * @return the live label map
     */
    ConcurrentMap<String, Label> labelsMap() {
        return labelsMap;
    }

    @Override
    public Collection<PrivacySettingValue> privacySettings() {
        return List.copyOf(privacySettingsValues);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V extends PrivacySettingValue> Optional<V> findPrivacySetting(PrivacySettingType<V> type) {
        if (type == null) {
            return Optional.empty();
        }
        for (var value : privacySettingsValues) {
            if (value.type().equals(type)) {
                return Optional.of((V) value);
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized void addPrivacySetting(PrivacySettingValue value) {
        Objects.requireNonNull(value, "value cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "setting privacy setting {0}", value.type());
        privacySettingsValues.removeIf(existing -> existing.type().equals(value.type()));
        privacySettingsValues.add(value);
    }

    @Override
    public Optional<StatusPrivacySetting> statusPrivacy() {
        return Optional.ofNullable(statusPrivacy);
    }

    @Override
    public LinkedWhatsAppSettingsStore setStatusPrivacy(StatusPrivacySetting statusPrivacy) {
        this.statusPrivacy = statusPrivacy;
        return this;
    }

    @Override
    public Optional<AccountDisappearingMode> disappearingMode() {
        return Optional.ofNullable(disappearingMode);
    }

    @Override
    public LinkedWhatsAppSettingsStore setDisappearingMode(AccountDisappearingMode disappearingMode) {
        this.disappearingMode = disappearingMode;
        return this;
    }

    @Override
    public boolean unarchiveChats() {
        return unarchiveChats;
    }

    @Override
    public LinkedWhatsAppSettingsStore setUnarchiveChats(boolean unarchiveChats) {
        this.unarchiveChats = unarchiveChats;
        return this;
    }

    @Override
    public boolean twentyFourHourFormat() {
        return twentyFourHourFormat;
    }

    @Override
    public LinkedWhatsAppSettingsStore setTwentyFourHourFormat(boolean twentyFourHourFormat) {
        this.twentyFourHourFormat = twentyFourHourFormat;
        return this;
    }

    @Override
    public ChatEphemeralTimer newChatsEphemeralTimer() {
        return newChatsEphemeralTimer;
    }

    @Override
    public LinkedWhatsAppSettingsStore setNewChatsEphemeralTimer(ChatEphemeralTimer newChatsEphemeralTimer) {
        this.newChatsEphemeralTimer = Objects.requireNonNull(newChatsEphemeralTimer, "newChatsEphemeralTimer cannot be null");
        return this;
    }

    @Override
    public boolean showSecurityNotifications() {
        return showSecurityNotifications;
    }

    @Override
    public LinkedWhatsAppSettingsStore setShowSecurityNotifications(boolean showSecurityNotifications) {
        this.showSecurityNotifications = showSecurityNotifications;
        return this;
    }

    @Override
    public boolean disableLinkPreviews() {
        return disableLinkPreviews;
    }

    @Override
    public LinkedWhatsAppSettingsStore setDisableLinkPreviews(boolean disableLinkPreviews) {
        this.disableLinkPreviews = disableLinkPreviews;
        return this;
    }

    @Override
    public boolean relayAllCalls() {
        return relayAllCalls;
    }

    @Override
    public LinkedWhatsAppSettingsStore setRelayAllCalls(boolean relayAllCalls) {
        this.relayAllCalls = relayAllCalls;
        return this;
    }

    @Override
    public Optional<ChatLockSettings> chatLockSettings() {
        return Optional.ofNullable(chatLockSettings);
    }

    @Override
    public LinkedWhatsAppSettingsStore setChatLockSettings(ChatLockSettings chatLockSettings) {
        this.chatLockSettings = chatLockSettings;
        return this;
    }

    @Override
    public Optional<Sticker> findRecentSticker(String stickerHash) {
        return Optional.ofNullable(recentStickersMap.get(stickerHash));
    }

    @Override
    public void addRecentSticker(String stickerHash, Sticker sticker) {
        Objects.requireNonNull(stickerHash, "stickerHash cannot be null");
        Objects.requireNonNull(sticker, "sticker cannot be null");
        recentStickersMap.put(stickerHash, sticker);
    }

    @Override
    public Optional<Sticker> removeRecentSticker(String stickerHash) {
        return Optional.ofNullable(recentStickersMap.remove(stickerHash));
    }

    @Override
    public SequencedCollection<Sticker> recentStickers() {
        return List.copyOf(recentStickersMap.values());
    }

    @Override
    public int removeAllRecentAvatarStickers() {
        var iterator = recentStickersMap.entrySet().iterator();
        var removed = 0;
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isAvatar()) {
                iterator.remove();
                removed++;
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removed {0} recent avatar stickers", removed);
        return removed;
    }

    @Override
    public Optional<Sticker> findFavouriteSticker(String stickerHash) {
        return Optional.ofNullable(favouriteStickersMap.get(stickerHash));
    }

    @Override
    public void addFavouriteSticker(String stickerHash, Sticker sticker) {
        Objects.requireNonNull(stickerHash, "stickerHash cannot be null");
        Objects.requireNonNull(sticker, "sticker cannot be null");
        favouriteStickersMap.put(stickerHash, sticker);
    }

    @Override
    public Optional<Sticker> removeFavouriteSticker(String stickerHash) {
        return Optional.ofNullable(favouriteStickersMap.remove(stickerHash));
    }

    @Override
    public SequencedCollection<Sticker> favouriteStickers() {
        return List.copyOf(favouriteStickersMap.values());
    }

    @Override
    public Optional<QuickReply> findQuickReply(String id) {
        return id == null
                ? Optional.empty()
                : Optional.ofNullable(quickRepliesMap.get(id));
    }

    @Override
    public void addQuickReply(QuickReply quickReply) {
        Objects.requireNonNull(quickReply, "quickReply cannot be null");
        quickRepliesMap.put(quickReply.id(), quickReply);
    }

    @Override
    public Optional<QuickReply> removeQuickReply(String id) {
        return id == null
                ? Optional.empty()
                : Optional.ofNullable(quickRepliesMap.remove(id));
    }

    @Override
    public List<QuickReply> quickReplies() {
        return List.copyOf(quickRepliesMap.values());
    }

    @Override
    public void addLabel(Label label) {
        Objects.requireNonNull(label, "label cannot be null");
        labelsMap.put(label.id(), label);
    }

    @Override
    public Collection<Label> labels() {
        return List.copyOf(labelsMap.values());
    }

    @Override
    public Optional<Label> removeLabel(String labelId) {
        return Optional.ofNullable(labelsMap.remove(labelId));
    }

    @Override
    public Optional<Label> findLabel(String labelId) {
        return Optional.ofNullable(labelsMap.get(labelId));
    }

    @Override
    public List<RecentEmojiWeight> recentEmojiWeights() {
        return List.copyOf(recentEmojiWeights);
    }

    @Override
    public LinkedWhatsAppSettingsStore setRecentEmojiWeights(List<RecentEmojiWeight> weights) {
        this.recentEmojiWeights = new CopyOnWriteArrayList<>(Objects.requireNonNull(weights, "weights cannot be null"));
        return this;
    }

    @Override
    public Set<String> acknowledgedTosNotices() {
        return Set.copyOf(acknowledgedTosNotices);
    }

    @Override
    public LinkedWhatsAppSettingsStore setAcknowledgedTosNotices(Set<String> noticeIds) {
        this.acknowledgedTosNotices = noticeIds == null ? ConcurrentHashMap.newKeySet() : ConcurrentHashMap.newKeySet(noticeIds.size());
        if (noticeIds != null) {
            this.acknowledgedTosNotices.addAll(noticeIds);
        }
        return this;
    }

    @Override
    public Optional<String> optOutListHash(String category) {
        return Optional.ofNullable(optOutListHashes.get(Objects.requireNonNull(category, "category cannot be null")));
    }

    @Override
    public List<OptOutEntry> optOutListEntries(String category) {
        var entries = optOutListEntries.get(Objects.requireNonNull(category, "category cannot be null"));
        return entries == null ? List.of() : entries;
    }

    @Override
    public LinkedWhatsAppSettingsStore setOptOutList(String category, String hash, List<OptOutEntry> entries) {
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(entries, "entries cannot be null");
        if (hash == null) {
            optOutListHashes.remove(category);
        } else {
            optOutListHashes.put(category, hash);
        }
        optOutListEntries.put(category, List.copyOf(entries));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opt-out list updated for category {0}, entries={1}", category, entries.size());
        return this;
    }

    @Override
    public Optional<String> contactBlacklistHash(String category) {
        return Optional.ofNullable(contactBlacklistHashes.get(Objects.requireNonNull(category, "category cannot be null")));
    }

    @Override
    public List<Jid> contactBlacklistEntries(String category) {
        var entries = contactBlacklistEntries.get(Objects.requireNonNull(category, "category cannot be null"));
        return entries == null ? List.of() : entries;
    }

    @Override
    public LinkedWhatsAppSettingsStore setContactBlacklist(String category, String hash, List<Jid> entries) {
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(entries, "entries cannot be null");
        if (hash == null) {
            contactBlacklistHashes.remove(category);
        } else {
            contactBlacklistHashes.put(category, hash);
        }
        contactBlacklistEntries.put(category, List.copyOf(entries));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "contact blacklist updated for category {0}, entries={1}", category, entries.size());
        return this;
    }

    @Override
    public Collection<OnboardingHintState> onboardingHintStates() {
        return List.copyOf(onboardingHintStates.values());
    }

    @Override
    public Optional<OnboardingHintState> findOnboardingHintState(String hintId) {
        if (hintId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(onboardingHintStates.get(hintId));
    }

    @Override
    public LinkedWhatsAppSettingsStore putOnboardingHintState(OnboardingHintState state) {
        Objects.requireNonNull(state, "state cannot be null");
        onboardingHintStates.put(state.hintId(), state);
        return this;
    }

    @Override
    public Optional<OnboardingHintState> removeOnboardingHintState(String hintId) {
        if (hintId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(onboardingHintStates.remove(hintId));
    }

    @Override
    public LinkedWhatsAppSettingsStore clearOnboardingHintStates() {
        onboardingHintStates.clear();
        return this;
    }

    @Override
    public Optional<NotificationActivitySettingAction.NotificationActivitySetting> notificationActivitySetting() {
        return Optional.ofNullable(notificationActivitySetting);
    }

    @Override
    public LinkedWhatsAppSettingsStore setNotificationActivitySetting(NotificationActivitySettingAction.NotificationActivitySetting setting) {
        this.notificationActivitySetting = setting;
        return this;
    }

    @Override
    public Optional<PrivateProcessingSettingAction.PrivateProcessingStatus> privateProcessingStatus() {
        return Optional.ofNullable(privateProcessingStatus);
    }

    @Override
    public LinkedWhatsAppSettingsStore setPrivateProcessingStatus(PrivateProcessingSettingAction.PrivateProcessingStatus status) {
        this.privateProcessingStatus = status;
        return this;
    }

    @Override
    public Optional<String> newsletterSubscriptionUserIdentifier() {
        return Optional.ofNullable(newsletterSubscriptionUserIdentifier);
    }

    @Override
    public LinkedWhatsAppSettingsStore setNewsletterSubscriptionUserIdentifier(String identifier) {
        this.newsletterSubscriptionUserIdentifier = identifier;
        return this;
    }

    @Override
    public Optional<String> newsletterSavedInterests() {
        return Optional.ofNullable(newsletterSavedInterests);
    }

    @Override
    public LinkedWhatsAppSettingsStore setNewsletterSavedInterests(String interests) {
        this.newsletterSavedInterests = interests;
        return this;
    }

    @Override
    public Optional<Boolean> statusPostOptInNotificationPreferencesEnabled() {
        return Optional.ofNullable(statusPostOptInNotificationPreferencesEnabled);
    }

    @Override
    public LinkedWhatsAppSettingsStore setStatusPostOptInNotificationPreferencesEnabled(Boolean enabled) {
        this.statusPostOptInNotificationPreferencesEnabled = enabled;
        return this;
    }

    @Override
    public Optional<Boolean> channelsPersonalisedRecommendationOptOut() {
        return Optional.ofNullable(channelsPersonalisedRecommendationOptOut);
    }

    @Override
    public LinkedWhatsAppSettingsStore setChannelsPersonalisedRecommendationOptOut(Boolean optOut) {
        this.channelsPersonalisedRecommendationOptOut = optOut;
        return this;
    }

    @Override
    public boolean startAtLogin() {
        return startAtLogin != null && startAtLogin;
    }

    @Override
    public LinkedWhatsAppSettingsStore setStartAtLogin(boolean startAtLogin) {
        this.startAtLogin = startAtLogin;
        return this;
    }

    @Override
    public boolean minimizeToTray() {
        return minimizeToTray != null && minimizeToTray;
    }

    @Override
    public LinkedWhatsAppSettingsStore setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
        return this;
    }

    @Override
    public boolean replaceTextWithEmoji() {
        return replaceTextWithEmoji != null && replaceTextWithEmoji;
    }

    @Override
    public LinkedWhatsAppSettingsStore setReplaceTextWithEmoji(boolean replaceTextWithEmoji) {
        this.replaceTextWithEmoji = replaceTextWithEmoji;
        return this;
    }

    @Override
    public Optional<SettingsSyncAction.DisplayMode> bannerNotificationDisplayMode() {
        return Optional.ofNullable(bannerNotificationDisplayMode);
    }

    @Override
    public LinkedWhatsAppSettingsStore setBannerNotificationDisplayMode(SettingsSyncAction.DisplayMode mode) {
        this.bannerNotificationDisplayMode = mode;
        return this;
    }

    @Override
    public Optional<SettingsSyncAction.DisplayMode> unreadCounterBadgeDisplayMode() {
        return Optional.ofNullable(unreadCounterBadgeDisplayMode);
    }

    @Override
    public LinkedWhatsAppSettingsStore setUnreadCounterBadgeDisplayMode(SettingsSyncAction.DisplayMode mode) {
        this.unreadCounterBadgeDisplayMode = mode;
        return this;
    }

    @Override
    public boolean messagesNotificationEnabled() {
        return messagesNotificationEnabled != null && messagesNotificationEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setMessagesNotificationEnabled(boolean enabled) {
        this.messagesNotificationEnabled = enabled;
        return this;
    }

    @Override
    public boolean callsNotificationEnabled() {
        return callsNotificationEnabled != null && callsNotificationEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setCallsNotificationEnabled(boolean enabled) {
        this.callsNotificationEnabled = enabled;
        return this;
    }

    @Override
    public boolean reactionsNotificationEnabled() {
        return reactionsNotificationEnabled != null && reactionsNotificationEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setReactionsNotificationEnabled(boolean enabled) {
        this.reactionsNotificationEnabled = enabled;
        return this;
    }

    @Override
    public boolean statusReactionsNotificationEnabled() {
        return statusReactionsNotificationEnabled != null && statusReactionsNotificationEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setStatusReactionsNotificationEnabled(boolean enabled) {
        this.statusReactionsNotificationEnabled = enabled;
        return this;
    }

    @Override
    public boolean textPreviewForNotificationEnabled() {
        return textPreviewForNotificationEnabled != null && textPreviewForNotificationEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setTextPreviewForNotificationEnabled(boolean enabled) {
        this.textPreviewForNotificationEnabled = enabled;
        return this;
    }

    @Override
    public OptionalInt defaultNotificationToneId() {
        return defaultNotificationToneId == null ? OptionalInt.empty() : OptionalInt.of(defaultNotificationToneId);
    }

    @Override
    public LinkedWhatsAppSettingsStore setDefaultNotificationToneId(Integer toneId) {
        this.defaultNotificationToneId = toneId;
        return this;
    }

    @Override
    public OptionalInt groupDefaultNotificationToneId() {
        return groupDefaultNotificationToneId == null ? OptionalInt.empty() : OptionalInt.of(groupDefaultNotificationToneId);
    }

    @Override
    public LinkedWhatsAppSettingsStore setGroupDefaultNotificationToneId(Integer toneId) {
        this.groupDefaultNotificationToneId = toneId;
        return this;
    }

    @Override
    public Optional<AppTheme> appTheme() {
        return Optional.ofNullable(appTheme);
    }

    @Override
    public LinkedWhatsAppSettingsStore setAppTheme(AppTheme appTheme) {
        this.appTheme = appTheme;
        return this;
    }

    @Override
    public OptionalInt wallpaperId() {
        return wallpaperId == null ? OptionalInt.empty() : OptionalInt.of(wallpaperId);
    }

    @Override
    public LinkedWhatsAppSettingsStore setWallpaperId(Integer wallpaperId) {
        this.wallpaperId = wallpaperId;
        return this;
    }

    @Override
    public boolean doodleWallpaperEnabled() {
        return doodleWallpaperEnabled != null && doodleWallpaperEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setDoodleWallpaperEnabled(boolean enabled) {
        this.doodleWallpaperEnabled = enabled;
        return this;
    }

    @Override
    public OptionalInt fontSize() {
        return fontSize == null ? OptionalInt.empty() : OptionalInt.of(fontSize);
    }

    @Override
    public LinkedWhatsAppSettingsStore setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    @Override
    public boolean photosAutodownloadEnabled() {
        return photosAutodownloadEnabled != null && photosAutodownloadEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setPhotosAutodownloadEnabled(boolean enabled) {
        this.photosAutodownloadEnabled = enabled;
        return this;
    }

    @Override
    public boolean audiosAutodownloadEnabled() {
        return audiosAutodownloadEnabled != null && audiosAutodownloadEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setAudiosAutodownloadEnabled(boolean enabled) {
        this.audiosAutodownloadEnabled = enabled;
        return this;
    }

    @Override
    public boolean videosAutodownloadEnabled() {
        return videosAutodownloadEnabled != null && videosAutodownloadEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setVideosAutodownloadEnabled(boolean enabled) {
        this.videosAutodownloadEnabled = enabled;
        return this;
    }

    @Override
    public boolean documentsAutodownloadEnabled() {
        return documentsAutodownloadEnabled != null && documentsAutodownloadEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setDocumentsAutodownloadEnabled(boolean enabled) {
        this.documentsAutodownloadEnabled = enabled;
        return this;
    }

    @Override
    public OptionalInt notificationToneId() {
        return notificationToneId == null ? OptionalInt.empty() : OptionalInt.of(notificationToneId);
    }

    @Override
    public LinkedWhatsAppSettingsStore setNotificationToneId(Integer toneId) {
        this.notificationToneId = toneId;
        return this;
    }

    @Override
    public Optional<SettingsSyncAction.MediaQualitySetting> mediaUploadQuality() {
        return Optional.ofNullable(mediaUploadQuality);
    }

    @Override
    public LinkedWhatsAppSettingsStore setMediaUploadQuality(SettingsSyncAction.MediaQualitySetting quality) {
        this.mediaUploadQuality = quality;
        return this;
    }

    @Override
    public boolean spellCheckEnabled() {
        return spellCheckEnabled != null && spellCheckEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setSpellCheckEnabled(boolean enabled) {
        this.spellCheckEnabled = enabled;
        return this;
    }

    @Override
    public boolean enterToSendEnabled() {
        return enterToSendEnabled != null && enterToSendEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setEnterToSendEnabled(boolean enabled) {
        this.enterToSendEnabled = enabled;
        return this;
    }

    @Override
    public boolean groupMessageNotificationEnabled() {
        return groupMessageNotificationEnabled != null && groupMessageNotificationEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setGroupMessageNotificationEnabled(boolean enabled) {
        this.groupMessageNotificationEnabled = enabled;
        return this;
    }

    @Override
    public boolean groupReactionsNotificationEnabled() {
        return groupReactionsNotificationEnabled != null && groupReactionsNotificationEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setGroupReactionsNotificationEnabled(boolean enabled) {
        this.groupReactionsNotificationEnabled = enabled;
        return this;
    }

    @Override
    public boolean statusNotificationEnabled() {
        return statusNotificationEnabled != null && statusNotificationEnabled;
    }

    @Override
    public LinkedWhatsAppSettingsStore setStatusNotificationEnabled(boolean enabled) {
        this.statusNotificationEnabled = enabled;
        return this;
    }

    @Override
    public OptionalInt statusNotificationToneId() {
        return statusNotificationToneId == null ? OptionalInt.empty() : OptionalInt.of(statusNotificationToneId);
    }

    @Override
    public LinkedWhatsAppSettingsStore setStatusNotificationToneId(Integer toneId) {
        this.statusNotificationToneId = toneId;
        return this;
    }

    @Override
    public boolean playSoundForCallNotification() {
        return playSoundForCallNotification != null && playSoundForCallNotification;
    }

    @Override
    public LinkedWhatsAppSettingsStore setPlaySoundForCallNotification(boolean enabled) {
        this.playSoundForCallNotification = enabled;
        return this;
    }

    @Override
    public Optional<UsernameChatStartModeAction.ChatStartMode> usernameChatStartMode() {
        return Optional.ofNullable(usernameChatStartMode);
    }

    @Override
    public LinkedWhatsAppSettingsStore setUsernameChatStartMode(UsernameChatStartModeAction.ChatStartMode mode) {
        this.usernameChatStartMode = mode;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtobufLinkedWhatsAppSettingsStore that)) {
            return false;
        }
        return unarchiveChats == that.unarchiveChats
               && twentyFourHourFormat == that.twentyFourHourFormat
               && showSecurityNotifications == that.showSecurityNotifications
               && disableLinkPreviews == that.disableLinkPreviews
               && relayAllCalls == that.relayAllCalls
               && Objects.equals(privacySettingsValues, that.privacySettingsValues)
               && newChatsEphemeralTimer == that.newChatsEphemeralTimer
               && Objects.equals(recentStickersMap, that.recentStickersMap)
               && Objects.equals(favouriteStickersMap, that.favouriteStickersMap)
               && Objects.equals(quickRepliesMap, that.quickRepliesMap)
               && Objects.equals(labelsMap, that.labelsMap)
               && Objects.equals(chatLockSettings, that.chatLockSettings)
               && Objects.equals(startAtLogin, that.startAtLogin)
               && Objects.equals(minimizeToTray, that.minimizeToTray)
               && Objects.equals(replaceTextWithEmoji, that.replaceTextWithEmoji)
               && bannerNotificationDisplayMode == that.bannerNotificationDisplayMode
               && unreadCounterBadgeDisplayMode == that.unreadCounterBadgeDisplayMode
               && Objects.equals(messagesNotificationEnabled, that.messagesNotificationEnabled)
               && Objects.equals(callsNotificationEnabled, that.callsNotificationEnabled)
               && Objects.equals(reactionsNotificationEnabled, that.reactionsNotificationEnabled)
               && Objects.equals(statusReactionsNotificationEnabled, that.statusReactionsNotificationEnabled)
               && Objects.equals(textPreviewForNotificationEnabled, that.textPreviewForNotificationEnabled)
               && Objects.equals(defaultNotificationToneId, that.defaultNotificationToneId)
               && Objects.equals(groupDefaultNotificationToneId, that.groupDefaultNotificationToneId)
               && appTheme == that.appTheme
               && Objects.equals(wallpaperId, that.wallpaperId)
               && Objects.equals(doodleWallpaperEnabled, that.doodleWallpaperEnabled)
               && Objects.equals(fontSize, that.fontSize)
               && Objects.equals(photosAutodownloadEnabled, that.photosAutodownloadEnabled)
               && Objects.equals(audiosAutodownloadEnabled, that.audiosAutodownloadEnabled)
               && Objects.equals(videosAutodownloadEnabled, that.videosAutodownloadEnabled)
               && Objects.equals(documentsAutodownloadEnabled, that.documentsAutodownloadEnabled)
               && Objects.equals(notificationToneId, that.notificationToneId)
               && mediaUploadQuality == that.mediaUploadQuality
               && Objects.equals(spellCheckEnabled, that.spellCheckEnabled)
               && Objects.equals(enterToSendEnabled, that.enterToSendEnabled)
               && Objects.equals(groupMessageNotificationEnabled, that.groupMessageNotificationEnabled)
               && Objects.equals(groupReactionsNotificationEnabled, that.groupReactionsNotificationEnabled)
               && Objects.equals(statusNotificationEnabled, that.statusNotificationEnabled)
               && Objects.equals(statusNotificationToneId, that.statusNotificationToneId)
               && Objects.equals(playSoundForCallNotification, that.playSoundForCallNotification)
               && Objects.equals(statusPrivacy, that.statusPrivacy)
               && Objects.equals(disappearingMode, that.disappearingMode)
               && Objects.equals(recentEmojiWeights, that.recentEmojiWeights)
               && Objects.equals(acknowledgedTosNotices, that.acknowledgedTosNotices)
               && Objects.equals(optOutListHashes, that.optOutListHashes)
               && Objects.equals(optOutListEntries, that.optOutListEntries)
               && Objects.equals(contactBlacklistHashes, that.contactBlacklistHashes)
               && Objects.equals(contactBlacklistEntries, that.contactBlacklistEntries)
               && Objects.equals(onboardingHintStates, that.onboardingHintStates)
               && Objects.equals(newsletterSubscriptionUserIdentifier, that.newsletterSubscriptionUserIdentifier)
               && Objects.equals(newsletterSavedInterests, that.newsletterSavedInterests)
               && Objects.equals(statusPostOptInNotificationPreferencesEnabled, that.statusPostOptInNotificationPreferencesEnabled)
               && Objects.equals(channelsPersonalisedRecommendationOptOut, that.channelsPersonalisedRecommendationOptOut)
               && Objects.equals(privateProcessingStatus, that.privateProcessingStatus)
               && Objects.equals(notificationActivitySetting, that.notificationActivitySetting)
               && usernameChatStartMode == that.usernameChatStartMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(privacySettingsValues, unarchiveChats, twentyFourHourFormat, newChatsEphemeralTimer,
                showSecurityNotifications, recentStickersMap, favouriteStickersMap, quickRepliesMap, labelsMap,
                disableLinkPreviews, relayAllCalls, chatLockSettings, startAtLogin, minimizeToTray, replaceTextWithEmoji,
                bannerNotificationDisplayMode, unreadCounterBadgeDisplayMode, messagesNotificationEnabled,
                callsNotificationEnabled, reactionsNotificationEnabled, statusReactionsNotificationEnabled,
                textPreviewForNotificationEnabled, defaultNotificationToneId, groupDefaultNotificationToneId, appTheme,
                wallpaperId, doodleWallpaperEnabled, fontSize, photosAutodownloadEnabled, audiosAutodownloadEnabled,
                videosAutodownloadEnabled, documentsAutodownloadEnabled, notificationToneId, mediaUploadQuality,
                spellCheckEnabled, enterToSendEnabled, groupMessageNotificationEnabled, groupReactionsNotificationEnabled,
                statusNotificationEnabled, statusNotificationToneId, playSoundForCallNotification, statusPrivacy,
                disappearingMode, recentEmojiWeights, acknowledgedTosNotices, optOutListHashes, optOutListEntries,
                contactBlacklistHashes, contactBlacklistEntries, onboardingHintStates,
                newsletterSubscriptionUserIdentifier, newsletterSavedInterests,
                statusPostOptInNotificationPreferencesEnabled, channelsPersonalisedRecommendationOptOut,
                privateProcessingStatus, notificationActivitySetting, usernameChatStartMode);
    }
}
