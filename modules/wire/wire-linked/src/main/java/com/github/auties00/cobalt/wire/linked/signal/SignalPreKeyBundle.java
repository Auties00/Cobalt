package com.github.auties00.cobalt.wire.linked.signal;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Carries a complete Signal pre-key bundle ready for upload.
 *
 * <p>This is the input model for both the post-registration pre-key
 * upload (via the {@code <iq xmlns="encrypt" type="set">} envelope) and
 * the registration-time pre-key upload (a separate but
 * shape-equivalent envelope). Both flows take the same payload — the
 * device's registration id, the Signal key-bundle type marker, the
 * long-term identity public key, the freshly-rotated signed pre-key,
 * and a fresh batch of one-time pre-keys.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it travels
 * from caller code to the wire-encoder and is never serialized as
 * protobuf itself.
 */
public final class SignalPreKeyBundle {
    /**
     * The local device's registration id (encoded as a four-byte
     * big-endian unsigned integer on the wire).
     */
    private final int registrationId;

    /**
     * The single-byte Signal key-bundle type marker.
     */
    private final byte keyBundleType;

    /**
     * The local long-term identity public key bytes (typically
     * thirty-two bytes).
     */
    private final byte[] identityPublicKey;

    /**
     * The freshly-rotated signed pre-key.
     */
    private final SignalSignedPreKey signedPreKey;

    /**
     * The fresh batch of one-time pre-keys to upload.
     */
    private final List<SignalPreKey> oneTimePreKeys;

    /**
     * Constructs a new bundle.
     *
     * @param registrationId    the local device's registration id
     * @param keyBundleType     the key-bundle type marker
     * @param identityPublicKey the identity public key bytes; never
     *                          {@code null}
     * @param signedPreKey      the signed pre-key; never {@code null}
     * @param oneTimePreKeys    the one-time pre-keys; never
     *                          {@code null} and never empty
     * @throws NullPointerException     if any reference argument is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code oneTimePreKeys} is
     *                                  empty
     */
    public SignalPreKeyBundle(int registrationId, byte keyBundleType,
                              byte[] identityPublicKey, SignalSignedPreKey signedPreKey,
                              List<SignalPreKey> oneTimePreKeys) {
        this.registrationId = registrationId;
        this.keyBundleType = keyBundleType;
        this.identityPublicKey = Objects.requireNonNull(identityPublicKey, "identityPublicKey cannot be null").clone();
        this.signedPreKey = Objects.requireNonNull(signedPreKey, "signedPreKey cannot be null");
        Objects.requireNonNull(oneTimePreKeys, "oneTimePreKeys cannot be null");
        if (oneTimePreKeys.isEmpty()) {
            throw new IllegalArgumentException("oneTimePreKeys cannot be empty");
        }
        this.oneTimePreKeys = List.copyOf(oneTimePreKeys);
    }

    /**
     * Returns the registration id.
     *
     * @return the registration id
     */
    public int registrationId() {
        return registrationId;
    }

    /**
     * Returns the key-bundle type marker.
     *
     * @return the type marker
     */
    public byte keyBundleType() {
        return keyBundleType;
    }

    /**
     * Returns a defensive copy of the identity public key bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] identityPublicKey() {
        return identityPublicKey.clone();
    }

    /**
     * Returns the signed pre-key.
     *
     * @return the signed pre-key; never {@code null}
     */
    public SignalSignedPreKey signedPreKey() {
        return signedPreKey;
    }

    /**
     * Returns the unmodifiable list of one-time pre-keys.
     *
     * @return the pre-keys; never {@code null} or empty
     */
    public List<SignalPreKey> oneTimePreKeys() {
        return oneTimePreKeys;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SignalPreKeyBundle) obj;
        return this.registrationId == that.registrationId
                && this.keyBundleType == that.keyBundleType
                && Arrays.equals(this.identityPublicKey, that.identityPublicKey)
                && Objects.equals(this.signedPreKey, that.signedPreKey)
                && Objects.equals(this.oneTimePreKeys, that.oneTimePreKeys);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(registrationId, keyBundleType, signedPreKey, oneTimePreKeys);
        result = 31 * result + Arrays.hashCode(identityPublicKey);
        return result;
    }

    @Override
    public String toString() {
        return "SignalPreKeyBundle[registrationId=" + registrationId
                + ", keyBundleType=" + keyBundleType
                + ", identityPublicKey=" + Arrays.toString(identityPublicKey)
                + ", signedPreKey=" + signedPreKey
                + ", oneTimePreKeys=" + oneTimePreKeys + ']';
    }
}
