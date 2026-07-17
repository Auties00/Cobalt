package com.github.auties00.cobalt.wire.stanza.iq.tos;

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
 * Models the sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqDeleteTosRequest}.
 *
 * <p>Callers discriminate the relay outcome by switching on the returned variant. A {@link Success}
 * is a bare acknowledgement that carries no payload and confirms the deletion landed. A
 * {@link ClientError} surfaces a relay rejection, and a {@link ServerError} surfaces a transient
 * relay failure.
 *
 * @implNote The {@link Success} parser is a no-op envelope check because WhatsApp Web builds its
 *           parser from {@code WAWebNoop}; the error variants reuse the standard SMAX server-error
 *           envelope.
 */
public sealed interface IqDeleteTosResponse extends IqStanza.Response
        permits IqDeleteTosResponse.Success, IqDeleteTosResponse.ClientError, IqDeleteTosResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqDeleteTosResponse} variant.
     *
     * <p>The priority ordering (success, then client-error, then server-error) matches the wire
     * shape so the variants are mutually exclusive and the match is never ambiguous.
     *
     * @implNote This implementation calls each variant's {@code of(stanza, request)} in turn and
     *           returns the first present result.
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant
     *         matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebTosJob",
            exports = "deleteTosState", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqDeleteTosResponse> of(Stanza stanza, Stanza request) {
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
     * Models the success variant in which the relay accepted the deletion and echoed only the IQ
     * envelope.
     *
     * <p>A present {@link Success} confirms that the server-side accepted state for the bound notice
     * id is now cleared. The variant carries no payload.
     */
    @WhatsAppWebModule(moduleName = "WAWebTosJob")
    final class Success implements IqDeleteTosResponse {
        /**
         * Constructs a successful reply.
         */
        public Success() {
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant when it matches the success
         * schema.
         *
         * <p>Returns empty when the SMAX result-envelope check fails; the parser never reads past
         * the envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does
         *         not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebTosJob",
                exports = "deleteTosState", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this variant to the given object for equality.
         *
         * <p>All {@link Success} instances are equal because the variant carries no state.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success}, {@code false} otherwise
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
         * Returns a debug string for this stateless variant.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqDeleteTosResponse.Success[]";
        }
    }

    /**
     * Models the client-error variant in which the relay rejected the deletion with a {@code 4xx}
     * code.
     *
     * <p>This typically signals a malformed envelope or an unauthorised caller. The notice-id
     * payload is opaque to the relay, so an unknown id does not produce a client error; the relay
     * silently no-ops instead.
     */
    @WhatsAppWebModule(moduleName = "WAWebTosJob")
    final class ClientError implements IqDeleteTosResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text; {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply carrying the relay-echoed envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
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
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} variant when it matches the standard
         * SMAX client-error envelope.
         *
         * <p>Returns empty when the envelope check fails. Envelope parsing is delegated to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does
         *         not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebTosJob",
                exports = "deleteTosState", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to the given object for equality.
         *
         * <p>Two client errors are equal when they carry the same code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with an equal code and
         *         text, {@code false} otherwise
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
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqDeleteTosResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the server-error variant in which the relay encountered a transient internal failure
     * processing the deletion.
     *
     * <p>This outcome is typically retryable after a short backoff. The server-side accepted state
     * is unchanged until a subsequent attempt returns {@link Success}.
     */
    @WhatsAppWebModule(moduleName = "WAWebTosJob")
    final class ServerError implements IqDeleteTosResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text; {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply carrying the relay-echoed envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
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
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} variant when it matches the standard
         * SMAX server-error envelope.
         *
         * <p>Returns empty when the envelope check fails. Envelope parsing is delegated to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does
         *         not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebTosJob",
                exports = "deleteTosState", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to the given object for equality.
         *
         * <p>Two server errors are equal when they carry the same code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with an equal code and
         *         text, {@code false} otherwise
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
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqDeleteTosResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
