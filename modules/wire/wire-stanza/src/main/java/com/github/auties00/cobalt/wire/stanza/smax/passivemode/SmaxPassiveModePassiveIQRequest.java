package com.github.auties00.cobalt.wire.stanza.smax.passivemode;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

/**
 * Models the outbound {@code <iq xmlns="passive" type="set">} stanza that pins the connection back into passive mode.
 *
 * <p>While in passive mode the relay buffers live messages so that the client can finish a long startup task without
 * dropped or interleaved deliveries. This stanza is the inverse of its {@link SmaxPassiveModeActiveIQRequest active}
 * counterpart and is rarely needed by clients that never expose a passive-mode lifecycle. The request carries no
 * payload beyond a single empty {@code <passive/>} child that discriminates the passive transition.
 *
 * @implNote
 * This implementation carries no per-call state, so a single instance can be reused across the connection lifecycle.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPassiveModePassiveIQRequest")
public final class SmaxPassiveModePassiveIQRequest implements SmaxStanza.Request {

    /**
     * Constructs an empty request envelope.
     *
     * <p>There is no payload to supply; the stanza shape is fixed and rendered entirely by {@link #toStanza()}.
     */
    public SmaxPassiveModePassiveIQRequest() {
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation hard-codes {@code xmlns="passive"}, {@code type="set"}, and {@code to=s.whatsapp.net}
     * resolved via {@link JidServer#user()}, then nests a single empty {@code <passive/>} child as the payload
     * discriminator.
     * @return the outbound stanza builder; never {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPassiveModePassiveIQRequest",
            exports = "makePassiveIQRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var passiveNode = new StanzaBuilder()
                .description("passive")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "passive")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(passiveNode);
    }

    /**
     * Indicates whether the given object is equal to this request.
     *
     * <p>Any instance of this type is equal to any other because the request carries no per-instance state.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a {@code SmaxPassiveModePassiveIQRequest}
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
     * <p>The hash is class-level so that it stays consistent with {@link #equals(Object)}, which treats every instance
     * as equal.
     *
     * @return the class-level hash code
     */
    @Override
    public int hashCode() {
        return SmaxPassiveModePassiveIQRequest.class.hashCode();
    }

    /**
     * Returns the string representation of this request.
     *
     * <p>The rendering mirrors the record-like form used across the {@code Smax*} stanza family.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxPassiveModePassiveIQRequest[]";
    }
}
