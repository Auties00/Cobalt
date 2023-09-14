package it.auties.whatsapp.model.message.button;

import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;

/**
 * A model that represents the header of a {@link ButtonsMessage}
 */
public sealed interface ButtonsMessageHeader permits ButtonsMessageHeaderText, DocumentMessage, ImageMessage, LocationMessage, VideoOrGifMessage {
    ButtonsMessageHeaderType buttonHeaderType();
}
