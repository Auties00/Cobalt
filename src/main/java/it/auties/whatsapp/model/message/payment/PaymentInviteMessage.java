package it.auties.whatsapp.model.message.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.UINT64;


/**
 * A model class that represents a message to decline a {@link RequestPaymentMessage}.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class PaymentInviteMessage
        implements PaymentMessage {
    /**
     * The type of service used for this payment
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = PaymentInviteMessageServiceType.class)
    private PaymentInviteMessageServiceType serviceType;

    /**
     * The timestamp of expiration for this message
     */
    @ProtobufProperty(index = 2, type = UINT64)
    private long expiryTimestamp;

    @Override
    public MessageType type() {
        return MessageType.PAYMENT_INVITE;
    }

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
        public static PaymentInviteMessageServiceType of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
