package com.github.auties00.cobalt.wire.linked.sync.action.setting;

import com.github.auties00.cobalt.wire.linked.setting.AppTheme;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents the aggregated desktop client settings bundle, synchronised
 * across the user's linked WhatsApp Desktop installations.
 *
 * <p>This single action groups every preference exposed by the desktop
 * client: startup behaviour, notification routing, notification tones,
 * appearance (theme, wallpaper, font size), media autodownload rules,
 * typing aids (spell check, enter-to-send), group notification toggles
 * and call-notification sound selection. Each individual preference is
 * an optional field so that a mutation can carry only the value that
 * changed; see {@link SettingKey} for the canonical identifier used in
 * the index arguments of such partial updates.
 *
 * <p>All fields use boxed types so that "unset" can be distinguished from
 * "explicitly false" or "explicitly zero"; Boolean getters return
 * {@code false} when unset while integer getters return empty
 * {@link OptionalInt} values.
 */
@ProtobufMessage(name = "SyncActionValue.SettingsSyncAction")
public final class SettingsSyncAction implements SyncAction<SettingsSyncActionArgs> {
    /**
     * Canonical action name used when this setting is encoded in an app
     * state sync mutation.
     */
    public static final String ACTION_NAME = "settings_sync";

    /**
     * Canonical action version emitted alongside this setting when it is
     * published to the app state sync collection.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * App state collection that stores this setting.
     *
     * <p>Desktop preferences are not latency-sensitive and therefore live
     * in the lowest-priority regular patch type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for this setting.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version for this setting.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Whether the desktop client launches automatically at OS login.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean startAtLogin;

    /**
     * Whether closing the main window minimises the client to the system
     * tray instead of terminating the process.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean minimizeToTray;

    /**
     * Desktop-specific language override.
     *
     * <p>Distinct from the account-wide {@link LocaleSetting}: this field
     * lets the user pin the desktop UI language independently of mobile
     * clients.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String language;

    /**
     * Whether typed emoticons are automatically replaced with their
     * graphical emoji equivalents while composing.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean replaceTextWithEmoji;

    /**
     * When banner notifications are shown on the desktop.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    DisplayMode bannerNotificationDisplayMode;

    /**
     * When the unread counter badge is shown on the application icon.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    DisplayMode unreadCounterBadgeDisplayMode;

    /**
     * Whether incoming message notifications are delivered at all.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    Boolean isMessagesNotificationEnabled;

    /**
     * Whether incoming call notifications are delivered.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    Boolean isCallsNotificationEnabled;

    /**
     * Whether reaction notifications are delivered.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
    Boolean isReactionsNotificationEnabled;

    /**
     * Whether status reaction notifications are delivered.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
    Boolean isStatusReactionsNotificationEnabled;

    /**
     * Whether notification banners include a text preview of the message.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    Boolean isTextPreviewForNotificationEnabled;

    /**
     * Default tone identifier used for one-to-one chat notifications.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.INT32)
    Integer defaultNotificationToneId;

    /**
     * Default tone identifier used for group chat notifications.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.INT32)
    Integer groupDefaultNotificationToneId;

    /**
     * Selected application theme (light, dark, system-default).
     *
     * <p>Mirrors the {@code ThemesSettingValue} JS enum that WhatsApp Web
     * defines in its bundle: the wire format is an {@code INT32} carrying
     * the {@link AppTheme#index()} of the selected constant.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.ENUM)
    AppTheme appTheme;

    /**
     * Identifier of the selected chat wallpaper.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.INT32)
    Integer wallpaperId;

    /**
     * Whether the doodle overlay is drawn on top of the chat wallpaper.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    Boolean isDoodleWallpaperEnabled;

    /**
     * Selected font size preset for chat rendering.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.INT32)
    Integer fontSize;

    /**
     * Whether incoming images are automatically downloaded.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    Boolean isPhotosAutodownloadEnabled;

    /**
     * Whether incoming audio messages are automatically downloaded.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.BOOL)
    Boolean isAudiosAutodownloadEnabled;

    /**
     * Whether incoming videos are automatically downloaded.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.BOOL)
    Boolean isVideosAutodownloadEnabled;

    /**
     * Whether incoming documents are automatically downloaded.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
    Boolean isDocumentsAutodownloadEnabled;

    /**
     * Whether URL link previews are suppressed for incoming and outgoing
     * messages.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
    Boolean disableLinkPreviews;

    /**
     * Identifier of the chat notification tone override, if set.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.INT32)
    Integer notificationToneId;

    /**
     * Quality preset applied when uploading photos and videos.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.ENUM)
    MediaQualitySetting mediaUploadQuality;

    /**
     * Whether spell check is enabled in the message composer.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    Boolean isSpellCheckEnabled;

    /**
     * Whether pressing Enter sends the current message instead of
     * inserting a newline.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.BOOL)
    Boolean isEnterToSendEnabled;

    /**
     * Whether group message notifications are delivered.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.BOOL)
    Boolean isGroupMessageNotificationEnabled;

    /**
     * Whether group reaction notifications are delivered.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.BOOL)
    Boolean isGroupReactionsNotificationEnabled;

    /**
     * Whether status update notifications are delivered.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    Boolean isStatusNotificationEnabled;

    /**
     * Identifier of the status notification tone.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.INT32)
    Integer statusNotificationToneId;

    /**
     * Whether call notifications play a ringtone in addition to showing
     * the banner.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.BOOL)
    Boolean shouldPlaySoundForCallNotification;


    /**
     * Constructs a fully populated settings snapshot.
     *
     * <p>Every parameter may be {@code null}; a missing value is treated
     * as "not set" rather than as a specific default, which lets consumers
     * distinguish a deliberate disable from a never-configured preference.
     *
     * @param startAtLogin                         launch at OS login
     * @param minimizeToTray                       minimise to tray on window close
     * @param language                             desktop UI language override
     * @param replaceTextWithEmoji                 auto-replace emoticons with emoji
     * @param bannerNotificationDisplayMode        banner notification display mode
     * @param unreadCounterBadgeDisplayMode        unread badge display mode
     * @param isMessagesNotificationEnabled        messages notifications
     * @param isCallsNotificationEnabled           calls notifications
     * @param isReactionsNotificationEnabled       chat reactions notifications
     * @param isStatusReactionsNotificationEnabled status reactions notifications
     * @param isTextPreviewForNotificationEnabled  text preview in notifications
     * @param defaultNotificationToneId            default chat tone id
     * @param groupDefaultNotificationToneId       default group chat tone id
     * @param appTheme                             selected theme id
     * @param wallpaperId                          selected wallpaper id
     * @param isDoodleWallpaperEnabled             doodle wallpaper overlay
     * @param fontSize                             chat font size preset
     * @param isPhotosAutodownloadEnabled          photos autodownload
     * @param isAudiosAutodownloadEnabled          audios autodownload
     * @param isVideosAutodownloadEnabled          videos autodownload
     * @param isDocumentsAutodownloadEnabled       documents autodownload
     * @param disableLinkPreviews                  disable link previews
     * @param notificationToneId                   chat notification tone override id
     * @param mediaUploadQuality                   media upload quality preset
     * @param isSpellCheckEnabled                  composer spell check
     * @param isEnterToSendEnabled                 enter-to-send behaviour
     * @param isGroupMessageNotificationEnabled    group message notifications
     * @param isGroupReactionsNotificationEnabled  group reaction notifications
     * @param isStatusNotificationEnabled          status update notifications
     * @param statusNotificationToneId             status notification tone id
     * @param shouldPlaySoundForCallNotification   call notification sound
     */
    SettingsSyncAction(Boolean startAtLogin, Boolean minimizeToTray, String language, Boolean replaceTextWithEmoji, DisplayMode bannerNotificationDisplayMode, DisplayMode unreadCounterBadgeDisplayMode, Boolean isMessagesNotificationEnabled, Boolean isCallsNotificationEnabled, Boolean isReactionsNotificationEnabled, Boolean isStatusReactionsNotificationEnabled, Boolean isTextPreviewForNotificationEnabled, Integer defaultNotificationToneId, Integer groupDefaultNotificationToneId, AppTheme appTheme, Integer wallpaperId, Boolean isDoodleWallpaperEnabled, Integer fontSize, Boolean isPhotosAutodownloadEnabled, Boolean isAudiosAutodownloadEnabled, Boolean isVideosAutodownloadEnabled, Boolean isDocumentsAutodownloadEnabled, Boolean disableLinkPreviews, Integer notificationToneId, MediaQualitySetting mediaUploadQuality, Boolean isSpellCheckEnabled, Boolean isEnterToSendEnabled, Boolean isGroupMessageNotificationEnabled, Boolean isGroupReactionsNotificationEnabled, Boolean isStatusNotificationEnabled, Integer statusNotificationToneId, Boolean shouldPlaySoundForCallNotification) {
        this.startAtLogin = startAtLogin;
        this.minimizeToTray = minimizeToTray;
        this.language = language;
        this.replaceTextWithEmoji = replaceTextWithEmoji;
        this.bannerNotificationDisplayMode = bannerNotificationDisplayMode;
        this.unreadCounterBadgeDisplayMode = unreadCounterBadgeDisplayMode;
        this.isMessagesNotificationEnabled = isMessagesNotificationEnabled;
        this.isCallsNotificationEnabled = isCallsNotificationEnabled;
        this.isReactionsNotificationEnabled = isReactionsNotificationEnabled;
        this.isStatusReactionsNotificationEnabled = isStatusReactionsNotificationEnabled;
        this.isTextPreviewForNotificationEnabled = isTextPreviewForNotificationEnabled;
        this.defaultNotificationToneId = defaultNotificationToneId;
        this.groupDefaultNotificationToneId = groupDefaultNotificationToneId;
        this.appTheme = appTheme;
        this.wallpaperId = wallpaperId;
        this.isDoodleWallpaperEnabled = isDoodleWallpaperEnabled;
        this.fontSize = fontSize;
        this.isPhotosAutodownloadEnabled = isPhotosAutodownloadEnabled;
        this.isAudiosAutodownloadEnabled = isAudiosAutodownloadEnabled;
        this.isVideosAutodownloadEnabled = isVideosAutodownloadEnabled;
        this.isDocumentsAutodownloadEnabled = isDocumentsAutodownloadEnabled;
        this.disableLinkPreviews = disableLinkPreviews;
        this.notificationToneId = notificationToneId;
        this.mediaUploadQuality = mediaUploadQuality;
        this.isSpellCheckEnabled = isSpellCheckEnabled;
        this.isEnterToSendEnabled = isEnterToSendEnabled;
        this.isGroupMessageNotificationEnabled = isGroupMessageNotificationEnabled;
        this.isGroupReactionsNotificationEnabled = isGroupReactionsNotificationEnabled;
        this.isStatusNotificationEnabled = isStatusNotificationEnabled;
        this.statusNotificationToneId = statusNotificationToneId;
        this.shouldPlaySoundForCallNotification = shouldPlaySoundForCallNotification;
    }

