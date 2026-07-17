package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One image attached to a product a WhatsApp Business AI agent can reference.
 *
 * <p>When the merchant teaches their auto-reply assistant about a product,
 * each product may carry images the assistant can show. This model holds one
 * such image's identifier together with the full and thumbnail URLs at which
 * it can be retrieved.
 */
@ProtobufMessage(name = "BusinessAiProductImage")
public final class BusinessAiProductImage {
    /**
     * Server-issued identifier of this image. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * URL of the full-resolution image. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String originalUrl;

    /**
     * URL of the downscaled thumbnail image. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String thumbnailUrl;

    /**
     * Constructs a new {@code BusinessAiProductImage}. Every argument may be
     * {@code null} when the server omitted the corresponding field.
     *
     * @param id           the image identifier, or {@code null}
     * @param originalUrl  the full-resolution URL, or {@code null}
     * @param thumbnailUrl the thumbnail URL, or {@code null}
     */
    BusinessAiProductImage(String id, String originalUrl, String thumbnailUrl) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Returns the server-issued identifier of this image.
     *
     * @return the image id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the URL of the full-resolution image.
     *
     * @return the full-resolution URL, or empty when the server omitted it
     */
    public Optional<String> originalUrl() {
        return Optional.ofNullable(originalUrl);
    }

    /**
     * Returns the URL of the downscaled thumbnail image.
     *
     * @return the thumbnail URL, or empty when the server omitted it
     */
    public Optional<String> thumbnailUrl() {
        return Optional.ofNullable(thumbnailUrl);
    }
}
