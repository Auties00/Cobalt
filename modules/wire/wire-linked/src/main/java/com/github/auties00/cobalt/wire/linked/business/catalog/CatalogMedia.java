package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * Media attached to a WhatsApp Business catalog product write payload.
 *
 * <p>A catalog product carries its imagery and video by URL. This model holds those URLs: the
 * {@link #image() image URLs} and the {@link #video() video URLs}.
 */
@ProtobufMessage(name = "CatalogMedia")
public final class CatalogMedia {
    /**
     * URLs of the product images, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final List<String> image;

    /**
     * URLs of the product videos, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> video;

    /**
     * Constructs a new {@code CatalogMedia}. Every {@code null} list argument is coerced to an empty
     * list.
     *
     * @param image the image URLs; {@code null} treated as empty
     * @param video the video URLs; {@code null} treated as empty
     */
    CatalogMedia(List<String> image, List<String> video) {
        this.image = image == null ? List.of() : List.copyOf(image);
        this.video = video == null ? List.of() : List.copyOf(video);
    }

    /**
     * Returns the URLs of the product images.
     *
     * @return an unmodifiable view of the image URLs; never {@code null}, possibly empty
     */
    public List<String> image() {
        return image;
    }

    /**
     * Returns the URLs of the product videos.
     *
     * @return an unmodifiable view of the video URLs; never {@code null}, possibly empty
     */
    public List<String> video() {
        return video;
    }
}
