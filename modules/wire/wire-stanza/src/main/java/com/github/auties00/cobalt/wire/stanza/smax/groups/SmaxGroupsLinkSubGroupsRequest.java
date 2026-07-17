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
 * The outbound {@code <iq xmlns="w:g2" type="set">} stanza that links one or more existing groups as sub-groups of a
 * community.
 *
 * <p>Backs the community admin "Manage groups" page: pass the community parent JID as {@link #parentGroupJid()} and
 * the list of candidate sub-groups with their per-group hidden marker. The relay caps the batch at 1000 entries
 * server-side.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsLinkSubGroupsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsLinkSubGroupsRequest implements SmaxStanza.Request {
    /**
     * The parent (community) group {@link Jid} surfaced on the IQ's {@code to} attribute.
     */
    private final Jid parentGroupJid;

    /**
     * The list of candidate sub-groups, each with an optional hidden-group marker.
     */
    private final List<RequestedGroup> groups;

    /**
     * Constructs a request.
     *
     * <p>The relay rejects empty batches as a client error; build the list with at least one entry.
     *
     * @param parentGroupJid the parent community {@link Jid}; never {@code null}
     * @param groups         the list of groups to link as sub-groups; never {@code null} and must be non-empty
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IllegalArgumentException when {@code groups} is empty
     */
    public SmaxGroupsLinkSubGroupsRequest(Jid parentGroupJid, List<RequestedGroup> groups) {
        Objects.requireNonNull(parentGroupJid, "parentGroupJid cannot be null");
        Objects.requireNonNull(groups, "groups cannot be null");
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("groups must contain at least one entry");
        }
        this.parentGroupJid = parentGroupJid;
        this.groups = List.copyOf(groups);
    }

    /**
     * Returns the parent group {@link Jid}.
     *
     * @return the parent community {@link Jid}; never {@code null}
     */
    public Jid parentGroupJid() {
        return parentGroupJid;
    }

    /**
     * Returns the list of candidate sub-groups.
     *
     * @return an unmodifiable list of {@link RequestedGroup}; never {@code null} or empty
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
     *         <links>
     *             <link link_type="sub_group">
     *                 <group jid="<jid>"><hidden_group/></group>
     *                 ...
     *             </link>
     *         </links>
     *     </iq>
     * }
     * where the {@code <hidden_group/>} marker is emitted per-group only when the {@link RequestedGroup#hiddenGroup()}
     * flag is {@code true}.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <links><link/></links>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsLinkSubGroupsRequest",
            exports = "makeLinkSubGroupsRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var groupNodes = new ArrayList<Stanza>();
        for (var group : groups) {
            var groupBuilder = new StanzaBuilder()
                    .description("group")
                    .attribute("jid", group.jid());
            if (group.hiddenGroup()) {
                var hiddenNode = new StanzaBuilder()
                        .description("hidden_group")
                        .build();
                groupBuilder.content(hiddenNode);
            }
            groupNodes.add(groupBuilder.build());
        }
        var linkNode = new StanzaBuilder()
                .description("link")
                .attribute("link_type", "sub_group")
                .content(groupNodes)
                .build();
        var linksNode = new StanzaBuilder()
                .description("links")
                .content(linkNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", parentGroupJid)
                .attribute("type", "set")
                .content(linksNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsLinkSubGroupsRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsLinkSubGroupsRequest) obj;
        return Objects.equals(this.parentGroupJid, that.parentGroupJid)
                && Objects.equals(this.groups, that.groups);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(parentGroupJid, groups);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsLinkSubGroupsRequest[parentGroupJid=" + parentGroupJid
                + ", groups=" + groups + ']';
    }

    /**
     * Single sub-group entry inside the outbound {@code <links><link/></links>} payload.
     *
     * <p>The {@link #hiddenGroup()} flag, when {@code true}, attaches a {@code <hidden_group/>} marker inside the
     * matching {@code <group/>} child to indicate that the linked group is hidden from the community directory.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutGroupsLinkSubGroupsRequest")
    public static final class RequestedGroup {
        /**
         * The candidate sub-group {@link Jid}.
         */
        private final Jid jid;

        /**
         * Whether to attach a {@code <hidden_group/>} marker inside the {@code <group/>} child.
         */
        private final boolean hiddenGroup;

        /**
         * Constructs a {@link RequestedGroup} entry.
         *
         * @param jid         the sub-group {@link Jid}; never {@code null}
         * @param hiddenGroup whether the sub-group is hidden from the community directory
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public RequestedGroup(Jid jid, boolean hiddenGroup) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.hiddenGroup = hiddenGroup;
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
         * Returns whether the {@code <hidden_group/>} marker is attached.
         *
         * @return {@code true} when the marker is emitted
         */
        public boolean hiddenGroup() {
            return hiddenGroup;
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
            return this.hiddenGroup == that.hiddenGroup
                    && Objects.equals(this.jid, that.jid);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(jid, hiddenGroup);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsLinkSubGroupsRequest.RequestedGroup[jid=" + jid
                    + ", hiddenGroup=" + hiddenGroup + ']';
        }
    }
}
