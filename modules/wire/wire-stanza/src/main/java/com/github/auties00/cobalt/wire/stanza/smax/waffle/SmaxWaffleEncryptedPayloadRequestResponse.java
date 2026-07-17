package com.github.auties00.cobalt.wire.stanza.smax.waffle;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound replies to a {@link SmaxWaffleEncryptedPayloadRequestRequest}.
 * <p>
 * A reply is exactly one of three shapes: a {@link Success} carrying the encrypted Facebook-side response
 * plus an optional deleted-state marker, a {@link ClientError} for malformed or unauthorised requests
 * (codes below {@code 500}), or a {@link ServerError} for transient relay failures (codes at or above
 * {@code 500}).
 */
public sealed interface SmaxWaffleEncryptedPayloadRequestResponse extends SmaxStanza.Response
        permits SmaxWaffleEncryptedPayloadRequestResponse.Success, SmaxWaffleEncryptedPayloadRequestResponse.ClientError, SmaxWaffleEncryptedPayloadRequestResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching variant.
     * <p>
     * The stanza is offered to the {@link Success} parser first, then the {@link ClientError} parser, then
     * the {@link ServerError} parser; the first that parses cleanly wins. An empty {@link Optional} means
     * none of the three matched.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when none of the three parsers matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxWaffleEncryptedPayloadRequestRPC",
            exports = "sendEncryptedPayloadRequestRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxWaffleEncryptedPayloadRequestResponse> of(Stanza stanza, Stanza request) {
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
     * Models the success reply: the relay forwarded the encrypted Facebook-side response.
     * <p>
     * The encrypted response is carried by {@link #encryptionMetadata()}; embedders decrypt it to recover
     * the Facebook-side payload. The optional {@link #wfDeleted()} flag surfaces only when the relay reports
     * that the linked Waffle state was deleted between the request and the reply.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleEncryptedPayloadRequestResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleIQResultResponseMixin")
    final class Success implements SmaxWaffleEncryptedPayloadRequestResponse {
        /**
         * Holds the relay-returned encryption metadata.
         */
        private final SmaxWaffleRsaEncryptionMetadata encryptionMetadata;

        /**
         * Holds the deleted-state flag, or {@code null} when the marker is absent.
         */
        private final Boolean wfDeleted;

        /**
         * Constructs a success reply from the relay-returned metadata and optional deleted-state flag.
         *
         * @param encryptionMetadata the relay-returned metadata; never {@code null}
         * @param wfDeleted          the deleted-state flag, or {@code null} when absent
         * @throws NullPointerException if {@code encryptionMetadata} is {@code null}
         */
        public Success(SmaxWaffleRsaEncryptionMetadata encryptionMetadata, Boolean wfDeleted) {
            this.encryptionMetadata = Objects.requireNonNull(encryptionMetadata, "encryptionMetadata cannot be null");
            this.wfDeleted = wfDeleted;
        }

        /**
         * Returns the relay-returned encryption metadata.
         *
         * @return the metadata as supplied by the relay; never {@code null}
         */
        public SmaxWaffleRsaEncryptionMetadata encryptionMetadata() {
            return encryptionMetadata;
        }

        /**
         * Returns the deleted-state flag when the relay surfaced one.
         * <p>
         * An empty {@link Optional} represents the wire absence rather than {@code false}, so callers can
         * distinguish a marker that was not surfaced from a marker that was explicitly false.
         *
         * @return an {@link Optional} carrying the flag, or empty when absent
         */
        public Optional<Boolean> wfDeleted() {
            return Optional.ofNullable(wfDeleted);
        }

        /**
         * Parses a success variant from the inbound stanza.
         * <p>
         * Returns an empty {@link Optional} when the envelope check fails, when the encryption-metadata
         * child is missing or malformed, or when the optional deleted-state child is unparsable.
         *
         * @implNote This implementation matches the deleted-state content case-insensitively against the
         * literal {@code "true"}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleEncryptedPayloadRequestResponseSuccess",
                exports = "parseEncryptedPayloadRequestResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var encryptionMetadataNode = stanza.getChild("encryption_metadata").orElse(null);
            if (encryptionMetadataNode == null) {
                return Optional.empty();
            }
            var metadata = SmaxWaffleRsaEncryptionMetadata.of(encryptionMetadataNode).orElse(null);
            if (metadata == null) {
                return Optional.empty();
            }
            Boolean wfDeleted = null;
            var wfDeletedNode = stanza.getChild("wf_deleted").orElse(null);
            if (wfDeletedNode != null) {
                var content = wfDeletedNode.toContentString().map(String::trim).orElse(null);
                if (content != null) {
                    wfDeleted = "true".equalsIgnoreCase(content);
                }
            }
            return Optional.of(new Success(metadata, wfDeleted));
        }

        /**
         * Returns whether the given object is a {@link Success} with equal payload fields.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both metadata and the deleted-state flag match
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
            return Objects.equals(this.encryptionMetadata, that.encryptionMetadata)
                    && Objects.equals(this.wfDeleted, that.wfDeleted);
        }

        /**
         * Returns a hash code derived from the payload fields.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(encryptionMetadata, wfDeleted);
        }

        /**
         * Returns a debug rendering of this success variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleEncryptedPayloadRequestResponse.Success[encryptionMetadata="
                    + encryptionMetadata
                    + ", wfDeleted=" + wfDeleted + ']';
        }
    }

    /**
     * Models the client-error reply: the relay rejected the request with a code below {@code 500}.
     * <p>
     * Surfaces malformed-request, unauthorised, and stale-nonce rejections. The carried {@link #errorCode()}
     * and {@link #errorText()} are the relay-reported failure details.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleEncryptedPayloadRequestResponseError")
    final class ClientError implements SmaxWaffleEncryptedPayloadRequestResponse {
        /**
         * Holds the numeric error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the relay-reported code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text, or {@code null} when absent
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the code as supplied by the relay
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
         * Parses a client-error variant from the inbound stanza.
         * <p>
         * The envelope and code-range check is delegated to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which only matches codes below {@code 500}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleEncryptedPayloadRequestResponseError",
                exports = "parseEncryptedPayloadRequestResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Returns whether the given object is a {@link ClientError} with equal code and text.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both code and text match
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
         * Returns a hash code derived from the code and text.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering of this client-error variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleEncryptedPayloadRequestResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the server-error reply: the relay rejected the request with a code of {@code 500} or above.
     * <p>
     * Indicates a transient relay-side failure. The carried {@link #errorCode()} and {@link #errorText()}
     * are the relay-reported failure details.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleEncryptedPayloadRequestResponseError")
    final class ServerError implements SmaxWaffleEncryptedPayloadRequestResponse {
        /**
         * Holds the numeric error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the relay-reported code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text, or {@code null} when absent
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the code as supplied by the relay
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
         * Parses a server-error variant from the inbound stanza.
         * <p>
         * The envelope and code-range check is delegated to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, which only matches codes at or above {@code 500}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleEncryptedPayloadRequestResponseError",
                exports = "parseEncryptedPayloadRequestResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Returns whether the given object is a {@link ServerError} with equal code and text.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both code and text match
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
         * Returns a hash code derived from the code and text.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering of this server-error variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleEncryptedPayloadRequestResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
