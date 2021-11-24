package it.auties.whatsapp.protobuf.beta;

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

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("int64")
  private long expiryTimestamp;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("PaymentInviteMessageServiceType")
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