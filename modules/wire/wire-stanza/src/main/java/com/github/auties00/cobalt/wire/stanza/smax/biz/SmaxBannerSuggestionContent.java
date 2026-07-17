package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the {@code <content/>} child of the CTWA banner-suggestion
 * {@code <banner/>}, carrying the banner copy (locale plus heading, body,
 * highlight) and the three optional localised parallels.
 * <p>
 * Drives the headline, body, and highlight text rendered by the WhatsApp
 * Business "suggested banner" panel via {@link #heading()}, {@link #body()},
 * {@link #highlight()}, and {@link #locale()}; the three {@code localised_*}
 * parallels carry the same copy after localisation plus the metadata downstream
 * translation telemetry needs.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest")
public final class SmaxBannerSuggestionContent {
    /**
     * Holds the mandatory {@code locale} attribute.
     */
    private final String locale;

    /**
     * Holds the text content of the mandatory {@code <heading/>} child.
     */
    private final String heading;

    /**
     * Holds the text content of the mandatory {@code <body/>} child.
     */
    private final String body;

    /**
     * Holds the text content of the mandatory {@code <highlight/>} child.
     */
    private final String highlight;

    /**
     * Holds the optional {@code <localised_heading/>} parallel.
     */
    private final SmaxBannerSuggestionLocalisedString localisedHeading;

    /**
     * Holds the optional {@code <localised_body/>} parallel.
     */
    private final SmaxBannerSuggestionLocalisedString localisedBody;

    /**
     * Holds the optional {@code <localised_highlight/>} parallel.
     */
    private final SmaxBannerSuggestionLocalisedString localisedHighlight;

    /**
     * Constructs a projection from already-validated wire values.
     * <p>
     * Callers normally obtain a projection by parsing a stanza via
     * {@link #of(Stanza)}; this constructor is exposed for tests and for
     * hand-built fixtures.
     *
     * @param locale             the locale identifier; never {@code null}
     * @param heading            the heading copy; never {@code null}
     * @param body               the body copy; never {@code null}
     * @param highlight          the highlight copy; never {@code null}
     * @param localisedHeading   the optional {@code <localised_heading/>} parallel; may be {@code null}
     * @param localisedBody      the optional {@code <localised_body/>} parallel; may be {@code null}
     * @param localisedHighlight the optional {@code <localised_highlight/>} parallel; may be {@code null}
     * @throws NullPointerException if any of the four mandatory arguments is {@code null}
     */
    public SmaxBannerSuggestionContent(String locale, String heading, String body, String highlight,
                   SmaxBannerSuggestionLocalisedString localisedHeading, SmaxBannerSuggestionLocalisedString localisedBody,
                   SmaxBannerSuggestionLocalisedString localisedHighlight) {
        this.locale = Objects.requireNonNull(locale, "locale cannot be null");
        this.heading = Objects.requireNonNull(heading, "heading cannot be null");
        this.body = Objects.requireNonNull(body, "body cannot be null");
        this.highlight = Objects.requireNonNull(highlight, "highlight cannot be null");
        this.localisedHeading = localisedHeading;
        this.localisedBody = localisedBody;
        this.localisedHighlight = localisedHighlight;
    }

    /**
     * Returns the locale identifier.
     * <p>
     * Forwarded to the banner-view component as the banner locale; downstream
     * CTWA understand-banner telemetry compares it to the client locale to flag
     * translation mismatches.
     *
     * @return the locale; never {@code null}
     */
    public String locale() {
        return locale;
    }

    /**
     * Returns the heading copy.
     * <p>
     * Rendered as the bold heading line of the banner panel.
     *
     * @return the heading text; never {@code null}
     */
    public String heading() {
        return heading;
    }

    /**
     * Returns the body copy.
     * <p>
     * Rendered as the main paragraph below the heading.
     *
     * @return the body text; never {@code null}
     */
    public String body() {
        return body;
    }

    /**
     * Returns the highlight copy.
     * <p>
     * Rendered as the accented call-to-action snippet inside the body.
     *
     * @return the highlight text; never {@code null}
     */
    public String highlight() {
        return highlight;
    }

    /**
     * Returns the optional {@code <localised_heading/>} parallel.
     * <p>
     * Carries the heading copy with full
     * {@link SmaxBannerSuggestionLocalisationMetadata translation metadata}
     * (translation uid plus parameters). Empty when the relay omitted the
     * parallel.
     *
     * @return an {@link Optional} carrying the parallel, or empty
     */
    public Optional<SmaxBannerSuggestionLocalisedString> localisedHeading() {
        return Optional.ofNullable(localisedHeading);
    }

    /**
     * Returns the optional {@code <localised_body/>} parallel.
     * <p>
     * Carries the body copy with full
     * {@link SmaxBannerSuggestionLocalisationMetadata translation metadata}.
     * Empty when the relay omitted the parallel.
     *
     * @return an {@link Optional} carrying the parallel, or empty
     */
    public Optional<SmaxBannerSuggestionLocalisedString> localisedBody() {
        return Optional.ofNullable(localisedBody);
    }

    /**
     * Returns the optional {@code <localised_highlight/>} parallel.
     * <p>
     * Carries the highlight copy with full
     * {@link SmaxBannerSuggestionLocalisationMetadata translation metadata}.
     * Empty when the relay omitted the parallel.
     *
     * @return an {@link Optional} carrying the parallel, or empty
     */
    public Optional<SmaxBannerSuggestionLocalisedString> localisedHighlight() {
        return Optional.ofNullable(localisedHighlight);
    }

    /**
     * Parses the projection from a {@code <content/>} stanza.
     * <p>
     * Returns empty when the stanza tag is wrong, when any mandatory copy element
     * ({@code locale}, {@code <heading>}, {@code <body>}, {@code <highlight>})
     * is missing or carries empty content, or when an optional
     * {@code <localised_*>} child is present but fails parsing.
     *
     * @implNote This implementation delegates each {@code <localised_*>}
     * parallel to {@link SmaxBannerSuggestionLocalisedString#of(Stanza, String)}
     * with the matching expected tag so a mis-shaped parallel aborts the whole
     * content parse.
     * @param stanza the candidate {@code <content/>} stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty when
     *         parsing fails at any step
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    public static Optional<SmaxBannerSuggestionContent> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("content")) {
            return Optional.empty();
        }
        var locale = stanza.getAttributeAsString("locale").orElse(null);
        if (locale == null) {
            return Optional.empty();
        }
        var headingNode = stanza.getChild("heading").orElse(null);
        if (headingNode == null) {
            return Optional.empty();
        }
        var heading = headingNode.toContentString().orElse(null);
        if (heading == null) {
            return Optional.empty();
        }
        var bodyNode = stanza.getChild("body").orElse(null);
        if (bodyNode == null) {
            return Optional.empty();
        }
        var body = bodyNode.toContentString().orElse(null);
        if (body == null) {
            return Optional.empty();
        }
        var highlightNode = stanza.getChild("highlight").orElse(null);
        if (highlightNode == null) {
            return Optional.empty();
        }
        var highlight = highlightNode.toContentString().orElse(null);
        if (highlight == null) {
            return Optional.empty();
        }
        SmaxBannerSuggestionLocalisedString localisedHeading = null;
        var localisedHeadingNode = stanza.getChild("localised_heading").orElse(null);
        if (localisedHeadingNode != null) {
            var parsed = SmaxBannerSuggestionLocalisedString.of(localisedHeadingNode, "localised_heading");
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            localisedHeading = parsed.get();
        }
        SmaxBannerSuggestionLocalisedString localisedBody = null;
        var localisedBodyNode = stanza.getChild("localised_body").orElse(null);
        if (localisedBodyNode != null) {
            var parsed = SmaxBannerSuggestionLocalisedString.of(localisedBodyNode, "localised_body");
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            localisedBody = parsed.get();
        }
        SmaxBannerSuggestionLocalisedString localisedHighlight = null;
        var localisedHighlightNode = stanza.getChild("localised_highlight").orElse(null);
        if (localisedHighlightNode != null) {
            var parsed = SmaxBannerSuggestionLocalisedString.of(localisedHighlightNode, "localised_highlight");
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            localisedHighlight = parsed.get();
        }
        return Optional.of(new SmaxBannerSuggestionContent(locale, heading, body, highlight,
                localisedHeading, localisedBody, localisedHighlight));
    }

    /**
     * Compares this projection to {@code obj} for structural equality on all
     * seven slots.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxBannerSuggestionContent}
     *         with matching {@link #locale()}, mandatory copy fields, and the
     *         three localised parallels
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxBannerSuggestionContent) obj;
        return Objects.equals(this.locale, that.locale)
                && Objects.equals(this.heading, that.heading)
                && Objects.equals(this.body, that.body)
                && Objects.equals(this.highlight, that.highlight)
                && Objects.equals(this.localisedHeading, that.localisedHeading)
                && Objects.equals(this.localisedBody, that.localisedBody)
                && Objects.equals(this.localisedHighlight, that.localisedHighlight);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash of all seven slots
     */
    @Override
    public int hashCode() {
        return Objects.hash(locale, heading, body, highlight,
                localisedHeading, localisedBody, localisedHighlight);
    }

    /**
     * Returns a debug-friendly rendering naming all seven slots.
     *
     * @return a record-style string with the seven slot values
     */
    @Override
    public String toString() {
        return "SmaxBannerSuggestionContent[locale=" + locale
                + ", heading=" + heading
                + ", body=" + body
                + ", highlight=" + highlight
                + ", localisedHeading=" + localisedHeading
                + ", localisedBody=" + localisedBody
                + ", localisedHighlight=" + localisedHighlight + ']';
    }
}
