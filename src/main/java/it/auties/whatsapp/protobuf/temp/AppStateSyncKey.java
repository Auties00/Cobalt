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
public class AppStateSyncKey {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("AppStateSyncKeyData")
  private AppStateSyncKeyData keyData;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("AppStateSyncKeyId")
  private AppStateSyncKeyId keyId;
}
