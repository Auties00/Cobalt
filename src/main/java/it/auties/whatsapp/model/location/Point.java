package it.auties.whatsapp.model.location;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.DOUBLE;
import static it.auties.protobuf.base.ProtobufType.INT32;

/**
 * This model class describes a Point in space
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class Point implements ProtobufMessage {
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
