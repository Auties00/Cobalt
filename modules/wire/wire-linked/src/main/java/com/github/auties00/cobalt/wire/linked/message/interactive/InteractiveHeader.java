package com.github.auties00.cobalt.wire.linked.message.interactive;

import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.message.media.VideoMessage;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * Represents the top portion of an interactive message, displayed above the body text.
 *
 * <p>The header can be either a simple piece of {@link Text} or a media preview that invites
 * the recipient to interact with the message. The supported header kinds are:
 * <ul>
 *   <li>{@link Text} a plain text caption</li>
 *   <li>{@link DocumentMessage} a document preview with filename and page count</li>
 *   <li>{@link ImageMessage} a still image preview</li>
 *   <li>{@link VideoMessage} a video preview with thumbnail and duration</li>
 *   <li>{@link LocationMessage} a map snippet with coordinates</li>
 * </ul>
 *
 * <p>Only one header kind can be present on a given interactive message.
 */
public sealed interface InteractiveHeader permits InteractiveHeader.Text, DocumentMessage, ImageMessage, VideoMessage, LocationMessage {

    /**
     * Wraps a plain text value used as the header of an interactive message.
     *
     * <p>This variant is serialized as a single {@code string} field by the protobuf layer
     * and rendered as the top caption of the surrounding message.
     */
    final class Text implements InteractiveHeader {
        /**
         * The literal header text shown to the recipient.
         */
        String text;

        /**
         * Constructs a new text header with the given value.
         *
         * @param text the literal header text
         */
        Text(String text) {
            this.text = text;
        }

        /**
         * Returns the literal header text.
         *
         * @return the text that will appear at the top of the interactive message
         */
        @ProtobufSerializer
        public String text() {
            return text;
        }

        /**
         * Creates a new text header from the supplied string.
         *
         * <p>This factory is invoked by the protobuf deserialization layer when materializing
         * an interactive message that carries a plain text header.
         *
         * @param text the literal header text
         * @return a new text header wrapping the string
         */
        @ProtobufDeserializer
        public static Text of(String text) {
            return new Text(text);
        }
    }
}
