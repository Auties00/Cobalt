package com.github.auties00.cobalt.wire.stanza.smax.waffle;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the shared encryption-metadata subtree carried by every Waffle RPC that exchanges encrypted payloads.
 * <p>
 * This subtree frames an RSA-2048-wrapped AES-GCM session over the WhatsApp Web account-linking surface,
 * which negotiates Facebook account linking for federated features such as Channels, Crossposting, and the
 * Communities entry-point. It holds four cryptographic blobs: the RSA-wrapped AES session key, the AES-GCM
 * nonce, the AES-GCM ciphertext, and the AES-GCM authentication tag. Embedders construct an instance from
 * a payload-wrap result, then hand it to whichever Waffle request they are dispatching, and parse the
 * counterpart back out of the corresponding success reply.
 *
 * @implNote This implementation collapses WhatsApp Web's separate outbound merge helper and inbound parser
 * into a single Java value class. The byte-array fields are stored by reference rather than defensively
 * copied; callers must not mutate the supplied arrays after construction.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutWaffleRSAEncryptionMetadataMixin")
@WhatsAppWebModule(moduleName = "WASmaxInWaffleRSAEncryptionMetadataMixin")
public final class SmaxWaffleRsaEncryptionMetadata {
    /**
     * Holds the RSA-2048-wrapped AES session key.
     */
    private final byte[] encryptedKey;

    /**
     * Holds the AES-GCM nonce.
     */
    private final byte[] nonce;

    /**
     * Holds the AES-GCM ciphertext.
     */
    private final byte[] encryptedData;

    /**
     * Holds the AES-GCM authentication tag.
     */
    private final byte[] authTag;

    /**
     * Constructs a metadata instance from the four pre-computed cryptographic blobs.
     *
     * @param encryptedKey  the RSA-wrapped AES session key; never {@code null}
     * @param nonce         the AES-GCM nonce; never {@code null}
     * @param encryptedData the AES-GCM ciphertext; never {@code null}
     * @param authTag       the AES-GCM authentication tag; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxWaffleRsaEncryptionMetadata(byte[] encryptedKey, byte[] nonce,
                                           byte[] encryptedData, byte[] authTag) {
        this.encryptedKey = Objects.requireNonNull(encryptedKey, "encryptedKey cannot be null");
        this.nonce = Objects.requireNonNull(nonce, "nonce cannot be null");
        this.encryptedData = Objects.requireNonNull(encryptedData, "encryptedData cannot be null");
        this.authTag = Objects.requireNonNull(authTag, "authTag cannot be null");
    }

    /**
     * Returns the RSA-wrapped AES session key.
     *
     * @return the wrapped key bytes; never {@code null}
     */
    public byte[] encryptedKey() {
        return encryptedKey;
    }

    /**
     * Returns the AES-GCM nonce.
     *
     * @return the nonce bytes; never {@code null}
     */
    public byte[] nonce() {
        return nonce;
    }

    /**
     * Returns the AES-GCM ciphertext.
     *
     * @return the ciphertext bytes; never {@code null}
     */
    public byte[] encryptedData() {
        return encryptedData;
    }

    /**
     * Returns the AES-GCM authentication tag.
     *
     * @return the tag bytes; never {@code null}
     */
    public byte[] authTag() {
        return authTag;
    }

    /**
     * Builds the {@code <encryption_metadata/>} subtree wrapping the four cryptographic blobs.
     * <p>
     * The subtree is the encryption-metadata child of the outer {@code <iq xmlns="waffle"/>} envelope and
     * carries the encrypted-key, nonce, encrypted-data, and auth-tag children. The fixed {@code version="1"}
     * and {@code algorithm="rsa2048"} attributes match the relay's RSA-2048 verification.
     *
     * @return the {@code <encryption_metadata/>} {@link Stanza}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutWaffleRSAEncryptionMetadataMixin",
            exports = "mergeRSAEncryptionMetadataMixin", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza toStanza() {
        var encryptedKeyNode = new StanzaBuilder()
                .description("encrypted_key")
                .content(encryptedKey)
                .build();
        var nonceNode = new StanzaBuilder()
                .description("nonce")
                .content(nonce)
                .build();
        var encryptedDataNode = new StanzaBuilder()
                .description("encrypted_data")
                .content(encryptedData)
                .build();
        var authTagNode = new StanzaBuilder()
                .description("auth_tag")
                .content(authTag)
                .build();
        return new StanzaBuilder()
                .description("encryption_metadata")
                .attribute("version", "1")
                .attribute("algorithm", "rsa2048")
                .content(encryptedKeyNode, nonceNode, encryptedDataNode, authTagNode)
                .build();
    }

    /**
     * Parses an inbound {@code <encryption_metadata/>} subtree.
     * <p>
     * Used by every Waffle success reply that receives encryption metadata back from the relay. The returned
     * instance owns the four extracted byte blobs that embedders subsequently decrypt. Returns an empty
     * {@link Optional} when the stanza does not match the expected shape.
     *
     * @implNote This implementation gates on the fixed {@code version="1"} and {@code algorithm="rsa2048"}
     * attributes and on the presence of all four child elements; the per-blob size ranges WhatsApp Web
     * enforces (key 1-2048 bytes, nonce 1-128, data 1-8192, tag 1-128) are not re-checked after the presence test.
     *
     * @param stanza the {@code <encryption_metadata/>} stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed metadata, or empty when the stanza does not match the expected shape
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInWaffleRSAEncryptionMetadataMixin",
            exports = "parseRSAEncryptionMetadataMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxWaffleRsaEncryptionMetadata> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasAttribute("version", "1")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("algorithm", "rsa2048")) {
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
        return Optional.of(new SmaxWaffleRsaEncryptionMetadata(encryptedKey, nonce, encryptedData, authTag));
    }

    /**
     * Returns whether the given object is a {@link SmaxWaffleRsaEncryptionMetadata} with equal cryptographic blobs.
     * <p>
     * The four byte arrays are compared element-wise.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when the four byte arrays match element-wise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxWaffleRsaEncryptionMetadata) obj;
        return Arrays.equals(this.encryptedKey, that.encryptedKey)
                && Arrays.equals(this.nonce, that.nonce)
                && Arrays.equals(this.encryptedData, that.encryptedData)
                && Arrays.equals(this.authTag, that.authTag);
    }

    /**
     * Returns a hash code derived from the four cryptographic blobs.
     *
     * @return a content-based hash consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        var result = Arrays.hashCode(encryptedKey);
        result = 31 * result + Arrays.hashCode(nonce);
        result = 31 * result + Arrays.hashCode(encryptedData);
        result = 31 * result + Arrays.hashCode(authTag);
        return result;
    }

    /**
     * Returns a debug rendering of this metadata.
     * <p>
     * Each blob is summarised as its length rather than its contents, both to bound the output and to avoid
     * leaking sensitive ciphertext into log files.
     *
     * @return a human-readable summary; never {@code null}
     */
    @Override
    public String toString() {
        return "SmaxWaffleRsaEncryptionMetadata[encryptedKey="
                + (encryptedKey != null ? encryptedKey.length + " bytes" : "null")
                + ", nonce="
                + (nonce != null ? nonce.length + " bytes" : "null")
                + ", encryptedData="
                + (encryptedData != null ? encryptedData.length + " bytes" : "null")
                + ", authTag="
                + (authTag != null ? authTag.length + " bytes" : "null") + ']';
    }
}
