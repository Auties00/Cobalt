package com.github.auties00.cobalt.wire.linked.sync.action.media;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A weighted emoji entry used to rank the emojis most frequently used by the
 * user.
 *
 * <p>Each entry pairs an emoji glyph with a numeric weight that reflects how
 * often the emoji has been picked relative to other emojis. Higher weights
 * indicate emojis that should be surfaced first in recent-emoji pickers across
 * linked devices.
 */
@ProtobufMessage(name = "RecentEmojiWeight")
public final class RecentEmojiWeight {
    /**
     * The emoji glyph whose usage weight is being recorded.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String emoji;

    /**
     * The usage weight assigned to {@link #emoji}. Higher values indicate a
     * more frequently picked emoji.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.FLOAT)
    Float weight;


    /**
     * Constructs a new recent-emoji weight entry.
     *
     * @param emoji  the emoji glyph, or {@code null} if unset
     * @param weight the usage weight, or {@code null} if unset
     */
    RecentEmojiWeight(String emoji, Float weight) {
        this.emoji = emoji;
        this.weight = weight;
    }

    /**
     * Returns the emoji glyph for this entry.
     *
     * @return the emoji glyph, or {@link Optional#empty()} if unset
     */
    public Optional<String> emoji() {
        return Optional.ofNullable(emoji);
    }

    /**
     * Returns the usage weight for this entry.
     *
     * @return the usage weight, or {@link OptionalDouble#empty()} if unset
     */
    public OptionalDouble weight() {
        return weight == null ? OptionalDouble.empty() : OptionalDouble.of(weight);
    }

    /**
     * Sets the emoji glyph for this entry.
     *
     * @param emoji the new emoji glyph, or {@code null} to clear it
     */
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    /**
     * Sets the usage weight for this entry.
     *
     * @param weight the new usage weight, or {@code null} to clear it
     */
    public void setWeight(Float weight) {
        this.weight = weight;
    }
}
