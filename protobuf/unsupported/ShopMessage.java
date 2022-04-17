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
public class ShopMessage {

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ShopMessageSurface {
    UNKNOWN_SURFACE(0),
    FB(1),
    IG(2),
    WA(3);

    @Getter private final int index;

    public static ShopMessageSurface forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @ProtobufProperty(index = 1, type = STRING)
  private String id;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ShopMessageSurface.class)
  private ShopMessageSurface surface;

  @ProtobufProperty(index = 3, type = INT32)
  private int messageVersion;
}
