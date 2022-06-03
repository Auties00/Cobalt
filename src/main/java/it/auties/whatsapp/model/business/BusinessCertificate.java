package it.auties.whatsapp.model.business;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessCertificate implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] details;

  @ProtobufProperty(index = 2, type = BYTES)
  private byte[] signature;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] serverSignature;
}
