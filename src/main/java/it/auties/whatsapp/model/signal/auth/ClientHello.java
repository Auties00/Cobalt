package it.auties.whatsapp.model.signal.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ClientHello implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] ephemeral;

  @ProtobufProperty(index = 2, type = BYTES)
  private byte[] staticText;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] payload;

  public ClientHello(byte @NonNull [] ephemeral){
    this.ephemeral = ephemeral;
  }
}
