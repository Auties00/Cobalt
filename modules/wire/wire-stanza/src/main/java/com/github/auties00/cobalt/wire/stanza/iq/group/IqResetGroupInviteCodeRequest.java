package com.github.auties00.cobalt.wire.stanza.iq.group;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Models the outbound {@code <iq xmlns="w:g2" type="set" to="<group-jid>"><invite/></iq>} request that rotates the invite code of a group.
 *
 * <p>This request backs the "revoke invite link" admin action. The relay responds with the
 * freshly-issued code, and subscribers update the cached {@code chat.whatsapp.com/<code>} URL
 * accordingly. The caller must be a group admin or a privileged super-admin; otherwise the server
 * returns an {@link IqResetGroupInviteCodeResponse.ClientError}.
 *
 * @implNote
 * This implementation sends a bare {@code <invite/>} child with no attributes and addresses the IQ
 * {@code to} attribute at the target group JID rather than the {@code g.us} group server.
 */
@WhatsAppWebModule(moduleName = "WAWebGroupInviteJob")
public final class IqResetGroupInviteCodeRequest implements IqStanza.Request {
    /**
     * Holds the group whose invite code is being rotated.
     */
    private final Jid groupJid;

    /**
     * Constructs a request that rotates the invite code of the given group.
     *
     * <p>The {@code groupJid} ends up verbatim in the {@code <iq to="...">} attribute and never as
     * the payload of a child element, so it must be a group JID (server {@code g.us}); other JIDs
     * cause the server to reply with a malformed-stanza error.
     *
     * @param groupJid the group whose invite code is to be rotated; never {@code null}
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public IqResetGroupInviteCodeRequest(Jid groupJid) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
    }

    /**
     * Returns the group whose invite code is being rotated.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Builds the outbound IQ stanza as a {@link StanzaBuilder} ready for dispatch.
     *
     * <p>The {@code <invite/>} child is empty (no attributes, no children) because the relay
     * derives the new code server-side and echoes it back inside the
     * {@link IqResetGroupInviteCodeResponse.Success} reply.
     *
     * @return the IQ envelope builder
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebGroupInviteJob",
            exports = "resetGroupInviteCode", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var invitePayload = new StanzaBuilder()
                .description("invite")
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(invitePayload);
    }

    /**
     * Compares this request with another object for equality.
     *
     * <p>Two requests are equal when they carry the same {@link #groupJid()}.
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
        var that = (IqResetGroupInviteCodeRequest) obj;
        return Objects.equals(this.groupJid, that.groupJid);
    }

    /**
     * Returns a hash code derived from the group JID.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid);
    }

    /**
     * Returns a debug string describing the group JID.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "IqResetGroupInviteCodeRequest[groupJid=" + groupJid + ']';
    }
}
