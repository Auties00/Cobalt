package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Selects the pagination cursor of a {@link SmaxNewslettersGetNewsletterMessageUpdatesRequest}.
 *
 * <p>Exactly one of {@link Before} or {@link After} must be supplied when polling the
 * message-updates delta for a single newsletter; the relay rejects a {@code <message_updates>}
 * element that carries both or neither.</p>
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersMessageUpdatesBeforeOrAfterMixinMixinGroup")
public sealed interface SmaxNewslettersGetNewsletterMessageUpdatesDirection permits SmaxNewslettersGetNewsletterMessageUpdatesDirection.Before, SmaxNewslettersGetNewsletterMessageUpdatesDirection.After {

    /**
     * Selects message updates with server-ids strictly below a pivot.
     *
     * <p>Materialised as the {@code before} attribute on the wire {@code <message_updates>}
     * element; used to paginate older message updates one slice at a time.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersMessageUpdatesBeforeMixinMixin")
    final class Before implements SmaxNewslettersGetNewsletterMessageUpdatesDirection {
        /**
         * The server-id pivot below which message updates are returned.
         */
        private final long pivot;

        /**
         * Constructs a backward-walking cursor at the given pivot.
         *
         * @param pivot the server-id pivot; the relay returns updates strictly less than this value
         */
        public Before(long pivot) {
            this.pivot = pivot;
        }

        /**
         * Returns the server-id pivot for this cursor.
         *
         * @return the pivot below which updates are fetched
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
            return "SmaxNewslettersGetNewsletterMessageUpdatesDirection.Before[pivot=" + pivot + ']';
        }
    }

    /**
     * Selects message updates with server-ids strictly above a pivot.
     *
     * <p>Materialised as the {@code after} attribute on the wire {@code <message_updates>}
     * element; this is the cursor a forward delta-sync poll uses to fetch updates past its
     * last-seen server-id.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersMessageUpdatesAfterMixinMixin")
    final class After implements SmaxNewslettersGetNewsletterMessageUpdatesDirection {
        /**
         * The server-id pivot above which message updates are returned.
         */
        private final long pivot;

        /**
         * Constructs a forward-walking cursor at the given pivot.
         *
         * @param pivot the server-id pivot; the relay returns updates strictly greater than this value
         */
        public After(long pivot) {
            this.pivot = pivot;
        }

        /**
         * Returns the server-id pivot for this cursor.
         *
         * @return the pivot above which updates are fetched
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
            return "SmaxNewslettersGetNewsletterMessageUpdatesDirection.After[pivot=" + pivot + ']';
        }
    }
}
