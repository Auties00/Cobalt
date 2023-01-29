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
@ProtobufName("IdentityKeyPairStructure")
public class IdentityKeyPairStructure implements ProtobufMessage {
  @ProtobufProperty(index = 1, name = "publicKey", type = ProtobufType.BYTES)
  private byte[] publicKey;

  @ProtobufProperty(index = 2, name = "privateKey", type = ProtobufType.BYTES)
  private byte[] privateKey;
}