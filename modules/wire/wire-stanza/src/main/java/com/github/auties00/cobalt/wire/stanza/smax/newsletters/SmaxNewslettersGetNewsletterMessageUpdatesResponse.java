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
 * Holds the inbound reply variants for a {@link SmaxNewslettersGetNewsletterMessageUpdatesRequest}.
 *
 * <p>The reply is one of {@link Success}, {@link ClientError}, or {@link ServerError}; both error
 * variants carry a relay-side status code that the configured error handler resolves.</p>
 */
public sealed interface SmaxNewslettersGetNewsletterMessageUpdatesResponse extends SmaxStanza.Response
        permits SmaxNewslettersGetNewsletterMessageUpdatesResponse.Success, SmaxNewslettersGetNewsletterMessageUpdatesResponse.ClientError, SmaxNewslettersGetNewsletterMessageUpdatesResponse.ServerError {

    /**
     * Dispatches the inbound IQ stanza to the first matching variant parser.
     *
     * <p>Tries {@link Success}, then {@link ClientError}, then {@link ServerError} in order and
     * surfaces the first that parses; an empty {@link Optional} means the stanza did not match any
     * documented variant.</p>
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza, used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no
     *         documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxNewslettersGetNewsletterMessageUpdatesRPC",
            exports = "sendGetNewsletterMessageUpdatesRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxNewslettersGetNewsletterMessageUpdatesResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the delta-of-message-updates batch returned by the relay.
     *
     * <p>The batch reuses the same
     * {@link SmaxNewslettersGetNewsletterMessagesResponse.NewsletterMessage} shape as the full
     * history fetch, so the same downstream add-on parser handles both.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterMessageUpdatesResponseSuccess")
    final class Success implements SmaxNewslettersGetNewsletterMessageUpdatesResponse {
        /**
         * The optional newsletter {@link Jid} echoed on the {@code <messages>} block.
         */
        private final Jid newsletterJid;

        /**
         * The optional unix-second timestamp echoed by the relay.
         */
        private final Long timestamp;

        /**
         * The list of message-update entries returned by the relay.
         */
        private final List<SmaxNewslettersGetNewsletterMessagesResponse.NewsletterMessage> messages;

        /**
         * Constructs a new successful reply.
         *
         * <p>Both {@code newsletterJid} and {@code timestamp} are optional because the relay only
         * echoes them when the corresponding attributes were present on the wire.</p>
         *
         * @param newsletterJid the optional echoed newsletter {@link Jid}; may be {@code null}
         * @param timestamp     the optional echoed unix-second timestamp; may be {@code null}
         * @param messages      the message-update entries; never {@code null} (empty allowed)
         */
        public Success(Jid newsletterJid,
                       Long timestamp,
                       List<SmaxNewslettersGetNewsletterMessagesResponse.NewsletterMessage> messages) {
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
         * Returns the message-update entries.
         *
         * @return an unmodifiable {@link List} of entries; never {@code null}
         */
        public List<SmaxNewslettersGetNewsletterMessagesResponse.NewsletterMessage> messages() {
            return messages;
        }

        /**
         * Tries to parse a {@link Success} from the inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the IQ envelope fails
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, the {@code <message_updates>} or
         * {@code <messages>} envelopes are missing, the {@code t} attribute is negative, or any
         * nested {@code <message>} child fails its own
         * {@link SmaxNewslettersGetNewsletterMessagesResponse.NewsletterMessage#of(Stanza)} parse.</p>
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound stanza; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterMessageUpdatesResponseSuccess",
                exports = "parseGetNewsletterMessageUpdatesResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var messageUpdates = stanza.getChild("message_updates").orElse(null);
            if (messageUpdates == null) {
                return Optional.empty();
            }
            var messagesNode = messageUpdates.getChild("messages").orElse(null);
            if (messagesNode == null) {
                return Optional.empty();
            }
            var jid = messagesNode.getAttributeAsJid("jid").orElse(null);
            Long timestamp = null;
            var tOpt = messagesNode.getAttributeAsLong("t");
            if (tOpt.isPresent()) {
                var tv = tOpt.getAsLong();
                if (tv < 0) {
                    return Optional.empty();
                }
                timestamp = tv;
            }
            var entries = new ArrayList<SmaxNewslettersGetNewsletterMessagesResponse.NewsletterMessage>();
            for (var messageNode : messagesNode.getChildren("message")) {
                var entry = SmaxNewslettersGetNewsletterMessagesResponse.NewsletterMessage.of(messageNode)
                        .orElse(null);
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
            return "SmaxNewslettersGetNewsletterMessageUpdatesResponse.Success[newsletterJid="
                    + newsletterJid + ", timestamp=" + timestamp
                    + ", messages=" + messages + ']';
        }
    }

    /**
     * Carries a relay-side client-rejection of the request.
     *
     * <p>Holds the numeric error code and optional text the relay returned for a malformed,
     * unauthorized, rate-limited, or otherwise rejected request.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterMessageUpdatesResponseClientError")
    final class ClientError implements SmaxNewslettersGetNewsletterMessageUpdatesResponse {
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
         * <p>The text is optional because not every relay-side error carries a human-readable
         * message; {@link #errorCode()} is always present.</p>
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
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which
         * validates the {@code <iq type="error">} envelope and extracts the error code and text.</p>
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterMessageUpdatesResponseClientError",
                exports = "parseGetNewsletterMessageUpdatesResponseClientError",
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
            return "SmaxNewslettersGetNewsletterMessageUpdatesResponse.ClientError[errorCode="
                    + errorCode + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a transient relay-side failure of the request.
     *
     * <p>Holds the numeric error code and optional text the relay returned for an internal-server
     * error; the variant itself performs no retry.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterMessageUpdatesResponseServerError")
    final class ServerError implements SmaxNewslettersGetNewsletterMessageUpdatesResponse {
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
         * <p>The text is optional because not every relay-side error carries a human-readable
         * message.</p>
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
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, which
         * validates the {@code <iq type="error">} envelope for an internal-server-error mixin.</p>
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterMessageUpdatesResponseServerError",
                exports = "parseGetNewsletterMessageUpdatesResponseServerError",
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
            return "SmaxNewslettersGetNewsletterMessageUpdatesResponse.ServerError[errorCode="
                    + errorCode + ", errorText=" + errorText + ']';
        }
    }
}
