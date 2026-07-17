package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * The outbound {@code <iq xmlns="w:g2" type="get" to="g.us">} stanza that lists every group the caller participates in.
 * <p>
 * The two flags toggle the per-group projection: {@link #includeParticipants()} asks the relay to inline each group's
 * participant list and {@link #includeDescription()} asks for the per-group description body. Replies are parsed
 * through {@link SmaxGroupsGetParticipatingGroupsResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetParticipatingGroupsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseGetServerMixin")
public final class SmaxGroupsGetParticipatingGroupsRequest implements SmaxStanza.Request {
    /**
     * Whether the relay should include a per-group participant list inside each {@code <group/>} child.
     */
    private final boolean includeParticipants;

    /**
     * Whether the relay should include the per-group description inside each {@code <group/>} child.
     */
    private final boolean includeDescription;

    /**
     * Constructs a request with explicit projection flags.
     * <p>
     * Passing both flags as {@code false} retrieves only the minimal {@code <group jid .../>} envelope per group.
     *
     * @param includeParticipants {@code true} to inline per-group participant lists
     * @param includeDescription  {@code true} to inline per-group description bodies
     */
    public SmaxGroupsGetParticipatingGroupsRequest(boolean includeParticipants, boolean includeDescription) {
        this.includeParticipants = includeParticipants;
        this.includeDescription = includeDescription;
    }

    /**
     * Returns whether participants are included.
     *
     * @return {@code true} when each group's participant list should be inlined
     */
    public boolean includeParticipants() {
        return includeParticipants;
    }

    /**
     * Returns whether descriptions are included.
     *
     * @return {@code true} when each group's description body should be inlined
     */
    public boolean includeDescription() {
        return includeDescription;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     * <p>
     * The {@code to} attribute is bound to {@link JidServer#groupOrCommunity()}. The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="g.us" type="get">
     *         <participating>
     *             <participants/>
     *             <description/>
     *         </participating>
     *     </iq>
     * }
     * where the {@code <participants/>} and {@code <description/>} markers are present only when the matching flag is
     * {@code true}.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <participating/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsGetParticipatingGroupsRequest",
            exports = "makeGetParticipatingGroupsRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var participatingBuilder = new StanzaBuilder().description("participating");
        if (includeParticipants) {
            participatingBuilder.content(new StanzaBuilder().description("participants").build());
        }
        if (includeDescription) {
            participatingBuilder.content(new StanzaBuilder().description("description").build());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", JidServer.groupOrCommunity())
                .attribute("type", "get")
                .content(participatingBuilder.build());
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsGetParticipatingGroupsRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsGetParticipatingGroupsRequest) obj;
        return this.includeParticipants == that.includeParticipants
                && this.includeDescription == that.includeDescription;
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(includeParticipants, includeDescription);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsGetParticipatingGroupsRequest[includeParticipants=" + includeParticipants
                + ", includeDescription=" + includeDescription + ']';
    }
}
