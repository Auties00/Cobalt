package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

/**
 * A model class that represents a time
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime")
public final class BusinessDateTime implements BusinessLocalizableParameterValue {
    /**
     * The date as a component
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = BusinessDateTimeComponent.class)
    private BusinessDateTimeComponent dateComponent;

    /**
     * The date as a unix epoch
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = BusinessDateTimeUnixEpoch.class)
    private BusinessDateTimeUnixEpoch dateUnixEpoch;

    /**
     * Constructs a new date time using a component
     *
     * @param dateComponent the non-null component
     * @return a non-null date time
     */
    public static BusinessDateTime of(@NonNull BusinessDateTimeValue dateComponent) {
        return switch (dateComponent){
            case BusinessDateTimeComponent businessDateTimeComponent -> new BusinessDateTime(businessDateTimeComponent, null);
            case BusinessDateTimeUnixEpoch businessDateTimeUnixEpoch -> new BusinessDateTime(null, businessDateTimeUnixEpoch);
        };
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
    public Optional<BusinessDateTimeValue> date() {
        if(dateComponent != null){
            return Optional.of(dateComponent);
        }

        if(dateUnixEpoch != null){
            return Optional.of(dateUnixEpoch);
        }

        return Optional.empty();
    }

    /**
     * Returns the date component of this component
     *
     * @return a non-null date type
     */
    public Optional<BusinessDateTimeComponent> dateComponent() {
        return Optional.ofNullable(dateComponent);
    }

    /**
     * Returns the unix epoch of this component
     *
     * @return a non-null date type
     */
    public Optional<BusinessDateTimeUnixEpoch> dateUnixEpoch() {
        return Optional.ofNullable(dateUnixEpoch);
    }

    @Override
    public BusinessLocalizableParameterType parameterType() {
        return BusinessLocalizableParameterType.DATE_TIME;
    }
}