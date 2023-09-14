package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.OptionalLong;

/**
 * A model class that represents a message to decline a {@link RequestPaymentMessage}.
 */

public record PaymentInviteMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        PaymentServiceType serviceType,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        OptionalLong expiryTimestamp
) implements PaymentMessage {
    @Override
    public MessageType type() {
        return MessageType.PAYMENT_INVITE;
    }
}