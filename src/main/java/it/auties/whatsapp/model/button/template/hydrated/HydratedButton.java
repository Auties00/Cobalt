package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * A model that represents all types of hydrated buttons
 */
public sealed interface HydratedButton permits HydratedCallButton, HydratedQuickReplyButton, HydratedURLButton {
    /**
     * Returns the text of this button
     *
     * @return a non-null string if the protobuf isn't corrupted
     */
    String text();

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
    @ProtobufEnum
    enum Type {
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
