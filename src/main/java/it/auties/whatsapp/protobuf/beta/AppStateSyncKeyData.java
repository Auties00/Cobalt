package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class AppStateSyncKeyData {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("int64")
  private long timestamp;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("AppStateSyncKeyFingerprint")
  private AppStateSyncKeyFingerprint fingerprint;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] keyData;
}
