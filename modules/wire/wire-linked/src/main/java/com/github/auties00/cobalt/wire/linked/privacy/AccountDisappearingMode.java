package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Account-wide default disappearing-message timer.
 *
 * <p>This is the global ephemeral-message default that WhatsApp applies to
 * every newly-created chat for the local user. When set to a non-zero
 * duration (typically 24 hours, 7 days or 90 days), every new conversation
 * the user starts has its disappearing-message timer pre-seeded to that
 * value; existing chats keep whatever per-chat timer they already had.
 *
 * <p>This setting is distinct from the per-chat ephemeral timer: it only
 * controls the default applied at chat-creation time. The pair is exposed
 * by the WhatsApp server through the {@code <iq xmlns="disappearing_mode">}
 * IQ and carries both the duration itself and the wall-clock instant the
 * user last changed it (used by clients to break ties when the same account
 * is updated from multiple devices).
 */
@ProtobufMessage
public final class AccountDisappearingMode {
    /**
     * The global default timer expressed in seconds. A value of zero means
     * "disappearing messages are off by default for new chats". Stored as
     * raw seconds so the on-the-wire encoding matches verbatim; the
     * {@link #duration()} accessor wraps it as a {@link Duration}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    long durationSeconds;

    /**
     * The wall-clock instant the user last changed the default at, encoded
     * on the wire as Unix seconds. Used by clients to resolve conflicts
     * when the same account updates the setting from multiple devices.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * Constructs a new {@code AccountDisappearingMode} with the supplied
     * duration (in seconds) and last-changed timestamp.
     *
     * @param durationSeconds the global default timer in seconds, or zero
     *                        when disappearing messages are off by default
     * @param timestamp       the wall-clock instant the default was last
     *                        changed at, or {@code null} if not set
     */
    AccountDisappearingMode(long durationSeconds, Instant timestamp) {
        this.durationSeconds = durationSeconds;
        this.timestamp = timestamp;
    }

    /**
     * Returns the global default disappearing-message timer.
     *
     * <p>The underlying field stores the timer as raw seconds matching the
     * wire encoding; this accessor wraps the value into a {@link Duration}
     * for caller convenience. {@link Duration#ZERO} means disappearing
     * messages are not enabled by default.
     *
     * @return the timer; never {@code null}
     */
    public Duration duration() {
        return Duration.ofSeconds(durationSeconds);
    }

    /**
     * Returns the wall-clock instant the default was last changed at.
     *
     * @return an {@code Optional} containing the timestamp, or empty if not
     *         set
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Sets the global default disappearing-message timer in seconds.
     *
     * @param durationSeconds the timer in seconds; zero clears the default
     */
    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    /**
     * Sets the global default disappearing-message timer.
     *
     * @param duration the timer; {@code null} is treated as
     *                 {@link Duration#ZERO}
     */
    public void setDuration(Duration duration) {
        this.durationSeconds = duration == null ? 0L : duration.getSeconds();
    }

    /**
     * Sets the wall-clock instant the default was last changed at.
     *
     * @param timestamp the timestamp to set, or {@code null} to clear
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
