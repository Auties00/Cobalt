package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.whatsapp.model.message.standard.EmptyMessage;
import it.auties.whatsapp.model.message.standard.ReactionMessage;

/**
 * A model interface that represents a message sent by a contact or by Whatsapp.
 */
public sealed interface Message extends ProtobufMessage
        permits ButtonMessage, ContextualMessage, DeviceMessage, PaymentMessage, ServerMessage, EmptyMessage, ReactionMessage {
    /**
     * Return message type
     *
     * @return a non-null message type
     */
    MessageType type();

    /**
     * Return message category
     *
     * @return a non-null message category
     */
    MessageCategory category();
}
