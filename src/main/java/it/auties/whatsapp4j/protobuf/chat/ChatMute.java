package it.auties.whatsapp4j.protobuf.chat;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * An immutable model class that represents a WhatsappMute.
 * To change the mute status of a {@link Chat} use {@link WhatsappAPI#mute(Chat)} and {@link WhatsappAPI#unmute(Chat)}.
 *
 * @param time the end date of the mute associated with this object stored as second since {@link Instant#EPOCH}
 */
public record ChatMute(long time) {
    /**
     * An instance of ChatMute that represents an unknown mute status
     */
    public static final ChatMute UNKNOWN = new ChatMute(Integer.MIN_VALUE);

    /**
     * Returns whether the chat associated with this object is muted or not.
     * If the mute status is unknown({@link ChatMute#type()} == {@link ChatMuteType#UNKNOWN}), false is returned.
     *
     * @return true if the chat associated with this object is muted
     */
    public boolean isMuted() {
        return type() != ChatMuteType.NOT_MUTED && type() != ChatMuteType.UNKNOWN;
    }

    /**
     * Returns a non null enum that describes the type of mute for this object
     *
     * @return a non null enum that describes the type of mute for this object
     */
    public @NonNull ChatMuteType type() {
        if (this == UNKNOWN) return ChatMuteType.UNKNOWN;
        if (time == -1) return ChatMuteType.MUTED_INDEFINITELY;
        if (time == 0) return ChatMuteType.NOT_MUTED;
        return ChatMuteType.MUTED_FOR_TIMEFRAME;
    }

    /**
     * Returns an optional time representing the date that the mute associated with this object ends
     *
     * @return a non empty optional date if {@link ChatMute#time} > 0
     */
    public @NonNull Optional<ZonedDateTime> muteEndDate() {
        return isMuted() ? Optional.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault())) : Optional.empty();
    }
}