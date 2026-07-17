package com.github.auties00.cobalt.wire.linked.bot.feedback;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Carries cryptographic signature proofs used to verify the authenticity of
 * AI bot messages.
 *
 * <p>WhatsApp attaches signature verification metadata to bot messages so that
 * clients can confirm the message was genuinely produced by Meta AI and has not
 * been tampered with. Each proof in the {@linkplain #proofs() proofs list}
 * corresponds to a specific {@link BotSignatureVerificationUseCaseProof.BotSignatureUseCase
 * use case} and includes the signature bytes and certificate chain needed for
 * cryptographic verification.
 *
 * @see BotSignatureVerificationUseCaseProof
 */
@ProtobufMessage(name = "BotSignatureVerificationMetadata")
public final class BotSignatureVerificationMetadata {
    /**
     * The list of use-case-specific cryptographic proofs.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BotSignatureVerificationUseCaseProof> proofs;


    /**
     * Constructs a new {@code BotSignatureVerificationMetadata} with the specified proofs.
     *
     * @param proofs the signature verification proofs, or {@code null}
     */
    BotSignatureVerificationMetadata(List<BotSignatureVerificationUseCaseProof> proofs) {
        this.proofs = proofs;
    }

    /**
     * Returns the list of use-case-specific cryptographic proofs.
     *
     * @return an unmodifiable list of proofs, never {@code null}
     */
    public List<BotSignatureVerificationUseCaseProof> proofs() {
        return proofs == null ? List.of() : Collections.unmodifiableList(proofs);
    }

    /**
     * Sets the list of use-case-specific cryptographic proofs.
     *
     * @param proofs the new proofs list, or {@code null}
     */
    public void setProofs(List<BotSignatureVerificationUseCaseProof> proofs) {
        this.proofs = proofs;
    }
}
