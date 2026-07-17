package com.github.auties00.cobalt.wire.stanza.iq.encrypt;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Closed family of reply variants observable on the {@code <iq xmlns="encrypt" type="get"/>}
 * digest-key roundtrip.
 *
 * <p>Each inbound {@code <iq>} that echoes an {@link IqDigestKeyRequest} is fed through
 * {@link #of(Stanza, Stanza)} and matched on the resulting sealed sub-type. {@link Success} exposes the
 * relay's reported registration id, key-bundle type, identity key, signed pre-key, one-time pre-key
 * identifier list and SHA-1 digest so the same hash can be recomputed locally to decide whether to
 * skip a {@link IqUploadPreKeysRequest pre-key re-upload}. {@link ClientError} surfaces the relay's
 * rejection envelope (notably {@code 404} "no record for this user", {@code 406} "malformed
 * request", or any other {@code 4xx}). {@link ServerError} carries {@code 5xx} envelopes such as
 * {@code 503} "service unavailable".
 */
@WhatsAppWebModule(moduleName = "WAWebDigestKeyJob")
public sealed interface IqDigestKeyResponse extends IqStanza.Response
        permits IqDigestKeyResponse.Success, IqDigestKeyResponse.ClientError, IqDigestKeyResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqDigestKeyResponse} variant.
     *
     * <p>Attempts {@link Success#of(Stanza, Stanza)} first, then {@link ClientError#of(Stanza, Stanza)},
     * then {@link ServerError#of(Stanza, Stanza)}. The {@code request} parameter is forwarded to each
     * variant parser so {@link SmaxIqResultResponseMixin} and {@link SmaxBaseServerErrorMixin} can
     * validate that the echoed envelope identifiers line up with the outbound stanza.
     *
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound stanza
     * @return the parsed variant wrapped in an {@link Optional}, or {@link Optional#empty()} when
     *         the stanza shape matched none of the three documented schemas
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebDigestKeyJob",
            exports = "digestKey", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqDigestKeyResponse> of(Stanza stanza, Stanza request) {
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
     * Successful echo from the relay carrying the full set of digest-key inputs.
     *
     * <p>The fields together allow the relay-side SHA-1 to be recomputed over
     * {@code identityPublicKey || signedPreKey.publicKey() || signedPreKey.signature() ||
     * concat(preKey.publicKey for each entry of preKeyIds)} and compared against {@link #hash()}. A
     * mismatch signals that the local key bundle has drifted out of sync with the server.
     */
    @WhatsAppWebModule(moduleName = "WAWebDigestKeyJob")
    final class Success implements IqDigestKeyResponse {
        /**
         * The relay's record of the local device's registration id, decoded from the four-byte
         * big-endian content of {@code <registration/>}.
         */
        private final int registrationId;

        /**
         * The single-byte Signal key-bundle type marker, read from the first byte of
         * {@code <type/>}.
         */
        private final byte keyBundleType;

        /**
         * The thirty-two-byte long-term identity public key, taken verbatim from
         * {@code <identity/>}.
         */
        private final byte[] identityPublicKey;

        /**
         * The currently advertised signed pre-key, parsed from the {@code <skey/>} subtree.
         */
        private final IqUploadPreKeysSignedPreKey signedPreKey;

        /**
         * The list of one-time pre-key identifiers, each decoded from a three-byte big-endian
         * {@code <list><key/>...} entry.
         */
        private final List<Integer> preKeyIds;

        /**
         * The relay-computed SHA-1 over the concatenated key material; twenty bytes read from
         * {@code <hash/>}.
         */
        private final byte[] hash;

        /**
         * Constructs a populated success envelope.
         *
         * <p>The {@code preKeyIds} list is defensively copied; mutating the passed-in list after
         * construction does not affect this instance.
         *
         * @param registrationId    the relay-side registration id
         * @param keyBundleType     the Signal key-bundle type marker
         * @param identityPublicKey the thirty-two-byte identity public key
         * @param signedPreKey      the parsed signed pre-key
         * @param preKeyIds         the one-time pre-key identifiers
         * @param hash              the twenty-byte SHA-1 digest
         * @throws NullPointerException if any reference argument is {@code null}
         */
        public Success(int registrationId, byte keyBundleType,
                       byte[] identityPublicKey, IqUploadPreKeysSignedPreKey signedPreKey,
                       List<Integer> preKeyIds, byte[] hash) {
            this.registrationId = registrationId;
            this.keyBundleType = keyBundleType;
            this.identityPublicKey = Objects.requireNonNull(identityPublicKey, "identityPublicKey cannot be null");
            this.signedPreKey = Objects.requireNonNull(signedPreKey, "signedPreKey cannot be null");
            Objects.requireNonNull(preKeyIds, "preKeyIds cannot be null");
            this.preKeyIds = List.copyOf(preKeyIds);
            this.hash = Objects.requireNonNull(hash, "hash cannot be null");
        }

        /**
         * Returns the relay-side registration id.
         *
         * <p>Compared against the local registration id to detect a device-identity drift before
         * recomputing the hash.
         *
         * @return the registration id
         */
        public int registrationId() {
            return registrationId;
        }

        /**
         * Returns the relay-side key-bundle type marker.
         *
         * @return the type marker
         */
        public byte keyBundleType() {
            return keyBundleType;
        }

        /**
         * Returns the relay-side identity public key.
         *
         * <p>Forms the first thirty-two-byte slice when recomputing the digest locally.
         *
         * @return the identity public key bytes
         */
        public byte[] identityPublicKey() {
            return identityPublicKey;
        }

        /**
         * Returns the relay-side signed pre-key.
         *
         * <p>Provides the second thirty-two-byte slice ({@link IqUploadPreKeysSignedPreKey#publicKey()})
         * and the sixty-four-byte signature slice ({@link IqUploadPreKeysSignedPreKey#signature()})
         * when recomputing the digest locally.
         *
         * @return the signed pre-key
         */
        public IqUploadPreKeysSignedPreKey signedPreKey() {
            return signedPreKey;
        }

        /**
         * Returns the unmodifiable list of one-time pre-key identifiers the relay is advertising.
         *
         * <p>For each identifier the local pre-key store entry is looked up and its thirty-two-byte
         * public key fed into the digest computation in list order.
         *
         * @return the identifiers
         */
        public List<Integer> preKeyIds() {
            return preKeyIds;
        }

        /**
         * Returns the relay-computed SHA-1 digest bytes.
         *
         * @return the twenty-byte digest
         */
        public byte[] hash() {
            return hash;
        }

        /**
         * Parses a {@link Success} variant from the inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the envelope fails the IQ-result echo check, when
         * the {@code <digest>} subtree is missing or short of any required child, or when any
         * content slot fails to decode. Parse failure is recoverable; the caller falls through to
         * the error variants.
         *
         * @implNote
         * This implementation drives a manual structural walk via {@link Stanza#getChild(String)} and
         * {@link Stanza#toContentBytes()} so that each variant returns {@link Optional#empty()} rather
         * than throwing, satisfying Cobalt's typed sealed-hierarchy contract.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed {@link Success}, or empty when the stanza shape does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebDigestKeyJob",
                exports = "digestResponseParser", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var digestChild = stanza.getChild("digest").orElse(null);
            if (digestChild == null) {
                return Optional.empty();
            }
            var registrationNode = digestChild.getChild("registration").orElse(null);
            var typeNode = digestChild.getChild("type").orElse(null);
            var identityNode = digestChild.getChild("identity").orElse(null);
            var skeyNode = digestChild.getChild("skey").orElse(null);
            var listNode = digestChild.getChild("list").orElse(null);
            var hashNode = digestChild.getChild("hash").orElse(null);
            if (registrationNode == null || typeNode == null || identityNode == null
                    || skeyNode == null || listNode == null || hashNode == null) {
                return Optional.empty();
            }
            var registrationBytes = registrationNode.toContentBytes().orElse(null);
            var typeBytes = typeNode.toContentBytes().orElse(null);
            var identityBytes = identityNode.toContentBytes().orElse(null);
            var hashBytes = hashNode.toContentBytes().orElse(null);
            if (registrationBytes == null || typeBytes == null || identityBytes == null
                    || hashBytes == null || typeBytes.length < 1) {
                return Optional.empty();
            }
            var registrationId = bigEndianUnsignedInt(registrationBytes);
            var keyBundleType = typeBytes[0];
            var skeyIdNode = skeyNode.getChild("id").orElse(null);
            var skeyValueNode = skeyNode.getChild("value").orElse(null);
            var skeySignatureNode = skeyNode.getChild("signature").orElse(null);
            if (skeyIdNode == null || skeyValueNode == null || skeySignatureNode == null) {
                return Optional.empty();
            }
            var skeyIdBytes = skeyIdNode.toContentBytes().orElse(null);
            var skeyValueBytes = skeyValueNode.toContentBytes().orElse(null);
            var skeySignatureBytes = skeySignatureNode.toContentBytes().orElse(null);
            if (skeyIdBytes == null || skeyValueBytes == null || skeySignatureBytes == null) {
                return Optional.empty();
            }
            var signedPreKey = new IqUploadPreKeysSignedPreKey(
                    bigEndianUnsignedInt(skeyIdBytes), skeyValueBytes, skeySignatureBytes);
            var preKeyIds = new ArrayList<Integer>();
            for (var preKeyChild : listNode.children()) {
                var idBytes = preKeyChild.toContentBytes().orElse(null);
                if (idBytes == null) {
                    return Optional.empty();
                }
                preKeyIds.add(bigEndianUnsignedInt(idBytes));
            }
            return Optional.of(new Success(registrationId, keyBundleType, identityBytes,
                    signedPreKey, preKeyIds, hashBytes));
        }

        /**
         * Decodes the supplied byte array as a big-endian unsigned integer.
         *
         * <p>Accepts one-to-four-byte inputs because the digest schema mixes a four-byte
         * {@code <registration/>} content with three-byte {@code <id/>} contents inside the
         * {@code <list/>} and {@code <skey/>} subtrees.
         *
         * @param bytes the source bytes
         * @return the decoded value
         */
        private static int bigEndianUnsignedInt(byte[] bytes) {
            var result = 0;
            for (var b : bytes) {
                result = (result << 8) | (b & 0xff);
            }
            return result;
        }

        /**
         * Compares this success envelope to another instance for equality.
         *
         * @param obj the candidate instance
         * @return {@code true} when {@code obj} is a {@code Success} carrying identical fields
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
            return this.registrationId == that.registrationId
                    && this.keyBundleType == that.keyBundleType
                    && Arrays.equals(this.identityPublicKey, that.identityPublicKey)
                    && Objects.equals(this.signedPreKey, that.signedPreKey)
                    && Objects.equals(this.preKeyIds, that.preKeyIds)
                    && Arrays.equals(this.hash, that.hash);
        }

        /**
         * Returns a hash code derived from every carried field.
         *
         * @return the combined hash
         */
        @Override
        public int hashCode() {
            var result = Objects.hash(registrationId, keyBundleType, signedPreKey, preKeyIds);
            result = 31 * result + Arrays.hashCode(identityPublicKey);
            result = 31 * result + Arrays.hashCode(hash);
            return result;
        }

        /**
         * Returns the record-style rendering for this success envelope.
         *
         * @return the rendered string
         */
        @Override
        public String toString() {
            return "IqDigestKeyResponse.Success[registrationId=" + registrationId
                    + ", keyBundleType=" + keyBundleType
                    + ", identityPublicKey=" + Arrays.toString(identityPublicKey)
                    + ", signedPreKey=" + signedPreKey
                    + ", preKeyIds=" + preKeyIds
                    + ", hash=" + Arrays.toString(hash) + ']';
        }
    }

    /**
     * Client-error variant; the relay rejected the digest probe with a {@code 4xx} envelope.
     *
     * <p>The two documented relay codes are {@code 404} ("no record for this user", which WA Web
     * treats as a hint to upload pre-keys from scratch) and {@code 406} ("malformed request", which
     * is logged and ignored).
     */
    @WhatsAppWebModule(moduleName = "WAWebDigestKeyJob")
    final class ClientError implements IqDigestKeyResponse {
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
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which
         * checks the envelope echo, the {@code type="error"} attribute and the {@code <error/>}
         * subtree before returning the code/text envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed {@link ClientError}, or empty when the stanza shape does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebDigestKeyJob",
                exports = "digestKey", adaptation = WhatsAppAdaptation.ADAPTED)
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
            return "IqDigestKeyResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Server-error variant; the relay reported a transient failure with a {@code 5xx} envelope.
     *
     * <p>The most common observed code is {@code 503} "service unavailable"; WA Web logs the digest
     * query as failed and lets the periodic re-check retry on the next push.
     */
    @WhatsAppWebModule(moduleName = "WAWebDigestKeyJob")
    final class ServerError implements IqDigestKeyResponse {
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
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, the
         * {@code 5xx} counterpart of {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed {@link ServerError}, or empty when the stanza shape does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebDigestKeyJob",
                exports = "digestKey", adaptation = WhatsAppAdaptation.ADAPTED)
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
            return "IqDigestKeyResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
