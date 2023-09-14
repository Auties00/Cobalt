package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * A model class that represents a time
 */
public record BusinessDateTime(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Optional<BusinessDateTimeComponent> dateComponent,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<BusinessDateTimeUnixEpoch> dateUnixEpoch
) implements BusinessLocalizableParameterValue {
    /**
     * Constructs a new date time using a component
     *
     * @param dateComponent the non-null component
     * @return a non-null date time
     */
    public static BusinessDateTime of(@NonNull BusinessDateTimeValue dateComponent) {
        if (dateComponent instanceof BusinessDateTimeComponent businessDateTimeComponent) {
            return new BusinessDateTime(Optional.of(businessDateTimeComponent), Optional.empty());
        } else if (dateComponent instanceof BusinessDateTimeUnixEpoch businessDateTimeUnixEpoch) {
            return new BusinessDateTime(Optional.empty(), Optional.of(businessDateTimeUnixEpoch));
        } else {
            return new BusinessDateTime(Optional.empty(), Optional.empty());
        }
    }

    /**
     * Returns the type of date of this component
     *
     * @return a non-null date type
     */
    public BusinessDateTimeType dateType() {
        return date().map(BusinessDateTimeValue::dateType)
                .orElse(BusinessDateTimeType.NONE);
    }

    /**
     * Returns the date of this component
     *
     * @return a non-null date type
     */
    public Optional<? extends BusinessDateTimeValue> date() {
        return dateComponent.isPresent() ? dateComponent : dateUnixEpoch;
    }

    @Override
    public BusinessLocalizableParameterType parameterType() {
        return BusinessLocalizableParameterType.DATE_TIME;
    }
}