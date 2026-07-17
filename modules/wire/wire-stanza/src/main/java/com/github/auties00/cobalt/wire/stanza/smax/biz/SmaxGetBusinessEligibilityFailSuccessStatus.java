package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import java.util.Locale;
import java.util.Optional;

/**
 * Models the binary pass/fail eligibility enum carried by the {@code <meta_verified/>} and
 * {@code <genai/>} children of the {@code GetBusinessEligibility} success reply.
 * <p>
 * Surfaced by {@link SmaxGetBusinessEligibilityResponse.Success.MetaVerified} and
 * {@link SmaxGetBusinessEligibilityResponse.Success.Genai} as the per-broadcast eligibility signal
 * for the Meta-Verified and GenAI surfaces.
 *
 * @implNote This implementation accepts only the documented case-insensitive literals {@code "FAIL"}
 * and {@code "SUCCESS"}; any other value is reported as {@link Optional#empty()} by
 * {@link #of(String)}.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageEnums")
public enum SmaxGetBusinessEligibilityFailSuccessStatus {
    /**
     * The probed feature is not currently available to the calling business.
     * <p>
     * Wire form {@code "FAIL"}. Callers that gate on broadcast eligibility hide the corresponding
     * compose UI.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FAIL_SUCCESS",
            adaptation = WhatsAppAdaptation.DIRECT)
    FAIL,
    /**
     * The probed feature is available.
     * <p>
     * Wire form {@code "SUCCESS"}. Callers expose the corresponding compose UI.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FAIL_SUCCESS",
            adaptation = WhatsAppAdaptation.DIRECT)
    SUCCESS;

    /**
     * Resolves a wire-form attribute string into the matching enum constant.
     * <p>
     * Any value outside the documented dictionary yields {@link Optional#empty()}, which callers
     * propagate as a parse failure on the enclosing projection.
     *
     * @implNote This implementation upper-cases the input under {@link Locale#ROOT} before delegating
     * to {@link #valueOf(String)} and swallows the resulting {@link IllegalArgumentException} as an
     * empty result.
     *
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching enum constant, or empty when the value is
     *         {@code null} or does not match a documented literal
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FAIL_SUCCESS",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxGetBusinessEligibilityFailSuccessStatus> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxGetBusinessEligibilityFailSuccessStatus.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
