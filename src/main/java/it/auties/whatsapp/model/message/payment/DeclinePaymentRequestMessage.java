package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;

/**
 * A model class that represents a message to decline a {@link RequestPaymentMessage}.
 */
@ProtobufMessage(name = "Message.DeclinePaymentRequestMessage")
public record DeclinePaymentRequestMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        ChatMessageKey key
) implements PaymentMessage {
    @Override
    public MessageType type() {
        return MessageType.DECLINE_PAYMENT_REQUEST;
    }
}
