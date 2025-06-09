package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.PaymentMessage;

import java.util.Objects;

/**
 * A model class that represents a message to decline a {@link RequestPaymentMessage}.
 */
@ProtobufMessage(name = "Message.DeclinePaymentRequestMessage")
public final class DeclinePaymentRequestMessage implements PaymentMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey key;

    DeclinePaymentRequestMessage(ChatMessageKey key) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
    }

    public ChatMessageKey key() {
        return key;
    }

    @Override
    public Type type() {
        return Type.DECLINE_PAYMENT_REQUEST;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DeclinePaymentRequestMessage that
                && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "DeclinePaymentRequestMessage[" +
                "key=" + key + ']';
    }
}