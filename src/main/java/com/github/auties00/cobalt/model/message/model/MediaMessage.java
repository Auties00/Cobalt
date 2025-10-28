package com.github.auties00.cobalt.model.message.model;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.media.MediaPath;
import com.github.auties00.cobalt.model.media.MediaProvider;
import com.github.auties00.cobalt.model.message.payment.PaymentInvoiceMessage;
import com.github.auties00.cobalt.model.message.standard.*;
import com.github.auties00.cobalt.model.message.standard.*;
import com.github.auties00.cobalt.model.message.standard.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A media message
 * Read its content using {@link WhatsAppClient#downloadMedia(MediaMessage)}
 */
public sealed interface MediaMessage
        extends ContextualMessage, MediaProvider
        permits PaymentInvoiceMessage, AudioMessage, DocumentMessage, ImageMessage, StickerMessage, VideoOrGifMessage {
    /**
     * Returns the timestampSeconds, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for{@link MediaMessage#mediaKey()}
     *
     * @return an unsigned long
     */
    OptionalLong mediaKeyTimestampSeconds();

    /**
     * Returns the timestampSeconds for{@link MediaMessage#mediaKey()}
     *
     * @return a zoned date time
     */
    Optional<ZonedDateTime> mediaKeyTimestamp();

    /**
     * Returns the media type of the media that this object wraps
     *
     * @return a non-null {@link Type}
     */
    Type mediaType();

    @Override
    default Message.Category category() {
        return Message.Category.MEDIA;
    }

    @Override
    default Message.Type type() {
        return mediaType().toMessageType();
    }

    @Override
    default MediaPath mediaPath() {
        return mediaType().toAttachmentType();
    }

    /**
     * The constants of this enumerated type describe the various types of media type that a
     * {@link MediaMessage} can hold
     */
    enum Type {
        /**
         * No media
         */
        NONE("", "", Message.Type.EMPTY, MediaPath.NONE),
        /**
         * The message is an image
         */
        IMAGE("jpg", "image/jpeg", Message.Type.IMAGE, MediaPath.IMAGE),
        /**
         * The message is a document
         */
        DOCUMENT("", "application/octet-stream", Message.Type.DOCUMENT, MediaPath.DOCUMENT),
        /**
         * The message is an audio
         */
        AUDIO("mp3", "audio/mpeg", Message.Type.AUDIO, MediaPath.AUDIO),
        /**
         * The message is a video
         */
        VIDEO("mp4", "video/mp4", Message.Type.VIDEO, MediaPath.VIDEO),
        /**
         * The message is a sticker
         */
        STICKER("webp", "image/webp", Message.Type.STICKER, MediaPath.IMAGE);

        /**
         * The default extension for this enumerated type. Might be right, might be wrong, who knows.
         */
        private final String extension;

        /**
         * The default mime type for this enumerated type. Might be right, might be wrong, who knows.
         */
        private final String mimeType;

        /**
         * The message type for this media
         */
        private final Message.Type messageType;

        /**
         * The attachment type for this media
         */
        private final MediaPath mediaPath;

        Type(String extension, String mimeType, Message.Type messageType, MediaPath mediaPath) {
            this.extension = extension;
            this.mimeType = mimeType;
            this.messageType = messageType;
            this.mediaPath = mediaPath;
        }

        /**
         * The message type for this media
         *
         * @return a message type
         */
        public Message.Type toMessageType() {
            return messageType;
        }

        /**
         * The attachment type for this media
         *
         * @return an attachment type
         */
        public MediaPath toAttachmentType() {
            return mediaPath;
        }

        /**
         * Returns the extension of this media
         *
         * @return a value
         */
        public String extension() {
            return extension;
        }

        /**
         * Returns the mime type of this media
         *
         * @return a value
         */
        public String mimeType() {
            return this.mimeType;
        }
    }
}
