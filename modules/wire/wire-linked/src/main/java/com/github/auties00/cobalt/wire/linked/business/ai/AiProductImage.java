package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Reference to an image already uploaded that a WhatsApp Business AI agent
 * product reuses.
 *
 * <p>When a merchant registers product-info knowledge and reuses imagery that
 * was uploaded earlier, the product carries lightweight references to those
 * images rather than re-uploading them. Each reference names the image by its
 * server-issued {@link #imageId() identifier} and the {@link #imageUrl() URL}
 * at which it can be retrieved.
 */
@ProtobufMessage(name = "AiProductImage")
public final class AiProductImage {
    /**
     * Server-issued identifier of the referenced image. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String imageId;

    /**
     * URL at which the referenced image can be retrieved. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String imageUrl;

    /**
     * Constructs a new {@code AiProductImage}. Every argument may be
     * {@code null} to leave the corresponding field unset.
     *
     * @param imageId  the image identifier, or {@code null}
     * @param imageUrl the image URL, or {@code null}
     */
    AiProductImage(String imageId, String imageUrl) {
        this.imageId = imageId;
        this.imageUrl = imageUrl;
    }

    /**
     * Returns the server-issued identifier of the referenced image.
     *
     * @return an {@link Optional} carrying the image id, or empty when unset
     */
    public Optional<String> imageId() {
        return Optional.ofNullable(imageId);
    }

    /**
     * Returns the URL at which the referenced image can be retrieved.
     *
     * @return an {@link Optional} carrying the image URL, or empty when unset
     */
    public Optional<String> imageUrl() {
        return Optional.ofNullable(imageUrl);
    }
}
