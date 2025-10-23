package com.github.auties00.cobalt.model.proto.button.template.highlyStructured;

import com.github.auties00.cobalt.model.proto.message.standard.DocumentMessage;
import com.github.auties00.cobalt.model.proto.message.standard.ImageMessage;
import com.github.auties00.cobalt.model.proto.message.standard.LocationMessage;
import com.github.auties00.cobalt.model.proto.message.standard.VideoOrGifMessage;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

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
    }
}
