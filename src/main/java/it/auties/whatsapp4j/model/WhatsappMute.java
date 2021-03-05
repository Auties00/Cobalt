package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public record WhatsappMute(long time) {
    public boolean isMuted() {
        return type() != WhatsappMuteType.NOT_MUTED;
    }

    public @NotNull WhatsappMuteType type() {
        return time == -1 ? WhatsappMuteType.MUTED_INDEFINITELY : time == 0 ? WhatsappMuteType.NOT_MUTED : WhatsappMuteType.MUTED_FOR_TIMEFRAME;
    }

    public @NotNull Optional<ZonedDateTime> muteEndDate() {
        return isMuted() ? Optional.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault())) : Optional.empty();
    }

    public enum WhatsappMuteType {
        NOT_MUTED, MUTED_FOR_TIMEFRAME, MUTED_INDEFINITELY
    }
}