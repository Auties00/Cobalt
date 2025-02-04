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
import java.util.Optional;


/**
 * A model class that holds the information related to a payment.
 */
@ProtobufMessage(name = "PaymentInfo")
public record PaymentInfo(
        @Deprecated
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        Currency currencyDeprecated,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        long amount1000,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        Jid receiverJid,
        @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
        Status status,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
        long transactionTimestampSeconds,
        @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
        ChatMessageKey requestMessageKey,
        @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
        long expiryTimestampSeconds,
        @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
        boolean futureProofed,
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        String currency,
        @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
        TransactionStatus transactionStatus,
        @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
        boolean useNoviFormat,
        @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
        PaymentMoney primaryAmount,
        @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
        PaymentMoney exchangeAmount
) implements Info {
    /**
     * Returns when the transaction happened
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> transactionTimestamp() {
        return Clock.parseSeconds(transactionTimestampSeconds);
    }

    /**
     * Returns when the transaction expires
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> expiryTimestamp() {
        return Clock.parseSeconds(expiryTimestampSeconds);
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

        public int index() {
            return index;
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

        public int index() {
            return index;
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

        public int index() {
            return index;
        }
    }
}