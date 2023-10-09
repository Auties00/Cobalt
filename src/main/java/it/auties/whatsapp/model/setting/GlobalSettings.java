package it.auties.whatsapp.model.setting;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.chat.ChatWallpaper;
import it.auties.whatsapp.model.media.MediaVisibility;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


@ProtobufMessageName("GlobalSettings")
public record GlobalSettings(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Optional<ChatWallpaper> lightThemeWallpaper,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        MediaVisibility mediaVisibility,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<ChatWallpaper> darkThemeWallpaper,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<AutoDownloadSettings> autoDownloadWiFi,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        Optional<AutoDownloadSettings> autoDownloadCellular,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        Optional<AutoDownloadSettings> autoDownloadRoaming,
        @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
        boolean showIndividualNotificationsPreview,
        @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
        boolean showGroupNotificationsPreview,
        @ProtobufProperty(index = 9, type = ProtobufType.INT32)
        int disappearingModeDuration,
        @ProtobufProperty(index = 10, type = ProtobufType.INT64)
        long disappearingModeTimestampSeconds,
        @ProtobufProperty(index = 11, type = ProtobufType.OBJECT)
        AvatarUserSettings avatarUserSettings
) implements ProtobufMessage {
    /**
     * Returns when the disappearing mode was toggled
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> disappearingModeTimestamp() {
        return Clock.parseSeconds(disappearingModeTimestampSeconds);
    }
}