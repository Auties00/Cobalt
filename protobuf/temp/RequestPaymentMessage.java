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
public class RequestPaymentMessage {
  @JsonProperty(value = "7")
  @JsonPropertyDescription("PaymentBackground")
  private PaymentBackground background;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("Money")
  private Money amount;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("int64")
  private long expiryTimestamp;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  private String requestFrom;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("uint64")
  private long amount1000;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String currencyCodeIso4217;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("Message")
  private Message noteMessage;
}
