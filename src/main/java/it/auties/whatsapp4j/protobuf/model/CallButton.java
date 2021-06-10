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
public class CallButton {
  @JsonProperty(value = "2")
  private HighlyStructuredMessage phoneNumber;

  @JsonProperty(value = "1")
  private HighlyStructuredMessage displayText;
}
