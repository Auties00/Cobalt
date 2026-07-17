package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Represents the sealed disjunction over the optional pagination cursor
 * of a {@link SmaxNewslettersGetNewsletterStatusesRequest}.
 * Selecting {@link Before} walks backwards from a pivot server-id and
 * {@link After} walks forwards; omitting the cursor altogether on the
 * request requests the latest slice.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersStatusDirections")
public sealed interface SmaxNewslettersGetNewsletterStatusesDirection permits SmaxNewslettersGetNewsletterStatusesDirection.Before, SmaxNewslettersGetNewsletterStatusesDirection.After {

    /**
     * Represents the variant that walks backwards from a pivot server-id.
     * Selects statuses with server-ids strictly less than {@link #pivot()},
     * materialised as the {@code before} attribute on the wire
     * {@code <statuses>} element.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersStatusBeforeMixinMixin")
    final class Before implements SmaxNewslettersGetNewsletterStatusesDirection {
        /**
         * Holds the server-id pivot below which statuses are returned.
         */
        private final long pivot;

        /**
         * Constructs a backward-walking cursor at the given pivot.
         *
         * @param pivot the server-id pivot; the relay returns statuses strictly less than this value
         */
        public Before(long pivot) {
            this.pivot = pivot;
        }

        /**
         * Returns the server-id pivot for this cursor.
         *
         * @return the pivot below which statuses are fetched
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
            return "SmaxNewslettersGetNewsletterStatusesDirection.Before[pivot=" + pivot + ']';
        }
    }

    /**
     * Represents the variant that walks forwards from a pivot server-id.
     * Selects statuses with server-ids strictly greater than {@link #pivot()},
     * materialised as the {@code after} attribute on the wire
     * {@code <statuses>} element.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersStatusAfterMixinMixin")
    final class After implements SmaxNewslettersGetNewsletterStatusesDirection {
        /**
         * Holds the server-id pivot above which statuses are returned.
         */
        private final long pivot;

        /**
         * Constructs a forward-walking cursor at the given pivot.
         *
         * @param pivot the server-id pivot; the relay returns statuses strictly greater than this value
         */
        public After(long pivot) {
            this.pivot = pivot;
        }

        /**
         * Returns the server-id pivot for this cursor.
         *
         * @return the pivot above which statuses are fetched
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
            return "SmaxNewslettersGetNewsletterStatusesDirection.After[pivot=" + pivot + ']';
        }
    }
}
