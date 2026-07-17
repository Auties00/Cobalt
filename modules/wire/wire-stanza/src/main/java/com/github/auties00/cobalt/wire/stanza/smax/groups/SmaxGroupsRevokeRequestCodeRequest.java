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
 * Models the outbound {@code <iq type="set" xmlns="w:g2">} stanza that revokes the pending join codes of one or
 * more candidates against a membership-approval group.
 *
 * <p>This request cancels pending join codes that an admin issued to candidates for an approval-mode group. The
 * relay accepts up to 1000 entries per request and returns a per-participant outcome list in the matching
 * {@link SmaxGroupsRevokeRequestCodeResponse.Success#participants()}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsRevokeRequestCodeRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsRevokeRequestCodeRequest implements SmaxStanza.Request {
    /**
     * The parent group {@link Jid} that anchors the pending join codes.
     */
    private final Jid groupJid;

    /**
     * The candidate participant {@link Jid}s whose pending join codes are being revoked.
     */
    private final List<Jid> participants;

    /**
     * Constructs a revoke-request-code request.
     *
     * <p>The relay enforces a 1..1000 cardinality on the {@code <participant>} children, so callers should
     * pre-batch larger candidate lists. The participant list is copied so post-construction mutation of the
     * caller's list has no effect on the request.
     *
     * @param groupJid     the parent group {@link Jid}
     * @param participants the candidate participant {@link Jid}s whose request codes are to be revoked
     * @throws NullPointerException     if {@code groupJid} or {@code participants} is {@code null}
     * @throws IllegalArgumentException if {@code participants} is empty
     */
    public SmaxGroupsRevokeRequestCodeRequest(Jid groupJid, List<Jid> participants) {
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
     * <p>The value routes verbatim into the IQ's {@code to} attribute.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the participant {@link Jid}s whose pending join codes are being revoked.
     *
     * @return an unmodifiable list of {@link Jid}s; never {@code null} and never empty
     */
    public List<Jid> participants() {
        return participants;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * <p>The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="set">
     *         <revoke>
     *             <participant jid="<jid0>"/>
     *             <participant jid="<jid1>"/>
     *             ...
     *         </revoke>
     *     </iq>
     * }
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <revoke>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsRevokeRequestCodeRequest",
            exports = "makeRevokeRequestCodeRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var participantNodes = new ArrayList<Stanza>(participants.size());
        for (var participantJid : participants) {
            var participantNode = new StanzaBuilder()
                    .description("participant")
                    .attribute("jid", participantJid)
                    .build();
            participantNodes.add(participantNode);
        }
        var revokeNode = new StanzaBuilder()
                .description("revoke")
                .content(participantNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(revokeNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across both fields.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsRevokeRequestCodeRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsRevokeRequestCodeRequest) obj;
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
        return "SmaxGroupsRevokeRequestCodeRequest[groupJid=" + groupJid
                + ", participants=" + participants + ']';
    }
}
