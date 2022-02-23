package it.auties.whatsapp.protobuf.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class BusinessLocalizedName {
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String lg;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String lc;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String name;
}
