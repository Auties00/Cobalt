package com.github.auties00.cobalt.model.proto.message.button;

import com.github.auties00.cobalt.model.proto.message.standard.DocumentMessage;
import com.github.auties00.cobalt.model.proto.message.standard.ImageMessage;
import com.github.auties00.cobalt.model.proto.message.standard.LocationMessage;
import com.github.auties00.cobalt.model.proto.message.standard.VideoOrGifMessage;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

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

        public boolean hasMedia() {
            return this == DOCUMENT
                    || this == IMAGE
                    || this == VIDEO;
        }
    }
}
