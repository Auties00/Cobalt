package it.auties.whatsapp.protobuf.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class BusinessDateTime {
  @JsonProperty("1")
  @JsonPropertyDescription("HSMDateTimeComponent")
  private BusinessDateTimeComponent component;
  
  @JsonProperty("2")
  @JsonPropertyDescription("HSMDateTimeUnixEpoch")
  private BusinessDateTimeUnixEpoch unixEpoch;

  public DatetimeOneof datetimeOneofType() {
    if (component != null) return DatetimeOneof.COMPONENT;
    if (unixEpoch != null) return DatetimeOneof.UNIX_EPOCH;
    return DatetimeOneof.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum DatetimeOneof {
    UNKNOWN(0),
    COMPONENT(1),
    UNIX_EPOCH(2);

    @Getter
    private final int index;

    @JsonCreator
    public static DatetimeOneof forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(DatetimeOneof.UNKNOWN);
    }
  }
}
