package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single appeal record filed against a newsletter
 * enforcement decision.
 *
 * <p>When the WhatsApp moderation pipeline takes an enforcement action
 * (a content removal, a profile-picture deletion, a suspension, and so
 * on) it records the decision under a {@code report_id}. The newsletter
 * owner may then file an appeal contesting the decision: this type
 * captures the resulting record, including the user-supplied
 * justification, the relay-assigned appeal identifier, the appeal
 * lifecycle state and the moment at which it was filed.
 */
@ProtobufMessage
public final class NewsletterReportAppeal {
    /**
     * The relay-assigned identifier of the appeal record itself.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String appealId;

    /**
     * The identifier of the report whose enforcement decision is being
     * contested.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String reportId;

    /**
     * The lifecycle state of this appeal (for example {@code "PENDING"},
     * {@code "ACCEPTED"} or {@code "REJECTED"}).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String state;

    /**
     * The free-form justification supplied by the user when filing the
     * appeal.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String reason;

    /**
     * The instant at which this appeal was filed.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant creationTime;

    /**
     * Constructs a new {@code NewsletterReportAppeal}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param appealId     the relay-assigned appeal identifier, may be
     *                     {@code null}
     * @param reportId     the identifier of the contested report, may be
     *                     {@code null}
     * @param state        the lifecycle state, may be {@code null}
     * @param reason       the user-supplied justification, may be {@code null}
     * @param creationTime the moment the appeal was filed, may be {@code null}
     */
    NewsletterReportAppeal(String appealId, String reportId, String state, String reason, Instant creationTime) {
        this.appealId = appealId;
        this.reportId = reportId;
        this.state = state;
        this.reason = reason;
        this.creationTime = creationTime;
    }

    /**
     * Returns the relay-assigned identifier of this appeal record.
     *
     * @return an {@link Optional} carrying the appeal identifier, or
     *         empty when not reported
     */
    public Optional<String> appealId() {
        return Optional.ofNullable(appealId);
    }

    /**
     * Returns the identifier of the report whose enforcement decision is
     * being contested.
     *
     * @return an {@link Optional} carrying the report identifier, or
     *         empty when not reported
     */
    public Optional<String> reportId() {
        return Optional.ofNullable(reportId);
    }

    /**
     * Returns the lifecycle state of this appeal.
     *
     * @return an {@link Optional} carrying the state, or empty when not
     *         reported
     */
    public Optional<String> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Returns the free-form justification supplied when filing this
     * appeal.
     *
     * @return an {@link Optional} carrying the reason, or empty when not
     *         reported
     */
    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    /**
     * Returns the instant at which this appeal was filed.
     *
     * @return an {@link Optional} carrying the creation instant, or empty
     *         when not reported
     */
    public Optional<Instant> creationTime() {
        return Optional.ofNullable(creationTime);
    }

    /**
     * Returns whether this appeal record equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterReportAppeal} carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterReportAppeal that
                && Objects.equals(appealId, that.appealId)
                && Objects.equals(reportId, that.reportId)
                && Objects.equals(state, that.state)
                && Objects.equals(reason, that.reason)
                && Objects.equals(creationTime, that.creationTime);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(appealId, reportId, state, reason, creationTime);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "NewsletterReportAppeal[appealId=" + appealId +
                ", reportId=" + reportId +
                ", state=" + state + ']';
    }
}
