package com.github.auties00.cobalt.wire.stanza.iq.biz;

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
 * Models the sealed family of inbound reply variants the relay produces in response to an {@link IqQueryGetPublicKeyRequest}.
 *
 * <p>The matched variant drives the buyer-side direct-connection flow: {@link Success#certificate()}
 * carries the merchant's PEM-encoded ECC certificate when one is registered, {@link ClientError}
 * surfaces a rejected request and {@link ServerError} surfaces a transient internal failure.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryGetPublicKeyJob")
public sealed interface IqQueryGetPublicKeyResponse extends IqStanza.Response
        permits IqQueryGetPublicKeyResponse.Success, IqQueryGetPublicKeyResponse.ClientError, IqQueryGetPublicKeyResponse.ServerError {

    /**
     * Tries each variant in priority order until one matches.
     *
     * <p>The order is {@link Success}, then {@link ClientError}, then {@link ServerError},
     * mirroring the priority that the WA Web parser applies before throwing a server-status error.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqQueryGetPublicKeyResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the merchant's PEM-encoded catalog certificate when one is registered.
     *
     * <p>The certificate from {@link #certificate()} feeds the direct-connection encryption
     * pipeline; an empty optional means the merchant has not yet registered a key and the cart UI
     * must surface a server-side onboarding error to the buyer.
     */
    final class Success implements IqQueryGetPublicKeyResponse {
        /**
         * Holds the PEM-encoded ECC certificate string echoed by the relay, or {@code null} when the merchant has not registered a key.
         */
        private final String certificate;

        /**
         * Constructs a successful reply from the parsed PEM string.
         *
         * <p>A {@code null} value marks an absent {@code <pem/>} grandchild.
         *
         * @param certificate the PEM string; may be {@code null}
         */
        public Success(String certificate) {
            this.certificate = certificate;
        }

        /**
         * Returns the PEM-encoded certificate fed into the buyer-side direct-connection encryption flow.
         *
         * <p>An empty optional means the merchant has not yet registered a key.
         *
         * @return an {@link Optional} carrying the certificate
         */
        public Optional<String> certificate() {
            return Optional.ofNullable(certificate);
        }

        /**
         * Tries to parse a {@link Success} variant from the inbound stanza.
         *
         * <p>The method validates the {@code <iq type="result">} envelope and reads the optional
         * {@code <public_key><pem/></public_key>} grandchild, returning an empty optional when the
         * schema does not match.
         *
         * @implNote
         * This implementation reads the {@code <pem/>} body verbatim; WA Web's reference parser
         * additionally calls {@code WAWebDirectConnectionUtils.stringToCertificateString} to
         * normalise the PEM line endings, which Cobalt defers to the caller.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryGetPublicKeyJob",
                exports = "QueryGetPublicKey", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var certificate = stanza.getChild("public_key")
                    .flatMap(publicKey -> publicKey.getChild("pem"))
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            return Optional.of(new Success(certificate));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.certificate, that.certificate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(certificate);
        }

        @Override
        public String toString() {
            return "IqQueryGetPublicKeyResponse.Success[certificate=" + certificate + ']';
        }
    }

    /**
     * Surfaces a relay rejection of the request as malformed or unauthorised.
     *
     * <p>This variant carries a user-facing 4xx-class error for the cart UI; the relay returns this
     * shape when the merchant JID is not a registered business or the caller lacks visibility into
     * the merchant catalog.
     */
    final class ClientError implements IqQueryGetPublicKeyResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the relay's {@code <error/>} envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code used to dispatch a localised message to the cart UI.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots, so the cart UI should
         * dispatch on {@link #errorCode()} instead.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} to extract
         * the (code, text) envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

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

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqQueryGetPublicKeyResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Surfaces a transient internal-failure status the relay returns while processing the request.
     *
     * <p>This variant drives a backoff-and-retry path in the cart UI; the relay returns this shape
     * when the catalog backend is temporarily unavailable.
     */
    final class ServerError implements IqQueryGetPublicKeyResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the relay's {@code <error/>} envelope.
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
         * <p>The text is server-localised and not stable across snapshots.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} to extract
         * the (code, text) envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

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

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqQueryGetPublicKeyResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
