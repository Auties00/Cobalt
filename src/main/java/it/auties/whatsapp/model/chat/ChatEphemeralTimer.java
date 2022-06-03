package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.NoSuchElementException;

import static java.util.concurrent.TimeUnit.DAYS;

@AllArgsConstructor
@Accessors(fluent = true)
public enum ChatEphemeralTimer implements ProtobufMessage {
    OFF(0),
    ONE_DAY(DAYS.toSeconds(1)),
    ONE_WEEK(DAYS.toSeconds(7)),
    THREE_MONTHS(DAYS.toSeconds(90));

    @Getter
    private final long timeInSeconds;

    @JsonCreator
    public static ChatEphemeralTimer forSeconds(long seconds) {
        return Arrays.stream(values())
                .filter(entry -> entry.timeInSeconds() == seconds)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("%s is not a valid ephemeral time".formatted(seconds)));
    }

    @Override
    public Object value() {
        return timeInSeconds;
    }
}
