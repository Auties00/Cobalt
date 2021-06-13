package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HSMDateTimeComponent {
  @JsonProperty(value = "7")
  private CalendarType calendar;

  @JsonProperty(value = "6")
  private int minute;

  @JsonProperty(value = "5")
  private int hour;

  @JsonProperty(value = "4")
  private int dayOfMonth;

  @JsonProperty(value = "3")
  private int month;

  @JsonProperty(value = "2")
  private int year;

  @JsonProperty(value = "1")
  private DayOfWeekType dayOfWeek;

  @Accessors(fluent = true)
  public enum DayOfWeekType {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    private final @Getter int index;

    DayOfWeekType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static DayOfWeekType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum CalendarType {
    GREGORIAN(1),
    SOLAR_HIJRI(2);

    private final @Getter int index;

    CalendarType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static CalendarType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
