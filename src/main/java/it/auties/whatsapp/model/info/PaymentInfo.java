package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.payment.PaymentMoney;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that holds the information related to a payment.
 */
@ProtobufMessage(name = "PaymentInfo")
public final class PaymentInfo implements Info {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final Currency currencyDeprecated;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    final long amount1000;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid receiverJid;

    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    final Status status;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    final long transactionTimestampSeconds;

    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final ChatMessageKey requestMessageKey;

    @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
    final long expiryTimestampSeconds;

    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    final boolean futureProofed;

    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String currency;

    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    final TransactionStatus transactionStatus;

    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    final boolean useNoviFormat;

    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    final PaymentMoney primaryAmount;

    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    final PaymentMoney exchangeAmount;

    PaymentInfo(Currency currencyDeprecated, long amount1000, Jid receiverJid, Status status, long transactionTimestampSeconds, ChatMessageKey requestMessageKey, long expiryTimestampSeconds, boolean futureProofed, String currency, TransactionStatus transactionStatus, boolean useNoviFormat, PaymentMoney primaryAmount, PaymentMoney exchangeAmount) {
        this.currencyDeprecated = Objects.requireNonNull(currencyDeprecated, "currencyDeprecated cannot be null");
        this.amount1000 = amount1000;
        this.receiverJid = Objects.requireNonNull(receiverJid, "receiverJid cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.transactionTimestampSeconds = transactionTimestampSeconds;
        this.requestMessageKey = Objects.requireNonNull(requestMessageKey, "requestMessageKey cannot be null");
        this.expiryTimestampSeconds = expiryTimestampSeconds;
        this.futureProofed = futureProofed;
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
        this.transactionStatus = Objects.requireNonNull(transactionStatus, "transactionStatus cannot be null");
        this.useNoviFormat = useNoviFormat;
        this.primaryAmount = Objects.requireNonNull(primaryAmount, "primaryAmount cannot be null");
        this.exchangeAmount = Objects.requireNonNull(exchangeAmount, "exchangeAmount cannot be null");
    }

    public Currency currencyDeprecated() {
        return currencyDeprecated;
    }

    public long amount1000() {
        return amount1000;
    }

    public Jid receiverJid() {
        return receiverJid;
    }

    public Status status() {
        return status;
    }

    public long transactionTimestampSeconds() {
        return transactionTimestampSeconds;
    }

    public ChatMessageKey requestMessageKey() {
        return requestMessageKey;
    }

    public long expiryTimestampSeconds() {
        return expiryTimestampSeconds;
    }

    public boolean futureProofed() {
        return futureProofed;
    }

    public String currency() {
        return currency;
    }

    public TransactionStatus transactionStatus() {
        return transactionStatus;
    }

    public boolean useNoviFormat() {
        return useNoviFormat;
    }

    public PaymentMoney primaryAmount() {
        return primaryAmount;
    }

    public PaymentMoney exchangeAmount() {
        return exchangeAmount;
    }

    public Optional<ZonedDateTime> transactionTimestamp() {
        return Clock.parseSeconds(transactionTimestampSeconds);
    }

    public Optional<ZonedDateTime> expiryTimestamp() {
        return Clock.parseSeconds(expiryTimestampSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PaymentInfo that
                && Objects.equals(currencyDeprecated, that.currencyDeprecated)
                && amount1000 == that.amount1000
                && Objects.equals(receiverJid, that.receiverJid)
                && Objects.equals(status, that.status)
                && transactionTimestampSeconds == that.transactionTimestampSeconds
                && Objects.equals(requestMessageKey, that.requestMessageKey)
                && expiryTimestampSeconds == that.expiryTimestampSeconds
                && futureProofed == that.futureProofed
                && Objects.equals(currency, that.currency)
                && Objects.equals(transactionStatus, that.transactionStatus)
                && useNoviFormat == that.useNoviFormat
                && Objects.equals(primaryAmount, that.primaryAmount)
                && Objects.equals(exchangeAmount, that.exchangeAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyDeprecated, amount1000, receiverJid, status, transactionTimestampSeconds,
                requestMessageKey, expiryTimestampSeconds, futureProofed, currency, transactionStatus, useNoviFormat,
                primaryAmount, exchangeAmount);
    }

    @Override
    public String toString() {
        return "PaymentInfo[" +
                "currencyDeprecated=" + currencyDeprecated + ", " +
                "amount1000=" + amount1000 + ", " +
                "receiverJid=" + receiverJid + ", " +
                "status=" + status + ", " +
                "transactionTimestampSeconds=" + transactionTimestampSeconds + ", " +
                "requestMessageKey=" + requestMessageKey + ", " +
                "expiryTimestampSeconds=" + expiryTimestampSeconds + ", " +
                "futureProofed=" + futureProofed + ", " +
                "currency=" + currency + ", " +
                "transactionStatus=" + transactionStatus + ", " +
                "useNoviFormat=" + useNoviFormat + ", " +
                "primaryAmount=" + primaryAmount + ", " +
                "exchangeAmount=" + exchangeAmount + ']';
    }

    /**
     * The constants of this enumerated type describe the status of a payment described by a
     * {@link PaymentInfo}
     */
    @ProtobufEnum(name = "PaymentInfo.Status")
    public enum Status {
        /**
         * Unknown status
         */
        UNKNOWN_STATUS(0),
        /**
         * Processing
         */
        PROCESSING(1),
        /**
         * Sent
         */
        SENT(2),
        /**
         * Need to accept
         */
        NEED_TO_ACCEPT(3),
        /**
         * Complete
         */
        COMPLETE(4),
        /**
         * Could not complete
         */
        COULD_NOT_COMPLETE(5),
        /**
         * Refunded
         */
        REFUNDED(6),
        /**
         * Expired
         */
        EXPIRED(7),
        /**
         * Rejected
         */
        REJECTED(8),
        /**
         * Cancelled
         */
        CANCELLED(9),
        /**
         * Waiting for payer
         */
        WAITING_FOR_PAYER(10),
        /**
         * Waiting
         */
        WAITING(11);

        final int index;

        Status(@ProtobufEnumIndex int index) {
            this.index = index;
        }

    }

    /**
     * The constants of this enumerated type describe the currencies supported for a transaction
     * described by a {@link PaymentInfo}
     */
    @ProtobufEnum(name = "PaymentInfo.Currency")
    public enum Currency {
        /**
         * Unknown currency
         */
        UNKNOWN_CURRENCY(0),
        /**
         * Indian rupees
         */
        INR(1);

        final int index;

        Currency(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }

    @ProtobufEnum(name = "PaymentInfo.TxnStatus")
    public enum TransactionStatus {
        UNKNOWN(0),
        PENDING_SETUP(1),
        PENDING_RECEIVER_SETUP(2),
        INIT(3),
        SUCCESS(4),
        COMPLETED(5),
        FAILED(6),
        FAILED_RISK(7),
        FAILED_PROCESSING(8),
        FAILED_RECEIVER_PROCESSING(9),
        FAILED_DA(10),
        FAILED_DA_FINAL(11),
        REFUNDED_TXN(12),
        REFUND_FAILED(13),
        REFUND_FAILED_PROCESSING(14),
        REFUND_FAILED_DA(15),
        EXPIRED_TXN(16),
        AUTH_CANCELED(17),
        AUTH_CANCEL_FAILED_PROCESSING(18),
        AUTH_CANCEL_FAILED(19),
        COLLECT_INIT(20),
        COLLECT_SUCCESS(21),
        COLLECT_FAILED(22),
        COLLECT_FAILED_RISK(23),
        COLLECT_REJECTED(24),
        COLLECT_EXPIRED(25),
        COLLECT_CANCELED(26),
        COLLECT_CANCELLING(27),
        IN_REVIEW(28),
        REVERSAL_SUCCESS(29),
        REVERSAL_PENDING(30),
        REFUND_PENDING(31);

        final int index;

        TransactionStatus(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}