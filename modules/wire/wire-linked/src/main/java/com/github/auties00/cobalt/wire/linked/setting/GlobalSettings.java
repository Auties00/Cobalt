package com.github.auties00.cobalt.wire.linked.setting;

import com.github.auties00.cobalt.wire.linked.media.MediaVisibility;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Aggregates the account-wide user preferences exposed by the WhatsApp client.
 *
 * <p>Every linked device shares the same global settings: they are persisted
 * on the primary device and propagated to companions through the app state
 * sync protocol. The object bundles together the appearance of the chat
 * window ({@link WallpaperSettings wallpapers}, {@link #fontSize() font
 * size}), the media download policy ({@link AutoDownloadSettings} per
 * network type), the notification preferences
 * ({@link NotificationSettings} for direct and group chats), the
 * disappearing-messages default, the security notification flag, the chat
 * lock configuration, and the linked Meta avatar credentials.
 *
 * @see WallpaperSettings
 * @see AutoDownloadSettings
 * @see NotificationSettings
 * @see ChatLockSettings
 * @see AvatarUserSettings
 */
@ProtobufMessage(name = "GlobalSettings")
public final class GlobalSettings {
    /**
     * Wallpaper used when the client runs in light theme.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    WallpaperSettings lightThemeWallpaper;

    /**
     * Policy applied to incoming media: either saved to the device gallery
     * or kept only inside WhatsApp's private storage.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    MediaVisibility mediaVisibility;

    /**
     * Wallpaper used when the client runs in dark theme.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    WallpaperSettings darkThemeWallpaper;

    /**
     * Auto-download policy applied when the device is connected to Wi-Fi.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    AutoDownloadSettings autoDownloadWiFi;

    /**
     * Auto-download policy applied when the device is on cellular data.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    AutoDownloadSettings autoDownloadCellular;

    /**
     * Auto-download policy applied when the device is roaming.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    AutoDownloadSettings autoDownloadRoaming;

    /**
     * Whether the content preview of direct-chat notifications is shown.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    Boolean showIndividualNotificationsPreview;

    /**
     * Whether the content preview of group-chat notifications is shown.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    Boolean showGroupNotificationsPreview;

    /**
     * Default duration, in seconds, applied to new chats when the user has
     * enabled the default disappearing-messages mode.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT32)
    Integer disappearingModeDuration;

    /**
     * Instant at which the default disappearing-messages mode was last changed.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant disappearingModeTimestamp;

    /**
     * Credentials that link the WhatsApp account to its Meta avatar profile.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    AvatarUserSettings avatarUserSettings;

    /**
     * Chat font size preference expressed as a client-defined integer scale.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.INT32)
    Integer fontSize;

    /**
     * Whether the client surfaces security notifications (for example when
     * the safety number of a contact changes).
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    Boolean securityNotifications;

    /**
     * Whether archived chats that receive a new message are moved back to
     * the main chat list automatically.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
    Boolean autoUnarchiveChats;

    /**
     * Preferred upload quality for video attachments, encoded as a
     * client-defined integer.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.INT32)
    Integer videoQualityMode;

    /**
     * Preferred upload quality for photo attachments, encoded as a
     * client-defined integer.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.INT32)
    Integer photoQualityMode;

    /**
     * Notification preferences applied to direct (one-to-one) chats.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    NotificationSettings individualNotificationSettings;

    /**
     * Notification preferences applied to group chats.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    NotificationSettings groupNotificationSettings;

    /**
     * Preferences for the chat lock feature that protects selected chats
     * behind a secret code.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.MESSAGE)
    ChatLockSettings chatLockSettings;

    /**
     * Instant at which the local chat database was last migrated to use LID
     * (linked identity) addressing.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant chatDbLidMigrationTimestamp;


    /**
     * Constructs a new global settings snapshot with the given values.
     *
     * @param lightThemeWallpaper               the light-theme wallpaper, may be {@code null}
     * @param mediaVisibility                   the gallery visibility policy, may be {@code null}
     * @param darkThemeWallpaper                the dark-theme wallpaper, may be {@code null}
     * @param autoDownloadWiFi                  the Wi-Fi auto-download policy, may be {@code null}
     * @param autoDownloadCellular              the cellular auto-download policy, may be {@code null}
     * @param autoDownloadRoaming               the roaming auto-download policy, may be {@code null}
     * @param showIndividualNotificationsPreview whether direct-chat notification previews are shown, may be {@code null}
     * @param showGroupNotificationsPreview     whether group-chat notification previews are shown, may be {@code null}
     * @param disappearingModeDuration          the default disappearing-messages duration in seconds, may be {@code null}
     * @param disappearingModeTimestamp         the instant the default disappearing-messages mode was last changed, may be {@code null}
     * @param avatarUserSettings                the Meta avatar credentials, may be {@code null}
     * @param fontSize                          the preferred chat font size, may be {@code null}
     * @param securityNotifications             whether security notifications are shown, may be {@code null}
     * @param autoUnarchiveChats                whether archived chats are auto-unarchived on new messages, may be {@code null}
     * @param videoQualityMode                  the preferred video upload quality, may be {@code null}
     * @param photoQualityMode                  the preferred photo upload quality, may be {@code null}
     * @param individualNotificationSettings    the direct-chat notification preferences, may be {@code null}
     * @param groupNotificationSettings         the group-chat notification preferences, may be {@code null}
     * @param chatLockSettings                  the chat lock preferences, may be {@code null}
     * @param chatDbLidMigrationTimestamp       the instant of the last LID chat database migration, may be {@code null}
     */
    GlobalSettings(WallpaperSettings lightThemeWallpaper, MediaVisibility mediaVisibility, WallpaperSettings darkThemeWallpaper, AutoDownloadSettings autoDownloadWiFi, AutoDownloadSettings autoDownloadCellular, AutoDownloadSettings autoDownloadRoaming, Boolean showIndividualNotificationsPreview, Boolean showGroupNotificationsPreview, Integer disappearingModeDuration, Instant disappearingModeTimestamp, AvatarUserSettings avatarUserSettings, Integer fontSize, Boolean securityNotifications, Boolean autoUnarchiveChats, Integer videoQualityMode, Integer photoQualityMode, NotificationSettings individualNotificationSettings, NotificationSettings groupNotificationSettings, ChatLockSettings chatLockSettings, Instant chatDbLidMigrationTimestamp) {
        this.lightThemeWallpaper = lightThemeWallpaper;
        this.mediaVisibility = mediaVisibility;
        this.darkThemeWallpaper = darkThemeWallpaper;
        this.autoDownloadWiFi = autoDownloadWiFi;
        this.autoDownloadCellular = autoDownloadCellular;
        this.autoDownloadRoaming = autoDownloadRoaming;
        this.showIndividualNotificationsPreview = showIndividualNotificationsPreview;
        this.showGroupNotificationsPreview = showGroupNotificationsPreview;
        this.disappearingModeDuration = disappearingModeDuration;
        this.disappearingModeTimestamp = disappearingModeTimestamp;
        this.avatarUserSettings = avatarUserSettings;
        this.fontSize = fontSize;
        this.securityNotifications = securityNotifications;
        this.autoUnarchiveChats = autoUnarchiveChats;
        this.videoQualityMode = videoQualityMode;
        this.photoQualityMode = photoQualityMode;
        this.individualNotificationSettings = individualNotificationSettings;
        this.groupNotificationSettings = groupNotificationSettings;
        this.chatLockSettings = chatLockSettings;
        this.chatDbLidMigrationTimestamp = chatDbLidMigrationTimestamp;
    }

