package com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the optional {@code <encryption-metadata version="1" algorithm="aes-256-gcm"/>}
 * child element nested inside a {@code <pair-success/>} stanza.
 *
 * <p>The pair-success flow uses this AES-256-GCM-wrapped envelope to deliver per-account
 * post-pairing key material, currently the ADV canonical-registration payload. Cobalt surfaces
 * it through {@link SmaxMdSetRegResponse#pairSuccessEncryptionMetadata()} so the registration
 * handler can unwrap the inner payload after the device identity has been verified.
 *
 * @implNote This implementation enforces the same literals as WA Web: {@code version="1"} and
 * {@code algorithm="aes-256-gcm"} are required, and the four nested byte-payload children
 * {@code encrypted_key}, {@code nonce}, {@code encrypted_data}, and {@code auth_tag} must each
 * resolve to non-empty bytes. Upstream additionally constrains the byte lengths to inclusive
 * ranges ({@code 1..2048}, {@code 1..128}, {@code 1..8192}, {@code 1..128}); Cobalt accepts any
 * non-empty payload here and defers length validation to the GCM unwrap step.
 */
@WhatsAppWebModule(moduleName = "WASmaxInMdAESEncryptionMetadataMixin")
public final class SmaxMdSetRegEncryptionMetadata {
    /**
     * Holds the {@code version} attribute literal, always {@code "1"}.
     *
     * <p>Reserved for forward-compatibility; only schema version 1 is defined today and the
     * parser rejects any other value.
     */
    private final String version;

    /**
     * Holds the {@code algorithm} attribute literal, always {@code "aes-256-gcm"}.
     *
     * <p>Any deviation surfaces as {@link Optional#empty()} from {@link #of(Stanza)}.
     */
    private final String algorithm;

    /**
     * Holds the wrapped key material carried in {@code <encrypted_key/>}.
     *
     * <p>The AES-256-GCM-encrypted symmetric key that the GCM unwrap step recovers; the recipient
     * decrypts it using the ADV secret key.
     */
    private final byte[] encryptedKey;

    /**
     * Holds the GCM nonce carried in {@code <nonce/>}.
     *
     * <p>Combined with {@link #encryptedKey()} during the GCM unwrap step to recover the inner
     * ciphertext key.
     */
    private final byte[] nonce;

    /**
     * Holds the wrapped payload carried in {@code <encrypted_data/>}.
     *
     * <p>The AES-256-GCM ciphertext that, once decrypted, yields the inner post-pairing payload
     * consumed by the canonical-registration handler.
     */
    private final byte[] encryptedData;

    /**
     * Holds the GCM authentication tag carried in {@code <auth_tag/>}.
     *
     * <p>Verifies the integrity of {@link #encryptedData()} during the GCM unwrap step.
     */
    private final byte[] authTag;

    /**
     * Constructs the typed projection from already-validated component fields.
     *
     * <p>This is the target of {@link #of(Stanza)} after parsing has succeeded. Public visibility
     * is preserved so unit tests can construct fixtures.
     *
     * @param version       the version literal; never {@code null}
     * @param algorithm     the algorithm literal; never {@code null}
     * @param encryptedKey  the wrapped key bytes; never {@code null}
     * @param nonce         the GCM nonce bytes; never {@code null}
     * @param encryptedData the ciphertext bytes; never {@code null}
     * @param authTag       the GCM auth-tag bytes; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxMdSetRegEncryptionMetadata(String version, String algorithm,
                              byte[] encryptedKey, byte[] nonce,
                              byte[] encryptedData, byte[] authTag) {
        this.version = Objects.requireNonNull(version, "version cannot be null");
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm cannot be null");
        this.encryptedKey = Objects.requireNonNull(encryptedKey, "encryptedKey cannot be null");
        this.nonce = Objects.requireNonNull(nonce, "nonce cannot be null");
        this.encryptedData = Objects.requireNonNull(encryptedData, "encryptedData cannot be null");
        this.authTag = Objects.requireNonNull(authTag, "authTag cannot be null");
    }

    /**
     * Returns the version literal.
     *
     * @return the version; never {@code null} and always {@code "1"}
     */
    public String version() {
        return version;
    }

    /**
     * Returns the algorithm literal.
     *
     * @return the algorithm; never {@code null} and always {@code "aes-256-gcm"}
     */
    public String algorithm() {
        return algorithm;
    }

    /**
     * Returns the wrapped key bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] encryptedKey() {
        return encryptedKey;
    }

    /**
     * Returns the GCM nonce bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] nonce() {
        return nonce;
    }

    /**
     * Returns the ciphertext bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] encryptedData() {
        return encryptedData;
    }

    /**
     * Returns the GCM auth-tag bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] authTag() {
        return authTag;
    }

    /**
     * Parses an {@code <encryption-metadata/>} child stanza into a typed projection.
     *
     * <p>Invoked by {@link SmaxMdSetRegResponse#of(Stanza)} for the optional
     * {@code <encryption-metadata/>} child of {@code <pair-success/>}; the result is
     * {@link Optional#empty()} when the inner shape fails any schema check, rather than an
     * exception.
     *
     * @implNote This implementation enforces tag-content matching on the four nested byte
     * payloads ({@code encrypted_key}, {@code nonce}, {@code encrypted_data}, {@code auth_tag})
     * but skips WA Web's inclusive byte-length range checks; the GCM unwrap step rejects malformed
     * payloads downstream.
     *
     * @param stanza the {@code <encryption-metadata/>} child stanza
     * @return an {@link Optional} carrying the projection, or empty when the stanza shape diverges
     *         from the schema
     */
    @WhatsAppWebExport(moduleName = "WASmaxInMdAESEncryptionMetadataMixin",
            exports = "parseAESEncryptionMetadataMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxMdSetRegEncryptionMetadata> of(Stanza stanza) {
        if (!stanza.hasAttribute("version", "1")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("algorithm", "aes-256-gcm")) {
            return Optional.empty();
        }
        var encryptedKey = stanza.getChild("encrypted_key")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (encryptedKey == null) {
            return Optional.empty();
        }
        var nonce = stanza.getChild("nonce")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (nonce == null) {
            return Optional.empty();
        }
        var encryptedData = stanza.getChild("encrypted_data")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (encryptedData == null) {
            return Optional.empty();
        }
        var authTag = stanza.getChild("auth_tag")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (authTag == null) {
            return Optional.empty();
        }
        return Optional.of(new SmaxMdSetRegEncryptionMetadata("1", "aes-256-gcm", encryptedKey, nonce, encryptedData, authTag));
    }

    /**
     * Compares this projection to another object for value equality.
     *
     * <p>Two projections are equal when their version, algorithm, and all four byte-payload
     * fields match element by element.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an equal projection
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxMdSetRegEncryptionMetadata) obj;
        return Objects.equals(this.version, that.version)
                && Objects.equals(this.algorithm, that.algorithm)
                && Arrays.equals(this.encryptedKey, that.encryptedKey)
                && Arrays.equals(this.nonce, that.nonce)
                && Arrays.equals(this.encryptedData, that.encryptedData)
                && Arrays.equals(this.authTag, that.authTag);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The byte-payload fields contribute through {@link Arrays#hashCode(byte[])} so equal
     * contents yield equal codes.
     *
     * @return the hash code derived from the version, algorithm, and byte payloads
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(version, algorithm);
        result = 31 * result + Arrays.hashCode(encryptedKey);
        result = 31 * result + Arrays.hashCode(nonce);
        result = 31 * result + Arrays.hashCode(encryptedData);
        result = 31 * result + Arrays.hashCode(authTag);
        return result;
    }

    /**
     * Returns a debug string listing the version, algorithm, and byte payloads.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxMdSetRegEncryptionMetadata[version=" + version
                + ", algorithm=" + algorithm
                + ", encryptedKey=" + Arrays.toString(encryptedKey)
                + ", nonce=" + Arrays.toString(nonce)
                + ", encryptedData=" + Arrays.toString(encryptedData)
                + ", authTag=" + Arrays.toString(authTag) + ']';
    }
}
