package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.PaymentMessage;

import java.util.Objects;

/**
 * A model class that represents a message that cancels a {@link RequestPaymentMessage}.
 */
@ProtobufMessage(name = "Message.CancelPaymentRequestMessage")
public final class CancelPaymentRequestMessage implements PaymentMessage {

    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey key;

    CancelPaymentRequestMessage(ChatMessageKey key) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
    }

    public ChatMessageKey key() {
        return key;
    }

    @Override
    public Type type() {
        return Type.CANCEL_PAYMENT_REQUEST;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CancelPaymentRequestMessage that
                && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "CancelPaymentRequestMessage[" +
                "key=" + key +
                ']';
    }
}