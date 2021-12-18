package it.auties.whatsapp.protobuf.message.model;

import it.auties.whatsapp.protobuf.message.standard.ProductMessage;

/**
 * A model interface that represents a WhatsappMessage sent by a contact or by Whatsapp.
 */
public sealed interface Message permits ContextualMessage, DeviceMessage, PaymentMessage, ServerMessage, ButtonMessage, ProductMessage {
}