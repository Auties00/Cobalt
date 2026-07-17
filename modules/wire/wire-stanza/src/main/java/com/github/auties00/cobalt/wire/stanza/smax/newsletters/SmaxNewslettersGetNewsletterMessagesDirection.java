package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Selects the optional pagination cursor of a {@link SmaxNewslettersGetNewsletterMessagesRequest}.
 *
 * <p>Used when scrolling a newsletter's message history; omitting the cursor altogether requests
 * the latest slice. {@link Before} walks up into older content, {@link After} walks down into
 * newer content.</p>
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersMessageDirections")
public sealed interface SmaxNewslettersGetNewsletterMessagesDirection permits SmaxNewslettersGetNewsletterMessagesDirection.Before, SmaxNewslettersGetNewsletterMessagesDirection.After {

    /**
     * Selects messages with server-ids strictly below a pivot.
     *
     * <p>Materialised as the {@code before} attribute on the wire {@code <messages>} element; this
     * is the cursor a history-scroll-up walk uses to fetch older messages.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersBeforeMixinMixin")
    final class Before implements SmaxNewslettersGetNewsletterMessagesDirection {
        /**
         * The server-id pivot below which messages are returned.
         */
        private final long pivot;

        /**
         * Constructs a backward-walking cursor at the given pivot.
         *
         * @param pivot the server-id pivot; the relay returns messages strictly less than this value
         */
        public Before(long pivot) {
            this.pivot = pivot;
        }

        /**
         * Returns the server-id pivot for this cursor.
         *
         * @return the pivot below which messages are fetched
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
            return "SmaxNewslettersGetNewsletterMessagesDirection.Before[pivot=" + pivot + ']';
        }
    }

    /**
     * Selects messages with server-ids strictly above a pivot.
     *
     * <p>Materialised as the {@code after} attribute on the wire {@code <messages>} element; used to
     * fetch newsletter messages newer than the highest-known server-id of the previous slice.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersAfterMixinMixin")
    final class After implements SmaxNewslettersGetNewsletterMessagesDirection {
        /**
         * The server-id pivot above which messages are returned.
         */
        private final long pivot;

        /**
         * Constructs a forward-walking cursor at the given pivot.
         *
         * @param pivot the server-id pivot; the relay returns messages strictly greater than this value
         */
        public After(long pivot) {
            this.pivot = pivot;
        }

        /**
         * Returns the server-id pivot for this cursor.
         *
         * @return the pivot above which messages are fetched
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
            return "SmaxNewslettersGetNewsletterMessagesDirection.After[pivot=" + pivot + ']';
        }
    }
}
