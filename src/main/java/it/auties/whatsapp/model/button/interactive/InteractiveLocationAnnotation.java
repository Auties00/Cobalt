package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * A model class that describes an interactive annotation linked to a message
 */
@ProtobufMessage(name = "InteractiveAnnotation")
public record InteractiveLocationAnnotation(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        List<InteractivePoint> polygonVertices,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        InteractiveLocation location
) {
    /**
     * Returns the type of sync
     *
     * @return a non-null Action
     */
    public Action type() {
        return location != null ? Action.LOCATION : Action.UNKNOWN;
    }

    /**
     * The constants of this enumerated type describe the various types of sync that an interactive
     * annotation can provide
     */
    @ProtobufEnum
    public enum Action {
        /**
         * Unknown
         */
        UNKNOWN(0),
        /**
         * Location
         */
        LOCATION(2);

        final int index;

        Action(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}