package com.github.auties00.cobalt.wire.linked.reporting;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.reportGroupForSpam} — reports a
 * group chat for trust-and-safety review.
 *
 * <p>{@link #group} and {@link #spamFlow} are required. The other
 * fields are optional attribution context: {@link #adder} for surfacing
 * add-spam patterns, {@link #subject} for context, {@link #isKnownChat}
 * for the known-chat marker, and {@link #reportedMessageIds} for the
 * list of evidence messages.
 */
@ProtobufMessage
public final class GroupSpamReport {
    /**
     * JID of the reported group.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid group;

    /**
     * Spam-flow code identifying which UI flow produced this report.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String spamFlow;

    /**
     * Optional JID of the user who originally added the reporter to
     * the group.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid adder;

    /**
     * Optional group subject echoed for attribution context.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String subject;

    /**
     * Optional known-chat marker.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String isKnownChat;

    /**
     * Optional list of reported message stanza ids attached as evidence.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final List<String> reportedMessageIds;

    /**
     * Constructs a new {@code GroupSpamReport}.
     *
     * @param group              the reported group JID; required
     * @param spamFlow           the spam-flow code; required
     * @param adder              the optional adder JID, or {@code null}
     * @param subject            the optional subject, or {@code null}
     * @param isKnownChat        the optional known-chat marker
     * @param reportedMessageIds the optional list of evidence message ids
     * @throws NullPointerException if {@code group} or {@code spamFlow}
     *                              is {@code null}
     */
    GroupSpamReport(Jid group, String spamFlow, Jid adder, String subject, String isKnownChat,
                    List<String> reportedMessageIds) {
        this.group = Objects.requireNonNull(group, "group cannot be null");
        this.spamFlow = Objects.requireNonNull(spamFlow, "spamFlow cannot be null");
        this.adder = adder;
        this.subject = subject;
        this.isKnownChat = isKnownChat;
        this.reportedMessageIds = reportedMessageIds;
    }

    /**
     * Returns the reported group JID.
     *
     * @return the JID, never {@code null}
     */
    public Jid group() {
        return group;
    }

    /**
     * Returns the spam-flow code.
     *
     * @return the code, never {@code null}
     */
    public String spamFlow() {
        return spamFlow;
    }

    /**
     * Returns the optional adder JID.
     *
     * @return an {@link Optional} carrying the adder JID, or empty when unset
     */
    public Optional<Jid> adder() {
        return Optional.ofNullable(adder);
    }

    /**
     * Returns the optional group subject.
     *
     * @return an {@link Optional} carrying the subject, or empty when unset
     */
    public Optional<String> subject() {
        return Optional.ofNullable(subject);
    }

    /**
     * Returns the optional known-chat marker.
     *
     * @return an {@link Optional} carrying the marker, or empty when unset
     */
    public Optional<String> isKnownChat() {
        return Optional.ofNullable(isKnownChat);
    }

    /**
     * Returns the evidence message ids.
     *
     * @return the message ids; never {@code null}
     */
    public List<String> reportedMessageIds() {
        return reportedMessageIds == null ? List.of() : reportedMessageIds;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GroupSpamReport) obj;
        return Objects.equals(group, that.group) &&
                Objects.equals(spamFlow, that.spamFlow) &&
                Objects.equals(adder, that.adder) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(isKnownChat, that.isKnownChat) &&
                Objects.equals(reportedMessageIds, that.reportedMessageIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, spamFlow, adder, subject, isKnownChat, reportedMessageIds);
    }

    @Override
    public String toString() {
        return "GroupSpamReport[" +
                "group=" + group + ", " +
                "spamFlow=" + spamFlow + ", " +
                "adder=" + adder + ", " +
                "subject=" + subject + ", " +
                "isKnownChat=" + isKnownChat + ", " +
                "reportedMessageIds=" + reportedMessageIds + ']';
    }
}
