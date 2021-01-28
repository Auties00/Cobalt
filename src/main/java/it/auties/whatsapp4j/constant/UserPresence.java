package it.auties.whatsapp4j.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@AllArgsConstructor
@Accessors(fluent = true)
public enum UserPresence {
    AVAILABLE("available", Flag.AVAILABLE),
    UNAVAILABLE("unavailable", Flag.UNAVAILABLE),
    COMPOSING("composing", Flag.COMPOSING),
    RECORDING("recording", Flag.RECORDING),
    PAUSED("paused", Flag.PAUSED);

    @Getter
    private final String content;
    @Getter
    private final byte data;

    @JsonCreator
    public static UserPresence forValue(@NotNull String jsonValue) {
        return Arrays.stream(values()).filter(entry -> entry.content().equalsIgnoreCase(jsonValue)).findFirst().orElseThrow();
    }
}
