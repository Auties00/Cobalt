package com.github.auties00.cobalt.wire.stanza.smax.message;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the structured projection of an inbound {@code <message type="media">} stanza addressed
 * from a newsletter JID.
 *
 * <p>The projection carries the envelope correlation metadata (newsletter JID, stanza id, server id,
 * timestamp, authorship, edit timestamps, admin-profile child, paid-partnership marker, and offline
 * counter) plus a classifier naming the fanout-content variant the inner payload represents. The
 * inner payload itself is not modelled here: {@link #raw()} exposes the underlying {@link Stanza} so
 * callers can decrypt and extract any of the documented fanout-content shapes (text, media, reaction,
 * poll, and so on) without Cobalt modelling each shape. A projection is answered with a
 * {@link SmaxMessageDeliverNewsletterAcknowledgement} to ack or NACK the delivery.
 *
 * @deprecated superseded by the general newsletter-message decode ({@code MessageStreamHandler}/{@code messageService});
 * too narrow (media-only).
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxInMessageDeliverNewsletterRequest")
@WhatsAppWebModule(moduleName = "WASmaxInMessageDeliverNewsletterMessageWithJIDMixin")
@WhatsAppWebModule(moduleName = "WASmaxInMessageDeliverNewsletterMessageFanoutMixin")
@WhatsAppWebModule(moduleName = "WASmaxInMessageDeliverReceiverContentTypeMediaRCATMixin")
public final class SmaxMessageDeliverNewsletterResponse implements SmaxStanza.Response {
    /**
     * Holds the newsletter JID this delivery originated from.
     */
    private final Jid newsletterJid;

    /**
     * Holds the relay-assigned stanza id.
     */
    private final String stanzaId;

    /**
     * Holds the relay-assigned message id within the originating newsletter, used as a stable handle
     * when correlating subsequent reactions and poll-votes back to this delivery.
     */
    private final long serverId;

    /**
     * Holds the unix-second timestamp of the post.
     */
    private final long timestamp;

    /**
     * Records whether the {@code is_sender="true"} attribute was present, which is the case when the
     * connected client authored this post.
     */
    private final boolean fromSelf;

    /**
     * Holds the optional original-message timestamp from the {@code <meta original_msg_t/>} child,
     * set when this post is an edit; {@code null} otherwise.
     */
    private final Long metaOriginalMsgT;

    /**
     * Holds the optional last-edit timestamp from the {@code <meta msg_edit_t/>} child; {@code null}
     * when omitted.
     */
    private final Long metaMsgEditT;

    /**
     * Holds the optional {@code <meta><admin_profile/></meta>} child as a raw {@link Stanza} for
     * downstream projection; {@code null} when omitted.
     */
    private final Stanza adminProfileMeta;

    /**
     * Records whether the {@code <meta><paid_partnership/></meta>} marker was present.
     */
    private final boolean hasPaidPartnership;

    /**
     * Holds the optional offline counter from the {@code offline} attribute, bounded to
     * {@code 0..12}; {@code null} when omitted.
     */
    private final Integer offline;

    /**
     * Holds the fanout-content variant name classifying the inner payload shape so callers can branch
     * on a single field rather than re-walking the children.
     *
     * <p>The documented values are {@code "NewsletterText"}, {@code "NewsletterMedia"},
     * {@code "NewsletterReaction"}, {@code "NewsletterReactionRevoke"}, {@code "NewsletterEdit"},
     * {@code "NewsletterRevoke"}, {@code "NewsletterPollCreation"}, {@code "NewsletterQuizCreation"},
     * {@code "NewsletterPollVote"}, {@code "NewsletterPollResultSnapshot"}, {@code "NewsletterQuestion"},
     * {@code "NewsletterQuestionResponse"}, {@code "NewsletterQuestionReply"}, and
     * {@code "NewsletterWAMOEmpty"}.
     */
    private final String fanoutContentName;

    /**
     * Holds the {@code mediatype} attribute on the {@code <plaintext/>} child, always the literal
     * {@code "url"} on documented deliveries.
     */
    private final String plaintextMediatype;

    /**
     * Holds the raw bytes carried by the {@code <rcat/>} child that feed the
     * receiver-content-type-media decrypt step; opaque to Cobalt.
     */
    private final byte[] rcatBytes;

    /**
     * Holds the raw {@code <message>} {@link Stanza} backing this projection so callers can project the
     * variable-shape fanout-content children without Cobalt modelling every documented payload
     * variant.
     */
    private final Stanza raw;

    /**
     * Constructs a newsletter-delivery projection from already-parsed fields.
     *
     * <p>This constructor is invoked by {@link #of(Stanza)} after a successful parse and is not intended
     * for direct caller use.
     *
     * @param newsletterJid      the source newsletter JID; never {@code null}
     * @param stanzaId           the relay-assigned stanza id; never {@code null}
     * @param serverId           the server-assigned message id
     * @param timestamp          the unix-second timestamp
     * @param fromSelf           whether the connected client authored the post
     * @param metaOriginalMsgT   the optional original timestamp; may be {@code null}
     * @param metaMsgEditT       the optional edit timestamp; may be {@code null}
     * @param adminProfileMeta   the optional admin-profile child stanza; may be {@code null}
     * @param hasPaidPartnership whether the paid-partnership marker was present
     * @param offline            the optional offline counter; may be {@code null}
     * @param fanoutContentName  the fanout-content variant name; never {@code null}
     * @param plaintextMediatype the plaintext mediatype literal; never {@code null}
     * @param rcatBytes          the raw rcat bytes; never {@code null}
     * @param raw                the underlying message stanza; never {@code null}
     * @throws NullPointerException if any non-nullable argument is {@code null}
     */
    public SmaxMessageDeliverNewsletterResponse(Jid newsletterJid,
                   String stanzaId,
                   long serverId,
                   long timestamp,
                   boolean fromSelf,
                   Long metaOriginalMsgT,
                   Long metaMsgEditT,
                   Stanza adminProfileMeta,
                   boolean hasPaidPartnership,
                   Integer offline,
                   String fanoutContentName,
                   String plaintextMediatype,
                   byte[] rcatBytes,
                   Stanza raw) {
        this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.fromSelf = fromSelf;
        this.metaOriginalMsgT = metaOriginalMsgT;
        this.metaMsgEditT = metaMsgEditT;
        this.adminProfileMeta = adminProfileMeta;
        this.hasPaidPartnership = hasPaidPartnership;
        this.offline = offline;
        this.fanoutContentName = Objects.requireNonNull(fanoutContentName, "fanoutContentName cannot be null");
        this.plaintextMediatype = Objects.requireNonNull(plaintextMediatype, "plaintextMediatype cannot be null");
        this.rcatBytes = Objects.requireNonNull(rcatBytes, "rcatBytes cannot be null");
        this.raw = Objects.requireNonNull(raw, "raw cannot be null");
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
     * Returns the relay-assigned stanza id, echoed into the matching
     * {@link SmaxMessageDeliverNewsletterAcknowledgement} stanza.
     *
     * @return the id; never {@code null}
     */
    public String stanzaId() {
        return stanzaId;
    }

    /**
     * Returns the server-assigned message id within the newsletter.
     *
     * @return the id
     */
    public long serverId() {
        return serverId;
    }

    /**
     * Returns the unix-second timestamp of the post.
     *
     * @return the timestamp
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Reports whether the connected client authored this post.
     *
     * @return {@code true} when the {@code is_sender="true"} attribute was present
     */
    public boolean fromSelf() {
        return fromSelf;
    }

    /**
     * Returns the optional original-message timestamp, present when the post is an edit and pointing
     * at the original post's timestamp.
     *
     * @return an {@link Optional} carrying the timestamp, or {@link Optional#empty()} when the post is
     *         not an edit
     */
    public Optional<Long> metaOriginalMsgT() {
        return Optional.ofNullable(metaOriginalMsgT);
    }

    /**
     * Returns the optional last-edit timestamp.
     *
     * @return an {@link Optional} carrying the timestamp, or {@link Optional#empty()} when omitted
     */
    public Optional<Long> metaMsgEditT() {
        return Optional.ofNullable(metaMsgEditT);
    }

    /**
     * Returns the optional admin-profile metadata projection.
     *
     * <p>The wrapped stanza is the raw {@code <meta><admin_profile/></meta>} child; callers project its
     * attributes as needed.
     *
     * @return an {@link Optional} carrying the stanza, or {@link Optional#empty()} when omitted
     */
    public Optional<Stanza> adminProfileMeta() {
        return Optional.ofNullable(adminProfileMeta);
    }

    /**
     * Reports whether the paid-partnership marker was present.
     *
     * @return {@code true} when the marker was present
     */
    public boolean hasPaidPartnership() {
        return hasPaidPartnership;
    }

    /**
     * Returns the optional offline counter.
     *
     * <p>The value is bounded to {@code 0..12} by the WhatsApp Web schema and is set on deliveries
     * replayed from the relay's offline backlog.
     *
     * @return an {@link Optional} carrying the counter, or {@link Optional#empty()} when omitted
     */
    public Optional<Integer> offline() {
        return Optional.ofNullable(offline);
    }

    /**
     * Returns the fanout-content variant name classifying the inner payload shape.
     *
     * @return the variant name; never {@code null}
     */
    public String fanoutContentName() {
        return fanoutContentName;
    }

    /**
     * Returns the {@code <plaintext mediatype>} literal, always {@code "url"} on documented
     * deliveries.
     *
     * @return the mediatype; never {@code null}
     */
    public String plaintextMediatype() {
        return plaintextMediatype;
    }

    /**
     * Returns the raw bytes carried by {@code <rcat/>} that feed the receiver-content-type-media
     * decrypt pipeline; opaque to Cobalt.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] rcatBytes() {
        return rcatBytes;
    }

    /**
     * Returns the underlying {@code <message>} {@link Stanza} so callers can project the variable-shape
     * fanout-content children without Cobalt modelling every documented payload variant.
     *
     * @return the raw stanza; never {@code null}
     */
    public Stanza raw() {
        return raw;
    }

    /**
     * Parses a newsletter-delivery projection from the given {@code <message>} stanza.
     *
     * <p>The result is {@link Optional#empty()} for any deviation from the documented schema: a wrong
     * description, a wrong type, a non-newsletter sender JID, a missing or out-of-range server id,
     * timestamp, or offline counter, a missing plaintext child, a missing or non-url mediatype, a
     * missing rcat child, missing rcat bytes, or an unrecognised fanout-content shape.
     *
     * @implNote
     * This implementation rolls the WhatsApp Web {@code parseNewsletterRequest},
     * {@code parseNewsletterMessageWithJIDMixin}, {@code parseNewsletterMessageFanoutMixin}, and
     * {@code parseReceiverContentTypeMediaRCATMixin} cascades into one straight-line pass. The
     * server-id range ({@code 99..2147476647}) and timestamps ({@code original_msg_t} in
     * {@code 1577865600..4102473600}, {@code msg_edit_t} in the corresponding millisecond-domain
     * bounds) mirror the WhatsApp Web range checks verbatim.
     *
     * @param stanza the inbound message stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or {@link Optional#empty()} when the stanza
     *         does not match the expected shape
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInMessageDeliverNewsletterRequest",
            exports = "parseNewsletterRequest", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxMessageDeliverNewsletterResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("message")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("type", "media")) {
            return Optional.empty();
        }
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null || !from.hasNewsletterServer()) {
            return Optional.empty();
        }
        var stanzaId = stanza.getAttributeAsString("id").orElse(null);
        if (stanzaId == null) {
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
        if (timestamp < 0) {
            return Optional.empty();
        }
        var fromSelf = stanza.hasAttribute("is_sender", "true");
        var meta = stanza.getChild("meta").orElse(null);
        Long metaOriginalMsgT = null;
        Long metaMsgEditT = null;
        Stanza adminProfileMeta = null;
        var hasPaidPartnership = false;
        if (meta != null) {
            var origOpt = meta.getAttributeAsLong("original_msg_t");
            if (origOpt.isPresent()) {
                var ov = origOpt.getAsLong();
                if (ov < 1577865600L || ov > 4102473600L) {
                    return Optional.empty();
                }
                metaOriginalMsgT = ov;
            }
            var editOpt = meta.getAttributeAsLong("msg_edit_t");
            if (editOpt.isPresent()) {
                var ev = editOpt.getAsLong();
                if (ev < 1577865600000L || ev > 4102473600000L) {
                    return Optional.empty();
                }
                metaMsgEditT = ev;
            }
            adminProfileMeta = meta.getChild("admin_profile").orElse(null);
            hasPaidPartnership = meta.hasChild("paid_partnership");
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
        var plaintext = stanza.getChild("plaintext").orElse(null);
        if (plaintext == null) {
            return Optional.empty();
        }
        if (!plaintext.hasAttribute("mediatype", "url")) {
            return Optional.empty();
        }
        var rcat = stanza.getChild("rcat").orElse(null);
        if (rcat == null) {
            return Optional.empty();
        }
        var rcatBytes = rcat.toContentBytes().orElse(null);
        if (rcatBytes == null) {
            return Optional.empty();
        }
        var fanoutContentName = classifyFanoutContent(stanza);
        if (fanoutContentName == null) {
            return Optional.empty();
        }
        return Optional.of(new SmaxMessageDeliverNewsletterResponse(
                from,
                stanzaId,
                serverId,
                timestamp,
                fromSelf,
                metaOriginalMsgT,
                metaMsgEditT,
                adminProfileMeta,
                hasPaidPartnership,
                offline,
                fanoutContentName,
                "url",
                rcatBytes,
                stanza));
    }

    /**
     * Classifies the fanout-content variant by inspecting the child structure of the message stanza,
     * returning {@code null} when no variant matched.
     *
     * @implNote
     * This implementation walks the WhatsApp Web {@code parseNewsletterMessageFanoutContent} priority
     * order (question, question-response, edit, question-reply, revoke, reaction with optional revoke,
     * poll-creation, quiz-creation, poll-vote, poll-result-snapshot, wamo-empty) and collapses the
     * classifier to a label-only result. Downstream callers re-walk {@link #raw} to extract the
     * variant payload, so this method only surfaces the variant name.
     *
     * @param stanza the message stanza; never {@code null}
     * @return the variant name, or {@code null} when no variant matched
     */
    @WhatsAppWebExport(moduleName = "WASmaxInMessageDeliverNewsletterMessageFanoutContent",
            exports = "parseNewsletterMessageFanoutContent",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String classifyFanoutContent(Stanza stanza) {
        if (stanza.hasChild("question")) {
            return "NewsletterQuestion";
        }
        if (stanza.hasChild("question_response")) {
            return "NewsletterQuestionResponse";
        }
        if (stanza.hasChild("edit")) {
            return "NewsletterEdit";
        }
        if (stanza.hasChild("question_reply")) {
            return "NewsletterQuestionReply";
        }
        if (stanza.hasChild("revoke")) {
            return "NewsletterRevoke";
        }
        if (stanza.hasChild("reaction")) {
            if (stanza.getChild("reaction")
                    .map(c -> c.hasAttribute("operation", "revoke"))
                    .orElse(false)) {
                return "NewsletterReactionRevoke";
            }
            return "NewsletterReaction";
        }
        if (stanza.hasChild("poll_creation")) {
            return "NewsletterPollCreation";
        }
        if (stanza.hasChild("quiz_creation")) {
            return "NewsletterQuizCreation";
        }
        if (stanza.hasChild("poll_vote")) {
            return "NewsletterPollVote";
        }
        if (stanza.hasChild("poll_result_snapshot")) {
            return "NewsletterPollResultSnapshot";
        }
        if (stanza.hasChild("wamo")) {
            return "NewsletterWAMOEmpty";
        }
        return "NewsletterText";
    }

    /**
     * Compares this projection to another object for value equality across every field, including the
     * underlying {@link Stanza}.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a {@link SmaxMessageDeliverNewsletterResponse} with
     *         identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxMessageDeliverNewsletterResponse) obj;
        return this.serverId == that.serverId
                && this.timestamp == that.timestamp
                && this.fromSelf == that.fromSelf
                && this.hasPaidPartnership == that.hasPaidPartnership
                && Objects.equals(this.newsletterJid, that.newsletterJid)
                && Objects.equals(this.stanzaId, that.stanzaId)
                && Objects.equals(this.metaOriginalMsgT, that.metaOriginalMsgT)
                && Objects.equals(this.metaMsgEditT, that.metaMsgEditT)
                && Objects.equals(this.adminProfileMeta, that.adminProfileMeta)
                && Objects.equals(this.offline, that.offline)
                && Objects.equals(this.fanoutContentName, that.fanoutContentName)
                && Objects.equals(this.plaintextMediatype, that.plaintextMediatype)
                && Arrays.equals(this.rcatBytes, that.rcatBytes)
                && Objects.equals(this.raw, that.raw);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @implNote
     * This implementation mixes {@link Arrays#hashCode(byte[])} of the rcat bytes into the field-wise
     * hash so byte-array contents drive the result.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(newsletterJid, stanzaId, serverId, timestamp, fromSelf,
                metaOriginalMsgT, metaMsgEditT, adminProfileMeta, hasPaidPartnership, offline,
                fanoutContentName, plaintextMediatype, raw);
        result = 31 * result + Arrays.hashCode(rcatBytes);
        return result;
    }

    /**
     * Returns a debug-friendly representation of this projection.
     *
     * <p>The rcat bytes and underlying stanza are deliberately omitted to keep the result readable, and
     * the format is intended for logging rather than as a stable contract.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxMessageDeliverNewsletterResponse[newsletterJid=" + newsletterJid
                + ", stanzaId=" + stanzaId
                + ", serverId=" + serverId
                + ", timestamp=" + timestamp
                + ", fromSelf=" + fromSelf
                + ", metaOriginalMsgT=" + metaOriginalMsgT
                + ", metaMsgEditT=" + metaMsgEditT
                + ", hasPaidPartnership=" + hasPaidPartnership
                + ", offline=" + offline
                + ", fanoutContentName=" + fanoutContentName
                + ", plaintextMediatype=" + plaintextMediatype + ']';
    }
}
