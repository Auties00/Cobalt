package it.auties.whatsapp4j.common.protobuf.message.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.protobuf.model.client.ClientFinish;
import it.auties.whatsapp4j.common.protobuf.model.client.ClientHello;
import it.auties.whatsapp4j.common.protobuf.model.server.ServerHello;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

  public HandshakeMessage(ClientFinish clientFinish){
    this.clientFinish = clientFinish;
  }
}
