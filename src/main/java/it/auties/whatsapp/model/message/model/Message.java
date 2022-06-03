package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.whatsapp.model.message.standard.ProductMessage;

/**
 * A model interface that represents a WhatsappMessage sent by a contact or by Whatsapp.
 */
public sealed interface Message extends ProtobufMessage permits ContextualMessage, DeviceMessage,
        PaymentMessage, ServerMessage, ButtonMessage, ProductMessage {
}
