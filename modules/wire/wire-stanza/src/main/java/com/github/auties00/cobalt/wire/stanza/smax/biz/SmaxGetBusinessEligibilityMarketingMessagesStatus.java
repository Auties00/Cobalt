package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import java.util.Locale;
import java.util.Optional;

/**
 * Models the four-way marketing-messages eligibility enum carried by the
 * {@code <marketing_messages/>} child of the {@code GetBusinessEligibility} success reply.
 * <p>
 * Surfaced by {@link SmaxGetBusinessEligibilityResponse.Success.MarketingMessages} as the gating
 * signal for the SMB marketing-messages broadcast surface; the broadcast-compose UI maps the four
 * states onto enable, pause, warn, and disable affordances.
 *
 * @implNote This implementation accepts only the case-insensitive literals {@code "FAIL"},
 * {@code "PAUSED"}, {@code "SUCCESS"}, and {@code "WARNING"}.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizMarketingMessageEnums")
public enum SmaxGetBusinessEligibilityMarketingMessagesStatus {
    /**
     * Wire form {@code "FAIL"}.
     * <p>
     * Marketing messages are not available; callers hide the compose entry point.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FAIL_PAUSED_SUCCESS_WARNING",
            adaptation = WhatsAppAdaptation.DIRECT)
    FAIL,
    /**
     * Wire form {@code "PAUSED"}.
     * <p>
     * The relay has temporarily paused sends; callers surface a pause banner instead of the compose
     * UI.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FAIL_PAUSED_SUCCESS_WARNING",
            adaptation = WhatsAppAdaptation.DIRECT)
    PAUSED,
    /**
     * Wire form {@code "SUCCESS"}.
     * <p>
     * Marketing messages are available with no caveats.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FAIL_PAUSED_SUCCESS_WARNING",
            adaptation = WhatsAppAdaptation.DIRECT)
    SUCCESS,
    /**
     * Wire form {@code "WARNING"}.
     * <p>
     * Marketing messages are available but the relay is surfacing a non-fatal warning (for example,
     * quota close to exhaustion); callers expose the compose UI with an inline advisory.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FAIL_PAUSED_SUCCESS_WARNING",
            adaptation = WhatsAppAdaptation.DIRECT)
    WARNING;

    /**
     * Resolves a wire-form attribute string into the matching enum constant.
     * <p>
     * Callers propagate {@link Optional#empty()} as a parse failure on the enclosing
     * {@link SmaxGetBusinessEligibilityResponse.Success.MarketingMessages}.
     *
     * @implNote This implementation upper-cases under {@link Locale#ROOT} before delegating to
     * {@link #valueOf(String)} and swallows the resulting {@link IllegalArgumentException} as an
     * empty result.
     *
     * @param value the attribute value; may be {@code null}
     * @return an {@link Optional} carrying the matching enum constant, or empty when the value does
     *         not match a documented literal
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizMarketingMessageEnums",
            exports = "ENUM_FAIL_PAUSED_SUCCESS_WARNING",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxGetBusinessEligibilityMarketingMessagesStatus> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SmaxGetBusinessEligibilityMarketingMessagesStatus.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
