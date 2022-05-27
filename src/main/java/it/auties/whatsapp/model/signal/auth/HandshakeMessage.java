package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class HandshakeMessage implements ProtobufMessage {
  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ClientHello.class)
  private ClientHello clientHello;

  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ServerHello.class)
  private ServerHello serverHello;

  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = ClientFinish.class)
  private ClientFinish clientFinish;

  public HandshakeMessage(ClientHello clientHello){
    this.clientHello = clientHello;
  }

  public HandshakeMessage(ClientFinish clientFinish){
    this.clientFinish = clientFinish;
  }
}
