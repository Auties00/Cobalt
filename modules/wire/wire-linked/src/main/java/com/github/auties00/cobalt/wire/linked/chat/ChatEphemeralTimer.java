package com.github.auties00.cobalt.wire.linked.chat;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.time.Duration;
import java.util.Arrays;

/**
 * Represents the available durations for disappearing messages in a WhatsApp chat.
 *
 * <p>When the disappearing-messages feature is enabled in a one-to-one or group
 * conversation, every new message sent to that chat is automatically deleted from
 * every participant's device after the configured duration has elapsed since the
 * message was sent. WhatsApp restricts the timer to a small fixed set of values,
 * exposed as the constants on this enum: 24 hours, 7 days, and 90 days, plus the
 * special {@link #OFF} value that disables the feature entirely.
 *
 * <p>The timer is serialized over the wire as the duration in seconds. The
 * {@link #of(Integer)} factory accepts either a seconds value (the canonical
 * wire encoding) or a days value (a convenience for callers that already work
 * in days), falling back to {@link #OFF} on unrecognized input.
 */
public enum ChatEphemeralTimer {
    /**
     * Disappearing messages are disabled. New messages sent to the chat are
     * retained indefinitely on every participant's device.
     */
    OFF(Duration.ofDays(0)),

    /**
     * Disappearing-messages timer of 24 hours. New messages are automatically
     * deleted one day after they are sent.
     */
    ONE_DAY(Duration.ofDays(1)),

    /**
     * Disappearing-messages timer of 7 days. New messages are automatically
     * deleted one week after they are sent.
     */
    ONE_WEEK(Duration.ofDays(7)),

    /**
     * Disappearing-messages timer of 90 days. New messages are automatically
     * deleted three months after they are sent.
     */
    THREE_MONTHS(Duration.ofDays(90));

    /**
     * The duration after which messages tagged with this timer are automatically
     * deleted, expressed as a {@link Duration}. {@link #OFF} carries a zero
     * duration to indicate that no auto-deletion takes place.
     */
    private final Duration period;

    /**
     * Constructs a {@code ChatEphemeralTimer} with the given duration.
     *
     * @param period the duration after which messages are deleted
     */
    ChatEphemeralTimer(Duration period) {
        this.period = period;
    }

    /**
     * Returns the duration after which messages tagged with this timer are
     * automatically deleted. {@link #OFF} returns a zero duration.
     *
     * @return the ephemeral timer duration, never {@code null}
     */
    public Duration period() {
        return period;
    }

    /**
     * Returns the {@code ChatEphemeralTimer} matching the given integer value.
     *
     * <p>The value may be expressed in seconds (the canonical wire encoding)
     * or in days (for caller convenience). {@code null} or an unrecognized
     * value yields {@link #OFF}.
     *
     * @param value the timer value in seconds or days, or {@code null}
     * @return the matching timer, or {@link #OFF} if no match is found
     */
    @ProtobufDeserializer
    public static ChatEphemeralTimer of(Integer value) {
        return value == null ? OFF : Arrays.stream(values())
                .filter(entry -> entry.period().toSeconds() == value || entry.period().toDays() == value)
                .findFirst()
                .orElse(OFF);
    }

    /**
     * Returns this timer's duration expressed in seconds, suitable for
     * protobuf serialization.
     *
     * @return the duration in seconds as an {@link Integer}
     */
    @ProtobufSerializer
    public Integer periodSeconds() {
        return (int) period.toSeconds();
    }

    /**
     * Returns whether the given duration in seconds is a valid ephemeral-timer
     * value accepted by WhatsApp.
     *
     * <p>A duration is allowed when it is either {@code 0} (which disables
     * disappearing messages, matching {@link #OFF}) or one of the fixed
     * positive values that correspond to {@link #ONE_DAY}, {@link #ONE_WEEK}
     * and {@link #THREE_MONTHS}. Negative values and any other positive value
     * not in the fixed set are rejected.
     *
     * @param durationSeconds the candidate duration, in seconds
     * @return {@code true} if {@code durationSeconds} is {@code 0} or matches
     *         one of the defined timers, {@code false} otherwise
     */
    public static boolean isEphemeralDurationAllowed(int durationSeconds) {
        if (durationSeconds < 0) {
            return false;
        }
        if (durationSeconds == 0) {
            return true;
        }
        for (var timer : values()) {
            if (timer != OFF && timer.period.toSeconds() == durationSeconds) {
                return true;
            }
        }
        return false;
    }
}
