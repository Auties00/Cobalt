package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HSMLocalizableParameter {
  @JsonProperty(value = "3")
  private HSMDateTime dateTime;

  @JsonProperty(value = "2")
  private HSMCurrency currency;

  @JsonProperty(value = "1")
  private String _default;

  public ParamOneof paramOneofCase() {
    if (currency != null) return ParamOneof.CURRENCY;
    if (dateTime != null) return ParamOneof.DATE_TIME;
    return ParamOneof.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum ParamOneof {
    UNKNOWN(0),
    CURRENCY(2),
    DATE_TIME(3);

    private final @Getter int index;

    ParamOneof(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ParamOneof forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(ParamOneof.UNKNOWN);
    }
  }
}
