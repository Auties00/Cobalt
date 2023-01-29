package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("PendingPreKey")
public class PendingPreKey implements ProtobufMessage {
  @ProtobufProperty(index = 1, name = "preKeyId", type = ProtobufType.UINT32)
  private Integer preKeyId;

  @ProtobufProperty(index = 3, name = "signedPreKeyId", type = ProtobufType.INT32)
  private Integer signedPreKeyId;

  @ProtobufProperty(index = 2, name = "baseKey", type = ProtobufType.BYTES)
  private byte[] baseKey;
}