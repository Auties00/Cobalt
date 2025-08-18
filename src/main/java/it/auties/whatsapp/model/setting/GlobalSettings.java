package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.chat.ChatWallpaper;
import it.auties.whatsapp.model.media.MediaVisibility;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@ProtobufMessage(name = "GlobalSettings")
public final class GlobalSettings {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatWallpaper lightThemeWallpaper;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final MediaVisibility mediaVisibility;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final ChatWallpaper darkThemeWallpaper;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final AutoDownloadSettings autoDownloadWiFi;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final AutoDownloadSettings autoDownloadCellular;

    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final AutoDownloadSettings autoDownloadRoaming;

    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    final boolean showIndividualNotificationsPreview;

    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    final boolean showGroupNotificationsPreview;

    @ProtobufProperty(index = 9, type = ProtobufType.INT32)
    final int disappearingModeDuration;

    @ProtobufProperty(index = 10, type = ProtobufType.INT64)
    final long disappearingModeTimestampSeconds;

    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    final AvatarUserSettings avatarUserSettings;

    GlobalSettings(ChatWallpaper lightThemeWallpaper, MediaVisibility mediaVisibility, ChatWallpaper darkThemeWallpaper,
                   AutoDownloadSettings autoDownloadWiFi, AutoDownloadSettings autoDownloadCellular, AutoDownloadSettings autoDownloadRoaming,
                   boolean showIndividualNotificationsPreview, boolean showGroupNotificationsPreview,
                   int disappearingModeDuration, long disappearingModeTimestampSeconds, AvatarUserSettings avatarUserSettings) {
        this.lightThemeWallpaper = lightThemeWallpaper;
        this.mediaVisibility = mediaVisibility;
        this.darkThemeWallpaper = darkThemeWallpaper;
        this.autoDownloadWiFi = autoDownloadWiFi;
        this.autoDownloadCellular = autoDownloadCellular;
        this.autoDownloadRoaming = autoDownloadRoaming;
        this.showIndividualNotificationsPreview = showIndividualNotificationsPreview;
        this.showGroupNotificationsPreview = showGroupNotificationsPreview;
        this.disappearingModeDuration = disappearingModeDuration;
        this.disappearingModeTimestampSeconds = disappearingModeTimestampSeconds;
        this.avatarUserSettings = avatarUserSettings;
    }

    public Optional<ChatWallpaper> lightThemeWallpaper() {
        return Optional.ofNullable(lightThemeWallpaper);
    }

    public MediaVisibility mediaVisibility() {
        return mediaVisibility;
    }

    public Optional<ChatWallpaper> darkThemeWallpaper() {
        return Optional.ofNullable(darkThemeWallpaper);
    }

    public Optional<AutoDownloadSettings> autoDownloadWiFi() {
        return Optional.ofNullable(autoDownloadWiFi);
    }

    public Optional<AutoDownloadSettings> autoDownloadCellular() {
        return Optional.ofNullable(autoDownloadCellular);
    }

    public Optional<AutoDownloadSettings> autoDownloadRoaming() {
        return Optional.ofNullable(autoDownloadRoaming);
    }

    public boolean showIndividualNotificationsPreview() {
        return showIndividualNotificationsPreview;
    }

    public boolean showGroupNotificationsPreview() {
        return showGroupNotificationsPreview;
    }

    public int disappearingModeDuration() {
        return disappearingModeDuration;
    }

    public long disappearingModeTimestampSeconds() {
        return disappearingModeTimestampSeconds;
    }

    public AvatarUserSettings avatarUserSettings() {
        return avatarUserSettings;
    }

    /**
     * Returns when the disappearing mode was toggled
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> disappearingModeTimestamp() {
        return Clock.parseSeconds(disappearingModeTimestampSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GlobalSettings that
                && Objects.equals(lightThemeWallpaper, that.lightThemeWallpaper)
                && Objects.equals(mediaVisibility, that.mediaVisibility)
                && Objects.equals(darkThemeWallpaper, that.darkThemeWallpaper)
                && Objects.equals(autoDownloadWiFi, that.autoDownloadWiFi)
                && Objects.equals(autoDownloadCellular, that.autoDownloadCellular)
                && Objects.equals(autoDownloadRoaming, that.autoDownloadRoaming)
                && showIndividualNotificationsPreview == that.showIndividualNotificationsPreview
                && showGroupNotificationsPreview == that.showGroupNotificationsPreview
                && disappearingModeDuration == that.disappearingModeDuration
                && disappearingModeTimestampSeconds == that.disappearingModeTimestampSeconds
                && Objects.equals(avatarUserSettings, that.avatarUserSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lightThemeWallpaper, mediaVisibility, darkThemeWallpaper,
                autoDownloadWiFi, autoDownloadCellular, autoDownloadRoaming,
                showIndividualNotificationsPreview, showGroupNotificationsPreview,
                disappearingModeDuration, disappearingModeTimestampSeconds, avatarUserSettings);
    }

    @Override
    public String toString() {
        return "GlobalSettings[" +
                "lightThemeWallpaper=" + lightThemeWallpaper + ", " +
                "mediaVisibility=" + mediaVisibility + ", " +
                "darkThemeWallpaper=" + darkThemeWallpaper + ", " +
                "autoDownloadWiFi=" + autoDownloadWiFi + ", " +
                "autoDownloadCellular=" + autoDownloadCellular + ", " +
                "autoDownloadRoaming=" + autoDownloadRoaming + ", " +
                "showIndividualNotificationsPreview=" + showIndividualNotificationsPreview + ", " +
                "showGroupNotificationsPreview=" + showGroupNotificationsPreview + ", " +
                "disappearingModeDuration=" + disappearingModeDuration + ", " +
                "disappearingModeTimestampSeconds=" + disappearingModeTimestampSeconds + ", " +
                "avatarUserSettings=" + avatarUserSettings + ']';
    }
}