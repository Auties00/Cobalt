package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Per-purpose public-key material returned by a cross-posting eligibility
 * evaluation.
 *
 * <p>WhatsApp encrypts the cross-posted payload under a per-purpose public-key
 * envelope. The server returns the ephemeral public key, the identity public
 * key and its signature, the identity-key encryption certificate, and a dummy
 * ciphertext-nonce pair the WhatsApp client uses to validate the encryption
 * parameters. Each value is exposed as the raw base64-encoded byte blob the
 * server emits.
 */
@ProtobufMessage(name = "CrossPostingPublicKeys")
public final class CrossPostingPublicKeys {
    /**
     * Base64-encoded ephemeral public key. {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String ephemeralPublicKey;

    /**
     * Base64-encoded identity public key. {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String identityPublicKey;

    /**
     * Base64-encoded identity-key signature. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String identityPublicKeySignature;

    /**
     * Base64-encoded identity-key encryption certificate. {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String identityPublicKeyEncryptionCertificate;

    /**
     * Base64-encoded dummy ciphertext used to validate the encryption
     * parameters. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String dummyCiphertext;

    /**
     * Base64-encoded dummy nonce used to validate the encryption parameters.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String dummyNonce;

    /**
     * Constructs a new {@code CrossPostingPublicKeys}. The reference arguments
     * may be {@code null} when the server omitted them.
     *
     * @param ephemeralPublicKey                     the base64 ephemeral public
     *                                               key, or {@code null}
     * @param identityPublicKey                      the base64 identity public
     *                                               key, or {@code null}
     * @param identityPublicKeySignature             the base64 identity-key
     *                                               signature, or {@code null}
     * @param identityPublicKeyEncryptionCertificate the base64 identity-key
     *                                               encryption certificate, or
     *                                               {@code null}
     * @param dummyCiphertext                        the base64 dummy ciphertext,
     *                                               or {@code null}
     * @param dummyNonce                             the base64 dummy nonce, or
     *                                               {@code null}
     */
    CrossPostingPublicKeys(String ephemeralPublicKey,
                           String identityPublicKey,
                           String identityPublicKeySignature,
                           String identityPublicKeyEncryptionCertificate,
                           String dummyCiphertext,
                           String dummyNonce) {
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.identityPublicKey = identityPublicKey;
        this.identityPublicKeySignature = identityPublicKeySignature;
        this.identityPublicKeyEncryptionCertificate = identityPublicKeyEncryptionCertificate;
        this.dummyCiphertext = dummyCiphertext;
        this.dummyNonce = dummyNonce;
    }

    /**
     * Returns the base64-encoded ephemeral public key.
     *
     * @return the ephemeral public key, or empty when the server omitted it
     */
    public Optional<String> ephemeralPublicKey() {
        return Optional.ofNullable(ephemeralPublicKey);
    }

    /**
     * Returns the base64-encoded identity public key.
     *
     * @return the identity public key, or empty when the server omitted it
     */
    public Optional<String> identityPublicKey() {
        return Optional.ofNullable(identityPublicKey);
    }

    /**
     * Returns the base64-encoded identity-key signature.
     *
     * @return the identity-key signature, or empty when the server omitted it
     */
    public Optional<String> identityPublicKeySignature() {
        return Optional.ofNullable(identityPublicKeySignature);
    }

    /**
     * Returns the base64-encoded identity-key encryption certificate.
     *
     * @return the encryption certificate, or empty when the server omitted it
     */
    public Optional<String> identityPublicKeyEncryptionCertificate() {
        return Optional.ofNullable(identityPublicKeyEncryptionCertificate);
    }

    /**
     * Returns the base64-encoded dummy ciphertext.
     *
     * @return the dummy ciphertext, or empty when the server omitted it
     */
    public Optional<String> dummyCiphertext() {
        return Optional.ofNullable(dummyCiphertext);
    }

    /**
     * Returns the base64-encoded dummy nonce.
     *
     * @return the dummy nonce, or empty when the server omitted it
     */
    public Optional<String> dummyNonce() {
        return Optional.ofNullable(dummyNonce);
    }
}