    /**
     * Returns whether the desktop client launches at OS login.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean startAtLogin() {
        return startAtLogin != null && startAtLogin;
    }

    /**
     * Returns whether closing the window minimises to the system tray.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean minimizeToTray() {
        return minimizeToTray != null && minimizeToTray;
    }

    /**
     * Returns the desktop UI language override.
     *
     * @return an {@link Optional} containing the language tag, or empty if
     *         no override was set
     */
    public Optional<String> language() {
        return Optional.ofNullable(language);
    }

    /**
     * Returns whether typed emoticons are replaced with emoji.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean replaceTextWithEmoji() {
        return replaceTextWithEmoji != null && replaceTextWithEmoji;
    }

    /**
     * Returns when banner notifications are displayed.
     *
     * @return an {@link Optional} containing the configured
     *         {@link DisplayMode}, or empty if unset
     */
    public Optional<DisplayMode> bannerNotificationDisplayMode() {
        return Optional.ofNullable(bannerNotificationDisplayMode);
    }

    /**
     * Returns when the unread counter badge is displayed.
     *
     * @return an {@link Optional} containing the configured
     *         {@link DisplayMode}, or empty if unset
     */
    public Optional<DisplayMode> unreadCounterBadgeDisplayMode() {
        return Optional.ofNullable(unreadCounterBadgeDisplayMode);
    }

