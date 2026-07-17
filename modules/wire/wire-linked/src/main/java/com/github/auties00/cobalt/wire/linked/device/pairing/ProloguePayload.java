package com.github.auties00.cobalt.wire.linked.device.pairing;

import com.github.auties00.cobalt.wire.linked.device.identity.CompanionCommitment;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Opening payload exchanged at the very start of the pairing handshake, carrying the
 * companion's ephemeral identity and its commitment to it.
 *
 * <p>Before the Noise handshake that protects pairing traffic can begin, the companion
 * sends a prologue containing its newly generated ephemeral identity bytes and a
 * {@link CompanionCommitment} digest computed over that identity. The primary keeps the
 * commitment and, once the handshake progresses far enough for the companion to open it,
 * verifies that the revealed ephemeral matches the committed digest. This binds the
 * companion to its key choice before it has seen any of the primary's contribution, which
 * prevents the companion from adaptively choosing its key after observing the primary's
 * public material.
 *
 * <p>Both fields are conceptually required on the wire, even though they are declared
 * optional at the protobuf level.
 */
@ProtobufMessage(name = "ProloguePayload")
public final class ProloguePayload {
    /**
     * Companion's ephemeral identity bytes, i.e. the serialised public half of the
     * ephemeral key pair that the companion will use for the Noise handshake.
     *
     * <p>Serialised as wire index {@code 1}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] companionEphemeralIdentity;

    /**
     * Commitment over {@link #companionEphemeralIdentity} that lets the primary verify
     * the companion did not pick its ephemeral key adaptively. The hash inside the
     * commitment is computed before this prologue is sent and opened in a later
     * handshake message.
     *
     * <p>Serialised as wire index {@code 2}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    CompanionCommitment commitment;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * @param companionEphemeralIdentity the companion's ephemeral identity bytes
     * @param commitment                 the commitment over the ephemeral identity
     */
    ProloguePayload(byte[] companionEphemeralIdentity, CompanionCommitment commitment) {
        this.companionEphemeralIdentity = companionEphemeralIdentity;
        this.commitment = commitment;
    }

    /**
     * Returns the companion's ephemeral identity bytes.
     *
     * @return the ephemeral identity bytes, or {@link Optional#empty()} when absent on
     *         the wire
     */
    public Optional<byte[]> companionEphemeralIdentity() {
        return Optional.ofNullable(companionEphemeralIdentity);
    }

    /**
     * Returns the commitment to the companion's ephemeral identity.
     *
     * @return the commitment, or {@link Optional#empty()} when absent on the wire
     */
    public Optional<CompanionCommitment> commitment() {
        return Optional.ofNullable(commitment);
    }

    /**
     * Replaces the companion's ephemeral identity bytes.
     *
     * @param companionEphemeralIdentity the new ephemeral identity, or {@code null} to
     *                                   clear it
     */
    public void setCompanionEphemeralIdentity(byte[] companionEphemeralIdentity) {
        this.companionEphemeralIdentity = companionEphemeralIdentity;
    }

    /**
     * Replaces the commitment to the companion's ephemeral identity.
     *
     * @param commitment the new commitment, or {@code null} to clear it
     */
    public void setCommitment(CompanionCommitment commitment) {
        this.commitment = commitment;
    }
}
