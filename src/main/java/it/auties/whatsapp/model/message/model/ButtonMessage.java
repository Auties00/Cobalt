package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.button.*;

/**
 * A model interface that represents a button message
 */
public sealed interface ButtonMessage extends Message
        permits ButtonsMessage, ButtonsResponseMessage, ButtonListMessage, ButtonListResponseMessage,
        ButtonStructureMessage, ButtonTemplateReplyMessage, ButtonTemplateMessage {
}
