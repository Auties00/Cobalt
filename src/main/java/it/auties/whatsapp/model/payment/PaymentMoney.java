package it.auties.whatsapp.model.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

@ProtobufMessage(name = "Money")
public final class PaymentMoney {
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    final long money;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final int offset;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String currencyCode;

    PaymentMoney(long money, int offset, String currencyCode) {
        this.money = money;
        this.offset = offset;
        this.currencyCode = Objects.requireNonNull(currencyCode, "currencyCode cannot be null");
    }

    public long money() {
        return money;
    }

    public int offset() {
        return offset;
    }

    public String currencyCode() {
        return currencyCode;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PaymentMoney that
                && money == that.money
                && offset == that.offset
                && Objects.equals(currencyCode, that.currencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(money, offset, currencyCode);
    }

    @Override
    public String toString() {
        return "PaymentMoney[" +
                "money=" + money +
                ", offset=" + offset +
                ", currencyCode=" + currencyCode +
                ']';
    }
}