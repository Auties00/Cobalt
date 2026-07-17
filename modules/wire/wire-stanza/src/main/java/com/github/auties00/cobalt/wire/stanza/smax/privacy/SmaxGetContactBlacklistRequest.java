package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Builds the outbound {@code <iq xmlns="privacy" type="get">} stanza that fetches a per-category contact-blacklist.
 *
 * <p>This stanza backs the Last-Seen, About, Group-Add, and Profile-Picture privacy surfaces. The caller maps a
 * privacy-list type to one of the four wire category names ({@code "last"}, {@code "status"}, {@code "groupadd"},
 * {@code "profile"}) and pairs it with an {@link SmaxGetContactBlacklistAddressingMode}; the
 * {@link SmaxGetContactBlacklistAddressingMode#LID} mode drives the LID-migration sub-flow. The reply is parsed
 * by {@link SmaxGetContactBlacklistResponse}.
 *
 * @implNote This implementation collapses the WA Web get-IQ mixin and the two LID and PN mixin arms into the
 * single {@link #toStanza()} below; the LID-versus-PN disambiguation reduces to the single {@code addressing_mode}
 * attribute on the inner {@code <privacy/>} element.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPrivacyGetContactBlacklistRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutPrivacyGetIQMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutPrivacyBaseIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutPrivacyGetContactBlacklistGetContactBlacklistLIDOrGetContactBlacklistPNMixinGroup")
@WhatsAppWebModule(moduleName = "WASmaxOutPrivacyGetContactBlacklistGetContactBlacklistLIDMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutPrivacyGetContactBlacklistGetContactBlacklistPNMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutPrivacyCategoryNamesForContactBlacklistMixin")
public final class SmaxGetContactBlacklistRequest implements SmaxStanza.Request {
    /**
     * The wire-level privacy category name routed into the {@code <list name=...>} attribute.
     *
     * <p>One of the four wire constants ({@code "last"}, {@code "status"}, {@code "groupadd"},
     * {@code "profile"}).
     */
    private final String categoryName;

    /**
     * The wire addressing mode selecting the LID or PN variant of the {@code <privacy/>} envelope.
     */
    private final SmaxGetContactBlacklistAddressingMode addressingMode;

    /**
     * Constructs a contact-blacklist request.
     *
     * <p>Pass {@link SmaxGetContactBlacklistAddressingMode#LID} to follow the LID migration path; the
     * {@link SmaxGetContactBlacklistAddressingMode#PN} mode is the legacy fallback retained for pre-migration
     * clients.
     *
     * @param categoryName   the wire category name; never {@code null}
     * @param addressingMode the addressing mode; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SmaxGetContactBlacklistRequest(String categoryName, SmaxGetContactBlacklistAddressingMode addressingMode) {
        this.categoryName = Objects.requireNonNull(categoryName, "categoryName cannot be null");
        this.addressingMode = Objects.requireNonNull(addressingMode, "addressingMode cannot be null");
    }

    /**
     * Returns the wire category name.
     *
     * @return the category name; never {@code null}
     */
    public String categoryName() {
        return categoryName;
    }

    /**
     * Returns the wire addressing mode.
     *
     * @return the addressing mode; never {@code null}
     */
    public SmaxGetContactBlacklistAddressingMode addressingMode() {
        return addressingMode;
    }

    /**
     * Builds the outbound {@code <iq>} stanza ready for dispatch.
     *
     * <p>The returned {@link StanzaBuilder} addresses {@code s.whatsapp.net} with {@code xmlns="privacy"} and
     * {@code type="get"}; the {@code id} attribute is generated downstream by the central client dispatcher. The
     * inner {@code <privacy><list value="contact_blacklist" name="..."/></privacy>} payload is fixed; the
     * {@code addressing_mode="lid"} attribute is added only when the addressing mode is
     * {@link SmaxGetContactBlacklistAddressingMode#LID}.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPrivacyGetContactBlacklistRequest",
            exports = "makeGetContactBlacklistRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var listNode = new StanzaBuilder()
                .description("list")
                .attribute("value", "contact_blacklist")
                .attribute("name", categoryName)
                .build();
        var privacyBuilder = new StanzaBuilder()
                .description("privacy");
        if (addressingMode == SmaxGetContactBlacklistAddressingMode.LID) {
            privacyBuilder.attribute("addressing_mode", "lid");
        }
        var privacyNode = privacyBuilder
                .content(listNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "privacy")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(privacyNode);
    }

    /**
     * Compares this request with another for equality by category name and addressing mode.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal {@link SmaxGetContactBlacklistRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGetContactBlacklistRequest) obj;
        return Objects.equals(this.categoryName, that.categoryName)
                && this.addressingMode == that.addressingMode;
    }

    /**
     * Returns a hash code derived from the category name and addressing mode.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(categoryName, addressingMode);
    }

    /**
     * Returns a debug representation carrying the category name and addressing mode.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxGetContactBlacklistRequest[categoryName=" + categoryName
                + ", addressingMode=" + addressingMode + ']';
    }
}
