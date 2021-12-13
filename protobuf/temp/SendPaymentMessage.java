package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SendPaymentMessage {
  @JsonProperty(value = "4")
  @JsonPropertyDescription("PaymentBackground")
  private PaymentBackground background;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("MessageKey")
  private MessageKey requestMessageKey;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("Message")
  private Message noteMessage;
}