    /**
     * Returns whether message notifications are enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isMessagesNotificationEnabled() {
        return isMessagesNotificationEnabled != null && isMessagesNotificationEnabled;
    }

    /**
     * Returns whether call notifications are enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isCallsNotificationEnabled() {
        return isCallsNotificationEnabled != null && isCallsNotificationEnabled;
    }

    /**
     * Returns whether reaction notifications are enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isReactionsNotificationEnabled() {
        return isReactionsNotificationEnabled != null && isReactionsNotificationEnabled;
    }

    /**
     * Returns whether status reaction notifications are enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isStatusReactionsNotificationEnabled() {
        return isStatusReactionsNotificationEnabled != null && isStatusReactionsNotificationEnabled;
    }

    /**
     * Returns whether notifications include a text preview of the message.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isTextPreviewForNotificationEnabled() {
        return isTextPreviewForNotificationEnabled != null && isTextPreviewForNotificationEnabled;
    }

    /**
     * Returns the default notification tone id for one-to-one chats.
     *
     * @return an {@link OptionalInt} containing the tone id, or empty if unset
     */
    public OptionalInt defaultNotificationToneId() {
        return defaultNotificationToneId == null ? OptionalInt.empty() : OptionalInt.of(defaultNotificationToneId);
    }

    /**
     * Returns the default notification tone id for group chats.
     *
     * @return an {@link OptionalInt} containing the tone id, or empty if unset
     */
    public OptionalInt groupDefaultNotificationToneId() {
        return groupDefaultNotificationToneId == null ? OptionalInt.empty() : OptionalInt.of(groupDefaultNotificationToneId);
    }

