package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ReactionMessage {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("int64")
  private long senderTimestampMs;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String groupingKey;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String text;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("MessageKey")
  private MessageKey key;
}
