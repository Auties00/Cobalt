package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents the wallpaper of a chat.
 */
@ProtobufMessage(name = "WallpaperSettings")
public final class ChatWallpaper {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String filename;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final int opacity;

    ChatWallpaper(String filename, int opacity) {
        this.filename = Objects.requireNonNull(filename, "filename cannot be null");
        this.opacity = opacity;
    }

    public String filename() {
        return filename;
    }

    public int opacity() {
        return opacity;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChatWallpaper that
                && Objects.equals(filename, that.filename)
                && opacity == that.opacity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, opacity);
    }

    @Override
    public String toString() {
        return "ChatWallpaper[" +
                "filename=" + filename +
                ", opacity=" + opacity +
                ']';
    }
}