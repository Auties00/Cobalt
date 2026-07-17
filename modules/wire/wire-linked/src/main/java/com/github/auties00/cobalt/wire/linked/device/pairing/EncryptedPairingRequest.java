package com.github.auties00.cobalt.wire.linked.device.pairing;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Carries an encrypted {@link PairingRequest} together with the initialisation vector
 * that was used to seal it.
 *
 * <p>During the multi device linking flow the companion transmits its pairing payload
 * wrapped in an AEAD envelope so that only a party holding the shared pairing secret can
 * read the companion's public key material and advertised secret. The wrapped payload
 * lives in {@link #encryptedPayload} while the fresh nonce needed to decrypt it lives in
 * {@link #iv}. Both are opaque byte blobs from the model layer's perspective; the
 * encryption, hashing and Curve25519 operations are performed by the pairing service
 * before values are placed into or read from this message.
 */
@ProtobufMessage(name = "EncryptedPairingRequest")
public final class EncryptedPairingRequest {
    /**
     * Ciphertext produced by encrypting a serialised {@link PairingRequest}. The payload
     * is decrypted by the peer using the shared pairing secret and {@link #iv}.
     * Serialised as wire index {@code 1}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] encryptedPayload;

    /**
     * Initialisation vector used when producing {@link #encryptedPayload}. Must be
     * unique for each pairing attempt and is supplied in the clear so the peer can
     * reconstruct the AEAD nonce. Serialised as wire index {@code 2}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] iv;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * @param encryptedPayload the ciphertext of the pairing request
     * @param iv               the initialisation vector used during encryption
     */
    EncryptedPairingRequest(byte[] encryptedPayload, byte[] iv) {
        this.encryptedPayload = encryptedPayload;
        this.iv = iv;
    }

    /**
     * Returns the ciphertext of the pairing request.
     *
     * @return the encrypted payload bytes, or {@link Optional#empty()} when absent on
     *         the wire
     */
    public Optional<byte[]> encryptedPayload() {
        return Optional.ofNullable(encryptedPayload);
    }

    /**
     * Returns the initialisation vector used when producing the ciphertext.
     *
     * @return the IV bytes, or {@link Optional#empty()} when absent on the wire
     */
    public Optional<byte[]> iv() {
        return Optional.ofNullable(iv);
    }

    /**
     * Replaces the ciphertext of the pairing request.
     *
     * @param encryptedPayload the new ciphertext, or {@code null} to clear it
     */
    public void setEncryptedPayload(byte[] encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    /**
     * Replaces the initialisation vector.
     *
     * @param iv the new IV, or {@code null} to clear it
     */
    public void setIv(byte[] iv) {
        this.iv = iv;
    }
}
