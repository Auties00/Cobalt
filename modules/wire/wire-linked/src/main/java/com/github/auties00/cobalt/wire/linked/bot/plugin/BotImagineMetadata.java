package com.github.auties00.cobalt.wire.linked.bot.plugin;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Describes an AI image-generation ("Imagine") response produced by the
 * Meta AI bot in WhatsApp.
 *
 * <p>When the Meta AI bot generates an image in response to a user prompt,
 * this metadata accompanies the reply to indicate which type of image
 * generation was performed (standard text-to-image, personalized MeMu,
 * flash generation, or image editing) and, optionally, a condensed version
 * of the prompt that was used. Clients can use the {@link #shortPrompt()}
 * to display a summary of the original request alongside the generated image.
 *
 * @see BotMemuMetadata
 */
@ProtobufMessage(name = "BotImagineMetadata")
public final class BotImagineMetadata {
    /**
     * The type of image-generation operation that produced this result, such
     * as standard text-to-image, personalized MeMu, or image editing.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    ImagineType imagineType;

    /**
     * A condensed version of the user's prompt used for image generation,
     * suitable for display alongside the generated image. For example,
     * {@code "sunset over mountains"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String shortPrompt;

    /**
     * Constructs a new {@code BotImagineMetadata} with the specified values.
     *
     * @param imagineType the type of image generation performed, or
     *                    {@code null} if unknown
     * @param shortPrompt a condensed version of the original prompt, or
     *                    {@code null} if not available
     */
    BotImagineMetadata(ImagineType imagineType, String shortPrompt) {
        this.imagineType = imagineType;
        this.shortPrompt = shortPrompt;
    }

    /**
     * Returns the type of image-generation operation that produced this result.
     *
     * @return an {@link Optional} describing the imagine type, or an empty
     *         {@code Optional} if not set
     */
    public Optional<ImagineType> imagineType() {
        return Optional.ofNullable(imagineType);
    }

    /**
     * Returns the condensed version of the user's prompt used for image
     * generation.
     *
     * @return an {@link Optional} describing the short prompt string, or an
     *         empty {@code Optional} if not available
     */
    public Optional<String> shortPrompt() {
        return Optional.ofNullable(shortPrompt);
    }

    /**
     * Sets the type of image-generation operation that produced this result.
     *
     * @param imagineType the new imagine type, or {@code null} to clear
     */
    public void setImagineType(ImagineType imagineType) {
        this.imagineType = imagineType;
    }

    /**
     * Sets the condensed version of the user's prompt.
     *
     * @param shortPrompt the new short prompt string, or {@code null} to clear
     */
    public void setShortPrompt(String shortPrompt) {
        this.shortPrompt = shortPrompt;
    }

    /**
     * Enumerates the types of AI image-generation operations that the Meta AI
     * bot can perform, ranging from standard text-to-image generation to
     * personalized face-based generation and image editing.
     */
    @ProtobufEnum(name = "BotImagineMetadata.ImagineType")
    public static enum ImagineType {
        /**
         * An unknown or unrecognized image-generation type.
         */
        UNKNOWN(0),

        /**
         * A standard text-to-image generation from a prompt.
         */
        IMAGINE(1),

        /**
         * A "Me, Myself, and AI" (MeMu) personalized image generation using
         * the user's face.
         */
        MEMU(2),

        /**
         * A quick "flash" image generation with faster, lower-quality results.
         */
        FLASH(3),

        /**
         * An image editing operation on an existing image.
         */
        EDIT(4);

        /**
         * Constructs a new imagine type constant.
         *
         * @param index the protobuf-assigned numeric index for this constant
         */
        ImagineType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned numeric index for this constant.
         */
        final int index;

        /**
         * Returns the protobuf-assigned numeric index for this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
