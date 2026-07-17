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
 * The outbound {@code <iq xmlns="w:g2" type="set">} stanza that approves or rejects pending membership-approval
 * requests on a group.
 *
 * <p>Backs the admin "Pending requests" UI. WA Web issues this IQ with either the approve list or the reject list
 * populated (never both), but the wire schema permits both lists to be populated in a single request. At least one
 * of {@link #participantsToApprove()} / {@link #participantsToReject()} must be non-empty.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsMembershipRequestsActionRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsMembershipRequestsActionRequest implements SmaxStanza.Request {
    /**
     * The group {@link Jid} hosting the pending requests; surfaced on the IQ's {@code to} attribute.
     */
    private final Jid groupJid;

    /**
     * The participant {@link Jid}s whose pending requests should be approved.
     */
    private final List<Jid> participantsToApprove;

    /**
     * The participant {@link Jid}s whose pending requests should be rejected.
     */
    private final List<Jid> participantsToReject;

    /**
     * Constructs a request.
     *
     * <p>Passing both lists empty triggers an {@link IllegalArgumentException}; the relay rejects no-op requests
     * as a client error.
     *
     * @param groupJid              the group {@link Jid}; never {@code null}
     * @param participantsToApprove the JIDs to approve; never {@code null}, may be empty
     * @param participantsToReject  the JIDs to reject; never {@code null}, may be empty
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if both lists are empty
     */
    public SmaxGroupsMembershipRequestsActionRequest(Jid groupJid, List<Jid> participantsToApprove,
                   List<Jid> participantsToReject) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(participantsToApprove, "participantsToApprove cannot be null");
        Objects.requireNonNull(participantsToReject, "participantsToReject cannot be null");
        if (participantsToApprove.isEmpty() && participantsToReject.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one of participantsToApprove / participantsToReject must be non-empty");
        }
        this.participantsToApprove = List.copyOf(participantsToApprove);
        this.participantsToReject = List.copyOf(participantsToReject);
    }

    /**
     * Returns the target group {@link Jid}.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the JIDs to approve.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<Jid> participantsToApprove() {
        return participantsToApprove;
    }

    /**
     * Returns the JIDs to reject.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<Jid> participantsToReject() {
        return participantsToReject;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * <p>The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="set">
     *         <membership_requests_action>
     *             <approve><participant jid="<jid>"/>...</approve>
     *             <reject><participant jid="<jid>"/>...</reject>
     *         </membership_requests_action>
     *     </iq>
     * }
     * where each {@code <approve>}/{@code <reject>} container is emitted only when the matching list is non-empty.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <membership_requests_action/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsMembershipRequestsActionRequest",
            exports = "makeMembershipRequestsActionRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var actionBuilder = new StanzaBuilder().description("membership_requests_action");
        if (!participantsToApprove.isEmpty()) {
            var approveChildren = new ArrayList<Stanza>(participantsToApprove.size());
            for (var participantJid : participantsToApprove) {
                var participantNode = new StanzaBuilder()
                        .description("participant")
                        .attribute("jid", participantJid)
                        .build();
                approveChildren.add(participantNode);
            }
            var approveNode = new StanzaBuilder()
                    .description("approve")
                    .content(approveChildren)
                    .build();
            actionBuilder.content(approveNode);
        }
        if (!participantsToReject.isEmpty()) {
            var rejectChildren = new ArrayList<Stanza>(participantsToReject.size());
            for (var participantJid : participantsToReject) {
                var participantNode = new StanzaBuilder()
                        .description("participant")
                        .attribute("jid", participantJid)
                        .build();
                rejectChildren.add(participantNode);
            }
            var rejectNode = new StanzaBuilder()
                    .description("reject")
                    .content(rejectChildren)
                    .build();
            actionBuilder.content(rejectNode);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(actionBuilder.build());
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsMembershipRequestsActionRequest} with identical
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
        var that = (SmaxGroupsMembershipRequestsActionRequest) obj;
        return Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.participantsToApprove, that.participantsToApprove)
                && Objects.equals(this.participantsToReject, that.participantsToReject);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, participantsToApprove, participantsToReject);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsMembershipRequestsActionRequest[groupJid=" + groupJid
                + ", participantsToApprove=" + participantsToApprove
                + ", participantsToReject=" + participantsToReject + ']';
    }
}
