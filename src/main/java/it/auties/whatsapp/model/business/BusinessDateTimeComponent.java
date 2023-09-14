package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents a time component
 */
public record BusinessDateTimeComponent(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        BusinessDayOfWeek dayOfWeek,
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
        @NonNull
        BusinessCalendarType calendar
) implements BusinessDateTimeValue {
    @Override
    public BusinessDateTimeType dateType() {
        return BusinessDateTimeType.COMPONENT;
    }
}