package com.github.auties00.cobalt.wire.linked.reporting;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * One reported message entry attached to a {@link NewsletterSpamReport}.
 * Mirrors the wire-level
 * {@code com.github.auties00.cobalt.node.smax.support.SmaxNewsletterReportMessageEntry}
 * tuple: the sender JID, the message timestamp (unix seconds), and the
 * stanza id.
 *
 * <p>All three fields are independently optional — the relay tolerates
 * partial evidence rows.
 */
@ProtobufMessage
public final class NewsletterSpamReportEntry {
    /**
     * Sender JID of the offending message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid messageFrom;

    /**
     * Message timestamp in seconds since the Unix epoch.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final long messageTimestamp;

    /**
     * Stanza id of the offending message.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String messageId;

    /**
     * Constructs a new {@code NewsletterSpamReportEntry}.
     *
     * @param messageFrom      the sender JID, or {@code null}
     * @param messageTimestamp the message timestamp in seconds
     * @param messageId        the optional stanza id
     */
    NewsletterSpamReportEntry(Jid messageFrom, long messageTimestamp, String messageId) {
        this.messageFrom = messageFrom;
        this.messageTimestamp = messageTimestamp;
        this.messageId = messageId;
    }

    /**
     * Returns the sender JID.
     *
     * @return an {@link Optional} carrying the JID, or empty when unset
     */
    public Optional<Jid> messageFrom() {
        return Optional.ofNullable(messageFrom);
    }

    /**
     * Returns the message timestamp.
     *
     * @return the timestamp in seconds
     */
    public long messageTimestamp() {
        return messageTimestamp;
    }

    /**
     * Returns the stanza id.
     *
     * @return an {@link Optional} carrying the id, or empty when unset
     */
    public Optional<String> messageId() {
        return Optional.ofNullable(messageId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (NewsletterSpamReportEntry) obj;
        return Objects.equals(messageFrom, that.messageFrom) &&
                messageTimestamp == that.messageTimestamp &&
                Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageFrom, messageTimestamp, messageId);
    }

    @Override
    public String toString() {
        return "NewsletterSpamReportEntry[" +
                "messageFrom=" + messageFrom + ", " +
                "messageTimestamp=" + messageTimestamp + ", " +
                "messageId=" + messageId + ']';
    }
}
