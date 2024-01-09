package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;
import it.auties.whatsapp.model.message.model.reserved.ExtendedMediaMessage;
import it.auties.whatsapp.model.message.payment.PaymentInvoiceMessage;
import it.auties.whatsapp.model.message.standard.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A media message
 * Read its content using {@link it.auties.whatsapp.api.Whatsapp#downloadMedia(ChatMessageInfo)}
 */
public sealed interface MediaMessage<T extends MediaMessage<T>> extends ContextualMessage<T>, MutableAttachmentProvider<T> permits ExtendedMediaMessage, PaymentInvoiceMessage, AudioMessage, DocumentMessage, ImageMessage, StickerMessage, VideoOrGifMessage {
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
     * @return a non-null {@link MediaMessageType}
     */
    MediaMessageType mediaType();

    @Override
    default MessageCategory category() {
        return MessageCategory.MEDIA;
    }

    @Override
    default MessageType type() {
        return mediaType().toMessageType();
    }

    @Override
    default AttachmentType attachmentType() {
        return mediaType().toAttachmentType();
    }
}
