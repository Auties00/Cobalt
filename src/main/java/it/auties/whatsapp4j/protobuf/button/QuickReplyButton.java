package it.auties.whatsapp4j.protobuf.button;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.message.HighlyStructuredMessage;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class QuickReplyButton {
  @JsonProperty(value = "2")
  private String id;

  @JsonProperty(value = "1")
  private HighlyStructuredMessage displayText;
}
