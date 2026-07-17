package com.github.auties00.cobalt.wire.stanza.smax.bot;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * The kind of grouping a bot-directory section applies to its entries.
 *
 * <p>Consumers of a {@link SmaxBotBotListResponse.SuccessV2.Section} or
 * {@link SmaxBotBotListResponse.SuccessV3.Section} read this discriminator to decide how
 * to label the section in a directory sheet. The same three values appear in both V2 and
 * V3 replies.
 *
 * @implNote
 * This implementation projects the wire literal through {@link #ofWire(String)}; unknown
 * literals collapse to {@link Optional#empty()} rather than being rejected, because the
 * enclosing section factories surface the failure to their callers.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBotEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBotEnums",
        exports = "ENUM_ALL_CATEGORY_FEATURED",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxBotBotListSectionType {
    /**
     * Aggregates every directory bot into one flat list.
     *
     * <p>Carried as the wire literal {@code "all"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_ALL_CATEGORY_FEATURED",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    ALL("all"),
    /**
     * Groups directory bots under a topical category heading.
     *
     * <p>Carried as the wire literal {@code "category"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_ALL_CATEGORY_FEATURED",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    CATEGORY("category"),
    /**
     * Promotes a curated subset of bots into a featured rail.
     *
     * <p>Carried as the wire literal {@code "featured"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_ALL_CATEGORY_FEATURED",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    FEATURED("featured");

    /**
     * The wire literal carried by the {@code <section type>} attribute.
     *
     * <p>Read by {@link #wireValue()} when re-serialising and by {@link #ofWire(String)}
     * during inbound parsing.
     */
    private final String wireValue;

    /**
     * Constructs an enum constant bound to its wire literal.
     *
     * <p>Invoked only by the enum's constant initialisers.
     *
     * @param wireValue the on-the-wire literal; never {@code null}
     */
    SmaxBotBotListSectionType(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire literal that {@code <section type>} carries for this section kind.
     *
     * <p>Callers use this when re-serialising a parsed constant or matching against
     * captured wire bytes.
     *
     * @return the literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves the constant associated with a {@code <section type>} wire literal.
     *
     * <p>Invoked by both the V2 and V3 section factories during inbound parsing of a
     * {@link SmaxBotBotListResponse}. Returns {@link Optional#empty()} for {@code null}
     * and for any literal outside {@code {"all", "category", "featured"}}.
     *
     * @implNote
     * This implementation performs a linear scan over {@link #values()}; the enum has
     * three constants so a hash-backed lookup would not pay off.
     *
     * @param wireValue the wire literal to resolve; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or
     *         {@link Optional#empty()} when the literal is {@code null} or unrecognised
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_ALL_CATEGORY_FEATURED",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxBotBotListSectionType> ofWire(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (value.wireValue.equals(wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
