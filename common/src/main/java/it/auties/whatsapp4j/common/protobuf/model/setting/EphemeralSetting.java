package it.auties.whatsapp4j.common.protobuf.model.setting;

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
public class EphemeralSetting {
  @JsonProperty(value = "2")
  private long timestamp;

  @JsonProperty(value = "1")
  private int duration;
}
