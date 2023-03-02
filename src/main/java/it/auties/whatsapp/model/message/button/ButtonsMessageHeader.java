package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.message.standard.*;

/**
 * A model that represents the header of a {@link ButtonsMessage}
 */
public sealed interface ButtonsMessageHeader extends ProtobufMessage permits DocumentMessage, ImageMessage, LocationMessage, TextMessage, VideoMessage {
}
