package it.auties.whatsapp.protobuf.message.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.MessageContainer;
import it.auties.whatsapp.protobuf.message.model.PaymentMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage to try to place a {@link PaymentMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newRequestPaymentMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class RequestPaymentMessage implements PaymentMessage {
  /**
   * The currency code for {@link RequestPaymentMessage#amount}.
   * Follows the ISO-4217 Standard.
   * For a list of valid currency codes click <a href="https://en.wikipedia.org/wiki/ISO_4217">here</a>
   */
  @JsonProperty("1")
  private String currency;

  /**
   * The amount of money being paid
   */
  @JsonProperty("2")
  private long amount;

  /**
   * The name of the Whatsapp business account that will receive the money
   */
  @JsonProperty("3")
  private String requestFrom;

  /**
   * The caption message, that is the message below the payment request
   */
  @JsonProperty("4")
  private MessageContainer noteMessage;

  /**
   * The timestamp, that is the endTimeStamp in seconds since {@link java.time.Instant#EPOCH}, for the expiration of this payment request
   */
  @JsonProperty("5")
  private long expiryTimestamp;
}
