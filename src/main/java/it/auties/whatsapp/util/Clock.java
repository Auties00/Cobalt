package it.auties.whatsapp.util;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.Instant.ofEpochMilli;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.ofInstant;

@UtilityClass
public class Clock {
    public long nowSeconds() {
        return Instant.now().getEpochSecond();
    }

    public long nowInMilliseconds() {
        return Instant.now().toEpochMilli();
    }

    public Optional<ZonedDateTime> parseSeconds(Long input) {
        return Optional.ofNullable(input)
                .filter(time -> time != 0)
                .map(time -> ofInstant(ofEpochSecond(time), systemDefault()));
    }

    public Optional<ZonedDateTime> parseMilliseconds(Long input) {
        return Optional.ofNullable(input)
                .filter(time -> time != 0)
                .map(time -> ofInstant(ofEpochMilli(time), systemDefault()));
    }
}
