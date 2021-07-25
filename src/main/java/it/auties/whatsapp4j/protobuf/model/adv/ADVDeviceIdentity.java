package it.auties.whatsapp4j.protobuf.model.adv;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ADVDeviceIdentity {
  @JsonProperty(value = "3")
  private int keyIndex;

  @JsonProperty(value = "2")
  private long timestamp;

  @JsonProperty(value = "1")
  private int rawId;
}
