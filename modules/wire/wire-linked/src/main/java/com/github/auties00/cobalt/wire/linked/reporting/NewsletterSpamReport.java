package com.github.auties00.cobalt.wire.linked.reporting;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.reportNewsletterForSpam} —
 * reports a newsletter (channel) for trust-and-safety review.
 *
 * <p>{@link #spamListJid} and {@link #spamListSpamFlow} are required.
 * {@link #spamListSubject} is optional context, and {@link #messages}
 * carries zero or more evidence-message entries.
 */
@ProtobufMessage
public final class NewsletterSpamReport {
    /**
     * JID of the reported newsletter.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid spamListJid;

    /**
     * Spam-flow code identifying which UI flow produced this report.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String spamListSpamFlow;

    /**
     * Optional newsletter subject echoed for attribution context.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String spamListSubject;

    /**
     * Optional list of reported message entries attached as evidence.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final List<NewsletterSpamReportEntry> messages;

    /**
     * Constructs a new {@code NewsletterSpamReport}.
     *
     * @param spamListJid      the newsletter JID; required
     * @param spamListSpamFlow the spam-flow code; required
     * @param spamListSubject  the optional subject, or {@code null}
     * @param messages         the optional list of evidence entries
     * @throws NullPointerException if {@code spamListJid} or
     *                              {@code spamListSpamFlow} is {@code null}
     */
    NewsletterSpamReport(Jid spamListJid, String spamListSpamFlow, String spamListSubject,
                         List<NewsletterSpamReportEntry> messages) {
        this.spamListJid = Objects.requireNonNull(spamListJid, "spamListJid cannot be null");
        this.spamListSpamFlow = Objects.requireNonNull(spamListSpamFlow, "spamListSpamFlow cannot be null");
        this.spamListSubject = spamListSubject;
        this.messages = messages;
    }

    /**
     * Returns the reported newsletter JID.
     *
     * @return the JID, never {@code null}
     */
    public Jid spamListJid() {
        return spamListJid;
    }

    /**
     * Returns the spam-flow code.
     *
     * @return the code, never {@code null}
     */
    public String spamListSpamFlow() {
        return spamListSpamFlow;
    }

    /**
     * Returns the optional newsletter subject.
     *
     * @return an {@link Optional} carrying the subject, or empty when unset
     */
    public Optional<String> spamListSubject() {
        return Optional.ofNullable(spamListSubject);
    }

    /**
     * Returns the evidence message entries.
     *
     * @return the entries; never {@code null}
     */
    public List<NewsletterSpamReportEntry> messages() {
        return messages == null ? List.of() : messages;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (NewsletterSpamReport) obj;
        return Objects.equals(spamListJid, that.spamListJid) &&
                Objects.equals(spamListSpamFlow, that.spamListSpamFlow) &&
                Objects.equals(spamListSubject, that.spamListSubject) &&
                Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spamListJid, spamListSpamFlow, spamListSubject, messages);
    }

    @Override
    public String toString() {
        return "NewsletterSpamReport[" +
                "spamListJid=" + spamListJid + ", " +
                "spamListSpamFlow=" + spamListSpamFlow + ", " +
                "spamListSubject=" + spamListSubject + ", " +
                "messages=" + messages + ']';
    }
}
