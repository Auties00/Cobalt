package it.auties.whatsapp4j.protobuf.model.action;

import com.fasterxml.jackson.annotation.*;
import it.auties.whatsapp4j.protobuf.message.model.MessageKey;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncActionMessage {
  @JsonProperty(value = "2")
  private long timestamp;

  @JsonProperty(value = "1")
  private MessageKey key;
}
