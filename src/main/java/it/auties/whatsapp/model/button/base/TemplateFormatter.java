package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredFourRowTemplate;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplate;
import it.auties.whatsapp.model.message.button.InteractiveMessage;
import it.auties.whatsapp.model.message.button.TemplateMessage;

/**
 * A formatter used to structure a button message
 */
public sealed interface TemplateFormatter permits HighlyStructuredFourRowTemplate, HydratedFourRowTemplate, InteractiveMessage {
    /**
     * Returns the type of this formatter
     *
     * @return a non-null type
     */
    Type templateType();

    /**
     * The constant of this enumerated type define the various of types of visual formats for a
     * {@link TemplateMessage}
     */
    @ProtobufEnum
    enum Type {
        /**
         * No format
         */
        NONE(0),
        /**
         * Four row template
         */
        FOUR_ROW(1),
        /**
         * Hydrated four row template
         */
        HYDRATED_FOUR_ROW(2),
        /**
         * Interactive message
         */
        INTERACTIVE(3);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }
    }
}
