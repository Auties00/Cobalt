package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the inbound reply variants for a {@link SmaxNewslettersGetNewsletterMessagesRequest}.
 *
 * <p>The reply is one of {@link Success}, {@link ClientError}, or {@link ServerError}; both error
 * variants carry a relay-side status code that the configured error handler resolves and that skips
 * the local store update.</p>
 */
public sealed interface SmaxNewslettersGetNewsletterMessagesResponse extends SmaxStanza.Response
        permits SmaxNewslettersGetNewsletterMessagesResponse.Success, SmaxNewslettersGetNewsletterMessagesResponse.ClientError, SmaxNewslettersGetNewsletterMessagesResponse.ServerError {

    /**
     * Dispatches the inbound IQ stanza to the first matching variant parser.
     *
     * <p>Tries {@link Success}, then {@link ClientError}, then {@link ServerError} in order and
     * surfaces the first that parses; returns {@link Optional#empty()} when none match.</p>
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza, used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no
     *         documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxNewslettersGetNewsletterMessagesRPC",
            exports = "sendGetNewsletterMessagesRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxNewslettersGetNewsletterMessagesResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Carries the requested message slice returned by the relay.
     *
     * <p>Each entry's underlying {@link Stanza} exposes the add-on children (reactions, polls,
     * forwards, views, paid-partnership content) so the consumer can drill into them without
     * re-parsing the envelope.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterMessagesResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMessageResponsePayloadMixin")
    final class Success implements SmaxNewslettersGetNewsletterMessagesResponse {
        /**
         * The optional newsletter {@link Jid} echoed by the relay.
         */
        private final Jid newsletterJid;

        /**
         * The optional unix-second timestamp echoed by the relay.
         */
        private final Long timestamp;

        /**
         * The list of newsletter message entries returned by the relay.
         */
        private final List<NewsletterMessage> messages;

        /**
         * Constructs a new successful reply.
         *
         * <p>Both {@code newsletterJid} and {@code timestamp} are optional because the relay only
         * echoes them when the corresponding attributes were present on the wire.</p>
         *
         * @param newsletterJid the optional echoed {@link Jid}; may be {@code null}
         * @param timestamp     the optional echoed unix-second timestamp; may be {@code null}
         * @param messages      the message entries; never {@code null} (empty allowed)
         */
        public Success(Jid newsletterJid, Long timestamp, List<NewsletterMessage> messages) {
            this.newsletterJid = newsletterJid;
            this.timestamp = timestamp;
            this.messages = List.copyOf(Objects.requireNonNullElse(messages, List.of()));
        }

        /**
         * Returns the optional echoed newsletter {@link Jid}.
         *
         * @return an {@link Optional} carrying the {@link Jid}, or empty when the relay omitted it
         */
        public Optional<Jid> newsletterJid() {
            return Optional.ofNullable(newsletterJid);
        }

        /**
         * Returns the optional echoed unix-second timestamp.
         *
         * @return an {@link Optional} carrying the timestamp, or empty when the relay omitted it
         */
        public Optional<Long> timestamp() {
            return Optional.ofNullable(timestamp);
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
         * Tries to parse a {@link Success} from the inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the IQ envelope fails
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, the relay's {@code from} attribute
         * is not {@link Jid#userServer()}, the {@code <messages>} envelope is missing, the {@code t}
         * attribute is negative, or any nested {@code <message>} fails its own
         * {@link NewsletterMessage#of(Stanza)} parse.</p>
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterMessagesResponseSuccess",
                exports = "parseGetNewsletterMessagesResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("from", Jid.userServer().toString())) {
                return Optional.empty();
            }
            var messagesNode = stanza.getChild("messages").orElse(null);
            if (messagesNode == null) {
                return Optional.empty();
            }
            var jid = messagesNode.getAttributeAsJid("jid").orElse(null);
            var tOpt = messagesNode.getAttributeAsLong("t");
            Long timestamp = null;
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
            return Optional.of(new Success(jid, timestamp, entries));
        }

        /**
         * Compares two replies for value equality on every field.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} carrying equal
         *         {@link #newsletterJid()}, {@link #timestamp()}, and {@link #messages()}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.newsletterJid, that.newsletterJid)
                    && Objects.equals(this.timestamp, that.timestamp)
                    && Objects.equals(this.messages, that.messages);
        }

        /**
         * Returns the hash code derived from every field.
         *
         * @return the combined hash of {@link #newsletterJid()}, {@link #timestamp()}, and
         *         {@link #messages()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(newsletterJid, timestamp, messages);
        }

        /**
         * Returns a debug representation including every field.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterMessagesResponse.Success[newsletterJid="
                    + newsletterJid + ", timestamp=" + timestamp
                    + ", messages=" + messages + ']';
        }
    }

    /**
     * Projects one {@code <message>} entry inside the {@code <messages>} envelope.
     *
     * <p>Carries the basic shape (stanza id, server-id, timestamp, sender flag) and exposes the
     * underlying {@link Stanza} so downstream consumers can drill into the variable-shape add-on
     * children (reactions, polls, responses, forwards, views, paid-partnership content) without
     * re-parsing the envelope.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMessageHistoryWithAddOnsMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterMessageHistoryMixin")
    final class NewsletterMessage {
        /**
         * The optional client-supplied stanza id of the message.
         */
        private final String stanzaId;

        /**
         * The server-assigned monotonic message id within the newsletter.
         */
        private final long serverId;

        /**
         * The optional unix-second timestamp of the message.
         */
        private final Long timestamp;

        /**
         * Whether the message was authored by the connected client.
         */
        private final boolean fromSelf;

        /**
         * The underlying {@link Stanza} exposing the variable-shape add-on children.
         */
        private final Stanza raw;

        /**
         * Constructs a new newsletter-message projection.
         *
         * <p>The {@code stanzaId} and {@code timestamp} are optional because the relay only emits
         * them when the message carries those attributes on the wire.</p>
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
         * Returns whether the message was authored by the connected client.
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
         * Tries to parse a {@link NewsletterMessage} from a {@code <message>} {@link Stanza}.
         *
         * <p>Returns {@link Optional#empty()} when the stanza description is not {@code message}, the
         * {@code server_id} attribute is missing or outside the {@code [99, 2147476647]} range, or
         * the optional {@code t} attribute is negative.</p>
         *
         * @implNote The {@code [99, 2147476647]} bounds match the server-side validators for a
         * newsletter server-id; ids outside that window cannot occur on a well-formed wire message.
         *
         * @param messageStanza the source {@code <message>} {@link Stanza}; never {@code null}
         * @return an {@link Optional} carrying the parsed entry, or empty when the stanza fails validation
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersNewsletterMessageHistoryWithAddOnsMixin",
                exports = "parseNewsletterMessageHistoryWithAddOnsMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<NewsletterMessage> of(Stanza messageStanza) {
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
         * Compares two entries for value equality on every field.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link NewsletterMessage} carrying equal
         *         {@link #stanzaId()}, {@link #serverId()}, {@link #timestamp()}, {@link #fromSelf()},
         *         and {@link #raw()}
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
         * @return the combined hash of {@link #stanzaId()}, {@link #serverId()}, {@link #timestamp()},
         *         {@link #fromSelf()}, and {@link #raw()}
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
            return "SmaxNewslettersGetNewsletterMessagesResponse.NewsletterMessage[stanzaId="
                    + stanzaId + ", serverId=" + serverId
                    + ", timestamp=" + timestamp + ", fromSelf=" + fromSelf + ']';
        }
    }

    /**
     * Carries a relay-side client-rejection of the request.
     *
     * <p>Holds the numeric error code and optional text the relay returned for a malformed,
     * unauthorized, rate-limited, suspended, or otherwise rejected request.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterMessagesResponseClientError")
    final class ClientError implements SmaxNewslettersGetNewsletterMessagesResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text from the relay.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         *
         * <p>The text is optional because not every relay-side error carries a human-readable message.</p>
         *
         * @param errorCode the numeric error code echoed by the relay
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code echoed by the relay
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.</p>
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterMessagesResponseClientError",
                exports = "parseGetNewsletterMessagesResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares two replies for value equality on both fields.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link ClientError} carrying equal
         *         {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns the hash code derived from both fields.
         *
         * @return the combined hash of {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation including both fields.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterMessagesResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a transient relay-side failure of the request.
     *
     * <p>Holds the numeric error code and optional text the relay returned for an internal-server
     * error; the variant itself performs no retry.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterMessagesResponseServerError")
    final class ServerError implements SmaxNewslettersGetNewsletterMessagesResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text from the relay.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         *
         * <p>The text is optional because not every relay-side error carries a human-readable message.</p>
         *
         * @param errorCode the numeric error code echoed by the relay
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code echoed by the relay
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.</p>
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterMessagesResponseServerError",
                exports = "parseGetNewsletterMessagesResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares two replies for value equality on both fields.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link ServerError} carrying equal
         *         {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns the hash code derived from both fields.
         *
         * @return the combined hash of {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation including both fields.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterMessagesResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
