package it.auties.whatsapp4j.protobuf.model.server;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ServerHello {
  @JsonProperty(value = "3")
  private byte[] payload;

  @JsonProperty(value = "2")
  private byte[] _static;

  @JsonProperty(value = "1")
  private byte[] ephemeral;
}
