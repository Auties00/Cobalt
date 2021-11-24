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
public class HSMDateTimeComponent {

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("HSMDateTimeComponentCalendarType")
  private HSMDateTimeComponentCalendarType calendar;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("uint32")
  private int minute;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("uint32")
  private int hour;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("uint32")
  private int dayOfMonth;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint32")
  private int month;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("uint32")
  private int year;

  @JsonProperty(value = "1", required = false)
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
