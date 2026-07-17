package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Outbound {@code <iq type="set" xmlns="w:g2">} stanza that accepts a v4 group invite.
 *
 * This request drives the join-via-v4-invite-link flow. The {@link #acceptCode()}, {@link #acceptExpiration()} and
 * {@link #acceptAdmin()} values are copied verbatim from the originating {@code <add_request/>} payload; the relay
 * only matches the pending invite when all three echo exactly. The reply is parsed by
 * {@link SmaxGroupsAcceptGroupAddResponse#of(Stanza, Stanza)}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsAcceptGroupAddRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQSetRequestMixin")
public final class SmaxGroupsAcceptGroupAddRequest implements SmaxStanza.Request {
    /**
     * Holds the group {@link Jid} hosting the pending {@code <add_request/>}.
     */
    private final Jid groupJid;

    /**
     * Holds the {@code code} attribute echoed verbatim from the pending {@code <add_request/>}.
     */
    private final String acceptCode;

    /**
     * Holds the {@code expiration} attribute, in seconds since epoch, echoed verbatim from the pending
     * {@code <add_request/>}.
     */
    private final long acceptExpiration;

    /**
     * Holds the {@link Jid} of the admin who issued the pending {@code <add_request/>}.
     */
    private final Jid acceptAdmin;

    /**
     * Constructs a fully-parametrised accept-group-add request.
     *
     * The {@code acceptCode}, {@code acceptExpiration} and {@code acceptAdmin} values must be passed straight from
     * the {@code <add_request/>} the caller is responding to; the relay only matches the pending invite when all
     * three echo exactly.
     *
     * @param groupJid         the {@link Jid} of the group hosting the pending invite
     * @param acceptCode       the {@code code} attribute from the pending {@code <add_request/>}
     * @param acceptExpiration the {@code expiration} attribute from the pending {@code <add_request/>}
     * @param acceptAdmin      the inviting admin's {@link Jid}
     * @throws NullPointerException if {@code groupJid}, {@code acceptCode} or {@code acceptAdmin} is {@code null}
     */
    public SmaxGroupsAcceptGroupAddRequest(Jid groupJid, String acceptCode, long acceptExpiration, Jid acceptAdmin) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.acceptCode = Objects.requireNonNull(acceptCode, "acceptCode cannot be null");
        this.acceptExpiration = acceptExpiration;
        this.acceptAdmin = Objects.requireNonNull(acceptAdmin, "acceptAdmin cannot be null");
    }

    /**
     * Returns the group {@link Jid} hosting the pending invite.
     *
     * The value routes verbatim into the IQ's {@code to} attribute.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the {@code code} attribute being echoed.
     *
     * The value is surfaced verbatim under {@code <accept code="..."/>}.
     *
     * @return the accept code; never {@code null}
     */
    public String acceptCode() {
        return acceptCode;
    }

    /**
     * Returns the {@code expiration} attribute being echoed.
     *
     * The value is surfaced verbatim as {@code <accept expiration="..."/>} and carries seconds since epoch.
     *
     * @return the expiration timestamp in seconds since epoch
     */
    public long acceptExpiration() {
        return acceptExpiration;
    }

    /**
     * Returns the inviting admin's {@link Jid}.
     *
     * The value is surfaced verbatim as {@code <accept admin="..."/>}.
     *
     * @return the admin {@link Jid}; never {@code null}
     */
    public Jid acceptAdmin() {
        return acceptAdmin;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="set">
     *         <accept code="<acceptCode>" expiration="<acceptExpiration>" admin="<acceptAdmin>"/>
     *     </iq>
     * }
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <accept/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsAcceptGroupAddRequest",
            exports = "makeAcceptGroupAddRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var acceptNode = new StanzaBuilder()
                .description("accept")
                .attribute("code", acceptCode)
                .attribute("expiration", acceptExpiration)
                .attribute("admin", acceptAdmin)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(acceptNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsAcceptGroupAddRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsAcceptGroupAddRequest) obj;
        return this.acceptExpiration == that.acceptExpiration
                && Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.acceptCode, that.acceptCode)
                && Objects.equals(this.acceptAdmin, that.acceptAdmin);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, acceptCode, acceptExpiration, acceptAdmin);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsAcceptGroupAddRequest[groupJid=" + groupJid
                + ", acceptCode=" + acceptCode
                + ", acceptExpiration=" + acceptExpiration
                + ", acceptAdmin=" + acceptAdmin + ']';
    }
}
