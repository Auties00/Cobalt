package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.button.ButtonsMessage;
import it.auties.whatsapp.model.message.button.HighlyStructuredMessage;
import it.auties.whatsapp.model.message.button.InteractiveMessage;
import it.auties.whatsapp.model.message.button.ListMessage;
import it.auties.whatsapp.model.message.button.NativeFlowResponseMessage;
import it.auties.whatsapp.model.message.button.TemplateMessage;
import it.auties.whatsapp.model.message.standard.ProductMessage;

/**
 * A model interface that represents a button message
 */
public sealed interface ButtonMessage
    extends Message
    permits ButtonsMessage, HighlyStructuredMessage, ListMessage, NativeFlowResponseMessage,
    TemplateMessage, ButtonReplyMessage, InteractiveMessage, ProductMessage {

  @Override
  default MessageCategory category() {
    return MessageCategory.BUTTON;
  }
}
