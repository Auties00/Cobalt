package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

/**
 * Builds the outbound {@code GetPrivacySetting} IQ request stanza.
 * <p>
 * The request reads the SMB data-sharing-with-Meta consent value for the active user before
 * the CTWA settings page surfaces the consent banner. The shape is parameter-free because the
 * relay enumerates the single documented consent surface on the active user's account and
 * accepts no client-side selectors.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBizSettingsGetPrivacySettingRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBizSettingsBaseIQGetRequestMixin")
public final class SmaxGetPrivacySettingRequest implements SmaxStanza.Request {
    /**
     * Constructs a request.
     */
    public SmaxGetPrivacySettingRequest() {
    }

    /**
     * Builds the outbound IQ stanza ready for dispatch.
     * <p>
     * Stamps the {@code xmlns="w:biz"} envelope with {@code to} addressed to the user server
     * and {@code type="get"}, and emits a bare {@code <privacy/>} child. The {@code id}
     * attribute is appended by Cobalt's send path.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the bare {@code <privacy/>} child
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBizSettingsGetPrivacySettingRequest",
            exports = "makeGetPrivacySettingRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizSettingsBaseIQGetRequestMixin",
            exports = "mergeBaseIQGetRequestMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var privacyNode = new StanzaBuilder()
                .description("privacy")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(privacyNode);
    }

    /**
     * Compares this request to another object for type equality.
     * <p>
     * All instances are interchangeable because the request carries no state.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxGetPrivacySettingRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj != null && obj.getClass() == this.getClass();
    }

    /**
     * Returns a constant hash code shared by all instances.
     *
     * @return the class hash code
     */
    @Override
    public int hashCode() {
        return SmaxGetPrivacySettingRequest.class.hashCode();
    }

    /**
     * Returns a constant debug rendering of this stateless request.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxGetPrivacySettingRequest[]";
    }
}
