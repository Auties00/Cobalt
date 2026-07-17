package com.github.auties00.cobalt.wire.stanza.iq.disappearing;

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
 * {@link IqSetDisappearingModeRequest}.
 *
 * <p>The hierarchy permits exactly three variants. {@link Success} is a bare acknowledgement that
 * the new default is live (the relay echoes no payload); {@link ClientError} surfaces a relay
 * rejection in the sub-{@code 500} code range; {@link ServerError} surfaces a transient relay
 * failure in the {@code 500}-and-above range. Callers switch on the parsed variant to discriminate
 * the relay outcome.
 */
@WhatsAppWebModule(moduleName = "WAWebSetDisappearingModeJob")
public sealed interface IqSetDisappearingModeResponse extends IqStanza.Response
        permits IqSetDisappearingModeResponse.Success, IqSetDisappearingModeResponse.ClientError, IqSetDisappearingModeResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqSetDisappearingModeResponse}
     * variant.
     *
     * <p>Each variant's {@code of(stanza, request)} factory is tried in priority order, success then
     * client-error then server-error, and the first present result is returned. The ordering
     * matches the wire shape so the variants never overlap; an empty result means no documented
     * variant matched.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant
     *         matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSetDisappearingModeJob",
            exports = "setDisappearingMode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqSetDisappearingModeResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the success outcome: the relay accepted the mutation and echoed only the IQ envelope.
     *
     * <p>The variant holds no payload because the relay reads no data beyond the
     * {@code type="result"} assertion; a present {@link Success} confirms the new default is now
     * live on the relay.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetDisappearingModeJob")
    final class Success implements IqSetDisappearingModeResponse {
        /**
         * Constructs a successful reply.
         *
         * <p>The variant is stateless because the relay echoes no payload on acknowledgement.
         */
        public Success() {
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant when it matches the success
         * schema.
         *
         * <p>Returns empty when the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}
         * result-envelope check fails; no data is read past the envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSetDisappearingModeJob",
                exports = "setDMResponseParser",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two success variants are equal when they share the same runtime class; the type
         * carries no state to distinguish instances.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a success variant of the same class
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a hash code for this variant derived from the runtime class.
         *
         * @return the class-derived hash code
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns a debug string for this variant.
         *
         * @return a parameterless string representation
         */
        @Override
        public String toString() {
            return "IqSetDisappearingModeResponse.Success[]";
        }
    }

    /**
     * Carries a client-error outcome: the relay rejected the mutation with a sub-{@code 500} code.
     *
     * <p>The failure is reportable rather than retryable; WA Web logs the rejection and re-throws
     * it as a plain error.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetDisappearingModeJob")
    final class ClientError implements IqSetDisappearingModeResponse {
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
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} variant when it matches the standard
         * SMAX client-error envelope.
         *
         * <p>Returns empty when the envelope check fails; the parse is delegated entirely to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSetDisappearingModeJob",
                exports = "setDisappearingMode",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two client-error variants are equal when they share the same runtime class, the same
         * {@link #errorCode()}, and the same error text.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal client-error variant
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
         * Returns a hash code derived from the error code and error text.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and error text.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqSetDisappearingModeResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a server-error outcome: the relay hit a transient {@code 500}-and-above failure
     * processing the mutation.
     *
     * <p>The failure is typically retryable after a short backoff; the new default is not live
     * until a subsequent attempt returns {@link Success}.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetDisappearingModeJob")
    final class ServerError implements IqSetDisappearingModeResponse {
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
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} variant when it matches the standard
         * SMAX server-error envelope.
         *
         * <p>Returns empty when the envelope check fails; the parse is delegated entirely to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSetDisappearingModeJob",
                exports = "setDisappearingMode",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two server-error variants are equal when they share the same runtime class, the same
         * {@link #errorCode()}, and the same error text.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal server-error variant
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
         * Returns a hash code derived from the error code and error text.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and error text.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqSetDisappearingModeResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
