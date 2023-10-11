package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents a currency
 */
@ProtobufMessageName("Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMCurrency")
public record HighlyStructuredCurrency(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String currencyCode,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long amount1000
) implements HighlyStructuredLocalizableParameterValue {
    @Override
    public HighlyStructuredLocalizableParameterValue.Type parameterType() {
        return HighlyStructuredLocalizableParameterValue.Type.CURRENCY;
    }
}
