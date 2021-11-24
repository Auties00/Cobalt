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
public class RequestPaymentMessage {

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("PaymentBackground")
  private PaymentBackground background;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("Money")
  private Money amount;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("int64")
  private long expiryTimestamp;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String requestFrom;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("uint64")
  private long amount1000;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String currencyCodeIso4217;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("Message")
  private Message noteMessage;
}
