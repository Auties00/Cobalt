package com.github.auties00.cobalt.wire.stanza.iq.encrypt;

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
 * Closed family of reply variants observable on the steady-state pre-key upload
 * {@link IqUploadPreKeysRequest} roundtrip.
 *
 * <p>{@link Success} signals that the new pre-key bundle is live and the local Signal store should
 * record that the server now holds pre-keys. {@link ClientError} surfaces {@code 4xx} envelopes,
 * notably {@code 406} "uploaded invalid keys". {@link ServerError} surfaces {@code 5xx} envelopes
 * (the WA Web log labels these as "server requested backoff"); WA Web's retry loop drives the next
 * attempt with a fibonacci backoff schedule capped at roughly ten minutes.
 */
@WhatsAppWebModule(moduleName = "WAWebUploadPreKeysJob")
public sealed interface IqUploadPreKeysResponse extends IqStanza.Response
        permits IqUploadPreKeysResponse.Success,
        IqUploadPreKeysResponse.ClientError,
        IqUploadPreKeysResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqUploadPreKeysResponse} variant.
     *
     * <p>Attempts {@link Success#of(Stanza, Stanza)} first, then {@link ClientError#of(Stanza, Stanza)},
     * then {@link ServerError#of(Stanza, Stanza)}.
     *
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound stanza
     * @return the parsed variant wrapped in an {@link Optional}, or {@link Optional#empty()} when
     *         the stanza shape matched none of the three documented schemas
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebUploadPreKeysJob",
            exports = "uploadPreKeyResParser", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqUploadPreKeysResponse> of(Stanza stanza, Stanza request) {
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
     * Successful echo from the relay; the new pre-key bundle is now live.
     *
     * <p>Carries no payload because the relay's reply is just an envelope ack. Callers mirror WA
     * Web's behaviour by recording locally that the server now holds pre-keys, so the next
     * stash-low push triggers another upload only after the server-side stash has been consumed.
     */
    @WhatsAppWebModule(moduleName = "WAWebUploadPreKeysJob")
    final class Success implements IqUploadPreKeysResponse {
        /**
         * Constructs the singleton-shaped success envelope.
         */
        public Success() {
        }

        /**
         * Parses a {@link Success} variant from the inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the envelope fails the IQ-result echo check.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed {@link Success}, or empty when the stanza shape does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebUploadPreKeysJob",
                exports = "uploadPreKeyResParser", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this success envelope to another instance for equality.
         *
         * <p>All instances are interchangeable; equality reduces to a class-identity check.
         *
         * @param obj the candidate instance
         * @return {@code true} when {@code obj} is a non-{@code null} {@code Success}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a stable hash code shared by all instances.
         *
         * @return the class identity hash
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns the canonical record-style rendering.
         *
         * @return the literal {@code "IqUploadPreKeysResponse.Success[]"}
         */
        @Override
        public String toString() {
            return "IqUploadPreKeysResponse.Success[]";
        }
    }

    /**
     * Client-error variant; the relay rejected the upload with a {@code 4xx} envelope.
     *
     * <p>The documented case is {@code 406} "uploaded invalid keys"; WA Web logs the failure and
     * the retry loop drops out of the upload step.
     */
    @WhatsAppWebModule(moduleName = "WAWebUploadPreKeysJob")
    final class ClientError implements IqUploadPreKeysResponse {
        /**
         * The relay's {@code 4xx} numeric error code.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text from the {@code <error text="..."/>} attribute.
         */
        private final String errorText;

        /**
         * Constructs a populated client-error envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, possibly {@code null}
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
         * Parses a {@link ClientError} variant from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed {@link ClientError}, or empty when the stanza shape does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebUploadPreKeysJob",
                exports = "uploadPreKeyResParser", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this client-error envelope to another instance for equality.
         *
         * @param obj the candidate instance
         * @return {@code true} when {@code obj} is a {@code ClientError} with identical fields
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
         * Returns a hash code derived from the code and text fields.
         *
         * @return the combined hash
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns the record-style rendering for this client-error envelope.
         *
         * @return the rendered string
         */
        @Override
        public String toString() {
            return "IqUploadPreKeysResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Server-error variant; the relay reported a transient failure with a {@code 5xx} envelope.
     *
     * <p>WA Web logs these as "server requested backoff" and schedules the next attempt with a
     * fibonacci timer (first {@code 1s}, second {@code 2s}, capped at {@code 610s}). Callers that
     * mirror the policy should defer retries accordingly.
     */
    @WhatsAppWebModule(moduleName = "WAWebUploadPreKeysJob")
    final class ServerError implements IqUploadPreKeysResponse {
        /**
         * The relay's {@code 5xx} numeric error code.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text from the {@code <error text="..."/>} attribute.
         */
        private final String errorText;

        /**
         * Constructs a populated server-error envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, possibly {@code null}
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
         * Parses a {@link ServerError} variant from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed {@link ServerError}, or empty when the stanza shape does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebUploadPreKeysJob",
                exports = "uploadPreKeyResParser", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this server-error envelope to another instance for equality.
         *
         * @param obj the candidate instance
         * @return {@code true} when {@code obj} is a {@code ServerError} with identical fields
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
         * Returns a hash code derived from the code and text fields.
         *
         * @return the combined hash
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns the record-style rendering for this server-error envelope.
         *
         * @return the rendered string
         */
        @Override
        public String toString() {
            return "IqUploadPreKeysResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
