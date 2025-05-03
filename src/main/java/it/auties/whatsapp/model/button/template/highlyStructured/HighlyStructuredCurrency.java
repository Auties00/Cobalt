package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents a currency
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMCurrency")
public final class HighlyStructuredCurrency implements HighlyStructuredLocalizableParameterValue {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String currencyCode;

    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final long amount1000;

    HighlyStructuredCurrency(String currencyCode, long amount1000) {
        this.currencyCode = Objects.requireNonNull(currencyCode, "currencyCode cannot be null");
        this.amount1000 = amount1000;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public long amount1000() {
        return amount1000;
    }

    @Override
    public Type parameterType() {
        return Type.CURRENCY;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredCurrency that
                && Objects.equals(currencyCode, that.currencyCode)
                && amount1000 == that.amount1000;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyCode, amount1000);
    }

    @Override
    public String toString() {
        return "HighlyStructuredCurrency[" +
                "currencyCode=" + currencyCode + ", " +
                "amount1000=" + amount1000 + ']';
    }
}