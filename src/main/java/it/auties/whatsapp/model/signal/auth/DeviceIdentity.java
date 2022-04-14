package it.auties.whatsapp.model.signal.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class DeviceIdentity implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = UINT32)
  private int rawId;

  @ProtobufProperty(index = 2, type = UINT64)
  private long timestamp;

  @ProtobufProperty(index = 3, type = UINT32)
  private int keyIndex;
}
