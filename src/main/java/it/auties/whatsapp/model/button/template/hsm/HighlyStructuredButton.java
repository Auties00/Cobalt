package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;

/**
 * A model that represents all types of hydrated buttons
 */
public sealed interface HighlyStructuredButton extends ProtobufMessage permits HighlyStructuredCallButton, HighlyStructuredQuickReplyButton, HighlyStructuredURLButton {
    /**
     * Returns the text of this button
     *
     * @return a non-null structure if the protobuf isn't corrupted
     */
    HighlyStructuredMessage text();

    /**
     * Returns the type of this button
     *
     * @return a non-null type
     */
    Type buttonType();

    /**
     * The constants of this enumerated type describe the various types of buttons that a template can
     * wrap
     */
    enum Type implements ProtobufEnum {
        /**
         * No button
         */
        NONE(0),
        /**
         * Quick reply button
         */
        QUICK_REPLY(1),
        /**
         * Url button
         */
        URL(2),
        /**
         * Call button
         */
        CALL(3);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}
