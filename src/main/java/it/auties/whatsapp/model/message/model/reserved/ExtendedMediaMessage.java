package it.auties.whatsapp.model.message.model.reserved;

import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.standard.*;

import java.util.Optional;

public abstract sealed class ExtendedMediaMessage<T extends ExtendedMediaMessage<T>> implements MediaMessage<T> permits AudioMessage, DocumentMessage, ImageMessage, StickerMessage, VideoOrGifMessage {
    private byte[] decodedMedia;
    private String handle;

    public Optional<String> handle() {
        return Optional.ofNullable(handle);
    }

    public Optional<byte[]> decodedMedia() {
        return Optional.ofNullable(decodedMedia);
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
