package it.auties.whatsapp.model.signal.auth;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class HandshakeMessage
    implements ProtobufMessage {

  @ProtobufProperty(index = 2, type = MESSAGE, implementation = ClientHello.class)
  private ClientHello clientHello;

  @ProtobufProperty(index = 3, type = MESSAGE, implementation = ServerHello.class)
  private ServerHello serverHello;

  @ProtobufProperty(index = 4, type = MESSAGE, implementation = ClientFinish.class)
  private ClientFinish clientFinish;

  public HandshakeMessage(ClientHello clientHello) {
    this.clientHello = clientHello;
  }

  public HandshakeMessage(ClientFinish clientFinish) {
    this.clientFinish = clientFinish;
  }
}
