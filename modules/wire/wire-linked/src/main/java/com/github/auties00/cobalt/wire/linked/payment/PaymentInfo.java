package com.github.auties00.cobalt.wire.linked.payment;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Metadata describing the state and financial details of an in-app payment
 * transaction attached to a chat message.
 *
 * <p>A {@code PaymentInfo} instance is present on every message that participates in
 * the payments lifecycle, including payment requests, sent payments, and server-side
 * payment notification stubs. It carries the monetary amount in thousandths of the
 * major currency unit, the JID of the payment receiver, the high-level
 * {@link Status} visible to users, and the fine-grained {@link TxnStatus} that
 * tracks the internal transaction state machine.
 *
 * <p>The {@link #currencyDeprecated() currencyDeprecated} field is an older
 * enum-based currency identifier that has been superseded by the string-based
 * {@link #currency() currency} field, which carries a standard ISO 4217 code. The
 * structured {@link Money} fields {@link #primaryAmount() primaryAmount} and
 * {@link #exchangeAmount() exchangeAmount} were introduced alongside the Novi
 * cryptocurrency wallet integration and provide explicit value-plus-offset
 * semantics.
 */
@ProtobufMessage(name = "PaymentInfo")
public final class PaymentInfo {
    /**
     * The deprecated enum-based currency identifier. This field has been superseded
     * by the string-based {@link #currency() currency} field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    Currency currencyDeprecated;

    /**
     * The payment amount expressed in thousandths of the major currency unit. For
     * example, a value of {@code 5000} represents {@code 5.00} in the given currency.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    Long amount1000;

    /**
     * The JID of the user who receives the payment.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    Jid receiverJid;

    /**
     * The high-level payment status visible to users, such as {@link Status#PROCESSING},
     * {@link Status#COMPLETE}, or {@link Status#EXPIRED}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    Status status;

    /**
     * The epoch-second timestamp of when the payment transaction was executed.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    Long transactionTimestamp;

    /**
     * The message key of the original payment request message that this payment
     * fulfills, if applicable.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    MessageKey requestMessageKey;

    /**
     * The epoch-second timestamp after which this payment request expires and can
     * no longer be fulfilled.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
    Long expiryTimestamp;

    /**
     * Whether this payment message uses a future-proofed format that older client
     * versions cannot fully render. When {@code true}, compatible clients may
     * display the message as a placeholder prompting the user to open the app on
     * their phone to see the full content.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    Boolean futureproofed;

    /**
     * The ISO 4217 currency code for this payment, such as {@code "INR"} or
     * {@code "BRL"}. This field supersedes the older {@link #currencyDeprecated()}
     * enum.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String currency;

    /**
     * The fine-grained internal transaction status that tracks the payment through
     * the server-side state machine, from initialization through completion, failure,
     * or reversal.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    TxnStatus txnStatus;

    /**
     * Whether this payment should be displayed using the Novi fiat currency format.
     * Novi was Meta's cryptocurrency wallet, and this flag indicates that the amount
     * values should be interpreted and formatted using Novi-specific rules.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    Boolean useNoviFiatFormat;

    /**
     * The primary monetary amount of this payment, expressed as a structured
     * {@link Money} value with explicit offset semantics.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    Money primaryAmount;

    /**
     * The exchange monetary amount when the payment involves a currency conversion.
     * This represents the amount in the receiver's currency when it differs from the
     * sender's currency.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    Money exchangeAmount;

    /**
     * Constructs a new {@code PaymentInfo} with the given properties.
     *
     * @param currencyDeprecated the deprecated enum-based currency
     * @param amount1000         the amount in thousandths of the major currency unit
     * @param receiverJid        the payment receiver JID
     * @param status             the high-level payment status
     * @param transactionTimestamp the epoch-second transaction timestamp
     * @param requestMessageKey  the key of the original request message
     * @param expiryTimestamp     the epoch-second expiry timestamp
     * @param futureproofed      whether the message uses a future-proofed format
     * @param currency           the ISO 4217 currency code
     * @param txnStatus          the fine-grained transaction status
     * @param useNoviFiatFormat  whether to use Novi fiat formatting
     * @param primaryAmount      the primary monetary amount
     * @param exchangeAmount     the exchange monetary amount
     */
    PaymentInfo(Currency currencyDeprecated, Long amount1000, Jid receiverJid, Status status, Long transactionTimestamp, MessageKey requestMessageKey, Long expiryTimestamp, Boolean futureproofed, String currency, TxnStatus txnStatus, Boolean useNoviFiatFormat, Money primaryAmount, Money exchangeAmount) {
        this.currencyDeprecated = currencyDeprecated;
        this.amount1000 = amount1000;
        this.receiverJid = receiverJid;
        this.status = status;
        this.transactionTimestamp = transactionTimestamp;
        this.requestMessageKey = requestMessageKey;
        this.expiryTimestamp = expiryTimestamp;
        this.futureproofed = futureproofed;
        this.currency = currency;
        this.txnStatus = txnStatus;
        this.useNoviFiatFormat = useNoviFiatFormat;
        this.primaryAmount = primaryAmount;
        this.exchangeAmount = exchangeAmount;
    }

    /**
     * Returns the deprecated enum-based currency identifier.
     *
     * @return an {@code Optional} containing the currency enum value, or empty if
     *         not set
     */
    public Optional<Currency> currencyDeprecated() {
        return Optional.ofNullable(currencyDeprecated);
    }

    /**
     * Returns the payment amount in thousandths of the major currency unit.
     *
     * @return an {@code OptionalLong} containing the amount, or empty if not set
     */
    public OptionalLong amount1000() {
        return amount1000 == null ? OptionalLong.empty() : OptionalLong.of(amount1000);
    }

    /**
     * Returns the JID of the payment receiver.
     *
     * @return an {@code Optional} containing the receiver JID, or empty if not set
     */
    public Optional<Jid> receiverJid() {
        return Optional.ofNullable(receiverJid);
    }

    /**
     * Returns the high-level payment status.
     *
     * @return an {@code Optional} containing the status, or empty if not set
     */
    public Optional<Status> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the timestamp of when the payment transaction was executed, as an
     * {@link Instant}.
     *
     * @return an {@code Optional} containing the transaction timestamp, or empty
     *         if not set
     */
    public Optional<Instant> transactionTimestamp() {
        return Optional.ofNullable(transactionTimestamp)
                .map(Instant::ofEpochSecond);
    }

    /**
     * Returns the message key of the original payment request that this payment
     * fulfills.
     *
     * @return an {@code Optional} containing the request message key, or empty if
     *         not set
     */
    public Optional<MessageKey> requestMessageKey() {
        return Optional.ofNullable(requestMessageKey);
    }

    /**
     * Returns the timestamp after which this payment request expires, as an
     * {@link Instant}.
     *
     * @return an {@code Optional} containing the expiry timestamp, or empty if not set
     */
    public Optional<Instant> expiryTimestamp() {
        return Optional.ofNullable(expiryTimestamp)
                .map(Instant::ofEpochSecond);
    }

    /**
     * Returns whether this payment message uses a future-proofed format.
     *
     * @return {@code true} if the message is future-proofed, {@code false} otherwise
     */
    public boolean futureproofed() {
        return futureproofed != null && futureproofed;
    }

    /**
     * Returns the ISO 4217 currency code for this payment.
     *
     * @return an {@code Optional} containing the currency code, or empty if not set
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the fine-grained internal transaction status.
     *
     * @return an {@code Optional} containing the transaction status, or empty if
     *         not set
     */
    public Optional<TxnStatus> txnStatus() {
        return Optional.ofNullable(txnStatus);
    }

    /**
     * Returns whether this payment should use the Novi fiat currency format.
     *
     * @return {@code true} if Novi fiat formatting should be used, {@code false}
     *         otherwise
     */
    public boolean useNoviFiatFormat() {
        return useNoviFiatFormat != null && useNoviFiatFormat;
    }

    /**
     * Returns the primary monetary amount of this payment.
     *
     * @return an {@code Optional} containing the primary amount, or empty if not set
     */
    public Optional<Money> primaryAmount() {
        return Optional.ofNullable(primaryAmount);
    }

    /**
     * Returns the exchange monetary amount when a currency conversion is involved.
     *
     * @return an {@code Optional} containing the exchange amount, or empty if not set
     */
    public Optional<Money> exchangeAmount() {
        return Optional.ofNullable(exchangeAmount);
    }

    /**
     * Sets the deprecated enum-based currency identifier.
     *
     * @param currencyDeprecated the currency enum value
     */
    public void setCurrencyDeprecated(Currency currencyDeprecated) {
        this.currencyDeprecated = currencyDeprecated;
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
     * Sets the JID of the payment receiver.
     *
     * @param receiverJid the receiver JID
     */
    public void setReceiverJid(Jid receiverJid) {
        this.receiverJid = receiverJid;
    }

    /**
     * Sets the high-level payment status.
     *
     * @param status the payment status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Sets the epoch-second timestamp of when the transaction was executed.
     *
     * @param transactionTimestamp the timestamp as epoch seconds
     */
    public void setTransactionTimestamp(Long transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    /**
     * Sets the message key of the original payment request.
     *
     * @param requestMessageKey the request message key
     */
    public void setRequestMessageKey(MessageKey requestMessageKey) {
        this.requestMessageKey = requestMessageKey;
    }

    /**
     * Sets the epoch-second timestamp after which this payment request expires.
     *
     * @param expiryTimestamp the expiry timestamp as epoch seconds
     */
    public void setExpiryTimestamp(Long expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
    }

    /**
     * Sets whether this payment message uses a future-proofed format.
     *
     * @param futureproofed {@code true} for future-proofed, {@code false} otherwise
     */
    public void setFutureproofed(Boolean futureproofed) {
        this.futureproofed = futureproofed;
    }

    /**
     * Sets the ISO 4217 currency code.
     *
     * @param currency the currency code
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Sets the fine-grained internal transaction status.
     *
     * @param txnStatus the transaction status
     */
    public void setTxnStatus(TxnStatus txnStatus) {
        this.txnStatus = txnStatus;
    }

    /**
     * Sets whether this payment should use the Novi fiat currency format.
     *
     * @param useNoviFiatFormat {@code true} for Novi formatting, {@code false}
     *                          otherwise
     */
    public void setUseNoviFiatFormat(Boolean useNoviFiatFormat) {
        this.useNoviFiatFormat = useNoviFiatFormat;
    }

    /**
     * Sets the primary monetary amount of this payment.
     *
     * @param primaryAmount the primary amount
     */
    public void setPrimaryAmount(Money primaryAmount) {
        this.primaryAmount = primaryAmount;
    }

    /**
     * Sets the exchange monetary amount for currency conversions.
     *
     * @param exchangeAmount the exchange amount
     */
    public void setExchangeAmount(Money exchangeAmount) {
        this.exchangeAmount = exchangeAmount;
    }

    /**
     * The deprecated enum-based currency identifier for a payment.
     *
     * <p>This enumeration was the original mechanism for identifying the payment
     * currency. It has been superseded by the string-based currency field on
     * {@link PaymentInfo} which carries a standard ISO 4217 code. Only Indian Rupee
     * ({@link #INR}) was ever defined in this enum, reflecting the initial India-only
     * rollout of WhatsApp Payments.
     */
    @ProtobufEnum(name = "PaymentInfo.Currency")
    public enum Currency {
        /**
         * The currency is not recognized or was not specified.
         */
        UNKNOWN_CURRENCY(0),

        /**
         * Indian Rupee, the currency used in the initial rollout of WhatsApp
         * Payments in India.
         */
        INR(1);

        /**
         * Constructs a new {@code Currency} with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        Currency(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index of this currency.
         */
        final int index;

        /**
         * Returns the protobuf enum index of this currency.
         *
         * @return the numeric index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * The high-level payment status visible to users in the WhatsApp chat view.
     *
     * <p>This enumeration models the user-facing lifecycle of a payment transaction,
     * from initial processing through completion or one of several terminal failure
     * states. The WhatsApp client maps a broader set of internal backend statuses
     * (see {@link TxnStatus}) down to these coarser categories for display purposes.
     */
    @ProtobufEnum(name = "PaymentInfo.Status")
    public enum Status {
        /**
         * The payment status is not recognized or was not specified.
         */
        UNKNOWN_STATUS(0),

        /**
         * The payment is currently being processed by the payment backend.
         */
        PROCESSING(1),

        /**
         * The payment has been sent by the sender and is awaiting the receiver's
         * payment provider to process it.
         */
        SENT(2),

        /**
         * The payment has been received and is awaiting acceptance by the receiver,
         * typically because the receiver needs to complete their payment account
         * setup first.
         */
        NEED_TO_ACCEPT(3),

        /**
         * The payment has been successfully completed and the funds have been
         * transferred.
         */
        COMPLETE(4),

        /**
         * The payment could not be completed due to a processing failure, a risk
         * check failure, or a failed refund attempt.
         */
        COULD_NOT_COMPLETE(5),

        /**
         * The payment was refunded to the sender after having been previously
         * completed.
         */
        REFUNDED(6),

        /**
         * The payment request expired before it was fulfilled, or the sender's
         * authorization was cancelled.
         */
        EXPIRED(7),

        /**
         * The payment request was rejected by the intended payer.
         */
        REJECTED(8),

        /**
         * The payment request was cancelled by the requester before it was
         * fulfilled.
         */
        CANCELLED(9),

        /**
         * The payment request is waiting for the payer to initiate the transaction.
         * This status is shown to the requester after they have sent the request.
         */
        WAITING_FOR_PAYER(10),

        /**
         * The payment is in a general waiting state, typically indicating that a
         * collect-style payment request has been initiated and is awaiting
         * processing.
         */
        WAITING(11);

        /**
         * Constructs a new {@code Status} with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        Status(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index of this payment status.
         */
        final int index;

        /**
         * Returns the protobuf enum index of this payment status.
         *
         * @return the numeric index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * The fine-grained internal transaction status that tracks a payment through
     * the server-side state machine.
     *
     * <p>This enumeration models every discrete state that a payment transaction can
     * occupy on the backend, including intermediate states during authorization,
     * collection, refund, and reversal flows. The WhatsApp client maps these
     * fine-grained statuses to the coarser {@link Status} enumeration for user-facing
     * display.
     *
     * <p>Transaction status names follow a convention where the prefix indicates the
     * flow type: {@code PENDING_} and {@code FAILED_} prefixes indicate setup and
     * error states for direct payments, while {@code COLLECT_} prefixes indicate
     * states in the collect (request-payment) flow. {@code AUTH_} prefixes indicate
     * states related to sender authorization.
     */
    @ProtobufEnum(name = "PaymentInfo.TxnStatus")
    public enum TxnStatus {
        /**
         * The transaction status is not recognized or was not specified.
         */
        UNKNOWN(0),

        /**
         * The transaction is pending setup on the sender's side, typically waiting
         * for the sender to complete their payment account configuration.
         */
        PENDING_SETUP(1),

        /**
         * The transaction is pending setup on the receiver's side, typically waiting
         * for the receiver to complete their payment account configuration.
         */
        PENDING_RECEIVER_SETUP(2),

        /**
         * The transaction has been initialized and is ready for processing.
         */
        INIT(3),

        /**
         * The transaction has been successfully processed by the payment backend.
         */
        SUCCESS(4),

        /**
         * The transaction has been fully completed, including any necessary
         * settlement.
         */
        COMPLETED(5),

        /**
         * The transaction failed due to a general processing error.
         */
        FAILED(6),

        /**
         * The transaction failed because it was flagged by a risk assessment check.
         */
        FAILED_RISK(7),

        /**
         * The transaction failed during backend processing.
         */
        FAILED_PROCESSING(8),

        /**
         * The transaction failed during receiver-side processing.
         */
        FAILED_RECEIVER_PROCESSING(9),

        /**
         * The transaction failed during direct account settlement.
         */
        FAILED_DA(10),

        /**
         * The transaction failed during direct account settlement and the failure
         * is final (non-retriable).
         */
        FAILED_DA_FINAL(11),

        /**
         * The transaction was refunded to the sender.
         */
        REFUNDED_TXN(12),

        /**
         * An attempted refund for this transaction failed.
         */
        REFUND_FAILED(13),

        /**
         * An attempted refund failed during backend processing.
         */
        REFUND_FAILED_PROCESSING(14),

        /**
         * An attempted refund failed during direct account settlement.
         */
        REFUND_FAILED_DA(15),

        /**
         * The transaction expired before it could be completed.
         */
        EXPIRED_TXN(16),

        /**
         * The sender cancelled their authorization for this transaction.
         */
        AUTH_CANCELED(17),

        /**
         * The cancellation of sender authorization failed during processing.
         */
        AUTH_CANCEL_FAILED_PROCESSING(18),

        /**
         * The cancellation of sender authorization failed.
         */
        AUTH_CANCEL_FAILED(19),

        /**
         * A collect (request-payment) transaction has been initialized.
         */
        COLLECT_INIT(20),

        /**
         * A collect transaction has been successfully processed.
         */
        COLLECT_SUCCESS(21),

        /**
         * A collect transaction failed due to a general processing error.
         */
        COLLECT_FAILED(22),

        /**
         * A collect transaction failed because it was flagged by a risk assessment
         * check.
         */
        COLLECT_FAILED_RISK(23),

        /**
         * A collect transaction was rejected by the payer.
         */
        COLLECT_REJECTED(24),

        /**
         * A collect transaction expired before the payer fulfilled it.
         */
        COLLECT_EXPIRED(25),

        /**
         * A collect transaction was cancelled by the requester.
         */
        COLLECT_CANCELED(26),

        /**
         * A collect transaction is in the process of being cancelled.
         */
        COLLECT_CANCELLING(27),

        /**
         * The transaction is under review by the payment provider.
         */
        IN_REVIEW(28),

        /**
         * A reversal of the transaction has been successfully processed.
         */
        REVERSAL_SUCCESS(29),

        /**
         * A reversal of the transaction is pending processing.
         */
        REVERSAL_PENDING(30),

        /**
         * A refund for this transaction is pending processing.
         */
        REFUND_PENDING(31);

        /**
         * Constructs a new {@code TxnStatus} with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        TxnStatus(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index of this transaction status.
         */
        final int index;

        /**
         * Returns the protobuf enum index of this transaction status.
         *
         * @return the numeric index
         */
        public int index() {
            return this.index;
        }
    }
}
