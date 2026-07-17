package com.github.auties00.cobalt.wire.cloud.waba;

import java.util.Objects;
import java.util.Optional;

/**
 * The business encryption configuration of a WhatsApp Cloud API phone number.
 *
 * <p>Businesses that run Flows with an encrypted data-exchange endpoint, or that opt into encrypted
 * webhook payloads, upload an RSA public key to Meta; Meta encrypts the relevant payloads with that
 * key so only the holder of the matching private key can read them. This model carries the management
 * view of that configuration: the PEM-encoded public key currently stored for the phone number, and
 * the signature status Meta computed for it.
 */
public final class CloudBusinessEncryption {
    /**
     * The RSA public key currently stored for the phone number, in PEM format.
     */
    private final String businessPublicKey;

    /**
     * The signature status Meta computed for the stored key, or {@code null} when none was reported.
     */
    private final CloudBusinessEncryptionSignatureStatus businessPublicKeySignatureStatus;

    /**
     * Constructs a new business encryption configuration.
     *
     * @param businessPublicKey                the PEM-encoded RSA public key currently stored
     * @param businessPublicKeySignatureStatus the signature status, or {@code null} when none was
     *                                         reported
     * @throws NullPointerException if {@code businessPublicKey} is {@code null}
     */
    public CloudBusinessEncryption(String businessPublicKey,
                                   CloudBusinessEncryptionSignatureStatus businessPublicKeySignatureStatus) {
        this.businessPublicKey = Objects.requireNonNull(businessPublicKey, "businessPublicKey must not be null");
        this.businessPublicKeySignatureStatus = businessPublicKeySignatureStatus;
    }

    /**
     * Returns the RSA public key currently stored for the phone number.
     *
     * @return the PEM-encoded public key
     */
    public String businessPublicKey() {
        return businessPublicKey;
    }

    /**
     * Returns the signature status Meta computed for the stored key.
     *
     * @return an {@link Optional} carrying the {@link CloudBusinessEncryptionSignatureStatus}, or empty
     *         when none was reported
     */
    public Optional<CloudBusinessEncryptionSignatureStatus> businessPublicKeySignatureStatus() {
        return Optional.ofNullable(businessPublicKeySignatureStatus);
    }
}
