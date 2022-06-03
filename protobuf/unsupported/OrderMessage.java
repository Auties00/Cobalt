package it.auties.whatsapp;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

import it.auties.protobuf.api.model.ProtobufProperty;
import java.util.*;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.jackson.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class OrderMessage {

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum OrderMessageOrderSurface {
    CATALOG(1);

    @Getter private final int index;

    public static OrderMessageOrderSurface forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum OrderMessageOrderStatus {
    INQUIRY(1);

    @Getter private final int index;

    public static OrderMessageOrderStatus forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @ProtobufProperty(index = 1, type = STRING)
  private String orderId;

  @ProtobufProperty(index = 2, type = BYTES)
  private byte[] thumbnail;

  @ProtobufProperty(index = 3, type = INT32)
  private Integer itemCount;

  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = OrderMessageOrderStatus.class)
  private OrderMessageOrderStatus status;

  @ProtobufProperty(index = 5, type = MESSAGE, concreteType = OrderMessageOrderSurface.class)
  private OrderMessageOrderSurface surface;

  @ProtobufProperty(index = 6, type = STRING)
  private String message;

  @ProtobufProperty(index = 7, type = STRING)
  private String orderTitle;

  @ProtobufProperty(index = 8, type = STRING)
  private String sellerJid;

  @ProtobufProperty(index = 9, type = STRING)
  private String token;

  @ProtobufProperty(index = 10, type = INT64)
  private Long totalAmount1000;

  @ProtobufProperty(index = 11, type = STRING)
  private String totalCurrencyCode;

  @ProtobufProperty(index = 17, type = MESSAGE, concreteType = ContextInfo.class)
  private ContextInfo contextInfo;
}
