package it.auties.whatsapp.protobuf.message.model;

import it.auties.whatsapp.protobuf.message.button.*;

/**
 * A model interface that represents a WhatsappMessage regarding a payment
 */
public sealed interface ButtonMessage extends Message permits ButtonsMessage, ButtonsResponseMessage, ListMessage, ListResponseMessage, StructuredButtonMessage, TemplateButtonReplyMessage, TemplateMessage {
}
