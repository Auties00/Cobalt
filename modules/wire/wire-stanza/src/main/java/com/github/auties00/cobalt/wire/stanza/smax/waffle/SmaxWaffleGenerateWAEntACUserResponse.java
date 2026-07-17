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
 * Models the sealed family of inbound replies to a {@link SmaxWaffleGenerateWAEntACUserRequest}.
 * <p>
 * A reply is exactly one of three shapes: a {@link Success} carrying fresh encryption metadata that decrypts
 * to the new linked {@code fbid}, a {@link ClientError} for malformed, unauthorised, or duplicate-link
 * requests (codes below {@code 500}), or a {@link ServerError} for transient relay failures (codes at or
 * above {@code 500}).
 */
public sealed interface SmaxWaffleGenerateWAEntACUserResponse extends SmaxStanza.Response
        permits SmaxWaffleGenerateWAEntACUserResponse.Success, SmaxWaffleGenerateWAEntACUserResponse.ClientError, SmaxWaffleGenerateWAEntACUserResponse.ServerError {

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
    @WhatsAppWebExport(moduleName = "WASmaxWaffleGenerateWAEntACUserRPC",
            exports = "sendGenerateWAEntACUserRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxWaffleGenerateWAEntACUserResponse> of(Stanza stanza, Stanza request) {
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
     * Models the success reply: the relay returned a fresh encryption-metadata subtree carrying the newly
     * bootstrapped {@code fbid}.
     * <p>
     * Embedders decrypt {@link #encryptionMetadata()} to recover the {@code fbid} and persist the link.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleGenerateWAEntACUserResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleIQResultResponseMixin")
    final class Success implements SmaxWaffleGenerateWAEntACUserResponse {
        /**
         * Holds the relay-returned encryption metadata carrying the new linked-account record.
         */
        private final SmaxWaffleRsaEncryptionMetadata encryptionMetadata;

        /**
         * Constructs a success reply from the relay-returned metadata.
         *
         * @param encryptionMetadata the relay-returned metadata; never {@code null}
         * @throws NullPointerException if {@code encryptionMetadata} is {@code null}
         */
        public Success(SmaxWaffleRsaEncryptionMetadata encryptionMetadata) {
            this.encryptionMetadata = Objects.requireNonNull(encryptionMetadata, "encryptionMetadata cannot be null");
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
         * Parses a success variant from the inbound stanza.
         * <p>
         * Returns an empty {@link Optional} when the envelope check fails, when the encryption-metadata
         * child is missing, or when the inner metadata subtree is malformed (see
         * {@link SmaxWaffleRsaEncryptionMetadata#of(Stanza)}).
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleGenerateWAEntACUserResponseSuccess",
                exports = "parseGenerateWAEntACUserResponseSuccess",
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
            return Optional.of(new Success(metadata));
        }

        /**
         * Returns whether the given object is a {@link Success} with equal encryption metadata.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both metadata subtrees match
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
            return Objects.equals(this.encryptionMetadata, that.encryptionMetadata);
        }

        /**
         * Returns a hash code derived from the encryption metadata.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(encryptionMetadata);
        }

        /**
         * Returns a debug rendering of this success variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleGenerateWAEntACUserResponse.Success[encryptionMetadata="
                    + encryptionMetadata + ']';
        }
    }

    /**
     * Models the client-error reply: the relay rejected the bootstrap with a code below {@code 500}.
     * <p>
     * Surfaces malformed-request, unauthorised, and stale-nonce rejections. The carried {@link #errorCode()}
     * and {@link #errorText()} are the relay-reported failure details.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleGenerateWAEntACUserResponseError")
    final class ClientError implements SmaxWaffleGenerateWAEntACUserResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleGenerateWAEntACUserResponseError",
                exports = "parseGenerateWAEntACUserResponseError",
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
            return "SmaxWaffleGenerateWAEntACUserResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the server-error reply: the relay rejected the bootstrap with a code of {@code 500} or above.
     * <p>
     * Indicates a transient relay-side failure. The carried {@link #errorCode()} and {@link #errorText()}
     * are the relay-reported failure details.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleGenerateWAEntACUserResponseError")
    final class ServerError implements SmaxWaffleGenerateWAEntACUserResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleGenerateWAEntACUserResponseError",
                exports = "parseGenerateWAEntACUserResponseError",
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
            return "SmaxWaffleGenerateWAEntACUserResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
