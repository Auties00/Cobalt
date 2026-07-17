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
 * Polls the relay for a delta of newsletter message updates.
 *
 * <p>The relay echoes the matching {@link SmaxNewslettersGetNewsletterMessageUpdatesResponse}.
 * The resulting IQ has shape:
 * {@snippet :
 *     <iq xmlns="newsletter" type="get" to="<newsletterJid>">
 *         <message_updates count="50" since="1700000000" after="42"/>
 *     </iq>
 * }</p>
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersGetNewsletterMessageUpdatesRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersNewsletterIQGetRequestMixin")
public final class SmaxNewslettersGetNewsletterMessageUpdatesRequest implements SmaxStanza.Request {
    /**
     * The newsletter {@link Jid} being polled; routed verbatim into the IQ's {@code to} attribute.
     */
    private final Jid newsletterJid;

    /**
     * The cap on returned {@code <message>} entries per round-trip.
     */
    private final int count;

    /**
     * The optional unix-second floor; when present the relay only returns updates applied at or
     * after this timestamp.
     */
    private final Long since;

    /**
     * The required pagination cursor, either
     * {@link SmaxNewslettersGetNewsletterMessageUpdatesDirection.Before} or
     * {@link SmaxNewslettersGetNewsletterMessageUpdatesDirection.After}.
     */
    private final SmaxNewslettersGetNewsletterMessageUpdatesDirection direction;

    /**
     * Constructs a new request.
     *
     * <p>A {@code null} {@code since} requests the full delta regardless of timestamp. The cursor
     * is mandatory because the relay rejects {@code <message_updates>} elements that lack both
     * {@code before} and {@code after}.</p>
     *
     * @param newsletterJid the newsletter {@link Jid} being polled; never {@code null}
     * @param count         the per-call cap on returned entries; must be non-negative
     * @param since         the optional unix-second floor; may be {@code null}
     * @param direction     the pagination cursor; never {@code null}
     * @throws NullPointerException if {@code newsletterJid} or {@code direction} is {@code null}
     */
    public SmaxNewslettersGetNewsletterMessageUpdatesRequest(Jid newsletterJid, int count, Long since, SmaxNewslettersGetNewsletterMessageUpdatesDirection direction) {
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
    public SmaxNewslettersGetNewsletterMessageUpdatesDirection direction() {
        return direction;
    }

    /**
     * Builds the outbound {@code <iq>} stanza carrying the {@code <message_updates>} payload.
     *
     * <p>The returned {@link StanzaBuilder} is not yet built; the Smax pipeline finalises it just
     * before transmission so the stanza id can be assigned by the central stanza-id allocator.</p>
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <message_updates>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutNewslettersGetNewsletterMessageUpdatesRequest",
            exports = "makeGetNewsletterMessageUpdatesRequest",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var updatesBuilder = new StanzaBuilder()
                .description("message_updates")
                .attribute("count", count);
        if (since != null) {
            updatesBuilder.attribute("since", since);
        }
        switch (direction) {
            case SmaxNewslettersGetNewsletterMessageUpdatesDirection.Before before -> updatesBuilder.attribute("before", before.pivot());
            case SmaxNewslettersGetNewsletterMessageUpdatesDirection.After after -> updatesBuilder.attribute("after", after.pivot());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "newsletter")
                .attribute("to", newsletterJid)
                .attribute("type", "get")
                .content(updatesBuilder.build());
    }

    /**
     * Compares two requests for value equality on all fields.
     *
     * @param obj the reference object to compare against
     * @return {@code true} when {@code obj} is a request carrying equal {@link #newsletterJid()},
     *         {@link #count()}, {@link #since()}, and {@link #direction()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewslettersGetNewsletterMessageUpdatesRequest) obj;
        return this.count == that.count
                && Objects.equals(this.newsletterJid, that.newsletterJid)
                && Objects.equals(this.since, that.since)
                && Objects.equals(this.direction, that.direction);
    }

    /**
     * Returns the hash code derived from all fields.
     *
     * @return the combined hash of {@link #newsletterJid()}, {@link #count()}, {@link #since()},
     *         and {@link #direction()}
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
        return "SmaxNewslettersGetNewsletterMessageUpdatesRequest[newsletterJid="
                + newsletterJid + ", count=" + count
                + ", since=" + since + ", direction=" + direction + ']';
    }
}
