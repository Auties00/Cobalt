package it.auties.whatsapp4j.protobuf.message.business;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class PaymentInviteMessage {
  @JsonProperty(value = "2")
  private long expiryTimestamp;

  @JsonProperty(value = "1")
  private PaymentInviteMessageServiceType serviceType;

  @Accessors(fluent = true)
  public enum PaymentInviteMessageServiceType {
    UNKNOWN(0),
    FBPAY(1),
    NOVI(2),
    UPI(3);

    private final @Getter int index;

    PaymentInviteMessageServiceType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static PaymentInviteMessageServiceType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
