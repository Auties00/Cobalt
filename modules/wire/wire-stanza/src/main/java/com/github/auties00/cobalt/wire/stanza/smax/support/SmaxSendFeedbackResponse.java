package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the three documented replies to a {@link SmaxSendFeedbackRequest}.
 *
 * <p>The sealed family enumerates the accepted-feedback outcome ({@link Success}), the
 * non-retryable client rejection ({@link ClientError}, typically {@code 400} or {@code 429}) and
 * the transient internal failure ({@link ServerError}, typically {@code 500}). The caller clears
 * the rating dialog on success, re-renders the form on a client error, and schedules a retry on a
 * server error.
 *
 * @implNote
 * This implementation splits WA Web's single error parser into Cobalt's {@link ClientError} (4xx)
 * and {@link ServerError} (5xx) so callers can dispatch on the outcome without re-inspecting the
 * code; parse order is success, then client error, then server error.
 */
public sealed interface SmaxSendFeedbackResponse extends SmaxStanza.Response
        permits SmaxSendFeedbackResponse.Success, SmaxSendFeedbackResponse.ClientError, SmaxSendFeedbackResponse.ServerError {

    /**
     * Parses the inbound feedback reply against each variant and returns the first that matches.
     *
     * <p>An empty result means the inbound stanza did not fit any of the three documented shapes.
     *
     * @implNote
     * This implementation short-circuits across {@link Success#of(Stanza, Stanza)},
     * {@link ClientError#of(Stanza, Stanza)} and {@link ServerError#of(Stanza, Stanza)}; no parse
     * exception is raised on a total miss.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the originating outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty on no-match
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxSupportMessageFeedbackSendFeedbackRPC",
            exports = "sendSendFeedbackRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxSendFeedbackResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the literal {@code "Success"} status of accepted feedback.
     *
     * <p>The caller clears the rating dialog and applies any local UI confirmation.
     *
     * @implNote
     * This implementation validates the {@code <result status="Success"/>} child shape; the
     * carried {@link #resultStatus()} is constant.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSupportMessageFeedbackSendFeedbackResponseSuccess")
    final class Success implements SmaxSendFeedbackResponse {
        /**
         * Holds the {@code status} attribute of the {@code <result/>} child, always the literal
         * {@code "Success"}.
         */
        private final String resultStatus;

        /**
         * Constructs an accepted-feedback reply from the parsed fields.
         *
         * @param resultStatus the status; never {@code null}
         * @throws NullPointerException if {@code resultStatus} is {@code null}
         */
        public Success(String resultStatus) {
            this.resultStatus = Objects.requireNonNull(resultStatus, "resultStatus cannot be null");
        }

        /**
         * Returns the result status, always {@code "Success"} for this variant.
         *
         * @return the status; never {@code null}
         */
        public String resultStatus() {
            return resultStatus;
        }

        /**
         * Tries to parse an inbound stanza as a {@link Success}.
         *
         * <p>Returns empty when the IQ envelope does not match a result for {@code request} or the
         * {@code <result/>} child is missing or carries a non-{@code "Success"} status.
         *
         * @implNote
         * This implementation validates the IQ envelope (description, type, matching id), then
         * descends into {@code <result/>} and asserts {@code status="Success"} before surfacing
         * the variant.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSupportMessageFeedbackSendFeedbackResponseSuccess",
                exports = "parseSendFeedbackResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "result")) {
                return Optional.empty();
            }
            var requestId = request.getAttributeAsString("id").orElse(null);
            if (requestId == null || !stanza.hasAttribute("id", requestId)) {
                return Optional.empty();
            }
            var resultChild = stanza.getChild("result").orElse(null);
            if (resultChild == null) {
                return Optional.empty();
            }
            var status = resultChild.getAttributeAsString("status").orElse(null);
            if (!"Success".equals(status)) {
                return Optional.empty();
            }
            return Optional.of(new Success(status));
        }

        /**
         * Compares this variant to another for value equality.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link Success}
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
            return Objects.equals(this.resultStatus, that.resultStatus);
        }

        /**
         * Returns a hash code derived from the result status.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(resultStatus);
        }

        /**
         * Returns a debug string carrying the result status.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxSendFeedbackResponse.Success[resultStatus=" + resultStatus + ']';
        }
    }

    /**
     * Carries a 4xx code/text pair of a non-retryable feedback rejection (typically
     * {@code 400 bad-request} or {@code 429 rate-overlimit}).
     *
     * <p>The caller renders an error banner and re-opens the rating form; the {@code 429} case can
     * be retried after back-off.
     *
     * @implNote
     * This implementation delegates 4xx envelope extraction to
     * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}; the documented codes are
     * {@code 400} and {@code 429} but the parser does not enforce the disjunction.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSupportMessageFeedbackSendFeedbackResponseError")
    final class ClientError implements SmaxSendFeedbackResponse {
        /**
         * Holds the numeric error code from the {@code <error/>} envelope ({@code 400} or
         * {@code 429}).
         */
        private final int errorCode;

        /**
         * Holds the optional error text ({@code "bad-request"} or {@code "rate-overlimit"}), or
         * {@code null} when the envelope omitted it.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the parsed fields.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code classifying the 4xx rejection.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         *
         * <p>Empty when the {@code <error/>} envelope omitted the {@code text} attribute.
         *
         * @return an {@link Optional} carrying the error text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse an inbound stanza as a {@link ClientError}.
         *
         * <p>Returns empty when the inbound stanza is not a 4xx error reply to {@code request}.
         *
         * @implNote
         * This implementation delegates IQ-envelope and {@code <error/>} extraction to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSupportMessageFeedbackSendFeedbackResponseError",
                exports = "parseSendFeedbackResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another for value equality across all fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link ClientError}
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
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from all fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string listing every field.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxSendFeedbackResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a 5xx code/text pair of a transient internal feedback failure (typically
     * {@code 500 internal-server-error}).
     *
     * <p>The caller schedules a retry with back-off rather than re-opening the form.
     *
     * @implNote
     * This implementation delegates 5xx envelope extraction to
     * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInSupportMessageFeedbackSendFeedbackResponseError")
    final class ServerError implements SmaxSendFeedbackResponse {
        /**
         * Holds the numeric error code from the {@code <error/>} envelope (typically
         * {@code 500}).
         */
        private final int errorCode;

        /**
         * Holds the optional error text (typically {@code "internal-server-error"}), or
         * {@code null} when the envelope omitted it.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the parsed fields.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code classifying the 5xx failure.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         *
         * <p>Empty when the {@code <error/>} envelope omitted the {@code text} attribute.
         *
         * @return an {@link Optional} carrying the error text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse an inbound stanza as a {@link ServerError}.
         *
         * <p>Returns empty when the inbound stanza is not a 5xx error reply to {@code request}.
         *
         * @implNote
         * This implementation delegates IQ-envelope and {@code <error/>} extraction to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the originating outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInSupportMessageFeedbackSendFeedbackResponseError",
                exports = "parseSendFeedbackResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another for value equality across all fields.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal {@link ServerError}
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
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from all fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string listing every field.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxSendFeedbackResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
