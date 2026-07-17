package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * The outbound {@code <iq type="set" xmlns="w:g2">} stanza that flags a single message in a group for moderator
 * review.
 *
 * <p>Backs the "Report message" affordance available to group admins on the message context menu. The relay
 * returns a bare {@link SmaxGroupsReportMessagesResponse.Success} envelope on success; there is no per-report
 * payload because the report is opaque to the client.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsReportMessagesRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
public final class SmaxGroupsReportMessagesRequest implements SmaxStanza.Request {
    /**
     * The group {@link Jid} hosting the reported message.
     */
    private final Jid groupJid;

    /**
     * The stanza identifier of the reported message.
     */
    private final String reportMessageId;

    /**
     * Constructs a report-messages request.
     *
     * @param groupJid        the group {@link Jid}
     * @param reportMessageId the reported message id (a stanza id as carried by WhatsApp message envelopes)
     * @throws NullPointerException if either argument is {@code null}
     */
    public SmaxGroupsReportMessagesRequest(Jid groupJid, String reportMessageId) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.reportMessageId = Objects.requireNonNull(reportMessageId, "reportMessageId cannot be null");
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
     * Returns the reported message identifier.
     *
     * @return the stanza id of the message being reported; never {@code null}
     */
    public String reportMessageId() {
        return reportMessageId;
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * <p>The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="set">
     *         <reports>
     *             <report message_id="<reportMessageId>"/>
     *         </reports>
     *     </iq>
     * }
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <reports>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsReportMessagesRequest",
            exports = "makeReportMessagesRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var reportNode = new StanzaBuilder()
                .description("report")
                .attribute("message_id", reportMessageId)
                .build();
        var reportsNode = new StanzaBuilder()
                .description("reports")
                .content(reportNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(reportsNode);
    }

    /**
     * Compares this request to {@code obj} for value equality across both fields.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsReportMessagesRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsReportMessagesRequest) obj;
        return Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.reportMessageId, that.reportMessageId);
    }

    /**
     * Returns a hash composed of both fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, reportMessageId);
    }

    /**
     * Returns a debug string carrying both fields.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsReportMessagesRequest[groupJid=" + groupJid
                + ", reportMessageId=" + reportMessageId + ']';
    }
}
