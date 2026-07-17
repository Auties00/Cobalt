package com.github.auties00.cobalt.wire.linked.federated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Envelope of RSA-2048 wrapped material that every encrypted federated-identity
 * (Waffle) RPC exchanges with the relay.
 *
 * <p>WhatsApp's federated-identity bridge (internally code-named "Waffle") lets
 * a WhatsApp account link to a Meta-side identity (Facebook or Instagram). To
 * keep the linked-account payload opaque to the WhatsApp server, the client and
 * the relay tunnel a small AES-GCM frame inside an RSA-2048 envelope. Every
 * outbound and inbound Waffle request that carries an encrypted body uses the
 * exact same shape:
 *
 * <ol>
 *   <li>A symmetric key, RSA-2048 wrapped against the relay's public key.</li>
 *   <li>A 12-byte AES-GCM nonce.</li>
 *   <li>The AES-GCM ciphertext.</li>
 *   <li>The 16-byte AES-GCM authentication tag.</li>
 * </ol>
 *
 * <p>This class is the domain-model projection of that four-blob envelope. It
 * appears as a request parameter on the Waffle RPCs that submit encrypted
 * payloads (ping, refresh-tokens, encrypted-action, enterprise-customer
 * generation) and as the wrapped reply body on the responses that surface a
 * fresh envelope (refresh-tokens, encrypted-action, generate-customer).
 *
 * <p>Cobalt collapses the WhatsApp Web {@code
 * WASmaxOutWaffleRSAEncryptionMetadataMixin} and {@code
 * WASmaxInWaffleRSAEncryptionMetadataMixin} pair into the single value class
 * here, since the inbound and outbound shapes are byte-identical.
 */
@ProtobufMessage(name = "FederatedRsaEncryption")
public final class FederatedRsaEncryption {
    /**
     * RSA-2048 wrapped symmetric AES-GCM key. The relay unwraps it with its
     * private RSA key to recover the per-request session key that decrypts
     * {@link #encryptedData}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] encryptedKey;

    /**
     * The 12-byte AES-GCM nonce used for the symmetric encryption of
     * {@link #encryptedData}. Each request must use a fresh, random nonce.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] nonce;

    /**
     * AES-GCM ciphertext of the federated-identity payload. The plaintext is
     * the RPC-specific protobuf message that the relay forwards to the linked
     * Meta account; only the relay can decrypt it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] encryptedData;

    /**
     * The 16-byte AES-GCM authentication tag covering {@link #encryptedData}.
     * The tag is verified together with the ciphertext to detect tampering.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] authTag;

    /**
     * Constructs a new {@code FederatedRsaEncryption} envelope.
     *
     * @param encryptedKey  the RSA-wrapped AES-GCM key, or {@code null} if
     *                      absent
     * @param nonce         the AES-GCM nonce, or {@code null} if absent
     * @param encryptedData the AES-GCM ciphertext, or {@code null} if absent
     * @param authTag       the AES-GCM authentication tag, or {@code null} if
     *                      absent
     */
    FederatedRsaEncryption(byte[] encryptedKey, byte[] nonce, byte[] encryptedData, byte[] authTag) {
        this.encryptedKey = encryptedKey;
        this.nonce = nonce;
        this.encryptedData = encryptedData;
        this.authTag = authTag;
    }

    /**
     * Returns the RSA-2048 wrapped symmetric AES-GCM key.
     *
     * @return an {@link Optional} containing the wrapped key bytes, or empty
     *         when the envelope omitted the field
     */
    public Optional<byte[]> encryptedKey() {
        return Optional.ofNullable(encryptedKey);
    }

    /**
     * Returns the AES-GCM nonce used for the symmetric encryption.
     *
     * @return an {@link Optional} containing the nonce bytes, or empty when
     *         the envelope omitted the field
     */
    public Optional<byte[]> nonce() {
        return Optional.ofNullable(nonce);
    }

    /**
     * Returns the AES-GCM ciphertext of the federated-identity payload.
     *
     * @return an {@link Optional} containing the ciphertext bytes, or empty
     *         when the envelope omitted the field
     */
    public Optional<byte[]> encryptedData() {
        return Optional.ofNullable(encryptedData);
    }

    /**
     * Returns the AES-GCM authentication tag covering the ciphertext.
     *
     * @return an {@link Optional} containing the tag bytes, or empty when
     *         the envelope omitted the field
     */
    public Optional<byte[]> authTag() {
        return Optional.ofNullable(authTag);
    }

    /**
     * Replaces the RSA-wrapped symmetric key.
     *
     * @param encryptedKey the new wrapped key bytes, or {@code null} to clear
     */
    public void setEncryptedKey(byte[] encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    /**
     * Replaces the AES-GCM nonce.
     *
     * @param nonce the new nonce bytes, or {@code null} to clear
     */
    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    /**
     * Replaces the AES-GCM ciphertext.
     *
     * @param encryptedData the new ciphertext bytes, or {@code null} to clear
     */
    public void setEncryptedData(byte[] encryptedData) {
        this.encryptedData = encryptedData;
    }

    /**
     * Replaces the AES-GCM authentication tag.
     *
     * @param authTag the new tag bytes, or {@code null} to clear
     */
    public void setAuthTag(byte[] authTag) {
        this.authTag = authTag;
    }
}
