package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Deactivates a community (parent group) along with its sub-groups via an
 * {@code <iq type="set" xmlns="w:g2">} stanza.
 *
 * <p>The payload is a bare {@code <delete_parent/>} child with no attributes; the target community is
 * identified solely by the IQ envelope's {@code to} attribute. Callers emit one request per community to be
 * torn down and pair it with {@link SmaxGroupsDeleteParentGroupResponse} to read the relay's verdict.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsDeleteParentGroupRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsDeleteParentGroupRequest implements SmaxStanza.Request {
    /**
     * Holds the parent (community) group {@link Jid} routed verbatim into the IQ envelope's {@code to}
     * attribute.
     */
    private final Jid parentGroupJid;

    /**
     * Constructs a request targeting the given community.
     *
     * <p>The relay does not accept multiple parents in a single IQ, so callers build one request per community
     * deletion.
     *
     * @param parentGroupJid the community {@link Jid}; never {@code null}
     * @throws NullPointerException if {@code parentGroupJid} is {@code null}
     */
    public SmaxGroupsDeleteParentGroupRequest(Jid parentGroupJid) {
        this.parentGroupJid = Objects.requireNonNull(parentGroupJid, "parentGroupJid cannot be null");
    }

    /**
     * Returns the parent community {@link Jid} targeted by this request.
     *
     * <p>The returned value is the one rendered into the IQ envelope's {@code to} attribute.
     *
     * @return the parent group {@link Jid}; never {@code null}
     */
    public Jid parentGroupJid() {
        return parentGroupJid;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits a single empty {@code <delete_parent/>} child inside the
     * {@code <iq xmlns="w:g2" type="set">} envelope addressed to {@link #parentGroupJid()}; no further
     * attributes or children are sent.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsDeleteParentGroupRequest",
            exports = "makeDeleteParentGroupRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var deleteParentNode = new StanzaBuilder()
                .description("delete_parent")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", parentGroupJid)
                .attribute("type", "set")
                .content(deleteParentNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsDeleteParentGroupRequest} with the same
     *         parent group {@link Jid}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsDeleteParentGroupRequest) obj;
        return Objects.equals(this.parentGroupJid, that.parentGroupJid);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(parentGroupJid);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsDeleteParentGroupRequest[parentGroupJid=" + parentGroupJid + ']';
    }
}
