package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reports a newsletter and its offending messages as an outbound {@code spam} IQ.
 *
 * <p>Every field is required: the newsletter JID, spam-flow, subject and the list of offending
 * messages, each modelled by a {@link SmaxNewsletterReportMessageEntry}. The relay's verdict is
 * parsed by {@link SmaxNewsletterReportResponse}.
 *
 * @implNote
 * This implementation flattens the WA Web mixin chain into a single {@link StanzaBuilder} that pins
 * {@code xmlns="spam"}, {@code to=JidServer.user()} and {@code type="set"}. Unlike the group and
 * individual variants, the outer IQ never carries optional children.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutSpamNewsletterReportRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBaseReportMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSpamEntitySubjectMixin")
public final class SmaxNewsletterReportRequest implements SmaxStanza.Request {
    /**
     * Holds the newsletter JID being reported, routed into {@code <spam_list jid="..."/>}.
     */
    private final Jid spamListJid;

    /**
     * Holds the spam-flow identifier routed into {@code <spam_list spam_flow="..."/>}, naming the
     * user-facing surface that issued the report.
     */
    private final String spamListSpamFlow;

    /**
     * Holds the newsletter subject string routed into {@code <spam_list subject="..."/>} for
     * attribution context.
     */
    private final String spamListSubject;

    /**
     * Holds the offending {@link SmaxNewsletterReportMessageEntry} entries appended in insertion
     * order under {@code <spam_list>} (WA Web caps the count at 65).
     */
    private final List<SmaxNewsletterReportMessageEntry> messages;

    /**
     * Constructs a newsletter-report request.
     *
     * @param spamListJid      the newsletter JID; never {@code null}
     * @param spamListSpamFlow the spam-flow string; never {@code null}
     * @param spamListSubject  the subject string; never {@code null}
     * @param messages         the message entries; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxNewsletterReportRequest(Jid spamListJid, String spamListSpamFlow, String spamListSubject,
                   List<SmaxNewsletterReportMessageEntry> messages) {
        this.spamListJid = Objects.requireNonNull(spamListJid, "spamListJid cannot be null");
        this.spamListSpamFlow = Objects.requireNonNull(spamListSpamFlow, "spamListSpamFlow cannot be null");
        this.spamListSubject = Objects.requireNonNull(spamListSubject, "spamListSubject cannot be null");
        this.messages = List.copyOf(Objects.requireNonNull(messages, "messages cannot be null"));
    }

    /**
     * Returns the newsletter JID, the {@code <spam_list jid>} value.
     *
     * @return the JID; never {@code null}
     */
    public Jid spamListJid() {
        return spamListJid;
    }

    /**
     * Returns the spam-flow identifier, the {@code <spam_list spam_flow>} value.
     *
     * @return the spam-flow; never {@code null}
     */
    public String spamListSpamFlow() {
        return spamListSpamFlow;
    }

    /**
     * Returns the newsletter subject, the {@code <spam_list subject>} value.
     *
     * @return the subject; never {@code null}
     */
    public String spamListSubject() {
        return spamListSubject;
    }

    /**
     * Returns the message entries that {@link #toStanza()} embeds under {@code <spam_list>}.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<SmaxNewsletterReportMessageEntry> messages() {
        return messages;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Materialises each message entry into a {@code <message>} child under {@code <spam_list>}
     * and wraps it in the outer {@code <iq>} envelope.
     *
     * @implNote
     * This implementation materialises each entry via
     * {@link SmaxNewsletterReportMessageEntry#toNode()}; the outer IQ never carries optional
     * children.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutSpamNewsletterReportRequest",
            exports = "makeNewsletterReportRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>(messages.size());
        for (var message : messages) {
            children.add(message.toStanza());
        }
        var spamListBuilder = new StanzaBuilder()
                .description("spam_list")
                .attribute("jid", spamListJid)
                .attribute("spam_flow", spamListSpamFlow)
                .attribute("subject", spamListSubject)
                .content(children);
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "spam")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(spamListBuilder.build());
    }

    /**
     * Compares this request to another for value equality across all fields.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal {@link SmaxNewsletterReportRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewsletterReportRequest) obj;
        return Objects.equals(this.spamListJid, that.spamListJid)
                && Objects.equals(this.spamListSpamFlow, that.spamListSpamFlow)
                && Objects.equals(this.spamListSubject, that.spamListSubject)
                && Objects.equals(this.messages, that.messages);
    }

    /**
     * Returns a hash code derived from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(spamListJid, spamListSpamFlow, spamListSubject, messages);
    }

    /**
     * Returns a debug string listing every field.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxNewsletterReportRequest[spamListJid=" + spamListJid
                + ", spamListSpamFlow=" + spamListSpamFlow
                + ", spamListSubject=" + spamListSubject
                + ", messages=" + messages + ']';
    }
}
