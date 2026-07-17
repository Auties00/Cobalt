package com.github.auties00.cobalt.wire.linked.business.acs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Discrete-log-equality proof the server returns alongside each issued
 * WhatsApp anonymous credential.
 *
 * <p>When the WhatsApp anonymous-credential service issues credentials,
 * the server returns one proof per blinded token: the client
 * base64url-decodes the two proof components and verifies the issuance
 * was honest before unblinding the corresponding
 * {@link AnonymousCredentialEvaluation} into a usable token.
 *
 * <p>This model is that proof: the two base64url-encoded components.
 */
@ProtobufMessage(name = "AnonymousCredentialProof")
public final class AnonymousCredentialProof {
    /**
     * Base64url-encoded first component of the proof, or {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String firstComponent;

    /**
     * Base64url-encoded second component of the proof, or {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String secondComponent;

    /**
     * Constructs a new {@code AnonymousCredentialProof}. Either
     * reference argument may be {@code null} when the server omitted the
     * corresponding field.
     *
     * @param firstComponent  the base64url-encoded first component, or
     *                        {@code null}
     * @param secondComponent the base64url-encoded second component, or
     *                        {@code null}
     */
    AnonymousCredentialProof(String firstComponent, String secondComponent) {
        this.firstComponent = firstComponent;
        this.secondComponent = secondComponent;
    }

    /**
     * Returns the base64url-encoded first component of the proof.
     *
     * @return an {@code Optional} carrying the first component, or empty
     *         when the server omitted it
     */
    public Optional<String> firstComponent() {
        return Optional.ofNullable(firstComponent);
    }

    /**
     * Returns the base64url-encoded second component of the proof.
     *
     * @return an {@code Optional} carrying the second component, or empty
     *         when the server omitted it
     */
    public Optional<String> secondComponent() {
        return Optional.ofNullable(secondComponent);
    }
}