    /**
     * Returns the selected application theme.
     *
     * @return an {@link Optional} containing the {@link AppTheme} constant,
     *         or empty if unset
     */
    public Optional<AppTheme> appTheme() {
        return Optional.ofNullable(appTheme);
    }

    /**
     * Returns the selected wallpaper identifier.
     *
     * @return an {@link OptionalInt} containing the wallpaper id, or empty if unset
     */
    public OptionalInt wallpaperId() {
        return wallpaperId == null ? OptionalInt.empty() : OptionalInt.of(wallpaperId);
    }

    /**
     * Returns whether the doodle wallpaper overlay is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isDoodleWallpaperEnabled() {
        return isDoodleWallpaperEnabled != null && isDoodleWallpaperEnabled;
    }

    /**
     * Returns the selected font size preset.
     *
     * @return an {@link OptionalInt} containing the font size preset, or
     *         empty if unset
     */
    public OptionalInt fontSize() {
        return fontSize == null ? OptionalInt.empty() : OptionalInt.of(fontSize);
    }

    /**
     * Returns whether photos are automatically downloaded.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isPhotosAutodownloadEnabled() {
        return isPhotosAutodownloadEnabled != null && isPhotosAutodownloadEnabled;
    }

    /**
     * Returns whether audio messages are automatically downloaded.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isAudiosAutodownloadEnabled() {
        return isAudiosAutodownloadEnabled != null && isAudiosAutodownloadEnabled;
    }

    /**
     * Returns whether videos are automatically downloaded.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isVideosAutodownloadEnabled() {
        return isVideosAutodownloadEnabled != null && isVideosAutodownloadEnabled;
    }

    /**
     * Returns whether documents are automatically downloaded.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isDocumentsAutodownloadEnabled() {
        return isDocumentsAutodownloadEnabled != null && isDocumentsAutodownloadEnabled;
    }

    /**
     * Returns whether link previews are suppressed.
     *
     * @return {@code true} if link previews are disabled, {@code false}
     *         otherwise or if unset
     */
    public boolean disableLinkPreviews() {
        return disableLinkPreviews != null && disableLinkPreviews;
    }

    /**
     * Returns the chat notification tone override identifier.
     *
     * @return an {@link OptionalInt} containing the tone id, or empty if unset
     */
    public OptionalInt notificationToneId() {
        return notificationToneId == null ? OptionalInt.empty() : OptionalInt.of(notificationToneId);
    }

    /**
     * Returns the media upload quality preset.
     *
     * @return an {@link Optional} containing the configured
     *         {@link MediaQualitySetting}, or empty if unset
     */
    public Optional<MediaQualitySetting> mediaUploadQuality() {
        return Optional.ofNullable(mediaUploadQuality);
    }

    /**
     * Returns whether composer spell check is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isSpellCheckEnabled() {
        return isSpellCheckEnabled != null && isSpellCheckEnabled;
    }

    /**
     * Returns whether Enter sends the message instead of inserting a
     * newline.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isEnterToSendEnabled() {
        return isEnterToSendEnabled != null && isEnterToSendEnabled;
    }

    /**
     * Returns whether group message notifications are enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isGroupMessageNotificationEnabled() {
        return isGroupMessageNotificationEnabled != null && isGroupMessageNotificationEnabled;
    }

    /**
     * Returns whether group reaction notifications are enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isGroupReactionsNotificationEnabled() {
        return isGroupReactionsNotificationEnabled != null && isGroupReactionsNotificationEnabled;
    }

    /**
     * Returns whether status update notifications are enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean isStatusNotificationEnabled() {
        return isStatusNotificationEnabled != null && isStatusNotificationEnabled;
    }

    /**
     * Returns the status notification tone identifier.
     *
     * @return an {@link OptionalInt} containing the tone id, or empty if unset
     */
    public OptionalInt statusNotificationToneId() {
        return statusNotificationToneId == null ? OptionalInt.empty() : OptionalInt.of(statusNotificationToneId);
    }

