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
public class OrderMessage {

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("string")
  private String totalCurrencyCode;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("int64")
  private long totalAmount1000;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("string")
  private String token;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("string")
  private String sellerJid;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("string")
  private String orderTitle;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("string")
  private String message;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("OrderMessageOrderSurface")
  private OrderMessageOrderSurface surface;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("OrderMessageOrderStatus")
  private OrderMessageOrderStatus status;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("int32")
  private int itemCount;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
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
