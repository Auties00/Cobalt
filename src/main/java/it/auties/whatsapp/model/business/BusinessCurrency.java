package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a currency
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessCurrency implements ProtobufMessage {
    /**
     * The currency code
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String currencyCode;

    /**
     * The amount
     */
    @ProtobufProperty(index = 2, type = INT64)
    private long amount1000;
}
