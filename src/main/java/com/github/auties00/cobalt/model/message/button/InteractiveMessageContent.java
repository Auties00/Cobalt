package com.github.auties00.cobalt.model.message.button;

import com.github.auties00.cobalt.model.button.interactive.InteractiveCollection;
import com.github.auties00.cobalt.model.button.interactive.InteractiveNativeFlow;
import com.github.auties00.cobalt.model.button.interactive.InteractiveShop;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * A model class that represents a message that can be used as the children of a {@link InteractiveMessage}
 */
public sealed interface InteractiveMessageContent permits InteractiveShop, InteractiveCollection, InteractiveNativeFlow {
    /**
     * Returns the type of this children
     *
     * @return a non-null type
     */
    Type contentType();

    /**
     * The constants of this enumerated type describe the various types of children that an interactive
     * message can wrap
     */
    @ProtobufEnum
    enum Type {
        /**
         * No children
         */
        NONE(0),
        /**
         * Shop
         */
        SHOP(1),
        /**
         * Collection
         */
        COLLECTION(2),
        /**
         * Native flow
         */
        NATIVE_FLOW(3);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}
