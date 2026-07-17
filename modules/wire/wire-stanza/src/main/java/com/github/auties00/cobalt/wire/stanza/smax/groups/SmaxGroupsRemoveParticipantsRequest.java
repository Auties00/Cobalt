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
 * The outbound {@code <iq type="set" xmlns="w:g2">} stanza that removes participants from an existing group.
 *
 * <p>Backs the "Remove from group" affordance on the participant context menu. The relay accepts up to 1024
 * entries per request and returns a per-participant outcome list in the matching
 * {@link SmaxGroupsRemoveParticipantsResponse.Success#participants()}. When the optional
 * {@link #removeLinkedGroups()} flag is set, the relay additionally drops the listed participants from every
 * sub-group of a community parent group; the flag has no effect on a non-community group.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsRemoveParticipantsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsRemoveParticipantsRequest implements SmaxStanza.Request {
    /**
     * The group {@link Jid} from which participants are being removed.
     */
    private final Jid groupJid;

    /**
     * The candidate participant {@link Jid}s to remove.
     */
    private final List<Jid> participants;

    /**
     * Whether the relay should cascade the removal across every linked sub-group of a community parent group.
     */
    private final boolean removeLinkedGroups;

    /**
     * Constructs a remove-participants request.
     *
     * <p>The relay enforces a 1..1024 cardinality on the {@code <participant>} children; callers should
     * pre-batch larger contact lists. The participant list is defensively copied so post-construction mutation
     * of the caller's list has no effect on the request.
     *
     * @param groupJid           the group {@link Jid}
     * @param participants       the candidate participant {@link Jid}s to remove
     * @param removeLinkedGroups whether to cascade the removal across every linked sub-group (community parent
     *                           groups only)
     * @throws NullPointerException     if {@code groupJid} or {@code participants} is {@code null}
     * @throws IllegalArgumentException if {@code participants} is empty
     */
    public SmaxGroupsRemoveParticipantsRequest(Jid groupJid, List<Jid> participants, boolean removeLinkedGroups) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(participants, "participants cannot be null");
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("participants cannot be empty");
        }
        this.participants = List.copyOf(participants);
        this.removeLinkedGroups = removeLinkedGroups;
    }

    /**
     * Returns the target group {@link Jid}.
     *
     * <p>The value routes verbatim into the IQ's {@code to} attribute.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the participant {@link Jid}s to remove.
     *
     * @return an unmodifiable list of {@link Jid}s; never {@code null} and never empty
     */
    public List<Jid> participants() {
        return participants;
    }

    /**
     * Returns whether the linked-groups cascade is enabled.
     *
     * <p>Only honoured by the relay when {@link #groupJid()} is a community parent group; ignored on regular
     * groups.
     *
     * @return {@code true} when the {@code linked_groups="true"} attribute is emitted on the {@code <remove>}
     *         child
     */
    public boolean removeLinkedGroups() {
        return removeLinkedGroups;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * <p>The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="set">
     *         <remove linked_groups="true">
     *             <participant jid="<jid0>"/>
     *             <participant jid="<jid1>"/>
     *             ...
     *         </remove>
     *     </iq>
     * }
     * the {@code linked_groups} attribute is omitted when {@link #removeLinkedGroups()} is {@code false}.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <remove>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsRemoveParticipantsRequest",
            exports = "makeRemoveParticipantsRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var participantNodes = new ArrayList<Stanza>(participants.size());
        for (var participantJid : participants) {
            var participantNode = new StanzaBuilder()
                    .description("participant")
                    .attribute("jid", participantJid)
                    .build();
            participantNodes.add(participantNode);
        }
        var removeBuilder = new StanzaBuilder()
                .description("remove")
                .content(participantNodes);
        if (removeLinkedGroups) {
            removeBuilder.attribute("linked_groups", "true");
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(removeBuilder.build());
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsRemoveParticipantsRequest} with identical
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
        var that = (SmaxGroupsRemoveParticipantsRequest) obj;
        return this.removeLinkedGroups == that.removeLinkedGroups
                && Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.participants, that.participants);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, participants, removeLinkedGroups);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsRemoveParticipantsRequest[groupJid=" + groupJid
                + ", participants=" + participants
                + ", removeLinkedGroups=" + removeLinkedGroups + ']';
    }
}
