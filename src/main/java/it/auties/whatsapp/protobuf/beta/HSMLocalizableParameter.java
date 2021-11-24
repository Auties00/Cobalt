package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HSMLocalizableParameter {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("HSMDateTime")
  private HSMDateTime dateTime;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("HSMCurrency")
  private HSMCurrency currency;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
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
