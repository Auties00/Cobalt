package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a message holding a live location inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
@Accessors(fluent = true)
public final class LiveLocationMessage extends ContextualMessage {
    /**
     * The latitude of the location that this message wraps
     */
    @ProtobufProperty(index = 1, type = DOUBLE)
    private double latitude;

    /**
     * The longitude of the location that this message wraps
     */
    @ProtobufProperty(index = 2, type = DOUBLE)
    private double longitude;

    /**
     * The accuracy in meters of the location that this message wraps
     */
    @ProtobufProperty(index = 3, type = UINT32)
    private int accuracy;

    /**
     * The speed in meters per second of the device that sent this live location message
     */
    @ProtobufProperty(index = 4, type = FLOAT)
    private float speed;

    /**
     * Degrees Clockwise from Magnetic North
     */
    @ProtobufProperty(index = 5, type = UINT32)
    private int magneticNorthOffset;

    /**
     * The caption of this message
     */
    @ProtobufProperty(index = 6, type = STRING)
    private String caption;

    /**
     * This property probably refers to the numberWithoutPrefix of updates that this live location message.
     */
    @ProtobufProperty(index = 7, type = UINT64)
    private long sequenceNumber;

    /**
     * This offset probably refers to the seconds since the last update to this live location message.
     * In addition, it is measured in seconds since {@link java.time.Instant#EPOCH}.
     */
    @ProtobufProperty(index = 8, type = UINT32)
    private int timeOffset;

    /**
     * The thumbnail for this live location message encoded as jpeg in an array of bytes
     */
    @ProtobufProperty(index = 16, type = BYTES)
    private byte[] thumbnail;

    @Override
    public MessageType type() {
        return MessageType.LIVE_LOCATION;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}