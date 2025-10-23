package com.github.auties00.cobalt.model.proto.message.standard;

import com.github.auties00.cobalt.model.proto.message.model.Message;

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
