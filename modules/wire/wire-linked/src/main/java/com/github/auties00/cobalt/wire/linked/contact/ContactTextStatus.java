package com.github.auties00.cobalt.wire.linked.contact;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A model representing the text-based "about" status of a WhatsApp user.
 *
 * <p>A text status is the short personal note that a user sets on their own
 * profile (for example {@code "Available"}, {@code "At work"} or a custom
 * sentence). It is distinct from story-style {@code Status} updates posted to
 * the Status tab: a text status is a persistent piece of profile metadata that
 * is shown alongside a contact's name, while story statuses are time-limited
 * media posts.
 *
 * <p>A text status is composed of up to three pieces of information: a free
 * form text string, an optional emoji shown next to the text, and an optional
 * ephemeral duration in seconds after which the status is automatically
 * cleared. The {@linkplain #lastUpdateTime() last update time} records when
 * the status was last changed on the server and is used by the client to
 * decide whether the locally cached status is still fresh.
 *
 * <p>Text statuses are fetched from the server through the USync protocol
 * using the {@code text_status} query and are kept synchronised by dedicated
 * handlers. This class is a local model only: modifying its fields does not
 * send any request to the WhatsApp servers; to update the current user's own
 * text status use the dedicated profile management APIs of
 * {@code LinkedWhatsAppClient}.
 *
 * @see Contact
 */
@ProtobufMessage
public final class ContactTextStatus {
    /**
     * The free-form text shown as the contact's about status. This value is
     * {@code null} when the user has not set any custom text, in which case
     * the client typically falls back to the default localised "Available"
     * string.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String text;

    /**
     * The emoji displayed next to the {@linkplain #text() text} on the contact's
     * about status. This is a short unicode string (usually a single emoji
     * grapheme) chosen by the user through the status editor. This value is
     * {@code null} when the user has not associated any emoji with the status.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String emoji;

    /**
     * The ephemeral duration of the status in seconds. When set, the status is
     * automatically cleared on the server after this amount of time has
     * elapsed from the {@linkplain #lastUpdateTime() last update time}, after
     * which the user's about reverts to the default string. This value is
     * {@code null} for non-ephemeral statuses, which persist until explicitly
     * changed by the user.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer ephemeralDurationSeconds;

    /**
     * The epoch-second timestamp of the last time the server reported that the
     * status was updated. This value is used by the client both to display
     * "updated at" metadata and to detect whether a cached status is still
     * fresh compared to a newly fetched server response. This value is
     * {@code null} when the status has never been retrieved from the server.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    Long lastUpdateTimeSeconds;

    /**
     * Constructs a new text status with the given field values.
     *
     * @param text                     the free-form status text, or {@code null}
     * @param emoji                    the emoji associated with the status,
     *                                 or {@code null}
     * @param ephemeralDurationSeconds the ephemeral duration in seconds, or
     *                                 {@code null} for non-ephemeral statuses
     * @param lastUpdateTimeSeconds    the epoch-second timestamp of the last
     *                                 update, or {@code null} if unknown
     */
    ContactTextStatus(String text, String emoji, Integer ephemeralDurationSeconds, Long lastUpdateTimeSeconds) {
        this.text = text;
        this.emoji = emoji;
        this.ephemeralDurationSeconds = ephemeralDurationSeconds;
        this.lastUpdateTimeSeconds = lastUpdateTimeSeconds;
    }

    /**
     * Returns the free-form text of this status.
     *
     * @return an {@code Optional} containing the status text, or empty if the
     *         user has not set any custom text
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the emoji associated with this status.
     *
     * @return an {@code Optional} containing the emoji string, or empty if no
     *         emoji has been set
     */
    public Optional<String> emoji() {
        return Optional.ofNullable(emoji);
    }

    /**
     * Returns the ephemeral duration of this status in seconds.
     *
     * <p>When present, the status is automatically cleared by the server after
     * this many seconds have elapsed since the {@linkplain #lastUpdateTime()
     * last update time}.
     *
     * @return an {@code OptionalInt} containing the duration in seconds, or
     *         empty for non-ephemeral statuses
     */
    public OptionalInt ephemeralDurationSeconds() {
        return ephemeralDurationSeconds == null ? OptionalInt.empty() : OptionalInt.of(ephemeralDurationSeconds);
    }

    /**
     * Returns the timestamp of the last status update reported by the server.
     *
     * @return an {@code Optional} containing the last update instant, or empty
     *         if unknown
     */
    public Optional<Instant> lastUpdateTime() {
        return lastUpdateTimeSeconds == null ? Optional.empty() : Optional.of(Instant.ofEpochSecond(lastUpdateTimeSeconds));
    }

    /**
     * Sets the free-form text of this status.
     *
     * @param text the new status text, or {@code null} to clear it
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the emoji associated with this status.
     *
     * @param emoji the new emoji string, or {@code null} to clear it
     */
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    /**
     * Sets the ephemeral duration of this status in seconds.
     *
     * @param ephemeralDurationSeconds the new duration in seconds, or
     *                                 {@code null} to make the status
     *                                 non-ephemeral
     */
    public void setEphemeralDurationSeconds(Integer ephemeralDurationSeconds) {
        this.ephemeralDurationSeconds = ephemeralDurationSeconds;
    }

    /**
     * Sets the timestamp of the last status update.
     *
     * @param lastUpdateTime the new update instant, or {@code null} to clear
     *                       it
     */
    public void setLastUpdateTime(Instant lastUpdateTime) {
        this.lastUpdateTimeSeconds = lastUpdateTime == null ? null : lastUpdateTime.getEpochSecond();
    }

    /**
     * Returns whether this text status is equal to the given object.
     *
     * <p>Two text statuses are considered equal when their text, emoji,
     * ephemeral duration and last update timestamp are all equal.
     *
     * @param o the object to compare with
     * @return {@code true} if the other object is a {@code ContactTextStatus}
     *         with identical field values
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ContactTextStatus that
                && Objects.equals(text, that.text)
                && Objects.equals(emoji, that.emoji)
                && Objects.equals(ephemeralDurationSeconds, that.ephemeralDurationSeconds)
                && Objects.equals(lastUpdateTimeSeconds, that.lastUpdateTimeSeconds);
    }

    /**
     * Returns a hash code derived from the text, emoji, ephemeral duration and
     * last update timestamp of this status.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(text, emoji, ephemeralDurationSeconds, lastUpdateTimeSeconds);
    }
}
