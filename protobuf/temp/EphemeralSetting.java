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
public class EphemeralSetting {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("sfixed64")
  private long timestamp;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("sfixed32")
  private int duration;
}
