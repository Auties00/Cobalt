package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * Models the {@code <token_type>} dictionary enum carried by the CTWA ad-account access-token reply.
 * <p>
 * Surfaced by {@link SmaxGetAccessTokenAndSessionCookiesResponse.Success} when reading the Facebook
 * Graph access-token strength returned by the SMB CTWA ad-account session bridge; callers branch on
 * {@link #STRONG} for full-access tokens and {@link #WEAK} for the short-lived recovery variant.
 *
 * @implNote This implementation accepts only the two case-sensitive wire literals {@code "Strong"}
 * and {@code "Weak"}; any other casing or value is reported as a parse failure.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBizCtwaAdAccountEnums",
        exports = "ENUM_STRONG_WEAK",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxGetAccessTokenAndSessionCookiesTokenType {
    /**
     * A long-lived strong Facebook Graph access token.
     * <p>
     * Wire form {@code "Strong"}. The CTWA ad-account session bridge issued a token suitable for
     * full Ads-Manager identity work without an immediate re-auth round-trip.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaAdAccountEnums",
            exports = "ENUM_STRONG_WEAK",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    STRONG("Strong"),
    /**
     * A short-lived weak Facebook Graph access token.
     * <p>
     * Wire form {@code "Weak"}. The CTWA ad-account session bridge issued a recovery-grade token;
     * callers typically prompt for a fresh login before invoking sensitive Ads-Manager operations.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaAdAccountEnums",
            exports = "ENUM_STRONG_WEAK",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    WEAK("Weak");

    /**
     * The exact wire-form literal carried by the {@code <token_type>} element content.
     */
    private final String wireValue;

    /**
     * Constructs a constant for the supplied wire-form literal.
     *
     * @param wireValue the exact wire-form literal; never {@code null}
     */
    SmaxGetAccessTokenAndSessionCookiesTokenType(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the exact wire-form literal for this constant.
     * <p>
     * Used when re-emitting the literal onto an outbound stanza or comparing the constant against a
     * captured raw value.
     *
     * @return the wire-form literal ({@code "Strong"} or {@code "Weak"}); never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a wire-form content string into the matching enum constant.
     * <p>
     * Any value that is not one of the documented literals yields empty, which callers propagate as
     * a parse failure on the enclosing variant.
     *
     * @implNote This implementation performs a case-sensitive switch over the two documented
     * literals; a {@code null} input is reported as empty rather than thrown.
     *
     * @param value the content string; may be {@code null}
     * @return an {@link Optional} carrying the matching enum constant, or empty when {@code value}
     *         is {@code null} or does not match a documented literal
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaAdAccountEnums",
            exports = "ENUM_STRONG_WEAK",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxGetAccessTokenAndSessionCookiesTokenType> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return switch (value) {
            case "Strong" -> Optional.of(STRONG);
            case "Weak" -> Optional.of(WEAK);
            default -> Optional.empty();
        };
    }
}
