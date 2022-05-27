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
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessDateTimeComponent implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = HSMDateTimeComponentDayOfWeekType.class)
  private HSMDateTimeComponentDayOfWeekType dayOfWeek;

  @ProtobufProperty(index = 2, type = UINT32)
  private Integer year;

  @ProtobufProperty(index = 3, type = UINT32)
  private Integer month;

  @ProtobufProperty(index = 4, type = UINT32)
  private Integer dayOfMonth;

  @ProtobufProperty(index = 5, type = UINT32)
  private Integer hour;

  @ProtobufProperty(index = 6, type = UINT32)
  private Integer minute;

  @ProtobufProperty(index = 7, type = MESSAGE, concreteType = HSMDateTimeComponentCalendarType.class)
  private HSMDateTimeComponentCalendarType calendar;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum HSMDateTimeComponentDayOfWeekType implements ProtobufMessage {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    @Getter
    private final int index;

    @JsonCreator
    public static HSMDateTimeComponentDayOfWeekType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum HSMDateTimeComponentCalendarType implements ProtobufMessage {
    GREGORIAN(1),
    SOLAR_HIJRI(2);

    @Getter
    private final int index;

    public static HSMDateTimeComponentCalendarType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
