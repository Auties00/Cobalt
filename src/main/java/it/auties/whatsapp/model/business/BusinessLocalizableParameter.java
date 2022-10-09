package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a time a localizable parameter
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newRawLocalizableParameterBuilder")
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
    private BusinessCurrency currencyParameter;

    /**
     * The time parameter
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = BusinessDateTime.class)
    private BusinessDateTime dateTimeParameter;

    /**
     * Constructs a new localizable parameter with a currency parameter
     *
     * @param defaultValue      the default value
     * @param currencyParameter the non-null currency
     * @return a non-null localizable parameter
     */
    public static BusinessLocalizableParameter of(String defaultValue, @NonNull BusinessCurrency currencyParameter) {
        return BusinessLocalizableParameter.newRawLocalizableParameterBuilder()
                .defaultValue(defaultValue)
                .currencyParameter(currencyParameter)
                .build();
    }

    /**
     * Constructs a new localizable parameter with a date time parameter
     *
     * @param defaultValue      the default value
     * @param dateTimeParameter the non-null date time
     * @return a non-null localizable parameter
     */
    public static BusinessLocalizableParameter of(String defaultValue, @NonNull BusinessDateTime dateTimeParameter) {
        return BusinessLocalizableParameter.newRawLocalizableParameterBuilder()
                .defaultValue(defaultValue)
                .dateTimeParameter(dateTimeParameter)
                .build();
    }

    /**
     * Returns the type of parameter that this message wraps
     *
     * @return a non-null parameter type
     */
    public ParameterType parameterType() {
        if (currencyParameter != null)
            return ParameterType.CURRENCY;
        if (dateTimeParameter != null)
            return ParameterType.DATE_TIME;
        return ParameterType.NONE;
    }

    /**
     * The constants of this enumerated type describe the various types of parameters that can be wrapped
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ParameterType implements ProtobufMessage {
        /**
         * No parameter
         */
        NONE(0),

        /**
         * Currency parameter
         */
        CURRENCY(2),

        /**
         * Date time parameter
         */
        DATE_TIME(3);

        @Getter
        private final int index;

        @JsonCreator
        public static ParameterType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(ParameterType.NONE);
        }
    }
}
