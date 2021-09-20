package it.auties.whatsapp4j.common.protobuf.model.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(fluent = true)
@Data
public class ServerHello {
  @JsonProperty(value = "3")
  private byte[] payload;

  @JsonProperty(value = "2")
  private byte[] staticText;

  @JsonProperty(value = "1")
  private byte[] ephemeral;

  public byte @NonNull [] completeMessage(){
    return BinaryArray.forArray(ephemeral)
            .append(staticText)
            .append(payload)
            .data();
  }
}
