package it.auties.whatsapp.protobuf.model.app;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AppStateSyncKey {
  @JsonProperty(value = "2")
  private AppStateSyncKeyData keyData;

  @JsonProperty(value = "1")
  private AppStateSyncKeyId keyId;
}
