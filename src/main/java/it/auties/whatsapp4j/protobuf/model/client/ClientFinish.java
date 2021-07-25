package it.auties.whatsapp4j.protobuf.model.client;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ClientFinish {
  @JsonProperty(value = "2")
  private byte[] payload;

  @JsonProperty(value = "1")
  private byte[] _static;
}
