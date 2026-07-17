package com.github.auties00.cobalt.wire.stanza.smax.bot;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Optional;

/**
 * The render mode hint the relay assigns to a V3 bot-directory section.
 *
 * <p>Consumers of a {@link SmaxBotBotListResponse.SuccessV3.Section} read this hint to
 * decide how the directory sheet should lay out the section's entries: hidden, a
 * horizontal scroller in one of three sizes, an icebreaker-prompt scroller, or a plain
 * vertical list. V2 directory replies do not carry this attribute, so the discriminator
 * is V3-only.
 *
 * @implNote
 * This implementation projects the wire literal through {@link #ofWire(String)}; unknown
 * literals collapse to {@link Optional#empty()} rather than being rejected, because the
 * enclosing V3 section factory surfaces the failure to its caller.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBotEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBotEnums",
        exports = "ENUM_HIDDEN_HSCROLL_HSCROLLICEBREAKERS_HSCROLLLARGE_HSCROLLSMALL_LISTVIEW",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxBotBotListSectionDisplayType {
    /**
     * Suppresses the section from the directory sheet.
     *
     * <p>Carried as the wire literal {@code "hidden"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_HIDDEN_HSCROLL_HSCROLLICEBREAKERS_HSCROLLLARGE_HSCROLLSMALL_LISTVIEW",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    HIDDEN("hidden"),
    /**
     * Renders the section as a default horizontal scroller.
     *
     * <p>Carried as the wire literal {@code "hscroll"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_HIDDEN_HSCROLL_HSCROLLICEBREAKERS_HSCROLLLARGE_HSCROLLSMALL_LISTVIEW",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    HSCROLL("hscroll"),
    /**
     * Renders the section as a horizontal scroller carrying icebreaker prompt cards.
     *
     * <p>Carried as the wire literal {@code "hscroll_icebreakers"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_HIDDEN_HSCROLL_HSCROLLICEBREAKERS_HSCROLLLARGE_HSCROLLSMALL_LISTVIEW",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    HSCROLL_ICEBREAKERS("hscroll_icebreakers"),
    /**
     * Renders the section as a horizontal scroller with large bot cards.
     *
     * <p>Carried as the wire literal {@code "hscroll_large"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_HIDDEN_HSCROLL_HSCROLLICEBREAKERS_HSCROLLLARGE_HSCROLLSMALL_LISTVIEW",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    HSCROLL_LARGE("hscroll_large"),
    /**
     * Renders the section as a horizontal scroller with small bot cards.
     *
     * <p>Carried as the wire literal {@code "hscroll_small"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_HIDDEN_HSCROLL_HSCROLLICEBREAKERS_HSCROLLLARGE_HSCROLLSMALL_LISTVIEW",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    HSCROLL_SMALL("hscroll_small"),
    /**
     * Renders the section as a vertical list.
     *
     * <p>Carried as the wire literal {@code "listview"}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_HIDDEN_HSCROLL_HSCROLLICEBREAKERS_HSCROLLLARGE_HSCROLLSMALL_LISTVIEW",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    LISTVIEW("listview");

    /**
     * The wire literal carried by the {@code <section display_type>} attribute.
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
    SmaxBotBotListSectionDisplayType(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire literal that {@code <section display_type>} carries for this
     * render mode.
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
     * Resolves the constant associated with a {@code <section display_type>} wire literal.
     *
     * <p>Invoked by
     * {@link SmaxBotBotListResponse.SuccessV3.Section#of(Stanza)}
     * during inbound parsing of a V3 directory reply. Returns {@link Optional#empty()}
     * for {@code null} and for any literal outside the documented six values.
     *
     * @implNote
     * This implementation performs a linear scan over {@link #values()}; the enum has
     * six constants so a hash-backed lookup would not pay off.
     *
     * @param wireValue the wire literal to resolve; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or
     *         {@link Optional#empty()} when the literal is {@code null} or unrecognised
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_HIDDEN_HSCROLL_HSCROLLICEBREAKERS_HSCROLLLARGE_HSCROLLSMALL_LISTVIEW",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxBotBotListSectionDisplayType> ofWire(String wireValue) {
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
