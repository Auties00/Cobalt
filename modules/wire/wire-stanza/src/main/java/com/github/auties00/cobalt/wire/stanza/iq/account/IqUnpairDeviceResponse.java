package com.github.auties00.cobalt.wire.stanza.iq.account;

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
 * Models the inbound reply produced by the relay in response to an {@link IqUnpairDeviceRequest}.
 *
 * <p>This sealed family splits the reply into {@link Success}, {@link ClientError}, and
 * {@link ServerError} so callers can react differently to a stale device handle versus a transient
 * server failure.
 *
 * @implNote
 * This implementation preserves a distinction that WA Web's parser discards: there the reply is
 * collapsed to a single status record that conflates the success path with WhatsApp Web GraphQL errors.
 */
@WhatsAppWebModule(moduleName = "WAWebUnpairDeviceJob")
public sealed interface IqUnpairDeviceResponse extends IqStanza.Response
        permits IqUnpairDeviceResponse.Success, IqUnpairDeviceResponse.ClientError, IqUnpairDeviceResponse.ServerError {

    /**
     * Parses the inbound stanza into the first {@link IqUnpairDeviceResponse} variant that matches.
     *
     * <p>Candidates are tried in priority order: {@link Success} (a {@code type="result"}
     * envelope), then {@link ClientError} (a {@code type="error"} envelope whose {@code <error/>}
     * child code falls in the {@code 4xx} range), then {@link ServerError} (any other
     * {@code type="error"} envelope). The first candidate that parses cleanly is returned. This
     * method is invoked by the legacy-IQ dispatcher once the inbound {@code <iq>} stanza has been
     * matched against the outbound request by id.
     *
     * @implNote
     * This implementation does not return a variant blindly: each candidate revalidates the echoed
     * id against {@code request} via {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} or
     * {@link SmaxBaseServerErrorMixin}, so a reply mis-routed by the dispatcher surfaces as
     * {@link Optional#empty()} rather than a silently-wrong variant.
     *
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
     *         when no documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUnpairDeviceJob",
            exports = "unpairDevice", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqUnpairDeviceResponse> of(Stanza stanza, Stanza request) {
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
     * Signals that the relay accepted the unpair request.
     *
     * <p>This variant carries no payload; the success envelope conveys nothing beyond the echoed
     * id.
     */
    @WhatsAppWebModule(moduleName = "WAWebUnpairDeviceJob")
    final class Success implements IqUnpairDeviceResponse {
        /**
         * Constructs an empty successful reply.
         *
         * <p>The constructor takes no arguments because the success envelope carries no payload
         * beyond the echoed id.
         */
        public Success() {
        }

        /**
         * Parses a {@link Success} variant from the given inbound stanza, when present.
         *
         * <p>A populated {@link Optional} is returned only when the stanza is a
         * {@code type="result"} envelope that echoes the {@code request} id, as checked by
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebUnpairDeviceJob",
                exports = "unpairResponse", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this reply to another object for value equality.
         *
         * <p>Because the variant carries no payload, any other {@link Success} is equal.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link Success}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return a constant hash code shared by all {@link Success} instances
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns a debug representation of this reply.
         *
         * @return a constant string identifying the empty success variant
         */
        @Override
        public String toString() {
            return "IqUnpairDeviceResponse.Success[]";
        }
    }

    /**
     * Signals that the relay rejected the unpair request as malformed, unauthorised, or
     * referencing an unknown device.
     *
     * <p>This variant maps to the {@code 4xx} branch of the relay reply, reading the
     * {@code <error code/>} child of a {@code type="error"} envelope.
     */
    @WhatsAppWebModule(moduleName = "WAWebUnpairDeviceJob")
    final class ClientError implements IqUnpairDeviceResponse {
        /**
         * Holds the numeric server-side error code read from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text read from the {@code <error text/>}
         * attribute, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the given code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, or {@code null} when omitted
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
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ClientError} variant from the given inbound stanza, when present.
         *
         * <p>A populated {@link Optional} is returned only when the stanza is a
         * {@code type="error"} envelope that echoes the {@code request} id and carries an
         * {@code <error/>} child whose {@code code} attribute falls in the {@code 4xx} range, per
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebUnpairDeviceJob",
                exports = "unpairResponse", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality.
         *
         * <p>Two client errors are equal when they share the same {@link #errorCode()} and
         * {@link #errorText()}.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is an equal {@link ClientError}
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code derived from {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation of this reply.
         *
         * @return a string containing the {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public String toString() {
            return "IqUnpairDeviceResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Signals that the relay encountered a transient internal failure while processing the unpair
     * request.
     *
     * <p>This variant maps to the {@code 5xx} branch of the relay reply; callers may retry the
     * same request after a backoff once the socket has settled.
     */
    @WhatsAppWebModule(moduleName = "WAWebUnpairDeviceJob")
    final class ServerError implements IqUnpairDeviceResponse {
        /**
         * Holds the numeric server-side error code read from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text read from the {@code <error text/>}
         * attribute, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the given code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, or {@code null} when omitted
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
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ServerError} variant from the given inbound stanza, when present.
         *
         * <p>A populated {@link Optional} is returned only when the stanza is a
         * {@code type="error"} envelope that echoes the {@code request} id and carries an
         * {@code <error/>} child whose {@code code} attribute falls outside the {@code 4xx} range,
         * per {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebUnpairDeviceJob",
                exports = "unpairResponse", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for value equality.
         *
         * <p>Two server errors are equal when they share the same {@link #errorCode()} and
         * {@link #errorText()}.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is an equal {@link ServerError}
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code derived from {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation of this reply.
         *
         * @return a string containing the {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public String toString() {
            return "IqUnpairDeviceResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
