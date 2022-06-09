package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.payment.RequestPaymentMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that holds the information related to a payment.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
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
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = PaymentInfoCurrency.class)
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
  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = PaymentInfoStatus.class)
  private PaymentInfoStatus status;

  /**
   * The timestamp, that is the seconds since {@link java.time.Instant#EPOCH}, when the {@link RequestPaymentMessage} that originated this transaction was sent
   */
  @ProtobufProperty(index = 5, type = UINT64)
  private long transactionTimestamp;

  /**
   * The MessageKey of the {@link RequestPaymentMessage} that originated this transaction
   */
  @ProtobufProperty(index = 6, type = MESSAGE, concreteType = MessageKey.class)
  private MessageKey requestMessageKey;

  /**
   * The date of expiration of this transaction
   */
  @ProtobufProperty(index = 7, type = UINT64)
  private long expiryTimestamp;

  /**
   * Future proofed
   */
  @ProtobufProperty(index = 8, type = BOOLEAN)
  private boolean futureProofed;

  /**
   * The currency of this transaction
   */
  @ProtobufProperty(index = 9, type = STRING)
  private String currency;

  /**
   * The status of this transaction
   */
  @ProtobufProperty(index = 10, type = MESSAGE, concreteType = PaymentInfoTxnStatus.class)
  private PaymentInfoTxnStatus transactionStatus;

  /**
   * The constants of this enumerated type describe the currencies supported for a transaction described by a {@link PaymentInfo}
   */
  @Accessors(fluent = true)
  @Deprecated
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

    PaymentInfoCurrency(int index) {
      this.index = index;
    }

    @JsonCreator
    public static PaymentInfoCurrency forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  /**
   * The constants of this enumerated type describe the status of a payment described by a {@link PaymentInfo}
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
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

    public static PaymentInfoStatus forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
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
    COLLECT_CANCELLING(27);

    @Getter
    private final int index;

    public static PaymentInfoTxnStatus forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
