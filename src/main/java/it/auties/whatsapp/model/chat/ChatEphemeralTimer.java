package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.time.Duration;
import java.util.Arrays;

/**
 * Enum representing the ChatEphemeralTimer period. Each constant is associated with a specific
 * duration period.
 */
public enum ChatEphemeralTimer {
    /**
     * ChatEphemeralTimer with duration of 0 days.
     */
    OFF(Duration.ofDays(0)),

    /**
     * ChatEphemeralTimer with duration of 1 day.
     */
    ONE_DAY(Duration.ofDays(1)),

    /**
     * ChatEphemeralTimer with duration of 7 days.
     */
    ONE_WEEK(Duration.ofDays(7)),

    /**
     * ChatEphemeralTimer with duration of 90 days.
     */
    THREE_MONTHS(Duration.ofDays(90));

    private final Duration period;

    ChatEphemeralTimer(Duration period) {
        this.period = period;
    }

    public Duration period() {
        return period;
    }

    @ProtobufDeserializer
    public static ChatEphemeralTimer of(int value) {
        return Arrays.stream(values())
                .filter(entry -> entry.period().toSeconds() == value || entry.period().toDays() == value)
                .findFirst()
                .orElse(OFF);
    }

    @ProtobufSerializer
    public int periodSeconds() {
        return (int) period.toSeconds();
    }
}