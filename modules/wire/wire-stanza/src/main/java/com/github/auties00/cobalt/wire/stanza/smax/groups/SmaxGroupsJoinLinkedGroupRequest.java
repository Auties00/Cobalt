package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * The outbound {@code <iq xmlns="w:g2" type="set">} stanza that joins a sub-group inside a community.
 * <p>
 * {@link #parentGroupJid()} is the parent community JID, {@link #joinLinkedGroupJid()} is the target sub-group JID, and
 * the optional join-type discriminator distinguishes the sub-group kind: {@code "default_sub_group"} for linked
 * announcement groups and {@code "sub_group"} for ordinary sub-groups.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsJoinLinkedGroupRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsJoinLinkedGroupRequest implements SmaxStanza.Request {
    /**
     * The parent (community) group {@link Jid} surfaced on the IQ's {@code to} attribute.
     */
    private final Jid parentGroupJid;

    /**
     * The sub-group {@link Jid} the caller wishes to join.
     */
    private final Jid joinLinkedGroupJid;

    /**
     * The optional join-type discriminator, typically {@code "sub_group"} or {@code "default_sub_group"};
     * {@code null} omits the {@code type} attribute.
     */
    private final String joinLinkedGroupType;

    /**
     * Constructs a request.
     * <p>
     * Pass {@code "default_sub_group"} for linked announcement groups and {@code "sub_group"} otherwise; passing
     * {@code null} produces a request without a {@code type} attribute.
     *
     * @param parentGroupJid       the parent community {@link Jid}; never {@code null}
     * @param joinLinkedGroupJid   the sub-group {@link Jid} to join; never {@code null}
     * @param joinLinkedGroupType  the optional join-type discriminator; may be {@code null}
     * @throws NullPointerException if {@code parentGroupJid} or {@code joinLinkedGroupJid} is {@code null}
     */
    public SmaxGroupsJoinLinkedGroupRequest(Jid parentGroupJid, Jid joinLinkedGroupJid, String joinLinkedGroupType) {
        this.parentGroupJid = Objects.requireNonNull(parentGroupJid, "parentGroupJid cannot be null");
        this.joinLinkedGroupJid = Objects.requireNonNull(joinLinkedGroupJid, "joinLinkedGroupJid cannot be null");
        this.joinLinkedGroupType = joinLinkedGroupType;
    }

    /**
     * Returns the parent community group {@link Jid}.
     *
     * @return the parent group {@link Jid}; never {@code null}
     */
    public Jid parentGroupJid() {
        return parentGroupJid;
    }

    /**
     * Returns the sub-group {@link Jid} being joined.
     *
     * @return the linked group {@link Jid}; never {@code null}
     */
    public Jid joinLinkedGroupJid() {
        return joinLinkedGroupJid;
    }

    /**
     * Returns the optional join-type discriminator.
     *
     * @return an {@link Optional} carrying the join-type token, or empty when the request omits the {@code type}
     *         attribute
     */
    public Optional<String> joinLinkedGroupType() {
        return Optional.ofNullable(joinLinkedGroupType);
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     * <p>
     * The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<parentGroupJid>" type="set">
     *         <join_linked_group jid="<joinLinkedGroupJid>" type="<joinLinkedGroupType>"/>
     *     </iq>
     * }
     * where the {@code type} attribute is omitted when {@link #joinLinkedGroupType()} is empty.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <join_linked_group/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsJoinLinkedGroupRequest",
            exports = "makeJoinLinkedGroupRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var joinBuilder = new StanzaBuilder()
                .description("join_linked_group")
                .attribute("jid", joinLinkedGroupJid);
        if (joinLinkedGroupType != null) {
            joinBuilder.attribute("type", joinLinkedGroupType);
        }
        var joinNode = joinBuilder.build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", parentGroupJid)
                .attribute("type", "set")
                .content(joinNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsJoinLinkedGroupRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsJoinLinkedGroupRequest) obj;
        return Objects.equals(this.parentGroupJid, that.parentGroupJid)
                && Objects.equals(this.joinLinkedGroupJid, that.joinLinkedGroupJid)
                && Objects.equals(this.joinLinkedGroupType, that.joinLinkedGroupType);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(parentGroupJid, joinLinkedGroupJid, joinLinkedGroupType);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsJoinLinkedGroupRequest[parentGroupJid=" + parentGroupJid
                + ", joinLinkedGroupJid=" + joinLinkedGroupJid
                + ", joinLinkedGroupType=" + joinLinkedGroupType + ']';
    }
}
