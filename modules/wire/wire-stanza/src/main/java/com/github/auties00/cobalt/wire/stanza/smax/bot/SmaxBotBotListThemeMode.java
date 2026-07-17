package com.github.auties00.cobalt.wire.stanza.smax.bot;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Optional;

/**
 * The colour scheme of a bot-directory theme override.
 *
 * <p>Consumers of a {@link SmaxBotBotListResponse.SuccessV2.ThemeBundle} read this
 * discriminator to decide whether the bundle's three colour element values apply to
 * dark-mode or light-mode rendering of the directory sheet. V3 directory replies drop
 * theming entirely, so the discriminator is V2-only.
 *
 * @implNote
 * This implementation projects the wire literal {@code "dark"} or {@code "light"}
 * through {@link #ofWire(String)}; unknown literals collapse to
 * {@link Optional#empty()} rather than being rejected, because the enclosing
 * {@link SmaxBotBotListResponse.SuccessV2.ThemeBundle#of(Stanza)}
 * factory already surfaces the failure to its caller.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBotEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBotEnums",
        exports = "ENUM_DARK_LIGHT",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxBotBotListThemeMode {
    /**
     * Selects the dark-mode colour bundle for the bot card.
     *
     * <p>Carried as the wire literal {@code "dark"} on the {@code <theme mode>}
     * attribute of a V2 directory reply.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_DARK_LIGHT",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    DARK("dark"),
    /**
     * Selects the light-mode colour bundle for the bot card.
     *
     * <p>Carried as the wire literal {@code "light"} on the {@code <theme mode>}
     * attribute of a V2 directory reply.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_DARK_LIGHT",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    LIGHT("light");

    /**
     * The wire literal carried by the {@code <theme mode>} attribute.
     *
     * <p>Read by {@link #wireValue()} when serialising a constant back to its
     * on-the-wire string form, and by {@link #ofWire(String)} during inbound parsing.
     */
    private final String wireValue;

    /**
     * Constructs an enum constant bound to its wire literal.
     *
     * <p>Invoked only by the enum's constant initialisers.
     *
     * @param wireValue the on-the-wire literal; never {@code null}
     */
    SmaxBotBotListThemeMode(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire literal that {@code <theme mode>} carries for this colour scheme.
     *
     * <p>Callers use this when re-serialising a parsed constant back into a stanza or
     * when matching against captured wire bytes.
     *
     * @return the literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves the constant associated with a {@code <theme mode>} wire literal.
     *
     * <p>Invoked by
     * {@link SmaxBotBotListResponse.SuccessV2.ThemeBundle#of(Stanza)}
     * during inbound parsing of a V2 directory reply. Returns {@link Optional#empty()}
     * for {@code null} and for any literal outside {@code {"dark", "light"}}, letting
     * the caller treat unknown values as a parse failure.
     *
     * @implNote
     * This implementation performs a linear scan over {@link #values()}; the enum has
     * two constants so a hash-backed lookup would not pay off.
     *
     * @param wireValue the wire literal to resolve; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or
     *         {@link Optional#empty()} when the literal is {@code null} or unrecognised
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBotEnums",
            exports = "ENUM_DARK_LIGHT",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxBotBotListThemeMode> ofWire(String wireValue) {
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
