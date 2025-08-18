package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Arrays;
import java.util.Optional;


/**
 * A model class that represents a message holding a live location inside
 */
@ProtobufMessage(name = "Message.LiveLocationMessage")
public final class LiveLocationMessage implements ContextualMessage<LiveLocationMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
    private final double latitude;
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    private final double longitude;
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    private final int accuracy;
    @ProtobufProperty(index = 4, type = ProtobufType.FLOAT)
    private final float speed;
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    private final int magneticNorthOffset;
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    private final String caption;
    @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
    private final long sequenceNumber;
    @ProtobufProperty(index = 8, type = ProtobufType.UINT32)
    private final int timeOffset;
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    private final byte[] thumbnail;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    public LiveLocationMessage(double latitude, double longitude, int accuracy, float speed, int magneticNorthOffset, String caption, long sequenceNumber, int timeOffset, byte[] thumbnail, ContextInfo contextInfo) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.speed = speed;
        this.magneticNorthOffset = magneticNorthOffset;
        this.caption = caption;
        this.sequenceNumber = sequenceNumber;
        this.timeOffset = timeOffset;
        this.thumbnail = thumbnail;
        this.contextInfo = contextInfo;
    }

    @Override
    public MessageType type() {
        return MessageType.LIVE_LOCATION;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public int accuracy() {
        return accuracy;
    }

    public float speed() {
        return speed;
    }

    public int magneticNorthOffset() {
        return magneticNorthOffset;
    }

    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    public long sequenceNumber() {
        return sequenceNumber;
    }

    public int timeOffset() {
        return timeOffset;
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public LiveLocationMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "LiveLocationMessage[" +
                "latitude=" + latitude + ", " +
                "longitude=" + longitude + ", " +
                "accuracy=" + accuracy + ", " +
                "speed=" + speed + ", " +
                "magneticNorthOffset=" + magneticNorthOffset + ", " +
                "caption=" + caption + ", " +
                "sequenceNumber=" + sequenceNumber + ", " +
                "timeOffset=" + timeOffset + ", " +
                "thumbnail=" + Arrays.toString(thumbnail) + ", " +
                "contextInfo=" + contextInfo + ']';
    }
}