package it.auties.whatsapp.model.message.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT64;


/**
 * A model class that represents a WhatsappMessage to decline a {@link RequestPaymentMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newPaymentInviteMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class PaymentInviteMessage implements PaymentMessage {
  /**
   * The type of service used for this payment
   */
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = PaymentInviteMessageServiceType.class)
  private PaymentInviteMessageServiceType serviceType;

  /**
   * The timestamp of expiration for this message
   */
  @ProtobufProperty(index = 2, type = UINT64)
  private Long expiryTimestamp;

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

    @Getter
    private final int index;

    @JsonCreator
    public static PaymentInviteMessageServiceType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
