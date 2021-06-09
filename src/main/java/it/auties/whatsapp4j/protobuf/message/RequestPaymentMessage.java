package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent to a WhatsappBusiness account to try to place an order.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class RequestPaymentMessage implements Message {
  /**
   * The timestamp, that is the time in seconds since {@link java.time.Instant#EPOCH}, for the expiration of this payment request
   */
  @JsonProperty(value = "5")
  private long expiryTimestamp;

  /**
   * The name of the Whatsapp business account that will receive the money
   */
  @JsonProperty(value = "3")
  private String requestFrom;

  /**
   * The amount of money being paid
   */
  @JsonProperty(value = "2")
  private long amount1000;

  /**
   * The currency code for {@link RequestPaymentMessage#amount1000}.
   * For a list of valid currency codes click <a href="https://en.wikipedia.org/wiki/ISO_4217">here</a>
   */
  @JsonProperty(value = "1")
  private String currencyCodeIso4217;

  /**
   * The caption message, that is the message below the payment request
   */
  @JsonProperty(value = "4")
  private MessageContainer noteMessage;
}
