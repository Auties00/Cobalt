package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.UINT32;

/**
 * A model class that represents a time component
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessDateTimeComponent implements ProtobufMessage {
    /**
     * The day of the week
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = DayOfWeek.class)
    private DayOfWeek dayOfWeek;

    /**
     * The year
     */
    @ProtobufProperty(index = 2, type = UINT32)
    private int year;

    /**
     * The month
     */
    @ProtobufProperty(index = 3, type = UINT32)
    private int month;

    /**
     * The day of the month
     */
    @ProtobufProperty(index = 4, type = UINT32)
    private int dayOfMonth;

    /**
     * The hour
     */
    @ProtobufProperty(index = 5, type = UINT32)
    private int hour;

    /**
     * The minute
     */
    @ProtobufProperty(index = 6, type = UINT32)
    private int minute;

    /**
     * The type of calendar
     */
    @ProtobufProperty(index = 7, type = MESSAGE, implementation = CalendarType.class)
    private CalendarType calendar;

    /**
     * The constants of this enumerated type describe the days of the week
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum DayOfWeek implements ProtobufMessage {
        /**
         * Monday
         */
        MONDAY(1),

        /**
         * Tuesday
         */
        TUESDAY(2),

        /**
         * Wednesday
         */
        WEDNESDAY(3),

        /**
         * Thursday
         */
        THURSDAY(4),

        /**
         * Friday
         */
        FRIDAY(5),

        /**
         * Saturday
         */
        SATURDAY(6),

        /**
         * Sunday
         */
        SUNDAY(7);

        @Getter
        private final int index;

        @JsonCreator
        public static DayOfWeek of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * The constants of this enumerated type describe the supported calendar types
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum CalendarType implements ProtobufMessage {
        /**
         * Gregorian calendar
         */
        GREGORIAN(1),

        /**
         * Solar calendar
         */
        SOLAR_HIJRI(2);

        @Getter
        private final int index;

        public static CalendarType of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
