package it.auties.whatsapp.model.message.standard;

import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;

/**
 * A model class that represents an empty message. Used to prevent NPEs from empty messages sent by
 * Whatsapp. Consider this a stub type.
 */
public final class EmptyMessage implements Message {
    @Override
    public MessageType type() {
        return MessageType.EMPTY;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}
