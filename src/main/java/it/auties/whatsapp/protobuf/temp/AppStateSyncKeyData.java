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
public class AppStateSyncKeyData {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("int64")
  private long timestamp;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("AppStateSyncKeyFingerprint")
  private AppStateSyncKeyFingerprint fingerprint;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] keyData;
}
