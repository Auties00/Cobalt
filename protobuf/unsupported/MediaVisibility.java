package it.auties.whatsapp;

import java.util.*;
import lombok.*;
import lombok.experimental.*;

@AllArgsConstructor
@Accessors(fluent = true)
public enum MediaVisibility {
  DEFAULT(0),
  OFF(1),
  ON(2);

  @Getter private final int index;

  public static MediaVisibility forIndex(int index) {
    return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
  }
}
