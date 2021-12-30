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
public class BusinessLocalizableParameter {
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String defaultValue;

  @JsonProperty("2")
  @JsonPropertyDescription("HSMCurrency")
  private BusinessCurrency currency;
  
  @JsonProperty("3")
  @JsonPropertyDescription("HSMDateTime")
  private BusinessDateTime dateTime;

  public ParamOneof paramOneofType() {
    if (currency != null) return ParamOneof.CURRENCY;
    if (dateTime != null) return ParamOneof.DATE_TIME;
    return ParamOneof.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ParamOneof {
    UNKNOWN(0),
    CURRENCY(2),
    DATE_TIME(3);

    @Getter
    private final int index;
    
    @JsonCreator
    public static ParamOneof forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(ParamOneof.UNKNOWN);
    }
  }
}
