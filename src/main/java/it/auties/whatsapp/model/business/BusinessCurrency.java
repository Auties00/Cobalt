package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents a currency
 */
public record BusinessCurrency(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String currencyCode,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long amount1000
) implements BusinessLocalizableParameterValue {
    @Override
    public BusinessLocalizableParameterType parameterType() {
        return BusinessLocalizableParameterType.CURRENCY;
    }
}
