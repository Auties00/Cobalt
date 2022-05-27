package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT64;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class DeviceIdentity implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = UINT32)
  private Integer rawId;

  @ProtobufProperty(index = 2, type = UINT64)
  private Long timestamp;

  @ProtobufProperty(index = 3, type = UINT32)
  private Integer keyIndex;
}
