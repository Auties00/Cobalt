package com.github.auties00.cobalt.wire.linked.device.identity;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Commitment sent by a companion device at the very start of the pairing handshake to
 * prove that it has already chosen its ephemeral key.
 *
 * <p>Before the primary and companion devices can run the Noise handshake that protects
 * their pairing traffic, the companion must pick an ephemeral Curve25519 key pair. To
 * prevent the companion from adaptively choosing its key after seeing the primary's
 * contribution, the companion first commits to its selection by hashing the public half
 * (together with a nonce) and sending only the hash as a {@code CompanionCommitment}. The
 * full {@link CompanionEphemeralIdentity} is revealed in a later handshake message, and
 * the primary rejects the session if the revealed value does not match the commitment.
 *
 * <p>The structure deliberately contains nothing beyond the digest: its whole point is to
 * lock the companion in without leaking the ephemeral key until the protocol is ready
 * for it.
 */
@ProtobufMessage(name = "CompanionCommitment")
public final class CompanionCommitment {
    /**
     * SHA-256 commitment to the companion's ephemeral identity.
     *
     * <p>Computed by hashing the serialized {@link CompanionEphemeralIdentity} that the
     * companion will later reveal. The hash is verified by the primary after the
     * companion opens the commitment, which guarantees the companion's ephemeral key was
     * chosen before it learned anything about the primary's contribution.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] hash;

    /**
     * Constructs a commitment with the given digest. Package-private: use the generated
     * {@code CompanionCommitmentBuilder} or the protobuf decoder.
     *
     * @param hash the commitment bytes, or {@code null}
     */
    CompanionCommitment(byte[] hash) {
        this.hash = hash;
    }

    /**
     * Returns the commitment hash.
     *
     * @return the digest bytes, or {@link Optional#empty()} when the field was absent on
     *         the wire
     */
    public Optional<byte[]> hash() {
        return Optional.ofNullable(hash);
    }

    /**
     * Sets the commitment hash.
     *
     * @param hash the new digest bytes, or {@code null} to clear
     */
    public void setHash(byte[] hash) {
        this.hash = hash;
    }
}
