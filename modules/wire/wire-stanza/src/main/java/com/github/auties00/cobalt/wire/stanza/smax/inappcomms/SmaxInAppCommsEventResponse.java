package com.github.auties00.cobalt.wire.stanza.smax.inappcomms;

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
 * Closes the family of inbound reply variants to a
 * {@link SmaxInAppCommsEventRequest}.
 *
 * <p>The hierarchy permits {@link Success} (the relay accepted the report) and
 * {@link Error} (the relay rejected or dropped it). Both variants are terminal:
 * the WA Web {@code WAWebJob*QuickPromotion} call sites log an {@link Error} but
 * never retry.
 */
public sealed interface SmaxInAppCommsEventResponse extends SmaxStanza.Response
        permits SmaxInAppCommsEventResponse.Success, SmaxInAppCommsEventResponse.Error {

    /**
     * Tries each {@link SmaxInAppCommsEventResponse} variant in priority order
     * and returns the first that parses cleanly.
     *
     * <p>The {@link Success} variant is tried first; on a miss the
     * {@link Error} variant absorbs both the {@code 408} client-range timeout
     * and the {@code 500}/{@code 503} server-range rejections. Returns an empty
     * {@link Optional} when no documented variant matches the stanza shape.
     *
     * @param stanza the inbound IQ stanza received from the relay; never
     *             {@code null}
     * @param request the original outbound stanza, used to validate echoed
     *                identifiers; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no
     *         documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInAppCommsEventRPC",
            exports = "sendEventRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxInAppCommsEventResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        return Error.of(stanza, request);
    }

    /**
     * Models the successful reply variant in which the relay accepted the
     * event report.
     *
     * <p>The variant carries no payload beyond the {@code <iq type="result">}
     * envelope echo; the {@code <event>} body the request supplied is not
     * echoed back.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInInAppCommsEventResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInInAppCommsIQResultResponseMixin")
    final class Success implements SmaxInAppCommsEventResponse {
        /**
         * Constructs a successful reply.
         *
         * <p>Instances are produced by {@link #of(Stanza, Stanza)} once the
         * envelope shape has been validated.
         */
        public Success() {
        }

        /**
         * Parses a {@link Success} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not an
         * {@code <iq type="result">} that echoes the original request's
         * {@code id} and {@code to}.
         *
         * @implNote
         * This implementation delegates the envelope shape check to
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, which folds
         * the {@code id}/{@code from}/{@code type} echo predicates into a
         * single call.
         *
         * @param stanza the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInInAppCommsEventResponseSuccess",
                exports = "parseEventResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Returns whether the given object is also a {@link Success}.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash code; the {@link Success} variant carries no
         * fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns a debug-friendly textual representation of this variant.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxInAppCommsEventResponse.Success[]";
        }
    }

    /**
     * Models the error reply variant in which the relay rejected or dropped
     * the event report.
     *
     * <p>The InAppComms domain documents three reply codes:
     * {@code 408 request-timeout} (the client-range timeout envelope),
     * {@code 500 internal-server-error}, and {@code 503 service-unavailable}
     * (both server-range envelopes). All three collapse into a single
     * {@code (errorCode, errorText)} pair; consumers that need code-specific
     * handling switch on {@link #errorCode()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInInAppCommsEventResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInInAppCommsEventErrorTypes")
    final class Error implements SmaxInAppCommsEventResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text supplied by the relay, or
         * {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs an error reply.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be
         *                  {@code null}
         */
        public Error(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code echoed by the relay.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when
         *         the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses an {@link Error} variant from the given inbound stanza.
         *
         * <p>The server-range error envelope is tried first; on a miss a
         * client-range envelope is accepted only when its code is {@code 408},
         * so unrelated {@code 4xx} replies are not silently absorbed.
         *
         * @implNote
         * This implementation reuses
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} and
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} rather
         * than duplicating the envelope-shape predicate; the priority ordering
         * matches WA Web's combined error disjunction.
         *
         * @param stanza the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInInAppCommsEventResponseError",
                exports = "parseEventResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Error> of(Stanza stanza, Stanza request) {
            var serverEnvelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (serverEnvelope != null) {
                return Optional.of(new Error(serverEnvelope.code(), serverEnvelope.text()));
            }
            var clientEnvelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (clientEnvelope != null && clientEnvelope.code() == 408) {
                return Optional.of(new Error(clientEnvelope.code(), clientEnvelope.text()));
            }
            return Optional.empty();
        }

        /**
         * Returns whether the given object is an {@link Error} with an equal
         * code and text.
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
            var that = (Error) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug-friendly textual representation of this variant.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxInAppCommsEventResponse.Error[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
