package com.github.auties00.cobalt.wire.stanza.iq.disappearing;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

/**
 * Builds the outbound {@code <iq xmlns="disappearing_mode" type="get">} stanza that fetches the
 * account's current default disappearing-mode duration.
 *
 * <p>The duration returned by the relay is the timer applied to newly-created chats; it backs the
 * "default message timer" privacy setting. The request carries no parameters because the relay
 * infers the bound user from the authenticated session, addressing the stanza to
 * {@link JidServer#user()}. The matching reply is parsed by {@link IqQueryDisappearingModeResponse}.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryDisappearingModeJob")
public final class IqQueryDisappearingModeRequest implements IqStanza.Request {
    /**
     * Constructs a query-disappearing-mode request.
     *
     * <p>The request is stateless; the relay infers the bound user from the authenticated session.
     */
    public IqQueryDisappearingModeRequest() {
    }

    /**
     * Builds the outbound {@code <iq>} stanza wrapping the bare query envelope.
     *
     * <p>The returned {@link StanzaBuilder} is wire-ready except for the IQ {@code id} attribute,
     * which the dispatch layer assigns. The stanza carries no child payload.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQueryDisappearingModeJob",
            exports = "queryDisappearingMode",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "disappearing_mode")
                .attribute("to", JidServer.user())
                .attribute("type", "get");
    }

    /**
     * Compares this request to another object for equality.
     *
     * <p>Two query-disappearing-mode requests are equal when they share the same runtime class;
     * the type carries no state to distinguish instances.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a query-disappearing-mode request of the same class
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj != null && obj.getClass() == this.getClass();
    }

    /**
     * Returns a hash code for this request.
     *
     * <p>The hash is derived from the runtime class since the type is stateless.
     *
     * @return the class-derived hash code
     */
    @Override
    public int hashCode() {
        return IqQueryDisappearingModeRequest.class.hashCode();
    }

    /**
     * Returns a debug string for this request.
     *
     * @return a parameterless string representation
     */
    @Override
    public String toString() {
        return "IqQueryDisappearingModeRequest[]";
    }
}
