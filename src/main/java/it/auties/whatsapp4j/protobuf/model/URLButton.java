package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.message.HighlyStructuredMessage;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class URLButton {
  @JsonProperty(value = "2")
  private HighlyStructuredMessage url;

  @JsonProperty(value = "1")
  private HighlyStructuredMessage displayText;
}
