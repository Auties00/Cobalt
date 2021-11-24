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
public class ADVDeviceIdentity {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint32")
  private int keyIndex;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("uint64")
  private long timestamp;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("uint32")
  private int rawId;
}
