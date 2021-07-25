package it.auties.whatsapp4j.protobuf.message.server;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.model.client.ClientFinish;
import it.auties.whatsapp4j.protobuf.model.client.ClientHello;
import it.auties.whatsapp4j.protobuf.model.server.ServerHello;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HandshakeMessage {
  @JsonProperty(value = "4")
  private ClientFinish clientFinish;

  @JsonProperty(value = "3")
  private ServerHello serverHello;

  @JsonProperty(value = "2")
  private ClientHello clientHello;

  public HandshakeMessage(ClientHello clientHello){
    this.clientHello = clientHello;
  }
}
