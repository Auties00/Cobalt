package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;
import it.auties.whatsapp.model.message.payment.PaymentInvoiceMessage;
import it.auties.whatsapp.model.message.standard.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A media message
 * Read its content using {@link it.auties.whatsapp.api.Whatsapp#downloadMedia(ChatMessageInfo)}
 */
public sealed abstract class MediaMessage implements ContextualMessage, MutableAttachmentProvider permits PaymentInvoiceMessage, AudioMessage, DocumentMessage, ImageMessage, StickerMessage, VideoOrGifMessage {
    private byte[] decodedMedia;
    private String handle;

    public Optional<String> handle() {
        return Optional.ofNullable(handle);
    }

    public Optional<byte[]> decodedMedia() {
        return Optional.ofNullable(decodedMedia);
    }

    /**
     * Returns the timestampSeconds, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for{@link MediaMessage#mediaKey()}
     *
     * @return an unsigned long
     */
    public abstract OptionalLong mediaKeyTimestampSeconds();

    /**
     * Returns the timestampSeconds for{@link MediaMessage#mediaKey()}
     *
     * @return a zoned date time
     */
    public abstract Optional<ZonedDateTime> mediaKeyTimestamp();

    /**
     * Returns the media type of the media that this object wraps
     *
     * @return a non-null {@link Type}
     */
    public abstract Type mediaType();

    @Override
    public Category category() {
        return Category.MEDIA;
    }

    @Override
    public Message.Type type() {
        return mediaType().toMessageType();
    }

    @Override
    public AttachmentType attachmentType() {
        return mediaType().toAttachmentType();
    }
    
    public void setDecodedMedia(byte[] decodedMedia) {
        this.decodedMedia = decodedMedia;
    }
    
    public void setHandle(String handle) {
        this.handle = handle;
    }

    /**
     * The constants of this enumerated type describe the various types of media type that a
     * {@link MediaMessage} can hold
     */
    public enum Type {
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
         * @return a string
         */
        public String extension() {
            return extension;
        }

        /**
         * Returns the mime type of this media
         *
         * @return a string
         */
        public String mimeType() {
            return this.mimeType;
        }
    }
}
