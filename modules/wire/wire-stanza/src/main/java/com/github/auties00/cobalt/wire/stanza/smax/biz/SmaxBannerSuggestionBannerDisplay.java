package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Locale;
import java.util.Optional;

/**
 * Validates the {@code display} attribute on the {@code <config/>} child of the
 * CTWA banner-suggestion stanza, accepting the wire literals {@code "info"} and
 * {@code "warning"}.
 * <p>
 * Selects the visual styling applied by the WhatsApp Business "suggested
 * banner" panel: {@link #INFO} renders as a neutral informational tile,
 * {@link #WARNING} renders as a warning-styled tile to flag user attention.
 *
 * @implNote This implementation enumerates only the two accepted literals; any
 * other value fails validation.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBizCtwaActionEnums",
        exports = "ENUM_INFO_WARNING",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxBannerSuggestionBannerDisplay {
    /**
     * Represents the wire literal {@code "info"}.
     * <p>
     * Indicates the banner should render as a neutral informational tile.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaActionEnums",
            exports = "ENUM_INFO_WARNING",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    INFO,
    /**
     * Represents the wire literal {@code "warning"}.
     * <p>
     * Indicates the banner should render with warning-style chrome to draw the
     * user's attention.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaActionEnums",
            exports = "ENUM_INFO_WARNING",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    WARNING;

    /**
     * Parses a wire-form attribute string into the matching enum constant.
     * <p>
     * Any value other than the two documented lowercase literals yields empty
     * and aborts the surrounding stanza parse.
     *
     * @implNote This implementation upper-cases the input via
     * {@link Locale#ROOT} before delegating to
     * {@link Enum#valueOf(Class, String)}; the wire is always lowercase while
     * the constant names are upper-case in line with Java conventions.
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or empty when
     *         {@code value} is {@code null} or does not match
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaActionEnums",
            exports = "ENUM_INFO_WARNING",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxBannerSuggestionBannerDisplay> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxBannerSuggestionBannerDisplay.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
