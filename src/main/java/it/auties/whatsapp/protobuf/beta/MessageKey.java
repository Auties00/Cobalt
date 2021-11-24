package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class MessageKey {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String participant;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String id;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("bool")
  private boolean fromMe;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String remoteJid;
}