    /**
     * Returns whether call notifications play a ringtone.
     *
     * @return {@code true} if enabled, {@code false} otherwise or if unset
     */
    public boolean shouldPlaySoundForCallNotification() {
        return shouldPlaySoundForCallNotification != null && shouldPlaySoundForCallNotification;
    }

    /**
     * Updates the launch-at-login preference.
     *
     * @param startAtLogin the new value, or {@code null} to clear it
     */
    public void setStartAtLogin(Boolean startAtLogin) {
        this.startAtLogin = startAtLogin;
    }

    /**
     * Updates the minimise-to-tray preference.
     *
     * @param minimizeToTray the new value, or {@code null} to clear it
     */
    public void setMinimizeToTray(Boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
    }

    /**
     * Updates the desktop language override.
     *
     * @param language the new language tag, or {@code null} to clear it
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Updates the auto-emoji replacement preference.
     *
     * @param replaceTextWithEmoji the new value, or {@code null} to clear it
     */
    public void setReplaceTextWithEmoji(Boolean replaceTextWithEmoji) {
        this.replaceTextWithEmoji = replaceTextWithEmoji;
    }

    /**
     * Updates the banner notification display mode.
     *
     * @param bannerNotificationDisplayMode the new mode, or {@code null}
     *                                      to clear it
     */
    public void setBannerNotificationDisplayMode(DisplayMode bannerNotificationDisplayMode) {
        this.bannerNotificationDisplayMode = bannerNotificationDisplayMode;
    }

    /**
     * Updates the unread counter badge display mode.
     *
     * @param unreadCounterBadgeDisplayMode the new mode, or {@code null}
     *                                      to clear it
     */
    public void setUnreadCounterBadgeDisplayMode(DisplayMode unreadCounterBadgeDisplayMode) {
        this.unreadCounterBadgeDisplayMode = unreadCounterBadgeDisplayMode;
    }

    /**
     * Updates the messages-notification toggle.
     *
     * @param isMessagesNotificationEnabled the new value, or {@code null}
     *                                      to clear it
     */
    public void setMessagesNotificationEnabled(Boolean isMessagesNotificationEnabled) {
        this.isMessagesNotificationEnabled = isMessagesNotificationEnabled;
    }

    /**
     * Updates the calls-notification toggle.
     *
     * @param isCallsNotificationEnabled the new value, or {@code null} to
     *                                   clear it
     */
    public void setCallsNotificationEnabled(Boolean isCallsNotificationEnabled) {
        this.isCallsNotificationEnabled = isCallsNotificationEnabled;
    }

    /**
     * Updates the reactions-notification toggle.
     *
     * @param isReactionsNotificationEnabled the new value, or {@code null}
     *                                       to clear it
     */
    public void setReactionsNotificationEnabled(Boolean isReactionsNotificationEnabled) {
        this.isReactionsNotificationEnabled = isReactionsNotificationEnabled;
    }

    /**
     * Updates the status-reactions-notification toggle.
     *
     * @param isStatusReactionsNotificationEnabled the new value, or
     *                                             {@code null} to clear it
     */
    public void setStatusReactionsNotificationEnabled(Boolean isStatusReactionsNotificationEnabled) {
        this.isStatusReactionsNotificationEnabled = isStatusReactionsNotificationEnabled;
    }

    /**
     * Updates the notification text-preview toggle.
     *
     * @param isTextPreviewForNotificationEnabled the new value, or
     *                                            {@code null} to clear it
     */
    public void setTextPreviewForNotificationEnabled(Boolean isTextPreviewForNotificationEnabled) {
        this.isTextPreviewForNotificationEnabled = isTextPreviewForNotificationEnabled;
    }

    /**
     * Updates the default chat notification tone id.
     *
     * @param defaultNotificationToneId the new id, or {@code null} to clear it
     */
    public void setDefaultNotificationToneId(Integer defaultNotificationToneId) {
        this.defaultNotificationToneId = defaultNotificationToneId;
    }

    /**
     * Updates the default group notification tone id.
     *
     * @param groupDefaultNotificationToneId the new id, or {@code null} to
     *                                       clear it
     */
    public void setGroupDefaultNotificationToneId(Integer groupDefaultNotificationToneId) {
        this.groupDefaultNotificationToneId = groupDefaultNotificationToneId;
    }

    /**
     * Updates the selected application theme.
     *
     * @param appTheme the new {@link AppTheme} constant, or {@code null} to
     *                 clear it
     */
    public void setAppTheme(AppTheme appTheme) {
        this.appTheme = appTheme;
    }

