package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound reply the relay produces in response to an {@link IqSendCoverPhotoRequest}.
 *
 * <p>The sealed family is pattern-matched to drive the cover-photo edit surface: {@link Success}
 * carries no payload, {@link ClientError} surfaces a rejected mutation and {@link ServerError}
 * surfaces a transient internal failure.
 */
@WhatsAppWebModule(moduleName = "WAWebBusinessProfileJob")
public sealed interface IqSendCoverPhotoResponse extends IqStanza.Response
        permits IqSendCoverPhotoResponse.Success, IqSendCoverPhotoResponse.ClientError, IqSendCoverPhotoResponse.ServerError {

    /**
     * Tries each variant in priority order until one matches.
     *
     * <p>The order is {@link Success}, then {@link ClientError}, then {@link ServerError}; call this
     * on every IQ stanza acking a cover-photo upload.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqSendCoverPhotoResponse> of(Stanza stanza, Stanza request) {
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
     * Models the {@code Success} variant, which carries no payload.
     *
     * <p>The relay echoes a bare {@code <iq type="result"/>} envelope when the cover-photo update
     * lands; the cover photo itself is then surfaced via the next business-profile fetch.
     */
    final class Success implements IqSendCoverPhotoResponse {
        /**
         * Constructs an empty success reply; called from {@link #of(Stanza, Stanza)}.
         */
        public Success() {
        }

        /**
         * Tries to parse a {@link Success} variant from the stanza.
         *
         * <p>The method validates the {@code <iq type="result">} envelope and returns the empty
         * success; called from {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         */
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this variant with another; all {@link Success} instances are equal.
         *
         * @param obj the object to compare against; may be {@code null}
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
         * Returns a constant hash code, since the variant carries no state.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns a diagnostic string for the empty success.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqSendCoverPhotoResponse.Success[]";
        }
    }

    /**
     * Models the {@code ClientError} variant, emitted when the relay rejects the mutation as
     * malformed or referencing an unknown upload.
     *
     * <p>The relay returns this shape when the upload artefact has expired or the token does not
     * match; surface it as a user-facing 4xx-class error on the cover-photo edit surface.
     */
    final class ClientError implements IqSendCoverPhotoResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the relay's {@code <error/>} envelope; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code, used to dispatch on the relay-side rejection reason.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots, so it is suitable for
         * logging only.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the stanza.
         *
         * <p>The method delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} to
         * extract the (code, text) envelope; called from {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
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
         * Compares this variant with another for value equality on the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal client error
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string naming the error code and text.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqSendCoverPhotoResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the {@code ServerError} variant, emitted when the relay returns a transient
     * internal-failure status while processing the mutation.
     *
     * <p>The relay returns this shape when the business-profile backend is temporarily unavailable;
     * use it to drive a backoff-and-retry path on the cover-photo edit surface.
     */
    final class ServerError implements IqSendCoverPhotoResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the relay's {@code <error/>} envelope; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code; a 5xx-class value is the canonical retry trigger.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots, so it is suitable for
         * logging only.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the stanza.
         *
         * <p>The method delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} to
         * extract the (code, text) envelope; called from {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
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
         * Compares this variant with another for value equality on the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal server error
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string naming the error code and text.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqSendCoverPhotoResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
