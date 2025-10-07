package com.github.auties00.cobalt.model.message.model;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.model.media.AttachmentType;
import com.github.auties00.cobalt.model.media.MutableAttachmentProvider;
import com.github.auties00.cobalt.model.message.payment.PaymentInvoiceMessage;
import com.github.auties00.cobalt.model.message.standard.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A media message
 * Read its content using {@link Whatsapp#downloadMedia(MediaMessage)}
 */
public sealed interface MediaMessage
        extends ContextualMessage, MutableAttachmentProvider
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
    default AttachmentType attachmentType() {
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
        NONE("", "", Message.Type.EMPTY, AttachmentType.NONE),
        /**
         * The message is an image
         */
        IMAGE("jpg", "image/jpeg", Message.Type.IMAGE, AttachmentType.IMAGE),
        /**
         * The message is a document
         */
        DOCUMENT("", "application/octet-stream", Message.Type.DOCUMENT, AttachmentType.DOCUMENT),
        /**
         * The message is an audio
         */
        AUDIO("mp3", "audio/mpeg", Message.Type.AUDIO, AttachmentType.AUDIO),
        /**
         * The message is a video
         */
        VIDEO("mp4", "video/mp4", Message.Type.VIDEO, AttachmentType.VIDEO),
        /**
         * The message is a sticker
         */
        STICKER("webp", "image/webp", Message.Type.STICKER, AttachmentType.IMAGE);

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
        private final AttachmentType attachmentType;

        Type(String extension, String mimeType, Message.Type messageType, AttachmentType attachmentType) {
            this.extension = extension;
            this.mimeType = mimeType;
            this.messageType = messageType;
            this.attachmentType = attachmentType;
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
        public AttachmentType toAttachmentType() {
            return attachmentType;
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
