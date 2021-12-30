package it.auties.whatsapp.protobuf.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class BusinessCurrency {
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String currencyCode;

  @JsonProperty("2")
  @JsonPropertyDescription("int64")
  private long amount1000;
}
