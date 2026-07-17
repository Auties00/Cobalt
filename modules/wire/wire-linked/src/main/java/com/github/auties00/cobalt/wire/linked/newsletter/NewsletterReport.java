package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single moderation report filed against a newsletter.
 *
 * <p>The relay tracks every moderation report filed against newsletters
 * the authenticated account has authority over. Each entry exposes the
 * status of the report, the channel it was filed against, the kind of
 * content the report concerns and the optional appeal record (if the
 * channel owner has filed one).
 *
 * <p>The type of reported content is captured by {@link #reportedContent()},
 * which discriminates between the three GraphQL inline fragments the
 * relay returns: a regular channel post, a status update, or a question
 * response. Each variant carries the relevant identifiers in fields
 * appropriate to that content kind.
 */
@ProtobufMessage
public final class NewsletterReport {
    /**
     * The relay-assigned identifier of this report.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String reportId;

    /**
     * The wire-level status string (for example {@code "PENDING"} or
     * {@code "RESOLVED"}).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String status;

    /**
     * The instant at which this report was filed.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant creationTime;

    /**
     * The instant at which this report was last updated.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant lastUpdateTime;

    /**
     * The display name of the channel the report was filed against.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String channelName;

    /**
     * The JID of the channel the report was filed against.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    Jid channelJid;

    /**
     * The reported content descriptor.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    ReportedContent reportedContent;

    /**
     * The associated appeal record, when the channel owner has filed one.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    NewsletterReportAppeal appeal;

    /**
     * Constructs a new {@code NewsletterReport}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param reportId        the report identifier, may be {@code null}
     * @param status          the wire status string, may be {@code null}
     * @param creationTime    the moment the report was filed, may be {@code null}
     * @param lastUpdateTime  the moment of last update, may be {@code null}
     * @param channelName     the channel display name, may be {@code null}
     * @param channelJid      the channel JID, may be {@code null}
     * @param reportedContent the reported content descriptor, may be {@code null}
     * @param appeal          the associated appeal record, may be {@code null}
     */
    NewsletterReport(String reportId, String status, Instant creationTime, Instant lastUpdateTime, String channelName, Jid channelJid, ReportedContent reportedContent, NewsletterReportAppeal appeal) {
        this.reportId = reportId;
        this.status = status;
        this.creationTime = creationTime;
        this.lastUpdateTime = lastUpdateTime;
        this.channelName = channelName;
        this.channelJid = channelJid;
        this.reportedContent = reportedContent;
        this.appeal = appeal;
    }

    /**
     * Returns the relay-assigned identifier of this report.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         not reported
     */
    public Optional<String> reportId() {
        return Optional.ofNullable(reportId);
    }

    /**
     * Returns the wire-level status string.
     *
     * @return an {@link Optional} carrying the status, or empty when not
     *         reported
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the instant at which this report was filed.
     *
     * @return an {@link Optional} carrying the creation instant, or
     *         empty when not reported
     */
    public Optional<Instant> creationTime() {
        return Optional.ofNullable(creationTime);
    }

    /**
     * Returns the instant at which this report was last updated.
     *
     * @return an {@link Optional} carrying the last-update instant, or
     *         empty when not reported
     */
    public Optional<Instant> lastUpdateTime() {
        return Optional.ofNullable(lastUpdateTime);
    }

    /**
     * Returns the display name of the channel this report was filed
     * against.
     *
     * @return an {@link Optional} carrying the channel name, or empty
     *         when not reported
     */
    public Optional<String> channelName() {
        return Optional.ofNullable(channelName);
    }

    /**
     * Returns the JID of the channel this report was filed against.
     *
     * @return an {@link Optional} carrying the channel JID, or empty
     *         when not reported
     */
    public Optional<Jid> channelJid() {
        return Optional.ofNullable(channelJid);
    }

    /**
     * Returns the reported content descriptor.
     *
     * @return an {@link Optional} carrying the descriptor, or empty when
     *         not reported
     */
    public Optional<ReportedContent> reportedContent() {
        return Optional.ofNullable(reportedContent);
    }

    /**
     * Returns the associated appeal record, when filed.
     *
     * @return an {@link Optional} carrying the appeal record, or empty
     *         when no appeal has been filed
     */
    public Optional<NewsletterReportAppeal> appeal() {
        return Optional.ofNullable(appeal);
    }

    /**
     * Returns whether this report equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterReport}
     *         carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterReport that
                && Objects.equals(reportId, that.reportId)
                && Objects.equals(status, that.status)
                && Objects.equals(creationTime, that.creationTime)
                && Objects.equals(lastUpdateTime, that.lastUpdateTime)
                && Objects.equals(channelName, that.channelName)
                && Objects.equals(channelJid, that.channelJid)
                && Objects.equals(reportedContent, that.reportedContent)
                && Objects.equals(appeal, that.appeal);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(reportId, status, creationTime, lastUpdateTime, channelName, channelJid, reportedContent, appeal);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "NewsletterReport[reportId=" + reportId +
                ", status=" + status +
                ", channelJid=" + channelJid + ']';
    }

    /**
     * Describes the content that has been reported.
     *
     * <p>The relay reports content under one of three discriminated
     * inline fragments. Cobalt collapses them into a single descriptor
     * that callers inspect through {@link #kind()}; the supporting
     * scalars are exposed as Optionals because they are present only on
     * the variants that carry them.
     */
    @ProtobufMessage
    public static final class ReportedContent {
        /**
         * The kind of reported content.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        Kind kind;

        /**
         * The server-assigned identifier of the reported message, when
         * the kind is {@link Kind#CHANNEL_MESSAGE} or
         * {@link Kind#QUESTION_RESPONSE}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String serverMessageId;

        /**
         * The server-assigned identifier of the reported status, when
         * the kind is {@link Kind#STATUS}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String serverStatusId;

        /**
         * The server-assigned identifier of the reported question
         * response, when the kind is {@link Kind#QUESTION_RESPONSE}.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String serverResponseId;

        /**
         * The push-name of the user that submitted the reported question
         * response, when the kind is {@link Kind#QUESTION_RESPONSE}.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String responderName;

        /**
         * Constructs a new {@code ReportedContent}. Invoked by the
         * generated protobuf deserializer and by the converters that
         * adapt wire responses into the domain model.
         *
         * @param kind             the content kind, may be {@code null}
         * @param serverMessageId  the message identifier, may be {@code null}
         * @param serverStatusId   the status identifier, may be {@code null}
         * @param serverResponseId the response identifier, may be {@code null}
         * @param responderName    the responder display name, may be {@code null}
         */
        ReportedContent(Kind kind, String serverMessageId, String serverStatusId, String serverResponseId, String responderName) {
            this.kind = kind;
            this.serverMessageId = serverMessageId;
            this.serverStatusId = serverStatusId;
            this.serverResponseId = serverResponseId;
            this.responderName = responderName;
        }

        /**
         * Returns the kind of reported content.
         *
         * @return an {@link Optional} carrying the kind, or empty when
         *         the relay did not classify the content
         */
        public Optional<Kind> kind() {
            return Optional.ofNullable(kind);
        }

        /**
         * Returns the server-assigned identifier of the reported message.
         *
         * @return an {@link Optional} carrying the identifier, or empty
         *         when not present on this variant
         */
        public Optional<String> serverMessageId() {
            return Optional.ofNullable(serverMessageId);
        }

        /**
         * Returns the server-assigned identifier of the reported status.
         *
         * @return an {@link Optional} carrying the identifier, or empty
         *         when not present on this variant
         */
        public Optional<String> serverStatusId() {
            return Optional.ofNullable(serverStatusId);
        }

        /**
         * Returns the server-assigned identifier of the reported
         * question response.
         *
         * @return an {@link Optional} carrying the identifier, or empty
         *         when not present on this variant
         */
        public Optional<String> serverResponseId() {
            return Optional.ofNullable(serverResponseId);
        }

        /**
         * Returns the push-name of the user that submitted the reported
         * question response.
         *
         * @return an {@link Optional} carrying the responder name, or
         *         empty when not present on this variant
         */
        public Optional<String> responderName() {
            return Optional.ofNullable(responderName);
        }

        /**
         * Returns whether this descriptor equals the supplied object.
         *
         * @param o the object to compare against
         * @return {@code true} if {@code o} is a {@code ReportedContent}
         *         carrying equal fields
         */
        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof ReportedContent that
                    && kind == that.kind
                    && Objects.equals(serverMessageId, that.serverMessageId)
                    && Objects.equals(serverStatusId, that.serverStatusId)
                    && Objects.equals(serverResponseId, that.serverResponseId)
                    && Objects.equals(responderName, that.responderName);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(kind, serverMessageId, serverStatusId, serverResponseId, responderName);
        }

        /**
         * Returns a debug-oriented string representation.
         *
         * @return a human-readable string
         */
        @Override
        public String toString() {
            return "ReportedContent[kind=" + kind +
                    ", serverMessageId=" + serverMessageId +
                    ", serverStatusId=" + serverStatusId +
                    ", serverResponseId=" + serverResponseId + ']';
        }

        /**
         * Discriminates the kind of content a moderation report was filed
         * against.
         */
        @ProtobufEnum
        public enum Kind {
            /**
             * The reported content is a regular channel post.
             */
            CHANNEL_MESSAGE(0),

            /**
             * The reported content is a channel status update.
             */
            STATUS(1),

            /**
             * The reported content is a response to a question post.
             */
            QUESTION_RESPONSE(2);

            /**
             * The protobuf wire index associated with this constant.
             */
            final int index;

            /**
             * Constructs a new enum constant bound to the supplied
             * protobuf wire index.
             *
             * @param index the protobuf wire index
             */
            Kind(@ProtobufEnumIndex int index) {
                this.index = index;
            }
        }
    }
}
