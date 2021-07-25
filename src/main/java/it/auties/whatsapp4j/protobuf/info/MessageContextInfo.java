package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.model.misc.DeviceListMetadata;
import lombok.*;
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
