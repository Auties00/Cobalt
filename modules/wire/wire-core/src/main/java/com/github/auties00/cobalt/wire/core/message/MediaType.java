package com.github.auties00.cobalt.wire.core.message;

/**
 * The kind of media carried by a {@link MediaContent} body.
 *
 * <p>This discriminator lets a single media content contract stand in for every media message type
 * both transports carry, so callers and encoders can branch on the kind without a distinct interface
 * per media type.
 */
public enum MediaType {
    /**
     * A still image.
     */
    IMAGE,
    /**
     * A video clip.
     */
    VIDEO,
    /**
     * An audio clip or voice note.
     */
    AUDIO,
    /**
     * A document or file attachment.
     */
    DOCUMENT,
    /**
     * A sticker.
     */
    STICKER
}
