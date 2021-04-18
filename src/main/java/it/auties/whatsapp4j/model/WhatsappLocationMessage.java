package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.builder.WhatsappLocationMessageBuilder;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a location inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
@ToString
public final class WhatsappLocationMessage extends WhatsappUserMessage {
    /**
     * The coordinates of the location wrapped by this object
     */
    private final @NotNull WhatsappCoordinates coordinates;

    /**
     * The caption of the message wrapped by this object
     */
    private final @NotNull String caption;

    /**
     * The non encrypted thumbnail of the message wrapped by this object
     */
    private final byte @NotNull [] thumbnail;

    /**
     * Whether the location wrapped by this object is being updated in real time or not
     */
    private final boolean isLive;

    /**
     * The accuracy in meters of the coordinates of the location that this object wraps
     */
    private final int accuracy;

    /**
     * The speed in meters per second of the device that sent the location that this object wraps
     */
    private final float speed;

    /**
     * Constructs a WhatsappUserMessage from a raw protobuf object if it holds a location
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappLocationMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, info.getMessage().hasLiveLocationMessage() || info.getMessage().hasLocationMessage());
        var latitude = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getDegreesLatitude() : info.getMessage().getLocationMessage().getDegreesLatitude();
        var longitude = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getDegreesLongitude() : info.getMessage().getLocationMessage().getDegreesLongitude();
        var north = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getDegreesClockwiseFromMagneticNorth() : info.getMessage().getLocationMessage().getDegreesClockwiseFromMagneticNorth();
        this.coordinates = new WhatsappCoordinates(latitude, longitude, north);
        this.caption = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getCaption() : info.getMessage().getLocationMessage().getComment();
        this.thumbnail = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getJpegThumbnail().toByteArray() : info.getMessage().getLocationMessage().getJpegThumbnail().toByteArray();
        this.isLive = info.getMessage().hasLiveLocationMessage() || info.getMessage().getLocationMessage().getIsLive();
        this.accuracy = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getAccuracyInMeters() : info.getMessage().getLocationMessage().getAccuracyInMeters();
        this.speed = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getSpeedInMps() : info.getMessage().getLocationMessage().getSpeedInMps();
    }

    /**
     * Constructs a new {@link WhatsappLocationMessageBuilder} to build a new message that can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @return a non null WhatsappLocationMessageBuilder
     */
    public static @NotNull WhatsappLocationMessageBuilder newLocationMessage(){
        return new WhatsappLocationMessageBuilder();
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        var message = info.getMessage();
        if(message.hasLiveLocationMessage()){
            return message.getLiveLocationMessage().hasContextInfo() ? Optional.of(message.getLiveLocationMessage().getContextInfo()) : Optional.empty();
        }

        return message.getLocationMessage().hasContextInfo() ? Optional.of(message.getLocationMessage().getContextInfo()) : Optional.empty();
    }
}
