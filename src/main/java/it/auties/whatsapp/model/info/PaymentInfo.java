package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.payment.RequestPaymentMessage;
import it.auties.whatsapp.model.payment.PaymentMoney;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.*;
/**
 * A model class that holds the information related to a payment.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class PaymentInfo implements Info {
    /**
     * The currency of this transaction
     */
    @Deprecated
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = PaymentInfo.PaymentInfoCurrency.class)
    private PaymentInfoCurrency currencyDeprecated;

    /**
     * The amount of money involved in this transaction
     */
    @ProtobufProperty(index = 2, type = UINT64)
    private long amount1000;

    /**
     * The jid of the beneficiary of this transaction
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String receiverJid;

    /**
     * The status of this payment
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = PaymentInfo.PaymentInfoStatus.class)
    private PaymentInfoStatus status;

    /**
     * The timestamp, that is the seconds since {@link java.time.Instant#EPOCH}, when the {@link RequestPaymentMessage} that originated this transaction was sent
     */
    @ProtobufProperty(index = 5, type = UINT64)
    private long transactionTimestamp;

    /**
     * The MessageKey of the {@link RequestPaymentMessage} that originated this transaction
     */
    @ProtobufProperty(index = 6, type = MESSAGE, implementation = MessageKey.class)
    private MessageKey requestMessageKey;

    /**
     * The date of expiration of this transaction
     */
    @ProtobufProperty(index = 7, type = UINT64)
    private long expiryTimestamp;

    /**
     * Future proofed
     */
    @ProtobufProperty(index = 8, type = BOOL)
    private boolean futureProofed;

    /**
     * The currency of this transaction
     */
    @ProtobufProperty(index = 9, type = STRING)
    private String currency;

    /**
     * The status of this transaction
     */
    @ProtobufProperty(index = 10, type = MESSAGE, implementation = PaymentInfo.PaymentInfoTxnStatus.class)
    private PaymentInfoTxnStatus transactionStatus;

    /**
     * Whether the novi format should be used
     */
    @ProtobufProperty(index = 11, type = BOOL)
    private boolean useNoviFormat;

    /**
     * The primary amount
     */
    @ProtobufProperty(index = 12, type = MESSAGE, implementation = PaymentMoney.class)
    private PaymentMoney primaryAmount;

    /**
     * The exchange amount
     */
    @ProtobufProperty(index = 13, type = MESSAGE, implementation = PaymentMoney.class)
    private PaymentMoney exchangeAmount;

    /**
     * The constants of this enumerated type describe the currencies supported for a transaction described by a {@link PaymentInfo}
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    @Deprecated
    @ProtobufName("Currency")
    public enum PaymentInfoCurrency {

        /**
         * Unknown currency
         */
        UNKNOWN_CURRENCY(0),
        /**
         * Indian rupees
         */
        INR(1);

        @Getter
        private final int index;

        @JsonCreator
        public static PaymentInfoCurrency of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }

    /**
     * The constants of this enumerated type describe the status of a payment described by a {@link PaymentInfo}
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("Status")
    public enum PaymentInfoStatus {

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
        @Getter
        private final int index;

        public static PaymentInfoStatus of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("TxnStatus")
    public enum PaymentInfoTxnStatus {

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
        @Getter
        private final int index;

        public static PaymentInfoTxnStatus of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }
}