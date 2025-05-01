package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredFourRowTemplateTitle;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplateTitle;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessageHeader;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Arrays;
import java.util.Optional;


/**
 * A model class that represents a message holding a location inside
 */
@ProtobufMessage(name = "Message.LocationMessage")
public final class LocationMessage implements ContextualMessage<LocationMessage>, ButtonsMessageHeader, HighlyStructuredFourRowTemplateTitle, HydratedFourRowTemplateTitle {
    @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
    private final double latitude;
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    private final double longitude;
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    private final String name;
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    private final String address;
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    private final String url;
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    private final boolean live;
    @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
    private final int accuracy;
    @ProtobufProperty(index = 8, type = ProtobufType.FLOAT)
    private final float speed;
    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    private final int magneticNorthOffset;
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    private final String caption;
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    private final byte[] thumbnail;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    public LocationMessage(double latitude, double longitude, String name, String address, String url, boolean live, int accuracy, float speed, int magneticNorthOffset, String caption, byte[] thumbnail, ContextInfo contextInfo) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.address = address;
        this.url = url;
        this.live = live;
        this.accuracy = accuracy;
        this.speed = speed;
        this.magneticNorthOffset = magneticNorthOffset;
        this.caption = caption;
        this.thumbnail = thumbnail;
        this.contextInfo = contextInfo;
    }

    @Override
    public MessageType type() {
        return MessageType.LOCATION;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    @Override
    public HighlyStructuredFourRowTemplateTitle.Type titleType() {
        return HighlyStructuredFourRowTemplateTitle.Type.LOCATION;
    }

    @Override
    public HydratedFourRowTemplateTitle.Type hydratedTitleType() {
        return HydratedFourRowTemplateTitle.Type.LOCATION;
    }

    @Override
    public ButtonsMessageHeader.Type buttonHeaderType() {
        return ButtonsMessageHeader.Type.LOCATION;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    public boolean live() {
        return live;
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

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public LocationMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "LocationMessage[" +
                "latitude=" + latitude + ", " +
                "longitude=" + longitude + ", " +
                "name=" + name + ", " +
                "address=" + address + ", " +
                "url=" + url + ", " +
                "live=" + live + ", " +
                "accuracy=" + accuracy + ", " +
                "speed=" + speed + ", " +
                "magneticNorthOffset=" + magneticNorthOffset + ", " +
                "caption=" + caption + ", " +
                "thumbnail=" + Arrays.toString(thumbnail) + ", " +
                "contextInfo=" + contextInfo + ']';
    }
}