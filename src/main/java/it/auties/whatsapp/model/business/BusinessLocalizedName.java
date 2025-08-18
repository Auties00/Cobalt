package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents a time a localizable name
 */
@ProtobufMessage(name = "LocalizedName")
public record BusinessLocalizedName(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String lg,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String lc,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String name
) {
}