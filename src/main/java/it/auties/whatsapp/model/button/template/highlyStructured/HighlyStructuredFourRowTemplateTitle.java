package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;

/**
 * A model that represents the title of a {@link HighlyStructuredFourRowTemplate}
 */
public sealed interface HighlyStructuredFourRowTemplateTitle permits DocumentMessage, HighlyStructuredMessage, ImageMessage, VideoOrGifMessage, LocationMessage {
    /**
     * Return the type of this title
     *
     * @return a non-null type
     */
    Type titleType();

    /**
     * The constants of this enumerated type describe the various types of title that a template can
     * have
     */
    @ProtobufEnum
    enum Type {
        /**
         * No title
         */
        NONE(0),
        /**
         * Document title
         */
        DOCUMENT(1),
        /**
         * Highly structured message title
         */
        HIGHLY_STRUCTURED(2),
        /**
         * Image title
         */
        IMAGE(3),
        /**
         * Video title
         */
        VIDEO(4),
        /**
         * Location title
         */
        LOCATION(5);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}
