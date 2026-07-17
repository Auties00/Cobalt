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
 * Fetches a slice of a newsletter's message history.
 *
 * <p>Address the target with a {@link SmaxNewslettersGetNewsletterMessagesQueryParams.ByJid} or
 * {@link SmaxNewslettersGetNewsletterMessagesQueryParams.ByInvite} mode; pass a {@code null} cursor
 * to fetch the latest slice. The relay echoes the matching
 * {@link SmaxNewslettersGetNewsletterMessagesResponse}. The resulting IQ has shape:
 * {@snippet :
 *     <iq xmlns="newsletter" type="get" to="s.whatsapp.net">
 *         <messages count="50" before="120">
 *             <smax$any type="jid" jid="..." view_role="..."/>
 *         </messages>
 *     </iq>
 * }</p>
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersGetNewsletterMessagesRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersNewsletterMessageRequestIQPayloadMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersNewsletterMessageRequestPayloadMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersSelfIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersBaseIQGetRequestMixin")
public final class SmaxNewslettersGetNewsletterMessagesRequest implements SmaxStanza.Request {
    /**
     * The cap on returned {@code <message>} entries per round-trip.
     */
    private final int count;

    /**
     * The newsletter addressing parameters, either
     * {@link SmaxNewslettersGetNewsletterMessagesQueryParams.ByJid} or
     * {@link SmaxNewslettersGetNewsletterMessagesQueryParams.ByInvite}.
     */
    private final SmaxNewslettersGetNewsletterMessagesQueryParams queryParams;

    /**
     * The optional pagination cursor; {@code null} requests the latest slice.
     */
    private final SmaxNewslettersGetNewsletterMessagesDirection direction;

    /**
     * Constructs a new request.
     *
     * @implNote This implementation does not clamp {@code count} on the way out; callers targeting
     * WhatsApp Web parity should clamp to the server-reported maximum message count before invoking.
     *
     * @param count       the per-call cap; must be non-negative
     * @param queryParams the addressing parameters; never {@code null}
     * @param direction   the optional pagination cursor; may be {@code null}
     * @throws NullPointerException if {@code queryParams} is {@code null}
     */
    public SmaxNewslettersGetNewsletterMessagesRequest(int count, SmaxNewslettersGetNewsletterMessagesQueryParams queryParams, SmaxNewslettersGetNewsletterMessagesDirection direction) {
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
    public Optional<SmaxNewslettersGetNewsletterMessagesDirection> direction() {
        return Optional.ofNullable(direction);
    }

    /**
     * Builds the outbound {@code <iq>} stanza carrying the {@code <messages>} payload.
     *
     * <p>The IQ targets {@link Jid#userServer()} (not the newsletter JID) because the request
     * fetches across the relay's newsletter index; the {@code <smax$any>} child encodes the
     * addressing variant.</p>
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <messages>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutNewslettersGetNewsletterMessagesRequest",
            exports = "makeGetNewsletterMessagesRequest", adaptation = WhatsAppAdaptation.DIRECT)
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
        var messagesBuilder = new StanzaBuilder()
                .description("messages")
                .attribute("count", count);
        if (direction != null) {
            switch (direction) {
                case SmaxNewslettersGetNewsletterMessagesDirection.Before before -> messagesBuilder.attribute("before", before.pivot());
                case SmaxNewslettersGetNewsletterMessagesDirection.After after -> messagesBuilder.attribute("after", after.pivot());
            }
        }
        messagesBuilder.content(anyBuilder.build());
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "newsletter")
                .attribute("to", Jid.userServer())
                .attribute("type", "get")
                .content(messagesBuilder.build());
    }

    /**
     * Compares two requests for value equality on every field.
     *
     * @param obj the reference object to compare against
     * @return {@code true} when {@code obj} is a request carrying equal {@link #count()},
     *         {@link #queryParams()}, and {@link #direction()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewslettersGetNewsletterMessagesRequest) obj;
        return this.count == that.count
                && Objects.equals(this.queryParams, that.queryParams)
                && Objects.equals(this.direction, that.direction);
    }

    /**
     * Returns the hash code derived from every field.
     *
     * @return the combined hash of {@link #count()}, {@link #queryParams()}, and {@link #direction()}
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
        return "SmaxNewslettersGetNewsletterMessagesRequest[count=" + count
                + ", queryParams=" + queryParams
                + ", direction=" + direction + ']';
    }
}
