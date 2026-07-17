package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * Validates the SMB-profile discoverability tri-state carried on business-settings stanzas.
 * <p>
 * Accepts the uppercase wire literals {@code "DISCOVERABLE"}, {@code "HIDDEN"}, and
 * {@code "UNDEFINED"}. No Cobalt parser observes this enum on the wire today; it exists so the
 * SMB business-profile discoverability literal universe is fully represented.
 *
 * @implNote This implementation enumerates only the three literals; the wire literals are
 * themselves uppercase (unlike most other SMAX enums), so {@link #of(String)} preserves the input
 * case rather than normalising it.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizSettingsEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBizSettingsEnums",
        exports = "ENUM_DISCOVERABLE_HIDDEN_UNDEFINED",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxBizSettingsDiscoverableHiddenUndefinedFlag {
    /**
     * The wire literal {@code "DISCOVERABLE"}.
     * <p>
     * Indicates the SMB profile is publicly discoverable in WhatsApp's business directory and
     * search surfaces.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_DISCOVERABLE_HIDDEN_UNDEFINED",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    DISCOVERABLE,

    /**
     * The wire literal {@code "HIDDEN"}.
     * <p>
     * Indicates the SMB profile is hidden from directory and search surfaces.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_DISCOVERABLE_HIDDEN_UNDEFINED",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    HIDDEN,

    /**
     * The wire literal {@code "UNDEFINED"}.
     * <p>
     * Indicates the user has not yet picked a discoverability mode; the client surfaces the
     * relevant onboarding dialog whenever this value is observed.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_DISCOVERABLE_HIDDEN_UNDEFINED",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    UNDEFINED;

    /**
     * Parses a wire-form attribute string into the matching enum constant.
     * <p>
     * Any value other than the three documented uppercase literals yields empty, which aborts the
     * surrounding stanza parse.
     *
     * @implNote This implementation does NOT case-normalise the input because the wire literals are
     * themselves uppercase; passing a lowercase value yields empty.
     *
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or empty when {@code value} is
     *         {@code null} or does not match
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizSettingsEnums",
            exports = "ENUM_DISCOVERABLE_HIDDEN_UNDEFINED",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxBizSettingsDiscoverableHiddenUndefinedFlag> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxBizSettingsDiscoverableHiddenUndefinedFlag.valueOf(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
