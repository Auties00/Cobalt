package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents the inbound live-updates notification pushed by the relay
 * to a client previously subscribed via
 * {@link SmaxNewslettersSubscribeToLiveUpdatesRequest}.
 * The receive pipeline parses the inbound stanza, projects
 * {@link #messages()} into the local newsletter store, then emits a
 * {@link SmaxNewslettersLiveUpdatesNotificationAcknowledgement}. The
 * acknowledgement must fire even when the delivery is rejected,
 * otherwise the relay re-pushes the same notification.
 */
@WhatsAppWebModule(moduleName = "WASmaxInNewslettersLiveUpdatesNotificationRequest")
@WhatsAppWebModule(moduleName = "WASmaxInNewslettersCommonNotificationMixin")
@WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMessageResponsePayloadMixin")
public final class SmaxNewslettersLiveUpdatesNotificationResponse implements SmaxStanza.Response {
    /**
     * Holds the notification stanza id, echoed verbatim into the
     * {@link SmaxNewslettersLiveUpdatesNotificationAcknowledgement}.
     */
    private final String notificationId;

    /**
     * Holds the newsletter {@link Jid} that produced the notification.
     */
    private final Jid notificationFrom;

    /**
     * Holds the optional newsletter {@link Jid} echoed on the
     * {@code <messages>} payload.
     */
    private final Jid messagesJid;

    /**
     * Holds the optional unix-second timestamp echoed on the
     * {@code <messages>} payload.
     */
    private final Long messagesTimestamp;

    /**
     * Holds the list of newsletter messages carried in the live-updates
     * delta.
     */
    private final List<NewsletterMessage> messages;

    /**
     * Constructs a new inbound projection.
     * The {@code messagesJid} and {@code messagesTimestamp} are optional
     * because the relay only echoes them when the corresponding
     * attributes were present on the wire; {@code notificationId} and
     * {@code notificationFrom} are mandatory because the
     * {@link SmaxNewslettersLiveUpdatesNotificationAcknowledgement}
     * echoes them verbatim.
     *
     * @param notificationId    the notification id; never {@code null}
     * @param notificationFrom  the notification sender {@link Jid}; never {@code null}
     * @param messagesJid       the optional echoed {@link Jid}; may be {@code null}
     * @param messagesTimestamp the optional echoed timestamp; may be {@code null}
     * @param messages          the message entries; never {@code null} (empty allowed)
     * @throws NullPointerException if {@code notificationId} or {@code notificationFrom} is {@code null}
     */
    public SmaxNewslettersLiveUpdatesNotificationResponse(String notificationId, Jid notificationFrom,
                   Jid messagesJid, Long messagesTimestamp,
                   List<NewsletterMessage> messages) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.messagesJid = messagesJid;
        this.messagesTimestamp = messagesTimestamp;
        this.messages = List.copyOf(Objects.requireNonNullElse(messages, List.of()));
    }

    /**
     * Returns the notification id.
     *
     * @return the id; never {@code null}
     */
    public String notificationId() {
        return notificationId;
    }

    /**
     * Returns the notification sender {@link Jid}.
     *
     * @return the {@link Jid}; never {@code null}
     */
    public Jid notificationFrom() {
        return notificationFrom;
    }

    /**
     * Returns the optional echoed newsletter {@link Jid}.
     *
     * @return an {@link Optional} carrying the {@link Jid}, or empty when the relay omitted it
     */
    public Optional<Jid> messagesJid() {
        return Optional.ofNullable(messagesJid);
    }

    /**
     * Returns the optional echoed timestamp.
     *
     * @return an {@link Optional} carrying the unix-second timestamp, or empty when the relay omitted it
     */
    public Optional<Long> messagesTimestamp() {
        return Optional.ofNullable(messagesTimestamp);
    }

    /**
     * Returns the message entries.
     *
     * @return an unmodifiable {@link List} of entries; never {@code null}
     */
    public List<NewsletterMessage> messages() {
        return messages;
    }

    /**
     * Parses a notification projection from a {@code <notification/>}
     * {@link Stanza}.
     * Returns {@link Optional#empty()} when the description is not
     * {@code notification}, the {@code type} attribute is not
     * {@code "newsletter"}, the required {@code id} or {@code from}
     * attributes are missing, the {@code <live_updates>} or
     * {@code <messages>} envelope is absent, the {@code t} attribute on
     * {@code <messages>} is negative, or any nested {@code <message>}
     * fails its own {@link NewsletterMessage#of(Stanza)} parse.
     *
     * @param stanza the inbound notification stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty on no-match
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInNewslettersLiveUpdatesNotificationRequest",
            exports = "parseLiveUpdatesNotificationRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxNewslettersLiveUpdatesNotificationResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("notification")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("type", "newsletter")) {
            return Optional.empty();
        }
        var notificationId = stanza.getAttributeAsString("id").orElse(null);
        if (notificationId == null) {
            return Optional.empty();
        }
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null) {
            return Optional.empty();
        }
        var liveUpdates = stanza.getChild("live_updates").orElse(null);
        if (liveUpdates == null) {
            return Optional.empty();
        }
        var messagesNode = liveUpdates.getChild("messages").orElse(null);
        if (messagesNode == null) {
            return Optional.empty();
        }
        var messagesJid = messagesNode.getAttributeAsJid("jid").orElse(null);
        Long timestamp = null;
        var tOpt = messagesNode.getAttributeAsLong("t");
        if (tOpt.isPresent()) {
            var tv = tOpt.getAsLong();
            if (tv < 0) {
                return Optional.empty();
            }
            timestamp = tv;
        }
        var entries = new ArrayList<NewsletterMessage>();
        for (var messageNode : messagesNode.getChildren("message")) {
            var entry = NewsletterMessage.of(messageNode).orElse(null);
            if (entry == null) {
                return Optional.empty();
            }
            entries.add(entry);
        }
        return Optional.of(new SmaxNewslettersLiveUpdatesNotificationResponse(notificationId, from, messagesJid, timestamp, entries));
    }

    /**
     * Compares two notifications for value equality on every field.
     *
     * @param obj the reference object to compare against
     * @return {@code true} when {@code obj} is a notification with equal field values
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewslettersLiveUpdatesNotificationResponse) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.messagesJid, that.messagesJid)
                && Objects.equals(this.messagesTimestamp, that.messagesTimestamp)
                && Objects.equals(this.messages, that.messages);
    }

    /**
     * Returns the hash code derived from every field.
     *
     * @return the combined hash of every field
     */
    @Override
    public int hashCode() {
        return Objects.hash(notificationId, notificationFrom, messagesJid, messagesTimestamp, messages);
    }

    /**
     * Returns a debug representation including every field.
     *
     * @return a record-like rendering of this notification
     */
    @Override
    public String toString() {
        return "SmaxNewslettersLiveUpdatesNotificationResponse[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", messagesJid=" + messagesJid
                + ", messagesTimestamp=" + messagesTimestamp
                + ", messages=" + messages + ']';
    }

    /**
     * Represents one typed projection of a {@code <message>} entry
     * carried by a live-updates notification.
     *
     * @implNote This implementation declares its own message projection rather than reusing {@link SmaxNewslettersGetNewsletterMessagesResponse.NewsletterMessage}, even though the two share the wire shape and parser export name, to keep the live-updates payload self-contained and decouple the receive handler from the history-fetch type.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMessageHistoryWithAddOnsMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMessageHistoryMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMessageHistoryContent")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterReactionsMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterPollVotesMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterForwardsCountMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterResponsesCountMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterViewsCountViewsOrDeprecatedMixinGroup")
    public static final class NewsletterMessage {
        /**
         * Holds the optional client-supplied stanza id of the message.
         */
        private final String stanzaId;

        /**
         * Holds the server-assigned monotonic message id within the
         * newsletter.
         */
        private final long serverId;

        /**
         * Holds the optional unix-second timestamp of the message.
         */
        private final Long timestamp;

        /**
         * Holds whether the message was authored by the connected client.
         */
        private final boolean fromSelf;

        /**
         * Holds the underlying {@link Stanza} exposing the variable-shape
         * add-on children.
         */
        private final Stanza raw;

        /**
         * Holds the parsed content variant of the message, or {@code null}
         * when the {@code <message>} carries no content element (an
         * add-on-only update).
         */
        private final Variant contentVariant;

        /**
         * Holds the {@code <plaintext>} payload bytes (an end-to-end
         * {@code LinkedMessageContainer} protobuf), or {@code null} when the
         * payload is absent or outside the {@code [1, 1048576]} byte range.
         */
        private final byte[] plaintext;

        /**
         * Holds the {@code mediatype} attribute of the {@code <plaintext>}
         * child for a media message, or {@code null} when absent.
         */
        private final String mediaType;

        /**
         * Holds the {@code <meta original_msg_t>} value in unix seconds, or
         * {@code null} when absent.
         */
        private final Long originalMessageTimestamp;

        /**
         * Holds the {@code <meta msg_edit_t>} value in unix milliseconds, or
         * {@code null} when absent.
         */
        private final Long editTimestampMs;

        /**
         * Holds the aggregated reaction tallies keyed by emoji, or
         * {@code null} when the {@code <reactions>} element is absent; the
         * map is present but empty when the element is present yet carries
         * no {@code <reaction>} child.
         */
        private final Map<String, Long> reactions;

        /**
         * Holds the aggregated poll-vote tallies; never {@code null}, empty
         * when the {@code <votes>} element is absent or carries no valid
         * {@code <vote>} child.
         */
        private final List<PollVote> pollVotes;

        /**
         * Holds the {@code <forwards_count count>} value, or {@code null}
         * when the element is absent.
         */
        private final Long forwardsCount;

        /**
         * Holds the {@code <responses_count count>} value, or {@code null}
         * when the element is absent.
         */
        private final Long responsesCount;

        /**
         * Holds the resolved {@code <views_count count>} value, or
         * {@code null} when no applicable {@code <views_count>} element is
         * present.
         */
        private final Long viewsCount;

        /**
         * Constructs a new newsletter-message projection.
         * The {@code stanzaId} and {@code timestamp} are optional because
         * the relay only emits them when the message carries those
         * attributes on the wire. The content variant and the add-on
         * children (reactions, poll votes, forwards, responses, views) are
         * parsed eagerly from {@code raw} and exposed through the typed
         * accessors; this keeps the projection self-describing while
         * leaving {@link #equals(Object)} keyed on {@code raw}, which fully
         * determines every derived field.
         *
         * @param stanzaId  the optional stanza id; may be {@code null}
         * @param serverId  the server-assigned id
         * @param timestamp the optional unix-second timestamp; may be {@code null}
         * @param fromSelf  whether the message was authored by self
         * @param raw       the underlying {@link Stanza}; never {@code null}
         * @throws NullPointerException if {@code raw} is {@code null}
         */
        public NewsletterMessage(String stanzaId, long serverId, Long timestamp, boolean fromSelf, Stanza raw) {
            this.stanzaId = stanzaId;
            this.serverId = serverId;
            this.timestamp = timestamp;
            this.fromSelf = fromSelf;
            this.raw = Objects.requireNonNull(raw, "raw cannot be null");
            var metaNode = raw.getChild("meta").orElse(null);
            var plaintextNode = raw.getChild("plaintext").orElse(null);
            this.plaintext = plaintextNode == null ? null : plaintextNode.toContentBytes()
                    .filter(bytes -> bytes.length >= 1 && bytes.length <= 1048576)
                    .orElse(null);
            this.mediaType = plaintextNode == null ? null : plaintextNode.getAttributeAsString("mediatype").orElse(null);
            this.originalMessageTimestamp = metaNode == null ? null : boxedLong(metaNode.getAttributeAsLong("original_msg_t"));
            this.editTimestampMs = metaNode == null ? null : boxedLong(metaNode.getAttributeAsLong("msg_edit_t"));
            this.contentVariant = classifyContent(raw, plaintextNode, this.plaintext, this.mediaType, metaNode);
            this.reactions = parseReactions(raw);
            this.pollVotes = parsePollVotes(raw);
            this.forwardsCount = parseCount(raw, "forwards_count");
            this.responsesCount = parseCount(raw, "responses_count");
            this.viewsCount = parseViewsCount(raw);
        }

        /**
         * Returns the optional client-supplied stanza id.
         *
         * @return an {@link Optional} carrying the stanza id, or empty when the relay omitted it
         */
        public Optional<String> stanzaId() {
            return Optional.ofNullable(stanzaId);
        }

        /**
         * Returns the server-assigned message id.
         *
         * @return the server-assigned id
         */
        public long serverId() {
            return serverId;
        }

        /**
         * Returns the optional unix-second timestamp.
         *
         * @return an {@link Optional} carrying the timestamp, or empty when the relay omitted it
         */
        public Optional<Long> timestamp() {
            return Optional.ofNullable(timestamp);
        }

        /**
         * Returns whether the message was authored by the connected
         * client.
         *
         * @return {@code true} when {@code is_sender="true"} was present on the wire
         */
        public boolean fromSelf() {
            return fromSelf;
        }

        /**
         * Returns the underlying {@link Stanza}.
         *
         * @return the raw {@link Stanza} exposing the add-on children; never {@code null}
         */
        public Stanza raw() {
            return raw;
        }

        /**
         * Returns the parsed content {@link Variant}.
         *
         * @return an {@link Optional} carrying the variant, or empty when the message carries no content element (an add-on-only update)
         */
        public Optional<Variant> contentVariant() {
            return Optional.ofNullable(contentVariant);
        }

        /**
         * Returns whether the message carries a decodable content element.
         *
         * @return {@code true} when a content variant was matched
         */
        public boolean hasContent() {
            return contentVariant != null;
        }

        /**
         * Returns whether the message is an admin edit ({@code edit="3"}).
         *
         * @return {@code true} when the content variant is {@link Variant#EDIT}
         */
        public boolean isEdit() {
            return contentVariant == Variant.EDIT;
        }

        /**
         * Returns whether the message is an admin revoke ({@code edit="8"}).
         *
         * @return {@code true} when the content variant is {@link Variant#REVOKE}
         */
        public boolean isRevoke() {
            return contentVariant == Variant.REVOKE;
        }

        /**
         * Returns the {@code <plaintext>} payload bytes.
         * The payload is an end-to-end {@code LinkedMessageContainer} protobuf; it
         * is absent for the {@link Variant#REVOKE} and {@link Variant#WAMO_EMPTY}
         * variants and for add-on-only updates.
         *
         * @return an {@link Optional} carrying the payload bytes, or empty when absent or out of range
         */
        public Optional<byte[]> plaintext() {
            return Optional.ofNullable(plaintext);
        }

        /**
         * Returns the {@code mediatype} attribute of the {@code <plaintext>}
         * child.
         *
         * @return an {@link Optional} carrying the media type, or empty when the message is not a media message
         */
        public Optional<String> mediaType() {
            return Optional.ofNullable(mediaType);
        }

        /**
         * Returns the {@code <meta original_msg_t>} value in unix seconds.
         *
         * @return an {@link OptionalLong} carrying the original send timestamp, or empty when absent
         */
        public OptionalLong originalMessageTimestamp() {
            return originalMessageTimestamp == null ? OptionalLong.empty() : OptionalLong.of(originalMessageTimestamp);
        }

        /**
         * Returns the {@code <meta msg_edit_t>} value in unix milliseconds.
         *
         * @return an {@link OptionalLong} carrying the edit timestamp, or empty when absent
         */
        public OptionalLong editTimestampMs() {
            return editTimestampMs == null ? OptionalLong.empty() : OptionalLong.of(editTimestampMs);
        }

        /**
         * Returns the aggregated reaction tallies keyed by emoji.
         * An empty {@link Optional} means the {@code <reactions>} element was
         * absent; a present but empty map means the element was present yet
         * carried no {@code <reaction>} child. Both cases clear reactions on
         * apply; only a present, non-empty map replaces them.
         *
         * @return an {@link Optional} carrying an unmodifiable {@code emoji -> count} map, or empty when the element is absent
         */
        public Optional<Map<String, Long>> reactions() {
            return Optional.ofNullable(reactions);
        }

        /**
         * Returns the aggregated poll-vote tallies.
         *
         * @return an unmodifiable {@link List} of {@link PollVote}; never {@code null}, empty when absent
         */
        public List<PollVote> pollVotes() {
            return pollVotes;
        }

        /**
         * Returns the {@code <forwards_count count>} value.
         *
         * @return an {@link OptionalLong} carrying the forwards count, or empty when the element is absent
         */
        public OptionalLong forwardsCount() {
            return forwardsCount == null ? OptionalLong.empty() : OptionalLong.of(forwardsCount);
        }

        /**
         * Returns the {@code <responses_count count>} value.
         *
         * @return an {@link OptionalLong} carrying the responses count, or empty when the element is absent
         */
        public OptionalLong responsesCount() {
            return responsesCount == null ? OptionalLong.empty() : OptionalLong.of(responsesCount);
        }

        /**
         * Returns the resolved {@code <views_count count>} value.
         *
         * @return an {@link OptionalLong} carrying the view count, or empty when no applicable element is present
         */
        public OptionalLong viewsCount() {
            return viewsCount == null ? OptionalLong.empty() : OptionalLong.of(viewsCount);
        }

        /**
         * Parses a {@link NewsletterMessage} from a {@code <message>}
         * {@link Stanza}.
         * Returns {@link Optional#empty()} when the description is not
         * {@code message}, the {@code server_id} attribute is missing or
         * outside the {@code [99, 2147476647]} range, or the optional
         * {@code t} attribute is negative.
         *
         * @param messageStanza the source {@link Stanza}; never {@code null}
         * @return an {@link Optional} carrying the parsed entry, or empty on no-match
         * @throws NullPointerException if {@code messageStanza} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterMessageHistoryWithAddOnsMixin",
                exports = "parseNewsletterMessageHistoryWithAddOnsMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<NewsletterMessage> of(Stanza messageStanza) {
            Objects.requireNonNull(messageStanza, "messageStanza cannot be null");
            if (!messageStanza.hasDescription("message")) {
                return Optional.empty();
            }
            var stanzaId = messageStanza.getAttributeAsString("id").orElse(null);
            var serverIdOpt = messageStanza.getAttributeAsLong("server_id");
            if (serverIdOpt.isEmpty()) {
                return Optional.empty();
            }
            var serverId = serverIdOpt.getAsLong();
            if (serverId < 99 || serverId > 2147476647L) {
                return Optional.empty();
            }
            Long timestamp = null;
            var tOpt = messageStanza.getAttributeAsLong("t");
            if (tOpt.isPresent()) {
                var tv = tOpt.getAsLong();
                if (tv < 0) {
                    return Optional.empty();
                }
                timestamp = tv;
            }
            var fromSelf = messageStanza.hasAttribute("is_sender", "true");
            return Optional.of(new NewsletterMessage(stanzaId, serverId, timestamp, fromSelf, messageStanza));
        }

        /**
         * Boxes an {@link OptionalLong} into a nullable {@link Long}.
         *
         * @param value the optional value
         * @return the boxed value, or {@code null} when empty
         */
        private static Long boxedLong(OptionalLong value) {
            return value.isPresent() ? value.getAsLong() : null;
        }

        /**
         * Classifies the content variant of a {@code <message>} stanza in
         * the relay's documented first-match order.
         * The order mirrors the wire disjunction: question, edit, question
         * reply, revoke, text, media, poll creation, quiz creation, poll
         * result snapshot, WAMO-empty. A variant that needs a payload
         * (everything except revoke and WAMO-empty) requires a
         * {@code <plaintext>} carrying {@code [1, 1048576]} bytes; a media
         * variant additionally requires the {@code mediatype} attribute.
         * Returns {@code null} when no variant matches, which marks an
         * add-on-only update.
         *
         * @param message       the source {@code <message>} stanza
         * @param plaintextNode  the {@code <plaintext>} child, or {@code null} when absent
         * @param plaintext      the validated payload bytes, or {@code null} when absent or out of range
         * @param mediaType      the {@code mediatype} attribute, or {@code null} when absent
         * @param meta           the {@code <meta>} child, or {@code null} when absent
         * @return the matched {@link Variant}, or {@code null} for an add-on-only update
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterMessageHistoryContent",
                exports = "parseNewsletterMessageHistoryContent",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private static Variant classifyContent(Stanza message, Stanza plaintextNode, byte[] plaintext, String mediaType, Stanza meta) {
            var edit = message.getAttributeAsString("edit").orElse(null);
            var type = message.getAttributeAsString("type").orElse(null);
            var questionType = meta == null ? null : meta.getAttributeAsString("questiontype").orElse(null);
            var pollType = meta == null ? null : meta.getAttributeAsString("polltype").orElse(null);
            var wamoSub = meta != null && "true".equals(meta.getAttributeAsString("is_wamo_sub").orElse(null));
            var textOk = "text".equals(type) && plaintext != null;
            var mediaOk = "media".equals(type) && mediaType != null && plaintext != null;
            var textOrMedia = textOk || mediaOk;
            if ("question".equals(questionType) && textOrMedia) {
                return Variant.QUESTION;
            }
            if ("3".equals(edit) && textOrMedia) {
                return Variant.EDIT;
            }
            if ("reply".equals(questionType) && textOrMedia) {
                return Variant.QUESTION_REPLY;
            }
            if ("8".equals(edit) && "text".equals(type) && plaintextNode != null) {
                return Variant.REVOKE;
            }
            if (textOk) {
                return Variant.TEXT;
            }
            if (mediaOk) {
                return Variant.MEDIA;
            }
            if ("poll".equals(type) && plaintext != null) {
                if ("creation".equals(pollType)) {
                    return Variant.POLL_CREATION;
                }
                if ("quiz_creation".equals(pollType)) {
                    return Variant.QUIZ_CREATION;
                }
                if ("result_snapshot".equals(pollType)) {
                    return Variant.POLL_RESULT_SNAPSHOT;
                }
            }
            if (wamoSub && plaintextNode != null) {
                return Variant.WAMO_EMPTY;
            }
            return null;
        }

        /**
         * Parses the {@code <reactions>} add-on into an ordered
         * {@code emoji -> count} map.
         * Returns {@code null} when the {@code <reactions>} element is absent
         * so the caller can distinguish absent (clear on apply) from a
         * present but empty element. Each {@code <reaction>} contributes its
         * {@code code} keyed to its {@code count} when the count is at least
         * one; later duplicates for the same code overwrite earlier ones.
         *
         * @param message the source {@code <message>} stanza
         * @return an unmodifiable {@code emoji -> count} map, or {@code null} when the element is absent
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterReactionsMixin",
                exports = "parseNewsletterReactionsMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private static Map<String, Long> parseReactions(Stanza message) {
            var reactionsNode = message.getChild("reactions").orElse(null);
            if (reactionsNode == null) {
                return null;
            }
            var tally = new LinkedHashMap<String, Long>();
            for (var reaction : reactionsNode.getChildren("reaction")) {
                var code = reaction.getAttributeAsString("code").orElse(null);
                var count = reaction.getAttributeAsLong("count");
                if (code == null || count.isEmpty() || count.getAsLong() < 1) {
                    continue;
                }
                tally.put(code, count.getAsLong());
            }
            return Collections.unmodifiableMap(tally);
        }

        /**
         * Parses the {@code <votes>} add-on into a list of {@link PollVote}
         * tallies.
         * Each {@code <vote>} contributes its 32-byte content hash keyed to
         * its {@code count} when the count is at least one and the content is
         * exactly 32 bytes; malformed entries are skipped.
         *
         * @param message the source {@code <message>} stanza
         * @return an unmodifiable {@link List} of tallies; never {@code null}, empty when absent
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterPollVotesMixin",
                exports = "parseNewsletterPollVotesMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private static List<PollVote> parsePollVotes(Stanza message) {
            var votesNode = message.getChild("votes").orElse(null);
            if (votesNode == null) {
                return List.of();
            }
            var votes = new ArrayList<PollVote>();
            for (var vote : votesNode.getChildren("vote")) {
                var count = vote.getAttributeAsLong("count");
                if (count.isEmpty() || count.getAsLong() < 1) {
                    continue;
                }
                var hash = vote.toContentBytes().filter(bytes -> bytes.length == 32).orElse(null);
                if (hash == null) {
                    continue;
                }
                votes.add(new PollVote(hash, count.getAsLong()));
            }
            return List.copyOf(votes);
        }

        /**
         * Parses a flat {@code count} add-on child into a nullable
         * {@link Long}.
         * Used for both {@code <forwards_count>} and {@code <responses_count>},
         * whose only payload is a non-negative {@code count} attribute.
         *
         * @param message          the source {@code <message>} stanza
         * @param childDescription the add-on child description
         * @return the parsed count, or {@code null} when the element is absent or the count is negative
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterForwardsCountMixin",
                exports = "parseNewsletterForwardsCountMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterResponsesCountMixin",
                exports = "parseNewsletterResponsesCountMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private static Long parseCount(Stanza message, String childDescription) {
            var node = message.getChild(childDescription).orElse(null);
            if (node == null) {
                return null;
            }
            var count = node.getAttributeAsLong("count");
            return count.isPresent() && count.getAsLong() >= 0 ? count.getAsLong() : null;
        }

        /**
         * Resolves the {@code <views_count>} add-on to a single count.
         * When one or more {@code <views_count>} children are present, the
         * one carrying {@code type="views"} is preferred, then the one
         * carrying no {@code type} attribute (the deprecated shape); any
         * other {@code type} is ignored. Returns {@code null} when no
         * applicable element is present so the caller preserves the stored
         * view count.
         *
         * @param message the source {@code <message>} stanza
         * @return the resolved non-negative view count, or {@code null} when absent
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterViewsCountViewsOrDeprecatedMixinGroup",
                exports = "parseNewsletterViewsCountViewsOrDeprecatedMixinGroup",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private static Long parseViewsCount(Stanza message) {
            var viewsCounts = message.getChildren("views_count");
            if (viewsCounts.isEmpty()) {
                return null;
            }
            Stanza preferred = null;
            for (var candidate : viewsCounts) {
                if (candidate.hasAttribute("type", "views")) {
                    preferred = candidate;
                    break;
                }
            }
            if (preferred == null) {
                for (var candidate : viewsCounts) {
                    if (candidate.getAttributeAsString("type").isEmpty()) {
                        preferred = candidate;
                        break;
                    }
                }
            }
            if (preferred == null) {
                return null;
            }
            var count = preferred.getAttributeAsLong("count");
            return count.isPresent() && count.getAsLong() >= 0 ? count.getAsLong() : null;
        }

        /**
         * Enumerates the content variants a live-updates {@code <message>}
         * can carry, in the relay's documented first-match order.
         */
        public enum Variant {
            /**
             * A newsletter question post ({@code <meta questiontype="question">}).
             */
            QUESTION,
            /**
             * An admin edit of an existing message ({@code edit="3"}).
             */
            EDIT,
            /**
             * A reply to a newsletter question ({@code <meta questiontype="reply">}).
             */
            QUESTION_REPLY,
            /**
             * An admin revoke of an existing message ({@code edit="8"}).
             */
            REVOKE,
            /**
             * A plain text message ({@code type="text"}).
             */
            TEXT,
            /**
             * A media message ({@code type="media"} with a {@code mediatype}).
             */
            MEDIA,
            /**
             * A poll creation ({@code type="poll"}, {@code <meta polltype="creation">}).
             */
            POLL_CREATION,
            /**
             * A quiz creation ({@code type="poll"}, {@code <meta polltype="quiz_creation">}); applied as a poll creation.
             */
            QUIZ_CREATION,
            /**
             * A poll result snapshot ({@code type="poll"}, {@code <meta polltype="result_snapshot">}).
             */
            POLL_RESULT_SNAPSHOT,
            /**
             * A WAMO-subscription placeholder carrying no usable payload.
             */
            WAMO_EMPTY
        }

        /**
         * Represents one aggregated poll-vote tally carried by the
         * {@code <votes>} add-on.
         *
         * @param hash  the 32-byte poll-option content hash; never {@code null}
         * @param count the aggregated vote count for the option
         */
        public record PollVote(byte[] hash, long count) {
        }

        /**
         * Compares two entries for value equality on every field.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link NewsletterMessage} with equal field values
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (NewsletterMessage) obj;
            return this.serverId == that.serverId
                    && this.fromSelf == that.fromSelf
                    && Objects.equals(this.stanzaId, that.stanzaId)
                    && Objects.equals(this.timestamp, that.timestamp)
                    && Objects.equals(this.raw, that.raw);
        }

        /**
         * Returns the hash code derived from every field.
         *
         * @return the combined hash of every field
         */
        @Override
        public int hashCode() {
            return Objects.hash(stanzaId, serverId, timestamp, fromSelf, raw);
        }

        /**
         * Returns a debug representation including the typed fields.
         *
         * @return a record-like rendering of this entry, excluding the underlying {@link Stanza} for brevity
         */
        @Override
        public String toString() {
            return "SmaxNewslettersLiveUpdatesNotificationResponse.NewsletterMessage[stanzaId=" + stanzaId
                    + ", serverId=" + serverId
                    + ", timestamp=" + timestamp
                    + ", fromSelf=" + fromSelf + ']';
        }
    }
}
