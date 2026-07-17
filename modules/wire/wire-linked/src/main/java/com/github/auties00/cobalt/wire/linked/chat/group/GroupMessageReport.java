package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single in-group message that one or more group members have
 * previously reported to the group's administrators.
 *
 * <p>WhatsApp groups configured with the
 * {@code report_to_admin_mode} flag enabled allow ordinary members to
 * forward an offensive or unwanted message to the group's admins for
 * review. The relay tracks each report under the offending message's
 * stanza id and surfaces them through the
 * {@code GetReportedMessages} flow when an admin opens the
 * "View previously reported messages" UI surface.
 *
 * <p>Each report carries the offending message's stanza id together with
 * the list of {@link Reporter} entries that filed reports against it. A
 * single message can accumulate multiple reporters when several members
 * flag the same content.
 */
@ProtobufMessage(name = "GroupMessageReport")
public final class GroupMessageReport {
    /**
     * The stanza-id of the reported message. Required, never {@code null}
     * after construction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String messageId;

    /**
     * The non-empty list of reporters that filed reports against the
     * message. Each reporter contributes an independent report row.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<Reporter> reporters;

    /**
     * Constructs a new report entry.
     *
     * @param messageId the offending message's stanza id; must not be
     *                  {@code null}
     * @param reporters the per-reporter rows; must not be {@code null}
     *                  and must contain at least one entry
     * @throws NullPointerException     if any reference argument is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code reporters} is empty
     */
    GroupMessageReport(String messageId, List<Reporter> reporters) {
        this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(reporters, "reporters cannot be null");
        if (reporters.isEmpty()) {
            throw new IllegalArgumentException("reporters cannot be empty");
        }
        this.reporters = List.copyOf(reporters);
    }

    /**
     * Returns the stanza-id of the reported message.
     *
     * @return the non-{@code null} stanza id
     */
    public String messageId() {
        return messageId;
    }

    /**
     * Returns the list of reporters that filed reports against the
     * message.
     *
     * @return an unmodifiable, non-empty list of reporters
     */
    public List<Reporter> reporters() {
        return reporters == null ? List.of() : Collections.unmodifiableList(reporters);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GroupMessageReport that
                && Objects.equals(messageId, that.messageId)
                && Objects.equals(reporters, that.reporters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, reporters);
    }

    /**
     * Per-reporter projection — captures who filed the report and when.
     */
    @ProtobufMessage(name = "GroupMessageReport.Reporter")
    public static final class Reporter {
        /**
         * The reporter's user JID. Required, never {@code null} after
         * construction.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid reporterJid;

        /**
         * The instant at which the reporter filed the report. Required,
         * never {@code null} after construction.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
        Instant reportTimestamp;

        /**
         * Constructs a new reporter row.
         *
         * @param reporterJid     the reporter's JID; must not be
         *                        {@code null}
         * @param reportTimestamp the instant the report was filed at;
         *                        must not be {@code null}
         * @throws NullPointerException if any argument is {@code null}
         */
        Reporter(Jid reporterJid, Instant reportTimestamp) {
            this.reporterJid = Objects.requireNonNull(reporterJid, "reporterJid cannot be null");
            this.reportTimestamp = Objects.requireNonNull(reportTimestamp, "reportTimestamp cannot be null");
        }

        /**
         * Returns the reporter's JID.
         *
         * @return the non-{@code null} JID
         */
        public Jid reporterJid() {
            return reporterJid;
        }

        /**
         * Returns the instant the report was filed at.
         *
         * @return the non-{@code null} timestamp
         */
        public Instant reportTimestamp() {
            return reportTimestamp;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Reporter that
                    && Objects.equals(reporterJid, that.reporterJid)
                    && Objects.equals(reportTimestamp, that.reportTimestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reporterJid, reportTimestamp);
        }
    }
}