    /**
     * Updates the selected chat wallpaper identifier.
     *
     * @param wallpaperId the new wallpaper id, or {@code null} to clear it
     */
    public void setWallpaperId(Integer wallpaperId) {
        this.wallpaperId = wallpaperId;
    }

    /**
     * Updates the doodle wallpaper overlay toggle.
     *
     * @param isDoodleWallpaperEnabled the new value, or {@code null} to
     *                                 clear it
     */
    public void setDoodleWallpaperEnabled(Boolean isDoodleWallpaperEnabled) {
        this.isDoodleWallpaperEnabled = isDoodleWallpaperEnabled;
    }

    /**
     * Updates the font size preset.
     *
     * @param fontSize the new preset, or {@code null} to clear it
     */
    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Updates the photos autodownload toggle.
     *
     * @param isPhotosAutodownloadEnabled the new value, or {@code null} to
     *                                    clear it
     */
    public void setPhotosAutodownloadEnabled(Boolean isPhotosAutodownloadEnabled) {
        this.isPhotosAutodownloadEnabled = isPhotosAutodownloadEnabled;
    }

    /**
     * Updates the audios autodownload toggle.
     *
     * @param isAudiosAutodownloadEnabled the new value, or {@code null} to
     *                                    clear it
     */
    public void setAudiosAutodownloadEnabled(Boolean isAudiosAutodownloadEnabled) {
        this.isAudiosAutodownloadEnabled = isAudiosAutodownloadEnabled;
    }

    /**
     * Updates the videos autodownload toggle.
     *
     * @param isVideosAutodownloadEnabled the new value, or {@code null} to
     *                                    clear it
     */
    public void setVideosAutodownloadEnabled(Boolean isVideosAutodownloadEnabled) {
        this.isVideosAutodownloadEnabled = isVideosAutodownloadEnabled;
    }

    /**
     * Updates the documents autodownload toggle.
     *
     * @param isDocumentsAutodownloadEnabled the new value, or {@code null}
     *                                       to clear it
     */
    public void setDocumentsAutodownloadEnabled(Boolean isDocumentsAutodownloadEnabled) {
        this.isDocumentsAutodownloadEnabled = isDocumentsAutodownloadEnabled;
    }

    /**
     * Updates the link-preview suppression toggle.
     *
     * @param disableLinkPreviews the new value, or {@code null} to clear it
     */
    public void setDisableLinkPreviews(Boolean disableLinkPreviews) {
        this.disableLinkPreviews = disableLinkPreviews;
    }

    /**
     * Updates the chat notification tone override identifier.
     *
     * @param notificationToneId the new tone id, or {@code null} to clear it
     */
    public void setNotificationToneId(Integer notificationToneId) {
        this.notificationToneId = notificationToneId;
    }

    /**
     * Updates the media upload quality preset.
     *
     * @param mediaUploadQuality the new preset, or {@code null} to clear it
     */
    public void setMediaUploadQuality(MediaQualitySetting mediaUploadQuality) {
        this.mediaUploadQuality = mediaUploadQuality;
    }

    /**
     * Updates the composer spell check toggle.
     *
     * @param isSpellCheckEnabled the new value, or {@code null} to clear it
     */
    public void setSpellCheckEnabled(Boolean isSpellCheckEnabled) {
        this.isSpellCheckEnabled = isSpellCheckEnabled;
    }

    /**
     * Updates the Enter-to-send toggle.
     *
     * @param isEnterToSendEnabled the new value, or {@code null} to clear it
     */
    public void setEnterToSendEnabled(Boolean isEnterToSendEnabled) {
        this.isEnterToSendEnabled = isEnterToSendEnabled;
    }

    /**
     * Updates the group messages notification toggle.
     *
     * @param isGroupMessageNotificationEnabled the new value, or
     *                                          {@code null} to clear it
     */
    public void setGroupMessageNotificationEnabled(Boolean isGroupMessageNotificationEnabled) {
        this.isGroupMessageNotificationEnabled = isGroupMessageNotificationEnabled;
    }

    /**
     * Updates the group reactions notification toggle.
     *
     * @param isGroupReactionsNotificationEnabled the new value, or
     *                                            {@code null} to clear it
     */
    public void setGroupReactionsNotificationEnabled(Boolean isGroupReactionsNotificationEnabled) {
        this.isGroupReactionsNotificationEnabled = isGroupReactionsNotificationEnabled;
    }

    /**
     * Updates the status notification toggle.
     *
     * @param isStatusNotificationEnabled the new value, or {@code null} to
     *                                    clear it
     */
    public void setStatusNotificationEnabled(Boolean isStatusNotificationEnabled) {
        this.isStatusNotificationEnabled = isStatusNotificationEnabled;
    }

