package com.github.auties00.cobalt.wire.linked.bot.response;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Optional;

/**
 * Metadata for an inline image fragment within a WhatsApp AI bot rich
 * response.
 *
 * <p>An inline image is rendered directly within the message flow,
 * with configurable {@linkplain #alignment() alignment} and an
 * optional {@linkplain #tapLinkUrl() tap link} that opens a URL
 * when the user taps the image. The image itself is referenced via
 * an {@link AIRichResponseImageURL} that provides both preview and
 * high-resolution variants.
 *
 * <p>This type implements {@link AIRichResponseSubMessageContent} and
 * appears as the {@link AIRichResponseSubMessageType#INLINE_IMAGE INLINE_IMAGE}
 * variant within an {@link AIRichResponseSubMessage}.
 *
 * @see AIRichResponseSubMessage#content()
 */
@ProtobufMessage(name = "AIRichResponseInlineImageMetadata")
public final class AIRichResponseInlineImageMetadata implements AIRichResponseSubMessageContent {
    /**
     * The URL set containing preview and high-resolution variants
     * of this inline image.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    AIRichResponseImageURL imageUrl;

    /**
     * The alt text or caption associated with this image.
     *
     * <p>Example: {@code "A satellite view of the Eiffel Tower"}
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String imageText;

    /**
     * The horizontal alignment of this image within the message bubble.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    AIRichResponseImageAlignment alignment;

    /**
     * A URL that is opened when the user taps this image.
     *
     * <p>Example: {@code "https://www.example.com/article"}
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    URI tapLinkUrl;


    /**
     * Constructs a new inline image metadata instance.
     *
     * @param imageUrl   the image URL set with preview and high-res variants, or {@code null}
     * @param imageText  the alt text or caption, or {@code null}
     * @param alignment  the horizontal alignment, or {@code null}
     * @param tapLinkUrl the URL opened on tap, or {@code null}
     */
    AIRichResponseInlineImageMetadata(AIRichResponseImageURL imageUrl, String imageText, AIRichResponseImageAlignment alignment, URI tapLinkUrl) {
        this.imageUrl = imageUrl;
        this.imageText = imageText;
        this.alignment = alignment;
        this.tapLinkUrl = tapLinkUrl;
    }

    /**
     * Returns the URL set for this inline image.
     *
     * @return an {@link Optional} containing the image URL, or empty
     *         if not set
     */
    public Optional<AIRichResponseImageURL> imageUrl() {
        return Optional.ofNullable(imageUrl);
    }

    /**
     * Returns the alt text or caption for this image.
     *
     * @return an {@link Optional} containing the image text, or empty
     *         if not set
     */
    public Optional<String> imageText() {
        return Optional.ofNullable(imageText);
    }

    /**
     * Returns the horizontal alignment of this image within the
     * message bubble.
     *
     * @return an {@link Optional} containing the alignment, or empty
     *         if not set
     */
    public Optional<AIRichResponseImageAlignment> alignment() {
        return Optional.ofNullable(alignment);
    }

    /**
     * Returns the URL that is opened when the user taps this image.
     *
     * @return an {@link Optional} containing the tap link URL, or
     *         empty if not set
     */
    public Optional<URI> tapLinkUrl() {
        return Optional.ofNullable(tapLinkUrl);
    }

    /**
     * Sets the URL set for this inline image.
     *
     * @param imageUrl the image URL to set
     */
    public void setImageUrl(AIRichResponseImageURL imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Sets the alt text or caption for this image.
     *
     * @param imageText the image text to set
     */
    public void setImageText(String imageText) {
        this.imageText = imageText;
    }

    /**
     * Sets the horizontal alignment of this image within the message
     * bubble.
     *
     * @param alignment the alignment to set
     */
    public void setAlignment(AIRichResponseImageAlignment alignment) {
        this.alignment = alignment;
    }

    /**
     * Sets the URL that is opened when the user taps this image.
     *
     * @param tapLinkUrl the tap link URL to set
     */
    public void setTapLinkUrl(URI tapLinkUrl) {
        this.tapLinkUrl = tapLinkUrl;
    }

    /**
     * Horizontal alignment for an inline image within an AI rich response
     * message bubble.
     *
     * <p>The alignment is direction-aware: {@link #LEADING} and
     * {@link #TRAILING} adapt to the text direction (LTR or RTL) of the
     * conversation.
     */
    @ProtobufEnum(name = "AIRichResponseInlineImageMetadata.AIRichResponseImageAlignment")
    public static enum AIRichResponseImageAlignment {
        /**
         * The image is aligned to the leading edge (left in LTR layouts,
         * right in RTL layouts).
         */
        LEADING(0),

        /**
         * The image is aligned to the trailing edge (right in LTR layouts,
         * left in RTL layouts).
         */
        TRAILING(1),

        /**
         * The image is horizontally centred within the message bubble.
         */
        CENTER(2);

        /**
         * Constructs an alignment constant with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        AIRichResponseImageAlignment(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index for this alignment.
         */
        final int index;

        /**
         * Returns the protobuf index associated with this alignment.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
