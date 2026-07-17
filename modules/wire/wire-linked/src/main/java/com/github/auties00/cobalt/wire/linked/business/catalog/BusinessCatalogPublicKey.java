package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Public key a WhatsApp Business account publishes so catalog data can be sent
 * to it securely.
 *
 * <p>When an app talks to a merchant's catalog over the merchant's private
 * (direct) connection, the catalog payloads are encrypted to the merchant. To
 * do that the app first fetches the merchant's published key. This model
 * carries the {@link #publicKeyPem() public key} in PEM text form, the matching
 * {@link #publicKeySignature() signature} that lets the app confirm the key was
 * issued by WhatsApp rather than tampered with, and optionally the full
 * {@link #certificatePem() certificate} the key was extracted from.
 */
@ProtobufMessage
public final class BusinessCatalogPublicKey {
    /**
     * PEM-encoded public key used to encrypt catalog payloads addressed to the
     * merchant. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String publicKeyPem;

    /**
     * Detached signature over {@link #publicKeyPem} that lets the caller verify
     * the key was issued by WhatsApp. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String publicKeySignature;

    /**
     * Full PEM-encoded certificate the public key was extracted from. Empty
     * when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String certificatePem;

    /**
     * Constructs a new {@code BusinessCatalogPublicKey}. Every argument is
     * optional and may be {@code null} when the server omitted the field.
     *
     * @param publicKeyPem       the PEM-encoded public key, or {@code null}
     * @param publicKeySignature the detached key signature, or {@code null}
     * @param certificatePem     the full PEM certificate, or {@code null}
     */
    BusinessCatalogPublicKey(String publicKeyPem, String publicKeySignature, String certificatePem) {
        this.publicKeyPem = publicKeyPem;
        this.publicKeySignature = publicKeySignature;
        this.certificatePem = certificatePem;
    }

    /**
     * Returns the PEM-encoded public key used to encrypt catalog payloads
     * addressed to the merchant.
     *
     * @return an {@code Optional} containing the public key, or empty when the
     *         server omitted it
     */
    public Optional<String> publicKeyPem() {
        return Optional.ofNullable(publicKeyPem);
    }

    /**
     * Returns the detached signature over the public key that lets the caller
     * verify the key was issued by WhatsApp.
     *
     * @return an {@code Optional} containing the signature, or empty when the
     *         server omitted it
     */
    public Optional<String> publicKeySignature() {
        return Optional.ofNullable(publicKeySignature);
    }

    /**
     * Returns the full PEM-encoded certificate the public key was extracted
     * from.
     *
     * @return an {@code Optional} containing the certificate, or empty when the
     *         server omitted it
     */
    public Optional<String> certificatePem() {
        return Optional.ofNullable(certificatePem);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCatalogPublicKey) obj;
        return Objects.equals(this.publicKeyPem, that.publicKeyPem) &&
               Objects.equals(this.publicKeySignature, that.publicKeySignature) &&
               Objects.equals(this.certificatePem, that.certificatePem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicKeyPem, publicKeySignature, certificatePem);
    }

    @Override
    public String toString() {
        return "BusinessCatalogPublicKey[" +
               "publicKeyPem=" + publicKeyPem + ", " +
               "publicKeySignature=" + publicKeySignature + ", " +
               "certificatePem=" + certificatePem + ']';
    }
}
