package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;

/**
 * A model that represents the title of a {@link HydratedFourRowTemplate}
 */
public sealed interface HydratedFourRowTemplateTitle permits DocumentMessage, HydratedFourRowTemplateTextTitle, ImageMessage, VideoOrGifMessage, LocationMessage {
    /**
     * Return the type of this title
     *
     * @return a non-null type
     */
    HydratedFourRowTemplateTitleType hydratedTitleType();
}
