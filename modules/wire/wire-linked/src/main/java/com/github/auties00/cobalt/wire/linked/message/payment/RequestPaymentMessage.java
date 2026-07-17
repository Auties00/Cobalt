package com.github.auties00.cobalt.wire.linked.message.payment;

import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.payment.Money;
import com.github.auties00.cobalt.wire.linked.payment.PaymentBackground;

import java.time.Instant;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a message asking a contact to pay a specific amount of
 * money.
 *
 * <p>The request carries the amount being asked for, the currency it
 * is denominated in, an optional note shown alongside the payment
 * card, an expiry after which the request can no longer be fulfilled,
 * and optional visual customisation through a {@link PaymentBackground}.
 * Once the recipient acts on the request they may respond with a
 * {@link SendPaymentMessage}, a {@link DeclinePaymentRequestMessage},
 * or no response at all; the sender may also withdraw the request
 * with a {@link CancelPaymentRequestMessage}.
 */
@ProtobufMessage(name = "Message.RequestPaymentMessage")
public final class RequestPaymentMessage implements Message {
    /**
     * The note shown alongside the payment request card.
     *
     * <p>A free-form message (typically text) that the sender attaches
     * to explain what the payment is for.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    LinkedMessageContainer noteMessageContainer;

    /**
     * The ISO 4217 currency code of the legacy fixed-precision amount.
     *
     * <p>Used together with {@link #amount1000} to represent the
     * requested amount for backwards compatibility with older
     * clients that do not understand the {@link Money} field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String currencyCodeIso4217;

    /**
     * The legacy fixed-precision amount, expressed in thousandths of
     * the main currency unit.
     *
     * <p>For example, a value of {@code 1500} paired with a currency
     * code of {@code "USD"} represents {@code 1.500 USD}. This field
     * coexists with {@link #amount} for compatibility with older
     * client versions.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    Long amount1000;

    /**
     * The JID or handle identifying the party the payment is requested
     * from.
     *
     * <p>In group chats this distinguishes which participant is being
     * asked to pay.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String requestFrom;

    /**
     * The instant at which this payment request expires.
     *
     * <p>After this point the request can no longer be honoured and
     * clients should render it as expired.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant expiryTimestamp;

    /**
     * The requested amount as a structured {@link Money} value.
     *
     * <p>This is the modern replacement for the {@link #amount1000}
     * plus {@link #currencyCodeIso4217} pair and carries both the
     * currency and the amount in a single object.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    Money amount;

    /**
     * The optional visual background rendered behind the payment
     * request card.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    PaymentBackground background;


    /**
     * Constructs a new payment request message with the given fields.
     *
     * @param noteMessageContainer the note shown alongside the request
     *                             card, may be {@code null}
     * @param currencyCodeIso4217  the legacy ISO 4217 currency code,
     *                             may be {@code null}
     * @param amount1000           the legacy amount expressed in
     *                             thousandths of the main unit, may
     *                             be {@code null}
     * @param requestFrom          the party the payment is requested
     *                             from, may be {@code null}
     * @param expiryTimestamp      the instant at which the request
     *                             expires, may be {@code null}
     * @param amount               the structured {@link Money} amount,
     *                             may be {@code null}
     * @param background           the optional visual background,
     *                             may be {@code null}
     */
    RequestPaymentMessage(LinkedMessageContainer noteMessageContainer, String currencyCodeIso4217, Long amount1000, String requestFrom, Instant expiryTimestamp, Money amount, PaymentBackground background) {
        this.noteMessageContainer = noteMessageContainer;
        this.currencyCodeIso4217 = currencyCodeIso4217;
        this.amount1000 = amount1000;
        this.requestFrom = requestFrom;
        this.expiryTimestamp = expiryTimestamp;
        this.amount = amount;
        this.background = background;
    }

    /**
     * Returns the note shown alongside the payment request card.
     *
     * @return an {@link Optional} containing the note
     *         {@link LinkedMessageContainer}, or {@link Optional#empty()}
     *         if no note was attached
     */
    public Optional<LinkedMessageContainer> noteMessage() {
        return Optional.ofNullable(noteMessageContainer);
    }

    /**
     * Returns the legacy ISO 4217 currency code.
     *
     * @return an {@link Optional} containing the currency code, or
     *         {@link Optional#empty()} if not set
     */
    public Optional<String> currencyCodeIso4217() {
        return Optional.ofNullable(currencyCodeIso4217);
    }

    /**
     * Returns the legacy amount expressed in thousandths of the main
     * currency unit.
     *
     * @return an {@link OptionalLong} containing the amount, or
     *         {@link OptionalLong#empty()} if not set
     */
    public OptionalLong amount1000() {
        return amount1000 == null ? OptionalLong.empty() : OptionalLong.of(amount1000);
    }

    /**
     * Returns the identifier of the party the payment is requested
     * from.
     *
     * @return an {@link Optional} containing the handle or JID, or
     *         {@link Optional#empty()} if not set
     */
    public Optional<String> requestFrom() {
        return Optional.ofNullable(requestFrom);
    }

    /**
     * Returns the instant at which this request expires.
     *
     * @return an {@link Optional} containing the expiry
     *         {@link Instant}, or {@link Optional#empty()} if not set
     */
    public Optional<Instant> expiryTimestamp() {
        return Optional.ofNullable(expiryTimestamp);
    }

    /**
     * Returns the requested amount as a structured {@link Money} value.
     *
     * @return an {@link Optional} containing the amount, or
     *         {@link Optional#empty()} if not set
     */
    public Optional<Money> amount() {
        return Optional.ofNullable(amount);
    }

    /**
     * Returns the visual background rendered behind the request card.
     *
     * @return an {@link Optional} containing the
     *         {@link PaymentBackground}, or {@link Optional#empty()}
     *         if none was attached
     */
    public Optional<PaymentBackground> background() {
        return Optional.ofNullable(background);
    }

    /**
     * Sets the note shown alongside the payment request card.
     *
     * @param noteMessageContainer the note container, may be
     *                             {@code null}
     */
    public void setNoteMessage(LinkedMessageContainer noteMessageContainer) {
        this.noteMessageContainer = noteMessageContainer;
    }

    /**
     * Sets the legacy ISO 4217 currency code.
     *
     * @param currencyCodeIso4217 the currency code, may be
     *                            {@code null}
     */
    public void setCurrencyCodeIso4217(String currencyCodeIso4217) {
        this.currencyCodeIso4217 = currencyCodeIso4217;
    }

    /**
     * Sets the legacy amount expressed in thousandths of the main
     * currency unit.
     *
     * @param amount1000 the amount, may be {@code null}
     */
    public void setAmount1000(Long amount1000) {
        this.amount1000 = amount1000;
    }

    /**
     * Sets the identifier of the party the payment is requested from.
     *
     * @param requestFrom the handle or JID, may be {@code null}
     */
    public void setRequestFrom(String requestFrom) {
        this.requestFrom = requestFrom;
    }

    /**
     * Sets the instant at which this request expires.
     *
     * @param expiryTimestamp the expiry instant, may be {@code null}
     */
    public void setExpiryTimestamp(Instant expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
    }

    /**
     * Sets the requested amount as a structured {@link Money} value.
     *
     * @param amount the money amount, may be {@code null}
     */
    public void setAmount(Money amount) {
        this.amount = amount;
    }

    /**
     * Sets the visual background rendered behind the request card.
     *
     * @param background the background, may be {@code null}
     */
    public void setBackground(PaymentBackground background) {
        this.background = background;
    }
}
