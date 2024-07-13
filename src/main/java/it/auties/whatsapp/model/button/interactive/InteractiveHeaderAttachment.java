package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;

/**
 * A sealed class that describes the various types of headers
 */
public sealed interface InteractiveHeaderAttachment permits DocumentMessage, ImageMessage, InteractiveHeaderThumbnail, VideoOrGifMessage {
    Type interactiveHeaderType();

    /**
     * The constants of this enumerated type describe the various types of attachment that a product
     * header can have
     */
    @ProtobufEnum
    enum Type {
        /**
         * No attachment
         */
        NONE(0),
        /**
         * Document message
         */
        DOCUMENT(3),
        /**
         * Image attachment
         */
        IMAGE(4),
        /**
         * Jpeg attachment
         */
        THUMBNAIL(6),
        /**
         * Video attachment
         */
        VIDEO(7);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}
