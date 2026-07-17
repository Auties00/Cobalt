package com.github.auties00.cobalt.wire.linked.reporting;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.reportIndividualForSpam} —
 * reports a one-to-one contact for trust-and-safety review.
 *
 * <p>{@link #target} and {@link #spamFlow} are required.
 * {@link #isKnownChat} is optional context, and
 * {@link #reportedMessageIds} carries zero or more evidence message
 * ids (empty list when no messages are attached).
 */
@ProtobufMessage
public final class IndividualSpamReport {
    /**
     * JID of the reported contact.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid target;

    /**
     * WhatsApp spam-flow code identifying which UI flow produced this
     * report.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String spamFlow;

    /**
     * Optional {@code is_known_chat} marker (typically {@code "1"} when
     * the reporter already has the target in their chat list).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String isKnownChat;

    /**
     * Optional list of reported message stanza ids attached as evidence.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> reportedMessageIds;

    /**
     * Constructs a new {@code IndividualSpamReport}.
     *
     * @param target             the reported contact JID; required
     * @param spamFlow           the spam-flow code; required
     * @param isKnownChat        the optional known-chat marker
     * @param reportedMessageIds the optional list of evidence message ids
     * @throws NullPointerException if {@code target} or {@code spamFlow}
     *                              is {@code null}
     */
    IndividualSpamReport(Jid target, String spamFlow, String isKnownChat, List<String> reportedMessageIds) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.spamFlow = Objects.requireNonNull(spamFlow, "spamFlow cannot be null");
        this.isKnownChat = isKnownChat;
        this.reportedMessageIds = reportedMessageIds;
    }

    /**
     * Returns the reported contact JID.
     *
     * @return the JID, never {@code null}
     */
    public Jid target() {
        return target;
    }

    /**
     * Returns the spam-flow code.
     *
     * @return the spam-flow code, never {@code null}
     */
    public String spamFlow() {
        return spamFlow;
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
        var that = (IndividualSpamReport) obj;
        return Objects.equals(target, that.target) &&
                Objects.equals(spamFlow, that.spamFlow) &&
                Objects.equals(isKnownChat, that.isKnownChat) &&
                Objects.equals(reportedMessageIds, that.reportedMessageIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, spamFlow, isKnownChat, reportedMessageIds);
    }

    @Override
    public String toString() {
        return "IndividualSpamReport[" +
                "target=" + target + ", " +
                "spamFlow=" + spamFlow + ", " +
                "isKnownChat=" + isKnownChat + ", " +
                "reportedMessageIds=" + reportedMessageIds + ']';
    }
}
