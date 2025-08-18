package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents a time a localizable parameter
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter")
public record HighlyStructuredLocalizableParameter(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String defaultValue,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        Optional<HighlyStructuredCurrency> parameterCurrency,
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        Optional<HighlyStructuredDateTime> parameterDateTime
) {
    /**
     * Constructs a new localizable parameter with a default value and a parameter
     *
     * @param defaultValue the default value
     * @param parameter    the parameter
     * @return a non-null localizable parameter
     */
    public static HighlyStructuredLocalizableParameter of(String defaultValue, HighlyStructuredLocalizableParameterValue parameter) {
        var builder = new HighlyStructuredLocalizableParameterBuilder()
                .defaultValue(defaultValue);
        switch (parameter) {
            case HighlyStructuredCurrency highlyStructuredCurrency ->
                    builder.parameterCurrency(highlyStructuredCurrency);
            case HighlyStructuredDateTime businessDateTime -> builder.parameterDateTime(businessDateTime);
            case null -> {
            }
        }
        return builder.build();
    }

    /**
     * Returns the type of parameter that this message wraps
     *
     * @return a non-null parameter type
     */
    public HighlyStructuredLocalizableParameterValue.Type parameterType() {
        return parameter()
                .map(HighlyStructuredLocalizableParameterValue::parameterType)
                .orElse(HighlyStructuredLocalizableParameterValue.Type.NONE);
    }

    /**
     * Returns the parameter that this message wraps
     *
     * @return a non-null optional
     */
    public Optional<? extends HighlyStructuredLocalizableParameterValue> parameter() {
        return parameterCurrency.isPresent() ? parameterCurrency : parameterDateTime;
    }
}