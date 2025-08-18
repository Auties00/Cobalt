package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.button.ButtonsResponseMessage;
import it.auties.whatsapp.model.message.button.ListResponseMessage;
import it.auties.whatsapp.model.message.button.TemplateReplyMessage;

/**
 * A model interface that represents a reply to a button message
 */
public sealed interface ButtonReplyMessage<T extends ButtonReplyMessage<T>> extends ContextualMessage, ButtonMessage permits ListResponseMessage, TemplateReplyMessage, ButtonsResponseMessage {

}
