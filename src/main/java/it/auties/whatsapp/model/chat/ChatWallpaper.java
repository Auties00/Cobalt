package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents the wallpaper of a chat.
 */
@ProtobufMessageName("WallpaperSettings")
public record ChatWallpaper(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String filename,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        int opacity
) implements ProtobufMessage {
}
