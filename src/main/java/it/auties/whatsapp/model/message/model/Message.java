package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.whatsapp.model.message.standard.ReactionMessage;

/**
 * A model interface that represents a message sent by a contact or by Whatsapp.
 */
public sealed interface Message extends ProtobufMessage
        permits ContextualMessage, DeviceMessage, PaymentMessage, ServerMessage, ButtonMessage, BusinessMessage,
        ReactionMessage {
}
