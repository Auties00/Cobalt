package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.media.AttachmentProvider;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.payment.PaymentInvoiceMessage;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.util.Medias;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A model class that represents a message holding media inside
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Even though the same instance is in the wrapping message info(MessageInfo -> MessageContainer -> MediaMessage),
 * there is currently no way to navigate the tree upwards or any reason to do so considering that this is a special use case.
 * Considering that passing the same instance to {@link MediaMessage#decodedMedia(MediaConnection)} is verbose and unnecessary, there is a copy here.
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

    /**
     * Returns the cached decoded media wrapped by this object if available.
     * Otherwise, the encoded media that this object wraps is decoded, cached and returned.
     *
     * @param mediaConnection the non-null media connection to use to decode this media
     * @return a non-null array of bytes
     */
    public byte[] decodedMedia(@NonNull MediaConnection mediaConnection) {
        return Objects.requireNonNullElseGet(decodedMedia, () -> (this.decodedMedia = Medias.download(this, mediaConnection)));
    }

    /**
     * Decodes the encoded media that this object wraps, caches it and returns the decoded media.
     *
     * @param mediaConnection the non-null media connection to use to decode this media
     * @return a non-null array of bytes
     */
    public byte[] refreshMedia(@NonNull MediaConnection mediaConnection) {
        this.decodedMedia = null;
        return decodedMedia(mediaConnection);
    }

    /**
     * Returns the media type of the media that this object wraps
     *
     * @return a non-null {@link MediaMessageType}
     */
    public abstract MediaMessageType type();

    /**
     * Returns the timestamp, that is the endTimeStamp elapsed since {@link java.time.Instant#EPOCH}, for {@link MediaMessage#key()}
     *
     * @return an unsigned long
     */
    public abstract long mediaKeyTimestamp();

    @Override
    public String name() {
        return type().name()
                .toLowerCase(Locale.ROOT);
    }

    @Override
    public String keyName() {
        return type().keyName();
    }
}
