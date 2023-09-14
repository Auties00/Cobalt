package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import it.auties.whatsapp.model.payment.PaymentBackground;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * A model class that represents a message to confirm a {@link RequestPaymentMessage}.
 */
public record SendPaymentMessage(
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<MessageContainer> noteMessage,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        @NonNull
        MessageKey requestMessageKey,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<PaymentBackground> background
) implements PaymentMessage {

    @Override
    public MessageType type() {
        return MessageType.SEND_PAYMENT;
    }
}