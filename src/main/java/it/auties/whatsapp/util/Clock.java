package it.auties.whatsapp.util;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@UtilityClass
public class Clock {
    public long nowSeconds() {
        return Instant.now().getEpochSecond();
    }

    public long nowMilliseconds() {
        return Instant.now().toEpochMilli();
    }

    public ZonedDateTime parseSeconds(Integer input) {
        return parseSeconds(input.longValue());
    }

    public ZonedDateTime parseSeconds(long input) {
        return input <= 0 ? ZonedDateTime.now() : ZonedDateTime.ofInstant(Instant.ofEpochSecond(input), ZoneId.systemDefault());
    }

    public ZonedDateTime parseMilliseconds(long input) {
        return input <= 0 ? ZonedDateTime.now() : ZonedDateTime.ofInstant(Instant.ofEpochMilli(input), ZoneId.systemDefault());
    }
}
