package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class SendPaymentMessage implements Message {
  @JsonProperty(value = "3")
  private MessageKey requestMessageKey;

  @JsonProperty(value = "2")
  private MessageContainer noteMessage;
}
