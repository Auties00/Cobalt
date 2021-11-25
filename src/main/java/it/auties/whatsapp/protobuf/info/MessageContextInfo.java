package it.auties.whatsapp.protobuf.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.protobuf.temp.DeviceListMetadata;
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
public class MessageContextInfo {
  @JsonProperty(value = "2")
  private int deviceListMetadataVersion;

  @JsonProperty(value = "1")
  private DeviceListMetadata deviceListMetadata;
}
