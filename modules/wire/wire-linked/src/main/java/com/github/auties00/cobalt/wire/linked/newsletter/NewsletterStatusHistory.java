package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a slice of status envelopes returned by a newsletter
 * back-fill or delta query.
 *
 * <p>The status-history page (forward back-fill) and status-update
 * delta (incremental catch-up) share this shape. The
 * {@link #highWaterMark()} accessor exposes the relay-reported
 * "as of" timestamp so callers can drive the next delta query
 * without losing or re-fetching already-seen entries.
 */
@ProtobufMessage
public final class NewsletterStatusHistory {
    /**
     * The relay-reported "as of" timestamp for this slice, used as
     * the cursor for the next delta call.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant highWaterMark;

    /**
     * The status envelopes returned in this slice, in server order.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<NewsletterStatusHistoryEntry> entries;

    /**
     * Constructs a new {@code NewsletterStatusHistory}. Invoked by
     * the generated protobuf deserializer and by the converters that
     * adapt wire responses into the domain model.
     *
     * @param highWaterMark the optional "as of" timestamp; may be
     *                      {@code null}
     * @param entries       the status entries; defaulted to an empty
     *                      list when {@code null}
     */
    NewsletterStatusHistory(Instant highWaterMark, List<NewsletterStatusHistoryEntry> entries) {
        this.highWaterMark = highWaterMark;
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    /**
     * Returns the relay-reported "as of" timestamp for this slice.
     *
     * @return an {@link Optional} carrying the timestamp, or empty
     *         when the relay omitted it
     */
    public Optional<Instant> highWaterMark() {
        return Optional.ofNullable(highWaterMark);
    }

    /**
     * Returns the status envelopes returned in this slice.
     *
     * @return an unmodifiable list of entries, never {@code null}
     */
    public List<NewsletterStatusHistoryEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Returns whether this slice equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterStatusHistory} carrying equal field
     *         values
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterStatusHistory that
                && Objects.equals(highWaterMark, that.highWaterMark)
                && Objects.equals(entries, that.entries);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(highWaterMark, entries);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string listing the entry count and
     *         high-water mark
     */
    @Override
    public String toString() {
        return "NewsletterStatusHistory[" +
                "highWaterMark=" + highWaterMark +
                ", entries=" + entries.size() +
                ']';
    }
}
