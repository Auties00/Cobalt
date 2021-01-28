package it.auties.whatsapp4j.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@Accessors(fluent = true)
public enum MuteType {
    NOT_MUTED(0),
    EIGHT_HOURS(1611776396),
    ONE_WEEK(1612352446),
    ALWAYS(-1);

    @Getter
    private final int timeInSeconds;

    public static MuteType forValue(int value) {
        return Arrays.stream(values()).filter(entry -> entry.timeInSeconds() == value).findFirst().orElseThrow();
    }
}