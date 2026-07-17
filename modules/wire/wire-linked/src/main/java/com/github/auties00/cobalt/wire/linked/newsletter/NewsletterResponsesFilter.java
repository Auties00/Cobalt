package com.github.auties00.cobalt.wire.linked.newsletter;

/**
 * Optional filter for {@code LinkedWhatsAppClient.queryNewsletterResponses} —
 * narrows the response slice to a single contributor cohort. At most
 * one filter is applied per request; {@code null} on the call site
 * returns responses from every contributor.
 */
public sealed interface NewsletterResponsesFilter permits NewsletterResponsesFilter.Contacts, NewsletterResponsesFilter.Replied {
    /**
     * Returns the singleton {@link Contacts} filter.
     *
     * @return the contacts filter
     */
    static Contacts contacts() {
        return Contacts.INSTANCE;
    }

    /**
     * Returns the singleton {@link Replied} filter.
     *
     * @return the replied filter
     */
    static Replied replied() {
        return Replied.INSTANCE;
    }

    /**
     * Filters the response slice to entries authored by the user's
     * address-book contacts.
     */
    record Contacts() implements NewsletterResponsesFilter {
        /**
         * Shared singleton.
         */
        public static final Contacts INSTANCE = new Contacts();
    }

    /**
     * Filters the response slice to entries the question owner has
     * already explicitly replied to.
     */
    record Replied() implements NewsletterResponsesFilter {
        /**
         * Shared singleton.
         */
        public static final Replied INSTANCE = new Replied();
    }
}
