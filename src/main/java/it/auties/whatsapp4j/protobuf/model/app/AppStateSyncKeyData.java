package it.auties.whatsapp4j.protobuf.model.app;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class AppStateSyncKeyData {
  @JsonProperty(value = "3")
  private long timestamp;

  @JsonProperty(value = "2")
  private AppStateSyncKeyFingerprint fingerprint;

  @JsonProperty(value = "1")
  private byte[] keyData;
}
