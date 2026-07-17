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
 * Closed family of reply variants observable on the one-shot registration-time pre-key upload
 * {@link IqUploadPrekeysForRegRequest} roundtrip.
 *
 * <p>The wire shape is identical to {@link IqUploadPreKeysResponse}; this stays a separate typed
 * surface so the registration code path can match against its own response type and avoid coupling
 * the device-link bootstrap to the steady-state retry policy.
 */
@WhatsAppWebModule(moduleName = "WAWebUploadPrekeysForRegTask")
public sealed interface IqUploadPrekeysForRegResponse extends IqStanza.Response
        permits IqUploadPrekeysForRegResponse.Success, IqUploadPrekeysForRegResponse.ClientError, IqUploadPrekeysForRegResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqUploadPrekeysForRegResponse}
     * variant.
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
    @WhatsAppWebExport(moduleName = "WAWebUploadPrekeysForRegTask",
            exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqUploadPrekeysForRegResponse> of(Stanza stanza, Stanza request) {
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
     * Successful echo from the relay; the registration-time pre-key bundle is now live.
     *
     * <p>Carries no payload; the device-link bootstrap proceeds to the next companion stage on the
     * presence of this variant.
     */
    @WhatsAppWebModule(moduleName = "WAWebUploadPrekeysForRegTask")
    final class Success implements IqUploadPrekeysForRegResponse {
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
        @WhatsAppWebExport(moduleName = "WAWebUploadPrekeysForRegTask",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
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
         * @return the literal {@code "IqUploadPrekeysForRegResponse.Success[]"}
         */
        @Override
        public String toString() {
            return "IqUploadPrekeysForRegResponse.Success[]";
        }
    }

    /**
     * Client-error variant; the relay rejected the registration-time upload with a {@code 4xx}
     * envelope.
     *
     * <p>Mirrors {@link IqUploadPreKeysResponse.ClientError}; the documented case is {@code 406}
     * "uploaded invalid keys".
     */
    @WhatsAppWebModule(moduleName = "WAWebUploadPrekeysForRegTask")
    final class ClientError implements IqUploadPrekeysForRegResponse {
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
        @WhatsAppWebExport(moduleName = "WAWebUploadPrekeysForRegTask",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
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
            return "IqUploadPrekeysForRegResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Server-error variant; the relay reported a transient failure with a {@code 5xx} envelope
     * during the registration-time upload.
     *
     * <p>WA Web's {@code WAWebUploadPrekeysForRegTask} logs the code and lets the surrounding
     * device-link flow drop the key-upload passive task; the next link attempt re-runs the stage
     * from scratch.
     */
    @WhatsAppWebModule(moduleName = "WAWebUploadPrekeysForRegTask")
    final class ServerError implements IqUploadPrekeysForRegResponse {
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
        @WhatsAppWebExport(moduleName = "WAWebUploadPrekeysForRegTask",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
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
            return "IqUploadPrekeysForRegResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
