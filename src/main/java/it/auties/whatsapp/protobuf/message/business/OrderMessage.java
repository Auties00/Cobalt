package it.auties.whatsapp.protobuf.message.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class OrderMessage {
  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "11")
  private String totalCurrencyCode;

  @JsonProperty(value = "10")
  private long totalAmount1000;

  @JsonProperty(value = "9")
  private String token;

  @JsonProperty(value = "8")
  private String sellerJid;

  @JsonProperty(value = "7")
  private String orderTitle;

  @JsonProperty(value = "6")
  private String message;

  @JsonProperty(value = "5")
  private OrderMessageOrderSurface surface;

  @JsonProperty(value = "4")
  private OrderMessageOrderStatus status;

  @JsonProperty(value = "3")
  private int itemCount;

  @JsonProperty(value = "2")
  private byte[] thumbnail;

  @JsonProperty(value = "1")
  private String orderId;

  @Accessors(fluent = true)
  public enum OrderMessageOrderStatus {
    INQUIRY(1);

    private final @Getter int index;

    OrderMessageOrderStatus(int index) {
      this.index = index;
    }

    @JsonCreator
    public static OrderMessageOrderStatus forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum OrderMessageOrderSurface {
    CATALOG(1);

    private final @Getter int index;

    OrderMessageOrderSurface(int index) {
      this.index = index;
    }

    @JsonCreator
    public static OrderMessageOrderSurface forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
