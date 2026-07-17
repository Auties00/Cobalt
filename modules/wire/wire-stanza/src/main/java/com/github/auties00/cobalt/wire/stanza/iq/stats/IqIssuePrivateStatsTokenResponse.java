package com.github.auties00.cobalt.wire.stanza.iq.stats;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Roots the sealed family of inbound reply variants the relay produces in response to an
 * {@link IqIssuePrivateStatsTokenRequest}.
 *
 * <p>The hierarchy permits exactly three variants: {@link Success} carries the signed credential
 * and its DLEQ proof, {@link ClientError} reports a non-retryable rejection, and
 * {@link ServerError} reports a transient internal failure that the private-stats driver may
 * retry with the same blinded credential. The {@link ClientError} and {@link ServerError} split
 * mirrors the relay's WAM telemetry classification of credential signing failures.
 */
public sealed interface IqIssuePrivateStatsTokenResponse extends IqStanza.Response
        permits IqIssuePrivateStatsTokenResponse.Success, IqIssuePrivateStatsTokenResponse.ClientError, IqIssuePrivateStatsTokenResponse.ServerError {

    /**
     * Parses the inbound stanza into the first {@link IqIssuePrivateStatsTokenResponse} variant
     * that matches.
     *
     * <p>The variants are tried in the fixed priority order {@link Success}, {@link ClientError},
     * {@link ServerError}, and the first that parses cleanly is returned. An empty result means
     * none of the documented variants matched the stanza shape.
     *
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no
     *         documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxPrivatestatsSignCredentialRPC",
            exports = "sendSignCredentialRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqIssuePrivateStatsTokenResponse> of(Stanza stanza, Stanza request) {
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
     * Models the reply variant carrying the signed credential, the relay's public key, and the
     * DLEQ proof of correct signing.
     *
     * <p>The signed credential, the public key, and both DLEQ coordinates ({@code c} and
     * {@code s}) are 32-byte elliptic-curve scalars. The client unblinds the signed credential
     * against the blinding factor it retained from request-build time and verifies the DLEQ proof
     * against the relay's published public key before redeeming the resulting token. The
     * {@code signCredentialT} timestamp records the relay-side wall-clock when the signature was
     * minted.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsSignCredentialResponseSuccess")
    final class Success implements IqIssuePrivateStatsTokenResponse {
        /**
         * Holds the relay-side mint timestamp in seconds since epoch, read from the
         * {@code <sign_credential t/>} attribute.
         */
        private final long signCredentialT;

        /**
         * Holds the raw 32-byte signed-credential elliptic-curve scalar.
         */
        private final byte[] signedCredential;

        /**
         * Holds the raw 32-byte ACS public key the relay published when signing the credential.
         */
        private final byte[] acsPublicKey;

        /**
         * Holds the raw 32-byte DLEQ proof {@code c} coordinate.
         */
        private final byte[] dleqProofC;

        /**
         * Holds the raw 32-byte DLEQ proof {@code s} coordinate.
         */
        private final byte[] dleqProofS;

        /**
         * Holds the echoed UTF-8 project-name string carried under the {@code <project_name>}
         * grandchild, equal to the request's project name modulo encoding.
         */
        private final String projectName;

        /**
         * Constructs a new successful reply from the minted credential and its DLEQ proof.
         *
         * <p>Every byte-array argument is defensively cloned; the string is immutable and stored
         * directly.
         *
         * @param signCredentialT  the relay-side mint timestamp in seconds since epoch
         * @param signedCredential the 32-byte signed-credential scalar
         * @param acsPublicKey     the 32-byte ACS public key
         * @param dleqProofC       the 32-byte DLEQ proof {@code c} coordinate
         * @param dleqProofS       the 32-byte DLEQ proof {@code s} coordinate
         * @param projectName      the echoed project name
         * @throws NullPointerException if any byte-array argument or {@code projectName} is
         *                              {@code null}
         */
        public Success(long signCredentialT, byte[] signedCredential, byte[] acsPublicKey,
                       byte[] dleqProofC, byte[] dleqProofS, String projectName) {
            this.signCredentialT = signCredentialT;
            this.signedCredential = Objects.requireNonNull(signedCredential, "signedCredential cannot be null").clone();
            this.acsPublicKey = Objects.requireNonNull(acsPublicKey, "acsPublicKey cannot be null").clone();
            this.dleqProofC = Objects.requireNonNull(dleqProofC, "dleqProofC cannot be null").clone();
            this.dleqProofS = Objects.requireNonNull(dleqProofS, "dleqProofS cannot be null").clone();
            this.projectName = Objects.requireNonNull(projectName, "projectName cannot be null");
        }

        /**
         * Returns the relay-side mint timestamp.
         *
         * @return the mint timestamp in seconds since epoch
         */
        public long signCredentialT() {
            return signCredentialT;
        }

        /**
         * Returns a defensive copy of the signed-credential bytes.
         *
         * @return a clone of the signed-credential scalar, never {@code null}
         */
        public byte[] signedCredential() {
            return signedCredential.clone();
        }

        /**
         * Returns a defensive copy of the ACS public-key bytes.
         *
         * @return a clone of the public-key scalar, never {@code null}
         */
        public byte[] acsPublicKey() {
            return acsPublicKey.clone();
        }

        /**
         * Returns a defensive copy of the DLEQ proof {@code c} coordinate.
         *
         * @return a clone of the {@code c} scalar, never {@code null}
         */
        public byte[] dleqProofC() {
            return dleqProofC.clone();
        }

        /**
         * Returns a defensive copy of the DLEQ proof {@code s} coordinate.
         *
         * @return a clone of the {@code s} scalar, never {@code null}
         */
        public byte[] dleqProofS() {
            return dleqProofS.clone();
        }

        /**
         * Returns the echoed project name.
         *
         * @return the project-name string, never {@code null}
         */
        public String projectName() {
            return projectName;
        }

        /**
         * Parses a {@link Success} variant from the given inbound stanza.
         *
         * <p>An empty result is returned when any of the four mandatory scalar fields is missing
         * or not 32 bytes long, when the {@code <sign_credential t/>} timestamp is absent or
         * negative, when the {@code <dleq_proof/>} grandchild is missing, or when the
         * {@code <project_name>} grandchild is absent; the caller then falls through to the error
         * variants.
         *
         * @implNote
         * This implementation enforces a strict 32-byte width on every scalar field regardless of
         * the relay's wire encoding; WhatsApp Web's parser is silent on the width, but the
         * consuming credential-unblinding routine requires it.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPrivatestatsSignCredentialResponseSuccess",
                exports = "parseSignCredentialResponseSuccess", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var signCredential = stanza.getChild("sign_credential").orElse(null);
            if (signCredential == null) {
                return Optional.empty();
            }
            var t = signCredential.getAttributeAsLong("t", -1L);
            if (t < 0L) {
                return Optional.empty();
            }
            var signedCredentialBytes = signCredential.getChild("signed_credential")
                    .flatMap(Stanza::toContentBytes)
                    .orElse(null);
            if (signedCredentialBytes == null || signedCredentialBytes.length != 32) {
                return Optional.empty();
            }
            var acsPublicKeyBytes = signCredential.getChild("acs_public_key")
                    .flatMap(Stanza::toContentBytes)
                    .orElse(null);
            if (acsPublicKeyBytes == null || acsPublicKeyBytes.length != 32) {
                return Optional.empty();
            }
            var dleqProof = signCredential.getChild("dleq_proof").orElse(null);
            if (dleqProof == null) {
                return Optional.empty();
            }
            var dleqCBytes = dleqProof.getChild("c")
                    .flatMap(Stanza::toContentBytes)
                    .orElse(null);
            if (dleqCBytes == null || dleqCBytes.length != 32) {
                return Optional.empty();
            }
            var dleqSBytes = dleqProof.getChild("s")
                    .flatMap(Stanza::toContentBytes)
                    .orElse(null);
            if (dleqSBytes == null || dleqSBytes.length != 32) {
                return Optional.empty();
            }
            var projectNameValue = signCredential.getChild("project_name")
                    .flatMap(Stanza::toContentString)
                    .orElse(null);
            if (projectNameValue == null) {
                return Optional.empty();
            }
            return Optional.of(new Success(t, signedCredentialBytes, acsPublicKeyBytes,
                    dleqCBytes, dleqSBytes, projectNameValue));
        }

        /**
         * Compares this reply with the given object for equality.
         *
         * <p>Two replies are equal when their mint timestamps match, all four scalar byte arrays
         * are element-wise equal, and their project names are equal.
         *
         * @param obj the object to compare against
         * @return {@code true} if the objects are equal, {@code false} otherwise
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
                    && Arrays.equals(this.signedCredential, that.signedCredential)
                    && Arrays.equals(this.acsPublicKey, that.acsPublicKey)
                    && Arrays.equals(this.dleqProofC, that.dleqProofC)
                    && Arrays.equals(this.dleqProofS, that.dleqProofS)
                    && Objects.equals(this.projectName, that.projectName);
        }

        /**
         * Returns a hash code derived from the timestamp, the four scalar byte arrays, and the
         * project name.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(signCredentialT, Arrays.hashCode(signedCredential),
                    Arrays.hashCode(acsPublicKey), Arrays.hashCode(dleqProofC),
                    Arrays.hashCode(dleqProofS), projectName);
        }

        /**
         * Returns a diagnostic string carrying the timestamp, scalar lengths, and project name.
         *
         * <p>Only the scalar lengths are rendered, never the raw credential bytes, so the value
         * can be logged without leaking the signed credential or the DLEQ proof.
         *
         * @return a string describing this reply
         */
        @Override
        public String toString() {
            return "IqIssuePrivateStatsTokenResponse.Success[signCredentialT=" + signCredentialT
                    + ", signedCredentialLength=" + signedCredential.length
                    + ", acsPublicKeyLength=" + acsPublicKey.length
                    + ", dleqProofCLength=" + dleqProofC.length
                    + ", dleqProofSLength=" + dleqProofS.length
                    + ", projectName=" + projectName + ']';
        }
    }

    /**
     * Models the reply variant signalling that the relay rejected the credential issuance request
     * as malformed or otherwise non-retryable.
     *
     * <p>This variant covers the relay's non-retryable error branch, spanning {@code bad-request},
     * {@code feature-not-implemented}, {@code service-unavailable}, {@code decryption-error}, and
     * unknown SMAX statuses. The private-stats driver should not retry the same blinded credential
     * after receiving it.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsSignCredentialResponseErrorNoRetry")
    final class ClientError implements IqIssuePrivateStatsTokenResponse {
        /**
         * Holds the numeric server-side error code read from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text read from the {@code <error text/>}
         * attribute, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply from the parsed error code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, or {@code null} when omitted
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
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when the relay
         *         omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>A populated result is returned only when the stanza is a {@code type="error"} envelope
         * echoing the {@code request} id and carrying an {@code <error/>} child whose {@code code}
         * attribute is below {@code 500}, per the partition enforced by
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPrivatestatsSignCredentialResponseErrorNoRetry",
                exports = "parseSignCredentialResponseErrorNoRetry",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with the given object for equality.
         *
         * <p>Two replies are equal when their error codes match and their error texts are equal.
         *
         * @param obj the object to compare against
         * @return {@code true} if the objects are equal, {@code false} otherwise
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
         * Returns a hash code derived from the error code and error text.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string carrying the error code and error text.
         *
         * @return a string describing this reply
         */
        @Override
        public String toString() {
            return "IqIssuePrivateStatsTokenResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the reply variant signalling that the relay encountered a transient internal failure
     * while issuing the credential.
     *
     * <p>This variant covers the relay's retryable error branch, spanning
     * {@code internal-server-error}. The caller may retry with the same blinded credential after a
     * backoff; WhatsApp Web retries up to three times with the same blinded input on this branch.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivatestatsSignCredentialResponseErrorRetry")
    final class ServerError implements IqIssuePrivateStatsTokenResponse {
        /**
         * Holds the numeric server-side error code read from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text read from the {@code <error text/>}
         * attribute, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply from the parsed error code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, or {@code null} when omitted
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
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when the relay
         *         omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>A populated result is returned only when the stanza is a {@code type="error"} envelope
         * echoing the {@code request} id and carrying an {@code <error/>} child whose {@code code}
         * attribute is {@code 500} or greater, per the partition enforced by
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
         *         when the stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPrivatestatsSignCredentialResponseErrorRetry",
                exports = "parseSignCredentialResponseErrorRetry",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply with the given object for equality.
         *
         * <p>Two replies are equal when their error codes match and their error texts are equal.
         *
         * @param obj the object to compare against
         * @return {@code true} if the objects are equal, {@code false} otherwise
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
         * Returns a hash code derived from the error code and error text.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string carrying the error code and error text.
         *
         * @return a string describing this reply
         */
        @Override
        public String toString() {
            return "IqIssuePrivateStatsTokenResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
