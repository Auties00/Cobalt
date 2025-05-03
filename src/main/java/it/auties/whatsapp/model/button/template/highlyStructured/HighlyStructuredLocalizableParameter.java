package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a time a localizable parameter
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter")
public final class HighlyStructuredLocalizableParameter {

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String defaultValue;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final HighlyStructuredCurrency parameterCurrency;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final HighlyStructuredDateTime parameterDateTime;

    HighlyStructuredLocalizableParameter(String defaultValue, HighlyStructuredCurrency parameterCurrency, HighlyStructuredDateTime parameterDateTime) {
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue cannot be null");
        this.parameterCurrency = parameterCurrency;
        this.parameterDateTime = parameterDateTime;
    }

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
            case null -> {}
        }
        return builder.build();
    }

    public String defaultValue() {
        return defaultValue;
    }

    public Optional<HighlyStructuredCurrency> parameterCurrency() {
        return Optional.ofNullable(parameterCurrency);
    }

    public Optional<HighlyStructuredDateTime> parameterDateTime() {
        return Optional.ofNullable(parameterDateTime);
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
        if(parameterCurrency != null) {
            return Optional.of(parameterCurrency);
        }else if (parameterDateTime != null) {
            return Optional.of(parameterDateTime);
        }else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredLocalizableParameter that
                && Objects.equals(defaultValue, that.defaultValue)
                && Objects.equals(parameterCurrency, that.parameterCurrency)
                && Objects.equals(parameterDateTime, that.parameterDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultValue, parameterCurrency, parameterDateTime);
    }

    @Override
    public String toString() {
        return "HighlyStructuredLocalizableParameter[" +
                "defaultValue=" + defaultValue + ", " +
                "parameterCurrency=" + parameterCurrency + ", " +
                "parameterDateTime=" + parameterDateTime + ']';
    }
}