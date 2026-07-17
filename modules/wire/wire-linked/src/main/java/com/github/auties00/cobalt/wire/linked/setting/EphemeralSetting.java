package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Represents the disappearing messages policy for a single conversation.
 *
 * <p>When an ephemeral setting is associated with a chat, every message sent
 * in that chat is marked to expire after the configured duration counting
 * from the moment the recipient reads it. Setting the duration to zero
 * disables disappearing messages. The timestamp carries the instant (in
 * seconds since the Unix epoch) at which the current policy was last changed,
 * which is used by the protocol to resolve conflicts between devices.
 */
@ProtobufMessage(name = "EphemeralSetting")
public final class EphemeralSetting {
    /**
     * Expiration delay in seconds applied to new messages in the chat, or
     * zero if disappearing messages are disabled.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.SFIXED32)
    Integer duration;

    /**
     * Instant, in seconds since the Unix epoch, at which the current policy
     * was last updated.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.SFIXED64)
    Long timestamp;


    /**
     * Constructs a new ephemeral setting with the given duration and timestamp.
     *
     * @param duration  expiration delay in seconds, may be {@code null}
     * @param timestamp instant of the last policy change, in seconds since epoch, may be {@code null}
     */
    EphemeralSetting(Integer duration, Long timestamp) {
        this.duration = duration;
        this.timestamp = timestamp;
    }

    /**
     * Returns the expiration delay applied to new messages in the chat.
     *
     * @return an {@link OptionalInt} containing the delay in seconds, or empty if not set
     */
    public OptionalInt duration() {
        return duration == null ? OptionalInt.empty() : OptionalInt.of(duration);
    }

    /**
     * Returns the instant at which the current policy was last updated.
     *
     * @return an {@link OptionalLong} containing the seconds since epoch, or empty if not set
     */
    public OptionalLong timestamp() {
        return timestamp == null ? OptionalLong.empty() : OptionalLong.of(timestamp);
    }

    /**
     * Updates the expiration delay applied to new messages in the chat.
     *
     * @param duration the new delay in seconds, or {@code null} to unset the field
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     * Updates the timestamp of the current policy change.
     *
     * @param timestamp the new timestamp in seconds since epoch, or {@code null} to unset the field
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
