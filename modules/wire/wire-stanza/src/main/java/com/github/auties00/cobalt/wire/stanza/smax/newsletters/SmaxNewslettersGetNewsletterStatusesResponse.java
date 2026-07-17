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
 * Represents the sealed family of inbound reply variants for a
 * {@link SmaxNewslettersGetNewsletterStatusesRequest}.
 * Consumers pattern-match on the three permitted variants
 * ({@link Success}, {@link ClientError}, {@link ServerError}); only
 * {@link Success#statuses()} carries the requested status slice, while
 * the two error variants describe a relay-side rejection or failure.
 */
public sealed interface SmaxNewslettersGetNewsletterStatusesResponse extends SmaxStanza.Response
        permits SmaxNewslettersGetNewsletterStatusesResponse.Success, SmaxNewslettersGetNewsletterStatusesResponse.ClientError, SmaxNewslettersGetNewsletterStatusesResponse.ServerError {

    /**
     * Dispatches the inbound IQ stanza to the first matching variant parser.
     * Tries {@link Success#of(Stanza, Stanza)}, then
     * {@link ClientError#of(Stanza, Stanza)}, then
     * {@link ServerError#of(Stanza, Stanza)}, in that order, and returns the
     * first variant that matches the stanza shape.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza, used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxNewslettersGetNewsletterStatusesRPC",
            exports = "sendGetNewsletterStatusesRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxNewslettersGetNewsletterStatusesResponse> of(Stanza stanza, Stanza request) {
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
     * Represents the variant that carries the requested status slice.
     * Each entry's underlying {@link Stanza} exposes the view-count and
     * reaction add-on children, which the consumer projects onto the
     * admin status panel.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterStatusesResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersNewsletterStatusResponsePayloadMixin")
    final class Success implements SmaxNewslettersGetNewsletterStatusesResponse {
        /**
         * Holds the optional newsletter {@link Jid} echoed by the relay.
         */
        private final Jid newsletterJid;

        /**
         * Holds the optional unix-second timestamp echoed by the relay.
         */
        private final Long timestamp;

        /**
         * Holds the list of status entries returned by the relay.
         */
        private final List<NewsletterStatus> statuses;

        /**
         * Constructs a new successful reply.
         * The {@code newsletterJid} and {@code timestamp} are optional
         * because the relay only echoes them when the corresponding
         * attributes were present on the wire.
         *
         * @param newsletterJid the optional echoed {@link Jid}; may be {@code null}
         * @param timestamp     the optional echoed unix-second timestamp; may be {@code null}
         * @param statuses      the status entries; never {@code null} (empty allowed)
         */
        public Success(Jid newsletterJid, Long timestamp, List<NewsletterStatus> statuses) {
            this.newsletterJid = newsletterJid;
            this.timestamp = timestamp;
            this.statuses = List.copyOf(Objects.requireNonNullElse(statuses, List.of()));
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
         * Returns the status entries.
         *
         * @return an unmodifiable {@link List} of entries; never {@code null}
         */
        public List<NewsletterStatus> statuses() {
            return statuses;
        }

        /**
         * Parses a {@link Success} from the inbound stanza.
         * Returns {@link Optional#empty()} when the IQ envelope fails
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, the
         * relay's {@code from} attribute is not {@link Jid#userServer()},
         * the {@code <statuses>} envelope is missing, the {@code t}
         * attribute is negative, or any {@code <status>} fails its own
         * {@link NewsletterStatus#of(Stanza)} parse.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterStatusesResponseSuccess",
                exports = "parseGetNewsletterStatusesResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("from", Jid.userServer().toString())) {
                return Optional.empty();
            }
            var statusesNode = stanza.getChild("statuses").orElse(null);
            if (statusesNode == null) {
                return Optional.empty();
            }
            var jid = statusesNode.getAttributeAsJid("jid").orElse(null);
            Long timestamp = null;
            var tOpt = statusesNode.getAttributeAsLong("t");
            if (tOpt.isPresent()) {
                var tv = tOpt.getAsLong();
                if (tv < 0) {
                    return Optional.empty();
                }
                timestamp = tv;
            }
            var entries = new ArrayList<NewsletterStatus>();
            for (var statusNode : statusesNode.getChildren("status")) {
                var entry = NewsletterStatus.of(statusNode).orElse(null);
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
         * @return {@code true} when {@code obj} is a {@link Success} with equal field values
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
                    && Objects.equals(this.statuses, that.statuses);
        }

        /**
         * Returns the hash code derived from every field.
         *
         * @return the combined hash of every field
         */
        @Override
        public int hashCode() {
            return Objects.hash(newsletterJid, timestamp, statuses);
        }

        /**
         * Returns a debug representation including every field.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterStatusesResponse.Success[newsletterJid="
                    + newsletterJid + ", timestamp=" + timestamp
                    + ", statuses=" + statuses + ']';
        }
    }

    /**
     * Represents one typed projection of a {@code <status>} entry inside
     * the {@code <statuses>} envelope.
     * Carries only the underlying {@link Stanza} because the variable shape
     * of the add-on children (view-count counters, reaction counts per
     * emoji) defeats a static schema; the consumer drills into the raw
     * stanza via the add-on helpers.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersStatusNewsletterHistoryWithAddOnsMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersStatusNewsletterHistoryMixin")
    final class NewsletterStatus {
        /**
         * Holds the underlying {@link Stanza} carrying the variable-shape
         * add-on children.
         */
        private final Stanza raw;

        /**
         * Constructs a new newsletter-status projection.
         *
         * @param raw the underlying {@link Stanza}; never {@code null}
         * @throws NullPointerException if {@code raw} is {@code null}
         */
        public NewsletterStatus(Stanza raw) {
            this.raw = Objects.requireNonNull(raw, "raw cannot be null");
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
         * Parses a {@link NewsletterStatus} from a {@code <status>}
         * {@link Stanza}.
         * Returns {@link Optional#empty()} when the stanza description is
         * not {@code status}; no further validation is applied, so the
         * consumer decides how strict to be on the add-on children.
         *
         * @param statusStanza the source {@link Stanza}; never {@code null}
         * @return an {@link Optional} carrying the parsed entry, or empty when the description does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersStatusNewsletterHistoryWithAddOnsMixin",
                exports = "parseStatusNewsletterHistoryWithAddOnsMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<NewsletterStatus> of(Stanza statusStanza) {
            if (!statusStanza.hasDescription("status")) {
                return Optional.empty();
            }
            return Optional.of(new NewsletterStatus(statusStanza));
        }

        /**
         * Compares two entries for value equality on {@link #raw()}.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link NewsletterStatus} wrapping an equal {@link Stanza}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (NewsletterStatus) obj;
            return Objects.equals(this.raw, that.raw);
        }

        /**
         * Returns the hash code derived from {@link #raw()}.
         *
         * @return the {@link Stanza}'s hash
         */
        @Override
        public int hashCode() {
            return Objects.hash(raw);
        }

        /**
         * Returns a debug representation including the raw stanza.
         *
         * @return a record-like rendering of this entry
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterStatusesResponse.NewsletterStatus[raw=" + raw + ']';
        }
    }

    /**
     * Represents the variant carrying a relay-side client-rejection.
     * Every documented sub-error name collapses onto the same numeric
     * {@link #errorCode()}, plus a fallback {@code code=0} for any
     * unhandled sub-error name.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterStatusesResponseClientError")
    final class ClientError implements SmaxNewslettersGetNewsletterStatusesResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the relay.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         * The text is optional because not every sub-error carries a
         * human-readable message.
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
         * Parses a {@link ClientError} from the inbound stanza by
         * delegating to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterStatusesResponseClientError",
                exports = "parseGetNewsletterStatusesResponseClientError",
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
         * @return {@code true} when {@code obj} is a {@link ClientError} with equal {@link #errorCode()} and {@link #errorText()}
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
            return "SmaxNewslettersGetNewsletterStatusesResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the variant carrying a transient relay-side failure.
     * Mirrors {@link ClientError} for relay-side internal failures; a
     * consuming layer may retry through its back-off helper.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterStatusesResponseServerError")
    final class ServerError implements SmaxNewslettersGetNewsletterStatusesResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the relay.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         * The text is optional because not every sub-error carries a
         * human-readable message.
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
         * Parses a {@link ServerError} from the inbound stanza by
         * delegating to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterStatusesResponseServerError",
                exports = "parseGetNewsletterStatusesResponseServerError",
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
         * @return {@code true} when {@code obj} is a {@link ServerError} with equal {@link #errorCode()} and {@link #errorText()}
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
            return "SmaxNewslettersGetNewsletterStatusesResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
