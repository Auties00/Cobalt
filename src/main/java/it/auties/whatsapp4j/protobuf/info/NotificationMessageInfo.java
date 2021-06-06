package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.message.MessageContainer;
import it.auties.whatsapp4j.protobuf.message.MessageKey;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class NotificationMessageInfo {
  @JsonProperty(value = "4")
  private String participant;

  @JsonProperty(value = "3")
  private long messageTimestamp;

  @JsonProperty(value = "2")
  private MessageContainer message;

  @JsonProperty(value = "1")
  private MessageKey key;
}
