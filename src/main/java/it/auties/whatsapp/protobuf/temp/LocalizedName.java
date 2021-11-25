package it.auties.whatsapp.protobuf.temp;

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
public class LocalizedName {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  private String verifiedName;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String lc;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String lg;
}
