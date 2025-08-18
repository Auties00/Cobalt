package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;

import java.util.OptionalLong;

/**
 * A model class that represents a message to decline a {@link RequestPaymentMessage}.
 */
@ProtobufMessage(name = "Message.PaymentInviteMessage")
public record PaymentInviteMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        ServiceType serviceType,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        OptionalLong expiryTimestamp
) implements PaymentMessage {
    @Override
    public MessageType type() {
        return MessageType.PAYMENT_INVITE;
    }

    @ProtobufEnum(name = "Message.PaymentInviteMessage.ServiceType")
    public enum ServiceType {
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

        final int index;

        ServiceType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}