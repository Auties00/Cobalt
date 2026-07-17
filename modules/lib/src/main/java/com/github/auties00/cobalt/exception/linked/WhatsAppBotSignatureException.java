package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.wire.wam.type.CertVerificationResultType;

import java.util.Objects;

/**
 * Thrown when the certificate chain or signature of a forwarded AI bot message fails verification.
 *
 * <p>WhatsApp signs every AI bot response so that a recipient who forwards it can prove the content is
 * the genuine, untampered response. Validating that proof can fail in several ways: an incomplete or
 * unparseable certificate chain, an expired certificate, a chain that does not anchor to the embedded
 * {@code Meta WA Feature Root CA}, a certificate listed in (or not yet covered by) the bot-feature
 * certificate revocation list, or an Ed25519 signature that does not verify. Each failure carries the
 * {@link CertVerificationResultType} describing the specific mode, the same code that is committed to
 * the {@code CertificateValidationEvent} telemetry.
 *
 * @apiNote
 * Raised for a single forwarded message; {@link #toErrorResult()} returns
 * {@link WhatsAppLinkedClientErrorResult#DISCARD}, so a configured error handler can withhold or flag
 * the forward without tearing the messaging session down.
 *
 * @implNote
 * This implementation returns {@link WhatsAppLinkedClientErrorResult#DISCARD} because a verification
 * failure is local to one message: forwarding gating decides whether to block or merely log it based on
 * the configured enforcement level, never on a session-control result.
 */
public final class WhatsAppBotSignatureException extends WhatsAppLinkedException {
    /**
     * The verification result describing the failure mode.
     */
    private final transient CertVerificationResultType result;

    /**
     * Constructs a new bot-signature exception carrying the given verification result.
     *
     * @param result the verification result describing the failure
     * @throws NullPointerException if {@code result} is {@code null}
     */
    public WhatsAppBotSignatureException(CertVerificationResultType result) {
        super(Objects.requireNonNull(result, "result cannot be null").name());
        this.result = result;
    }

    /**
     * Returns the verification result describing the failure mode.
     *
     * @return the verification result
     */
    public CertVerificationResultType result() {
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link WhatsAppLinkedClientErrorResult#DISCARD}: a
     * verification failure affects only the forwarded message in hand.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }
}
