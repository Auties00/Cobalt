package it.auties.whatsapp.protobuf.message.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.authentication.ClientFinish;
import it.auties.whatsapp.protobuf.authentication.ClientHello;
import it.auties.whatsapp.protobuf.authentication.ServerHello;
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
  @JsonPropertyDescription("ClientFinish")
  private ClientFinish clientFinish;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("ServerHello")
  private ServerHello serverHello;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("ClientHello")
  private ClientHello clientHello;

  public HandshakeMessage(ClientHello clientHello){
    this.clientHello = clientHello;
  }

  public HandshakeMessage(ClientFinish clientFinish){
    this.clientFinish = clientFinish;
  }
}
