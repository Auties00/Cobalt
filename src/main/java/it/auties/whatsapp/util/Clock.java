package it.auties.whatsapp.util;

import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.ofInstant;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Clock {

  public long now() {
    return Instant.now()
        .getEpochSecond();
  }

  public Optional<ZonedDateTime> parse(Long input) {
    return Optional.ofNullable(input)
        .filter(time -> time != 0)
        .map(time -> ofInstant(ofEpochSecond(time), systemDefault()));
  }
}
