package com.github.auties00.cobalt.wire.stanza.smax.biz;

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
 * The sealed family of inbound reply variants produced by the relay in response to a
 * {@link SmaxUpdatePreferenceRequest}.
 * Backs the biz user-feedback preference flow that records a per-contact feedback action (block, unblock,
 * allow, report) on a business chat. The three variants split the wire outcome into {@link Success} (relay
 * accepted the write), {@link ClientError} (relay returned a {@code 4xx} rejection envelope) and
 * {@link ServerError} (relay returned a transient {@code 5xx} failure envelope).
 *
 * @implSpec
 * Permitted variants are exactly {@link Success}, {@link ClientError}, and {@link ServerError}; new wire
 * outcomes must be added here and to {@link #of(Stanza, Stanza)} in priority order.
 *
 * @implNote
 * This implementation tries each variant in priority order via {@link #of(Stanza, Stanza)} and returns the first
 * successful parse.
 */
public sealed interface SmaxUpdatePreferenceResponse extends SmaxStanza.Response
        permits SmaxUpdatePreferenceResponse.Success, SmaxUpdatePreferenceResponse.ClientError, SmaxUpdatePreferenceResponse.ServerError {

    /**
     * Tries each {@link SmaxUpdatePreferenceResponse} variant in priority order and returns the first that
     * parses cleanly.
     * Invoked by the smax reply pump after dispatching a {@link SmaxUpdatePreferenceRequest}. The priority
     * order ({@link Success} then {@link ClientError} then {@link ServerError}) ensures that a malformed
     * {@code Success} stanza falls through to {@link ClientError} rather than masking an error. An unrecognised
     * stanza shape yields {@link Optional#empty()}.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza, used to validate echoed identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented
     *         variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBizMsgUserFeedbackUpdatePreferenceRPC",
            exports = "sendUpdatePreferenceRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxUpdatePreferenceResponse> of(Stanza stanza, Stanza request) {
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
     * The {@code Success} reply variant signalling that the relay accepted the user-feedback preference write.
     * Projected by {@link SmaxUpdatePreferenceResponse#of(Stanza, Stanza)} when the relay returns the documented
     * bare {@code <iq type="result"/>} envelope; carries no payload because the success wire form propagates
     * no fields.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceResponseSuccess")
    final class Success implements SmaxUpdatePreferenceResponse {
        /**
         * Constructs a new successful reply.
         * Takes no arguments because the wire form carries no projected payload.
         */
        public Success() {
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         * Validates the IQ result envelope and produces a payload-free instance on success, or
         * {@link Optional#empty()} when the stanza does not match the success schema.
         *
         * @implNote
         * This implementation enforces the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} envelope
         * check before constructing the instance.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the
         *         success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceResponseSuccess",
                exports = "parseUpdatePreferenceResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this reply with another object for equality.
         * Every {@link Success} instance is equal to every other, since the variant carries no fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} instance
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash code shared by all {@link Success} instances.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns a debug representation of this payload-free reply.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxUpdatePreferenceResponse.Success[]";
        }
    }

    /**
     * The {@code ClientError} reply variant carrying a documented {@code 4xx} preference-update rejection.
     * Surfaced when the relay rejected the request as malformed, unauthorised, rate-limited, or not-acceptable
     * for the active user; the caller returns the {@code (errorCode, errorText)} pair to the UI rather than
     * retrying.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceResponseInvalidRequest")
    @WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceReqErrors")
    final class ClientError implements SmaxUpdatePreferenceResponse {
        /**
         * The numeric server-side error code in the {@code 4xx} range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         * Admits the full {@code 4xx} range as a catch-all, returning {@link Optional#empty()} when the stanza
         * does not match the client-error schema.
         *
         * @implNote
         * This implementation routes the {@code <iq>} and {@code <error>} extraction through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the
         *         client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceResponseInvalidRequest",
                exports = "parseUpdatePreferenceResponseInvalidRequest",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another object for equality.
         * Two replies are equal when both the {@code errorCode} and the {@code errorText} match.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with the same code and text
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
         * Returns a hash code derived from the {@code errorCode} and the {@code errorText}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation listing the {@code errorCode} and the {@code errorText}.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxUpdatePreferenceResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code ServerError} reply variant carrying a transient {@code 5xx} relay failure.
     * Surfaced when the relay returned a transient internal failure while processing the preference write; the
     * caller can re-issue the request with backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceResponseServerError")
    @WhatsAppWebModule(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceServerErrors")
    final class ServerError implements SmaxUpdatePreferenceResponse {
        /**
         * The numeric server-side error code in the {@code 5xx} range.
         */
        private final int errorCode;

        /**
         * The human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         * Returns {@link Optional#empty()} for any stanza outside the {@code 5xx} range.
         *
         * @implNote
         * This implementation delegates the {@code 5xx} range check to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the
         *         server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizMsgUserFeedbackUpdatePreferenceResponseServerError",
                exports = "parseUpdatePreferenceResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with another object for equality.
         * Two replies are equal when both the {@code errorCode} and the {@code errorText} match.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with the same code and text
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
         * Returns a hash code derived from the {@code errorCode} and the {@code errorText}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation listing the {@code errorCode} and the {@code errorText}.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxUpdatePreferenceResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
