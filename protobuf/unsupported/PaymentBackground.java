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
public class PaymentBackground {

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum PaymentBackgroundType {
    UNKNOWN(0),
    DEFAULT(1);

    @Getter private final int index;

    public static PaymentBackgroundType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @ProtobufProperty(index = 1, type = STRING)
  private String id;

  @ProtobufProperty(index = 2, type = UINT64)
  private Long fileLength;

  @ProtobufProperty(index = 3, type = UINT32)
  private Integer width;

  @ProtobufProperty(index = 4, type = UINT32)
  private Integer height;

  @ProtobufProperty(index = 5, type = STRING)
  private String mimetype;

  @ProtobufProperty(index = 6, type = FIXED32)
  private Integer placeholderArgb;

  @ProtobufProperty(index = 7, type = FIXED32)
  private Integer textArgb;

  @ProtobufProperty(index = 8, type = FIXED32)
  private Integer subtextArgb;

  @ProtobufProperty(index = 9, type = MESSAGE, concreteType = PBMediaData.class)
  private PBMediaData mediaData;

  @ProtobufProperty(index = 10, type = MESSAGE, concreteType = PaymentBackgroundType.class)
  private PaymentBackgroundType type;
}
