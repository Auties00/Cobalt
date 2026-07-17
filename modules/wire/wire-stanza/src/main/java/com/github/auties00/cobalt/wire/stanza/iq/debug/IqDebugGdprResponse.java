package com.github.auties00.cobalt.wire.stanza.iq.debug;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Roots the sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqDebugGdprRequest}.
 *
 * <p>The variant discriminates the relay outcome: {@link Success} echoes the post-cancel GDPR
 * status (typically the {@code "none"} string with a zeroed expiration), {@link ClientError}
 * carries a relay rejection (commonly {@code 404} when no GDPR request is in flight), and
 * {@link ServerError} carries a transient relay failure the caller may retry. Callers obtain a
 * variant through {@link #of(Stanza, Stanza)}.
 */
@WhatsAppWebModule(moduleName = "WAWebGdprHookUtils")
public sealed interface IqDebugGdprResponse extends IqStanza.Response
        permits IqDebugGdprResponse.Success, IqDebugGdprResponse.ClientError, IqDebugGdprResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqDebugGdprResponse} variant.
     *
     * <p>Each variant's {@code of(Stanza, Stanza)} is tried in priority order, success first, then
     * client-error, then server-error, and the first present result is returned. The ordering
     * matches the wire shape, so it never returns an ambiguous match.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented
     *         variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebDebugGDPR",
            exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqDebugGdprResponse> of(Stanza stanza, Stanza request) {
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
     * Represents the success variant in which the relay accepted the cancel request and echoed
     * the post-cancel GDPR status.
     *
     * <p>{@link #status()} carries the textual status (typically {@code "none"} after a
     * successful cancel) and {@link #expirationSeconds()} carries the relay-echoed expiration
     * timestamp (typically {@code 0} after cancel). Both are absent when the relay returned a
     * bare acknowledgement without a {@code <gdpr>} child.
     */
    @WhatsAppWebModule(moduleName = "WAWebDebugGDPR")
    final class Success implements IqDebugGdprResponse {
        /**
         * Holds the relay-echoed GDPR status string, or {@code null} when the relay omitted the
         * {@code <gdpr>} child entirely.
         */
        private final String status;

        /**
         * Holds the relay-echoed expiration timestamp in seconds since epoch, or {@code null}
         * when the relay omitted the attribute.
         */
        private final Long expirationSeconds;

        /**
         * Constructs a successful reply carrying the relay-echoed fields.
         *
         * <p>Both fields are nullable because the relay may return either a bare acknowledgement
         * or a {@code <gdpr>} child with arbitrary attribute coverage.
         *
         * @param status            the status string; may be {@code null}
         * @param expirationSeconds the expiration timestamp; may be {@code null}
         */
        public Success(String status, Long expirationSeconds) {
            this.status = status;
            this.expirationSeconds = expirationSeconds;
        }

        /**
         * Returns the relay-echoed GDPR status string, if any.
         *
         * @return an {@link Optional} carrying the status (typically {@code "none"} after
         *         cancel), or empty when omitted
         */
        public Optional<String> status() {
            return Optional.ofNullable(status);
        }

        /**
         * Returns the relay-echoed expiration timestamp in seconds, if any.
         *
         * @return an {@link Optional} carrying the timestamp, or empty when omitted
         */
        public Optional<Long> expirationSeconds() {
            return Optional.ofNullable(expirationSeconds);
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant when it matches the success
         * schema.
         *
         * <p>Returns empty when the SMAX result-envelope check
         * ({@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}) fails. A missing
         * {@code <gdpr>} child is accepted as the relay's bare-acknowledgement shape and yields
         * a {@link Success} with both fields {@code null}; otherwise the optional {@code status}
         * and {@code expiration} attributes are read off the child.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does
         *         not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebDebugGDPR",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var gdpr = stanza.getChild("gdpr").orElse(null);
            if (gdpr == null) {
                return Optional.of(new Success(null, null));
            }
            var status = gdpr.getAttributeAsString("status").orElse(null);
            var expirationAttr = gdpr.getAttributeAsLong("expiration");
            var expirationSeconds = expirationAttr.isPresent()
                    ? expirationAttr.getAsLong()
                    : null;
            return Optional.of(new Success(status, expirationSeconds));
        }

        /**
         * Compares this variant with another for equality by status and expiration.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with equal
         *         {@link #status()} and {@link #expirationSeconds()}, {@code false} otherwise
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
            return Objects.equals(this.status, that.status)
                    && Objects.equals(this.expirationSeconds, that.expirationSeconds);
        }

        /**
         * Returns a hash code derived from the status and expiration.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(status, expirationSeconds);
        }

        /**
         * Returns a debug string carrying the status and expiration.
         *
         * @return a human-readable representation of this variant
         */
        @Override
        public String toString() {
            return "IqDebugGdprResponse.Success[status=" + status
                    + ", expirationSeconds=" + expirationSeconds + ']';
        }
    }

    /**
     * Represents the client-error variant in which the relay rejected the cancel request with a
     * {@code 4xx} code.
     *
     * <p>The dominant client error is {@code 404} when no GDPR request is in flight for the bound
     * report type; because the request payload itself is well-formed, a different code typically
     * signals an authorisation issue.
     */
    @WhatsAppWebModule(moduleName = "WAWebDebugGDPR")
    final class ClientError implements IqDebugGdprResponse {
        /**
         * Holds the numeric server-side error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay, or {@code null} when omitted.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply carrying the relay-echoed envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, if any.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} variant when it matches the
         * standard SMAX client-error envelope.
         *
         * <p>Returns empty when the envelope check fails; the parse delegates entirely to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does
         *         not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebDebugGDPR",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant with another for equality by error code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with equal
         *         {@link #errorCode()} and {@link #errorText()}, {@code false} otherwise
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
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and text.
         *
         * @return a human-readable representation of this variant
         */
        @Override
        public String toString() {
            return "IqDebugGdprResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the server-error variant in which the relay hit a transient {@code 5xx} failure
     * processing the cancel request.
     *
     * <p>The failure is typically retryable after a short backoff; the relay surfaces no
     * machine-readable backoff hint on this stanza, so the caller's standard retry policy
     * applies.
     */
    @WhatsAppWebModule(moduleName = "WAWebDebugGDPR")
    final class ServerError implements IqDebugGdprResponse {
        /**
         * Holds the numeric server-side error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay, or {@code null} when omitted.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply carrying the relay-echoed envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, if any.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} variant when it matches the
         * standard SMAX server-error envelope.
         *
         * <p>Returns empty when the envelope check fails; the parse delegates entirely to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does
         *         not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebDebugGDPR",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant with another for equality by error code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with equal
         *         {@link #errorCode()} and {@link #errorText()}, {@code false} otherwise
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
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and text.
         *
         * @return a human-readable representation of this variant
         */
        @Override
        public String toString() {
            return "IqDebugGdprResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
