package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.model.ProtobufMessage;
import it.auties.whatsapp.model.message.standard.*;

/**
 * A model interface that represents a message sent by a contact or by Whatsapp.
 */
public sealed interface Message extends ProtobufMessage permits ButtonMessage, ContextualMessage, PaymentMessage, ServerMessage, CallMessage, EmptyMessage, KeepInChatMessage, NewsletterAdminInviteMessage, PollUpdateMessage, ReactionMessage {
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