    /**
     * Returns the wallpaper used when the client runs in light theme.
     *
     * @return an {@link Optional} containing the wallpaper, or empty if not set
     */
    public Optional<WallpaperSettings> lightThemeWallpaper() {
        return Optional.ofNullable(lightThemeWallpaper);
    }

    /**
     * Returns the policy that decides whether incoming media are exposed to
     * the device gallery.
     *
     * @return an {@link Optional} containing the policy, or empty if not set
     */
    public Optional<MediaVisibility> mediaVisibility() {
        return Optional.ofNullable(mediaVisibility);
    }

    /**
     * Returns the wallpaper used when the client runs in dark theme.
     *
     * @return an {@link Optional} containing the wallpaper, or empty if not set
     */
    public Optional<WallpaperSettings> darkThemeWallpaper() {
        return Optional.ofNullable(darkThemeWallpaper);
    }

    /**
     * Returns the auto-download policy applied on Wi-Fi.
     *
     * @return an {@link Optional} containing the policy, or empty if not set
     */
    public Optional<AutoDownloadSettings> autoDownloadWiFi() {
        return Optional.ofNullable(autoDownloadWiFi);
    }

    /**
     * Returns the auto-download policy applied on cellular data.
     *
     * @return an {@link Optional} containing the policy, or empty if not set
     */
    public Optional<AutoDownloadSettings> autoDownloadCellular() {
        return Optional.ofNullable(autoDownloadCellular);
    }

