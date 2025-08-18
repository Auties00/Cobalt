package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import it.auties.whatsapp.model.payment.PaymentBackground;

import java.util.Optional;

/**
 * A model class that represents a message to confirm a {@link RequestPaymentMessage}.
 */
@ProtobufMessage(name = "Message.SendPaymentMessage")
public record SendPaymentMessage(
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        Optional<MessageContainer> noteMessage,
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        ChatMessageKey requestMessageKey,
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        Optional<PaymentBackground> background
) implements PaymentMessage {

    @Override
    public MessageType type() {
        return MessageType.SEND_PAYMENT;
    }
}