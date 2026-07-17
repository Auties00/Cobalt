package com.github.auties00.cobalt.wire.stanza.iq.group;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupInvite;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

import java.util.Objects;

/**
 * Models the outbound {@code <iq xmlns="w:g2" type="get">} stanza that fetches the group metadata
 * behind a v4 in-band invite.
 *
 * <p>A v4 invite ({@link GroupInvite}) carries the invite code, the issuing admin, the invited user,
 * and the expiration. Dispatching this request as an {@link IqStanza.Request} produces an
 * {@code <iq>} envelope addressed to the issuing admin, wrapping a single {@code <add_request>} child
 * carrying the {@code code}, {@code expiration}, {@code admin}, and {@code invitee} attributes. The
 * relay replies with the group metadata parsed by {@link IqQueryGroupInviteV4Response}.
 *
 * <p>This shape is distinct from the usync/SMAX group-info fetch: the {@code <add_request>} node sits
 * directly under the {@code <iq>} and carries the {@code invitee}, because the query redeems a
 * specific invite rather than a bare group JID.
 */
@WhatsAppWebModule(moduleName = "WAWebGroupInviteV4Job")
public final class IqQueryGroupInviteV4Request implements IqStanza.Request {
    /**
     * Holds the v4 invite whose four fields populate the {@code <add_request>} child and the
     * {@code <iq>} recipient.
     *
     * <p>The invite's {@link GroupInvite#sender()} is used both as the {@code admin} attribute and as
     * the IQ recipient; {@link GroupInvite#invitee()} becomes the {@code invitee} attribute,
     * {@link GroupInvite#inviteCode()} the {@code code} attribute, and
     * {@link GroupInvite#expiration()} the {@code expiration} attribute. Never {@code null}.
     */
    private final GroupInvite invite;

    /**
     * Constructs a query-group-invite-v4 request bound to the given invite.
     *
     * @param invite the v4 invite; never {@code null}
     * @throws NullPointerException if {@code invite} is {@code null}
     */
    public IqQueryGroupInviteV4Request(GroupInvite invite) {
        this.invite = Objects.requireNonNull(invite, "invite cannot be null");
    }

    /**
     * Returns the invite bound to this request.
     *
     * @return the invite; never {@code null}
     */
    public GroupInvite invite() {
        return invite;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code <iq xmlns="w:g2" type="get">} envelope addressed to the invite's
     * {@link GroupInvite#sender()}, wrapping a single {@code <add_request>} child that carries the
     * {@code code}, {@code expiration} (in epoch seconds), {@code admin}, and {@code invitee}
     * attributes. The IQ {@code id} attribute is assigned by the dispatch layer.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the {@code <add_request>}
     *         payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebGroupInviteV4Job", exports = "queryGroupInviteV4",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var addRequestNode = new StanzaBuilder()
                .description("add_request")
                .attribute("code", invite.inviteCode())
                .attribute("expiration", invite.expiration().getEpochSecond())
                .attribute("admin", invite.sender())
                .attribute("invitee", invite.invitee())
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", invite.sender())
                .attribute("type", "get")
                .content(addRequestNode);
    }

    /**
     * Compares this request with another object for value equality on the bound invite.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an {@link IqQueryGroupInviteV4Request} carrying an
     *         equal invite, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqQueryGroupInviteV4Request) obj;
        return Objects.equals(this.invite, that.invite);
    }

    /**
     * Returns a hash code derived from the bound invite.
     *
     * @return the field-derived hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(invite);
    }

    /**
     * Returns a debug string rendering the bound invite.
     *
     * @return a string representation of this request
     */
    @Override
    public String toString() {
        return "IqQueryGroupInviteV4Request[invite=" + invite + ']';
    }
}
