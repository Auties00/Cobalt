package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.whatsapp.model.message.button.HighlyStructuredMessage;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;

/**
 * A model that represents the title of a {@link HighlyStructuredFourRowTemplate}
 */
public sealed interface HighlyStructuredFourRowTemplateTitle extends ProtobufMessage permits DocumentMessage, HighlyStructuredMessage, ImageMessage, VideoMessage, LocationMessage {
    /**
     * Return the type of this title
     *
     * @return a non-null type
     */
    HighlyStructuredFourRowTemplateTitleType titleType();
}
