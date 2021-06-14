package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.message.model.MessageKey;
import it.auties.whatsapp4j.protobuf.message.business.RequestPaymentMessage;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * A model class that holds the information related to a payment.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class PaymentInfo {
  /**
   * The status of this transaction
   */
  @JsonProperty(value = "10")
  private PaymentInfoTxnStatus transactionStatus;

  /**
   * The currency of this transaction
   */
  @JsonProperty(value = "9")
  private String currency;

  /**
   * Future proofed
   */
  @JsonProperty(value = "8")
  private boolean futureproofed;

  /**
   * The date of expiration of this transaction
   */
  @JsonProperty(value = "7")
  private long expiryTimestamp;

  /**
   * The MessageKey of the {@link RequestPaymentMessage} that originated this transaction
   */
  @JsonProperty(value = "6")
  private MessageKey requestMessageKey;

  /**
   * The timestamp, that is the seconds since {@link java.time.Instant#EPOCH}, when the {@link RequestPaymentMessage} that originated this transaction was sent
   */
  @JsonProperty(value = "5")
  private long transactionTimestamp;

  /**
   * The status of this payment
   */
  @JsonProperty(value = "4")
  private PaymentInfoStatus status;

  /**
   * The jid of the beneficiary of this transaction
   */
  @JsonProperty(value = "3")
  private String receiverJid;

  /**
   * The amount of money involved in this transaction
   */
  @JsonProperty(value = "2")
  private long amount1000;

  /**
   * The currency of this transaction
   */
  @Deprecated
  @JsonProperty(value = "1")
  private PaymentInfoCurrency currencyDeprecated;

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

    private final @Getter int index;

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

    private final @Getter int index;

    PaymentInfoStatus(int index) {
      this.index = index;
    }

    @JsonCreator
    public static PaymentInfoStatus forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

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

    private final @Getter int index;

    PaymentInfoTxnStatus(int index) {
      this.index = index;
    }

    @JsonCreator
    public static PaymentInfoTxnStatus forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
