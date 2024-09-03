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
public sealed abstract class MediaMessage<T extends MediaMessage<T>> implements ContextualMessage<T>, MutableAttachmentProvider<T> permits PaymentInvoiceMessage, AudioMessage, DocumentMessage, ImageMessage, StickerMessage, VideoOrGifMessage {
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
     * @return a non-null {@link MediaMessageType}
     */
    public abstract MediaMessageType mediaType();

    @Override
    public MessageCategory category() {
        return MessageCategory.MEDIA;
    }

    @Override
    public MessageType type() {
        return mediaType().toMessageType();
    }

    @Override
    public AttachmentType attachmentType() {
        return mediaType().toAttachmentType();
    }

    @SuppressWarnings("unchecked")
    public T setDecodedMedia(byte[] decodedMedia) {
        this.decodedMedia = decodedMedia;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setHandle(String handle) {
        this.handle = handle;
        return (T) this;
    }
}
