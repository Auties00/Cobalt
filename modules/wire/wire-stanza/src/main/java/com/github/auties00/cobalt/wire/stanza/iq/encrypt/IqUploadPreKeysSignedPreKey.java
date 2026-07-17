package com.github.auties00.cobalt.wire.stanza.iq.encrypt;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Typed container for the {@code <skey/>} subtree carrying the currently advertised signed pre-key.
 *
 * <p>One {@code <skey/>} subtree holds a three-byte big-endian {@code <id/>} content, a
 * thirty-two-byte raw {@code <value/>} content (the X25519 public key), and a sixty-four-byte
 * {@code <signature/>} content (the Ed25519 detached signature produced by the local identity key
 * over the public key). The same record is used both outbound (under {@link IqUploadPreKeysRequest},
 * {@link IqUploadPrekeysForRegRequest}, {@link IqRotateKeyRequest}) and as a parsed sub-projection
 * of {@link IqDigestKeyResponse.Success}.
 */
@WhatsAppWebModule(moduleName = "WAWebSignalUtilsApi")
public final class IqUploadPreKeysSignedPreKey {
    /**
     * The signed pre-key identifier; serialised as a three-byte big-endian unsigned integer into
     * the {@code <id/>} grandchild.
     */
    private final int id;

    /**
     * The thirty-two-byte X25519 public key carried verbatim by the {@code <value/>} grandchild.
     */
    private final byte[] publicKey;

    /**
     * The sixty-four-byte Ed25519 detached signature over {@link #publicKey()} produced by the
     * local identity key, carried verbatim by the {@code <signature/>} grandchild.
     */
    private final byte[] signature;

    /**
     * Constructs a populated signed pre-key.
     *
     * @param id        the signed pre-key identifier
     * @param publicKey the public-key bytes
     * @param signature the detached signature bytes
     * @throws NullPointerException if {@code publicKey} or {@code signature} is {@code null}
     */
    public IqUploadPreKeysSignedPreKey(int id, byte[] publicKey, byte[] signature) {
        this.id = id;
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey cannot be null");
        this.signature = Objects.requireNonNull(signature, "signature cannot be null");
    }

    /**
     * Returns the signed pre-key identifier.
     *
     * @return the identifier
     */
    public int id() {
        return id;
    }

    /**
     * Returns the signed pre-key public-key bytes.
     *
     * @return the public-key bytes
     */
    public byte[] publicKey() {
        return publicKey;
    }

    /**
     * Returns the detached signature bytes.
     *
     * @return the signature bytes
     */
    public byte[] signature() {
        return signature;
    }

    /**
     * Renders this signed pre-key as the canonical {@code <skey/>} subtree.
     *
     * <p>Called by {@link IqUploadPreKeysRequest#toStanza()},
     * {@link IqUploadPrekeysForRegRequest#toStanza()} and {@link IqRotateKeyRequest#toStanza()} to
     * assemble the {@code <skey/>} portion of the outbound payload.
     *
     * @implNote
     * This implementation packs the identifier with
     * {@link DataUtils#intToBytes(int, int) DataUtils.intToBytes(id, 3)} so the wire content is
     * three bytes regardless of {@link #id()}'s magnitude.
     *
     * @return the rendered {@code <skey/>} stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebSignalUtilsApi",
            exports = "xmppSignedPreKey", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza toNode() {
        var idNode = new StanzaBuilder()
                .description("id")
                .content(DataUtils.intToBytes(id, 3))
                .build();
        var valueNode = new StanzaBuilder()
                .description("value")
                .content(publicKey)
                .build();
        var signatureNode = new StanzaBuilder()
                .description("signature")
                .content(signature)
                .build();
        return new StanzaBuilder()
                .description("skey")
                .content(idNode, valueNode, signatureNode)
                .build();
    }

    /**
     * Compares this signed pre-key to another instance for equality.
     *
     * @param obj the candidate instance
     * @return {@code true} when {@code obj} is an {@code IqUploadPreKeysSignedPreKey} carrying
     *         the same identifier and identical key/signature bytes
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqUploadPreKeysSignedPreKey) obj;
        return this.id == that.id
                && Arrays.equals(this.publicKey, that.publicKey)
                && Arrays.equals(this.signature, that.signature);
    }

    /**
     * Returns a hash code derived from every carried field.
     *
     * @return the combined hash
     */
    @Override
    public int hashCode() {
        var result = Integer.hashCode(id);
        result = 31 * result + Arrays.hashCode(publicKey);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }

    /**
     * Returns the record-style rendering for this signed pre-key.
     *
     * @return the rendered string
     */
    @Override
    public String toString() {
        return "IqUploadPreKeysSignedPreKey[id=" + id
                + ", publicKey=" + Arrays.toString(publicKey)
                + ", signature=" + Arrays.toString(signature) + ']';
    }
}
