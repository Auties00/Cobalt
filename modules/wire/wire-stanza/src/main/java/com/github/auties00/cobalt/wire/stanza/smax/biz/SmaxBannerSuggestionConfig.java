package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the {@code <config/>} child of the CTWA banner-suggestion
 * {@code <banner/>}, carrying the banner lifecycle attributes (expiry, visual
 * style, revocation marker).
 * <p>
 * Consumers check {@link #revoked()} first: when it is
 * {@link SmaxBannerSuggestionFalseTrueFlag#TRUE} the banner has been pulled and
 * rendering is skipped, otherwise {@link #expiresAt()} and {@link #display()}
 * drive the banner-view component.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest")
public final class SmaxBannerSuggestionConfig {
    /**
     * Holds the {@code expires_at} attribute in epoch seconds, validated as
     * {@code >= 1}.
     */
    private final long expiresAt;

    /**
     * Holds the {@code display} attribute (info or warning).
     */
    private final SmaxBannerSuggestionBannerDisplay display;

    /**
     * Holds the {@code revoked} attribute, false when the banner is still
     * active.
     */
    private final SmaxBannerSuggestionFalseTrueFlag revoked;

    /**
     * Constructs a projection from already-validated wire values.
     * <p>
     * Callers normally obtain a projection by parsing a stanza via
     * {@link #of(Stanza)}; this constructor is exposed for tests and for
     * hand-built fixtures.
     *
     * @param expiresAt the expiration timestamp in epoch seconds; must be {@code >= 1}
     * @param display   the {@link SmaxBannerSuggestionBannerDisplay} style; never {@code null}
     * @param revoked   the {@link SmaxBannerSuggestionFalseTrueFlag} revocation marker; never {@code null}
     * @throws NullPointerException     if {@code display} or {@code revoked} is {@code null}
     * @throws IllegalArgumentException if {@code expiresAt < 1}
     */
    public SmaxBannerSuggestionConfig(long expiresAt, SmaxBannerSuggestionBannerDisplay display, SmaxBannerSuggestionFalseTrueFlag revoked) {
        if (expiresAt < 1) {
            throw new IllegalArgumentException("expiresAt must be >= 1");
        }
        this.expiresAt = expiresAt;
        this.display = Objects.requireNonNull(display, "display cannot be null");
        this.revoked = Objects.requireNonNull(revoked, "revoked cannot be null");
    }

    /**
     * Returns the expiration timestamp in epoch seconds.
     *
     * @return the timestamp in epoch seconds
     */
    public long expiresAt() {
        return expiresAt;
    }

    /**
     * Returns the visual style.
     * <p>
     * Selects between informational and warning-styled chrome for the banner
     * panel.
     *
     * @return the style; never {@code null}
     */
    public SmaxBannerSuggestionBannerDisplay display() {
        return display;
    }

    /**
     * Returns the revocation marker.
     * <p>
     * {@link SmaxBannerSuggestionFalseTrueFlag#TRUE} short-circuits the consumer
     * into the revoked branch, which dismisses the banner by id without
     * rendering any copy.
     *
     * @return the marker; never {@code null}
     */
    public SmaxBannerSuggestionFalseTrueFlag revoked() {
        return revoked;
    }

    /**
     * Parses the projection from a {@code <config/>} stanza.
     * <p>
     * Returns empty when the stanza tag is wrong, when {@code expires_at} is
     * missing or below the {@code 1} floor, or when either of the two
     * literal-tuple attributes fails validation.
     *
     * @implNote This implementation validates {@code expires_at} as
     * {@code >= 1}; any value below the floor yields empty even though the
     * underlying type is a {@code long}. The {@code display} and {@code revoked}
     * attributes are case-sensitive dictionary lookups against
     * {@link SmaxBannerSuggestionBannerDisplay} and
     * {@link SmaxBannerSuggestionFalseTrueFlag} respectively.
     * @param stanza the candidate {@code <config/>} stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty when
     *         parsing fails at any step
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest",
            exports = "parseBannerSuggestionRequestCtwaSuggestionBanner",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxBannerSuggestionConfig> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("config")) {
            return Optional.empty();
        }
        var expiresAt = stanza.getAttributeAsLong("expires_at");
        if (expiresAt.isEmpty() || expiresAt.getAsLong() < 1) {
            return Optional.empty();
        }
        var displayStr = stanza.getAttributeAsString("display").orElse(null);
        var display = SmaxBannerSuggestionBannerDisplay.of(displayStr).orElse(null);
        if (display == null) {
            return Optional.empty();
        }
        var revokedStr = stanza.getAttributeAsString("revoked").orElse(null);
        var revoked = SmaxBannerSuggestionFalseTrueFlag.of(revokedStr).orElse(null);
        if (revoked == null) {
            return Optional.empty();
        }
        return Optional.of(new SmaxBannerSuggestionConfig(expiresAt.getAsLong(), display, revoked));
    }

    /**
     * Compares this projection to {@code obj} for structural equality on the
     * three wire attributes.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxBannerSuggestionConfig}
     *         with matching {@link #expiresAt()}, {@link #display()}, and
     *         {@link #revoked()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxBannerSuggestionConfig) obj;
        return this.expiresAt == that.expiresAt
                && this.display == that.display
                && this.revoked == that.revoked;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash of the three wire attributes
     */
    @Override
    public int hashCode() {
        return Objects.hash(expiresAt, display, revoked);
    }

    /**
     * Returns a debug-friendly rendering naming the three wire attributes.
     *
     * @return a record-style string with the three attribute values
     */
    @Override
    public String toString() {
        return "SmaxBannerSuggestionConfig[expiresAt=" + expiresAt
                + ", display=" + display
                + ", revoked=" + revoked + ']';
    }
}
