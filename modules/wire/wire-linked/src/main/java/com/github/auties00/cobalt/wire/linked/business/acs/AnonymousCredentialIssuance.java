package com.github.auties00.cobalt.wire.linked.business.acs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Outcome of a WhatsApp anonymous-credential batch issuance.
 *
 * <p>WhatsApp uses anonymous credentials (privacy-preserving tokens) for
 * surfaces where a client needs to prove it is entitled to do something
 * without revealing which user did it. The client blinds a batch of
 * tokens, sends the blinded batch to the server for issuance, and on
 * success receives one signed evaluation and one corresponding proof per
 * token: the client unblinds the evaluation into a usable token after
 * verifying the proof.
 *
 * <p>This model is the outcome of one issuance: a success marker, the
 * per-token {@linkplain AnonymousCredentialEvaluation evaluations} and
 * {@linkplain AnonymousCredentialProof proofs} the server returned, and a
 * human-readable error message populated on failure.
 */
@ProtobufMessage(name = "AnonymousCredentialIssuance")
public final class AnonymousCredentialIssuance {
    /**
     * Whether the issuance succeeded. {@code false} both when the server
     * explicitly reported failure and when it omitted the success marker
     * entirely.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean success;

    /**
     * Per-token server evaluations, in the order the server returned
     * them. Never {@code null}, possibly empty when the server returned
     * none.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<AnonymousCredentialEvaluation> evaluations;

    /**
     * Per-token discrete-log-equality proofs, in the order the server
     * returned them. Never {@code null}, possibly empty when the server
     * returned none.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<AnonymousCredentialProof> proofs;

    /**
     * Human-readable failure reason returned by the server, or
     * {@code null} when the issuance succeeded or the server attached no
     * reason.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String errorMessage;

    /**
     * Constructs a new {@code AnonymousCredentialIssuance}. {@code null}
     * lists are coerced to empty lists.
     *
     * @param success      whether the issuance succeeded
     * @param evaluations  the per-token server evaluations; {@code null}
     *                     treated as empty
     * @param proofs       the per-token proofs; {@code null} treated as
     *                     empty
     * @param errorMessage the failure reason, or {@code null}
     */
    AnonymousCredentialIssuance(boolean success, List<AnonymousCredentialEvaluation> evaluations,
                                List<AnonymousCredentialProof> proofs, String errorMessage) {
        this.success = success;
        this.evaluations = evaluations == null ? List.of() : evaluations;
        this.proofs = proofs == null ? List.of() : proofs;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns whether the issuance succeeded.
     *
     * @return {@code true} when the server reported the issuance
     *         succeeded
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns the per-token server evaluations.
     *
     * @return an unmodifiable view of the evaluations; never {@code null},
     *         possibly empty
     */
    public List<AnonymousCredentialEvaluation> evaluations() {
        return Collections.unmodifiableList(evaluations);
    }

    /**
     * Returns the per-token discrete-log-equality proofs.
     *
     * @return an unmodifiable view of the proofs; never {@code null},
     *         possibly empty
     */
    public List<AnonymousCredentialProof> proofs() {
        return Collections.unmodifiableList(proofs);
    }

    /**
     * Returns the human-readable failure reason.
     *
     * @return an {@code Optional} carrying the failure reason, or empty
     *         when the issuance succeeded or the server attached no
     *         reason
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
