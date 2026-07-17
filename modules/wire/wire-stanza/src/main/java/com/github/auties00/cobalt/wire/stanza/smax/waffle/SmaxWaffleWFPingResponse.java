package com.github.auties00.cobalt.wire.stanza.smax.waffle;

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
 * Models the sealed family of inbound replies to a {@link SmaxWaffleWFPingRequest}.
 *
 * <p>The Waffle relay answers a WFPing request with exactly one of three reply shapes, each
 * captured by a permitted variant. {@link Success} carries the relay-chosen next-ping cadence and
 * lets the account-linking scheduler reschedule the following ping; the relay may lower or raise the
 * cadence to throttle individual clients. {@link ClientError} represents a rejection with a code
 * below {@code 500} (malformed, unauthorised, or unknown-state requests). {@link ServerError}
 * represents a transient relay-side failure with a code at or above {@code 500}. The three variants
 * partition the response space, so an inbound stanza matches at most one of them.
 */
public sealed interface SmaxWaffleWFPingResponse extends SmaxStanza.Response
        permits SmaxWaffleWFPingResponse.Success, SmaxWaffleWFPingResponse.ClientError, SmaxWaffleWFPingResponse.ServerError {

    /**
     * Tries each {@link SmaxWaffleWFPingResponse} variant in priority order and returns the first
     * that parses cleanly.
     *
     * <p>The inbound stanza is offered to the {@link Success} parser first, then the
     * {@link ClientError} parser, then the {@link ServerError} parser. The first parser that matches
     * wins; when none matches, the result is empty.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when none of the three
     *         parsers matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxWaffleWFPingRPC",
            exports = "sendWFPingRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxWaffleWFPingResponse> of(Stanza stanza, Stanza request) {
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
     * Represents the success reply variant, where the relay accepted the ping and surfaced the
     * next-ping cadence.
     *
     * <p>The carried {@link #pingInterval()} is the seconds-between-pings cadence the relay chose
     * for this client. The account-linking scheduler reads this value to reschedule the next ping,
     * which lets the relay throttle individual clients by lowering or raising the cadence.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleWFPingResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleIQResultResponseMixin")
    final class Success implements SmaxWaffleWFPingResponse {
        /**
         * Holds the relay-chosen seconds-between-pings cadence.
         */
        private final int pingInterval;

        /**
         * Constructs a new success projection from the relay-chosen cadence.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} once the envelope shape has been validated.
         *
         * @param pingInterval the relay-chosen cadence in seconds
         */
        public Success(int pingInterval) {
            this.pingInterval = pingInterval;
        }

        /**
         * Returns the relay-chosen ping cadence.
         *
         * @return the cadence in seconds as supplied by the relay
         */
        public int pingInterval() {
            return pingInterval;
        }

        /**
         * Tries to parse a {@link Success} variant from the inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the envelope check fails or when the
         * {@code ping_interval} child is missing or non-numeric; otherwise returns a {@link Success}
         * carrying the parsed cadence.
         *
         * @implNote
         * This implementation parses the {@code ping_interval} content as an ASCII integer via
         * {@link Integer#parseInt(String)} and treats a {@link NumberFormatException} as a no-match
         * rather than propagating it.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleWFPingResponseSuccess",
                exports = "parseWFPingResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var pingIntervalNode = stanza.getChild("ping_interval").orElse(null);
            if (pingIntervalNode == null) {
                return Optional.empty();
            }
            var pingInterval = pingIntervalNode.toContentString()
                    .map(String::trim)
                    .map(s -> {
                        try {
                            return Integer.parseInt(s);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .orElse(null);
            if (pingInterval == null) {
                return Optional.empty();
            }
            return Optional.of(new Success(pingInterval));
        }

        /**
         * Returns whether the given object is a {@link Success} with an equal ping cadence.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both cadences match
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
            return this.pingInterval == that.pingInterval;
        }

        /**
         * Returns a hash code derived from the ping cadence.
         *
         * @return the {@link Integer#hashCode(int)} of the cadence
         */
        @Override
        public int hashCode() {
            return Integer.hashCode(pingInterval);
        }

        /**
         * Returns a debug rendering of this success variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleWFPingResponse.Success[pingInterval=" + pingInterval + ']';
        }
    }

    /**
     * Represents the client-error reply variant, where the relay rejected the ping with a code
     * below {@code 500}.
     *
     * <p>This variant surfaces malformed-request, unauthorised, and unknown-state rejections from
     * the Waffle backend. Callers route {@link #errorCode()} and {@link #errorText()} through their
     * Waffle IQ error handling, which may schedule a nonce-refresh retry for the relevant rejection
     * names.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleWFPingResponseError")
    final class ClientError implements SmaxWaffleWFPingResponse {
        /**
         * Holds the numeric error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply from a code and optional text.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} once the envelope shape has been validated.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text, or {@code null} when absent
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the code as supplied by the relay
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
         * Tries to parse a {@link ClientError} variant from the inbound stanza.
         *
         * <p>The envelope and code-range check are delegated to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which matches only codes
         * below {@code 500}. Returns {@link Optional#empty()} when that check does not match.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleWFPingResponseError",
                exports = "parseWFPingResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Returns whether the given object is a {@link ClientError} with equal code and text.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both code and text match
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
         * Returns a hash code derived from the code and text.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering of this client-error variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleWFPingResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the server-error reply variant, where the relay rejected the ping with a code at or
     * above {@code 500}.
     *
     * <p>This variant indicates a transient relay-side failure. Its {@link #errorCode()} and
     * {@link #errorText()} are surfaced through the same Waffle IQ error handling as
     * {@link ClientError} for telemetry consistency.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleWFPingResponseError")
    final class ServerError implements SmaxWaffleWFPingResponse {
        /**
         * Holds the numeric error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply from a code and optional text.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} once the envelope shape has been validated.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text, or {@code null} when absent
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the code as supplied by the relay
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
         * Tries to parse a {@link ServerError} variant from the inbound stanza.
         *
         * <p>The envelope and code-range check are delegated to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, which matches only codes at
         * or above {@code 500}. Returns {@link Optional#empty()} when that check does not match.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleWFPingResponseError",
                exports = "parseWFPingResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Returns whether the given object is a {@link ServerError} with equal code and text.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both code and text match
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
         * Returns a hash code derived from the code and text.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering of this server-error variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleWFPingResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
