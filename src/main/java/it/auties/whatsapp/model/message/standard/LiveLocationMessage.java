package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that represents a message holding a live location inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newLiveLocationMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class LiveLocationMessage extends ContextualMessage {
    /**
     * The latitude of the location that this message wraps
     */
    @ProtobufProperty(index = 1, type = DOUBLE)
    private Double latitude;

    /**
     * The longitude of the location that this message wraps
     */
    @ProtobufProperty(index = 2, type = DOUBLE)
    private Double longitude;

    /**
     * The accuracy in meters of the location that this message wraps
     */
    @ProtobufProperty(index = 3, type = UINT32)
    private Integer accuracy;

    /**
     * The speed in meters per second of the device that sent this live location message
     */
    @ProtobufProperty(index = 4, type = FLOAT)
    private Float speed;

    /**
     * Degrees Clockwise from Magnetic North
     */
    @ProtobufProperty(index = 5, type = UINT32)
    private Integer magneticNorthOffset;

    /**
     * The caption of this message
     */
    @ProtobufProperty(index = 6, type = STRING)
    private String caption;

    /**
     * This property probably refers to the number of updates that this live location message.
     */
    @ProtobufProperty(index = 7, type = UINT64)
    private Long sequenceNumber;

    /**
     * This offset probably refers to the endTimeStamp since the last update to this live location message.
     * In addition, it is measured in seconds since {@link java.time.Instant#EPOCH}.
     */
    @ProtobufProperty(index = 8, type = UINT32)
    private Integer timeOffset;

    /**
     * The thumbnail for this live location message encoded as jpeg in an array of bytes
     */
    @ProtobufProperty(index = 16, type = BYTES)
    private byte[] thumbnail;
}
