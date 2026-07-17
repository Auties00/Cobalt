package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * The outbound {@code <iq xmlns="w:g2" type="get">} stanza that asks the relay for the union of participants across a
 * community's sub-groups.
 * <p>
 * The target {@link Jid} must be a community parent group; the relay rejects requests addressed to non-parent groups.
 * Replies are parsed through {@link SmaxGroupsGetLinkedGroupsParticipantsResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetLinkedGroupsParticipantsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseGetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQGetRequestMixin")
public final class SmaxGroupsGetLinkedGroupsParticipantsRequest implements SmaxStanza.Request {
    /**
     * The community parent group {@link Jid} surfaced on the IQ's {@code to} attribute.
     */
    private final Jid groupJid;

    /**
     * Constructs a request for the given community parent group.
     *
     * @param groupJid the community parent group {@link Jid}; never {@code null}
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxGroupsGetLinkedGroupsParticipantsRequest(Jid groupJid) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
    }

    /**
     * Returns the community parent group {@link Jid}.
     *
     * @return the parent group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     * <p>
     * The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="get">
     *         <linked_groups_participants/>
     *     </iq>
     * }
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <linked_groups_participants/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsGetLinkedGroupsParticipantsRequest",
            exports = "makeGetLinkedGroupsParticipantsRequest",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var payload = new StanzaBuilder()
                .description("linked_groups_participants")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "get")
                .content(payload);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsGetLinkedGroupsParticipantsRequest} with identical
     *         fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsGetLinkedGroupsParticipantsRequest) obj;
        return Objects.equals(this.groupJid, that.groupJid);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsGetLinkedGroupsParticipantsRequest[groupJid=" + groupJid + ']';
    }
}
