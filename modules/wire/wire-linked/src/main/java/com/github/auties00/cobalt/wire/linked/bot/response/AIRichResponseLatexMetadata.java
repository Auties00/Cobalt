package com.github.auties00.cobalt.wire.linked.bot.response;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Metadata for a LaTeX mathematical expression fragment within a WhatsApp
 * AI bot rich response.
 *
 * <p>The fragment includes the original {@linkplain #text() text} that
 * surrounds or includes the expressions, together with a list of
 * {@link AIRichResponseLatexExpression} entries. Each expression is
 * pre-rendered as an image on the server; the client downloads and
 * displays the image at the specified dimensions and padding.
 *
 * <p>This type implements {@link AIRichResponseSubMessageContent} and
 * appears as the {@link AIRichResponseSubMessageType#LATEX LATEX}
 * variant within an {@link AIRichResponseSubMessage}.
 *
 * @see AIRichResponseSubMessage#content()
 */
@ProtobufMessage(name = "AIRichResponseLatexMetadata")
public final class AIRichResponseLatexMetadata implements AIRichResponseSubMessageContent {
    /**
     * The text content that surrounds the LaTeX expressions.
     *
     * <p>This may include plain text interspersed with placeholder
     * markers indicating where the rendered expressions should be
     * inserted.
     *
     * <p>Example: {@code "The quadratic formula is:"}
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String text;

    /**
     * The list of LaTeX expressions contained in this fragment.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<AIRichResponseLatexExpression> expressions;


    /**
     * Constructs a new LaTeX metadata instance.
     *
     * @param text        the surrounding text content, or {@code null}
     * @param expressions the list of LaTeX expressions, or {@code null}
     */
    AIRichResponseLatexMetadata(String text, List<AIRichResponseLatexExpression> expressions) {
        this.text = text;
        this.expressions = expressions;
    }

    /**
     * Returns the text content that surrounds the LaTeX expressions.
     *
     * @return an {@link Optional} containing the text, or empty if
     *         not set
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the list of LaTeX expressions in this fragment.
     *
     * @return an unmodifiable list of expressions, never {@code null}
     */
    public List<AIRichResponseLatexExpression> expressions() {
        return expressions == null ? List.of() : Collections.unmodifiableList(expressions);
    }

    /**
     * Sets the text content that surrounds the LaTeX expressions.
     *
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the list of LaTeX expressions in this fragment.
     *
     * @param expressions the expressions to set
     */
    public void setExpressions(List<AIRichResponseLatexExpression> expressions) {
        this.expressions = expressions;
    }

    /**
     * A single LaTeX expression that has been pre-rendered as an image
     * on the server.
     *
     * <p>The client downloads the image from the provided
     * {@linkplain #url() URL} and renders it at the specified
     * {@linkplain #width() width} and {@linkplain #height() height},
     * applying the configured padding on each side.
     */
    @ProtobufMessage(name = "AIRichResponseLatexMetadata.AIRichResponseLatexExpression")
    public static final class AIRichResponseLatexExpression {
        /**
         * The raw LaTeX source of this expression.
         *
         * <p>Example: {@code "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"}
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String latexExpression;

        /**
         * The URL of the pre-rendered image for this expression.
         *
         * <p>Example: {@code "https://scontent.xx.fbcdn.net/v/t39.8562-6/latex_abc123.png"}
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String url;

        /**
         * The width of the rendered image in logical pixels.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.DOUBLE)
        Double width;

        /**
         * The height of the rendered image in logical pixels.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.DOUBLE)
        Double height;

        /**
         * The font height used when rendering the expression, in
         * logical pixels.
         *
         * <p>Clients use this to align the expression baseline with
         * the surrounding text.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.DOUBLE)
        Double fontHeight;

        /**
         * The top padding of the rendered image in logical pixels.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.DOUBLE)
        Double imageTopPadding;

        /**
         * The leading (left in LTR) padding of the rendered image in
         * logical pixels.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.DOUBLE)
        Double imageLeadingPadding;

        /**
         * The bottom padding of the rendered image in logical pixels.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.DOUBLE)
        Double imageBottomPadding;

        /**
         * The trailing (right in LTR) padding of the rendered image
         * in logical pixels.
         */
        @ProtobufProperty(index = 9, type = ProtobufType.DOUBLE)
        Double imageTrailingPadding;


        /**
         * Constructs a new LaTeX expression instance.
         *
         * @param latexExpression     the raw LaTeX source, or {@code null}
         * @param url                 the pre-rendered image URL, or {@code null}
         * @param width               the image width in logical pixels, or {@code null}
         * @param height              the image height in logical pixels, or {@code null}
         * @param fontHeight          the font height for baseline alignment, or {@code null}
         * @param imageTopPadding     the top padding in logical pixels, or {@code null}
         * @param imageLeadingPadding the leading padding in logical pixels, or {@code null}
         * @param imageBottomPadding  the bottom padding in logical pixels, or {@code null}
         * @param imageTrailingPadding the trailing padding in logical pixels, or {@code null}
         */
        AIRichResponseLatexExpression(String latexExpression, String url, Double width, Double height, Double fontHeight, Double imageTopPadding, Double imageLeadingPadding, Double imageBottomPadding, Double imageTrailingPadding) {
            this.latexExpression = latexExpression;
            this.url = url;
            this.width = width;
            this.height = height;
            this.fontHeight = fontHeight;
            this.imageTopPadding = imageTopPadding;
            this.imageLeadingPadding = imageLeadingPadding;
            this.imageBottomPadding = imageBottomPadding;
            this.imageTrailingPadding = imageTrailingPadding;
        }

        /**
         * Returns the raw LaTeX source of this expression.
         *
         * @return an {@link Optional} containing the LaTeX source, or
         *         empty if not set
         */
        public Optional<String> latexExpression() {
            return Optional.ofNullable(latexExpression);
        }

        /**
         * Returns the URL of the pre-rendered image for this expression.
         *
         * @return an {@link Optional} containing the image URL, or
         *         empty if not set
         */
        public Optional<String> url() {
            return Optional.ofNullable(url);
        }

        /**
         * Returns the width of the rendered image in logical pixels.
         *
         * @return an {@link OptionalDouble} containing the width, or
         *         empty if not set
         */
        public OptionalDouble width() {
            return width == null ? OptionalDouble.empty() : OptionalDouble.of(width);
        }

        /**
         * Returns the height of the rendered image in logical pixels.
         *
         * @return an {@link OptionalDouble} containing the height, or
         *         empty if not set
         */
        public OptionalDouble height() {
            return height == null ? OptionalDouble.empty() : OptionalDouble.of(height);
        }

        /**
         * Returns the font height used when rendering the expression.
         *
         * @return an {@link OptionalDouble} containing the font height,
         *         or empty if not set
         */
        public OptionalDouble fontHeight() {
            return fontHeight == null ? OptionalDouble.empty() : OptionalDouble.of(fontHeight);
        }

        /**
         * Returns the top padding of the rendered image in logical
         * pixels.
         *
         * @return an {@link OptionalDouble} containing the top padding,
         *         or empty if not set
         */
        public OptionalDouble imageTopPadding() {
            return imageTopPadding == null ? OptionalDouble.empty() : OptionalDouble.of(imageTopPadding);
        }

        /**
         * Returns the leading padding of the rendered image in logical
         * pixels.
         *
         * @return an {@link OptionalDouble} containing the leading
         *         padding, or empty if not set
         */
        public OptionalDouble imageLeadingPadding() {
            return imageLeadingPadding == null ? OptionalDouble.empty() : OptionalDouble.of(imageLeadingPadding);
        }

        /**
         * Returns the bottom padding of the rendered image in logical
         * pixels.
         *
         * @return an {@link OptionalDouble} containing the bottom
         *         padding, or empty if not set
         */
        public OptionalDouble imageBottomPadding() {
            return imageBottomPadding == null ? OptionalDouble.empty() : OptionalDouble.of(imageBottomPadding);
        }

        /**
         * Returns the trailing padding of the rendered image in logical
         * pixels.
         *
         * @return an {@link OptionalDouble} containing the trailing
         *         padding, or empty if not set
         */
        public OptionalDouble imageTrailingPadding() {
            return imageTrailingPadding == null ? OptionalDouble.empty() : OptionalDouble.of(imageTrailingPadding);
        }

        /**
         * Sets the raw LaTeX source of this expression.
         *
         * @param latexExpression the LaTeX source to set
         */
        public void setLatexExpression(String latexExpression) {
            this.latexExpression = latexExpression;
    }

        /**
         * Sets the URL of the pre-rendered image for this expression.
         *
         * @param url the image URL to set
         */
        public void setUrl(String url) {
            this.url = url;
    }

        /**
         * Sets the width of the rendered image in logical pixels.
         *
         * @param width the width to set
         */
        public void setWidth(Double width) {
            this.width = width;
    }

        /**
         * Sets the height of the rendered image in logical pixels.
         *
         * @param height the height to set
         */
        public void setHeight(Double height) {
            this.height = height;
    }

        /**
         * Sets the font height used when rendering the expression.
         *
         * @param fontHeight the font height to set
         */
        public void setFontHeight(Double fontHeight) {
            this.fontHeight = fontHeight;
    }

        /**
         * Sets the top padding of the rendered image in logical pixels.
         *
         * @param imageTopPadding the top padding to set
         */
        public void setImageTopPadding(Double imageTopPadding) {
            this.imageTopPadding = imageTopPadding;
    }

        /**
         * Sets the leading padding of the rendered image in logical
         * pixels.
         *
         * @param imageLeadingPadding the leading padding to set
         */
        public void setImageLeadingPadding(Double imageLeadingPadding) {
            this.imageLeadingPadding = imageLeadingPadding;
    }

        /**
         * Sets the bottom padding of the rendered image in logical
         * pixels.
         *
         * @param imageBottomPadding the bottom padding to set
         */
        public void setImageBottomPadding(Double imageBottomPadding) {
            this.imageBottomPadding = imageBottomPadding;
    }

        /**
         * Sets the trailing padding of the rendered image in logical
         * pixels.
         *
         * @param imageTrailingPadding the trailing padding to set
         */
        public void setImageTrailingPadding(Double imageTrailingPadding) {
            this.imageTrailingPadding = imageTrailingPadding;
    }
    }
}
