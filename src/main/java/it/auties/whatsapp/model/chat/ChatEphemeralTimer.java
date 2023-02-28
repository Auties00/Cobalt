package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufConverter;
import it.auties.protobuf.base.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.Arrays;

/**
 * Enum representing the ChatEphemeralTimer period. Each constant is associated with a specific
 * duration period.
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum ChatEphemeralTimer implements ProtobufMessage {
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

    /**
     * Getter for the duration period
     */
    @Getter
    private final Duration period;

    /**
     * Factory method for creating a ChatEphemeralTimer instance based on the specified value.
     *
     * @param value the value to use for creating the ChatEphemeralTimer
     * @return the ChatEphemeralTimer instance that matches the specified value
     */
    @JsonCreator
    public static ChatEphemeralTimer of(long value) {
        return Arrays.stream(values())
                .filter(entry -> entry.period().toSeconds() == value || entry.period().toDays() == value)
                .findFirst()
                .orElse(OFF);
    }

    @ProtobufConverter
    @SuppressWarnings("unused")
    public Object toValue() {
        return period.toSeconds();
    }
}