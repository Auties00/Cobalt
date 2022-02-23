package it.auties.whatsapp.protobuf.signal.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class DeviceIdentity {
  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int rawId;

  @JsonProperty("2")
  @JsonPropertyDescription("uint64")
  private long timestamp;

  @JsonProperty("3")
  @JsonPropertyDescription("uint32")
  private int keyIndex;
}
