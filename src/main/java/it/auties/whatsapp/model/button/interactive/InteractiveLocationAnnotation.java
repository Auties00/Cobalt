package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * A model class that describes an interactive annotation linked to a message
 */
@ProtobufMessageName("InteractiveAnnotation")
public record InteractiveLocationAnnotation(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        List<InteractivePoint> polygonVertices,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        InterativeLocation location
) implements ProtobufMessage {
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
    public enum Action implements ProtobufEnum {
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