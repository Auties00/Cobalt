package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Polls the relay for a delta of newsletter status updates (view counts and emoji reactions per
 * status post).
 *
 * <p>The relay echoes the matching {@link SmaxNewslettersGetNewsletterStatusUpdatesResponse}. The
 * resulting IQ has shape:
 * {@snippet :
 *     <iq xmlns="newsletter" type="get" to="<newsletterJid>">
 *         <status_updates count="100" since="1700000000" after="99"/>
 *     </iq>
 * }</p>
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersGetNewsletterStatusUpdatesRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersNewsletterIQGetRequestMixin")
public final class SmaxNewslettersGetNewsletterStatusUpdatesRequest implements SmaxStanza.Request {
    /**
     * The newsletter {@link Jid} being polled.
     */
    private final Jid newsletterJid;

    /**
     * The cap on returned {@code <status>} entries per round-trip.
     */
    private final int count;

    /**
     * The optional unix-second floor.
     */
    private final Long since;

    /**
     * The required pagination cursor.
     */
    private final SmaxNewslettersGetNewsletterStatusUpdatesDirection direction;

    /**
     * Constructs a new request.
     *
     * <p>A periodic full-history poll passes
     * {@link SmaxNewslettersGetNewsletterStatusUpdatesDirection.After} at pivot {@code 99} (the
     * minimum allowed server-id) and a {@code count} of {@code 100} to pull the full known-status
     * history, leaving {@code since} {@code null}.</p>
     *
     * @param newsletterJid the newsletter {@link Jid}; never {@code null}
     * @param count         the per-call cap; must be non-negative
     * @param since         the optional unix-second floor; may be {@code null}
     * @param direction     the cursor; never {@code null}
     * @throws NullPointerException if {@code newsletterJid} or {@code direction} is {@code null}
     */
    public SmaxNewslettersGetNewsletterStatusUpdatesRequest(Jid newsletterJid, int count, Long since, SmaxNewslettersGetNewsletterStatusUpdatesDirection direction) {
        this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        this.count = count;
        this.since = since;
        this.direction = Objects.requireNonNull(direction, "direction cannot be null");
    }

    /**
     * Returns the newsletter {@link Jid} being polled.
     *
     * @return the {@link Jid}; never {@code null}
     */
    public Jid newsletterJid() {
        return newsletterJid;
    }

    /**
     * Returns the per-call cap on returned entries.
     *
     * @return the cap
     */
    public int count() {
        return count;
    }

    /**
     * Returns the optional unix-second floor.
     *
     * @return an {@link Optional} carrying the floor, or empty when the request asks for the full delta
     */
    public Optional<Long> since() {
        return Optional.ofNullable(since);
    }

    /**
     * Returns the required pagination cursor.
     *
     * @return the cursor; never {@code null}
     */
    public SmaxNewslettersGetNewsletterStatusUpdatesDirection direction() {
        return direction;
    }

    /**
     * Builds the outbound {@code <iq>} stanza carrying the {@code <status_updates>} payload.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <status_updates>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutNewslettersGetNewsletterStatusUpdatesRequest",
            exports = "makeGetNewsletterStatusUpdatesRequest",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var updatesBuilder = new StanzaBuilder()
                .description("status_updates")
                .attribute("count", count);
        if (since != null) {
            updatesBuilder.attribute("since", since);
        }
        switch (direction) {
            case SmaxNewslettersGetNewsletterStatusUpdatesDirection.Before before -> updatesBuilder.attribute("before", before.pivot());
            case SmaxNewslettersGetNewsletterStatusUpdatesDirection.After after -> updatesBuilder.attribute("after", after.pivot());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "newsletter")
                .attribute("to", newsletterJid)
                .attribute("type", "get")
                .content(updatesBuilder.build());
    }

    /**
     * Compares two requests for value equality on every field.
     *
     * @param obj the reference object to compare against
     * @return {@code true} when {@code obj} is a request with equal field values
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewslettersGetNewsletterStatusUpdatesRequest) obj;
        return this.count == that.count
                && Objects.equals(this.newsletterJid, that.newsletterJid)
                && Objects.equals(this.since, that.since)
                && Objects.equals(this.direction, that.direction);
    }

    /**
     * Returns the hash code derived from every field.
     *
     * @return the combined hash of every field
     */
    @Override
    public int hashCode() {
        return Objects.hash(newsletterJid, count, since, direction);
    }

    /**
     * Returns a debug representation including every field.
     *
     * @return a record-like rendering of this request
     */
    @Override
    public String toString() {
        return "SmaxNewslettersGetNewsletterStatusUpdatesRequest[newsletterJid="
                + newsletterJid + ", count=" + count
                + ", since=" + since + ", direction=" + direction + ']';
    }
}