    /**
     * Returns the auto-download policy applied when roaming.
     *
     * @return an {@link Optional} containing the policy, or empty if not set
     */
    public Optional<AutoDownloadSettings> autoDownloadRoaming() {
        return Optional.ofNullable(autoDownloadRoaming);
    }

    /**
     * Returns whether the content preview of direct-chat notifications is shown.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if previews are shown, {@code false} otherwise
     */
    public boolean showIndividualNotificationsPreview() {
        return showIndividualNotificationsPreview != null && showIndividualNotificationsPreview;
    }

    /**
     * Returns whether the content preview of group-chat notifications is shown.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if previews are shown, {@code false} otherwise
     */
    public boolean showGroupNotificationsPreview() {
        return showGroupNotificationsPreview != null && showGroupNotificationsPreview;
    }

    /**
     * Returns the default duration applied to new disappearing-messages chats.
     *
     * @return an {@link OptionalInt} containing the duration in seconds, or empty if not set
     */
    public OptionalInt disappearingModeDuration() {
        return disappearingModeDuration == null ? OptionalInt.empty() : OptionalInt.of(disappearingModeDuration);
    }

    /**
     * Returns the instant at which the default disappearing-messages mode
     * was last changed.
     *
     * @return an {@link Optional} containing the instant, or empty if not set
     */
    public Optional<Instant> disappearingModeTimestamp() {
        return Optional.ofNullable(disappearingModeTimestamp);
    }

    /**
     * Returns the credentials that link the WhatsApp account to its Meta
     * avatar profile.
     *
     * @return an {@link Optional} containing the credentials, or empty if not set
     */
    public Optional<AvatarUserSettings> avatarUserSettings() {
        return Optional.ofNullable(avatarUserSettings);
    }

    /**
     * Returns the preferred chat font size.
     *
     * @return an {@link OptionalInt} containing the font size, or empty if not set
     */
    public OptionalInt fontSize() {
        return fontSize == null ? OptionalInt.empty() : OptionalInt.of(fontSize);
    }

    /**
     * Returns whether security notifications are surfaced to the user.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if security notifications are shown, {@code false} otherwise
     */
    public boolean securityNotifications() {
        return securityNotifications != null && securityNotifications;
    }

    /**
     * Returns whether archived chats are automatically moved back to the main
     * list when they receive a new message.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if auto-unarchive is enabled, {@code false} otherwise
     */
    public boolean autoUnarchiveChats() {
        return autoUnarchiveChats != null && autoUnarchiveChats;
    }

