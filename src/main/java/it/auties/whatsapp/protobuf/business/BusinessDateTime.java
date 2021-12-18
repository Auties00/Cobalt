package it.auties.whatsapp.protobuf.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class BusinessDateTime {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("HSMDateTimeUnixEpoch")
  private BusinessDateTimeUnixEpoch unixEpoch;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("HSMDateTimeComponent")
  private BusinessDateTimeComponent component;

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
