package it.auties.whatsapp4j.protobuf.model.setting;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
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
