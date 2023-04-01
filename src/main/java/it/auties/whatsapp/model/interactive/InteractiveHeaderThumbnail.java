package it.auties.whatsapp.model.interactive;

import lombok.Builder;
import lombok.NonNull;

/**
 * A model that represents the jpeg thumbnail of a {@link InteractiveHeader}
 *
 * @param thumbnail the non-null jpeg thumbnail
 */
@Builder
public record InteractiveHeaderThumbnail(byte @NonNull [] thumbnail) implements InteractiveHeaderAttachment {
    /**
     * Constructs a new thumbnail from a non-null array of bytes
     *
     * @param thumbnail the non-null jpeg thumbnail
     * @return a non-null thumbnail
     */
    public static InteractiveHeaderThumbnail of(byte[] thumbnail){
        return new InteractiveHeaderThumbnail(thumbnail);
    }
}
