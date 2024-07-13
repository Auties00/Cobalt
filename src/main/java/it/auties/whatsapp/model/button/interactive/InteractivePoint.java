package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * This model class describes a Point in space
 */
@ProtobufMessage(name = "Point")
public record InteractivePoint(
        @ProtobufProperty(index = 1, type = ProtobufType.INT32)
        @Deprecated
        int xDeprecated,
        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
        @Deprecated
        int yDeprecated,
        @ProtobufProperty(index = 3, type = ProtobufType.DOUBLE)
        double x,
        @ProtobufProperty(index = 4, type = ProtobufType.DOUBLE)
        double y
) {

}
