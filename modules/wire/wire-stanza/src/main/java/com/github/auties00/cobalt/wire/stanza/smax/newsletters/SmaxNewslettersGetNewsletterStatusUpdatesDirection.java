package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Selects the pagination cursor of a {@link SmaxNewslettersGetNewsletterStatusUpdatesRequest}.
 *
 * <p>Exactly one of {@link Before} or {@link After} must be supplied when polling the status-updates
 * delta (view counts and reactions) for a single newsletter. A poll that wants the complete
 * known-status history in one round-trip uses {@link After} at a server-id floor of {@code 99}, the
 * lowest allowed newsletter server-id.</p>
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersStatusUpdatesBeforeOrAfterMixinMixinGroup")
public sealed interface SmaxNewslettersGetNewsletterStatusUpdatesDirection permits SmaxNewslettersGetNewsletterStatusUpdatesDirection.Before, SmaxNewslettersGetNewsletterStatusUpdatesDirection.After {

    /**
     * Selects status updates with server-ids strictly below a pivot.
     *
     * <p>Materialised as the {@code before} attribute on the wire {@code <status_updates>} element.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersStatusUpdatesBeforeMixinMixin")
    final class Before implements SmaxNewslettersGetNewsletterStatusUpdatesDirection {
        /**
         * The server-id pivot below which status updates are returned.
         */
        private final long pivot;

        /**
         * Constructs a backward-walking cursor at the given pivot.
         *
         * @param pivot the server-id pivot; the relay returns status updates strictly less than this value
         */
        public Before(long pivot) {
            this.pivot = pivot;
        }

        /**
         * Returns the server-id pivot for this cursor.
         *
         * @return the pivot below which status updates are fetched
         */
        public long pivot() {
            return pivot;
        }

        /**
         * Compares two cursors for value equality on {@link #pivot()}.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link Before} carrying the same pivot
         */
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Before that && this.pivot == that.pivot;
        }

        /**
         * Returns the hash code derived from {@link #pivot()}.
         *
         * @return the {@link Long#hashCode(long)} of {@link #pivot()}
         */
        @Override
        public int hashCode() {
            return Long.hashCode(pivot);
        }

        /**
         * Returns a debug representation including the pivot.
         *
         * @return a record-like rendering of this cursor
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterStatusUpdatesDirection.Before[pivot=" + pivot + ']';
        }
    }

    /**
     * Selects status updates with server-ids strictly above a pivot.
     *
     * <p>Materialised as the {@code after} attribute on the wire {@code <status_updates>} element;
     * this is the cursor a forward status-updates poll uses to fetch updates past its last-seen
     * server-id, and at a pivot of {@code 99} pulls the full known-status history in one round-trip.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersStatusUpdatesAfterMixinMixin")
    final class After implements SmaxNewslettersGetNewsletterStatusUpdatesDirection {
        /**
         * The server-id pivot above which status updates are returned.
         */
        private final long pivot;

        /**
         * Constructs a forward-walking cursor at the given pivot.
         *
         * @param pivot the server-id pivot; the relay returns status updates strictly greater than this value
         */
        public After(long pivot) {
            this.pivot = pivot;
        }

        /**
         * Returns the server-id pivot for this cursor.
         *
         * @return the pivot above which status updates are fetched
         */
        public long pivot() {
            return pivot;
        }

        /**
         * Compares two cursors for value equality on {@link #pivot()}.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is an {@link After} carrying the same pivot
         */
        @Override
        public boolean equals(Object obj) {
            return obj instanceof After that && this.pivot == that.pivot;
        }

        /**
         * Returns the hash code derived from {@link #pivot()}.
         *
         * @return the {@link Long#hashCode(long)} of {@link #pivot()}
         */
        @Override
        public int hashCode() {
            return Long.hashCode(pivot);
        }

        /**
         * Returns a debug representation including the pivot.
         *
         * @return a record-like rendering of this cursor
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterStatusUpdatesDirection.After[pivot=" + pivot + ']';
        }
    }
}
