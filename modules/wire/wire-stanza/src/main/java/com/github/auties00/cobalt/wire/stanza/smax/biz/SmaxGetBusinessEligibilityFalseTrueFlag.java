package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import java.util.Locale;
import java.util.Optional;

/**
 * Models the boolean privacy-interstitial toggle carried by the {@code <meta_verified/>} child of
 * the {@code GetBusinessEligibility} success reply.
 * <p>
 * Surfaced as the {@code should_show_privacy_interstitial_to_new_users} attribute on
 * {@link SmaxGetBusinessEligibilityResponse.Success.MetaVerified}; the Meta-Verified onboarding flow
 * renders the privacy interstitial card when the value is {@link #TRUE} and skips it when
 * {@link #FALSE}.
 *
 * @implNote This implementation accepts only the case-insensitive literals {@code "TRUE"} and
 * {@code "FALSE"}.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageEnums")
public enum SmaxGetBusinessEligibilityFalseTrueFlag {
    /**
     * Wire form {@code "FALSE"}.
     * <p>
     * The Meta-Verified privacy interstitial should NOT be shown.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FALSE_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED)
    FALSE,
    /**
     * Wire form {@code "TRUE"}.
     * <p>
     * The Meta-Verified privacy interstitial SHOULD be shown to new users.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FALSE_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED)
    TRUE;

    /**
     * Resolves a wire-form attribute string into the matching enum constant.
     * <p>
     * Callers propagate {@link Optional#empty()} as a parse failure on the enclosing
     * {@link SmaxGetBusinessEligibilityResponse.Success.MetaVerified}.
     *
     * @implNote This implementation upper-cases under {@link Locale#ROOT} before delegating to
     * {@link #valueOf(String)} and swallows the resulting {@link IllegalArgumentException} as an
     * empty result.
     *
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching enum constant, or empty when the value is
     *         {@code null} or does not match a documented literal
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FALSE_TRUE",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxGetBusinessEligibilityFalseTrueFlag> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxGetBusinessEligibilityFalseTrueFlag.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
