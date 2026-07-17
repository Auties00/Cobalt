package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * The outbound {@code <iq xmlns="w:g2" type="get">} stanza that lists the pending membership-approval requests for a
 * group.
 * <p>
 * The default constructor sends the bare query; the rich constructor sets {@link #requestorFetch()} so the relay
 * populates the {@code requestor}, {@code requestor_pn}, and {@code requestor_username} attributes on each entry,
 * which the admin surface needs when triaging community-link join requests issued under a different identity. Replies
 * are parsed through {@link SmaxGroupsGetMembershipApprovalRequestsResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetMembershipApprovalRequestsRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseGetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetMembershipApprovalRequestsRequestorFetchMixin")
public final class SmaxGroupsGetMembershipApprovalRequestsRequest implements SmaxStanza.Request {
    /**
     * The group {@link Jid} whose pending approval queue is being queried; surfaced on the IQ's {@code to} attribute.
     */
    private final Jid groupJid;

    /**
     * Whether the relay should populate the rich {@code requestor}/{@code requestor_pn}/{@code requestor_username}
     * projection on each {@code <membership_approval_request>} child.
     */
    private final boolean requestorFetch;

    /**
     * Constructs a request without the rich requestor projection.
     * <p>
     * Delegates to {@link #SmaxGroupsGetMembershipApprovalRequestsRequest(Jid, boolean)} with
     * {@code requestorFetch=false}, surfacing only each requesting user JID and timestamp.
     *
     * @param groupJid the group {@link Jid}; never {@code null}
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxGroupsGetMembershipApprovalRequestsRequest(Jid groupJid) {
        this(groupJid, false);
    }

    /**
     * Constructs a fully-parametrised request.
     * <p>
     * Pass {@code requestorFetch=true} to surface the {@code requestor}/{@code requestor_pn}/
     * {@code requestor_username} attributes needed when sub-group admins triage community-link join requests issued
     * under a different identity.
     *
     * @param groupJid       the group {@link Jid}; never {@code null}
     * @param requestorFetch whether the relay should populate the rich requestor projection
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxGroupsGetMembershipApprovalRequestsRequest(Jid groupJid, boolean requestorFetch) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.requestorFetch = requestorFetch;
    }

    /**
     * Returns the group {@link Jid}.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns whether the rich requestor projection is requested.
     *
     * @return {@code true} when the relay should populate {@code requestor}/{@code requestor_pn}/
     *         {@code requestor_username}
     */
    public boolean requestorFetch() {
        return requestorFetch;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     * <p>
     * The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="get">
     *         <membership_approval_requests requestor_fetch="true"/>
     *     </iq>
     * }
     * where the {@code requestor_fetch} attribute is omitted when {@link #requestorFetch()} is {@code false}.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <membership_approval_requests/>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsGetMembershipApprovalRequestsRequest",
            exports = "makeGetMembershipApprovalRequestsRequest",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var payloadBuilder = new StanzaBuilder()
                .description("membership_approval_requests");
        if (requestorFetch) {
            payloadBuilder.attribute("requestor_fetch", "true");
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "get")
                .content(payloadBuilder.build());
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsGetMembershipApprovalRequestsRequest} with identical
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
        var that = (SmaxGroupsGetMembershipApprovalRequestsRequest) obj;
        return this.requestorFetch == that.requestorFetch
                && Objects.equals(this.groupJid, that.groupJid);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, requestorFetch);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsGetMembershipApprovalRequestsRequest[groupJid=" + groupJid
                + ", requestorFetch=" + requestorFetch + ']';
    }
}
