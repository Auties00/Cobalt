package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the shared shape of every {@code <localised_*>} parallel under the
 * CTWA banner-suggestion {@code <content/>}, pairing the translated copy with
 * its {@link SmaxBannerSuggestionLocalisationMetadata translation metadata}.
 * <p>
 * Used as the projection for {@code <localised_heading/>},
 * {@code <localised_body/>}, and {@code <localised_highlight/>}. The surrounding
 * {@link SmaxBannerSuggestionContent} carries the source-language copy on its
 * mandatory {@code <heading/>}, {@code <body/>}, {@code <highlight/>} children;
 * each {@code <localised_*>} parallel captures the same copy after localisation
 * plus the metadata downstream translation telemetry needs to attribute it.
 *
 * @implNote This implementation is a single class shared across the three
 * parallels because they differ only by the expected tag name; the
 * {@code expectedTag} parameter to {@link #of(Stanza, String)} picks which one is
 * in play.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest")
public final class SmaxBannerSuggestionLocalisedString {
    /**
     * Holds the mandatory {@code value} attribute, the localised string.
     */
    private final String value;

    /**
     * Holds the mandatory {@link SmaxBannerSuggestionLocalisationMetadata}
     * projection extracted from the {@code <localisation_metadata/>}
     * grandchild.
     */
    private final SmaxBannerSuggestionLocalisationMetadata localisationMetadata;

    /**
     * Constructs a projection from already-validated wire values.
     * <p>
     * Callers normally obtain a projection by parsing a stanza via
     * {@link #of(Stanza, String)}; this constructor is exposed for tests and for
     * hand-built fixtures.
     *
     * @param value                the localised string; never {@code null}
     * @param localisationMetadata the localisation metadata projection; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SmaxBannerSuggestionLocalisedString(String value, SmaxBannerSuggestionLocalisationMetadata localisationMetadata) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.localisationMetadata = Objects.requireNonNull(localisationMetadata,
                "localisationMetadata cannot be null");
    }

    /**
     * Returns the localised string.
     * <p>
     * The post-translation copy the banner-view component renders instead of
     * the source-language copy when the client locale matches.
     *
     * @return the localised value; never {@code null}
     */
    public String value() {
        return value;
    }

    /**
     * Returns the localisation metadata.
     * <p>
     * Carries the translation-unit identifier, owning translation project, and
     * runtime placeholder substitutions for downstream translation telemetry.
     *
     * @return the metadata projection; never {@code null}
     */
    public SmaxBannerSuggestionLocalisationMetadata localisationMetadata() {
        return localisationMetadata;
    }

    /**
     * Parses the projection from a {@code <localised_*>} stanza, asserting the
     * supplied tag.
     * <p>
     * Returns empty when the stanza tag does not match {@code expectedTag}, when
     * the mandatory {@code value} attribute is missing, when the mandatory
     * {@code <localisation_metadata/>} grandchild is missing, or when that
     * grandchild fails to parse. Callers pass one of
     * {@code "localised_heading"}, {@code "localised_body"}, or
     * {@code "localised_highlight"} depending on which parallel they are
     * parsing.
     *
     * @implNote This implementation collapses three near-identical sibling
     * parsers (heading, body, highlight) into a single helper switched on the
     * {@code expectedTag} parameter; the underlying steps (tag assertion,
     * {@code value} attribute extraction, metadata grandchild lookup) are
     * identical.
     * @param stanza        the candidate {@code <localised_*>} stanza; never {@code null}
     * @param expectedTag the expected tag name; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty when
     *         parsing fails at any step
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest",
            exports = "parseBannerSuggestionRequestCtwaSuggestionBannerContentLocalisedHeading",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest",
            exports = "parseBannerSuggestionRequestCtwaSuggestionBannerContentLocalisedBody",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest",
            exports = "parseBannerSuggestionRequestCtwaSuggestionBannerContentLocalisedHighlight",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxBannerSuggestionLocalisedString> of(Stanza stanza, String expectedTag) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(expectedTag, "expectedTag cannot be null");
        if (!stanza.hasDescription(expectedTag)) {
            return Optional.empty();
        }
        var value = stanza.getAttributeAsString("value").orElse(null);
        if (value == null) {
            return Optional.empty();
        }
        var metadataNode = stanza.getChild("localisation_metadata").orElse(null);
        if (metadataNode == null) {
            return Optional.empty();
        }
        var metadata = SmaxBannerSuggestionLocalisationMetadata.of(metadataNode).orElse(null);
        if (metadata == null) {
            return Optional.empty();
        }
        return Optional.of(new SmaxBannerSuggestionLocalisedString(value, metadata));
    }

    /**
     * Compares this projection to {@code obj} for structural equality on value
     * and metadata.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxBannerSuggestionLocalisedString}
     *         with matching {@link #value()} and {@link #localisationMetadata()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxBannerSuggestionLocalisedString) obj;
        return Objects.equals(this.value, that.value)
                && Objects.equals(this.localisationMetadata, that.localisationMetadata);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash of value and metadata
     */
    @Override
    public int hashCode() {
        return Objects.hash(value, localisationMetadata);
    }

    /**
     * Returns a debug-friendly rendering naming value and metadata.
     *
     * @return a record-style string with value and metadata
     */
    @Override
    public String toString() {
        return "SmaxBannerSuggestionLocalisedString[value=" + value
                + ", localisationMetadata=" + localisationMetadata + ']';
    }
}
