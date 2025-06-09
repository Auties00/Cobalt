package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import it.auties.whatsapp.model.payment.PaymentBackground;
import it.auties.whatsapp.model.payment.PaymentMoney;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a message to try to place a {@link PaymentMessage}.
 */
@ProtobufMessage(name = "Message.RequestPaymentMessage")
public final class RequestPaymentMessage implements PaymentMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String currency;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    final long amount1000;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid requestFrom;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final MessageContainer noteMessage;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    final long expiryTimestampSeconds;

    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final PaymentMoney amount;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final PaymentBackground background;

    RequestPaymentMessage(String currency, long amount1000, Jid requestFrom, MessageContainer noteMessage, long expiryTimestampSeconds, PaymentMoney amount, PaymentBackground background) {
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
        this.amount1000 = amount1000;
        this.requestFrom = Objects.requireNonNull(requestFrom, "requestFrom cannot be null");
        this.noteMessage = noteMessage;
        this.expiryTimestampSeconds = expiryTimestampSeconds;
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.background = background;
    }

    public String currency() {
        return currency;
    }

    public long amount1000() {
        return amount1000;
    }

    public Jid requestFrom() {
        return requestFrom;
    }

    public Optional<MessageContainer> noteMessage() {
        return Optional.ofNullable(noteMessage);
    }

    public long expiryTimestampSeconds() {
        return expiryTimestampSeconds;
    }

    public PaymentMoney amount() {
        return amount;
    }

    public Optional<PaymentBackground> background() {
        return Optional.ofNullable(background);
    }

    /**
     * Returns when the transaction expires
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> expiryTimestamp() {
        return Clock.parseSeconds(expiryTimestampSeconds);
    }

    @Override
    public Type type() {
        return Type.REQUEST_PAYMENT;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RequestPaymentMessage that
                && Objects.equals(currency, that.currency)
                && amount1000 == that.amount1000
                && Objects.equals(requestFrom, that.requestFrom)
                && Objects.equals(noteMessage, that.noteMessage)
                && expiryTimestampSeconds == that.expiryTimestampSeconds
                && Objects.equals(amount, that.amount)
                && Objects.equals(background, that.background);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amount1000, requestFrom, noteMessage, expiryTimestampSeconds, amount, background);
    }

    @Override
    public String toString() {
        return "RequestPaymentMessage[" +
                "currency=" + currency + ", " +
                "amount1000=" + amount1000 + ", " +
                "requestFrom=" + requestFrom + ", " +
                "noteMessage=" + noteMessage + ", " +
                "expiryTimestampSeconds=" + expiryTimestampSeconds + ", " +
                "amount=" + amount + ", " +
                "background=" + background + ']';
    }
}