package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import it.auties.whatsapp.model.payment.PaymentBackground;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a message to confirm a {@link RequestPaymentMessage}.
 */
@ProtobufMessage(name = "Message.SendPaymentMessage")
public final class SendPaymentMessage implements PaymentMessage {
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final MessageContainer noteMessage;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final ChatMessageKey requestMessageKey;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final PaymentBackground background;

    SendPaymentMessage(MessageContainer noteMessage, ChatMessageKey requestMessageKey, PaymentBackground background) {
        this.noteMessage = noteMessage;
        this.requestMessageKey = Objects.requireNonNull(requestMessageKey, "requestMessageKey cannot be null");
        this.background = background;
    }

    public Optional<MessageContainer> noteMessage() {
        return Optional.ofNullable(noteMessage);
    }

    public ChatMessageKey requestMessageKey() {
        return requestMessageKey;
    }

    public Optional<PaymentBackground> background() {
        return Optional.ofNullable(background);
    }

    @Override
    public Type type() {
        return Type.SEND_PAYMENT;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SendPaymentMessage that
                && Objects.equals(noteMessage, that.noteMessage)
                && Objects.equals(requestMessageKey, that.requestMessageKey)
                && Objects.equals(background, that.background);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteMessage, requestMessageKey, background);
    }

    @Override
    public String toString() {
        return "SendPaymentMessage[" +
                "noteMessage=" + noteMessage + ", " +
                "requestMessageKey=" + requestMessageKey + ", " +
                "background=" + background + ']';
    }
}