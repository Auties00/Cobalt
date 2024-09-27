package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import it.auties.whatsapp.model.payment.PaymentBackground;
import it.auties.whatsapp.model.payment.PaymentMoney;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that represents a message to try to place a {@link PaymentMessage}.
 */
@ProtobufMessage(name = "Message.RequestPaymentMessage")
public record RequestPaymentMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String currency,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        long amount1000,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        Jid requestFrom,
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        Optional<MessageContainer> noteMessage,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
        long expiryTimestampSeconds,
        @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
        PaymentMoney amount,
        @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
        Optional<PaymentBackground> background
) implements PaymentMessage {
    /**
     * Returns when the transaction expires
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> expiryTimestamp() {
        return Clock.parseSeconds(expiryTimestampSeconds);
    }

    @Override
    public MessageType type() {
        return MessageType.REQUEST_PAYMENT;
    }
}
