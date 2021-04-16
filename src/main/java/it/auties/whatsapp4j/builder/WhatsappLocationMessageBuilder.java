package it.auties.whatsapp4j.builder;

import it.auties.whatsapp4j.model.WhatsappCoordinates;
import it.auties.whatsapp4j.model.WhatsappLocationMessage;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import jakarta.validation.constraints.NotNull;


import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Accessors(fluent = true)
public class WhatsappLocationMessageBuilder extends WhatsappMessageBuilder<WhatsappLocationMessage> {
    /**
     * The coordinates of the location wrapped by this object
     */
    private  @Setter WhatsappCoordinates coordinates;

    /**
     * The caption of the message wrapped by this object
     */
    private  @Setter String caption;

    /**
     * The non encrypted thumbnail of the message wrapped by this object
     */
    private @Setter byte  [] thumbnail;

    /**
     * Whether the location wrapped by this object is being updated in real time or not
     */
    private @Setter boolean live;

    /**
     * The accuracy in meters of the coordinates of the location that this object wraps
     */
    private @Setter int accuracy;

    /**
     * The speed in meters per second of the device that sent the location that this object wraps
     */
    private @Setter float speed;

    /**
     * Builds a {@link WhatsappLocationMessage} from the data provided
     *
     * @return a non null WhatsappLocationMessage
     */
    @Override
    public @NotNull WhatsappLocationMessage create() {
        Objects.requireNonNull(chat, "WhatsappAPI: Cannot create a WhatsappLocation with a null chat");
        Objects.requireNonNull(coordinates, "WhatsappAPI: Cannot create a WhatsappLocation with null coordinates");
        return new WhatsappLocationMessage(ProtobufUtils.createMessageInfo(ProtobufUtils.createLocationMessage(coordinates, caption, thumbnail, live, accuracy, speed), chat.jid()));
    }
}
