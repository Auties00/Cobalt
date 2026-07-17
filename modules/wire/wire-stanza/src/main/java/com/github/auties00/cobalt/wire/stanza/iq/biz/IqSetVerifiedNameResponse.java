package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.Objects;
import java.util.Optional;

/**
 * Roots the sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqSetVerifiedNameRequest}.
 *
 * <p>The hierarchy permits exactly three variants. {@link Success} carries the verified-name id the
 * relay echoes in the {@code <verified_name>} child of a {@code type="result"} reply;
 * {@link ClientError} surfaces a relay rejection in the sub-{@code 500} code range; {@link ServerError}
 * surfaces a transient relay failure in the {@code 500}-and-above range. Callers switch on the parsed
 * variant to discriminate the relay outcome.
 */
public sealed interface IqSetVerifiedNameResponse extends IqStanza.Response
        permits IqSetVerifiedNameResponse.Success, IqSetVerifiedNameResponse.ClientError, IqSetVerifiedNameResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqSetVerifiedNameResponse} variant.
     *
     * <p>Each variant's {@code of(stanza, request)} factory is tried in priority order, success then
     * client-error then server-error, and the first present result is returned. An empty result means
     * no documented variant matched.
     *
     * @param stanza  the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant
     *         matched
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqSetVerifiedNameResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the success outcome: the relay accepted the certificate and echoed the verified-name
     * id.
     *
     * <p>{@link #verifiedNameId()} is the identifier the relay assigned to the accepted certificate,
     * taken from the {@code id} attribute of the {@code <verified_name>} child; it is empty when the
     * relay acknowledged the submission without echoing a child (in which case callers treat the id as
     * the empty string).
     */
    final class Success implements IqSetVerifiedNameResponse {
        /**
         * Holds the echoed verified-name id, or {@code null} when the relay omitted it.
         */
        private final String verifiedNameId;

        /**
         * Constructs a successful reply carrying the echoed verified-name id.
         *
         * @param verifiedNameId the verified-name id; may be {@code null}
         */
        public Success(String verifiedNameId) {
            this.verifiedNameId = verifiedNameId;
        }

        /**
         * Returns the echoed verified-name id.
         *
         * @return an {@link Optional} carrying the id, or empty when the relay omitted it
         */
        public Optional<String> verifiedNameId() {
            return Optional.ofNullable(verifiedNameId);
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant when it matches the success
         * schema.
         *
         * <p>Returns empty when the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}
         * result-envelope check fails. When the envelope validates, the {@code id} attribute of the
         * {@code <verified_name>} child is read into {@link #verifiedNameId()} when present, and an
         * absent child yields an empty id.
         *
         * @param stanza  the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         */
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var verifiedNameId = stanza.getChild("verified_name")
                    .flatMap(node -> node.getAttributeAsString("id"))
                    .orElse(null);
            return Optional.of(new Success(verifiedNameId));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two success variants are equal when they share the same runtime class and the same
         * {@link #verifiedNameId()}.
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
            return Objects.equals(this.verifiedNameId, that.verifiedNameId);
        }

        /**
         * Returns a hash code derived from the verified-name id.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(verifiedNameId);
        }

        /**
         * Returns a debug string carrying the verified-name id.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqSetVerifiedNameResponse.Success[verifiedNameId=" + verifiedNameId + ']';
        }
    }

    /**
     * Carries a client-error outcome: the relay rejected the submission with a sub-{@code 500} code.
     *
     * <p>The failure is reportable rather than retryable; it typically signals an invalid or
     * unauthorised certificate.
     */
    final class ClientError implements IqSetVerifiedNameResponse {
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
         * @param stanza  the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
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
            return "IqSetVerifiedNameResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a server-error outcome: the relay hit a transient {@code 500}-and-above failure
     * processing the submission.
     *
     * <p>The failure is typically retryable after a short backoff; the certificate is not live until a
     * subsequent attempt returns {@link Success}.
     */
    final class ServerError implements IqSetVerifiedNameResponse {
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
         * @param stanza  the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
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
            return "IqSetVerifiedNameResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
