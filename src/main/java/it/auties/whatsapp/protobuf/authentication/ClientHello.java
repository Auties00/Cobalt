package it.auties.whatsapp.protobuf.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ClientHello {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] payload;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] staticText;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] ephemeral;

  public ClientHello(byte @NonNull [] ephemeral){
    this.ephemeral = ephemeral;
  }
}
