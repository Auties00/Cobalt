package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.business.*;

/**
 * A model interface that represents a button message
 */
public sealed interface BusinessMessage extends Message
        permits InteractiveMessage, ProductMessage, ListMessage, ListResponseMessage, HighlyStructuredMessage,
        TemplateMessage, TemplateReplyMessage, ButtonsMessage, ButtonsResponseMessage {
}
