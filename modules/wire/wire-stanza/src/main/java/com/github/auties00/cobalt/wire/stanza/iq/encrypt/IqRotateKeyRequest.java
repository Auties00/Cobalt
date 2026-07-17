package com.github.auties00.cobalt.wire.stanza.iq.encrypt;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;
import java.util.Objects;

/**
 * Builds the {@code <iq xmlns="encrypt" type="set"/>} that uploads a freshly generated signed
 * pre-key to the relay, replacing the current one.
 *
 * <p>This request backs the rolling rotation of the local long-lived signed pre-key. WA Web
 * schedules it as a persisted job periodically driven by the rotation policy; on a {@code 409}
 * response or an unknown error the same job falls back to {@link IqDigestKeyRequest} to revalidate
 * the relay's view of the key bundle. The request body is a single {@code <rotate/>} child wrapping
 * the canonical {@code <skey/>} subtree.
 */
@WhatsAppWebModule(moduleName = "WAWebRotateKeyJob")
public final class IqRotateKeyRequest implements IqStanza.Request {
    /**
     * The freshly generated signed pre-key being uploaded, rendered verbatim into the
     * {@code <skey/>} grandchild.
     */
    private final IqUploadPreKeysSignedPreKey signedPreKey;

    /**
     * Constructs a rotate-key request for the supplied signed pre-key.
     *
     * <p>The caller is expected to have already persisted the new signed pre-key against the local
     * Signal store before issuing this request; on success the upload is the final step of the
     * rotation, on failure the local store still records the new key for retry.
     *
     * @param signedPreKey the freshly generated signed pre-key
     * @throws NullPointerException if {@code signedPreKey} is {@code null}
     */
    public IqRotateKeyRequest(IqUploadPreKeysSignedPreKey signedPreKey) {
        this.signedPreKey = Objects.requireNonNull(signedPreKey, "signedPreKey cannot be null");
    }

    /**
     * Returns the signed pre-key being uploaded.
     *
     * @return the signed pre-key
     */
    public IqUploadPreKeysSignedPreKey signedPreKey() {
        return signedPreKey;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces an {@code <iq>} addressed to {@link JidServer#user()} with {@code xmlns="encrypt"}
     * and {@code type="set"}, wrapping the {@link IqUploadPreKeysSignedPreKey#toNode()} render under
     * a single {@code <rotate/>} parent.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebRotateKeyJob",
            exports = "rotateKey", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var skeyNode = signedPreKey.toNode();
        var rotateNode = new StanzaBuilder()
                .description("rotate")
                .content(skeyNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("id", RandomIdUtils.newId())
                .attribute("xmlns", "encrypt")
                .attribute("type", "set")
                .attribute("to", JidServer.user())
                .content(rotateNode);
    }

    /**
     * Compares this request to another instance for equality.
     *
     * @param obj the candidate instance
     * @return {@code true} when {@code obj} is an {@code IqRotateKeyRequest} carrying an equal
     *         signed pre-key
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqRotateKeyRequest) obj;
        return Objects.equals(this.signedPreKey, that.signedPreKey);
    }

    /**
     * Returns a hash code derived from the signed pre-key.
     *
     * @return the combined hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(signedPreKey);
    }

    /**
     * Returns the record-style rendering for this request.
     *
     * @return the rendered string
     */
    @Override
    public String toString() {
        return "IqRotateKeyRequest[signedPreKey=" + signedPreKey + ']';
    }
}
