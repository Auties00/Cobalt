package it.auties.whatsapp.protobuf.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class BusinessDateTimeComponent {
  @JsonProperty("1")
  @JsonPropertyDescription("HSMDateTimeComponentDayOfWeekType")
  private HSMDateTimeComponentDayOfWeekType dayOfWeek;

  @JsonProperty("2")
  @JsonPropertyDescription("uint32")
  private int year;

  @JsonProperty("3")
  @JsonPropertyDescription("uint32")
  private int month;

  @JsonProperty("4")
  @JsonPropertyDescription("uint32")
  private int dayOfMonth;

  @JsonProperty("5")
  @JsonPropertyDescription("uint32")
  private int hour;

  @JsonProperty("6")
  @JsonPropertyDescription("uint32")
  private int minute;

  @JsonProperty("7")
  @JsonPropertyDescription("HSMDateTimeComponentCalendarType")
  private HSMDateTimeComponentCalendarType calendar;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum HSMDateTimeComponentDayOfWeekType {
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
  public enum HSMDateTimeComponentCalendarType {
    GREGORIAN(1),
    SOLAR_HIJRI(2);

    @Getter
    private final int index;

    @JsonCreator
    public static HSMDateTimeComponentCalendarType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
