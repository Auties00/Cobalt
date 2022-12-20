package it.auties.whatsapp.model.payment;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class PaymentMoney
        implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = INT64)
    private long money;

    @ProtobufProperty(index = 2, type = UINT32)
    private int offset;

    @ProtobufProperty(index = 3, type = STRING)
    private String currencyCode;
}
