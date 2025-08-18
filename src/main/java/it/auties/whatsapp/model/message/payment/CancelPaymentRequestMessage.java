package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;

/**
 * A model class that represents a message that cancels a {@link RequestPaymentMessage}.
 */
@ProtobufMessage(name = "Message.CancelPaymentRequestMessage")
public record CancelPaymentRequestMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        ChatMessageKey key
) implements PaymentMessage {
    @Override
    public MessageType type() {
        return MessageType.CANCEL_PAYMENT_REQUEST;
    }
}
