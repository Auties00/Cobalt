package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.message.standard.*;

/**
 * A model that represents the title of a {@link HydratedFourRowTemplate}
 */
public sealed interface HydratedFourRowTemplateTitle extends ProtobufMessage permits DocumentMessage, TextMessage, ImageMessage, VideoMessage, LocationMessage {
    /**
     * Return the type of this title
     *
     * @return a non-null type
     */
    HydratedFourRowTemplateTitleType hydratedTitleType();
}
