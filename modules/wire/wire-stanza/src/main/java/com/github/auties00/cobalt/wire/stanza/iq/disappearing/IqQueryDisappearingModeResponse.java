package com.github.auties00.cobalt.wire.stanza.iq.disappearing;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Roots the sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqQueryDisappearingModeRequest}.
 *
 * <p>The hierarchy permits exactly three variants. {@link Success} carries the current default
 * duration and the wall-clock at which it was last applied; {@link ClientError} surfaces a relay
 * rejection in the sub-{@code 500} code range; {@link ServerError} surfaces a transient relay
 * failure in the {@code 500}-and-above range. Callers switch on the parsed variant to discriminate
 * the relay outcome.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryDisappearingModeJob")
public sealed interface IqQueryDisappearingModeResponse extends IqStanza.Response
        permits IqQueryDisappearingModeResponse.Success, IqQueryDisappearingModeResponse.ClientError, IqQueryDisappearingModeResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqQueryDisappearingModeResponse}
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
    @WhatsAppWebExport(moduleName = "WAWebQueryDisappearingModeJob",
            exports = "queryDisappearingMode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqQueryDisappearingModeResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the success outcome: the current default disappearing-mode duration and the
     * wall-clock at which it was last applied.
     *
     * <p>{@link #duration()} is the default applied to newly-created chats, with
     * {@link Duration#ZERO} encoding the off state, and {@link #appliedAtSeconds()} is the relay's
     * wall-clock anchor in seconds since epoch.
     */
    @WhatsAppWebModule(moduleName = "WAWebQueryDisappearingModeJob")
    final class Success implements IqQueryDisappearingModeResponse {
        /**
         * Holds the default disappearing-mode duration applied to newly-created chats.
         *
         * <p>{@link Duration#ZERO} encodes the off state.
         */
        private final Duration duration;

        /**
         * Holds the wall-clock at which the duration was last applied, in seconds since epoch.
         */
        private final long appliedAtSeconds;

        /**
         * Constructs a successful reply bound to the given duration and apply-timestamp.
         *
         * @param duration         the default duration; never {@code null}
         * @param appliedAtSeconds the wall-clock the duration was last applied, in seconds since
         *                         epoch
         * @throws NullPointerException if {@code duration} is {@code null}
         */
        public Success(Duration duration, long appliedAtSeconds) {
            this.duration = Objects.requireNonNull(duration, "duration cannot be null");
            this.appliedAtSeconds = appliedAtSeconds;
        }

        /**
         * Returns the bound default disappearing-mode duration.
         *
         * @return the duration; never {@code null}
         */
        public Duration duration() {
            return duration;
        }

        /**
         * Returns the wall-clock at which the duration was last applied.
         *
         * @return seconds since epoch
         */
        public long appliedAtSeconds() {
            return appliedAtSeconds;
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant when it matches the success
         * schema.
         *
         * <p>Returns empty when the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}
         * result-envelope check fails, when the {@code <disappearing_mode>} child is absent, or
         * when either the {@code duration} or {@code t} attribute is missing.
         *
         * @implNote This implementation reads both {@code duration} and {@code t} as {@code long}
         * via {@link Stanza#getAttributeAsLong(String)} where WA Web reads them as {@code int}; the
         * underlying wire range is identical.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryDisappearingModeJob",
                exports = "dmParser",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var dmNode = stanza.getChild("disappearing_mode").orElse(null);
            if (dmNode == null) {
                return Optional.empty();
            }
            var durationAttr = dmNode.getAttributeAsLong("duration");
            if (durationAttr.isEmpty()) {
                return Optional.empty();
            }
            var tAttr = dmNode.getAttributeAsLong("t");
            if (tAttr.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Success(Duration.ofSeconds(durationAttr.getAsLong()),
                    tAttr.getAsLong()));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two success variants are equal when they share the same runtime class, the same
         * {@link #duration()}, and the same {@link #appliedAtSeconds()}.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal success variant
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
            return this.appliedAtSeconds == that.appliedAtSeconds
                    && Objects.equals(this.duration, that.duration);
        }

        /**
         * Returns a hash code derived from {@link #duration()} and {@link #appliedAtSeconds()}.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(duration, appliedAtSeconds);
        }

        /**
         * Returns a debug string carrying the duration and apply-timestamp.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqQueryDisappearingModeResponse.Success[duration=" + duration
                    + ", appliedAtSeconds=" + appliedAtSeconds + ']';
        }
    }

    /**
     * Carries a client-error outcome: the relay rejected the query with a sub-{@code 500} code.
     *
     * <p>A client error is uncommon here because the request has no payload; it typically signals
     * an authorisation or session-state problem rather than a malformed request.
     */
    @WhatsAppWebModule(moduleName = "WAWebQueryDisappearingModeJob")
    final class ClientError implements IqQueryDisappearingModeResponse {
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
        @WhatsAppWebExport(moduleName = "WAWebQueryDisappearingModeJob",
                exports = "queryDisappearingMode",
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
            return "IqQueryDisappearingModeResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a server-error outcome: the relay hit a transient {@code 500}-and-above failure
     * processing the query.
     *
     * <p>The failure is transient; a subsequent attempt may succeed. WA Web wraps the same outcome
     * in a server-status-code error that its account-sync warmup catches to skip the
     * disappearing-mode propagation for the current cycle.
     */
    @WhatsAppWebModule(moduleName = "WAWebQueryDisappearingModeJob")
    final class ServerError implements IqQueryDisappearingModeResponse {
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
        @WhatsAppWebExport(moduleName = "WAWebQueryDisappearingModeJob",
                exports = "queryDisappearingMode",
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
            return "IqQueryDisappearingModeResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
