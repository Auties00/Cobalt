package com.github.auties00.cobalt.wire.linked.payment;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A notification describing a payment transaction that arrived without a corresponding
 * chat message in the user's local history.
 *
 * <p>Orphan payment notifications are delivered by the server when the payment backend
 * records a transaction event for which the original payment message is missing from
 * the client, for example because the message was deleted, never received, or is part
 * of a transaction lifecycle that began before the current session was initialized.
 * The notification carries enough information for the client to display or reconcile
 * the payment without needing to correlate it to an existing chat entry.
 *
 * <p>Each notification identifies the original payment by {@link #messageId() messageId}
 * and describes the transaction through its currency, amount, type, status, and the
 * timestamp at which the underlying transaction occurred.
 */
@ProtobufMessage
public final class OrphanPaymentNotification {
    /**
     * The identifier of the original payment message that this notification relates
     * to. This id allows the client to correlate the orphan notification with any
     * message that may later be synced.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String messageId;

    /**
     * The JID of the user who was the intended receiver of the payment.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    Jid receiverJid;

    /**
     * The ISO 4217 currency code of the payment, such as {@code "INR"} or
     * {@code "BRL"}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String currency;

    /**
     * The payment amount expressed in thousandths of the major currency unit. For
     * example, a value of {@code 5000} represents {@code 5.00} in the given
     * currency.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    Long amount1000;

    /**
     * A textual descriptor of the transaction type, such as a payment, refund, or
     * reversal, as reported by the payment backend.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String transactionType;

    /**
     * A textual descriptor of the transaction status, reflecting the latest state
     * reported by the payment backend.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String status;

    /**
     * The epoch-second timestamp indicating when the underlying transaction was
     * executed on the payment backend.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT64)
    Long transactionTimestamp;

    /**
     * Constructs a new {@code OrphanPaymentNotification} with the given properties.
     *
     * @param messageId            the identifier of the original payment message
     * @param receiverJid          the JID of the payment receiver
     * @param currency             the ISO 4217 currency code
     * @param amount1000           the amount in thousandths of the major currency unit
     * @param transactionType      the textual transaction type descriptor
     * @param status               the textual status descriptor
     * @param transactionTimestamp the epoch-second timestamp of the transaction
     * @throws NullPointerException if {@code messageId} is {@code null}
     */
    OrphanPaymentNotification(String messageId, Jid receiverJid, String currency, Long amount1000, String transactionType, String status, Long transactionTimestamp) {
        this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
        this.receiverJid = receiverJid;
        this.currency = currency;
        this.amount1000 = amount1000;
        this.transactionType = transactionType;
        this.status = status;
        this.transactionTimestamp = transactionTimestamp;
    }

    /**
     * Returns the identifier of the original payment message that this notification
     * relates to.
     *
     * @return the message identifier
     */
    public String messageId() {
        return messageId;
    }

    /**
     * Returns the JID of the intended receiver of the payment.
     *
     * @return an {@code Optional} containing the receiver JID, or empty if not set
     */
    public Optional<Jid> receiverJid() {
        return Optional.ofNullable(receiverJid);
    }

    /**
     * Returns the ISO 4217 currency code of the payment.
     *
     * @return an {@code Optional} containing the currency code, or empty if not set
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the payment amount in thousandths of the major currency unit.
     *
     * @return an {@code Optional} containing the amount, or empty if not set
     */
    public Optional<Long> amount1000() {
        return Optional.ofNullable(amount1000);
    }

    /**
     * Returns the textual transaction type descriptor.
     *
     * @return an {@code Optional} containing the transaction type, or empty if not set
     */
    public Optional<String> transactionType() {
        return Optional.ofNullable(transactionType);
    }

    /**
     * Returns the textual transaction status descriptor.
     *
     * @return an {@code Optional} containing the status, or empty if not set
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the timestamp at which the underlying transaction was executed.
     *
     * @return an {@code Optional} containing the transaction timestamp as epoch
     *         seconds, or empty if not set
     */
    public Optional<Long> transactionTimestamp() {
        return Optional.ofNullable(transactionTimestamp);
    }

    /**
     * Sets the JID of the intended receiver of the payment.
     *
     * @param receiverJid the receiver JID
     */
    public void setReceiverJid(Jid receiverJid) {
        this.receiverJid = receiverJid;
    }

    /**
     * Sets the ISO 4217 currency code of the payment.
     *
     * @param currency the currency code
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Sets the payment amount in thousandths of the major currency unit.
     *
     * @param amount1000 the amount
     */
    public void setAmount1000(Long amount1000) {
        this.amount1000 = amount1000;
    }

    /**
     * Sets the textual transaction type descriptor.
     *
     * @param transactionType the transaction type
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * Sets the textual transaction status descriptor.
     *
     * @param status the status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Sets the epoch-second timestamp at which the underlying transaction was
     * executed.
     *
     * @param transactionTimestamp the transaction timestamp as epoch seconds
     */
    public void setTransactionTimestamp(Long transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }
}