    /**
     * Returns the preferred upload quality for video attachments.
     *
     * @return an {@link OptionalInt} containing the quality value, or empty if not set
     */
    public OptionalInt videoQualityMode() {
        return videoQualityMode == null ? OptionalInt.empty() : OptionalInt.of(videoQualityMode);
    }

    /**
     * Returns the preferred upload quality for photo attachments.
     *
     * @return an {@link OptionalInt} containing the quality value, or empty if not set
     */
    public OptionalInt photoQualityMode() {
        return photoQualityMode == null ? OptionalInt.empty() : OptionalInt.of(photoQualityMode);
    }

    /**
     * Returns the notification preferences applied to direct chats.
     *
     * @return an {@link Optional} containing the preferences, or empty if not set
     */
    public Optional<NotificationSettings> individualNotificationSettings() {
        return Optional.ofNullable(individualNotificationSettings);
    }

    /**
     * Returns the notification preferences applied to group chats.
     *
     * @return an {@link Optional} containing the preferences, or empty if not set
     */
    public Optional<NotificationSettings> groupNotificationSettings() {
        return Optional.ofNullable(groupNotificationSettings);
    }

    /**
     * Returns the preferences for the chat lock feature.
     *
     * @return an {@link Optional} containing the preferences, or empty if not set
     */
    public Optional<ChatLockSettings> chatLockSettings() {
        return Optional.ofNullable(chatLockSettings);
    }

    /**
     * Returns the instant at which the local chat database was last migrated
     * to LID addressing.
     *
     * @return an {@link Optional} containing the instant, or empty if not set
     */
    public Optional<Instant> chatDbLidMigrationTimestamp() {
        return Optional.ofNullable(chatDbLidMigrationTimestamp);
    }

    /**
     * Updates the wallpaper used when the client runs in light theme.
     *
     * @param lightThemeWallpaper the new wallpaper, or {@code null} to unset the field
     */
    public void setLightThemeWallpaper(WallpaperSettings lightThemeWallpaper) {
        this.lightThemeWallpaper = lightThemeWallpaper;
    }

    /**
     * Updates the policy that decides whether incoming media are exposed to
     * the device gallery.
     *
     * @param mediaVisibility the new policy, or {@code null} to unset the field
     */
    public void setMediaVisibility(MediaVisibility mediaVisibility) {
        this.mediaVisibility = mediaVisibility;
    }

    /**
     * Updates the wallpaper used when the client runs in dark theme.
     *
     * @param darkThemeWallpaper the new wallpaper, or {@code null} to unset the field
     */
    public void setDarkThemeWallpaper(WallpaperSettings darkThemeWallpaper) {
        this.darkThemeWallpaper = darkThemeWallpaper;
    }

    /**
     * Updates the auto-download policy applied on Wi-Fi.
     *
     * @param autoDownloadWiFi the new policy, or {@code null} to unset the field
     */
    public void setAutoDownloadWiFi(AutoDownloadSettings autoDownloadWiFi) {
        this.autoDownloadWiFi = autoDownloadWiFi;
    }

    /**
     * Updates the auto-download policy applied on cellular data.
     *
     * @param autoDownloadCellular the new policy, or {@code null} to unset the field
     */
    public void setAutoDownloadCellular(AutoDownloadSettings autoDownloadCellular) {
        this.autoDownloadCellular = autoDownloadCellular;
    }

    /**
     * Updates the auto-download policy applied when roaming.
     *
     * @param autoDownloadRoaming the new policy, or {@code null} to unset the field
     */
    public void setAutoDownloadRoaming(AutoDownloadSettings autoDownloadRoaming) {
        this.autoDownloadRoaming = autoDownloadRoaming;
    }

    /**
     * Updates the flag that controls direct-chat notification previews.
     *
     * @param showIndividualNotificationsPreview the new value, or {@code null} to unset the field
     */
    public void setShowIndividualNotificationsPreview(Boolean showIndividualNotificationsPreview) {
        this.showIndividualNotificationsPreview = showIndividualNotificationsPreview;
    }

