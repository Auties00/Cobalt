package com.github.auties00.cobalt.wire.stanza.iq.group;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Models the outbound {@code <iq xmlns="w:g2" type="set" to="g.us"><invite code="..."/></iq>} request that redeems a group invite code.
 *
 * <p>This request backs the "join via invite link" and "join from invite message" flows. The
 * caller declares up front, via {@link #expectsMembershipApproval()}, whether the group is
 * expected to be approval-gated; the relay's reply differs in shape (a {@code <group>} child when
 * joined immediately, a {@code <membership_approval_request>} child when queued) and the response
 * parser uses this expectation to detect approval-mode mismatches and surface them as
 * {@link IqJoinGroupByInviteCodeResponse.UnexpectedJoinShape}.
 *
 * @implNote
 * This implementation carries {@link #expectsMembershipApproval()} purely so the response parser
 * can correlate request and reply expectations; the flag has no on-wire representation because the
 * relay always returns whichever shape applies.
 */
@WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
public final class IqJoinGroupByInviteCodeRequest implements IqStanza.Request {
    /**
     * Holds the invite code being redeemed.
     */
    private final String code;

    /**
     * Holds {@code true} when the caller expects approval-gated entry, {@code false} when it
     * expects immediate admission.
     */
    private final boolean expectsMembershipApproval;

    /**
     * Constructs a request that redeems the given invite code.
     *
     * <p>Callers discover whether a group is approval-gated by querying the invite-link metadata
     * first and pass the result here as {@code expectsMembershipApproval} so the response parser
     * can detect mismatches between the caller's expectation and the relay's actual gating mode.
     *
     * @param code                      the invite code; never {@code null}
     * @param expectsMembershipApproval {@code true} when the caller expects approval-gated entry
     * @throws NullPointerException if {@code code} is {@code null}
     */
    public IqJoinGroupByInviteCodeRequest(String code, boolean expectsMembershipApproval) {
        this.code = Objects.requireNonNull(code, "code cannot be null");
        this.expectsMembershipApproval = expectsMembershipApproval;
    }

    /**
     * Returns the invite code being redeemed.
     *
     * @return the code; never {@code null}
     */
    public String code() {
        return code;
    }

    /**
     * Returns whether the caller expects approval-gated entry.
     *
     * @return {@code true} when the caller expects a {@code <membership_approval_request>} reply, {@code false} when the caller expects an immediate {@code <group>} reply
     */
    public boolean expectsMembershipApproval() {
        return expectsMembershipApproval;
    }

    /**
     * Builds the outbound IQ stanza as a {@link StanzaBuilder} ready for dispatch.
     *
     * <p>Only the invite code crosses the wire; the {@link #expectsMembershipApproval()} flag is
     * client-side parser plumbing and is not serialised.
     *
     * @return the IQ envelope builder
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
            exports = "joinGroupViaInvite",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var inviteNode = new StanzaBuilder()
                .description("invite")
                .attribute("code", code)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", JidServer.groupOrCommunity())
                .attribute("type", "set")
                .content(inviteNode);
    }

    /**
     * Compares this request with another object for equality.
     *
     * <p>Two requests are equal when they carry the same {@link #code()} and the same
     * {@link #expectsMembershipApproval()} flag.
     *
     * @param obj the object to compare with; may be {@code null}
     * @return {@code true} when {@code obj} is an equal request, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqJoinGroupByInviteCodeRequest) obj;
        return this.expectsMembershipApproval == that.expectsMembershipApproval
                && Objects.equals(this.code, that.code);
    }

    /**
     * Returns a hash code derived from the code and approval flag.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(code, expectsMembershipApproval);
    }

    /**
     * Returns a debug string describing the code and approval flag.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "IqJoinGroupByInviteCodeRequest[code=" + code
                + ", expectsMembershipApproval=" + expectsMembershipApproval + ']';
    }
}
