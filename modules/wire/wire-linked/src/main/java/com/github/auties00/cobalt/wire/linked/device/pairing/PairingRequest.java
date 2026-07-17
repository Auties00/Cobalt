package com.github.auties00.cobalt.wire.linked.device.pairing;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Pairing payload that a companion device sends to the primary during multi device
 * linking, containing the public key material the primary needs to authenticate the
 * companion and establish an ADV (auxiliary device) trust chain.
 *
 * <p>When a new companion connects for the first time it hands the primary three values:
 * its current Curve25519 session public key, its long lived identity public key, and a
 * short advertised secret that will be used to derive the ADV signature key. The primary
 * verifies these, signs the companion's identity with its own ADV key and returns the
 * resulting account signature. This message is always transmitted wrapped in an
 * {@link EncryptedPairingRequest} so that only a party holding the shared pairing secret
 * can read its contents.
 *
 * <p>All three fields are modelled as byte arrays because they are raw cryptographic
 * material; the calling layers are responsible for generating them via Curve25519 key
 * agreement routines and for never logging or persisting their values.
 */
@ProtobufMessage(name = "PairingRequest")
public final class PairingRequest {
    /**
     * Companion's ephemeral Curve25519 public key for the pairing session, used together
     * with the primary's key to derive the shared pairing secret.
     *
     * <p>Serialised as wire index {@code 1}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] companionPublicKey;

    /**
     * Companion's long lived identity Curve25519 public key. The primary signs this key
     * with its ADV key to produce the account signature that the companion stores and
     * presents on every subsequent session.
     *
     * <p>Serialised as wire index {@code 2}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] companionIdentityKey;

    /**
     * Advertised secret chosen by the companion. The primary feeds this secret into the
     * ADV key derivation together with its own identity, producing the symmetric key
     * that seals later ADV signatures.
     *
     * <p>Serialised as wire index {@code 3}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] advSecret;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * @param companionPublicKey   the companion's ephemeral session public key
     * @param companionIdentityKey the companion's long lived identity public key
     * @param advSecret            the companion's advertised ADV secret
     */
    PairingRequest(byte[] companionPublicKey, byte[] companionIdentityKey, byte[] advSecret) {
        this.companionPublicKey = companionPublicKey;
        this.companionIdentityKey = companionIdentityKey;
        this.advSecret = advSecret;
    }

    /**
     * Returns the companion's ephemeral session public key.
     *
     * @return the public key bytes, or {@link Optional#empty()} when absent on the wire
     */
    public Optional<byte[]> companionPublicKey() {
        return Optional.ofNullable(companionPublicKey);
    }

    /**
     * Returns the companion's long lived identity public key.
     *
     * @return the identity key bytes, or {@link Optional#empty()} when absent on the wire
     */
    public Optional<byte[]> companionIdentityKey() {
        return Optional.ofNullable(companionIdentityKey);
    }

    /**
     * Returns the companion's advertised ADV secret.
     *
     * @return the secret bytes, or {@link Optional#empty()} when absent on the wire
     */
    public Optional<byte[]> advSecret() {
        return Optional.ofNullable(advSecret);
    }

    /**
     * Replaces the companion's ephemeral session public key.
     *
     * @param companionPublicKey the new public key, or {@code null} to clear it
     */
    public void setCompanionPublicKey(byte[] companionPublicKey) {
        this.companionPublicKey = companionPublicKey;
    }

    /**
     * Replaces the companion's long lived identity public key.
     *
     * @param companionIdentityKey the new identity key, or {@code null} to clear it
     */
    public void setCompanionIdentityKey(byte[] companionIdentityKey) {
        this.companionIdentityKey = companionIdentityKey;
    }

    /**
     * Replaces the advertised ADV secret.
     *
     * @param advSecret the new secret, or {@code null} to clear it
     */
    public void setAdvSecret(byte[] advSecret) {
        this.advSecret = advSecret;
    }
}
