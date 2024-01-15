package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents a time component
 */
@ProtobufMessageName("Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent")
public record HighlyStructuredDateTimeComponent(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        DayOfWeek dayOfWeek,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        int year,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        int month,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
        int dayOfMonth,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
        int hour,
        @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
        int minute,
        @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
        CalendarType calendar
) implements HighlyStructuredDateTimeValue {
    @Override
    public HighlyStructuredDateTimeValue.Type dateType() {
        return HighlyStructuredDateTimeValue.Type.COMPONENT;
    }

    /**
     * The constants of this enumerated type describe the supported calendar types
     */
    @ProtobufMessageName("Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent.CalendarType")
    public enum CalendarType implements ProtobufEnum {
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

        public int index() {
            return index;
        }
    }

    /**
     * The constants of this enumerated type describe the days of the week
     */
    @ProtobufMessageName("Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime.HSMDateTimeComponent.DayOfWeekType")
    public enum DayOfWeek implements ProtobufEnum {
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

        public int index() {
            return index;
        }
    }
}