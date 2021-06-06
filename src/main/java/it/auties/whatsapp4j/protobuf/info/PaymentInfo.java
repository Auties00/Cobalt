package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.message.MessageKey;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class PaymentInfo {
  @JsonProperty(value = "10")
  private PaymentInfoTxnStatus txnStatus;

  @JsonProperty(value = "9")
  private String currency;

  @JsonProperty(value = "8")
  private boolean futureproofed;

  @JsonProperty(value = "7")
  private long expiryTimestamp;

  @JsonProperty(value = "6")
  private MessageKey requestMessageKey;

  @JsonProperty(value = "5")
  private long transactionTimestamp;

  @JsonProperty(value = "4")
  private PaymentInfoStatus status;

  @JsonProperty(value = "3")
  private String receiverJid;

  @JsonProperty(value = "2")
  private long amount1000;

  @JsonProperty(value = "1")
  private PaymentInfoCurrency currencyDeprecated;

  @Accessors(fluent = true)
  public enum PaymentInfoCurrency {
    UNKNOWN_CURRENCY(0),
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

  @Accessors(fluent = true)
  public enum PaymentInfoStatus {
    UNKNOWN_STATUS(0),
    PROCESSING(1),
    SENT(2),
    NEED_TO_ACCEPT(3),
    COMPLETE(4),
    COULD_NOT_COMPLETE(5),
    REFUNDED(6),
    EXPIRED(7),
    REJECTED(8),
    CANCELLED(9),
    WAITING_FOR_PAYER(10),
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
