package com.github.auties00.cobalt.wire.stanza.iq.encrypt;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

/**
 * Builds the outbound {@code <iq xmlns="encrypt" type="get"/>} that asks the relay to echo back its
 * recorded Signal key-bundle digest for the local user.
 *
 * <p>This request backs the digest-key sanity check that fires when the relay sends an
 * {@code <notification type="encrypt"><digest/></notification>} push. The client compares the
 * relay's SHA-1 over {@code identityPubKey || skeyPubKey || skeySignature || preKeyPubKeys} against
 * a local recomputation, and on mismatch falls back to a full
 * {@link IqUploadPreKeysRequest pre-key re-upload}. The request carries no payload other than a bare
 * {@code <digest/>} child.
 */
@WhatsAppWebModule(moduleName = "WAWebDigestKeyJob")
public final class IqDigestKeyRequest implements IqStanza.Request {
    /**
     * Constructs an empty digest-key probe.
     *
     * <p>The request body has no parameters; the only outbound information is the {@code <digest/>}
     * tag and the IQ envelope identifier.
     */
    public IqDigestKeyRequest() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces an {@code <iq>} addressed to {@link JidServer#user()} with {@code xmlns="encrypt"}
     * and {@code type="get"}, carrying a single empty {@code <digest/>} child and a freshly minted
     * {@link RandomIdUtils#newId() request identifier}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebDigestKeyJob",
            exports = "digestKey", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var digestNode = new StanzaBuilder()
                .description("digest")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("id", RandomIdUtils.newId())
                .attribute("xmlns", "encrypt")
                .attribute("type", "get")
                .attribute("to", JidServer.user())
                .content(digestNode);
    }

    /**
     * Compares this request to another instance for equality.
     *
     * <p>All instances of {@code IqDigestKeyRequest} are interchangeable because the request carries
     * no per-instance state; equality reduces to a class-identity check.
     *
     * @param obj the candidate instance
     * @return {@code true} when {@code obj} is a non-{@code null} {@code IqDigestKeyRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj != null && obj.getClass() == this.getClass();
    }

    /**
     * Returns a stable hash code shared by all instances.
     *
     * @return the class identity hash
     */
    @Override
    public int hashCode() {
        return IqDigestKeyRequest.class.hashCode();
    }

    /**
     * Returns the canonical record-style rendering for this request.
     *
     * @return the literal {@code "IqDigestKeyRequest[]"}
     */
    @Override
    public String toString() {
        return "IqDigestKeyRequest[]";
    }
}
