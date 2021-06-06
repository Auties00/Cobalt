package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HSMDateTime {
  @JsonProperty(value = "2")
  private HSMDateTimeUnixEpoch unixEpoch;

  @JsonProperty(value = "1")
  private HSMDateTimeComponent component;

  public DatetimeOneof datetimeOneofCase() {
    if (component != null) return DatetimeOneof.COMPONENT;
    if (unixEpoch != null) return DatetimeOneof.UNIX_EPOCH;
    return DatetimeOneof.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum DatetimeOneof {
    UNKNOWN(0),
    COMPONENT(1),
    UNIX_EPOCH(2);

    private final @Getter int index;

    DatetimeOneof(int index) {
      this.index = index;
    }

    @JsonCreator
    public static DatetimeOneof forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(DatetimeOneof.UNKNOWN);
    }
  }
}
