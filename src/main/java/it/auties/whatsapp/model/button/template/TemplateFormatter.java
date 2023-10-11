package it.auties.whatsapp.model.button.template;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.whatsapp.model.button.template.hsm.HighlyStructuredFourRowTemplate;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplate;
import it.auties.whatsapp.model.message.button.InteractiveMessage;
import it.auties.whatsapp.model.message.button.TemplateMessage;

/**
 * A formatter used to structure a button message
 */
public sealed interface TemplateFormatter extends ProtobufMessage permits HighlyStructuredFourRowTemplate, HydratedFourRowTemplate, InteractiveMessage {
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
    enum Type implements ProtobufEnum {
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
