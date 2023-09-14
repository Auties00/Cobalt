package it.auties.whatsapp.model.payment;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;


public record PaymentMoney(
        @ProtobufProperty(index = 1, type = ProtobufType.INT64)
        long money,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        int offset,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        @NonNull
        String currencyCode
) implements ProtobufMessage {

}
