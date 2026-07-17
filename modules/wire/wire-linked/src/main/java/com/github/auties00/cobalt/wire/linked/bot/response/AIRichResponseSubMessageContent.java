package com.github.auties00.cobalt.wire.linked.bot.response;

/**
 * Sealed interface representing the content variants that an
 * {@link AIRichResponseSubMessage} can carry within a WhatsApp AI bot
 * rich response.
 *
 * <p>Each permitted type corresponds to one of the content types
 * supported by the WhatsApp AI rich response protocol:
 * <ul>
 *   <li>{@link AIRichResponseText} - plain text or markdown
 *   <li>{@link AIRichResponseCodeMetadata} - syntax-highlighted code blocks
 *   <li>{@link AIRichResponseTableMetadata} - tabular data
 *   <li>{@link AIRichResponseGridImageMetadata} - image grid collages
 *   <li>{@link AIRichResponseInlineImageMetadata} - inline images
 *   <li>{@link AIRichResponseDynamicMetadata} - dynamic media (images or GIFs)
 *   <li>{@link AIRichResponseLatexMetadata} - LaTeX mathematical expressions
 *   <li>{@link AIRichResponseMapMetadata} - interactive map views
 *   <li>{@link AIRichResponseContentItemsMetadata} - content item collections (reels)
 * </ul>
 *
 * <p>Using this sealed interface with pattern matching guarantees
 * exhaustive handling of all content types at compile time:
 *
 * <pre>{@code
 *     switch (content) {
 *         case AIRichResponseText t                   -> renderText(t.value());
 *         case AIRichResponseCodeMetadata c            -> renderCode(c);
 *         case AIRichResponseTableMetadata t           -> renderTable(t);
 *         case AIRichResponseGridImageMetadata g       -> renderGridImage(g);
 *         case AIRichResponseInlineImageMetadata i     -> renderInlineImage(i);
 *         case AIRichResponseDynamicMetadata d         -> renderDynamic(d);
 *         case AIRichResponseLatexMetadata l           -> renderLatex(l);
 *         case AIRichResponseMapMetadata m             -> renderMap(m);
 *         case AIRichResponseContentItemsMetadata ci   -> renderContentItems(ci);
 *     }
 * }</pre>
 *
 * <p>Instances of {@link AIRichResponseSubMessage} should be
 * constructed via the generated {@code AIRichResponseSubMessageBuilder},
 * which accepts a single {@code AIRichResponseSubMessageContent}
 * parameter to guarantee that exactly one payload is set and the
 * {@code messageType} discriminator is consistent.
 *
 * @see AIRichResponseSubMessage#content()
 * @see AIRichResponseSubMessage
 */
public sealed interface AIRichResponseSubMessageContent permits
        AIRichResponseText,
        AIRichResponseGridImageMetadata,
        AIRichResponseInlineImageMetadata,
        AIRichResponseCodeMetadata,
        AIRichResponseTableMetadata,
        AIRichResponseDynamicMetadata,
        AIRichResponseLatexMetadata,
        AIRichResponseMapMetadata,
        AIRichResponseContentItemsMetadata {
}
