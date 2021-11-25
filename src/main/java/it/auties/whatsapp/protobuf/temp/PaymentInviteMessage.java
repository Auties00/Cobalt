package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class PaymentInviteMessage {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("int64")
  private long expiryTimestamp;

  @JsonProperty(value = "1")
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
