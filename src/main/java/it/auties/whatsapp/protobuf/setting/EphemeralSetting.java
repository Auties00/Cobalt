package it.auties.whatsapp.protobuf.setting;

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
public final class EphemeralSetting implements Setting {
  @JsonProperty("1")
  @JsonPropertyDescription("sfixed32")
  private int duration;

  @JsonProperty("2")
  @JsonPropertyDescription("sfixed64")
  private long timestamp;

  @Override
  public String indexName() {
    return "unknown";
  }
}
