package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * An immutable model class that represents a WhatsappMute.
 * To change the mute status of a {@link WhatsappChat} use {@link WhatsappAPI#mute(WhatsappChat)} and {@link WhatsappAPI#unmute(WhatsappChat)}.
 *
 * @param time the end date of the mute associated with this object stored as second since {@link Instant#EPOCH}
 */
public record WhatsappMute(long time) {
    /**
     * Returns whether the chat associated with this object is muted or not
     *
     * @return true if the chat associated with this object is muted
     */
    public boolean isMuted() {
        return type() != WhatsappMuteType.NOT_MUTED;
    }

    /**
     * Returns a non null enum that describes the type of mute for this object
     *
     * @return a non null enum that describes the type of mute for this object
     */
    public @NotNull WhatsappMuteType type() {
        return time == -1 ? WhatsappMuteType.MUTED_INDEFINITELY : time == 0 ? WhatsappMuteType.NOT_MUTED : WhatsappMuteType.MUTED_FOR_TIMEFRAME;
    }

    /**
     * Returns an optional time representing the date that the mute associated with this object ends
     *
     * @return a non empty optional date if {@link WhatsappMute#time} > 0
     */
    public @NotNull Optional<ZonedDateTime> muteEndDate() {
        return isMuted() ? Optional.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault())) : Optional.empty();
    }
}