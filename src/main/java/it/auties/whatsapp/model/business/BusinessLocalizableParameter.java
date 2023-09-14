package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * A model class that represents a time a localizable parameter
 */
public record BusinessLocalizableParameter(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String defaultValue,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<BusinessCurrency> parameterCurrency,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<BusinessDateTime> parameterDateTime
) implements ProtobufMessage {
    /**
     * Constructs a new localizable parameter with a default value and a parameter
     *
     * @param defaultValue the default value
     * @param parameter    the parameter
     * @return a non-null localizable parameter
     */
    public static BusinessLocalizableParameter of(String defaultValue, BusinessLocalizableParameterValue parameter) {
        var builder = new BusinessLocalizableParameterBuilder()
                .defaultValue(defaultValue);
        switch (parameter) {
            case BusinessCurrency businessCurrency -> builder.parameterCurrency(businessCurrency);
            case BusinessDateTime businessDateTime -> builder.parameterDateTime(businessDateTime);
            case null -> {}
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
    public Optional<? extends BusinessLocalizableParameterValue> parameter() {
        return parameterCurrency.isPresent() ? parameterCurrency : parameterDateTime;
    }
}