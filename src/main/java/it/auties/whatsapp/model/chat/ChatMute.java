package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.whatsapp.util.Clock;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * An immutable model class that represents a mute
 *
 * @param endTimeStamp the end date of the mute associated with this object stored as second since
 *                     {@link Instant#EPOCH}
 */
public record ChatMute(long endTimeStamp) {
    /**
     * Not muted flag
     */
    private static final long NOT_MUTED_FLAG = 0;

    /**
     * Muted flag
     */
    private static final long MUTED_INDEFINITELY_FLAG = -1;

    /**
     * Not muted constant
     */
    private static final ChatMute NOT_MUTED = new ChatMute(NOT_MUTED_FLAG);

    /**
     * Muted constant
     */
    private static final ChatMute MUTED_INDEFINITELY = new ChatMute(MUTED_INDEFINITELY_FLAG);

    /**
     * Constructs a new not muted ChatMute
     *
     * @return a non-null mute
     */
    public static ChatMute notMuted() {
        return NOT_MUTED;
    }

    /**
     * Constructs a new muted ChatMute
     *
     * @return a non-null mute
     */
    public static ChatMute muted() {
        return MUTED_INDEFINITELY;
    }

    /**
     * Constructs a new mute that lasts eight hours
     *
     * @return a non-null mute
     */
    public static ChatMute mutedForEightHours() {
        return muted(ZonedDateTime.now().plusHours(8).toEpochSecond());
    }

    /**
     * Do not use this method, reserved for protobuf
     */
    @ProtobufDeserializer
    public static ChatMute ofProtobuf(long object) {
        return muted(object);
    }

    /**
     * Constructs a new mute for a duration in endTimeStamp
     *
     * @param seconds can be null and is considered as not muted
     * @return a non-null mute
     */
    public static ChatMute muted(Long seconds) {
        if (seconds == null || seconds == NOT_MUTED_FLAG) {
            return NOT_MUTED;
        }
        if (seconds == MUTED_INDEFINITELY_FLAG) {
            return MUTED_INDEFINITELY;
        }
        return new ChatMute(seconds);
    }

    /**
     * Constructs a new mute that lasts one week
     *
     * @return a non-null mute
     */
    public static ChatMute mutedForOneWeek() {
        return muted(ZonedDateTime.now().plusWeeks(1).toEpochSecond());
    }

    /**
     * Returns whether the chat associated with this object is muted or not.
     *
     * @return true if the chat associated with this object is muted
     */
    public boolean isMuted() {
        return type() != Type.NOT_MUTED;
    }

    /**
     * Returns a non-null enum that describes the type of mute for this object
     *
     * @return a non-null enum that describes the type of mute for this object
     */
    public Type type() {
        if (endTimeStamp == MUTED_INDEFINITELY_FLAG) {
            return Type.MUTED_INDEFINITELY;
        }
        if (endTimeStamp == NOT_MUTED_FLAG) {
            return Type.NOT_MUTED;
        }
        return Type.MUTED_FOR_TIMEFRAME;
    }

    /**
     * Returns the date when this mute expires if the chat is muted and not indefinitely
     *
     * @return a non-empty optional date if {@link ChatMute#endTimeStamp} > 0
     */
    public Optional<ZonedDateTime> end() {
        return Clock.parseSeconds(endTimeStamp);
    }

    @ProtobufSerializer
    public long endTimeStamp() {
        return endTimeStamp;
    }

    /**
     * The constants of this enumerated type describe the various types of mute a {@link ChatMute} can
     * describe
     */
    public enum Type {
        /**
         * This constant describes a {@link ChatMute} that holds a seconds greater than 0 Simply put,
         * {@link ChatMute#endTimeStamp()} > 0
         */
        MUTED_FOR_TIMEFRAME,
        /**
         * This constant describes a {@link ChatMute} that holds a seconds equal to -1 Simply put,
         * {@link ChatMute#endTimeStamp()} == -1
         */
        MUTED_INDEFINITELY,
        /**
         * This constant describes a {@link ChatMute} that holds a seconds equal to 0 Simply put,
         * {@link ChatMute#endTimeStamp()} == 0
         */
        NOT_MUTED
    }
}