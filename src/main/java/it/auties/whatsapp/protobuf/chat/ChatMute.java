package it.auties.whatsapp.protobuf.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.NonNull;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * An immutable model class that represents a WhatsappMute.
 * To change the mute status of a {@link Chat} use {@link Whatsapp#mute(Chat)} and {@link Whatsapp#unmute(Chat)}.
 *
 * @param time the end date of the mute associated with this object stored as second since {@link Instant#EPOCH}
 */
public record ChatMute(@JsonProperty long time) {
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
    public @NonNull ChatMuteType type() {
        if (time == -1) {
            return ChatMuteType.MUTED_INDEFINITELY;
        }

        if (time == 0) {
            return ChatMuteType.NOT_MUTED;
        }

        return ChatMuteType.MUTED_FOR_TIMEFRAME;
    }

    /**
     * Returns an optional time representing the date that the mute associated with this object ends
     *
     * @return a non-empty optional date if {@link ChatMute#time} > 0
     */
    public @NonNull Optional<ZonedDateTime> end() {
        return WhatsappUtils.parseWhatsappTime(time);
    }
}