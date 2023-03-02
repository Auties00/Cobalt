package it.auties.whatsapp.model.product;

import lombok.Builder;
import lombok.NonNull;

/**
 * A model that represents the jpeg thumbnail of a {@link ProductHeader}
 *
 * @param thumbnail the non-null jpeg thumbnail
 */
@Builder
public record ProductHeaderThumbnail(byte @NonNull [] thumbnail) implements ProductHeaderAttachment {
    /**
     * Constructs a new thumbnail from a non-null array of bytes
     *
     * @param thumbnail the non-null jpeg thumbnail
     * @return a non-null thumbnail
     */
    public static ProductHeaderThumbnail of(byte[] thumbnail){
        return new ProductHeaderThumbnail(thumbnail);
    }
}
