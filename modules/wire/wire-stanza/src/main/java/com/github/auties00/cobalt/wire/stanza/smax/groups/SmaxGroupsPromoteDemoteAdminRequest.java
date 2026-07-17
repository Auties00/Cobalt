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
 * The outbound {@code <iq type="set" xmlns="w:g2">} stanza that adjusts the community-admin roster of a parent
 * group.
 *
 * <p>Backs the "Add community admin" and "Remove community admin" affordances on the community-management screen.
 * The relay accepts the promote and demote lists inside a single {@code <admin>} envelope (each capped at 1024
 * entries) and returns a per-participant outcome list in the matching
 * {@link SmaxGroupsPromoteDemoteAdminResponse.SuccessMultiAdmin#participants()}. At least one of the two lists
 * must be non-empty; an entirely empty payload is rejected client-side.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsPromoteDemoteAdminRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsPromoteDemoteAdminRequest implements SmaxStanza.Request {
    /**
     * The community parent-group {@link Jid} that anchors the admin roster.
     */
    private final Jid groupJid;

    /**
     * The candidate {@link Jid}s to promote to community admin.
     */
    private final List<Jid> participantsToPromote;

    /**
     * The candidate {@link Jid}s to demote from community admin.
     */
    private final List<Jid> participantsToDemote;

    /**
     * Constructs a promote-demote-admin request.
     *
     * <p>The relay caps each of the {@code <promote>} and {@code <demote>} child sets at 1024 entries; a caller
     * batching a wider roster change should split the work across multiple requests. The two lists are
     * defensively copied so post-construction mutation of the caller's lists has no effect on the request.
     *
     * @param groupJid              the community parent-group {@link Jid}
     * @param participantsToPromote the {@link Jid}s to promote
     * @param participantsToDemote  the {@link Jid}s to demote
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if both lists are empty
     */
    public SmaxGroupsPromoteDemoteAdminRequest(Jid groupJid, List<Jid> participantsToPromote,
                   List<Jid> participantsToDemote) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(participantsToPromote, "participantsToPromote cannot be null");
        Objects.requireNonNull(participantsToDemote, "participantsToDemote cannot be null");
        if (participantsToPromote.isEmpty() && participantsToDemote.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one of participantsToPromote / participantsToDemote must be non-empty");
        }
        this.participantsToPromote = List.copyOf(participantsToPromote);
        this.participantsToDemote = List.copyOf(participantsToDemote);
    }

    /**
     * Returns the community parent-group {@link Jid}.
     *
     * <p>The value routes verbatim into the IQ's {@code to} attribute.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the {@link Jid}s to promote to community admin.
     *
     * @return an unmodifiable list of {@link Jid}s; never {@code null}
     */
    public List<Jid> participantsToPromote() {
        return participantsToPromote;
    }

    /**
     * Returns the {@link Jid}s to demote from community admin.
     *
     * @return an unmodifiable list of {@link Jid}s; never {@code null}
     */
    public List<Jid> participantsToDemote() {
        return participantsToDemote;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * <p>The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="set">
     *         <admin>
     *             <promote>
     *                 <participant jid="<promote0>"/>
     *                 ...
     *             </promote>
     *             <demote>
     *                 <participant jid="<demote0>"/>
     *                 ...
     *             </demote>
     *         </admin>
     *     </iq>
     * }
     * either {@code <promote>} or {@code <demote>} is omitted when the corresponding caller list is empty.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <admin>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsPromoteDemoteAdminRequest",
            exports = "makePromoteDemoteAdminRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var adminBuilder = new StanzaBuilder().description("admin");
        if (!participantsToPromote.isEmpty()) {
            var promoteChildren = new ArrayList<Stanza>(participantsToPromote.size());
            for (var participantJid : participantsToPromote) {
                var participantNode = new StanzaBuilder()
                        .description("participant")
                        .attribute("jid", participantJid)
                        .build();
                promoteChildren.add(participantNode);
            }
            var promoteNode = new StanzaBuilder()
                    .description("promote")
                    .content(promoteChildren)
                    .build();
            adminBuilder.content(promoteNode);
        }
        if (!participantsToDemote.isEmpty()) {
            var demoteChildren = new ArrayList<Stanza>(participantsToDemote.size());
            for (var participantJid : participantsToDemote) {
                var participantNode = new StanzaBuilder()
                        .description("participant")
                        .attribute("jid", participantJid)
                        .build();
                demoteChildren.add(participantNode);
            }
            var demoteNode = new StanzaBuilder()
                    .description("demote")
                    .content(demoteChildren)
                    .build();
            adminBuilder.content(demoteNode);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(adminBuilder.build());
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsPromoteDemoteAdminRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsPromoteDemoteAdminRequest) obj;
        return Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.participantsToPromote, that.participantsToPromote)
                && Objects.equals(this.participantsToDemote, that.participantsToDemote);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, participantsToPromote, participantsToDemote);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsPromoteDemoteAdminRequest[groupJid=" + groupJid
                + ", participantsToPromote=" + participantsToPromote
                + ", participantsToDemote=" + participantsToDemote + ']';
    }
}
