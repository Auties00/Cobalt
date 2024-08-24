package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.time.Duration;
import java.util.Arrays;

/**
 * Enum representing the ChatEphemeralTimer period. Each constant is associated with a specific
 * duration period.
 */
@ProtobufEnum
public enum ChatEphemeralTimer {
    /**
     * ChatEphemeralTimer with duration of 0 days.
     */
    OFF(0, Duration.ofDays(0)),

    /**
     * ChatEphemeralTimer with duration of 1 day.
     */
    ONE_DAY(1, Duration.ofDays(1)),

    /**
     * ChatEphemeralTimer with duration of 7 days.
     */
    ONE_WEEK(2, Duration.ofDays(7)),

    /**
     * ChatEphemeralTimer with duration of 90 days.
     */
    THREE_MONTHS(3, Duration.ofDays(90));

    private final Duration period;
    final int index;

    ChatEphemeralTimer(@ProtobufEnumIndex int index, Duration period) {
        this.index = index;
        this.period = period;
    }

    public int index() {
        return index;
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