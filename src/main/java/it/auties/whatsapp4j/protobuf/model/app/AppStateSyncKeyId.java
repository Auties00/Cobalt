package it.auties.whatsapp4j.protobuf.model.app;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class AppStateSyncKeyId {
  @JsonProperty(value = "1")
  private byte[] keyId;
}
