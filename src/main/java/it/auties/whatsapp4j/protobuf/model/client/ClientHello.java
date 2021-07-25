package it.auties.whatsapp4j.protobuf.model.client;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
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
