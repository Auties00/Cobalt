package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code spam} IQ that reports an offending newsletter-status post under
 * the v2 schema.
 * <p>
 * The v2 schema replaces the legacy {@code <message>} envelope of {@link SmaxStatusReportRequest}
 * with a {@code <status>} envelope carrying {@code server_id} and {@code t} attributes plus an
 * optional payload child. Callers pair this request with {@link SmaxStatusReportV2Response} to
 * consume the relay's verdict.
 *
 * @implNote
 * This implementation flattens the WA Web mixin chain into a single {@link StanzaBuilder} that
 * pins {@code xmlns="spam"}, {@code to} to {@link JidServer#user()} and {@code type="set"}.
 * The supplied payload stanza is appended verbatim under {@code <status>} rather than being
 * composed from its constituent text-or-media parts as WA Web does.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutSpamStatusReportV2Request")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBaseReportMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamReportableNewsletterStatusMixin")
public final class SmaxStatusReportV2Request implements SmaxStanza.Request {
    /**
     * Holds the newsletter JID being reported.
     * <p>
     * This value is routed into the {@code <spam_list jid="..."/>} attribute.
     */
    private final Jid spamListJid;

    /**
     * Holds the spam-flow identifier naming the user-facing report flow.
     * <p>
     * This value is routed into the {@code <spam_list spam_flow="..."/>} attribute.
     */
    private final String spamListSpamFlow;

    /**
     * Holds the {@code server_id} of the offending status post.
     * <p>
     * This value is routed into the {@code <status server_id="..."/>} attribute.
     */
    private final long statusServerId;

    /**
     * Holds the Unix-second timestamp of the offending status post.
     * <p>
     * This value is routed into the {@code <status t="..."/>} attribute.
     */
    private final long statusTimestamp;

    /**
     * Holds the optional newsletter subject string echoed by the relay for attribution context.
     * <p>
     * This value is routed into the {@code <spam_list subject="..."/>} attribute, or omitted
     * when {@code null}.
     */
    private final String spamListSubject;

    /**
     * Holds the optional pre-built payload child for the {@code <status>} envelope.
     * <p>
     * When present this stanza carries the text-or-media inner content and is appended verbatim
     * under {@code <status>}; when {@code null} the {@code <status>} envelope is emitted without
     * a content child.
     */
    private final Stanza statusPayloadContent;

    /**
     * Constructs a v2 status-report request from the collected form fields and the harvested
     * offending status.
     *
     * @param spamListJid          the newsletter JID; never {@code null}
     * @param spamListSpamFlow     the spam-flow string; never {@code null}
     * @param statusServerId       the status's {@code server_id}
     * @param statusTimestamp      the status's Unix-second timestamp
     * @param spamListSubject      the optional subject; may be {@code null}
     * @param statusPayloadContent the optional payload child; may be {@code null}
     * @throws NullPointerException if {@code spamListJid} or {@code spamListSpamFlow} is
     *                              {@code null}
     */
    public SmaxStatusReportV2Request(Jid spamListJid, String spamListSpamFlow,
                   long statusServerId, long statusTimestamp,
                   String spamListSubject, Stanza statusPayloadContent) {
        this.spamListJid = Objects.requireNonNull(spamListJid, "spamListJid cannot be null");
        this.spamListSpamFlow = Objects.requireNonNull(spamListSpamFlow, "spamListSpamFlow cannot be null");
        this.statusServerId = statusServerId;
        this.statusTimestamp = statusTimestamp;
        this.spamListSubject = spamListSubject;
        this.statusPayloadContent = statusPayloadContent;
    }

    /**
     * Returns the newsletter JID.
     * <p>
     * This is the value routed into the {@code <spam_list jid>} attribute.
     *
     * @return the JID; never {@code null}
     */
    public Jid spamListJid() {
        return spamListJid;
    }

    /**
     * Returns the spam-flow identifier.
     * <p>
     * This is the value routed into the {@code <spam_list spam_flow>} attribute that names the
     * user-facing surface.
     *
     * @return the spam-flow; never {@code null}
     */
    public String spamListSpamFlow() {
        return spamListSpamFlow;
    }

    /**
     * Returns the status {@code server_id}.
     * <p>
     * This is the value routed into the {@code <status server_id>} attribute.
     *
     * @return the {@code server_id}
     */
    public long statusServerId() {
        return statusServerId;
    }

    /**
     * Returns the status timestamp.
     * <p>
     * This is the Unix-second value routed into the {@code <status t>} attribute.
     *
     * @return the timestamp
     */
    public long statusTimestamp() {
        return statusTimestamp;
    }

    /**
     * Returns the optional newsletter subject.
     * <p>
     * This is the value routed into the {@code <spam_list subject>} attribute, empty when it was
     * not supplied.
     *
     * @return an {@link Optional} carrying the subject, or empty when omitted
     */
    public Optional<String> spamListSubject() {
        return Optional.ofNullable(spamListSubject);
    }

    /**
     * Returns the optional pre-built payload child.
     * <p>
     * The result is empty when the {@code <status>} envelope should be emitted without a content
     * child.
     *
     * @return an {@link Optional} carrying the stanza, or empty when omitted
     */
    public Optional<Stanza> statusPayloadContent() {
        return Optional.ofNullable(statusPayloadContent);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation builds the {@code <status>} envelope first ({@code server_id},
     * {@code t}, optional payload child) and then nests it under {@code <spam_list>} with the
     * optional {@code subject} attribute; the outer {@code <iq>} carries no further optional
     * children.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutSpamStatusReportV2Request",
            exports = "makeStatusReportV2Request", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var statusBuilder = new StanzaBuilder()
                .description("status")
                .attribute("server_id", statusServerId)
                .attribute("t", statusTimestamp);
        if (statusPayloadContent != null) {
            statusBuilder.content(statusPayloadContent);
        }
        var spamListBuilder = new StanzaBuilder()
                .description("spam_list")
                .attribute("jid", spamListJid)
                .attribute("spam_flow", spamListSpamFlow);
        if (spamListSubject != null) {
            spamListBuilder.attribute("subject", spamListSubject);
        }
        spamListBuilder.content(statusBuilder.build());
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "spam")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(spamListBuilder.build());
    }

    /**
     * Compares this request to another for equality across all reported fields.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a {@link SmaxStatusReportV2Request} with equal
     *         JID, spam-flow, server id, timestamp, subject, and payload content
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxStatusReportV2Request) obj;
        return this.statusServerId == that.statusServerId
                && this.statusTimestamp == that.statusTimestamp
                && Objects.equals(this.spamListJid, that.spamListJid)
                && Objects.equals(this.spamListSpamFlow, that.spamListSpamFlow)
                && Objects.equals(this.spamListSubject, that.spamListSubject)
                && Objects.equals(this.statusPayloadContent, that.statusPayloadContent);
    }

    /**
     * Returns a hash code derived from all reported fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(spamListJid, spamListSpamFlow, statusServerId, statusTimestamp,
                spamListSubject, statusPayloadContent);
    }

    /**
     * Returns a debug string carrying the scalar reported fields.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxStatusReportV2Request[spamListJid=" + spamListJid
                + ", spamListSpamFlow=" + spamListSpamFlow
                + ", statusServerId=" + statusServerId
                + ", statusTimestamp=" + statusTimestamp
                + ", spamListSubject=" + spamListSubject + ']';
    }
}
