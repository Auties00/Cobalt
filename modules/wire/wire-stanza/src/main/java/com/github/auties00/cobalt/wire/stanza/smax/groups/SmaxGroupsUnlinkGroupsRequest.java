package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models the outbound {@code <iq type="set" xmlns="w:g2">} stanza that unlinks one or more sub-groups from a
 * community parent group.
 *
 * <p>This request backs removing sub-groups from a community. The relay accepts up to 1000 entries per request
 * and returns a per-sub-group result row in the matching
 * {@link SmaxGroupsUnlinkGroupsResponse.Success#unlinkedGroups()}. When the optional
 * {@link RequestedGroup#removeOrphanedMembers()} flag is set on an entry, the relay also evicts community members
 * who, after the unlink, are no longer affiliated with any remaining sub-group.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsUnlinkGroupsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsUnlinkGroupsRequest implements SmaxStanza.Request {
    /**
     * The parent (community) group {@link Jid} that anchors the sub-group set.
     */
    private final Jid parentGroupJid;

    /**
     * The list of sub-groups to unlink.
     */
    private final List<RequestedGroup> groups;

    /**
     * Constructs an unlink-groups request.
     *
     * <p>The relay enforces a 1..1000 cardinality on the {@code <group>} children, so callers should pre-batch
     * larger lists. The {@code groups} list is copied so post-construction mutation of the caller's list has no
     * effect on the request.
     *
     * @param parentGroupJid the parent community {@link Jid}
     * @param groups         the sub-groups to unlink
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IllegalArgumentException when {@code groups} is empty
     */
    public SmaxGroupsUnlinkGroupsRequest(Jid parentGroupJid, List<RequestedGroup> groups) {
        Objects.requireNonNull(parentGroupJid, "parentGroupJid cannot be null");
        Objects.requireNonNull(groups, "groups cannot be null");
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("groups must contain at least one entry");
        }
        this.parentGroupJid = parentGroupJid;
        this.groups = List.copyOf(groups);
    }

    /**
     * Returns the parent (community) group {@link Jid}.
     *
     * <p>The value routes verbatim into the IQ's {@code to} attribute.
     *
     * @return the parent group {@link Jid}; never {@code null}
     */
    public Jid parentGroupJid() {
        return parentGroupJid;
    }

    /**
     * Returns the list of sub-groups to unlink.
     *
     * @return an unmodifiable list of {@link RequestedGroup} entries; never {@code null} or empty
     */
    public List<RequestedGroup> groups() {
        return groups;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * <p>The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<parentGroupJid>" type="set">
     *         <unlink unlink_type="sub_group">
     *             <group jid="<subGroup0>" remove_orphaned_members="true"/>
     *             <group jid="<subGroup1>"/>
     *             ...
     *         </unlink>
     *     </iq>
     * }
     * The {@code remove_orphaned_members} attribute is emitted only on entries where the flag is set.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <unlink>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsUnlinkGroupsRequest",
            exports = "makeUnlinkGroupsRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var groupNodes = new ArrayList<Stanza>();
        for (var group : groups) {
            var groupBuilder = new StanzaBuilder()
                    .description("group")
                    .attribute("jid", group.jid());
            if (group.removeOrphanedMembers()) {
                groupBuilder.attribute("remove_orphaned_members", "true");
            }
            groupNodes.add(groupBuilder.build());
        }
        var unlinkNode = new StanzaBuilder()
                .description("unlink")
                .attribute("unlink_type", "sub_group")
                .content(groupNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", parentGroupJid)
                .attribute("type", "set")
                .content(unlinkNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across both fields.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsUnlinkGroupsRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsUnlinkGroupsRequest) obj;
        return Objects.equals(this.parentGroupJid, that.parentGroupJid)
                && Objects.equals(this.groups, that.groups);
    }

    /**
     * Returns a hash composed of both fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(parentGroupJid, groups);
    }

    /**
     * Returns a debug string carrying both fields.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsUnlinkGroupsRequest[parentGroupJid=" + parentGroupJid
                + ", groups=" + groups + ']';
    }

    /**
     * Represents a single sub-group entry inside the outbound {@code <unlink/>} payload.
     *
     * <p>Pairs a sub-group {@link Jid} with the optional {@link #removeOrphanedMembers() remove_orphaned_members}
     * flag controlling whether the relay should also evict community members no longer affiliated with any
     * remaining sub-group.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutGroupsUnlinkGroupsRequest")
    public static final class RequestedGroup {
        /**
         * The sub-group {@link Jid} to unlink.
         */
        private final Jid jid;

        /**
         * Whether to ask the relay to evict community members no longer affiliated with any sub-group.
         */
        private final boolean removeOrphanedMembers;

        /**
         * Constructs a {@link RequestedGroup} entry.
         *
         * @param jid                   the sub-group {@link Jid}
         * @param removeOrphanedMembers whether to evict community members orphaned by this unlink
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public RequestedGroup(Jid jid, boolean removeOrphanedMembers) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.removeOrphanedMembers = removeOrphanedMembers;
        }

        /**
         * Returns the sub-group {@link Jid}.
         *
         * @return the sub-group {@link Jid}; never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Returns whether the eviction flag is set.
         *
         * @return {@code true} when the {@code remove_orphaned_members="true"} attribute is emitted
         */
        public boolean removeOrphanedMembers() {
            return removeOrphanedMembers;
        }

        /**
         * Compares this entry to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link RequestedGroup} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (RequestedGroup) obj;
            return this.removeOrphanedMembers == that.removeOrphanedMembers
                    && Objects.equals(this.jid, that.jid);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(jid, removeOrphanedMembers);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsUnlinkGroupsRequest.RequestedGroup[jid=" + jid
                    + ", removeOrphanedMembers=" + removeOrphanedMembers + ']';
        }
    }
}
