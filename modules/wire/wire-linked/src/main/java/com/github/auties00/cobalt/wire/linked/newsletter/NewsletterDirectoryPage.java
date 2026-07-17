package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single page of newsletter directory results.
 *
 * <p>The directory queries — recommended, similar, list-by-view, and
 * search — return their results in pages of varying size. Each page
 * carries the result rows together with a forward cursor that callers
 * feed back to fetch the next page; the cursor is {@link Optional}
 * because the relay omits it when there is no further page to fetch.
 *
 * <p>Backward pagination is intentionally not exposed: WhatsApp Web
 * never paginates the directory backwards in any user-visible flow, and
 * the relay's cursor protocol is inherently one-way.
 */
@ProtobufMessage
public final class NewsletterDirectoryPage {
    /**
     * The directory entries returned on this page.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<NewsletterDirectoryEntry> entries;

    /**
     * The opaque cursor token to be supplied on the next call to fetch
     * the following page.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String nextCursor;

    /**
     * Constructs a new {@code NewsletterDirectoryPage}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param entries    the directory entries on this page; defaulted to
     *                   an empty list when {@code null}
     * @param nextCursor the forward pagination cursor, may be {@code null}
     *                   when no further page is available
     */
    NewsletterDirectoryPage(List<NewsletterDirectoryEntry> entries, String nextCursor) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        this.nextCursor = nextCursor;
    }

    /**
     * Returns the directory entries returned on this page.
     *
     * @return an unmodifiable list of entries, never {@code null}
     */
    public List<NewsletterDirectoryEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Returns the opaque forward cursor token to be supplied on the next
     * call to fetch the following page.
     *
     * @return an {@link Optional} carrying the cursor, or empty when no
     *         further page is available
     */
    public Optional<String> nextCursor() {
        return Optional.ofNullable(nextCursor);
    }

    /**
     * Returns whether this page equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterDirectoryPage}
     *         carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterDirectoryPage that
                && Objects.equals(entries, that.entries)
                && Objects.equals(nextCursor, that.nextCursor);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(entries, nextCursor);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string listing the entry count and cursor
     */
    @Override
    public String toString() {
        return "NewsletterDirectoryPage[entries=" + entries.size() +
                ", nextCursor=" + nextCursor + ']';
    }
}
