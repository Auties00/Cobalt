package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class RequestPaymentMessage implements Message {
  @JsonProperty(value = "5")
  private long expiryTimestamp;

  @JsonProperty(value = "3")
  private String requestFrom;

  @JsonProperty(value = "2")
  private long amount1000;

  @JsonProperty(value = "1")
  private String currencyCodeIso4217;

  @JsonProperty(value = "4")
  private MessageContainer noteMessage;
}
