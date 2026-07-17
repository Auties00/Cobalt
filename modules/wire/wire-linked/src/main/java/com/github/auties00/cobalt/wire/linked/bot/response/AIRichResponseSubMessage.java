package com.github.auties00.cobalt.wire.linked.bot.response;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A single content fragment within an
 * {@link com.github.auties00.cobalt.wire.linked.message.bot.AIRichResponseMessage AIRichResponseMessage}.
 *
 * <p>Each sub-message carries exactly one content payload whose type
 * is exposed through the {@link AIRichResponseSubMessageContent}
 * sealed interface. Instances should be constructed via the generated
 * {@code AIRichResponseSubMessageBuilder}, which accepts a single
 * {@link AIRichResponseSubMessageContent} parameter to guarantee that
 * the payload and its type discriminator are always consistent:
 *
 * <pre>{@code
 *     var text = new AIRichResponseSubMessageBuilder()
 *         .content(AIRichResponseText.of("Hello"))
 *         .build();
 *
 *     var code = new AIRichResponseSubMessageBuilder()
 *         .content(codeMetadata)
 *         .build();
 * }</pre>
 *
 * <p>The {@link #content()} accessor returns the active variant for
 * exhaustive pattern matching:
 *
 * <pre>{@code
 *     subMessage.content().ifPresent(content -> {
 *         switch (content) {
 *             case AIRichResponseText t         -> renderText(t.value());
 *             case AIRichResponseCodeMetadata c -> renderCode(c);
 *             // ...
 *         }
 *     });
 * }</pre>
 *
 * @see AIRichResponseSubMessageContent
 */
@ProtobufMessage(name = "AIRichResponseSubMessage", generateBuilder = false)
public final class AIRichResponseSubMessage {
    /**
     * The content type of this fragment, indicating which metadata
     * field is populated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    AIRichResponseSubMessageType messageType;

    /**
     * Metadata for a grid image fragment, present when
     * {@code messageType} is
     * {@link AIRichResponseSubMessageType#GRID_IMAGE}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    AIRichResponseGridImageMetadata gridImageMetadata;

    /**
     * The plain-text or markdown content, present when
     * {@code messageType} is
     * {@link AIRichResponseSubMessageType#TEXT}.
     *
     * <p>Example: {@code "Here is the information you requested:"}
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    AIRichResponseText messageText;

    /**
     * Metadata for an inline image fragment, present when
     * {@code messageType} is
     * {@link AIRichResponseSubMessageType#INLINE_IMAGE}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    AIRichResponseInlineImageMetadata imageMetadata;

    /**
     * Metadata for a syntax-highlighted code block, present when
     * {@code messageType} is
     * {@link AIRichResponseSubMessageType#CODE}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    AIRichResponseCodeMetadata codeMetadata;

    /**
     * Metadata for a table fragment, present when
     * {@code messageType} is
     * {@link AIRichResponseSubMessageType#TABLE}.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    AIRichResponseTableMetadata tableMetadata;

    /**
     * Metadata for a dynamic media fragment (image or GIF), present
     * when {@code messageType} is
     * {@link AIRichResponseSubMessageType#DYNAMIC}.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    AIRichResponseDynamicMetadata dynamicMetadata;

    /**
     * Metadata for a LaTeX mathematical expression, present when
     * {@code messageType} is
     * {@link AIRichResponseSubMessageType#LATEX}.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    AIRichResponseLatexMetadata latexMetadata;

    /**
     * Metadata for a map view with annotations, present when
     * {@code messageType} is
     * {@link AIRichResponseSubMessageType#MAP}.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    AIRichResponseMapMetadata mapMetadata;

    /**
     * Metadata for a content items collection (e.g. reels carousel),
     * present when {@code messageType} is
     * {@link AIRichResponseSubMessageType#CONTENT_ITEMS}.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    AIRichResponseContentItemsMetadata contentItemsMetadata;


    /**
     * Constructs a new sub-message with all possible payload fields.
     *
     * <p>Callers should prefer the type-safe
     * {@link #of(AIRichResponseSubMessageContent)} factory method to
     * ensure that exactly one payload is populated.
     *
     * @param messageType          the content type discriminator, or {@code null}
     * @param gridImageMetadata    the grid image payload, or {@code null}
     * @param messageText          the text payload, or {@code null}
     * @param imageMetadata        the inline image payload, or {@code null}
     * @param codeMetadata         the code block payload, or {@code null}
     * @param tableMetadata        the table payload, or {@code null}
     * @param dynamicMetadata      the dynamic media payload, or {@code null}
     * @param latexMetadata        the LaTeX payload, or {@code null}
     * @param mapMetadata          the map payload, or {@code null}
     * @param contentItemsMetadata the content items payload, or {@code null}
     */
    AIRichResponseSubMessage(AIRichResponseSubMessageType messageType, AIRichResponseGridImageMetadata gridImageMetadata, AIRichResponseText messageText, AIRichResponseInlineImageMetadata imageMetadata, AIRichResponseCodeMetadata codeMetadata, AIRichResponseTableMetadata tableMetadata, AIRichResponseDynamicMetadata dynamicMetadata, AIRichResponseLatexMetadata latexMetadata, AIRichResponseMapMetadata mapMetadata, AIRichResponseContentItemsMetadata contentItemsMetadata) {
        this.messageType = messageType;
        this.gridImageMetadata = gridImageMetadata;
        this.messageText = messageText;
        this.imageMetadata = imageMetadata;
        this.codeMetadata = codeMetadata;
        this.tableMetadata = tableMetadata;
        this.dynamicMetadata = dynamicMetadata;
        this.latexMetadata = latexMetadata;
        this.mapMetadata = mapMetadata;
        this.contentItemsMetadata = contentItemsMetadata;
    }

    /**
     * Constructs an {@code AIRichResponseSubMessage} from a type-safe
     * {@link AIRichResponseSubMessageContent} variant.
     *
     * <p>The {@code messageType} discriminator is set automatically
     * based on the concrete type of the supplied content, ensuring
     * that exactly one payload field is populated and the type is
     * consistent.
     *
     * @param content the content variant to wrap, or {@code null}
     * @return a new {@code AIRichResponseSubMessage} whose
     *         {@code messageType} and payload are consistent
     */
    @ProtobufBuilder(className = "AIRichResponseSubMessageBuilder")
    static AIRichResponseSubMessage of(AIRichResponseSubMessageContent content) {
        if (content == null) {
            return new AIRichResponseSubMessage(null, null, null, null, null, null, null, null, null, null);
        }

        return switch (content) {
            case AIRichResponseText t -> new AIRichResponseSubMessage(
                    AIRichResponseSubMessageType.TEXT,
                    null, t, null, null, null, null, null, null, null);
            case AIRichResponseGridImageMetadata g -> new AIRichResponseSubMessage(
                    AIRichResponseSubMessageType.GRID_IMAGE,
                    g, null, null, null, null, null, null, null, null);
            case AIRichResponseInlineImageMetadata i -> new AIRichResponseSubMessage(
                    AIRichResponseSubMessageType.INLINE_IMAGE,
                    null, null, i, null, null, null, null, null, null);
            case AIRichResponseCodeMetadata c -> new AIRichResponseSubMessage(
                    AIRichResponseSubMessageType.CODE,
                    null, null, null, c, null, null, null, null, null);
            case AIRichResponseTableMetadata t -> new AIRichResponseSubMessage(
                    AIRichResponseSubMessageType.TABLE,
                    null, null, null, null, t, null, null, null, null);
            case AIRichResponseDynamicMetadata d -> new AIRichResponseSubMessage(
                    AIRichResponseSubMessageType.DYNAMIC,
                    null, null, null, null, null, d, null, null, null);
            case AIRichResponseLatexMetadata l -> new AIRichResponseSubMessage(
                    AIRichResponseSubMessageType.LATEX,
                    null, null, null, null, null, null, l, null, null);
            case AIRichResponseMapMetadata m -> new AIRichResponseSubMessage(
                    AIRichResponseSubMessageType.MAP,
                    null, null, null, null, null, null, null, m, null);
            case AIRichResponseContentItemsMetadata ci -> new AIRichResponseSubMessage(
                    AIRichResponseSubMessageType.CONTENT_ITEMS,
                    null, null, null, null, null, null, null, null, ci);
        };
    }

    /**
     * Returns the active content variant of this fragment.
     *
     * <p>Callers can use exhaustive pattern matching on the returned
     * {@link AIRichResponseSubMessageContent} sealed interface to
     * handle each variant:
     *
     * <pre>{@code
     *     subMessage.content().ifPresent(content -> {
     *         switch (content) {
     *             case AIRichResponseText t         -> handleText(t.value());
     *             case AIRichResponseCodeMetadata c -> handleCode(c);
     *             // ...
     *         }
     *     });
     * }</pre>
     *
     * @return an {@link Optional} containing the active content
     *         variant, or empty if the message type is unset or
     *         unknown
     */
    public Optional<AIRichResponseSubMessageContent> content() {
        if (messageType == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(switch (messageType) {
            case TEXT -> messageText;
            case GRID_IMAGE -> gridImageMetadata;
            case INLINE_IMAGE -> imageMetadata;
            case CODE -> codeMetadata;
            case TABLE -> tableMetadata;
            case DYNAMIC -> dynamicMetadata;
            case LATEX -> latexMetadata;
            case MAP -> mapMetadata;
            case CONTENT_ITEMS -> contentItemsMetadata;
            case UNKNOWN -> null;
        });
    }
}
