package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.whatsapp.model.button.interactive.InteractiveCollection;
import it.auties.whatsapp.model.button.interactive.InteractiveNativeFlow;
import it.auties.whatsapp.model.button.interactive.InteractiveShop;

/**
 * A model class that represents a message that can be used as the content of a {@link it.auties.whatsapp.model.message.button.InteractiveMessage}
 */
public sealed interface InteractiveMessageContent extends ProtobufMessage permits InteractiveShop, InteractiveCollection, InteractiveNativeFlow {
    /**
     * Returns the type of this content
     *
     * @return a non-null type
     */
    Type contentType();

    /**
     * The constants of this enumerated type describe the various types of content that an interactive
     * message can wrap
     */
    enum Type implements ProtobufEnum {
        /**
         * No content
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

        public int index() {
            return this.index;
        }
    }
}
