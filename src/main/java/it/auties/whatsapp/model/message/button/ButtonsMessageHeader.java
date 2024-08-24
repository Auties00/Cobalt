package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;

/**
 * A model that represents the header of a {@link ButtonsMessage}
 */
public sealed interface ButtonsMessageHeader permits ButtonsMessageHeaderText, DocumentMessage, ImageMessage, LocationMessage, VideoOrGifMessage {
    Type buttonHeaderType();

    /**
     * The constants of this enumerated type describe the various types of headers that a {@link ButtonsMessage} can have
     */
    @ProtobufEnum
    enum Type {
        /**
         * Unknown
         */
        UNKNOWN(0),
        /**
         * Empty
         */
        EMPTY(1),
        /**
         * Text message
         */
        TEXT(2),
        /**
         * Document message
         */
        DOCUMENT(3),
        /**
         * Image message
         */
        IMAGE(4),
        /**
         * Video message
         */
        VIDEO(5),
        /**
         * Location message
         */
        LOCATION(6);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        public boolean hasMedia() {
            return this == DOCUMENT
                    || this == IMAGE
                    || this == VIDEO;
        }
    }
}
