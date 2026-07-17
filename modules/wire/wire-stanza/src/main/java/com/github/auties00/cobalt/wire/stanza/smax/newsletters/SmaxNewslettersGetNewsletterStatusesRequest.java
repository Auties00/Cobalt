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
 * Represents the outbound stanza that fetches a slice of a newsletter's
 * status (view-count and reaction) history.
 * The same {@link SmaxNewslettersGetNewsletterMessagesQueryParams}
 * disjunction used for
 * {@link SmaxNewslettersGetNewsletterMessagesRequest} also picks the
 * addressing mode here. The resulting IQ has shape:
 * {@snippet :
 *     <iq xmlns="newsletter" type="get" to="s.whatsapp.net">
 *         <statuses count="100" before="200">
 *             <smax$any type="jid" jid="..." view_role="..."/>
 *         </statuses>
 *     </iq>
 * }
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersGetNewsletterStatusesRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersNewsletterStatusRequestIQPayloadMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersNewsletterStatusRequestPayloadMixin")
public final class SmaxNewslettersGetNewsletterStatusesRequest implements SmaxStanza.Request {
    /**
     * Holds the cap on returned {@code <status>} entries per round-trip.
     */
    private final int count;

    /**
     * Holds the newsletter addressing parameters; either
     * {@link SmaxNewslettersGetNewsletterMessagesQueryParams.ByJid} or
     * {@link SmaxNewslettersGetNewsletterMessagesQueryParams.ByInvite}.
     */
    private final SmaxNewslettersGetNewsletterMessagesQueryParams queryParams;

    /**
     * Holds the optional pagination cursor; {@code null} requests the
     * latest slice.
     */
    private final SmaxNewslettersGetNewsletterStatusesDirection direction;

    /**
     * Constructs a new request.
     *
     * @param count       the per-call cap; must be non-negative
     * @param queryParams the addressing parameters; never {@code null}
     * @param direction   the optional pagination cursor; may be {@code null}
     * @throws NullPointerException if {@code queryParams} is {@code null}
     */
    public SmaxNewslettersGetNewsletterStatusesRequest(int count,
                   SmaxNewslettersGetNewsletterMessagesQueryParams queryParams,
                   SmaxNewslettersGetNewsletterStatusesDirection direction) {
        this.count = count;
        this.queryParams = Objects.requireNonNull(queryParams, "queryParams cannot be null");
        this.direction = direction;
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
     * Returns the newsletter addressing parameters.
     *
     * @return the parameters; never {@code null}
     */
    public SmaxNewslettersGetNewsletterMessagesQueryParams queryParams() {
        return queryParams;
    }

    /**
     * Returns the optional pagination cursor.
     *
     * @return an {@link Optional} carrying the cursor, or empty when requesting the latest slice
     */
    public Optional<SmaxNewslettersGetNewsletterStatusesDirection> direction() {
        return Optional.ofNullable(direction);
    }

    /**
     * Builds the outbound {@code <iq>} stanza carrying the
     * {@code <statuses>} payload.
     * The IQ targets {@link Jid#userServer()} rather than the newsletter
     * JID because the request fetches across the relay's newsletter
     * index; the {@code <smax$any>} child encodes the addressing variant.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <statuses>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutNewslettersGetNewsletterStatusesRequest",
            exports = "makeGetNewsletterStatusesRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var anyBuilder = new StanzaBuilder()
                .description("smax$any");
        switch (queryParams) {
            case SmaxNewslettersGetNewsletterMessagesQueryParams.ByJid byJid -> {
                anyBuilder.attribute("type", "jid");
                anyBuilder.attribute("jid", byJid.newsletterJid());
                byJid.viewRole().ifPresent(role -> anyBuilder.attribute("view_role", role));
            }
            case SmaxNewslettersGetNewsletterMessagesQueryParams.ByInvite byInvite -> {
                anyBuilder.attribute("type", "invite");
                anyBuilder.attribute("key", byInvite.inviteKey());
                byInvite.viewRole().ifPresent(role -> anyBuilder.attribute("view_role", role));
            }
        }
        var statusesBuilder = new StanzaBuilder()
                .description("statuses")
                .attribute("count", count);
        if (direction != null) {
            switch (direction) {
                case SmaxNewslettersGetNewsletterStatusesDirection.Before before -> statusesBuilder.attribute("before", before.pivot());
                case SmaxNewslettersGetNewsletterStatusesDirection.After after -> statusesBuilder.attribute("after", after.pivot());
            }
        }
        statusesBuilder.content(anyBuilder.build());
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "newsletter")
                .attribute("to", Jid.userServer())
                .attribute("type", "get")
                .content(statusesBuilder.build());
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
        var that = (SmaxNewslettersGetNewsletterStatusesRequest) obj;
        return this.count == that.count
                && Objects.equals(this.queryParams, that.queryParams)
                && Objects.equals(this.direction, that.direction);
    }

    /**
     * Returns the hash code derived from every field.
     *
     * @return the combined hash of every field
     */
    @Override
    public int hashCode() {
        return Objects.hash(count, queryParams, direction);
    }

    /**
     * Returns a debug representation including every field.
     *
     * @return a record-like rendering of this request
     */
    @Override
    public String toString() {
        return "SmaxNewslettersGetNewsletterStatusesRequest[count=" + count
                + ", queryParams=" + queryParams
                + ", direction=" + direction + ']';
    }
}
