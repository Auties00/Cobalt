package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * A model class that describes an interactive annotation linked to a message
 */
@ProtobufMessage(name = "InteractiveAnnotation")
public final class InteractiveLocationAnnotation {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<InteractivePoint> polygonVertices;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final InteractiveLocation location;

    InteractiveLocationAnnotation(List<InteractivePoint> polygonVertices, InteractiveLocation location) {
        this.polygonVertices = Objects.requireNonNullElse(polygonVertices, List.of());
        this.location = location;
    }

    public List<InteractivePoint> polygonVertices() {
        return polygonVertices;
    }

    public InteractiveLocation location() {
        return location;
    }

    /**
     * Returns the type of sync
     *
     * @return a non-null Action
     */
    public Action type() {
        return location != null ? Action.LOCATION : Action.UNKNOWN;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InteractiveLocationAnnotation that
                && Objects.equals(polygonVertices, that.polygonVertices)
                && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(polygonVertices, location);
    }

    @Override
    public String toString() {
        return "InteractiveLocationAnnotation[" +
                "polygonVertices=" + polygonVertices + ", " +
                "location=" + location + ']';
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
    }
}