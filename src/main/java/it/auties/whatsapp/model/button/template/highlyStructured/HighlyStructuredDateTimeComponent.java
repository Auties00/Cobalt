package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents a time component
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent")
public final class HighlyStructuredDateTimeComponent implements HighlyStructuredDateTimeValue {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final DayOfWeek dayOfWeek;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final int year;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    final int month;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    final int dayOfMonth;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    final int hour;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    final int minute;

    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    final CalendarType calendar;

    HighlyStructuredDateTimeComponent(DayOfWeek dayOfWeek, int year, int month, int dayOfMonth, int hour, int minute, CalendarType calendar) {
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek cannot be null");
        this.year = year;
        this.month = month;
        this.dayOfMonth = dayOfMonth;
        this.hour = hour;
        this.minute = minute;
        this.calendar = Objects.requireNonNull(calendar, "calendar cannot be null");
    }

    public DayOfWeek dayOfWeek() {
        return dayOfWeek;
    }

    public int year() {
        return year;
    }

    public int month() {
        return month;
    }

    public int dayOfMonth() {
        return dayOfMonth;
    }

    public int hour() {
        return hour;
    }

    public int minute() {
        return minute;
    }

    public CalendarType calendar() {
        return calendar;
    }

    @Override
    public Type dateType() {
        return Type.COMPONENT;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredDateTimeComponent that
                && Objects.equals(dayOfWeek, that.dayOfWeek)
                && year == that.year
                && month == that.month
                && dayOfMonth == that.dayOfMonth
                && hour == that.hour
                && minute == that.minute
                && Objects.equals(calendar, that.calendar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, year, month, dayOfMonth, hour, minute, calendar);
    }

    @Override
    public String toString() {
        return "HighlyStructuredDateTimeComponent[" +
                "dayOfWeek=" + dayOfWeek + ", " +
                "year=" + year + ", " +
                "month=" + month + ", " +
                "dayOfMonth=" + dayOfMonth + ", " +
                "hour=" + hour + ", " +
                "minute=" + minute + ", " +
                "calendar=" + calendar + ']';
    }

    /**
     * The constants of this enumerated type describe the supported calendar types
     */
    @ProtobufEnum(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent.CalendarType")
    public enum CalendarType {
        /**
         * Gregorian calendar
         */
        GREGORIAN(1),
        /**
         * Solar calendar
         */
        SOLAR_HIJRI(2);

        final int index;

        CalendarType(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }

    /**
     * The constants of this enumerated type describe the days of the week
     */
    @ProtobufEnum(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent.DayOfWeekType")
    public enum DayOfWeek {
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

        final int index;

        DayOfWeek(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}