package com.github.auties00.cobalt.wire.stanza.smax.psa;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

/**
 * Models the outbound {@code <iq type="get"><query><blocking_status/></query></iq>}
 * request that asks the relay whether the current account has muted the PSA
 * broadcast channel.
 *
 * <p>The request carries no fields; its stanza shape is fixed. The relay
 * answers with a {@link SmaxPsaChatBlockGetResponse}, whose
 * {@link SmaxPsaChatBlockGetResponse.Success} variant exposes the current
 * {@link SmaxPsaChatBlockGetBlockingStatus} that callers fold into the local
 * PSA-muted preference.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPsaChatBlockGetRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutPsaBaseIQGetRequestMixin")
public final class SmaxPsaChatBlockGetRequest implements SmaxStanza.Request {
    /**
     * Constructs the empty request.
     */
    public SmaxPsaChatBlockGetRequest() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds the {@code <iq xmlns="w:comms:chat" type="get">} envelope,
     * addressed to the {@linkplain JidServer#user() user server}, wrapping a
     * {@code <query><blocking_status/></query>} payload.
     *
     * @return a {@link StanzaBuilder} carrying the
     *         {@code <iq><query><blocking_status/></query></iq>} stanza
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPsaChatBlockGetRequest",
            exports = "makeChatBlockGetRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var blockingStatusNode = new StanzaBuilder()
                .description("blocking_status")
                .build();
        var queryNode = new StanzaBuilder()
                .description("query")
                .content(blockingStatusNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:comms:chat")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj != null && obj.getClass() == this.getClass();
    }

    @Override
    public int hashCode() {
        return SmaxPsaChatBlockGetRequest.class.hashCode();
    }

    @Override
    public String toString() {
        return "SmaxPsaChatBlockGetRequest[]";
    }
}
