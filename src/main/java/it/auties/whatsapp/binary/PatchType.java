package it.auties.whatsapp.binary;

import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;

public enum PatchType {
  CRITICAL_BLOCK,
  CRITICAL_UNBLOCK_LOW,
  REGULAR_HIGH,
  REGULAR_LOW,
  REGULAR;

  public static PatchType of(String name) {
    return Arrays.stream(values())
        .filter(entry -> entry.toString()
            .equals(name))
        .findAny()
        .orElseThrow(() -> new NoSuchElementException("No sync matches %s".formatted(name)));
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
