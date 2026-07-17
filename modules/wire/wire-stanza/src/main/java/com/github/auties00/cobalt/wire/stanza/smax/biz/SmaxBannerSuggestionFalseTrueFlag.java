package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Locale;
import java.util.Optional;

/**
 * Validates the CTWA-banner-suggestion boolean attributes, accepting the wire
 * literals {@code "false"} and {@code "true"}.
 * <p>
 * Consumed on the {@code <config revoked="..."/>} attribute that drives the
 * revoked-banner short-circuit: when the value is {@link #TRUE} the consumer
 * dismisses the banner and skips rendering.
 *
 * @implNote This implementation is kept separate from the identically-named
 * {@link SmaxBizSettingsFalseTrueFlag} even though both enumerate the same two
 * literals; the two enums are not merged so each preserves its own
 * module-level source provenance.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBizCtwaActionEnums",
        exports = "ENUM_FALSE_TRUE",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxBannerSuggestionFalseTrueFlag {
    /**
     * Represents the wire literal {@code "false"}.
     * <p>
     * On the {@code <config revoked="..."/>} attribute, indicates the banner is
     * still active and should be rendered.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaActionEnums",
            exports = "ENUM_FALSE_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    FALSE,
    /**
     * Represents the wire literal {@code "true"}.
     * <p>
     * On the {@code <config revoked="..."/>} attribute, indicates the banner has
     * been pulled server-side and the consumer should dismiss it instead of
     * rendering.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaActionEnums",
            exports = "ENUM_FALSE_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    TRUE;

    /**
     * Parses a wire-form attribute string into the matching enum constant.
     * <p>
     * Any value other than the two documented lowercase literals yields empty
     * and aborts the surrounding stanza parse.
     *
     * @implNote This implementation upper-cases the input via
     * {@link Locale#ROOT} before delegating to
     * {@link Enum#valueOf(Class, String)}; the wire form is always lowercase
     * while the Java constants are upper-case.
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or empty when
     *         {@code value} is {@code null} or does not match
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaActionEnums",
            exports = "ENUM_FALSE_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxBannerSuggestionFalseTrueFlag> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxBannerSuggestionFalseTrueFlag.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
