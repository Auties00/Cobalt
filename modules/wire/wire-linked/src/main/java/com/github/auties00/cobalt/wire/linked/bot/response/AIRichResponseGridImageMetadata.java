package com.github.auties00.cobalt.wire.linked.bot.response;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata for a grid image fragment within a WhatsApp AI bot rich response.
 *
 * <p>A grid image displays a composite collage as the primary visual,
 * with individual source images accessible separately. The
 * {@linkplain #gridImageUrl() grid image URL} references the
 * pre-composed collage, while the {@linkplain #imageUrls() image URLs}
 * list provides the individual images that were combined into the grid.
 *
 * <p>This type implements {@link AIRichResponseSubMessageContent} and
 * appears as the {@link AIRichResponseSubMessageType#GRID_IMAGE GRID_IMAGE}
 * variant within an {@link AIRichResponseSubMessage}.
 *
 * @see AIRichResponseSubMessage#content()
 */
@ProtobufMessage(name = "AIRichResponseGridImageMetadata")
public final class AIRichResponseGridImageMetadata implements AIRichResponseSubMessageContent {
    /**
     * The URL set for the composite grid image (the collage thumbnail).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    AIRichResponseImageURL gridImageUrl;

    /**
     * The individual image URLs that compose the grid.
     *
     * <p>Each entry provides preview, high-resolution, and source URLs
     * for a single image within the grid.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<AIRichResponseImageURL> imageUrls;


    /**
     * Constructs a new grid image metadata instance.
     *
     * @param gridImageUrl the URL set for the composite collage, or {@code null}
     * @param imageUrls    the individual image URL sets, or {@code null}
     */
    AIRichResponseGridImageMetadata(AIRichResponseImageURL gridImageUrl, List<AIRichResponseImageURL> imageUrls) {
        this.gridImageUrl = gridImageUrl;
        this.imageUrls = imageUrls;
    }

    /**
     * Returns the URL set for the composite grid image.
     *
     * @return an {@link Optional} containing the grid image URL,
     *         or empty if not set
     */
    public Optional<AIRichResponseImageURL> gridImageUrl() {
        return Optional.ofNullable(gridImageUrl);
    }

    /**
     * Returns the list of individual image URLs that compose the grid.
     *
     * @return an unmodifiable list of image URLs, never {@code null}
     */
    public List<AIRichResponseImageURL> imageUrls() {
        return imageUrls == null ? List.of() : Collections.unmodifiableList(imageUrls);
    }

    /**
     * Sets the URL set for the composite grid image.
     *
     * @param gridImageUrl the grid image URL to set
     */
    public void setGridImageUrl(AIRichResponseImageURL gridImageUrl) {
        this.gridImageUrl = gridImageUrl;
    }

    /**
     * Sets the list of individual image URLs that compose the grid.
     *
     * @param imageUrls the image URLs to set
     */
    public void setImageUrls(List<AIRichResponseImageURL> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
