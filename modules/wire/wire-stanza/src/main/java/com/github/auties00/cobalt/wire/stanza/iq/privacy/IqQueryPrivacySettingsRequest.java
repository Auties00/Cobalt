package com.github.auties00.cobalt.wire.stanza.iq.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

/**
 * Outbound legacy {@code <iq xmlns="privacy" type="get"><privacy/></iq>} stanza that fetches the
 * user's full set of privacy settings from the relay.
 * <p>
 * Dispatching this request seeds or refreshes the local privacy snapshot consumed by downstream
 * features (read-receipt gating, last-seen visibility, group-add audience, defense mode). The reply
 * is one of {@link IqQueryPrivacySettingsResponse.Success},
 * {@link IqQueryPrivacySettingsResponse.ClientError}, or
 * {@link IqQueryPrivacySettingsResponse.ServerError}.
 *
 * @implNote
 * This implementation emits only the legacy XML path; WA Web additionally has a MEX-based variant
 * gated by {@code mex_get_privacy_settings_mode} that fetches the same data over a GraphQL mutation.
 * Cobalt does not model the MEX variant.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryPrivacySettingsJob")
public final class IqQueryPrivacySettingsRequest implements IqStanza.Request {
    /**
     * Constructs a new request.
     * <p>
     * The request carries no parameters; the relay returns every documented category in one shot.
     */
    public IqQueryPrivacySettingsRequest() {
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation wraps an empty {@code <privacy/>} marker child in the canonical
     * {@code <iq xmlns="privacy" to="s.whatsapp.net" type="get">} envelope; no per-category
     * filtering is exposed on the wire.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQueryPrivacySettingsJob",
            exports = "getPrivacy", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var privacyNode = new StanzaBuilder()
                .description("privacy")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "privacy")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(privacyNode);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation treats the request as a singleton value type; all instances are equal to
     * each other.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj != null && obj.getClass() == this.getClass();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns a class-stable hash consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return IqQueryPrivacySettingsRequest.class.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits a parameterless debug representation; the format is not stable and
     * must not be parsed.
     */
    @Override
    public String toString() {
        return "IqQueryPrivacySettingsRequest[]";
    }
}
