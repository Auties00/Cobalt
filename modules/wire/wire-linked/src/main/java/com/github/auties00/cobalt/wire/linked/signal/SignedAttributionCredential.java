package com.github.auties00.cobalt.wire.linked.signal;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Server-signed counterpart of an anonymous-attribution token.
 *
 * <p>WhatsApp's "private stats" pipeline is an anonymous-attribution
 * telemetry channel: the client blinds an attribution token locally,
 * the relay's signing service co-signs the blinded token without
 * learning the underlying value, and the client later unblinds the
 * server-issued counterpart to obtain an unlinkable credential it can
 * spend to attest to a stats event without revealing its identity.
 *
 * <p>This record carries the four byte arrays the client needs to
 * complete the unblinding step locally:
 * <ul>
 *   <li>{@link #signedCredential()} is the blinded credential after
 *       the server applied its signing key;</li>
 *   <li>{@link #acsPublicKey()} is the per-project public key the
 *       client uses to validate the signature;</li>
 *   <li>{@link #dleqProofC()} and {@link #dleqProofS()} are the two
 *       components of the DLEQ proof the server emits so the client
 *       can verify the signing key is the one bound to the project
 *       name.</li>
 * </ul>
 *
 * <p>The {@link #signTimestamp()} is the relay-side issue time and
 * the {@link #projectName()} is the echo of the project identifier
 * the client passed in the request — useful when a single client
 * pipelines multiple in-flight signing requests for different
 * projects and needs to demultiplex the replies.
 */
@ProtobufMessage
public final class SignedAttributionCredential {
    /**
     * The server-side issue time, in seconds since the Unix epoch.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    long signTimestamp;

    /**
     * The 32-byte server-signed credential bytes. The client unblinds
     * this value using its locally-stored blinding factor to obtain
     * the spendable credential.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] signedCredential;

    /**
     * The 32-byte ACS (Account Centre Service) public key bytes the
     * client uses to validate the server signature.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] acsPublicKey;

    /**
     * The 32-byte DLEQ proof {@code c} component the server emits so
     * the client can verify the signing key is the one bound to the
     * project name.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] dleqProofC;

    /**
     * The 32-byte DLEQ proof {@code s} component the server emits so
     * the client can verify the signing key is the one bound to the
     * project name.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    byte[] dleqProofS;

    /**
     * The echo of the project identifier the client passed in the
     * request.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String projectName;

    /**
     * Constructs a new {@code SignedAttributionCredential} carrying
     * the relay-issued payload.
     *
     * @param signTimestamp    the server-side issue time, in seconds
     *                         since the Unix epoch
     * @param signedCredential the server-signed credential bytes
     * @param acsPublicKey     the ACS public key bytes
     * @param dleqProofC       the DLEQ proof {@code c} component
     * @param dleqProofS       the DLEQ proof {@code s} component
     * @param projectName      the project identifier echo
     */
    SignedAttributionCredential(long signTimestamp, byte[] signedCredential, byte[] acsPublicKey,
                                byte[] dleqProofC, byte[] dleqProofS, String projectName) {
        this.signTimestamp = signTimestamp;
        this.signedCredential = signedCredential;
        this.acsPublicKey = acsPublicKey;
        this.dleqProofC = dleqProofC;
        this.dleqProofS = dleqProofS;
        this.projectName = projectName;
    }

    /**
     * Returns the server-side issue time, in seconds since the Unix
     * epoch.
     *
     * @return the issue time
     */
    public long signTimestamp() {
        return signTimestamp;
    }

    /**
     * Returns the 32-byte server-signed credential bytes.
     *
     * @return the signed credential
     */
    public byte[] signedCredential() {
        return signedCredential;
    }

    /**
     * Returns the 32-byte ACS public key bytes.
     *
     * @return the ACS public key
     */
    public byte[] acsPublicKey() {
        return acsPublicKey;
    }

    /**
     * Returns the 32-byte DLEQ proof {@code c} component.
     *
     * @return the {@code c} component
     */
    public byte[] dleqProofC() {
        return dleqProofC;
    }

    /**
     * Returns the 32-byte DLEQ proof {@code s} component.
     *
     * @return the {@code s} component
     */
    public byte[] dleqProofS() {
        return dleqProofS;
    }

    /**
     * Returns the echo of the project identifier the client passed in
     * the request.
     *
     * @return the project identifier
     */
    public String projectName() {
        return projectName;
    }

    /**
     * Sets the server-side issue time.
     *
     * @param signTimestamp the new issue time
     */
    public void setSignTimestamp(long signTimestamp) {
        this.signTimestamp = signTimestamp;
    }

    /**
     * Sets the server-signed credential bytes.
     *
     * @param signedCredential the new bytes
     */
    public void setSignedCredential(byte[] signedCredential) {
        this.signedCredential = signedCredential;
    }

    /**
     * Sets the ACS public key bytes.
     *
     * @param acsPublicKey the new bytes
     */
    public void setAcsPublicKey(byte[] acsPublicKey) {
        this.acsPublicKey = acsPublicKey;
    }

    /**
     * Sets the DLEQ proof {@code c} component.
     *
     * @param dleqProofC the new bytes
     */
    public void setDleqProofC(byte[] dleqProofC) {
        this.dleqProofC = dleqProofC;
    }

    /**
     * Sets the DLEQ proof {@code s} component.
     *
     * @param dleqProofS the new bytes
     */
    public void setDleqProofS(byte[] dleqProofS) {
        this.dleqProofS = dleqProofS;
    }

    /**
     * Sets the project identifier echo.
     *
     * @param projectName the new project identifier
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
