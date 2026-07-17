package com.github.auties00.cobalt.wire.linked.signal;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Carries a relay-issued private-stats credential.
 *
 * <p>The relay accepts a blinded credential point from the client and
 * returns a signed point along with a DLEQ proof that the same secret
 * key was used both to sign the blinded point and to derive the ACS
 * public key. The client then unblinds the signed point to obtain a
 * redeemable token. This carrier surfaces the parsed reply — the
 * signed credential, the relay's published ACS public key, the DLEQ
 * proof coordinates, the relay-side mint timestamp, and the echoed
 * project name — to caller code so it can run the unblinding and
 * verification steps.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class PrivateStatsToken {
    /**
     * The relay-side mint timestamp.
     */
    private final Instant mintedAt;

    /**
     * The thirty-two-byte signed-credential elliptic-curve scalar.
     */
    private final byte[] signedCredential;

    /**
     * The thirty-two-byte ACS public key the relay published when
     * signing the credential.
     */
    private final byte[] acsPublicKey;

    /**
     * The thirty-two-byte DLEQ proof {@code c} coordinate.
     */
    private final byte[] dleqProofC;

    /**
     * The thirty-two-byte DLEQ proof {@code s} coordinate.
     */
    private final byte[] dleqProofS;

    /**
     * The echoed project name (UTF-8) the relay carries back from the
     * request.
     */
    private final String projectName;

    /**
     * Constructs a new private-stats credential.
     *
     * @param mintedAt         the relay-side mint timestamp; never
     *                         {@code null}
     * @param signedCredential the signed credential bytes; never
     *                         {@code null}
     * @param acsPublicKey     the ACS public key bytes; never
     *                         {@code null}
     * @param dleqProofC       the DLEQ proof {@code c} coordinate
     *                         bytes; never {@code null}
     * @param dleqProofS       the DLEQ proof {@code s} coordinate
     *                         bytes; never {@code null}
     * @param projectName      the echoed project name; never
     *                         {@code null}
     * @throws NullPointerException if any reference argument is
     *                              {@code null}
     */
    public PrivateStatsToken(Instant mintedAt, byte[] signedCredential, byte[] acsPublicKey,
                             byte[] dleqProofC, byte[] dleqProofS, String projectName) {
        this.mintedAt = Objects.requireNonNull(mintedAt, "mintedAt cannot be null");
        this.signedCredential = Objects.requireNonNull(signedCredential, "signedCredential cannot be null").clone();
        this.acsPublicKey = Objects.requireNonNull(acsPublicKey, "acsPublicKey cannot be null").clone();
        this.dleqProofC = Objects.requireNonNull(dleqProofC, "dleqProofC cannot be null").clone();
        this.dleqProofS = Objects.requireNonNull(dleqProofS, "dleqProofS cannot be null").clone();
        this.projectName = Objects.requireNonNull(projectName, "projectName cannot be null");
    }

    /**
     * Returns the relay-side mint timestamp.
     *
     * @return the timestamp; never {@code null}
     */
    public Instant mintedAt() {
        return mintedAt;
    }

    /**
     * Returns a defensive copy of the signed-credential bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] signedCredential() {
        return signedCredential.clone();
    }

    /**
     * Returns a defensive copy of the ACS public key bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] acsPublicKey() {
        return acsPublicKey.clone();
    }

    /**
     * Returns a defensive copy of the DLEQ proof {@code c} coordinate
     * bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] dleqProofC() {
        return dleqProofC.clone();
    }

    /**
     * Returns a defensive copy of the DLEQ proof {@code s} coordinate
     * bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] dleqProofS() {
        return dleqProofS.clone();
    }

    /**
     * Returns the echoed project name.
     *
     * @return the project name; never {@code null}
     */
    public String projectName() {
        return projectName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (PrivateStatsToken) obj;
        return Objects.equals(this.mintedAt, that.mintedAt)
                && Arrays.equals(this.signedCredential, that.signedCredential)
                && Arrays.equals(this.acsPublicKey, that.acsPublicKey)
                && Arrays.equals(this.dleqProofC, that.dleqProofC)
                && Arrays.equals(this.dleqProofS, that.dleqProofS)
                && Objects.equals(this.projectName, that.projectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mintedAt, Arrays.hashCode(signedCredential),
                Arrays.hashCode(acsPublicKey), Arrays.hashCode(dleqProofC),
                Arrays.hashCode(dleqProofS), projectName);
    }

    @Override
    public String toString() {
        return "PrivateStatsToken[mintedAt=" + mintedAt
                + ", signedCredentialLength=" + signedCredential.length
                + ", acsPublicKeyLength=" + acsPublicKey.length
                + ", dleqProofCLength=" + dleqProofC.length
                + ", dleqProofSLength=" + dleqProofS.length
                + ", projectName=" + projectName + ']';
    }
}
