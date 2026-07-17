package com.github.auties00.cobalt.wire.stanza.smax.privatestats;

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
 * Closes the family of inbound reply variants to a {@link SmaxPrivatestatsSignCredentialRequest}.
 *
 * <p>The hierarchy permits {@link Success} (the relay signed the credential and returned the
 * signature plus DLEQ proof), {@link ErrorNoRetry} (a permanent rejection), and {@link ErrorRetry}
 * (a transient {@code 500 internal-server-error}). Callers that drive the privatestats credential
 * cache reissue the request only on the {@link ErrorRetry} variant; the {@link ErrorNoRetry} variant
 * is surfaced verbatim to the caller.
 */
public sealed interface SmaxPrivatestatsSignCredentialResponse extends SmaxStanza.Response
        permits SmaxPrivatestatsSignCredentialResponse.Success, SmaxPrivatestatsSignCredentialResponse.ErrorNoRetry, SmaxPrivatestatsSignCredentialResponse.ErrorRetry {

    /**
     * Tries each {@link SmaxPrivatestatsSignCredentialResponse} variant in priority order and returns the first that parses cleanly.
     *
     * <p>The dispatcher tries {@link Success} first, then {@link ErrorNoRetry} (the
     * {@code 400}/{@code 501}/{@code 503} disjunction), then {@link ErrorRetry} (the
     * {@code 500 internal-server-error} fallback), returning empty when no documented variant matches.
     *
     * @param stanza the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxPrivatestatsSignCredentialRPC",
            exports = "sendSignCredentialRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxPrivatestatsSignCredentialResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var noRetry = ErrorNoRetry.of(stanza, request);
        if (noRetry.isPresent()) {
            return noRetry;
        }
        return ErrorRetry.of(stanza, request);
    }

    /**
     * Models the {@code Success} reply variant, returned when the relay signed the blinded credential.
     *
     * <p>This variant carries the signed credential, the ACS public key the local client must use to
     * verify it, the DLEQ proof {@code (c, s)} pair, the project-name echo, and a server-stamped
     * timestamp.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsSignCredentialResponseSuccess")
    final class Success implements SmaxPrivatestatsSignCredentialResponse {
        /**
         * Holds the Unix-epoch timestamp echoed on the {@code <sign_credential>} child's {@code t} attribute.
         */
        private final long signCredentialT;

        /**
         * Holds the 32-byte signed credential bytes.
         */
        private final byte[] signedCredentialElementValue;

        /**
         * Holds the 32-byte ACS public key bytes.
         */
        private final byte[] acsPublicKeyElementValue;

        /**
         * Holds the 32-byte DLEQ proof {@code c} component.
         */
        private final byte[] dleqProofCElementValue;

        /**
         * Holds the 32-byte DLEQ proof {@code s} component.
         */
        private final byte[] dleqProofSElementValue;

        /**
         * Holds the project-name echo.
         */
        private final String projectNameElementValue;

        /**
         * Constructs a new success reply from the parsed timestamp, byte payloads, and project-name echo.
         *
         * <p>This constructor is invoked by {@link #of(Stanza, Stanza)} after the stanza shape and the four
         * 32-byte length predicates have been validated; callers typically do not instantiate it directly.
         *
         * @param signCredentialT the timestamp echo
         * @param signedCredentialElementValue the signed credential bytes; never {@code null}; exactly 32 bytes
         * @param acsPublicKeyElementValue the ACS public key bytes; never {@code null}; exactly 32 bytes
         * @param dleqProofCElementValue the DLEQ proof {@code c} bytes; never {@code null}; exactly 32 bytes
         * @param dleqProofSElementValue the DLEQ proof {@code s} bytes; never {@code null}; exactly 32 bytes
         * @param projectNameElementValue the project-name echo; never {@code null}
         * @throws NullPointerException if any required argument is {@code null}
         */
        public Success(long signCredentialT,
                       byte[] signedCredentialElementValue,
                       byte[] acsPublicKeyElementValue,
                       byte[] dleqProofCElementValue,
                       byte[] dleqProofSElementValue,
                       String projectNameElementValue) {
            this.signCredentialT = signCredentialT;
            this.signedCredentialElementValue = Objects.requireNonNull(signedCredentialElementValue,
                    "signedCredentialElementValue cannot be null");
            this.acsPublicKeyElementValue = Objects.requireNonNull(acsPublicKeyElementValue,
                    "acsPublicKeyElementValue cannot be null");
            this.dleqProofCElementValue = Objects.requireNonNull(dleqProofCElementValue,
                    "dleqProofCElementValue cannot be null");
            this.dleqProofSElementValue = Objects.requireNonNull(dleqProofSElementValue,
                    "dleqProofSElementValue cannot be null");
            this.projectNameElementValue = Objects.requireNonNull(projectNameElementValue,
                    "projectNameElementValue cannot be null");
        }

        /**
         * Returns the timestamp echo.
         *
         * @return the timestamp
         */
        public long signCredentialT() {
            return signCredentialT;
        }

        /**
         * Returns the signed credential bytes.
         *
         * @return the bytes; never {@code null}
         */
        public byte[] signedCredentialElementValue() {
            return signedCredentialElementValue;
        }

        /**
         * Returns the ACS public key bytes.
         *
         * @return the bytes; never {@code null}
         */
        public byte[] acsPublicKeyElementValue() {
            return acsPublicKeyElementValue;
        }

        /**
         * Returns the DLEQ proof {@code c} component.
         *
         * @return the bytes; never {@code null}
         */
        public byte[] dleqProofCElementValue() {
            return dleqProofCElementValue;
        }

        /**
         * Returns the DLEQ proof {@code s} component.
         *
         * @return the bytes; never {@code null}
         */
        public byte[] dleqProofSElementValue() {
            return dleqProofSElementValue;
        }

        /**
         * Returns the project-name echo.
         *
         * @return the project name; never {@code null}
         */
        public String projectNameElementValue() {
            return projectNameElementValue;
        }

        /**
         * Parses a {@link Success} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when any required child is missing or any of the four byte
         * payloads is not exactly 32 bytes long.
         *
         * @implNote
         * This implementation walks the {@code <sign_credential t>} subtree top-down via
         * {@link Stanza#getChild(String)} and rejects on the first missing or oversized field; the
         * envelope shape check is delegated to {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}.
         * The fixed 32-byte predicate on the four byte payloads reflects the ACS Curve25519 parameter
         * sizes. The expected subtree shape is shown below.
         * {@snippet lang="xml" :
         * <sign_credential t="...">
         *   <signed_credential>BYTES</signed_credential>
         *   <acs_public_key>BYTES</acs_public_key>
         *   <dleq_proof>
         *     <c>BYTES</c>
         *     <s>BYTES</s>
         *   </dleq_proof>
         *   <project_name>STRING</project_name>
         * </sign_credential>
         * }
         *
         * @param stanza the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPrivatestatsSignCredentialResponseSuccess",
                exports = "parseSignCredentialResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var signCredential = stanza.getChild("sign_credential").orElse(null);
            if (signCredential == null) {
                return Optional.empty();
            }
            var t = signCredential.getAttributeAsLong("t");
            if (t.isEmpty() || t.getAsLong() < 0) {
                return Optional.empty();
            }
            var signedCredential = signCredential.getChild("signed_credential")
                    .flatMap(Stanza::toContentBytes)
                    .orElse(null);
            if (signedCredential == null || signedCredential.length != 32) {
                return Optional.empty();
            }
            var acsPublicKey = signCredential.getChild("acs_public_key")
                    .flatMap(Stanza::toContentBytes)
                    .orElse(null);
            if (acsPublicKey == null || acsPublicKey.length != 32) {
                return Optional.empty();
            }
            var dleqProof = signCredential.getChild("dleq_proof").orElse(null);
            if (dleqProof == null) {
                return Optional.empty();
            }
            var dleqProofC = dleqProof.getChild("c")
                    .flatMap(Stanza::toContentBytes)
                    .orElse(null);
            if (dleqProofC == null || dleqProofC.length != 32) {
                return Optional.empty();
            }
            var dleqProofS = dleqProof.getChild("s")
                    .flatMap(Stanza::toContentBytes)
                    .orElse(null);
            if (dleqProofS == null || dleqProofS.length != 32) {
                return Optional.empty();
            }
            var projectName = signCredential.getChild("project_name")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            if (projectName == null) {
                return Optional.empty();
            }
            return Optional.of(new Success(t.getAsLong(), signedCredential, acsPublicKey,
                    dleqProofC, dleqProofS, projectName));
        }

        /**
         * Returns whether the given object is a {@link Success} with equal timestamp, byte payloads, and project name.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when every field matches
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
            return this.signCredentialT == that.signCredentialT
                    && Arrays.equals(this.signedCredentialElementValue, that.signedCredentialElementValue)
                    && Arrays.equals(this.acsPublicKeyElementValue, that.acsPublicKeyElementValue)
                    && Arrays.equals(this.dleqProofCElementValue, that.dleqProofCElementValue)
                    && Arrays.equals(this.dleqProofSElementValue, that.dleqProofSElementValue)
                    && Objects.equals(this.projectNameElementValue, that.projectNameElementValue);
        }

        /**
         * Returns a hash code derived from every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            var result = Objects.hash(signCredentialT, projectNameElementValue);
            result = 31 * result + Arrays.hashCode(signedCredentialElementValue);
            result = 31 * result + Arrays.hashCode(acsPublicKeyElementValue);
            result = 31 * result + Arrays.hashCode(dleqProofCElementValue);
            result = 31 * result + Arrays.hashCode(dleqProofSElementValue);
            return result;
        }

        /**
         * Returns a debug-friendly textual representation of this variant.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxPrivatestatsSignCredentialResponse.Success[signCredentialT=" + signCredentialT
                    + ", signedCredentialElementValue="
                    + Arrays.toString(signedCredentialElementValue)
                    + ", acsPublicKeyElementValue="
                    + Arrays.toString(acsPublicKeyElementValue)
                    + ", dleqProofCElementValue=" + Arrays.toString(dleqProofCElementValue)
                    + ", dleqProofSElementValue=" + Arrays.toString(dleqProofSElementValue)
                    + ", projectNameElementValue=" + projectNameElementValue + ']';
        }
    }

    /**
     * Models the {@code ErrorNoRetry} reply variant, a permanent rejection that the local client must not retry.
     *
     * <p>This variant carries one of three documented {@code (code, text)} pairs:
     * {@code (400, "bad-request")}, {@code (501, "feature-not-implemented")}, or
     * {@code (503, "service-unavailable")}. The privatestats credential cache evicts the in-flight
     * request and surfaces the rejection to the caller rather than reissuing the RPC.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsSignCredentialResponseErrorNoRetry")
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsSignCredentialNoRetryError")
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsIQErrorBadRequestMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsIQErrorFeatureNotImplementedMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsIQErrorServiceUnavailableMixin")
    final class ErrorNoRetry implements SmaxPrivatestatsSignCredentialResponse {
        /**
         * Holds the numeric error code; one of {@code 400}, {@code 501}, or {@code 503}.
         */
        private final int errorCode;

        /**
         * Holds the error text; one of {@code "bad-request"}, {@code "feature-not-implemented"}, or {@code "service-unavailable"}.
         */
        private final String errorText;

        /**
         * Constructs a new no-retry error reply from the numeric error code and the optional error text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ErrorNoRetry(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses an {@link ErrorNoRetry} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the envelope does not match one of the three
         * documented {@code (code, text)} pairs; unknown codes are deliberately not absorbed so they
         * fall through to the {@link ErrorRetry} parser.
         *
         * @implNote
         * This implementation routes a 4xx envelope through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} and a 5xx envelope through
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, then accepts only the three
         * documented pairs.
         *
         * @param stanza the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPrivatestatsSignCredentialResponseErrorNoRetry",
                exports = "parseSignCredentialResponseErrorNoRetry",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ErrorNoRetry> of(Stanza stanza, Stanza request) {
            var clientEnvelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            var serverEnvelope = clientEnvelope == null
                    ? SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null)
                    : null;
            var envelope = clientEnvelope != null ? clientEnvelope : serverEnvelope;
            if (envelope == null) {
                return Optional.empty();
            }
            var code = envelope.code();
            var text = envelope.text();
            if ((code == 400 && "bad-request".equals(text))
                    || (code == 501 && "feature-not-implemented".equals(text))
                    || (code == 503 && "service-unavailable".equals(text))) {
                return Optional.of(new ErrorNoRetry(code, text));
            }
            return Optional.empty();
        }

        /**
         * Returns whether the given object is an {@link ErrorNoRetry} with an equal code and text.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both fields match
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ErrorNoRetry) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug-friendly textual representation of this variant.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxPrivatestatsSignCredentialResponse.ErrorNoRetry[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the {@code ErrorRetry} reply variant, a transient {@code 500 internal-server-error} rejection.
     *
     * <p>Callers driving the privatestats credential cache may reissue the request on the next flush
     * window; the variant carries no payload because the {@code (500, "internal-server-error")} pair is
     * implied by the type.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsSignCredentialResponseErrorRetry")
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsIQErrorInternalServerErrorMixin")
    final class ErrorRetry implements SmaxPrivatestatsSignCredentialResponse {
        /**
         * Constructs a new retry-error reply.
         *
         * <p>This constructor is invoked by {@link #of(Stanza, Stanza)} after the envelope shape has been
         * validated against the {@code (500, "internal-server-error")} pair; callers typically do not
         * instantiate it directly.
         */
        public ErrorRetry() {
        }

        /**
         * Returns the numeric error code; always {@code 500}.
         *
         * @return the code
         */
        public int errorCode() {
            return 500;
        }

        /**
         * Returns the error text; always {@code "internal-server-error"}.
         *
         * @return the text
         */
        public String errorText() {
            return "internal-server-error";
        }

        /**
         * Parses an {@link ErrorRetry} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the envelope is not a server-range error or when its
         * code/text pair is not exactly {@code (500, "internal-server-error")}.
         *
         * @param stanza the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPrivatestatsSignCredentialResponseErrorRetry",
                exports = "parseSignCredentialResponseErrorRetry",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ErrorRetry> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            if (envelope.code() != 500 || !"internal-server-error".equals(envelope.text())) {
                return Optional.empty();
            }
            return Optional.of(new ErrorRetry());
        }

        /**
         * Returns whether the given object is also an {@link ErrorRetry}.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when {@code obj} is an {@link ErrorRetry}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash code; the variant carries no fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return ErrorRetry.class.hashCode();
        }

        /**
         * Returns a debug-friendly textual representation of this variant.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxPrivatestatsSignCredentialResponse.ErrorRetry[]";
        }
    }
}
