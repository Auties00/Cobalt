package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.standard.ProductMessage;

/**
 * A model interface that represents a button message
 */
public sealed interface ButtonMessage extends Message permits ButtonsMessage, HighlyStructuredMessage, ListMessage, NativeFlowResponseMessage, TemplateMessage, ButtonReplyMessage, InteractiveMessage, ProductMessage {
    @Override
    default MessageCategory category() {
        return MessageCategory.BUTTON;
    }
}
