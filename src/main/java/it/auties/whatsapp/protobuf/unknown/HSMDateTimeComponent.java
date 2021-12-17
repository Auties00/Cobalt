package it.auties.whatsapp.protobuf.unknown;

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
public class HSMDateTimeComponent {
  @JsonProperty(value = "7")
  @JsonPropertyDescription("HSMDateTimeComponentCalendarType")
  private HSMDateTimeComponentCalendarType calendar;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("uint32")
  private int minute;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("uint32")
  private int hour;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("uint32")
  private int dayOfMonth;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("uint32")
  private int month;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("uint32")
  private int year;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("HSMDateTimeComponentDayOfWeekType")
  private HSMDateTimeComponentDayOfWeekType dayOfWeek;

  @Accessors(fluent = true)
  public enum HSMDateTimeComponentDayOfWeekType {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    private final @Getter int index;

    HSMDateTimeComponentDayOfWeekType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static HSMDateTimeComponentDayOfWeekType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum HSMDateTimeComponentCalendarType {
    GREGORIAN(1),
    SOLAR_HIJRI(2);

    private final @Getter int index;

    HSMDateTimeComponentCalendarType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static HSMDateTimeComponentCalendarType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
