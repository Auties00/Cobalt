package com.github.auties00.cobalt.wire.linked.message.payment;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.payment.PaymentBackground;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a message confirming that a payment has been sent to a
 * contact.
 *
 * <p>A send payment message is produced either spontaneously or in
 * response to a previously received {@link RequestPaymentMessage}.
 * When replying to a request, the {@code requestMessageKey} links this
 * message to the original request so that both chat UIs can mark it
 * as fulfilled. The message also carries an optional note shown next
 * to the payment card, an optional visual background and the
 * provider-specific transaction data describing the underlying
 * financial operation.
 */
@ProtobufMessage(name = "Message.SendPaymentMessage")
public final class SendPaymentMessage implements Message {
    /**
     * The note shown alongside the payment card.
     *
     * <p>A free-form message (typically text) that the sender attaches
     * to the payment, for example a thank-you note or a description
     * of what the payment is for.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    LinkedMessageContainer noteMessageContainer;

    /**
     * The key of the {@link RequestPaymentMessage} this send payment
     * message responds to, if any.
     *
     * <p>When present, allows both clients to correlate the payment
     * with the original request and mark it as fulfilled.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    MessageKey requestMessageKey;

    /**
     * The optional visual background rendered behind the payment card.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    PaymentBackground background;

    /**
     * The provider-specific transaction data describing the underlying
     * financial operation.
     *
     * <p>An opaque string whose format depends on the payment
     * provider; clients forward it to the provider to reconcile the
     * payment.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String transactionData;


    /**
     * Constructs a new send payment message with the given fields.
     *
     * @param noteMessageContainer the note shown alongside the
     *                             payment card, may be {@code null}
     * @param requestMessageKey    the key of the request this payment
     *                             responds to, may be {@code null}
     * @param background           the optional visual background,
     *                             may be {@code null}
     * @param transactionData      the provider-specific transaction
     *                             data, may be {@code null}
     */
    SendPaymentMessage(LinkedMessageContainer noteMessageContainer, MessageKey requestMessageKey, PaymentBackground background, String transactionData) {
        this.noteMessageContainer = noteMessageContainer;
        this.requestMessageKey = requestMessageKey;
        this.background = background;
        this.transactionData = transactionData;
    }

    /**
     * Returns the note shown alongside the payment card.
     *
     * @return an {@link Optional} containing the note
     *         {@link LinkedMessageContainer}, or {@link Optional#empty()}
     *         if no note was attached
     */
    public Optional<LinkedMessageContainer> noteMessage() {
        return Optional.ofNullable(noteMessageContainer);
    }

    /**
     * Returns the key of the request message this payment responds to.
     *
     * @return an {@link Optional} containing the request
     *         {@link MessageKey}, or {@link Optional#empty()} if this
     *         payment was not sent in response to a request
     */
    public Optional<MessageKey> requestMessageKey() {
        return Optional.ofNullable(requestMessageKey);
    }

    /**
     * Returns the visual background rendered behind the payment card.
     *
     * @return an {@link Optional} containing the
     *         {@link PaymentBackground}, or {@link Optional#empty()}
     *         if none was attached
     */
    public Optional<PaymentBackground> background() {
        return Optional.ofNullable(background);
    }

    /**
     * Returns the provider-specific transaction data.
     *
     * @return an {@link Optional} containing the opaque transaction
     *         data, or {@link Optional#empty()} if not set
     */
    public Optional<String> transactionData() {
        return Optional.ofNullable(transactionData);
    }

    /**
     * Sets the note shown alongside the payment card.
     *
     * @param noteMessageContainer the note container, may be
     *                             {@code null}
     */
    public void setNoteMessage(LinkedMessageContainer noteMessageContainer) {
        this.noteMessageContainer = noteMessageContainer;
    }

    /**
     * Sets the key of the request message this payment responds to.
     *
     * @param requestMessageKey the request message key, may be
     *                          {@code null}
     */
    public void setRequestMessageKey(MessageKey requestMessageKey) {
        this.requestMessageKey = requestMessageKey;
    }

    /**
     * Sets the visual background rendered behind the payment card.
     *
     * @param background the background, may be {@code null}
     */
    public void setBackground(PaymentBackground background) {
        this.background = background;
    }

    /**
     * Sets the provider-specific transaction data.
     *
     * @param transactionData the opaque transaction data, may be
     *                        {@code null}
     */
    public void setTransactionData(String transactionData) {
        this.transactionData = transactionData;
    }
}
