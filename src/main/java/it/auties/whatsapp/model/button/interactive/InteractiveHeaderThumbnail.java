package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufConverter;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model that represents the jpeg thumbnail of a {@link InteractiveHeader}
 *
 * @param thumbnail the non-null jpeg thumbnail
 */
public record InteractiveHeaderThumbnail(byte @NonNull [] thumbnail) implements InteractiveHeaderAttachment {
    @ProtobufConverter
    public static InteractiveHeaderThumbnail of(byte @NonNull [] thumbnail) {
        return new InteractiveHeaderThumbnail(thumbnail);
    }

    @ProtobufConverter
    @Override
    public byte[] thumbnail() {
        return thumbnail;
    }

    @Override
    public Type interactiveHeaderType() {
        return Type.THUMBNAIL;
    }
}
