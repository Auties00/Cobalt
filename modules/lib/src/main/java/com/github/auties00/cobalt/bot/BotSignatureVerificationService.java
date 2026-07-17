package com.github.auties00.cobalt.bot;

import com.github.auties00.cobalt.wire.linked.bot.feedback.BotSignatureVerificationMetadata;
import com.github.auties00.cobalt.wire.linked.bot.feedback.BotSignatureVerificationUseCaseProof.BotSignatureUseCase;
import com.github.auties00.cobalt.wire.linked.props.ABProp;

/**
 * Verifies the cryptographic signature carried by a forwarded AI bot message against Meta's
 * bot-feature certificate chain.
 *
 * <p>WhatsApp signs every AI bot response so that a recipient who forwards it can prove the
 * forwarded content is the genuine, untampered response. Each {@link BotSignatureVerificationMetadata}
 * carries one or more proofs; the {@link BotSignatureUseCase#WA_BOT_MSG} proof holds an EdDSA
 * signature over the response together with the X.509 certificate chain whose leaf key produced it.
 * Verification validates that chain up to the embedded {@code Meta WA Feature Root CA} (checking each
 * certificate's validity period, issuer signature and revocation status against the
 * {@link BotCertificateRevocationService CRL}), then checks the Ed25519 signature over
 * {@code version || botFbid || messageDigest}.
 *
 * <p>The whole feature is gated by {@link ABProp#AI_RICH_RESPONSE_FORWARDING_VERIFICATION_ENABLED_V1}:
 * when it resolves to {@code none} verification is skipped, when it resolves to {@code log_only} a
 * failure is reported but treated as passed, and when it resolves to {@code enforce_blocking} a
 * failure is propagated to the caller. Every outcome commits a {@code CertificateValidationEvent}
 * metric.
 */
public interface BotSignatureVerificationService {
    /**
     * Returns the configured forwarding-verification enforcement level.
     *
     * @implSpec
     * Implementations resolve the level from
     * {@link ABProp#AI_RICH_RESPONSE_FORWARDING_VERIFICATION_ENABLED_V1} and return
     * {@link BotSignatureEnforcementLevel#NONE} when the prop is unset or holds an unrecognised value.
     *
     * @return the resolved enforcement level
     */
    BotSignatureEnforcementLevel enforcementLevel();

    /**
     * Returns whether forwarding verification is enabled at any tier.
     *
     * @implSpec
     * Implementations return {@code true} when {@link #enforcementLevel()} is not
     * {@link BotSignatureEnforcementLevel#NONE}.
     *
     * @return {@code true} when the enforcement level is not {@link BotSignatureEnforcementLevel#NONE}
     */
    boolean isForwardVerificationEnabled();

    /**
     * Verifies the signature of a forwarded AI bot message and commits the outcome metric.
     *
     * @implSpec
     * Implementations return {@code true} when the message may be forwarded: when verification passes,
     * when it is skipped because the enforcement level is {@link BotSignatureEnforcementLevel#NONE}, or when it
     * fails while the enforcement level is not {@link BotSignatureEnforcementLevel#ENFORCE_BLOCKING}; they return
     * {@code false} only on a blocking failure. Every terminal outcome commits a
     * {@code CertificateValidationEvent} metric.
     *
     * @param botFbid       the Facebook id of the bot that authored the message
     * @param metadata      the signature verification metadata carried on the message
     * @param messageDigest the raw unified-response bytes that were signed
     * @return {@code true} when the message is allowed to be forwarded, {@code false} when a blocking
     *         failure occurred
     */
    boolean verifyBotMessageSignature(String botFbid, BotSignatureVerificationMetadata metadata, byte[] messageDigest);
}
