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
 * Outbound {@code <iq type="set" xmlns="w:g2">} stanza that adds participants to an existing group.
 *
 * This request backs the add-participants affordance on the group info screen. The relay accepts up to 1024
 * entries per request and returns a per-participant outcome list in the matching
 * {@link SmaxGroupsAddParticipantsResponse.Success#participants()}; callers should pre-batch larger contact lists.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsAddParticipantsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsAddParticipantsRequest implements SmaxStanza.Request {
    /**
     * Holds the group {@link Jid} receiving the new participants.
     */
    private final Jid groupJid;

    /**
     * Holds the candidate participant {@link Jid}s.
     */
    private final List<Jid> participants;

    /**
     * Constructs an add-participants request.
     *
     * The relay enforces a 1..1024 cardinality on the {@code <participant>} children; callers should pre-batch
     * larger contact lists. The supplied list is defensively copied.
     *
     * @param groupJid     the group {@link Jid}
     * @param participants the candidate {@link Jid}s
     * @throws NullPointerException     if {@code groupJid} or {@code participants} is {@code null}
     * @throws IllegalArgumentException if {@code participants} is empty
     */
    public SmaxGroupsAddParticipantsRequest(Jid groupJid, List<Jid> participants) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(participants, "participants cannot be null");
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("participants cannot be empty");
        }
        this.participants = List.copyOf(participants);
    }

    /**
     * Returns the target group {@link Jid}.
     *
     * The value routes verbatim into the IQ's {@code to} attribute.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the candidate participant {@link Jid}s.
     *
     * @return an unmodifiable list of {@link Jid}s; never {@code null} and never empty
     */
    public List<Jid> participants() {
        return participants;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="set">
     *         <add>
     *             <participant jid="<jid0>"/>
     *             <participant jid="<jid1>"/>
     *             ...
     *         </add>
     *     </iq>
     * }
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <add>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsAddParticipantsRequest",
            exports = "makeAddParticipantsRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var participantNodes = new ArrayList<Stanza>(participants.size());
        for (var participantJid : participants) {
            var participantNode = new StanzaBuilder()
                    .description("participant")
                    .attribute("jid", participantJid)
                    .build();
            participantNodes.add(participantNode);
        }
        var addNode = new StanzaBuilder()
                .description("add")
                .content(participantNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(addNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across both fields.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsAddParticipantsRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsAddParticipantsRequest) obj;
        return Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.participants, that.participants);
    }

    /**
     * Returns a hash composed of both fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, participants);
    }

    /**
     * Returns a debug string carrying both fields.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsAddParticipantsRequest[groupJid=" + groupJid
                + ", participants=" + participants + ']';
    }
}
