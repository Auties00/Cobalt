package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a message to decline a {@link RequestPaymentMessage}.
 */
@ProtobufMessage(name = "Message.PaymentInviteMessage")
public final class PaymentInviteMessage implements PaymentMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final ServiceType serviceType;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    final long expiryTimestampSeconds;

    PaymentInviteMessage(ServiceType serviceType, Long expiryTimestampSeconds) {
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType cannot be null");
        this.expiryTimestampSeconds = expiryTimestampSeconds;
    }

    public ServiceType serviceType() {
        return serviceType;
    }

    public long expiryTimestampSeconds() {
        return expiryTimestampSeconds;
    }

    public Optional<ZonedDateTime> expiryTimestamp() {
        return Clock.parseSeconds(expiryTimestampSeconds);
    }

    @Override
    public Type type() {
        return Type.PAYMENT_INVITE;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PaymentInviteMessage that
                && Objects.equals(serviceType, that.serviceType)
                && expiryTimestampSeconds == that.expiryTimestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceType, expiryTimestampSeconds);
    }

    @Override
    public String toString() {
        return "PaymentInviteMessage[" +
                "serviceType=" + serviceType + ", " +
                "expiryTimestamp=" + expiryTimestampSeconds + ']';
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
    }
}