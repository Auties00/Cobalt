package it.auties.whatsapp4j.protobuf.model.app;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class AppStateSyncKey {
  @JsonProperty(value = "2")
  private AppStateSyncKeyData keyData;

  @JsonProperty(value = "1")
  private AppStateSyncKeyId keyId;
}
