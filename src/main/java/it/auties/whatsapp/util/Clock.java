package it.auties.whatsapp.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;

public final class Clock {
    public static long nowSeconds() {
        return Instant.now().getEpochSecond();
    }

    public static long nowMilliseconds() {
        return Instant.now().toEpochMilli();
    }

    public static OptionalLong parseTimestamp(Number input) {
        return input == null ? OptionalLong.empty() : OptionalLong.of(input.longValue());
    }

    public static Optional<ZonedDateTime> parseSeconds(Number input) {
        return parseTimestamp(input, Instant::ofEpochSecond);
    }

    public static Optional<ZonedDateTime> parseMilliseconds(Number input) {
        return parseTimestamp(input, Instant::ofEpochMilli);
    }

    private static Optional<ZonedDateTime> parseTimestamp(Number input, Function<Long, Instant> converter) {
        return Optional.ofNullable(input)
                .map(Number::longValue)
                .filter(value -> value > 0)
                .map(value -> ZonedDateTime.ofInstant(converter.apply(value), ZoneId.systemDefault()));
    }
}
