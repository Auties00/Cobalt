package it.auties.whatsapp.protobuf.unknown;

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
public class Money {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  private String currencyCode;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("uint32")
  private int offset;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("int64")
  private long value;
}
