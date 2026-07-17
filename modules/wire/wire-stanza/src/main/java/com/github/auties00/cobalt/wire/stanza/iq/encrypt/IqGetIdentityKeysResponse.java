package com.github.auties00.cobalt.wire.stanza.iq.encrypt;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
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
 * Closed family of reply variants observable on the bulk identity-key
 * {@link IqGetIdentityKeysRequest} roundtrip.
 *
 * <p>{@link Success} carries one {@link Success.IdentityEntry} per requested device JID,
 * interleaving {@link Success.IdentityEntry.Resolved} entries with per-device
 * {@link Success.IdentityEntry.Failure} envelopes when the relay could not resolve a particular
 * device. {@link ClientError} and {@link ServerError} are envelope-level failures that affect the
 * whole batch.
 *
 * @implNote
 * WA Web's {@code identityKeysParser} throws on the first per-user {@code <error/>} it encounters
 * and discards the whole batch. This implementation instead keeps a per-device
 * {@link Success.IdentityEntry.Failure} record so the caller can still resolve identities for the
 * devices that succeeded and re-query just the failed ones.
 */
@WhatsAppWebModule(moduleName = "WAWebGetIdentityKeysJob")
public sealed interface IqGetIdentityKeysResponse extends IqStanza.Response
        permits IqGetIdentityKeysResponse.Success, IqGetIdentityKeysResponse.ClientError, IqGetIdentityKeysResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqGetIdentityKeysResponse} variant.
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
    @WhatsAppWebExport(moduleName = "WAWebGetIdentityKeysJob",
            exports = "getAndStoreIdentityKeys", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqGetIdentityKeysResponse> of(Stanza stanza, Stanza request) {
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
     * Successful bulk reply carrying one {@link IdentityEntry} per requested device.
     *
     * <p>{@link #entries()} is iterated and each variant pattern-matched: {@link IdentityEntry.Resolved}
     * exposes the device's identity key for installation into the local Signal store;
     * {@link IdentityEntry.Failure} carries the per-device error envelope the relay attached.
     */
    @WhatsAppWebModule(moduleName = "WAWebGetIdentityKeysJob")
    final class Success implements IqGetIdentityKeysResponse {
        /**
         * The list of per-device identity entries, interleaving {@link IdentityEntry.Resolved} and
         * {@link IdentityEntry.Failure} in {@code <list/>} order.
         */
        private final List<IdentityEntry> entries;

        /**
         * Constructs a populated success envelope.
         *
         * <p>The {@code entries} list is defensively copied; mutating the passed-in list after
         * construction does not affect this instance.
         *
         * @param entries the per-device identity entries
         * @throws NullPointerException if {@code entries} is {@code null}
         */
        public Success(List<IdentityEntry> entries) {
            Objects.requireNonNull(entries, "entries cannot be null");
            this.entries = List.copyOf(entries);
        }

        /**
         * Returns the unmodifiable list of per-device identity entries.
         *
         * @return the entries
         */
        public List<IdentityEntry> entries() {
            return entries;
        }

        /**
         * Parses a {@link Success} variant from the inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the envelope fails the IQ-result echo check, when
         * the top-level {@code <list/>} is missing, or when any {@code <user/>} grandchild lacks a
         * {@code jid} attribute or has malformed {@code <type/>} or {@code <identity/>} content.
         *
         * @implNote
         * This implementation walks the {@code <list/>} grandchildren and records per-device
         * outcomes without throwing. A {@code <user/>} carrying an {@code <error/>} grandchild
         * yields an {@link IdentityEntry.Failure}; otherwise the parser pulls the single-byte
         * {@link IdentityEntry.Resolved#keyBundleType()} and the thirty-two-byte
         * {@link IdentityEntry.Resolved#identityPublicKey()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed {@link Success}, or empty when the stanza shape does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebGetIdentityKeysJob",
                exports = "identityKeysParser", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var listNode = stanza.getChild("list").orElse(null);
            if (listNode == null) {
                return Optional.empty();
            }
            var entries = new ArrayList<IdentityEntry>();
            for (var userNode : listNode.children()) {
                var deviceJid = userNode.getAttributeAsJid("jid").orElse(null);
                if (deviceJid == null) {
                    return Optional.empty();
                }
                var errorChild = userNode.getChild("error").orElse(null);
                if (errorChild != null) {
                    var errorCode = errorChild.getAttributeAsInt("code").orElse(0);
                    var errorText = errorChild.getAttributeAsString("text").orElse(null);
                    entries.add(new IdentityEntry.Failure(deviceJid, errorCode, errorText));
                    continue;
                }
                var typeChild = userNode.getChild("type").orElse(null);
                var identityChild = userNode.getChild("identity").orElse(null);
                if (typeChild == null || identityChild == null) {
                    return Optional.empty();
                }
                var typeBytes = typeChild.toContentBytes().orElse(null);
                var identityBytes = identityChild.toContentBytes().orElse(null);
                if (typeBytes == null || typeBytes.length < 1 || identityBytes == null) {
                    return Optional.empty();
                }
                entries.add(new IdentityEntry.Resolved(deviceJid, typeBytes[0], identityBytes));
            }
            return Optional.of(new Success(entries));
        }

        /**
         * Compares this success envelope to another instance for equality.
         *
         * @param obj the candidate instance
         * @return {@code true} when {@code obj} is a {@code Success} carrying an equal entries list
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
            return Objects.equals(this.entries, that.entries);
        }

        /**
         * Returns a hash code derived from the entries list.
         *
         * @return the combined hash
         */
        @Override
        public int hashCode() {
            return Objects.hash(entries);
        }

        /**
         * Returns the record-style rendering for this success envelope.
         *
         * @return the rendered string
         */
        @Override
        public String toString() {
            return "IqGetIdentityKeysResponse.Success[entries=" + entries + ']';
        }
    }

    /**
     * Closed per-device entry variant carrying either a successfully fetched identity key or the
     * relay's per-device error envelope.
     *
     * <p>Pattern-matching on {@link Resolved} versus {@link Failure} dispatches identity-key
     * installation against error logging on a per-device basis.
     */
    @WhatsAppWebModule(moduleName = "WAWebGetIdentityKeysJob")
    sealed interface IdentityEntry permits IdentityEntry.Resolved, IdentityEntry.Failure {

        /**
         * Returns the device JID this entry corresponds to.
         *
         * @return the device JID
         */
        Jid deviceJid();

        /**
         * Per-device resolved entry; the relay returned the device's long-term identity public key.
         *
         * <p>The {@link #identityPublicKey()} bytes are recorded against the Signal address derived
         * from {@link #deviceJid()} after conversion to the Signal Curve25519 public-key form.
         */
        @WhatsAppWebModule(moduleName = "WAWebGetIdentityKeysJob")
        final class Resolved implements IdentityEntry {
            /**
             * The device JID this entry corresponds to.
             */
            private final Jid deviceJid;

            /**
             * The single-byte Signal key-bundle type marker, read from the {@code <type/>}
             * grandchild.
             */
            private final byte keyBundleType;

            /**
             * The thirty-two-byte identity public key, taken verbatim from the
             * {@code <identity/>} grandchild.
             */
            private final byte[] identityPublicKey;

            /**
             * Constructs a populated resolved entry.
             *
             * @param deviceJid         the device JID
             * @param keyBundleType     the type marker
             * @param identityPublicKey the identity public key bytes
             * @throws NullPointerException if {@code deviceJid} or {@code identityPublicKey} is
             *                              {@code null}
             */
            public Resolved(Jid deviceJid, byte keyBundleType, byte[] identityPublicKey) {
                this.deviceJid = Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
                this.keyBundleType = keyBundleType;
                this.identityPublicKey = Objects.requireNonNull(identityPublicKey, "identityPublicKey cannot be null");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Jid deviceJid() {
                return deviceJid;
            }

            /**
             * Returns the key-bundle type marker.
             *
             * @return the type marker
             */
            public byte keyBundleType() {
                return keyBundleType;
            }

            /**
             * Returns the identity public key bytes.
             *
             * @return the thirty-two-byte identity public key
             */
            public byte[] identityPublicKey() {
                return identityPublicKey;
            }

            /**
             * Compares this resolved entry to another instance for equality.
             *
             * @param obj the candidate instance
             * @return {@code true} when {@code obj} is a {@code Resolved} carrying identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Resolved) obj;
                return this.keyBundleType == that.keyBundleType
                        && Objects.equals(this.deviceJid, that.deviceJid)
                        && Arrays.equals(this.identityPublicKey, that.identityPublicKey);
            }

            /**
             * Returns a hash code derived from every carried field.
             *
             * @return the combined hash
             */
            @Override
            public int hashCode() {
                var result = Objects.hash(deviceJid, keyBundleType);
                result = 31 * result + Arrays.hashCode(identityPublicKey);
                return result;
            }

            /**
             * Returns the record-style rendering for this resolved entry.
             *
             * @return the rendered string
             */
            @Override
            public String toString() {
                return "IqGetIdentityKeysResponse.IdentityEntry.Resolved[deviceJid=" + deviceJid
                        + ", keyBundleType=" + keyBundleType
                        + ", identityPublicKey=" + Arrays.toString(identityPublicKey) + ']';
            }
        }

        /**
         * Per-device failure entry; the relay returned an {@code <error/>} grandchild instead of a
         * resolved identity key for this device JID.
         *
         * @implNote
         * WA Web's {@code identityKeysParser} would have thrown and abandoned the whole batch; this
         * implementation records the failure per-device so the caller can still process the
         * resolved entries from the same response.
         */
        @WhatsAppWebModule(moduleName = "WAWebGetIdentityKeysJob")
        final class Failure implements IdentityEntry {
            /**
             * The device JID this failure corresponds to.
             */
            private final Jid deviceJid;

            /**
             * The numeric per-device error code carried by {@code <error code="..."/>}.
             */
            private final int errorCode;

            /**
             * The optional human-readable per-device error text carried by
             * {@code <error text="..."/>}.
             */
            private final String errorText;

            /**
             * Constructs a populated failure entry.
             *
             * @param deviceJid the device JID
             * @param errorCode the numeric error code
             * @param errorText the optional human-readable text, possibly {@code null}
             * @throws NullPointerException if {@code deviceJid} is {@code null}
             */
            public Failure(Jid deviceJid, int errorCode, String errorText) {
                this.deviceJid = Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
                this.errorCode = errorCode;
                this.errorText = errorText;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Jid deviceJid() {
                return deviceJid;
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
             * @return an {@link Optional} carrying the error text, or empty when the relay omitted
             *         it
             */
            public Optional<String> errorText() {
                return Optional.ofNullable(errorText);
            }

            /**
             * Compares this failure entry to another instance for equality.
             *
             * @param obj the candidate instance
             * @return {@code true} when {@code obj} is a {@code Failure} carrying identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Failure) obj;
                return this.errorCode == that.errorCode
                        && Objects.equals(this.deviceJid, that.deviceJid)
                        && Objects.equals(this.errorText, that.errorText);
            }

            /**
             * Returns a hash code derived from every carried field.
             *
             * @return the combined hash
             */
            @Override
            public int hashCode() {
                return Objects.hash(deviceJid, errorCode, errorText);
            }

            /**
             * Returns the record-style rendering for this failure entry.
             *
             * @return the rendered string
             */
            @Override
            public String toString() {
                return "IqGetIdentityKeysResponse.IdentityEntry.Failure[deviceJid=" + deviceJid
                        + ", errorCode=" + errorCode
                        + ", errorText=" + errorText + ']';
            }
        }
    }

    /**
     * Client-error variant; the relay rejected the whole bulk fetch with a {@code 4xx} envelope.
     *
     * <p>Distinct from {@link Success.IdentityEntry.Failure}, which is a per-device failure inside
     * an otherwise successful batch.
     */
    @WhatsAppWebModule(moduleName = "WAWebGetIdentityKeysJob")
    final class ClientError implements IqGetIdentityKeysResponse {
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
        @WhatsAppWebExport(moduleName = "WAWebGetIdentityKeysJob",
                exports = "getAndStoreIdentityKeys", adaptation = WhatsAppAdaptation.ADAPTED)
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
            return "IqGetIdentityKeysResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Server-error variant; the relay reported a transient failure with a {@code 5xx} envelope
     * while processing the bulk fetch.
     *
     * <p>Affects the whole batch; the caller typically retries after the surrounding contact-sync
     * or device-sync flow re-runs.
     */
    @WhatsAppWebModule(moduleName = "WAWebGetIdentityKeysJob")
    final class ServerError implements IqGetIdentityKeysResponse {
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
        @WhatsAppWebExport(moduleName = "WAWebGetIdentityKeysJob",
                exports = "getAndStoreIdentityKeys", adaptation = WhatsAppAdaptation.ADAPTED)
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
            return "IqGetIdentityKeysResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
