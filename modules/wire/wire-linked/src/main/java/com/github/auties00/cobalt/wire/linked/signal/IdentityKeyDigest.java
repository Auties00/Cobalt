package com.github.auties00.cobalt.wire.linked.signal;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Carries the relay-side projection of the local Signal pre-key bundle
 * digest.
 *
 * <p>The relay computes a SHA-1 digest over the concatenated key
 * material (registration id, key-bundle type marker, identity public
 * key, signed pre-key, one-time pre-key identifiers) and returns it
 * alongside the canonical inputs so the caller can detect when the
 * remote-side bundle has drifted from the locally cached one. The
 * client compares this digest against a locally recomputed one and
 * issues a fresh upload when they diverge.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class IdentityKeyDigest {
    /**
     * The remote-side registration id echoed by the relay.
     */
    private final int registrationId;

    /**
     * The single-byte Signal key-bundle type marker echoed by the
     * relay.
     */
    private final byte keyBundleType;

    /**
     * The remote-side long-term identity public key bytes (typically
     * thirty-two bytes).
     */
    private final byte[] identityPublicKey;

    /**
     * The remote-side current signed pre-key.
     */
    private final SignalSignedPreKey signedPreKey;

    /**
     * The remote-side one-time pre-key identifier list.
     */
    private final List<Integer> preKeyIds;

    /**
     * The relay-supplied SHA-1 digest of the concatenated key
     * material (twenty bytes).
     */
    private final byte[] hash;

    /**
     * Constructs a new digest projection.
     *
     * @param registrationId    the registration id echoed by the
     *                          relay
     * @param keyBundleType     the key-bundle type marker echoed by
     *                          the relay
     * @param identityPublicKey the identity public key bytes; never
     *                          {@code null}
     * @param signedPreKey      the remote-side signed pre-key; never
     *                          {@code null}
     * @param preKeyIds         the remote-side one-time pre-key
     *                          identifiers; never {@code null}
     * @param hash              the SHA-1 digest bytes; never
     *                          {@code null}
     * @throws NullPointerException if any reference argument is
     *                              {@code null}
     */
    public IdentityKeyDigest(int registrationId, byte keyBundleType,
                             byte[] identityPublicKey, SignalSignedPreKey signedPreKey,
                             List<Integer> preKeyIds, byte[] hash) {
        this.registrationId = registrationId;
        this.keyBundleType = keyBundleType;
        this.identityPublicKey = Objects.requireNonNull(identityPublicKey, "identityPublicKey cannot be null").clone();
        this.signedPreKey = Objects.requireNonNull(signedPreKey, "signedPreKey cannot be null");
        Objects.requireNonNull(preKeyIds, "preKeyIds cannot be null");
        this.preKeyIds = List.copyOf(preKeyIds);
        this.hash = Objects.requireNonNull(hash, "hash cannot be null").clone();
    }

    /**
     * Returns the registration id echoed by the relay.
     *
     * @return the registration id
     */
    public int registrationId() {
        return registrationId;
    }

    /**
     * Returns the key-bundle type marker echoed by the relay.
     *
     * @return the type marker
     */
    public byte keyBundleType() {
        return keyBundleType;
    }

    /**
     * Returns a defensive copy of the identity public key bytes.
     *
     * @return the identity public key bytes; never {@code null}
     */
    public byte[] identityPublicKey() {
        return identityPublicKey.clone();
    }

    /**
     * Returns the remote-side signed pre-key.
     *
     * @return the signed pre-key; never {@code null}
     */
    public SignalSignedPreKey signedPreKey() {
        return signedPreKey;
    }

    /**
     * Returns the remote-side one-time pre-key identifier list.
     *
     * @return an unmodifiable list of identifiers; never {@code null}
     */
    public List<Integer> preKeyIds() {
        return preKeyIds;
    }

    /**
     * Returns a defensive copy of the SHA-1 digest bytes.
     *
     * @return the digest bytes; never {@code null}
     */
    public byte[] hash() {
        return hash.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IdentityKeyDigest) obj;
        return this.registrationId == that.registrationId
                && this.keyBundleType == that.keyBundleType
                && Arrays.equals(this.identityPublicKey, that.identityPublicKey)
                && Objects.equals(this.signedPreKey, that.signedPreKey)
                && Objects.equals(this.preKeyIds, that.preKeyIds)
                && Arrays.equals(this.hash, that.hash);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(registrationId, keyBundleType, signedPreKey, preKeyIds);
        result = 31 * result + Arrays.hashCode(identityPublicKey);
        result = 31 * result + Arrays.hashCode(hash);
        return result;
    }

    @Override
    public String toString() {
        return "IdentityKeyDigest[registrationId=" + registrationId
                + ", keyBundleType=" + keyBundleType
                + ", identityPublicKey=" + Arrays.toString(identityPublicKey)
                + ", signedPreKey=" + signedPreKey
                + ", preKeyIds=" + preKeyIds
                + ", hash=" + Arrays.toString(hash) + ']';
    }
}
