package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.button.ButtonsResponseMessage;
import it.auties.whatsapp.model.message.button.ListResponseMessage;
import it.auties.whatsapp.model.message.button.TemplateReplyMessage;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A model interface that represents a reply to a button message
 */
@NoArgsConstructor
@SuperBuilder
public abstract sealed class ButtonReplyMessage
    extends ContextualMessage
    implements ButtonMessage
    permits ListResponseMessage, TemplateReplyMessage, ButtonsResponseMessage {

}
