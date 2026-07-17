package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Optional;

/**
 * The outcome literal carried by the
 * {@link SmaxSendAccountRecoveryNonceResponse.Success} reply's
 * {@code <Result><status>...</status></Result>} content.
 *
 * <p>Distinguishes the two states the CTWA biz recovery-email flow can return: {@link #SUCCESS}
 * means the relay actually dispatched the recovery email (the UI advances to the code-entry step),
 * {@link #FAIL} means the relay refused to dispatch it (the UI surfaces a generic failure).
 *
 * @implNote
 * This implementation performs a case-sensitive {@code switch} on the wire literal in
 * {@link #of(String)}, rejecting any value outside the documented pair.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountEnums")
@WhatsAppWebExport(
        moduleName = "WASmaxInBizCtwaAdAccountEnums",
        exports = "ENUM_FAIL_SUCCESS",
        adaptation = WhatsAppAdaptation.ADAPTED
)
public enum SmaxSendAccountRecoveryNonceStatus {
    /**
     * Indicates that the relay attempted to dispatch the recovery
     * email and gave up.
     *
     * <p>Carries the wire literal {@code "Fail"}, triggering the caller to surface a generic
     * failure to the user.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaAdAccountEnums",
            exports = "ENUM_FAIL_SUCCESS",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    FAIL("Fail"),
    /**
     * Indicates that the relay successfully dispatched the recovery
     * email to the user's registered inbox.
     *
     * <p>Carries the wire literal {@code "Success"}, triggering the caller to advance the UI to the
     * recovery-code entry step.
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaAdAccountEnums",
            exports = "ENUM_FAIL_SUCCESS",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    SUCCESS("Success");

    /**
     * The wire literal carried by the {@code <status>} element content.
     */
    private final String wireValue;

    /**
     * Constructs a constant bound to the supplied wire literal.
     *
     * <p>The public surface is the two named constants together with {@link #of(String)} and
     * {@link #wireValue()}.
     *
     * @param wireValue the exact wire-form literal; never {@code null}
     */
    SmaxSendAccountRecoveryNonceStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire literal that corresponds to this constant.
     *
     * @return the wire literal (one of {@code "Fail"} /
     *         {@code "Success"}); never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Returns the constant whose wire literal equals {@code value}.
     *
     * <p>Decodes the {@code <Result><status>} content for
     * {@link SmaxSendAccountRecoveryNonceResponse.Success#of(Stanza, Stanza)}.
     * The lookup is case-sensitive and rejects any literal outside the documented pair.
     *
     * @param value the candidate wire literal; may be {@code null}
     * @return an {@link Optional} carrying the matching constant, or
     *         {@link Optional#empty()} when {@code value} is
     *         {@code null} or does not match any documented literal
     */
    @WhatsAppWebExport(
            moduleName = "WASmaxInBizCtwaAdAccountEnums",
            exports = "ENUM_FAIL_SUCCESS",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public static Optional<SmaxSendAccountRecoveryNonceStatus> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return switch (value) {
            case "Fail" -> Optional.of(FAIL);
            case "Success" -> Optional.of(SUCCESS);
            default -> Optional.empty();
        };
    }
}
