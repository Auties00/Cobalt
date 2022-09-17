package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.media.AttachmentProvider;
import it.auties.whatsapp.model.message.payment.PaymentInvoiceMessage;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.util.Medias;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Locale;
import java.util.Optional;

/**
 * A model class that represents a message holding media inside
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Even though the same instance is in the wrapping message info(MessageInfo -> MessageContainer -> MediaMessage),
 * there is currently no way to navigate the tree upwards or any reason to do so considering that this is a special use case.
 * Considering that passing the same instance to {@link MediaMessage#decodedMedia()} is verbose and unnecessary, there is a copy here.
 */
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public abstract sealed class MediaMessage extends ContextualMessage implements AttachmentProvider
        permits PaymentInvoiceMessage, AudioMessage, DocumentMessage, ImageMessage, StickerMessage, VideoMessage {
    /**
     * The cached decoded media, by default null
     */
    private byte[] decodedMedia;

    @Override
    public MessageType type() {
        return mediaType().messageType();
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    /**
     * Returns the cached decoded media wrapped by this object if available.
     * Otherwise, the encoded media that this object wraps is decoded, cached and returned.
     * If the media is no longer available on Whatsapp's servers, an empty optional is returned.
     *
     * @return a non-null optional
     */
    public Optional<byte[]> decodedMedia() {
        if(decodedMedia == null){
            this.decodedMedia = Medias.download(this)
                    .orElse(null);
        }

        return Optional.ofNullable(decodedMedia);
    }

    /**
     * Decodes the encoded media that this object wraps, caches it and returns the decoded media.
     * If the media is no longer available on Whatsapp's servers, an empty optional is returned.
     *
     * @return a non-null optional
     */
    public Optional<byte[]> refreshedMedia() {
        this.decodedMedia = null;
        return decodedMedia();
    }

    /**
     * Returns the media type of the media that this object wraps
     *
     * @return a non-null {@link MediaMessageType}
     */
    public abstract MediaMessageType mediaType();

    /**
     * Returns the timestamp, that is the endTimeStamp elapsed since {@link java.time.Instant#EPOCH}, for {@link MediaMessage#key()}
     *
     * @return an unsigned long
     */
    public abstract long mediaKeyTimestamp();

    @Override
    public String name() {
        return mediaType().name()
                .toLowerCase(Locale.ROOT);
    }

    @Override
    public String keyName() {
        return mediaType().keyName();
    }
}