    /**
     * Updates the status notification tone identifier.
     *
     * @param statusNotificationToneId the new tone id, or {@code null} to
     *                                 clear it
     */
    public void setStatusNotificationToneId(Integer statusNotificationToneId) {
        this.statusNotificationToneId = statusNotificationToneId;
    }

    /**
     * Updates the call-notification sound toggle.
     *
     * @param shouldPlaySoundForCallNotification the new value, or
     *                                           {@code null} to clear it
     */
    public void setShouldPlaySoundForCallNotification(Boolean shouldPlaySoundForCallNotification) {
        this.shouldPlaySoundForCallNotification = shouldPlaySoundForCallNotification;
    }

    /**
     * Enumeration of display modes for banner notifications and the
     * unread counter badge.
     */
    @ProtobufEnum(name = "SyncActionValue.SettingsSyncAction.DisplayMode")
    public static enum DisplayMode {
        /**
         * Unknown or uninitialised display mode.
         *
         * <p>Emitted by clients that did not populate the field and by
         * forward-compatibility paths; applications should treat this as
         * "fall back to the platform default".
         */
        DISPLAY_MODE_UNKNOWN(0),

        /**
         * The element is always displayed.
         */
        ALWAYS(1),

        /**
         * The element is never displayed.
         */
        NEVER(2),

        /**
         * The element is displayed only while the application window is
         * open and focused.
         */
        ONLY_WHEN_APP_IS_OPEN(3);

