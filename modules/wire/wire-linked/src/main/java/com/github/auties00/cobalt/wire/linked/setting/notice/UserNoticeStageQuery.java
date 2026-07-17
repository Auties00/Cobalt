package com.github.auties00.cobalt.wire.linked.setting.notice;

import java.util.Objects;

/**
 * Carries one per-disclosure query entry for the relay's
 * {@code get_disclosure_stage_by_ids} fetch.
 *
 * <p>Pairs the disclosure id to query with the client-side timestamp
 * the request carries on the wire. The relay routes both values to
 * its disclosure-stage cache to compute the per-id stage reply.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it travels
 * from caller code to the wire-encoder and is never serialized as
 * protobuf itself.
 */
public final class UserNoticeStageQuery {
    /**
     * The disclosure id to query.
     */
    private final long disclosureId;

    /**
     * The client-side timestamp in seconds.
     */
    private final long timestampSeconds;

    /**
     * Constructs a new query entry.
     *
     * @param disclosureId     the disclosure id
     * @param timestampSeconds the client-side timestamp in seconds
     */
    public UserNoticeStageQuery(long disclosureId, long timestampSeconds) {
        this.disclosureId = disclosureId;
        this.timestampSeconds = timestampSeconds;
    }

    /**
     * Returns the disclosure id.
     *
     * @return the id
     */
    public long disclosureId() {
        return disclosureId;
    }

    /**
     * Returns the client-side timestamp.
     *
     * @return the timestamp in seconds
     */
    public long timestampSeconds() {
        return timestampSeconds;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (UserNoticeStageQuery) obj;
        return this.disclosureId == that.disclosureId
                && this.timestampSeconds == that.timestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(disclosureId, timestampSeconds);
    }

    @Override
    public String toString() {
        return "UserNoticeStageQuery[disclosureId=" + disclosureId
                + ", timestampSeconds=" + timestampSeconds + ']';
    }
}
