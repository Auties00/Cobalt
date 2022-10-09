package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that describes an interactive annotation linked to a message
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newInteractiveAnnotation")
@Jacksonized
@Accessors(fluent = true)
public class InteractiveAnnotation implements ProtobufMessage {
    /**
     * Polygon vertices
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = Point.class, repeated = true)
    private List<Point> polygonVertices;

    /**
     * Location
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = Location.class)
    private Location location;

    /**
     * Returns the type of sync
     *
     * @return a non-null Action
     */
    public Action type() {
        return location != null ?
                Action.LOCATION :
                Action.UNKNOWN;
    }

    /**
     * The constants of this enumrated type describe the various types of sync that an interactive annotation can provide
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum Action implements ProtobufMessage {
        /**
         * Unknown
         */
        UNKNOWN(0),

        /**
         * Location
         */
        LOCATION(2);

        @Getter
        private final int index;

        @JsonCreator
        public static Action forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(Action.UNKNOWN);
        }
    }

    /**
     * This model class describes a Point in space
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Jacksonized
    @Builder
    @Accessors(fluent = true)
    public static class Point implements ProtobufMessage {
        /**
         * X coordinate, deprecated
         *
         * @deprecated use {@link Point#x instead}
         */
        @ProtobufProperty(index = 1, type = INT32)
        @Deprecated
        private int xDeprecated;

        /**
         * Y coordinate, deprecated
         *
         * @deprecated use {@link Point#y instead}
         */
        @ProtobufProperty(index = 2, type = INT32)
        @Deprecated
        private int yDeprecated;

        /**
         * X coordinate
         */
        @ProtobufProperty(index = 3, type = DOUBLE)
        private Double x;

        /**
         * Y coordinate
         */
        @ProtobufProperty(index = 4, type = DOUBLE)
        private Double y;
    }

    /**
     * This model class describes a Location
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Jacksonized
    @Builder
    @Accessors(fluent = true)
    public static class Location implements ProtobufMessage {
        /**
         * The latitude of this location, in degrees
         */
        @ProtobufProperty(index = 1, type = DOUBLE)
        private Double latitude;

        /**
         * The longitude of this location, in degrees
         */
        @ProtobufProperty(index = 2, type = DOUBLE)
        private Double longitude;

        /**
         * The name of this location
         */
        @ProtobufProperty(index = 3, type = STRING)
        private String name;
    }

    public static class InteractiveAnnotationBuilder {
        public InteractiveAnnotationBuilder polygonVertices(List<Point> polygonVertices) {
            if (this.polygonVertices == null)
                this.polygonVertices = new ArrayList<>();
            this.polygonVertices.addAll(polygonVertices);
            return this;
        }
    }
}
