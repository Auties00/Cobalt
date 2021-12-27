package it.auties.whatsapp.protobuf.signal.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
  @JsonProperty("2")
  @JsonPropertyDescription("ClientHello")
  private ClientHello clientHello;

  @JsonProperty("3")
  @JsonPropertyDescription("ServerHello")
  private ServerHello serverHello;

  @JsonProperty("4")
  @JsonPropertyDescription("ClientFinish")
  private ClientFinish clientFinish;

  public HandshakeMessage(ClientHello clientHello){
    this.clientHello = clientHello;
  }

  public HandshakeMessage(ClientFinish clientFinish){
    this.clientFinish = clientFinish;
  }
}
