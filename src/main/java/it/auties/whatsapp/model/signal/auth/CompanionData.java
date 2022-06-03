package it.auties.whatsapp.model.signal.auth;

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
public class CompanionData implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] id;

  @ProtobufProperty(index = 2, type = BYTES)
  private byte[] keyType;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] identifier;

  @ProtobufProperty(index = 4, type = BYTES)
  private byte[] signatureId;

  @ProtobufProperty(index = 5, type = BYTES)
  private byte[] signaturePublicKey;

  @ProtobufProperty(index = 6, type = BYTES)
  private byte[] signature;

  @ProtobufProperty(index = 7, type = BYTES)
  private byte[] buildHash;

  @ProtobufProperty(index = 8, type = BYTES)
  private byte[] companion;
}
