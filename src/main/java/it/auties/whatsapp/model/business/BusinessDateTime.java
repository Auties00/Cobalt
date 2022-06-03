package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessDateTime implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = BusinessDateTimeComponent.class)
  private BusinessDateTimeComponent component;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = BusinessDateTimeUnixEpoch.class)
  private BusinessDateTimeUnixEpoch unixEpoch;

  public DateType dateType() {
    if (component != null) return DateType.COMPONENT;
    if (unixEpoch != null) return DateType.UNIX_EPOCH;
    return DateType.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum DateType implements ProtobufMessage {
    UNKNOWN(0),
    COMPONENT(1),
    UNIX_EPOCH(2);

    @Getter
    private final int index;

    @JsonCreator
    public static DateType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(DateType.UNKNOWN);
    }
  }
}
