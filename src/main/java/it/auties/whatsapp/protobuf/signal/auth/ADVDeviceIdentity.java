package it.auties.whatsapp.protobuf.signal.auth;

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
public class ADVDeviceIdentity {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("uint32")
  private int keyIndex;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("uint64")
  private long timestamp;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint32")
  private int rawId;
}
