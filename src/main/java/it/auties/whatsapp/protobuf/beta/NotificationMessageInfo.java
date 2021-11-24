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
public class NotificationMessageInfo {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String participant;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint64")
  private long messageTimestamp;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("Message")
  private Message message;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("MessageKey")
  private MessageKey key;
}
