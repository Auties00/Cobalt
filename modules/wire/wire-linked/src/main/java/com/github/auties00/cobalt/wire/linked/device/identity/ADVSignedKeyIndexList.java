package com.github.auties00.cobalt.wire.linked.device.identity;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Authenticated wrapper around an {@link ADVKeyIndexList} that carries the account
 * signature proving the list of valid companion indexes is authentic.
 *
 * <p>Whenever an account adds or removes a companion device, the primary regenerates the
 * underlying {@link ADVKeyIndexList}, serializes it, and signs the blob with its account
 * identity key. The signed envelope is what gets distributed to peers: attached to
 * outgoing messages, broadcast through the companion sync flow, or sent as part of the
 * ADV check response. Peers verify the signature before updating their cached view of
 * the account's device membership.
 *
 * <p>Compared to {@link ADVSignedDeviceIdentity} this structure only needs a single
 * signature because it is produced solely by the account; no individual device
 * countersigns it. The signature prefix used on the payload is distinct from the device
 * identity prefixes so that the two signature types can never be mistaken for one
 * another.
 */
@ProtobufMessage(name = "ADVSignedKeyIndexList")
public final class ADVSignedKeyIndexList {
    /**
     * Serialized bytes of the underlying {@link ADVKeyIndexList}.
     *
     * <p>Must be preserved verbatim because the account signature is computed over this
     * exact byte sequence. Callers that need structured access should decode the bytes
     * back into an {@link ADVKeyIndexList} with the protobuf parser.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] details;

    /**
     * Signature produced by the account identity key over the serialized list.
     *
     * <p>Computed as {@code sign(accountKey, ADV_PREFIX_KEY_INDEX_LIST || details)}.
     * Verifying this signature lets peers decide whether to trust the claimed set of
     * valid companion indexes without having to consult the server.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] accountSignature;

    /**
     * Public half of the Curve25519 identity key used to produce the signature.
     *
     * <p>Optional on the wire. When the recipient already knows the account's public key
     * from its Signal session, the signing key can be omitted; when present, it allows a
     * fresh peer to verify the signature before it has established a session of its own.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] accountSignatureKey;

    /**
     * Constructs a signed key index list with the given bytes and signature.
     * Package-private: use the generated {@code ADVSignedKeyIndexListBuilder} or the
     * protobuf decoder.
     *
     * @param details             the serialized {@link ADVKeyIndexList}, or {@code null}
     * @param accountSignature    the signature over the details, or {@code null}
     * @param accountSignatureKey the public account key, or {@code null} if omitted
     */
    ADVSignedKeyIndexList(byte[] details, byte[] accountSignature, byte[] accountSignatureKey) {
        this.details = details;
        this.accountSignature = accountSignature;
        this.accountSignatureKey = accountSignatureKey;
    }

    /**
     * Returns the serialized key index list that was signed.
     *
     * @return the raw bytes, or {@link Optional#empty()} when the field was absent
     */
    public Optional<byte[]> details() {
        return Optional.ofNullable(details);
    }

    /**
     * Returns the account signature over the serialized list.
     *
     * @return the signature bytes, or {@link Optional#empty()} when the list has not been
     *         signed yet
     */
    public Optional<byte[]> accountSignature() {
        return Optional.ofNullable(accountSignature);
    }

    /**
     * Returns the public account identity key used to generate the signature.
     *
     * @return the key bytes, or {@link Optional#empty()} when the peer is expected to use
     *         its locally known copy of the account key during verification
     */
    public Optional<byte[]> accountSignatureKey() {
        return Optional.ofNullable(accountSignatureKey);
    }

    /**
     * Sets the serialized key index list.
     *
     * <p>The bytes must not be mutated after being assigned, because any change
     * invalidates the account signature.
     *
     * @param details the new payload bytes, or {@code null} to clear
     */
    public void setDetails(byte[] details) {
        this.details = details;
    }

    /**
     * Sets the account signature over the list.
     *
     * @param accountSignature the new signature bytes, or {@code null} to clear
     */
    public void setAccountSignature(byte[] accountSignature) {
        this.accountSignature = accountSignature;
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
}
