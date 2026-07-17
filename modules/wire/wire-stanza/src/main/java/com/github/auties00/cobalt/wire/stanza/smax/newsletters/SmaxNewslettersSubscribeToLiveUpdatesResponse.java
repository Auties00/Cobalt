package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the sealed family of inbound reply variants for a
 * {@link SmaxNewslettersSubscribeToLiveUpdatesRequest}.
 * Consumers pattern-match on the three permitted variants
 * ({@link Success}, {@link ClientError}, {@link ServerError}); only
 * {@link Success#duration()} carries the accepted subscription TTL (in
 * seconds), while the two error variants describe a relay-side
 * rejection or failure.
 */
public sealed interface SmaxNewslettersSubscribeToLiveUpdatesResponse extends SmaxStanza.Response
        permits SmaxNewslettersSubscribeToLiveUpdatesResponse.Success, SmaxNewslettersSubscribeToLiveUpdatesResponse.ClientError, SmaxNewslettersSubscribeToLiveUpdatesResponse.ServerError {

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
    @WhatsAppWebExport(moduleName = "WASmaxNewslettersSubscribeToLiveUpdatesRPC",
            exports = "sendSubscribeToLiveUpdatesRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxNewslettersSubscribeToLiveUpdatesResponse> of(Stanza stanza, Stanza request) {
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
     * Represents the variant that carries the accepted subscription TTL.
     * The TTL is expressed in seconds; consumers schedule a renewal at
     * or before {@link #duration()} to keep the live-updates feed
     * uninterrupted.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersSubscribeToLiveUpdatesResponseSuccess")
    final class Success implements SmaxNewslettersSubscribeToLiveUpdatesResponse {
        /**
         * Holds the relay-chosen TTL in seconds for this subscription.
         */
        private final int duration;

        /**
         * Constructs a new successful reply.
         * The {@code duration} is expected within {@code [30, 600]}; the
         * parser rejects values outside this range to match the relay's
         * server-side bounds.
         *
         * @param duration the relay-chosen TTL in seconds; bounded to {@code [30, 600]} by the relay
         */
        public Success(int duration) {
            this.duration = duration;
        }

        /**
         * Returns the relay-chosen subscription TTL in seconds.
         *
         * @return the TTL in seconds; bounded to {@code [30, 600]} by the relay
         */
        public int duration() {
            return duration;
        }

        /**
         * Parses a {@link Success} from the inbound stanza.
         * Returns {@link Optional#empty()} when the IQ envelope fails
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, the
         * {@code <live_updates>} child is missing, or the
         * {@code duration} attribute is outside {@code [30, 600]}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersSubscribeToLiveUpdatesResponseSuccess",
                exports = "parseSubscribeToLiveUpdatesResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var liveUpdates = stanza.getChild("live_updates").orElse(null);
            if (liveUpdates == null) {
                return Optional.empty();
            }
            var duration = liveUpdates.getAttributeAsInt("duration").orElse(-1);
            if (duration < 30 || duration > 600) {
                return Optional.empty();
            }
            return Optional.of(new Success(duration));
        }

        /**
         * Compares two replies for value equality on {@link #duration()}.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} carrying an equal {@link #duration()}
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
            return this.duration == that.duration;
        }

        /**
         * Returns the hash code derived from {@link #duration()}.
         *
         * @return the {@link Integer#hashCode(int)} of {@link #duration()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(duration);
        }

        /**
         * Returns a debug representation including the TTL.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersSubscribeToLiveUpdatesResponse.Success[duration=" + duration + ']';
        }
    }

    /**
     * Represents the variant carrying a relay-side client-rejection.
     * Every documented sub-error name collapses onto the same numeric
     * {@link #errorCode()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersSubscribeToLiveUpdatesResponseClientError")
    final class ClientError implements SmaxNewslettersSubscribeToLiveUpdatesResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersSubscribeToLiveUpdatesResponseClientError",
                exports = "parseSubscribeToLiveUpdatesResponseClientError",
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
            return "SmaxNewslettersSubscribeToLiveUpdatesResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the variant carrying a transient relay-side failure.
     * Mirrors {@link ClientError} for relay-side internal failures; a
     * consuming layer may retry through its back-off helper.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersSubscribeToLiveUpdatesResponseServerError")
    final class ServerError implements SmaxNewslettersSubscribeToLiveUpdatesResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersSubscribeToLiveUpdatesResponseServerError",
                exports = "parseSubscribeToLiveUpdatesResponseServerError",
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
            return "SmaxNewslettersSubscribeToLiveUpdatesResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
