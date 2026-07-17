package com.github.auties00.cobalt.wire.linked.device.identity;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Authenticated wrapper around an {@link ADVDeviceIdentity} carrying two signatures that
 * prove a companion device was authorised by its account.
 *
 * <p>During device pairing the primary device encodes an {@code ADVDeviceIdentity} into a
 * byte blob, signs it with the account's long-lived identity key, and hands the result to
 * the companion. The companion then counter-signs the same payload with its own identity
 * key to prove that it accepted the assignment. The resulting
 * {@code ADVSignedDeviceIdentity}, containing both signatures, is persisted by the
 * companion and attached to every outgoing message.
 *
 * <p>Remote peers that receive the blob decode the details, verify the account signature
 * against the account's known identity key, verify the device signature against the key
 * embedded in {@code details}, and only then trust the originating device. The
 * verification prefixes depend on the {@link ADVEncryptionType} of the account and of the
 * device, so hosted and end-to-end encrypted endpoints use different signing domains.
 *
 * <p>The class is mutable so that the pairing flow can produce an initial identity, patch
 * in the device signature once it is computed, and re-encode the blob for transmission.
 */
@ProtobufMessage(name = "ADVSignedDeviceIdentity")
public final class ADVSignedDeviceIdentity {
    /**
     * Serialized bytes of the underlying {@link ADVDeviceIdentity}.
     *
     * <p>This is the exact byte sequence that was signed; it must be preserved verbatim
     * so peers can recompute the signatures. Callers that need the structured view should
     * decode this field back into an {@link ADVDeviceIdentity} with the protobuf parser.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] details;

    /**
     * Public half of the Curve25519 identity key that signed {@link #details}.
     *
     * <p>Optional on the wire when the peer already knows the account's identity key
     * from another source (such as its Signal session). When present, the verifier uses
     * this key rather than the locally stored one so that freshly paired accounts can
     * bootstrap trust without a prior session.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] accountSignatureKey;

    /**
     * Signature produced by the account identity key over the identity details.
     *
     * <p>Computed as {@code sign(accountKey, ADV_PREFIX || details)}, where the prefix is
     * chosen from the account's encryption type. Verifying this signature establishes
     * that the owning account authorised the companion device referenced in
     * {@link #details}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] accountSignature;

    /**
     * Signature produced by the companion device over its own identity details.
     *
     * <p>Computed as {@code sign(deviceKey, ADV_PREFIX || details || accountKey)} using
     * the prefix associated with the device's encryption type. Verifying this signature
     * proves the companion device actually possesses the private key it claims, defending
     * against malicious actors who might otherwise forge an authorised identity using a
     * leaked account signature alone.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] deviceSignature;

    /**
     * Constructs a signed device identity with the given raw payload and signatures.
     * Package-private: use the generated {@code ADVSignedDeviceIdentityBuilder} or the
     * protobuf decoder instead.
     *
     * @param details              the serialized {@link ADVDeviceIdentity}, or {@code null}
     * @param accountSignatureKey  the public account identity key, or {@code null} if omitted
     * @param accountSignature     the account signature over {@code details}, or {@code null}
     * @param deviceSignature      the device signature over {@code details}, or {@code null}
     */
    ADVSignedDeviceIdentity(byte[] details, byte[] accountSignatureKey, byte[] accountSignature, byte[] deviceSignature) {
        this.details = details;
        this.accountSignatureKey = accountSignatureKey;
        this.accountSignature = accountSignature;
        this.deviceSignature = deviceSignature;
    }

    /**
     * Returns the serialized identity payload that was signed.
     *
     * @return the raw bytes, or {@link Optional#empty()} when the field was absent
     */
    public Optional<byte[]> details() {
        return Optional.ofNullable(details);
    }

    /**
     * Returns the public account identity key, if it was shipped in line.
     *
     * @return the key bytes, or {@link Optional#empty()} when the verifier is expected to
     *         look the key up locally
     */
    public Optional<byte[]> accountSignatureKey() {
        return Optional.ofNullable(accountSignatureKey);
    }

    /**
     * Returns the account signature over the identity details.
     *
     * @return the signature bytes, or {@link Optional#empty()} when the identity has not
     *         been signed yet
     */
    public Optional<byte[]> accountSignature() {
        return Optional.ofNullable(accountSignature);
    }

    /**
     * Returns the device signature over the identity details.
     *
     * @return the signature bytes, or {@link Optional#empty()} during intermediate pairing
     *         states where only the account signature has been computed
     */
    public Optional<byte[]> deviceSignature() {
        return Optional.ofNullable(deviceSignature);
    }

    /**
     * Sets the serialized identity payload.
     *
     * <p>The bytes are stored by reference and must not be mutated after being assigned,
     * because any change would invalidate the signatures computed over them.
     *
     * @param details the new identity bytes, or {@code null} to clear
     */
    public void setDetails(byte[] details) {
        this.details = details;
    }

    /**
     * Sets the public account identity key.
     *
     * @param accountSignatureKey the new key bytes, or {@code null} to rely on the peer's
     *                            locally stored account key during verification
     */
    public void setAccountSignatureKey(byte[] accountSignatureKey) {
        this.accountSignatureKey = accountSignatureKey;
    }

    /**
     * Sets the account signature produced over {@link #details}.
     *
     * @param accountSignature the new signature bytes, or {@code null} to clear
     */
    public void setAccountSignature(byte[] accountSignature) {
        this.accountSignature = accountSignature;
    }

    /**
     * Sets the device signature produced over {@link #details}.
     *
     * <p>The pairing flow calls this setter after the companion has signed its identity,
     * turning a half-signed blob into a complete one that can be sent to peers.
     *
     * @param deviceSignature the new signature bytes, or {@code null} to clear
     */
    public void setDeviceSignature(byte[] deviceSignature) {
        this.deviceSignature = deviceSignature;
    }
}
