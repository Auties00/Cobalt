package it.auties.whatsapp4j.protobuf.message;

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
  @JsonProperty(value = "4")
  private String participant;

  @JsonProperty(value = "3")
  private String id;

  @JsonProperty(value = "2")
  private boolean fromMe;

  @JsonProperty(value = "1")
  private String remoteJid;
}
