package it.auties.whatsapp4j.common.protobuf.model.client;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ClientHello {
  @JsonProperty(value = "3")
  private byte[] payload;

  @JsonProperty(value = "2")
  private byte[] _static;

  @JsonProperty(value = "1")
  private byte[] ephemeral;

  public ClientHello(byte[] ephemeral){
    this.ephemeral = ephemeral;
  }
}
