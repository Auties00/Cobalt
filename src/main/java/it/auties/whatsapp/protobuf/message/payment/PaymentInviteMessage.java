package it.auties.whatsapp.protobuf.message.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.PaymentMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;


/**
 * A model class that represents a WhatsappMessage to decline a {@link RequestPaymentMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder(builderMethodName = "newPaymentInviteMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class PaymentInviteMessage implements PaymentMessage {
  /**
   * The type of service used for this payment
   */
  @JsonProperty(value = "1")
  private PaymentInviteMessageServiceType serviceType;

  /**
   * The timestamp of expiration for this message
   */
  @JsonProperty(value = "2")
  private long expiryTimestamp;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum PaymentInviteMessageServiceType {
    /**
     * Unknown service provider
     */
    UNKNOWN(0),

    /**
     * Facebook Pay
     */
    FACEBOOK_PAY(1),

    /**
     * Novi
     */
    NOVI(2),

    /**
     * Upi
     */
    UPI(3);

    private final @Getter int index;

    @JsonCreator
    public static PaymentInviteMessageServiceType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
