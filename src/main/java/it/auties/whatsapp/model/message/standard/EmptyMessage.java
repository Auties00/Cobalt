package it.auties.whatsapp.model.message.standard;

import it.auties.whatsapp.model.message.model.Message;

/**
 * A model class that represents an empty message. Used to prevent NPEs from empty messages sent by
 * Whatsapp. Consider this a stub type.
 */
public final class EmptyMessage implements Message {
    @Override
    public Type type() {
        return Type.EMPTY;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
    }
}