        /**
         * Constructs a display mode with the given protobuf index.
         *
         * @param index the wire-level protobuf enum index
         */
        DisplayMode(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire-level protobuf enum index for this mode.
         */
        final int index;

        /**
         * Returns the wire-level protobuf enum index for this mode.
         *
         * @return the protobuf index associated with this enum constant
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumeration of the quality presets that may be applied to uploaded
     * media.
     */
    @ProtobufEnum(name = "SyncActionValue.SettingsSyncAction.MediaQualitySetting")
    public static enum MediaQualitySetting {
        /**
         * Unknown or uninitialised quality preset.
         */
        MEDIA_QUALITY_UNKNOWN(0),

        /**
         * Standard quality with stronger compression and smaller file size.
         */
        STANDARD(1),

        /**
         * High-definition quality with minimal recompression.
         */
        HD(2);

        /**
         * Constructs a quality preset with the given protobuf index.
         *
         * @param index the wire-level protobuf enum index
         */
        MediaQualitySetting(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire-level protobuf enum index for this preset.
         */
        final int index;

        /**
         * Returns the wire-level protobuf enum index for this preset.
         *
         * @return the protobuf index associated with this enum constant
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumeration of the individual settings keys that may appear in the
     * sync index when a single field of this bundle is being mutated.
     *
     * <p>Each constant corresponds 1:1 to a field of the enclosing action.
     * Applications use these values in the index arguments of
     * {@link SettingsSyncActionArgs} to identify which setting a mutation
     * targets.
     */
    @ProtobufEnum(name = "SyncActionValue.SettingsSyncAction.SettingKey")
    public static enum SettingKey {
        /**
         * Unknown or uninitialised key.
         */
        SETTING_KEY_UNKNOWN(0),

        /**
         * Key for {@code startAtLogin}.
         */
        START_AT_LOGIN(1),

        /**
         * Key for {@code minimizeToTray}.
         */
        MINIMIZE_TO_TRAY(2),

        /**
         * Key for {@code language}.
         */
        LANGUAGE(3),

        /**
         * Key for {@code replaceTextWithEmoji}.
         */
        REPLACE_TEXT_WITH_EMOJI(4),

        /**
         * Key for {@code bannerNotificationDisplayMode}.
         */
        BANNER_NOTIFICATION_DISPLAY_MODE(5),

        /**
         * Key for {@code unreadCounterBadgeDisplayMode}.
         */
        UNREAD_COUNTER_BADGE_DISPLAY_MODE(6),

        /**
         * Key for {@code isMessagesNotificationEnabled}.
         */
        IS_MESSAGES_NOTIFICATION_ENABLED(7),

        /**
         * Key for {@code isCallsNotificationEnabled}.
         */
        IS_CALLS_NOTIFICATION_ENABLED(8),

        /**
         * Key for {@code isReactionsNotificationEnabled}.
         */
        IS_REACTIONS_NOTIFICATION_ENABLED(9),

        /**
         * Key for {@code isStatusReactionsNotificationEnabled}.
         */
        IS_STATUS_REACTIONS_NOTIFICATION_ENABLED(10),

        /**
         * Key for {@code isTextPreviewForNotificationEnabled}.
         */
        IS_TEXT_PREVIEW_FOR_NOTIFICATION_ENABLED(11),

        /**
         * Key for {@code defaultNotificationToneId}.
         */
        DEFAULT_NOTIFICATION_TONE_ID(12),

        /**
         * Key for {@code groupDefaultNotificationToneId}.
         */
        GROUP_DEFAULT_NOTIFICATION_TONE_ID(13),

        /**
         * Key for {@code appTheme}.
         */
        APP_THEME(14),

        /**
         * Key for {@code wallpaperId}.
         */
        WALLPAPER_ID(15),

        /**
         * Key for {@code isDoodleWallpaperEnabled}.
         */
        IS_DOODLE_WALLPAPER_ENABLED(16),

        /**
         * Key for {@code fontSize}.
         */
        FONT_SIZE(17),

        /**
         * Key for {@code isPhotosAutodownloadEnabled}.
         */
        IS_PHOTOS_AUTODOWNLOAD_ENABLED(18),

        /**
         * Key for {@code isAudiosAutodownloadEnabled}.
         */
        IS_AUDIOS_AUTODOWNLOAD_ENABLED(19),

        /**
         * Key for {@code isVideosAutodownloadEnabled}.
         */
        IS_VIDEOS_AUTODOWNLOAD_ENABLED(20),

        /**
         * Key for {@code isDocumentsAutodownloadEnabled}.
         */
        IS_DOCUMENTS_AUTODOWNLOAD_ENABLED(21),

        /**
         * Key for {@code disableLinkPreviews}.
         */
        DISABLE_LINK_PREVIEWS(22),

        /**
         * Key for {@code notificationToneId}.
         */
        NOTIFICATION_TONE_ID(23),

        /**
         * Key for {@code mediaUploadQuality}.
         */
        MEDIA_UPLOAD_QUALITY(24),

        /**
         * Key for {@code isSpellCheckEnabled}.
         */
        IS_SPELL_CHECK_ENABLED(25),

        /**
         * Key for {@code isEnterToSendEnabled}.
         */
        IS_ENTER_TO_SEND_ENABLED(26),

        /**
         * Key for {@code isGroupMessageNotificationEnabled}.
         */
        IS_GROUP_MESSAGE_NOTIFICATION_ENABLED(27),

        /**
         * Key for {@code isGroupReactionsNotificationEnabled}.
         */
        IS_GROUP_REACTIONS_NOTIFICATION_ENABLED(28),

        /**
         * Key for {@code isStatusNotificationEnabled}.
         */
        IS_STATUS_NOTIFICATION_ENABLED(29),

        /**
         * Key for {@code statusNotificationToneId}.
         */
        STATUS_NOTIFICATION_TONE_ID(30),

        /**
         * Key for {@code shouldPlaySoundForCallNotification}.
         */
        SHOULD_PLAY_SOUND_FOR_CALL_NOTIFICATION(31);

        /**
         * Constructs a setting key with the given protobuf index.
         *
         * @param index the wire-level protobuf enum index
         */
        SettingKey(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire-level protobuf enum index for this key.
         */
        final int index;

        /**
         * Returns the wire-level protobuf enum index for this key.
         *
         * @return the protobuf index associated with this enum constant
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumeration of the client platforms that may publish settings.
     *
     * <p>Each value appears as the first component of the sync index
     * arguments for {@link SettingsSyncAction}, identifying the device
     * type that produced the mutation so that platform-specific settings
     * can be filtered by peers.
     */
    @ProtobufEnum(name = "SyncActionValue.SettingsSyncAction.SettingPlatform")
    public static enum SettingPlatform {
        /**
         * Unknown or uninitialised platform.
         */
        PLATFORM_UNKNOWN(0),

        /**
         * WhatsApp Web running inside a browser.
         */
        WEB(1),

        /**
         * Hybrid client (the cross-platform Electron-like shell).
         */
        HYBRID(2),

        /**
         * Native WhatsApp Desktop on Windows.
         */
        WINDOWS(3),

        /**
         * Native WhatsApp Desktop on macOS.
         */
        MAC(4);

        /**
         * Constructs a platform with the given protobuf index.
         *
         * @param index the wire-level protobuf enum index
         */
        SettingPlatform(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire-level protobuf enum index for this platform.
         */
        final int index;

        /**
         * Returns the wire-level protobuf enum index for this platform.
         *
         * @return the protobuf index associated with this enum constant
         */
        public int index() {
            return this.index;
        }
    }


}
