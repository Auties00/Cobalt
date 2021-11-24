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
public class EphemeralSetting {

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("sfixed64")
  private long timestamp;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("sfixed32")
  private int duration;
}
