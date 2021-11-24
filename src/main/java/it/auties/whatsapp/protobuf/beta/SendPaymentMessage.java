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
public class SendPaymentMessage {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("PaymentBackground")
  private PaymentBackground background;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("MessageKey")
  private MessageKey requestMessageKey;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("Message")
  private Message noteMessage;
}
