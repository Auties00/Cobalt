package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;

/**
 * Previews a group via its public invite code as an {@code <iq xmlns="w:g2" type="get" to="g.us">} stanza.
 *
 * <p>The caller passes the suffix of a {@code chat.whatsapp.com/<code>} URL and dispatches through the matching
 * {@link SmaxGroupsGetInviteGroupInfoResponse} parser to materialise the inviting group's preview metadata
 * before committing to joining.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetInviteGroupInfoRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseGetServerMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQGetRequestMixin")
public final class SmaxGroupsGetInviteGroupInfoRequest implements SmaxStanza.Request {
    /**
     * Holds the public invite code surfaced under the {@code <invite code="..."/>} child.
     */
    private final String inviteCode;

    /**
     * Constructs a request for the given invite code.
     *
     * <p>The caller passes the suffix of a {@code chat.whatsapp.com/<code>} URL verbatim; the relay rejects
     * empty codes and treats unknown codes as a client error.
     *
     * @param inviteCode the public invite code; never {@code null}
     * @throws NullPointerException if {@code inviteCode} is {@code null}
     */
    public SmaxGroupsGetInviteGroupInfoRequest(String inviteCode) {
        this.inviteCode = Objects.requireNonNull(inviteCode, "inviteCode cannot be null");
    }

    /**
     * Returns the invite code carried by this request.
     *
     * <p>The value is surfaced verbatim under {@code <invite code="..."/>}.
     *
     * @return the invite code; never {@code null}
     */
    public String inviteCode() {
        return inviteCode;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="g.us" type="get">
     *         <invite code="<inviteCode>"/>
     *     </iq>
     * }
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <invite/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsGetInviteGroupInfoRequest",
            exports = "makeGetInviteGroupInfoRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var inviteNode = new StanzaBuilder()
                .description("invite")
                .attribute("code", inviteCode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", JidServer.groupOrCommunity())
                .attribute("type", "get")
                .content(inviteNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsGetInviteGroupInfoRequest} with the same
     *         invite code
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsGetInviteGroupInfoRequest) obj;
        return Objects.equals(this.inviteCode, that.inviteCode);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(inviteCode);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsGetInviteGroupInfoRequest[inviteCode=" + inviteCode + ']';
    }
}
