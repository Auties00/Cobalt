package com.github.auties00.cobalt.wire.stanza.smax.status;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;
import java.util.Optional;

/**
 * The structured projection of an inbound newsletter {@code <status>} stanza.
 *
 * <p>This type carries the envelope correlation metadata and a content-type classifier for the
 * inner payload shape, which today is either a reaction or a reaction-revoke; the relay reserves
 * the classifier for future variants. Callers handling newsletter-status deliveries pair this
 * projection with a {@link SmaxStatusDeliverIncomingNewsletterStatusAcknowledgement.SuccessAck} to
 * ack the delivery. The {@link #raw()} accessor exposes the backing {@link Stanza} so callers can
 * decode the variable-shape payload (reaction emoji, target server-id) without Cobalt having to
 * model each variant here.
 *
 * @deprecated superseded by the general newsletter-status path ({@code NewsletterStatusStreamHandler}).
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxInStatusDeliverIncomingNewsletterStatusRequest")
@WhatsAppWebModule(moduleName = "WASmaxInStatusDeliverFromNewsletterMixin")
@WhatsAppWebModule(moduleName = "WASmaxInStatusDeliverStatusNewsletterContentMixin")
@WhatsAppWebModule(moduleName = "WASmaxInStatusDeliverOfflineMixin")
public final class SmaxStatusDeliverIncomingNewsletterStatusResponse implements SmaxStanza.Response {
    /**
     * The relay-assigned stanza id.
     */
    private final String stanzaId;

    /**
     * The newsletter JID the status was published from.
     */
    private final Jid newsletterJid;

    /**
     * The relay-assigned status-message id within the newsletter.
     */
    private final long serverId;

    /**
     * The unix-second timestamp of the post.
     */
    private final long timestamp;

    /**
     * Whether the {@code is_sender="true"} attribute was present, which is {@code true} when the
     * connected client authored this status post.
     */
    private final boolean fromSelf;

    /**
     * The content-type variant name classifying the inner payload shape.
     *
     * <p>Today this is either {@code "newsletter_reaction"} or
     * {@code "newsletter_reaction_revoke"}; future variants extend this set without breaking the
     * projection contract.
     */
    private final String contentTypeName;

    /**
     * The optional offline counter from the {@code offline} attribute, bounded to {@code 0..12}.
     */
    private final Integer offline;

    /**
     * The raw {@code <status>} {@link Stanza} backing this projection.
     *
     * <p>Retained so callers can project the variable-shape content children (reaction emoji,
     * target server-id) without Cobalt having to model every payload variant.
     */
    private final Stanza raw;

    /**
     * Constructs a newsletter-status projection.
     *
     * <p>This constructor is invoked by {@link #of(Stanza)} after a successful parse and is not
     * intended for direct caller use.
     *
     * @param stanzaId        the relay-assigned stanza id; never {@code null}
     * @param newsletterJid   the source newsletter JID; never {@code null}
     * @param serverId        the server-assigned status-message id
     * @param timestamp       the unix-second timestamp
     * @param fromSelf        whether the connected client authored the post
     * @param contentTypeName the content-type variant name; never {@code null}
     * @param offline         the optional offline counter; may be {@code null}
     * @param raw             the underlying status stanza; never {@code null}
     * @throws NullPointerException if any non-nullable argument is {@code null}
     */
    public SmaxStatusDeliverIncomingNewsletterStatusResponse(String stanzaId,
                   Jid newsletterJid,
                   long serverId,
                   long timestamp,
                   boolean fromSelf,
                   String contentTypeName,
                   Integer offline,
                   Stanza raw) {
        this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
        this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.fromSelf = fromSelf;
        this.contentTypeName = Objects.requireNonNull(contentTypeName, "contentTypeName cannot be null");
        this.offline = offline;
        this.raw = Objects.requireNonNull(raw, "raw cannot be null");
    }

    /**
     * Returns the relay-assigned stanza id.
     *
     * <p>Callers echo this value into the matching
     * {@link SmaxStatusDeliverIncomingNewsletterStatusAcknowledgement.SuccessAck}.
     *
     * @return the id; never {@code null}
     */
    public String stanzaId() {
        return stanzaId;
    }

    /**
     * Returns the source newsletter JID.
     *
     * @return the JID; never {@code null}
     */
    public Jid newsletterJid() {
        return newsletterJid;
    }

    /**
     * Returns the server-assigned status-message id within the newsletter.
     *
     * @return the id
     */
    public long serverId() {
        return serverId;
    }

    /**
     * Returns the unix-second timestamp.
     *
     * @return the timestamp
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Reports whether the connected client authored this status post.
     *
     * @return {@code true} when the {@code is_sender="true"} attribute was present
     */
    public boolean fromSelf() {
        return fromSelf;
    }

    /**
     * Returns the content-type variant name.
     *
     * @return the name; never {@code null}
     */
    public String contentTypeName() {
        return contentTypeName;
    }

    /**
     * Returns the optional offline counter.
     *
     * <p>The counter is bounded to {@code 0..12} by the WA Web schema and is set on deliveries
     * replayed from the relay's offline backlog.
     *
     * @return an {@link Optional} carrying the counter, or {@link Optional#empty()} when omitted
     */
    public Optional<Integer> offline() {
        return Optional.ofNullable(offline);
    }

    /**
     * Returns the underlying {@code <status>} {@link Stanza}.
     *
     * <p>Lets callers project the variable-shape content children without Cobalt having to model
     * every variant.
     *
     * @return the raw stanza; never {@code null}
     */
    public Stanza raw() {
        return raw;
    }

    /**
     * Parses a newsletter-status projection from the given {@code <status>} stanza.
     *
     * <p>Returns {@link Optional#empty()} for any deviation from the documented schema: a wrong
     * description, a missing id, a non-newsletter sender JID, an out-of-range server-id, timestamp,
     * or offline counter, or an unrecognised content-type shape.
     *
     * @implNote
     * This implementation enforces the WA Web range bounds inline: server-id in
     * {@code 99..2147476647}, timestamp in {@code 1577865600..4102473600}, and offline in
     * {@code 0..12}.
     *
     * @param stanza the inbound status stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or {@link Optional#empty()} when the
     *         stanza does not match the expected shape
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInStatusDeliverIncomingNewsletterStatusRequest",
            exports = "parseIncomingNewsletterStatusRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxStatusDeliverIncomingNewsletterStatusResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("status")) {
            return Optional.empty();
        }
        var stanzaId = stanza.getAttributeAsString("id").orElse(null);
        if (stanzaId == null) {
            return Optional.empty();
        }
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null || !from.hasNewsletterServer()) {
            return Optional.empty();
        }
        var serverIdOpt = stanza.getAttributeAsLong("server_id");
        if (serverIdOpt.isEmpty()) {
            return Optional.empty();
        }
        var serverId = serverIdOpt.getAsLong();
        if (serverId < 99 || serverId > 2147476647L) {
            return Optional.empty();
        }
        var timestampOpt = stanza.getAttributeAsLong("t");
        if (timestampOpt.isEmpty()) {
            return Optional.empty();
        }
        var timestamp = timestampOpt.getAsLong();
        if (timestamp < 1577865600L || timestamp > 4102473600L) {
            return Optional.empty();
        }
        var fromSelf = stanza.hasAttribute("is_sender", "true");
        var contentTypeName = classifyContent(stanza);
        if (contentTypeName == null) {
            return Optional.empty();
        }
        Integer offline = null;
        var offlineOpt = stanza.getAttributeAsInt("offline");
        if (offlineOpt.isPresent()) {
            var ov = offlineOpt.getAsInt();
            if (ov < 0 || ov > 12) {
                return Optional.empty();
            }
            offline = ov;
        }
        return Optional.of(new SmaxStatusDeliverIncomingNewsletterStatusResponse(stanzaId, from, serverId, timestamp, fromSelf,
                contentTypeName, offline, stanza));
    }

    /**
     * Classifies the content-type variant by inspecting the {@code <status>} child structure.
     *
     * <p>Returns {@code null} when no documented content variant matched.
     *
     * @implNote
     * This implementation only recognises the two reaction variants today; future content types
     * extend the cascade without breaking callers.
     *
     * @param stanza the status stanza; never {@code null}
     * @return the variant name, or {@code null} when no variant matched
     */
    @WhatsAppWebExport(moduleName = "WASmaxInStatusDeliverNewsletterStatusContentTypeMixins",
            exports = "parseNewsletterStatusContentTypeMixins",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String classifyContent(Stanza stanza) {
        if (stanza.hasChild("reaction")) {
            if (stanza.getChild("reaction")
                    .map(c -> c.hasAttribute("operation", "revoke"))
                    .orElse(false)) {
                return "newsletter_reaction_revoke";
            }
            return "newsletter_reaction";
        }
        return null;
    }

    /**
     * Compares this projection to another for value equality on every field including the
     * underlying {@link Stanza}.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a
     *         {@link SmaxStatusDeliverIncomingNewsletterStatusResponse} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxStatusDeliverIncomingNewsletterStatusResponse) obj;
        return this.serverId == that.serverId
                && this.timestamp == that.timestamp
                && this.fromSelf == that.fromSelf
                && Objects.equals(this.stanzaId, that.stanzaId)
                && Objects.equals(this.newsletterJid, that.newsletterJid)
                && Objects.equals(this.contentTypeName, that.contentTypeName)
                && Objects.equals(this.offline, that.offline)
                && Objects.equals(this.raw, that.raw);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(stanzaId, newsletterJid, serverId, timestamp, fromSelf,
                contentTypeName, offline, raw);
    }

    /**
     * Returns a debug-friendly representation of this projection.
     *
     * <p>The format is intended for logging and is not part of any contract; the underlying stanza
     * is deliberately omitted to keep the result readable.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxStatusDeliverIncomingNewsletterStatusResponse[stanzaId=" + stanzaId
                + ", newsletterJid=" + newsletterJid
                + ", serverId=" + serverId
                + ", timestamp=" + timestamp
                + ", fromSelf=" + fromSelf
                + ", contentTypeName=" + contentTypeName
                + ", offline=" + offline + ']';
    }
}
