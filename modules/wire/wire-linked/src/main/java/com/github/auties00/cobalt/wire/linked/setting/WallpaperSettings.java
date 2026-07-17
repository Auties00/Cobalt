package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Holds the user preferences that describe the background image shown behind
 * chat conversations.
 *
 * <p>WhatsApp stores two independent profiles of this settings object, one
 * for the light theme and one for the dark theme. Each profile references a
 * wallpaper file bundled with the client (or uploaded by the user) and an
 * opacity value that controls how visible the image is against the chat
 * contents.
 *
 * @see GlobalSettings
 */
@ProtobufMessage(name = "WallpaperSettings")
public final class WallpaperSettings {
    /**
     * Filename that identifies the wallpaper image inside the client catalog.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String filename;

    /**
     * Opacity of the wallpaper, expressed as a percentage between zero (fully
     * transparent) and one hundred (fully opaque).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer opacity;


    /**
     * Constructs a new wallpaper settings instance with the given values.
     *
     * @param filename the wallpaper filename, may be {@code null}
     * @param opacity  the wallpaper opacity percentage, may be {@code null}
     */
    WallpaperSettings(String filename, Integer opacity) {
        this.filename = filename;
        this.opacity = opacity;
    }

    /**
     * Returns the filename that identifies the wallpaper image.
     *
     * @return an {@link Optional} containing the filename, or empty if not set
     */
    public Optional<String> filename() {
        return Optional.ofNullable(filename);
    }

    /**
     * Returns the opacity percentage applied to the wallpaper.
     *
     * @return an {@link OptionalInt} containing the opacity percentage, or empty if not set
     */
    public OptionalInt opacity() {
        return opacity == null ? OptionalInt.empty() : OptionalInt.of(opacity);
    }

    /**
     * Updates the wallpaper filename.
     *
     * @param filename the new filename, or {@code null} to unset the field
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Updates the wallpaper opacity.
     *
     * @param opacity the new opacity percentage, or {@code null} to unset the field
     */
    public void setOpacity(Integer opacity) {
        this.opacity = opacity;
    }
}
