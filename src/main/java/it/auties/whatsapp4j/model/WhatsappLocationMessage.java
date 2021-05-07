package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
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
    private final @NotNull WhatsappLocationCoordinates coordinates;

    /**
     * The caption of the message wrapped by this object
     */
    private final String caption;

    /**
     * The non encrypted thumbnail of the message wrapped by this object
     */
    private final byte [] thumbnail;

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
        this.coordinates = new WhatsappLocationCoordinates(latitude, longitude, north);
        this.caption = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getCaption() : info.getMessage().getLocationMessage().getComment();
        this.thumbnail = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getJpegThumbnail().toByteArray() : info.getMessage().getLocationMessage().getJpegThumbnail().toByteArray();
        this.isLive = info.getMessage().hasLiveLocationMessage() || info.getMessage().getLocationMessage().getIsLive();
        this.accuracy = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getAccuracyInMeters() : info.getMessage().getLocationMessage().getAccuracyInMeters();
        this.speed = info.getMessage().hasLiveLocationMessage() ? info.getMessage().getLiveLocationMessage().getSpeedInMps() : info.getMessage().getLocationMessage().getSpeedInMps();
    }

    /**
     * Constructs a new builder to create a WhatsappLocationMessage that wraps an image.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat          the non null chat to which the new message should belong
     * @param coordinates   the non null coordinates of the new message
     * @param speed         the speed in meters per second of the device that sent the new message, by default not defined
     * @param accuracy      the accuracy in meters of the coordinates that the new message wraps, by default not defined
     * @param caption       the caption of the new message, by default empty
     * @param thumbnail     the thumbnail of the new message, by default empty
     * @param quotedMessage the message that the new message should quote, by default empty
     * @param forwarded     whether this message is forwarded or not, by default false
     */
    @Builder(builderMethodName = "newLocationMessage", buildMethodName = "create")
    public WhatsappLocationMessage(@NotNull(message = "Cannot create a WhatsappLocationMessage with no chat") WhatsappChat chat, @NotNull(message = "Cannot create a WhatsappLocationMessage with no coordinates") WhatsappLocationCoordinates coordinates, Float speed, Integer accuracy, byte[] thumbnail, String caption, WhatsappUserMessage quotedMessage, List<WhatsappContact> captionMentions, boolean forwarded) {
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createLocationMessage(coordinates, caption, thumbnail, accuracy, speed, quotedMessage, captionMentions, forwarded), chat.jid()));
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

    /**
     * Returns an optional String representing the caption of this location message
     *
     * @return a non empty optional if this message has a caption
     */
    public @NotNull Optional<String> caption(){
        return caption.isBlank() ? Optional.empty() : Optional.of(caption);
    }

    /**
     * Returns an optional String representing the jpeg thumbnail of this location message
     *
     * @return a non empty optional if this message has a thumbnail
     */
    public @NotNull Optional<byte[]> thumbnail(){
        return thumbnail.length == 0 ? Optional.empty() : Optional.of(thumbnail);
    }

    /**
     * Returns an optional Integer representing the accuracy in meters of the coordinates of this location message
     *
     * @return a non empty optional if this message has an accuracy
     */
    public @NotNull Optional<Integer> accuracy(){
        return accuracy == 0 ? Optional.empty() : Optional.of(accuracy);
    }

    /**
     * Returns an optional Float representing the speed in meters per second of the device that sent this location message
     *
     * @return a non empty optional if this message has a speed
     */
    public @NotNull Optional<Float> speed(){
        return speed == 0 ? Optional.empty() : Optional.of(speed);
    }
}
