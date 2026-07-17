package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Locale;
import java.util.Optional;

/**
 * Validates boolean-shaped attributes on SMB business-settings stanzas.
 * <p>
 * Accepts the lowercase wire literals {@code "false"} and {@code "true"}. No Cobalt parser observes
 * this enum on the wire today; it exists so the SMB data-sharing opt-in / opt-out literal universe
 * is fully represented.
 *
 * @implNote This implementation enumerates only the two literals of the settings module, kept
 * independent of the identically-named {@link SmaxBannerSuggestionFalseTrueFlag} from a different
 * module so the source provenance of each module-level export survives.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizSettingsEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBizSettingsEnums",
        exports = "ENUM_FALSE_TRUE",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxBizSettingsFalseTrueFlag {
    /**
     * The wire literal {@code "false"}.
     * <p>
     * The Java constant name is upper-case to follow Java conventions; round-trips with
     * {@link #of(String)}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_FALSE_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    FALSE,

    /**
     * The wire literal {@code "true"}.
     * <p>
     * The Java constant name is upper-case to follow Java conventions; round-trips with
     * {@link #of(String)}.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_FALSE_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    TRUE;

    /**
     * Parses a wire-form attribute string into the matching enum constant.
     * <p>
     * Any value other than the two documented lowercase literals yields empty, which aborts the
     * surrounding stanza parse.
     *
     * @implNote This implementation upper-cases the input via {@link Locale#ROOT} before delegating
     * to {@link Enum#valueOf(Class, String)}; the wire form is always lowercase while the Java
     * constants are upper-case.
     *
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or empty when {@code value} is
     *         {@code null} or does not match
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_FALSE_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxBizSettingsFalseTrueFlag> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxBizSettingsFalseTrueFlag.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
