package it.auties.whatsapp.protobuf.sync;

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
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] keyData;

  @JsonProperty("2")
  @JsonPropertyDescription("AppStateSyncKeyFingerprint")
  private AppStateSyncKeyFingerprint fingerprint;

  @JsonProperty("3")
  @JsonPropertyDescription("int64")
  private long timestamp;
}
