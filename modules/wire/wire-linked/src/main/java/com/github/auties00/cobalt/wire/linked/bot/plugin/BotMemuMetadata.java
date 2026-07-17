package com.github.auties00.cobalt.wire.linked.bot.plugin;

import com.github.auties00.cobalt.wire.linked.bot.rendering.BotMediaMetadata;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Carries the reference face images for the "Me, Myself, and AI" (MeMu)
 * personalized image-generation feature in WhatsApp.
 *
 * <p>MeMu allows the Meta AI bot to generate images featuring the user's
 * face. This metadata accompanies MeMu-related bot messages and contains
 * the user's face images that the AI model uses as reference input. Each
 * face image is represented as a {@link BotMediaMetadata} entry holding
 * the encrypted media reference (file hash, media key, direct path, etc.).
 *
 * @see BotImagineMetadata
 * @see BotMediaMetadata
 */
@ProtobufMessage(name = "BotMemuMetadata")
public final class BotMemuMetadata {
    /**
     * The user's face images that the AI model uses as reference input when
     * generating personalized MeMu images. Each entry contains encrypted media
     * references (file hash, media key, direct path, MIME type).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BotMediaMetadata> faceImages;

    /**
     * Constructs a new {@code BotMemuMetadata} with the specified reference
     * face images.
     *
     * @param faceImages the list of face image media metadata entries, or
     *                   {@code null} if no face images are provided
     */
    BotMemuMetadata(List<BotMediaMetadata> faceImages) {
        this.faceImages = faceImages;
    }

    /**
     * Returns the user's face images used as reference for personalized MeMu
     * image generation.
     *
     * @return an unmodifiable list of {@link BotMediaMetadata} entries, or an
     *         empty list if no face images are provided
     */
    public List<BotMediaMetadata> faceImages() {
        return faceImages == null ? List.of() : Collections.unmodifiableList(faceImages);
    }

    /**
     * Sets the user's face images used as reference for personalized MeMu
     * image generation.
     *
     * @param faceImages the new list of face image metadata entries, or
     *                   {@code null} to clear
     */
    public void setFaceImages(List<BotMediaMetadata> faceImages) {
        this.faceImages = faceImages;
    }
}
