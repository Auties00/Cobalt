package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;
import java.util.Optional;

/**
 * The outbound stanza that writes the SMB-to-Meta data-sharing
 * consent to the relay.
 *
 * <p>Propagates the user's choice on the SMB data-sharing settings screen. The
 * {@code dataSharingConsent} payload accepts one of {@code "true"} / {@code "false"} /
 * {@code "notset"}; passing {@code null} omits the inner consent child entirely.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBizSettingsSetPrivacySettingRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBizSettingsSmbDataSharingSettingMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBizSettingsSmbDataSharingSettingValueMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBizSettingsBaseIQSetRequestMixin")
public final class SmaxSetPrivacySettingRequest implements SmaxStanza.Request {
    /**
     * The optional consent value; {@code null} omits the inner
     * {@code <smb_data_sharing_with_meta_consent>} child.
     */
    private final String dataSharingConsent;

    /**
     * Constructs a new request optionally carrying a consent value.
     *
     * <p>Pass {@code null} to clear the stored choice (the inner consent child is omitted); pass
     * one of {@code "true"} / {@code "false"} / {@code "notset"} to record the user's preference.
     *
     * @param dataSharingConsent the consent value; may be
     *                           {@code null}
     */
    public SmaxSetPrivacySettingRequest(String dataSharingConsent) {
        this.dataSharingConsent = dataSharingConsent;
    }

    /**
     * Returns the optional consent value.
     *
     * <p>Returns {@link Optional#empty()} when the request was built without a consent payload.
     *
     * @return an {@link Optional} carrying the consent value
     */
    public Optional<String> dataSharingConsent() {
        return Optional.ofNullable(dataSharingConsent);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation stamps {@code xmlns="w:biz"}, {@code type="set"},
     * {@code to="s.whatsapp.net"} and emits a {@code <privacy>} child carrying the optional
     * {@code <smb_data_sharing_with_meta_consent value="..."/>} inner; the IQ {@code id} is
     * assigned by the dispatcher. The {@code value} attribute is stamped on the inner consent child
     * directly, fusing the JS pair {@code mergeSmbDataSharingSettingMixin} and
     * {@code mergeSmbDataSharingSettingValueMixin} into a single builder call. The JS pipeline
     * materialises an intermediate value-carrying stanza and folds it via {@code mergeStanzas}; the
     * wire result is identical.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBizSettingsSetPrivacySettingRequest",
            exports = "makeSetPrivacySettingRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizSettingsBaseIQSetRequestMixin",
            exports = "mergeBaseIQSetRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizSettingsSmbDataSharingSettingMixin",
            exports = "mergeSmbDataSharingSettingMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizSettingsSmbDataSharingSettingValueMixin",
            exports = "mergeSmbDataSharingSettingValueMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var privacyBuilder = new StanzaBuilder()
                .description("privacy");
        if (dataSharingConsent != null) {
            var consentNode = new StanzaBuilder()
                    .description("smb_data_sharing_with_meta_consent")
                    .attribute("value", dataSharingConsent)
                    .build();
            privacyBuilder.content(consentNode);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(privacyBuilder.build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxSetPrivacySettingRequest) obj;
        return Objects.equals(this.dataSharingConsent, that.dataSharingConsent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(dataSharingConsent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "SmaxSetPrivacySettingRequest[dataSharingConsent=" + dataSharingConsent + ']';
    }
}
