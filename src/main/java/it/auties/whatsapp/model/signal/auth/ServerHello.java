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
public class ServerHello implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] ephemeral;

  @ProtobufProperty(index = 2, type = BYTES)
  private byte[] staticText;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] payload;
}
