package it.auties.whatsapp.protobuf.signal.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.signal.auth.ClientFinish;
import it.auties.whatsapp.protobuf.signal.auth.ClientHello;
import it.auties.whatsapp.protobuf.signal.auth.ServerHello;
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
  @JsonProperty(value = "2")
  @JsonPropertyDescription("ClientHello")
  private ClientHello clientHello;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("ServerHello")
  private ServerHello serverHello;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("ClientFinish")
  private ClientFinish clientFinish;

  public HandshakeMessage(ClientHello clientHello){
    this.clientHello = clientHello;
  }

  public HandshakeMessage(ClientFinish clientFinish){
    this.clientFinish = clientFinish;
  }
}
