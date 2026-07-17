package com.github.auties00.cobalt.wire.linked.newsletter;

/**
 * Pagination cursor for the windowed newsletter history queries
 * ({@code queryNewsletterMessages}, {@code queryNewsletterMessageUpdates},
 * {@code queryNewsletterStatuses}, {@code queryNewsletterStatusUpdates}).
 *
 * <p>Sealed disjunction over a server-id pivot: either
 * {@link Before before} a server-id, {@link After after} a server-id, or
 * {@code null} on the call site for the latest slice.
 */
public sealed interface NewsletterHistoryDirection permits NewsletterHistoryDirection.Before, NewsletterHistoryDirection.After {
    /**
     * Returns the server-id pivot carried by this cursor.
     *
     * @return the pivot
     */
    long pivot();

    /**
     * Returns a {@code before} cursor at the given pivot — fetch entries
     * with server-ids strictly less than the pivot.
     *
     * @param pivot the server-id pivot
     * @return the cursor
     */
    static Before before(long pivot) {
        return new Before(pivot);
    }

    /**
     * Returns an {@code after} cursor at the given pivot — fetch entries
     * with server-ids strictly greater than the pivot.
     *
     * @param pivot the server-id pivot
     * @return the cursor
     */
    static After after(long pivot) {
        return new After(pivot);
    }

    /**
     * The {@code before} cursor.
     *
     * @param pivot the server-id pivot
     */
    record Before(long pivot) implements NewsletterHistoryDirection { }

    /**
     * The {@code after} cursor.
     *
     * @param pivot the server-id pivot
     */
    record After(long pivot) implements NewsletterHistoryDirection { }
}
