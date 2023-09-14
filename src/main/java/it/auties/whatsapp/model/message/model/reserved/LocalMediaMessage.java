package it.auties.whatsapp.model.message.model.reserved;

import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.standard.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public abstract sealed class LocalMediaMessage<T extends LocalMediaMessage<T>> implements MediaMessage<T> permits AudioMessage, DocumentMessage, ImageMessage, StickerMessage, VideoOrGifMessage {
    private byte @Nullable [] decodedMedia;

    public Optional<byte[]> decodedMedia() {
        return Optional.ofNullable(decodedMedia);
    }

    @SuppressWarnings("unchecked")
    public T setDecodedMedia(byte @Nullable [] decodedMedia) {
        this.decodedMedia = decodedMedia;
        return (T) this;
    }
}
