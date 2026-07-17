package com.github.auties00.cobalt.wire.stanza.smax.account;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.List;
import java.util.Objects;

/**
 * Builds the outbound {@code <iq xmlns="urn:xmpp:whatsapp:account">} stanza
 * for a v3 payments ToS acceptance.
 *
 * <p>The request records the user's acceptance of a specific Brazilian-FBPAY
 * or Indian-UPI payment-terms version. The relay pairs it with one of the
 * documented {@link SmaxAccountSetPaymentsTOSv3Response} variants: the success
 * reply persists the acceptance server-side, while the error reply signals one
 * of six rejection reasons.
 *
 * @implNote
 * This implementation flattens the WA Web smax mixin chain (set-IQ +
 * base-IQ-set + the BR/UPI consumer mixin group) into a single
 * {@link #toStanza()} call that emits the full nested stanza in one pass.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutAccountSetPaymentsTOSv3Request")
@WhatsAppWebModule(moduleName = "WASmaxOutAccountSetIQMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutAccountBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutAccountSetPaymentsTOSv3BRConsumerOrSetPaymentsTOSv3UPIConsumerPaymentsTOSv3MixinGroup")
public final class SmaxAccountSetPaymentsTOSv3Request implements SmaxStanza.Request {
    /**
     * Holds the integer ToS version the user is accepting.
     *
     * <p>Routed verbatim into {@code <accept_pay tos_version=...>}; callers
     * pass the version they bump to after each server-side terms refresh.
     */
    private final int acceptPayTosVersion;

    /**
     * Holds the Brazilian-FBPAY or Indian-UPI consumer-variant payload.
     *
     * <p>Selected by the caller depending on the user's payment jurisdiction;
     * drives the {@code service} attribute and the {@code <additional_notice/>}
     * list emitted by {@link #toStanza()}.
     */
    private final SmaxAccountSetPaymentsTOSv3ConsumerVariant variant;

    /**
     * Constructs a payments-ToS-v3 acceptance request for dispatch through the
     * smax send pipeline.
     *
     * @param acceptPayTosVersion the integer ToS version being accepted
     * @param variant             the BR or UPI consumer variant payload; never
     *                            {@code null}
     * @throws NullPointerException if {@code variant} is {@code null}
     */
    public SmaxAccountSetPaymentsTOSv3Request(int acceptPayTosVersion, SmaxAccountSetPaymentsTOSv3ConsumerVariant variant) {
        this.acceptPayTosVersion = acceptPayTosVersion;
        this.variant = Objects.requireNonNull(variant, "variant cannot be null");
    }

    /**
     * Returns the integer ToS version this request is accepting.
     *
     * <p>Read by {@link #toStanza()} and exposed so consumers of
     * {@link SmaxAccountSetPaymentsTOSv3Response.Success} can correlate a reply
     * with its originating version.
     *
     * @return the version
     */
    public int acceptPayTosVersion() {
        return acceptPayTosVersion;
    }

    /**
     * Returns the consumer-variant payload selecting between Brazilian-FBPAY
     * and Indian-UPI terms.
     *
     * <p>Read by {@link #toStanza()} to decide which service literal and notice
     * list to emit.
     *
     * @return the variant; never {@code null}
     */
    public SmaxAccountSetPaymentsTOSv3ConsumerVariant variant() {
        return variant;
    }

    /**
     * Builds the outbound {@code <iq>} stanza ready for dispatch through the
     * smax send pipeline.
     *
     * <p>The returned {@link StanzaBuilder} carries an unfinalised IQ envelope so
     * the dispatch layer can stamp the {@code id} attribute. The stanza has
     * shape
     * {@snippet lang=xml :
     * <iq xmlns="urn:xmpp:whatsapp:account" type="set" to="s.whatsapp.net">
     *   <accept_pay version="3" tos_version="N" service="FBPAY|UPI">
     *     <additional_notice notice="..."/>
     *     ...
     *   </accept_pay>
     * </iq>
     * }
     *
     * @implNote
     * This implementation inlines the BR vs UPI dispatch as a Java
     * pattern-matching switch on
     * {@link SmaxAccountSetPaymentsTOSv3ConsumerVariant}; the {@code 1..10}
     * bound on {@code <additional_notice/>} is enforced upstream by the
     * consumer-variant constructor and not re-checked here.
     *
     * @return a {@link StanzaBuilder} carrying the partially-built IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutAccountSetPaymentsTOSv3Request",
            exports = "makeSetPaymentsTOSv3Request",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutAccountSetIQMixin",
            exports = "mergeSetIQMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutAccountBaseIQSetRequestMixin",
            exports = "mergeBaseIQSetRequestMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutAccountSetPaymentsTOSv3BRConsumerOrSetPaymentsTOSv3UPIConsumerPaymentsTOSv3MixinGroup",
            exports = "mergeSetPaymentsTOSv3BRConsumerOrSetPaymentsTOSv3UPIConsumerPaymentsTOSv3MixinGroup",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutAccountSetPaymentsTOSv3BRConsumerPaymentsTOSv3Mixin",
            exports = "makeSetPaymentsTOSv3BRConsumerPaymentsTOSv3AdditionalNotice",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutAccountSetPaymentsTOSv3BRConsumerPaymentsTOSv3Mixin",
            exports = "mergeSetPaymentsTOSv3BRConsumerPaymentsTOSv3Mixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutAccountSetPaymentsTOSv3UPIConsumerPaymentsTOSv3Mixin",
            exports = "makeSetPaymentsTOSv3UPIConsumerPaymentsTOSv3AdditionalNotice",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutAccountSetPaymentsTOSv3UPIConsumerPaymentsTOSv3Mixin",
            exports = "mergeSetPaymentsTOSv3UPIConsumerPaymentsTOSv3Mixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        String service;
        List<String> notices;
        switch (variant) {
            case SmaxAccountSetPaymentsTOSv3ConsumerVariant.BrConsumer brConsumer -> {
                service = "FBPAY";
                notices = brConsumer.additionalNotices();
            }
            case SmaxAccountSetPaymentsTOSv3ConsumerVariant.UpiConsumer upiConsumer -> {
                service = "UPI";
                notices = upiConsumer.additionalNotices();
            }
        }
        var noticeNodes = new Stanza[notices.size()];
        for (var i = 0; i < notices.size(); i++) {
            noticeNodes[i] = new StanzaBuilder()
                    .description("additional_notice")
                    .attribute("notice", notices.get(i))
                    .build();
        }
        var acceptPay = new StanzaBuilder()
                .description("accept_pay")
                .attribute("version", "3")
                .attribute("tos_version", acceptPayTosVersion)
                .attribute("service", service)
                .content(noticeNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "urn:xmpp:whatsapp:account")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(acceptPay);
    }

    /**
     * Compares this request to another for value equality on the ToS version
     * and consumer variant.
     *
     * <p>Two requests are equal iff they carry the same version and the same
     * {@link SmaxAccountSetPaymentsTOSv3ConsumerVariant}.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a
     *         {@link SmaxAccountSetPaymentsTOSv3Request} with equal components
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxAccountSetPaymentsTOSv3Request) obj;
        return this.acceptPayTosVersion == that.acceptPayTosVersion
                && Objects.equals(this.variant, that.variant);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(acceptPayTosVersion, variant);
    }

    /**
     * Returns a debug-friendly representation listing the ToS version and
     * consumer variant.
     *
     * <p>The format is intended for logging and is not part of any contract.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxAccountSetPaymentsTOSv3Request[acceptPayTosVersion=" + acceptPayTosVersion
                + ", variant=" + variant + ']';
    }
}
