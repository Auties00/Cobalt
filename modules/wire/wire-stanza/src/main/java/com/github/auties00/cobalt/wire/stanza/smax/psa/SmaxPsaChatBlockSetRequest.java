package com.github.auties00.cobalt.wire.stanza.smax.psa;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Models the outbound {@code <iq type="set"><blocking action="..."/></iq>}
 * request that mutes or unmutes the PSA broadcast channel for the current
 * account.
 *
 * <p>The {@link #blockingAction()} string is the action to apply; typed
 * callers pass one of the {@link SmaxPsaChatBlockGetBlockingStatus} wire
 * literals ({@code "blocked"} or {@code "unblocked"}). The wire layer accepts
 * any non-empty string and lets the relay reject unknown literals server-side.
 * The reply is a {@link SmaxPsaChatBlockSetResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPsaChatBlockSetRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutPsaBaseIQSetRequestMixin")
public final class SmaxPsaChatBlockSetRequest implements SmaxStanza.Request {
    /**
     * Holds the free-form action string carried by the {@code action}
     * attribute; typed callers pass {@code "blocked"} or {@code "unblocked"}
     * to flip the PSA-mute flag.
     */
    private final String blockingAction;

    /**
     * Constructs a request around the given action string.
     *
     * @param blockingAction the action string; never {@code null}
     * @throws NullPointerException if {@code blockingAction} is {@code null}
     */
    public SmaxPsaChatBlockSetRequest(String blockingAction) {
        this.blockingAction = Objects.requireNonNull(blockingAction, "blockingAction cannot be null");
    }

    /**
     * Returns the action string carried by the {@code action} attribute.
     *
     * @return the action string; never {@code null}
     */
    public String blockingAction() {
        return blockingAction;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds the {@code <iq xmlns="w:comms:chat" type="set">} envelope,
     * addressed to the {@linkplain JidServer#user() user server}, wrapping a
     * {@code <blocking action="..."/>} child whose {@code action} attribute
     * carries {@link #blockingAction()}.
     *
     * @return a {@link StanzaBuilder} carrying the
     *         {@code <iq><blocking action="..."/></iq>} stanza
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPsaChatBlockSetRequest",
            exports = "makeChatBlockSetRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var blockingNode = new StanzaBuilder()
                .description("blocking")
                .attribute("action", blockingAction)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:comms:chat")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(blockingNode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxPsaChatBlockSetRequest) obj;
        return Objects.equals(this.blockingAction, that.blockingAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockingAction);
    }

    @Override
    public String toString() {
        return "SmaxPsaChatBlockSetRequest[blockingAction=" + blockingAction + ']';
    }
}
