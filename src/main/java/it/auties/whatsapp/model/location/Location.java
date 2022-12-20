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
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * This model class describes a Location
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class Location
        implements ProtobufMessage {
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
