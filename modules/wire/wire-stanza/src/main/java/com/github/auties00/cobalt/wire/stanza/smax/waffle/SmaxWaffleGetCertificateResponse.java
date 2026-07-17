package com.github.auties00.cobalt.wire.stanza.smax.waffle;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound replies to a {@link SmaxWaffleGetCertificateRequest}.
 * <p>
 * A reply is exactly one of three shapes: a {@link Success} carrying the requested PEM subset under a reply
 * envelope, a {@link ClientError} for malformed or unauthorised requests (codes below {@code 500}), or a
 * {@link ServerError} for transient relay failures (codes at or above {@code 500}).
 */
public sealed interface SmaxWaffleGetCertificateResponse extends SmaxStanza.Response
        permits SmaxWaffleGetCertificateResponse.Success, SmaxWaffleGetCertificateResponse.ClientError, SmaxWaffleGetCertificateResponse.ServerError {

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
    @WhatsAppWebExport(moduleName = "WASmaxWaffleGetCertificateRPC",
            exports = "sendGetCertificateRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxWaffleGetCertificateResponse> of(Stanza stanza, Stanza request) {
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
     * Models one PEM child carried inside the {@link Success} reply.
     * <p>
     * Captures the shape shared by the encryption, signature, and password PEM children: a positive
     * time-to-live in seconds, an optional key id (present only on the password PEM), and the raw PEM bytes.
     *
     * @implNote The PEM content is ASCII text but is modelled as raw bytes to match how the wire content is
     * extracted.
     *
     * @param ttl   the per-PEM time-to-live in seconds; always positive
     * @param keyId the per-PEM key id; non-{@code null} only on the password PEM
     * @param pem   the raw PEM content bytes
     */
    record Pem(int ttl, Integer keyId, byte[] pem) {

        /**
         * Returns the optional key id.
         * <p>
         * An empty {@link Optional} represents the wire absence rather than a sentinel zero; the encryption
         * and signature PEMs never carry the attribute, while the password PEM always does.
         *
         * @return an {@link Optional} carrying the key id, or empty when absent
         */
        public Optional<Integer> keyIdAsOptional() {
            return Optional.ofNullable(keyId);
        }

        /**
         * Returns whether the given object is a {@link Pem} with equal payload fields.
         * <p>
         * The PEM bytes are compared element-wise.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when ttl, key id, and PEM bytes all match
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Pem) obj;
            return this.ttl == that.ttl
                    && Objects.equals(this.keyId, that.keyId)
                    && Arrays.equals(this.pem, that.pem);
        }

        /**
         * Returns a hash code derived from the three payload fields.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            var result = Objects.hash(ttl, keyId);
            result = 31 * result + Arrays.hashCode(pem);
            return result;
        }

        /**
         * Returns a debug rendering of this PEM.
         * <p>
         * The PEM bytes are summarised as a length rather than as the raw content.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleGetCertificateResponse.Pem[ttl=" + ttl
                    + ", keyId=" + keyId
                    + ", pem=" + (pem != null ? pem.length + " bytes" : "null") + ']';
        }
    }

    /**
     * Models the success reply: the relay returned the requested PEM subset under a reply envelope.
     * <p>
     * Each of the three PEM children is independently optional; the caller only sees PEMs whose corresponding
     * marker was set on the request. The {@link #replyTimestamp()} is the server-stamped issue time used to
     * compute the per-PEM expiry from {@link Pem#ttl()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleGetCertificateResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleGetCertificateResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleIQResultResponseMixin")
    final class Success implements SmaxWaffleGetCertificateResponse {
        /**
         * Holds the relay-stamped reply timestamp, in seconds since the Unix epoch.
         */
        private final long replyTimestamp;

        /**
         * Holds the encryption PEM, or {@code null} when the relay omitted it.
         */
        private final Pem encryptionPem;

        /**
         * Holds the signature PEM, or {@code null} when the relay omitted it.
         */
        private final Pem signaturePem;

        /**
         * Holds the password PEM, or {@code null} when the relay omitted it.
         */
        private final Pem passwordPem;

        /**
         * Constructs a success reply from the reply timestamp and the three optional PEMs.
         *
         * @param replyTimestamp the relay-stamped reply timestamp
         * @param encryptionPem  the encryption PEM, or {@code null} when absent
         * @param signaturePem   the signature PEM, or {@code null} when absent
         * @param passwordPem    the password PEM, or {@code null} when absent
         */
        public Success(long replyTimestamp, Pem encryptionPem, Pem signaturePem, Pem passwordPem) {
            this.replyTimestamp = replyTimestamp;
            this.encryptionPem = encryptionPem;
            this.signaturePem = signaturePem;
            this.passwordPem = passwordPem;
        }

        /**
         * Returns the relay-stamped reply timestamp.
         *
         * @return the timestamp as supplied by the relay
         */
        public long replyTimestamp() {
            return replyTimestamp;
        }

        /**
         * Returns the encryption PEM when the relay supplied one.
         *
         * @return an {@link Optional} carrying the PEM, or empty when absent
         */
        public Optional<Pem> encryptionPem() {
            return Optional.ofNullable(encryptionPem);
        }

        /**
         * Returns the signature PEM when the relay supplied one.
         *
         * @return an {@link Optional} carrying the PEM, or empty when absent
         */
        public Optional<Pem> signaturePem() {
            return Optional.ofNullable(signaturePem);
        }

        /**
         * Returns the password PEM when the relay supplied one.
         *
         * @return an {@link Optional} carrying the PEM, or empty when absent
         */
        public Optional<Pem> passwordPem() {
            return Optional.ofNullable(passwordPem);
        }

        /**
         * Parses a success variant from the inbound stanza.
         * <p>
         * Returns an empty {@link Optional} when the envelope check fails, when the reply child is missing,
         * or when the inner {@code timestamp} attribute is absent or non-positive. Each of the three PEM
         * children is parsed through {@link #parsePem(Stanza)} and is dropped when absent or malformed.
         *
         * @implNote This implementation gates each PEM on a {@code ttl} attribute of at least {@code 1} and
         * silently drops the PEM otherwise; the per-blob byte-range checks WhatsApp Web applies (each PEM
         * 1-4092 bytes) are not re-applied here.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleGetCertificateResponseSuccess",
                exports = "parseGetCertificateResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var reply = stanza.getChild("reply").orElse(null);
            if (reply == null) {
                return Optional.empty();
            }
            var timestamp = reply.getAttributeAsLong("timestamp").orElse(-1L);
            if (timestamp < 1) {
                return Optional.empty();
            }
            var encryptionPem = parsePem(reply.getChild("encryption_pem").orElse(null));
            var signaturePem = parsePem(reply.getChild("signature_pem").orElse(null));
            var passwordPem = parsePem(reply.getChild("password_pem").orElse(null));
            return Optional.of(new Success(timestamp, encryptionPem, signaturePem, passwordPem));
        }

        /**
         * Parses a single PEM child stanza into a {@link Pem} record.
         * <p>
         * Returns {@code null} (rather than an empty {@link Optional}) when the stanza is missing or malformed,
         * so the three PEM slots on {@link Success} can be assigned directly.
         *
         * @implNote This implementation accepts both the {@code (ttl, key_id, content)} shape of the password
         * PEM and the {@code (ttl, content)} shape of the encryption and signature PEMs by treating the key
         * id as optional, collapsing WhatsApp Web's three distinct per-child parsers into one.
         *
         * @param pemStanza the PEM child stanza, or {@code null}
         * @return the parsed {@link Pem}, or {@code null} when the stanza is missing or malformed
         */
        private static Pem parsePem(Stanza pemStanza) {
            if (pemStanza == null) {
                return null;
            }
            var ttl = pemStanza.getAttributeAsInt("ttl").orElse(-1);
            if (ttl < 1) {
                return null;
            }
            var keyId = pemStanza.getAttributeAsInt("key_id").isPresent()
                    ? pemStanza.getAttributeAsInt("key_id").getAsInt()
                    : null;
            var bytes = pemStanza.toContentBytes().orElse(null);
            if (bytes == null) {
                return null;
            }
            return new Pem(ttl, keyId, bytes);
        }

        /**
         * Returns whether the given object is a {@link Success} with equal payload fields.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when timestamp and the three PEMs all match
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
            return this.replyTimestamp == that.replyTimestamp
                    && Objects.equals(this.encryptionPem, that.encryptionPem)
                    && Objects.equals(this.signaturePem, that.signaturePem)
                    && Objects.equals(this.passwordPem, that.passwordPem);
        }

        /**
         * Returns a hash code derived from the four payload fields.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(replyTimestamp, encryptionPem, signaturePem, passwordPem);
        }

        /**
         * Returns a debug rendering of this success variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleGetCertificateResponse.Success[replyTimestamp=" + replyTimestamp
                    + ", encryptionPem=" + encryptionPem
                    + ", signaturePem=" + signaturePem
                    + ", passwordPem=" + passwordPem + ']';
        }
    }

    /**
     * Models the client-error reply: the relay rejected the request with a code below {@code 500}.
     * <p>
     * Surfaces malformed-request and unauthorised rejections from the Waffle backend. The carried
     * {@link #errorCode()} and {@link #errorText()} are the relay-reported failure details.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleGetCertificateResponseError")
    final class ClientError implements SmaxWaffleGetCertificateResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleGetCertificateResponseError",
                exports = "parseGetCertificateResponseError",
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
            return "SmaxWaffleGetCertificateResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the server-error reply: the relay rejected the request with a code of {@code 500} or above.
     * <p>
     * Indicates a transient relay-side failure. The carried {@link #errorCode()} and {@link #errorText()}
     * are the relay-reported failure details.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleGetCertificateResponseError")
    final class ServerError implements SmaxWaffleGetCertificateResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleGetCertificateResponseError",
                exports = "parseGetCertificateResponseError",
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
            return "SmaxWaffleGetCertificateResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
