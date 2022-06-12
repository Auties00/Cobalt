package it.auties.whatsapp.model.chat;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.util.Clock;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * An immutable model class that represents a WhatsappMute.
 * To change the mute status of a {@link Chat} use {@link Whatsapp#mute(ContactJidProvider)} and {@link Whatsapp#unmute(ContactJidProvider)}.
 *
 * @param endTimeStamp the end date of the mute associated with this object stored as second since {@link Instant#EPOCH}
 */
public record ChatMute(long endTimeStamp) implements ProtobufMessage {
    /**
     * Not muted constant
     */
    private static final ChatMute NOT_MUTED = new ChatMute(-1);

    /**
     * Muted constant
     */
    private static final ChatMute MUTED = new ChatMute(0);

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
        return MUTED;
    }

    /**
     * Constructs a new muted ChatMute for an end timestamp in seconds
     *
     * @param endTimeStamp an end timestamp
     * @return a non-null mute
     */
    public static ChatMute muted(Long endTimeStamp) {
        return endTimeStamp == null ?
                NOT_MUTED :
                new ChatMute(endTimeStamp);
    }

    /**
     * Returns whether the chat associated with this object is muted or not.
     *
     * @return true if the chat associated with this object is muted
     */
    public boolean isMuted() {
        return type() != ChatMuteType.NOT_MUTED;
    }

    /**
     * Returns a non-null enum that describes the type of mute for this object
     *
     * @return a non-null enum that describes the type of mute for this object
     */
    public ChatMuteType type() {
        if (endTimeStamp == -1) {
            return ChatMuteType.MUTED_INDEFINITELY;
        }

        if (endTimeStamp == 0) {
            return ChatMuteType.NOT_MUTED;
        }

        return ChatMuteType.MUTED_FOR_TIMEFRAME;
    }

    /**
     * Returns an optional endTimeStamp representing the date that the mute associated with this object ends
     *
     * @return a non-empty optional date if {@link ChatMute#endTimeStamp} > 0
     */
    public Optional<ZonedDateTime> end() {
        return Clock.parse(endTimeStamp);
    }

    @Override
    public Long value() {
        return endTimeStamp;
    }
}