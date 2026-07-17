package com.github.auties00.cobalt.wire.linked.bot.response;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Discriminator that identifies which metadata payload is carried by an
 * {@link AIRichResponseSubMessage} fragment.
 *
 * <p>Each constant corresponds to one of the content variant types in the
 * {@link AIRichResponseSubMessageContent} sealed hierarchy. The client
 * uses this discriminator to determine how to render the fragment.
 * Callers typically do not inspect this enum directly; instead, call
 * {@link AIRichResponseSubMessage#content()} to obtain the active
 * variant as a type-safe {@code AIRichResponseSubMessageContent}.
 *
 * @see AIRichResponseSubMessage#content()
 * @see AIRichResponseSubMessageContent
 */
@ProtobufEnum(name = "AIRichResponseSubMessageType")
public enum AIRichResponseSubMessageType {
    /**
     * An unrecognised or unsupported fragment type.
     *
     * <p>Clients should skip or display a fallback placeholder for
     * fragments carrying this type.
     */
    UNKNOWN(0),

    /**
     * A grid of images rendered as a composite collage.
     *
     * <p>The corresponding content variant is
     * {@link AIRichResponseGridImageMetadata}.
     */
    GRID_IMAGE(1),

    /**
     * A plain-text or markdown-formatted text fragment.
     *
     * <p>The corresponding content variant is
     * {@link AIRichResponseText}.
     */
    TEXT(2),

    /**
     * A single inline image with optional alignment and tap link.
     *
     * <p>The corresponding content variant is
     * {@link AIRichResponseInlineImageMetadata}.
     */
    INLINE_IMAGE(3),

    /**
     * A tabular data fragment rendered as rows and columns.
     *
     * <p>The corresponding content variant is
     * {@link AIRichResponseTableMetadata}.
     */
    TABLE(4),

    /**
     * A syntax-highlighted code block.
     *
     * <p>The corresponding content variant is
     * {@link AIRichResponseCodeMetadata}.
     */
    CODE(5),

    /**
     * A dynamic media element such as an animated GIF or a statically
     * loaded image delivered via a URL.
     *
     * <p>The corresponding content variant is
     * {@link AIRichResponseDynamicMetadata}.
     */
    DYNAMIC(6),

    /**
     * An interactive map view with pin annotations.
     *
     * <p>The corresponding content variant is
     * {@link AIRichResponseMapMetadata}.
     */
    MAP(7),

    /**
     * A LaTeX mathematical expression rendered as a server-side image.
     *
     * <p>The corresponding content variant is
     * {@link AIRichResponseLatexMetadata}.
     */
    LATEX(8),

    /**
     * A collection of content items such as video reels, typically
     * displayed as a horizontally scrollable carousel.
     *
     * <p>The corresponding content variant is
     * {@link AIRichResponseContentItemsMetadata}.
     */
    CONTENT_ITEMS(9);

    /**
     * Constructs a sub-message type constant with the given protobuf index.
     *
     * @param index the protobuf enum index
     */
    AIRichResponseSubMessageType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf enum index for this sub-message type.
     */
    final int index;

    /**
     * Returns the protobuf index associated with this sub-message type.
     *
     * @return the protobuf index
     */
    public int index() {
        return this.index;
    }
}
