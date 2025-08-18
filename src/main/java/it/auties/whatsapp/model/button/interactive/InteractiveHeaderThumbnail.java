package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * A model that represents the jpeg thumbnail of a {@link InteractiveHeader}
 *
 * @param thumbnail the non-null jpeg thumbnail
 */
public record InteractiveHeaderThumbnail(byte[] thumbnail) implements InteractiveHeaderAttachment {
    @ProtobufDeserializer(builderBehaviour = ProtobufDeserializer.BuilderBehaviour.DISCARD)
    public static InteractiveHeaderThumbnail of(byte[] thumbnail) {
        return new InteractiveHeaderThumbnail(thumbnail);
    }

    @ProtobufSerializer
    @Override
    public byte[] thumbnail() {
        return thumbnail;
    }

    @Override
    public Type interactiveHeaderType() {
        return Type.THUMBNAIL;
    }
}