    /**
     * Updates the flag that controls group-chat notification previews.
     *
     * @param showGroupNotificationsPreview the new value, or {@code null} to unset the field
     */
    public void setShowGroupNotificationsPreview(Boolean showGroupNotificationsPreview) {
        this.showGroupNotificationsPreview = showGroupNotificationsPreview;
    }

    /**
     * Updates the default duration applied to new disappearing-messages chats.
     *
     * @param disappearingModeDuration the new duration in seconds, or {@code null} to unset the field
     */
    public void setDisappearingModeDuration(Integer disappearingModeDuration) {
        this.disappearingModeDuration = disappearingModeDuration;
    }

    /**
     * Updates the instant at which the default disappearing-messages mode
     * was last changed.
     *
     * @param disappearingModeTimestamp the new instant, or {@code null} to unset the field
     */
    public void setDisappearingModeTimestamp(Instant disappearingModeTimestamp) {
        this.disappearingModeTimestamp = disappearingModeTimestamp;
    }

    /**
     * Updates the Meta avatar credentials.
     *
     * @param avatarUserSettings the new credentials, or {@code null} to unset the field
     */
    public void setAvatarUserSettings(AvatarUserSettings avatarUserSettings) {
        this.avatarUserSettings = avatarUserSettings;
    }

    /**
     * Updates the preferred chat font size.
     *
     * @param fontSize the new font size, or {@code null} to unset the field
     */
    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Updates the flag that controls security notifications.
     *
     * @param securityNotifications the new value, or {@code null} to unset the field
     */
    public void setSecurityNotifications(Boolean securityNotifications) {
        this.securityNotifications = securityNotifications;
    }

    /**
     * Updates the flag that auto-unarchives chats when they receive a new message.
     *
     * @param autoUnarchiveChats the new value, or {@code null} to unset the field
     */
    public void setAutoUnarchiveChats(Boolean autoUnarchiveChats) {
        this.autoUnarchiveChats = autoUnarchiveChats;
    }

    /**
     * Updates the preferred upload quality for video attachments.
     *
     * @param videoQualityMode the new quality value, or {@code null} to unset the field
     */
    public void setVideoQualityMode(Integer videoQualityMode) {
        this.videoQualityMode = videoQualityMode;
    }

    /**
     * Updates the preferred upload quality for photo attachments.
     *
     * @param photoQualityMode the new quality value, or {@code null} to unset the field
     */
    public void setPhotoQualityMode(Integer photoQualityMode) {
        this.photoQualityMode = photoQualityMode;
    }

    /**
     * Updates the notification preferences applied to direct chats.
     *
     * @param individualNotificationSettings the new preferences, or {@code null} to unset the field
     */
    public void setIndividualNotificationSettings(NotificationSettings individualNotificationSettings) {
        this.individualNotificationSettings = individualNotificationSettings;
    }

    /**
     * Updates the notification preferences applied to group chats.
     *
     * @param groupNotificationSettings the new preferences, or {@code null} to unset the field
     */
    public void setGroupNotificationSettings(NotificationSettings groupNotificationSettings) {
        this.groupNotificationSettings = groupNotificationSettings;
    }

    /**
     * Updates the preferences for the chat lock feature.
     *
     * @param chatLockSettings the new preferences, or {@code null} to unset the field
     */
    public void setChatLockSettings(ChatLockSettings chatLockSettings) {
        this.chatLockSettings = chatLockSettings;
    }

    /**
     * Updates the instant at which the local chat database was last migrated
     * to LID addressing.
     *
     * @param chatDbLidMigrationTimestamp the new instant, or {@code null} to unset the field
     */
    public void setChatDbLidMigrationTimestamp(Instant chatDbLidMigrationTimestamp) {
        this.chatDbLidMigrationTimestamp = chatDbLidMigrationTimestamp;
    }
}
