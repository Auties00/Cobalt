package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a time a localizable parameter
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessLocalizableParameter implements ProtobufMessage {
    /**
     * The default value
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String defaultValue;

    /**
     * The currency parameter
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = BusinessCurrency.class)
    private BusinessCurrency parameterCurrency;

    /**
     * The time parameter
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = BusinessDateTime.class)
    private BusinessDateTime parameterDateTime;

    /**
     * Constructs a new localizable parameter with a default value and a parameter
     *
     * @param defaultValue the default value
     * @param parameter    the parameter
     * @return a non-null localizable parameter
     */
    public static BusinessLocalizableParameter of(String defaultValue, @NonNull BusinessLocalizableParameterValue parameter) {
        var builder = BusinessLocalizableParameter.builder()
                .defaultValue(defaultValue);
        if (parameter instanceof BusinessCurrency businessCurrency) {
            builder.parameterCurrency(businessCurrency);
        } else if (parameter instanceof BusinessDateTime businessDateTime) {
            builder.parameterDateTime(businessDateTime);
        }
        return builder.build();
    }

    /**
     * Returns the type of parameter that this message wraps
     *
     * @return a non-null parameter type
     */
    public BusinessLocalizableParameterType parameterType() {
        return parameter()
                .map(BusinessLocalizableParameterValue::parameterType)
                .orElse(BusinessLocalizableParameterType.NONE);
    }

    /**
     * Returns the parameter that this message wraps
     *
     * @return a non-null optional
     */
    public Optional<BusinessLocalizableParameterValue> parameter() {
        if(parameterCurrency != null){
            return Optional.of(parameterCurrency);
        }

        if(parameterDateTime != null){
            return Optional.of(parameterDateTime);
        }

        return Optional.empty();
    }

    /**
     * Returns the currency parameter that this message wraps
     *
     * @return a non-null optional
     */
    public Optional<BusinessCurrency> parameterCurrency(){
        return Optional.ofNullable(parameterCurrency);
    }

    /**
     * Returns the datetime parameter that this message wraps
     *
     * @return a non-null optional
     */
    public Optional<BusinessDateTime> parameterDateTime(){
        return Optional.ofNullable(parameterDateTime);
    }
}