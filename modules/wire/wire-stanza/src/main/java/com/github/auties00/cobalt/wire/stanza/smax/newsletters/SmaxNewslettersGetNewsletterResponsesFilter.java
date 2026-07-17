package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Selects the at-most-one filter applied to a {@link SmaxNewslettersGetNewsletterResponsesRequest}.
 *
 * <p>Narrows the question-responses slice to either address-book contacts ({@link Contacts}) or
 * entries the question owner has explicitly replied to ({@link Replied}). The two variants are
 * mutually exclusive and the parameter remains optional; omit it entirely to disable filtering.</p>
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersContactsOrRepliedFilterMixinMixinGroup")
public sealed interface SmaxNewslettersGetNewsletterResponsesFilter permits SmaxNewslettersGetNewsletterResponsesFilter.Contacts, SmaxNewslettersGetNewsletterResponsesFilter.Replied {

    /**
     * Restricts the slice to address-book contacts.
     *
     * <p>Materialised on the wire as an empty {@code <contacts/>} marker inside the
     * {@code <filters>} block.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersContactsFilterMixinMixin")
    final class Contacts implements SmaxNewslettersGetNewsletterResponsesFilter {
        /**
         * Constructs the contacts filter marker.
         *
         * <p>Carries no data; the variant identity alone selects the filter on the wire.</p>
         */
        public Contacts() {
        }

        /**
         * Compares two markers for type identity.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link Contacts}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a stable hash code for the marker.
         *
         * @return the class's {@link Object#hashCode()}
         */
        @Override
        public int hashCode() {
            return Contacts.class.hashCode();
        }

        /**
         * Returns a debug representation of the marker.
         *
         * @return a record-like rendering of this variant
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterResponsesFilter.Contacts[]";
        }
    }

    /**
     * Restricts the slice to entries the question owner has explicitly replied to.
     *
     * <p>Materialised on the wire as an empty {@code <replied/>} marker inside the
     * {@code <filters>} block.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersRepliedFilterMixinMixin")
    final class Replied implements SmaxNewslettersGetNewsletterResponsesFilter {
        /**
         * Constructs the replied filter marker.
         *
         * <p>Carries no data; the variant identity alone selects the filter on the wire.</p>
         */
        public Replied() {
        }

        /**
         * Compares two markers for type identity.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link Replied}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a stable hash code for the marker.
         *
         * @return the class's {@link Object#hashCode()}
         */
        @Override
        public int hashCode() {
            return Replied.class.hashCode();
        }

        /**
         * Returns a debug representation of the marker.
         *
         * @return a record-like rendering of this variant
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterResponsesFilter.Replied[]";
        }
    }
}
