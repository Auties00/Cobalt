package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

/**
 * This model class describes a Location
 */
@ProtobufMessageName("Location")
public record InterativeLocation(
        @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
        double latitude,
        @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
        double longitude,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String name
) implements ProtobufMessage {

}
