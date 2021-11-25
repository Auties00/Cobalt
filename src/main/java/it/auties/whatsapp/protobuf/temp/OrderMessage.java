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
public class OrderMessage {
  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("string")
  private String totalCurrencyCode;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("int64")
  private long totalAmount1000;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("string")
  private String token;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("string")
  private String sellerJid;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("string")
  private String orderTitle;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("string")
  private String message;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("OrderMessageOrderSurface")
  private OrderMessageOrderSurface surface;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("OrderMessageOrderStatus")
  private OrderMessageOrderStatus status;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("int32")
  private int itemCount;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  @JsonProperty(value = "1")
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
