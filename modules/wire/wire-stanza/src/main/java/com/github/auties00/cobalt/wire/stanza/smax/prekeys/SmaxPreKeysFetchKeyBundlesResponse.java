package com.github.auties00.cobalt.wire.stanza.smax.prekeys;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Enumerates the inbound reply variants produced by the relay in response to a {@link SmaxPreKeysFetchKeyBundlesRequest}.
 *
 * <p>The relay answers with one of three documented shapes: {@link Success} (per-user bundles
 * attached), {@link ClientError} (the outer request was malformed), and {@link ServerError}
 * (transient server-side failure). The {@link Success} branch can still contain per-user
 * {@link Success.UserError} entries for individual addressees the relay could not serve.
 */
public sealed interface SmaxPreKeysFetchKeyBundlesResponse extends SmaxStanza.Response
        permits SmaxPreKeysFetchKeyBundlesResponse.Success, SmaxPreKeysFetchKeyBundlesResponse.ClientError, SmaxPreKeysFetchKeyBundlesResponse.ServerError {

    /**
     * Tries each {@link SmaxPreKeysFetchKeyBundlesResponse} variant in priority order and returns the first that parses cleanly.
     *
     * <p>The absence of any match indicates a protocol violation, surfaced as
     * {@link Optional#empty()} for the caller to translate into the appropriate exception.
     *
     * @implNote
     * This implementation tries {@link Success}, then {@link ClientError}, then {@link ServerError},
     * mirroring WA Web's {@code parseFetchKeyBundlesResponseSuccess} /
     * {@code parseFetchKeyBundlesResponseRequestError} /
     * {@code parseFetchKeyBundlesResponseServerError} ordering.
     *
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no
     *         documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxPreKeysFetchKeyBundlesRPC",
            exports = "sendFetchKeyBundlesRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxPreKeysFetchKeyBundlesResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the {@code Success} reply variant, wrapping the {@code <list>} of one {@code <user>} child per addressee.
     *
     * <p>Each {@link UserEntry} is either a {@link UserKeyBundle} (the relay had the bundle on hand)
     * or a {@link UserError} (per-user failure with the other addressees still resolved). The send
     * path iterates this list and seeds Signal sessions in bulk.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchKeyBundlesResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysIQResultResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysResponsePaddingMixin")
    final class Success implements SmaxPreKeysFetchKeyBundlesResponse {
        /**
         * Holds the per-user entries in the same order the relay produced them.
         */
        private final List<UserEntry> users;

        /**
         * Constructs a successful reply.
         *
         * <p>The list is defensively copied so the constructed value is immutable.
         *
         * @param users the per-user entries; defaults to an empty list when {@code null}
         */
        public Success(List<UserEntry> users) {
            this.users = List.copyOf(Objects.requireNonNullElse(users, List.of()));
        }

        /**
         * Returns the list of per-user entries.
         *
         * <p>Each element is either a {@link UserKeyBundle} or a {@link UserError}; switch on the
         * sealed disjunction in the consumer.
         *
         * @return an unmodifiable {@link List} of {@link UserEntry}
         */
        public List<UserEntry> users() {
            return users;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the envelope fails the standard
         * {@code <iq type="result">} validation, when no {@code <list>} child is present, or when
         * any one {@code <user>} grandchild fails per-user parsing.
         *
         * @implNote
         * This implementation walks the {@code <list>} child once, collecting per-user entries via
         * {@link UserEntry#of(Stanza)}; any failed entry aborts the whole parse so the caller falls
         * through to the error branches.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchKeyBundlesResponseSuccess",
                exports = "parseFetchKeyBundlesResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var listNode = stanza.getChild("list").orElse(null);
            if (listNode == null) {
                return Optional.empty();
            }
            var entries = new ArrayList<UserEntry>();
            for (var userNode : listNode.getChildren("user")) {
                var entry = UserEntry.of(userNode).orElse(null);
                if (entry == null) {
                    return Optional.empty();
                }
                entries.add(entry);
            }
            return Optional.of(new Success(entries));
        }

        /**
         * {@inheritDoc}
         *
         * <p>Two instances are equal when their {@link #users} entries are equal in order.
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
            return Objects.equals(this.users, that.users);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Hashes the {@link #users} list to stay consistent with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(users);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxPreKeysFetchKeyBundlesResponse.Success[users=" + users + ']';
        }

        /**
         * Models the per-user reply as a disjunction of the two possible shapes.
         *
         * <p>An entry is either a {@link UserKeyBundle} when the relay had the bundle on hand, or a
         * {@link UserError} when the bundle could not be assembled (user blocked, registration
         * mismatch, no pre-keys uploaded, and so on).
         */
        public sealed interface UserEntry permits UserKeyBundle, UserError {

            /**
             * Returns the per-user {@link Jid} echoed by the relay.
             *
             * <p>Common to both variants so callers can correlate the entry with the original
             * request without down-casting.
             *
             * @return the per-user {@link Jid}
             */
            Jid userJid();

            /**
             * Tries to parse a {@link UserEntry} from the given {@code <user>} grandchild.
             *
             * <p>Lifts one {@code <user>} stanza into the sealed disjunction; returns
             * {@link Optional#empty()} when the stanza matches neither the success shape nor the error
             * shape.
             *
             * @implSpec
             * Implementations of the permitted variants must accept exactly one wire shape each so
             * that this factory can disambiguate them by trying each in turn.
             *
             * @implNote
             * This implementation tries {@link UserKeyBundle} first then {@link UserError}, matching
             * the WA Web parser's priority.
             *
             * @param userStanza the {@code <user>} grandchild
             * @return an {@link Optional} carrying the parsed entry, or empty on no-match
             * @throws NullPointerException if {@code userStanza} is {@code null}
             */
            static Optional<UserEntry> of(Stanza userStanza) {
                Objects.requireNonNull(userStanza, "userStanza cannot be null");
                var success = UserKeyBundle.of(userStanza).orElse(null);
                if (success != null) {
                    return Optional.of(success);
                }
                return UserError.of(userStanza).map(error -> error);
            }
        }

        /**
         * Carries the full Signal pre-key bundle for one successfully resolved user.
         *
         * <p>The fields map one-to-one onto the {@code prekeyBundle} record consumed by
         * {@code WAWebProcessKeyBundle.processKeyBundles} on the WA Web side: registration id,
         * identity key, optional unsigned pre-key, mandatory signed pre-key, and an optional
         * device-identity attestation when the request asked for it.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchKeyBundlesUserSuccessMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysRegistrationIDMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysKeyTypeMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysIdentityKeyMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysPreKeyMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysSignedPreKeyMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysDeviceIdentityMixin")
        public static final class UserKeyBundle implements UserEntry {
            /**
             * Holds the per-user {@link Jid} echoed by the relay.
             */
            private final Jid userJid;

            /**
             * Holds the optional relay-side timestamp from the {@code t} attribute, in seconds since the UNIX epoch.
             */
            private final Long timestamp;

            /**
             * Records whether the {@code is_cloud_api="true"} marker, set for Cloud-API hosted accounts, was present.
             */
            private final boolean cloudApi;

            /**
             * Holds the 4-byte registration id (raw bytes, big-endian).
             */
            private final byte[] registrationId;

            /**
             * Holds the optional 1-byte key-type marker (literal {@code [5]} for Curve25519); absent on legacy bundles.
             */
            private final byte[] keyType;

            /**
             * Holds the 32-byte Signal identity public key.
             */
            private final byte[] identityKey;

            /**
             * Holds the optional unsigned pre-key projection; {@code null} when the relay had no fresh pre-keys to serve, in which case the caller falls back to the signed pre-key alone.
             */
            private final PreKey preKey;

            /**
             * Holds the signed pre-key projection, always present in a successful bundle.
             */
            private final SignedPreKey signedPreKey;

            /**
             * Holds the optional device-identity attestation bytes, present only when the request set {@code reason="identity"}.
             */
            private final byte[] deviceIdentity;

            /**
             * Constructs a key-bundle projection.
             *
             * <p>Embedders usually obtain instances through {@link #of(Stanza)} rather than this
             * constructor directly.
             *
             * @param userJid        the per-user {@link Jid}
             * @param timestamp      the optional relay timestamp
             * @param cloudApi       whether the {@code is_cloud_api} marker was set
             * @param registrationId the 4-byte registration id
             * @param keyType        the optional 1-byte key-type marker
             * @param identityKey    the 32-byte identity key
             * @param preKey         the optional unsigned pre-key
             * @param signedPreKey   the signed pre-key
             * @param deviceIdentity the optional device-identity bytes
             * @throws NullPointerException if any non-optional argument is {@code null}
             */
            public UserKeyBundle(Jid userJid,
                                 Long timestamp,
                                 boolean cloudApi,
                                 byte[] registrationId,
                                 byte[] keyType,
                                 byte[] identityKey,
                                 PreKey preKey,
                                 SignedPreKey signedPreKey,
                                 byte[] deviceIdentity) {
                this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
                this.timestamp = timestamp;
                this.cloudApi = cloudApi;
                this.registrationId = Objects.requireNonNull(registrationId, "registrationId cannot be null");
                this.keyType = keyType;
                this.identityKey = Objects.requireNonNull(identityKey, "identityKey cannot be null");
                this.preKey = preKey;
                this.signedPreKey = Objects.requireNonNull(signedPreKey, "signedPreKey cannot be null");
                this.deviceIdentity = deviceIdentity;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Jid userJid() {
                return userJid;
            }

            /**
             * Returns the optional relay timestamp.
             *
             * <p>Surfaced for audit and replay-window enforcement; the caller can ignore it when not
             * needed.
             *
             * @return an {@link Optional} carrying the timestamp, or empty when absent
             */
            public Optional<Long> timestamp() {
                return Optional.ofNullable(timestamp);
            }

            /**
             * Returns whether the {@code is_cloud_api="true"} marker was set on the entry.
             *
             * <p>Lets callers route Cloud-API bundles through any custom handling distinct from
             * regular user accounts.
             *
             * @return {@code true} when the marker was present
             */
            public boolean cloudApi() {
                return cloudApi;
            }

            /**
             * Returns the 4-byte registration id.
             *
             * <p>Decoded as an unsigned big-endian integer by {@code WAWebProcessKeyBundle} on WA
             * Web; embedders that feed Signal directly do the same.
             *
             * @return the raw bytes
             */
            public byte[] registrationId() {
                return registrationId;
            }

            /**
             * Returns the optional 1-byte key-type marker.
             *
             * <p>Distinguishes Curve25519 ({@code [5]}) from legacy bundles that omit the marker;
             * modern bundles always include it.
             *
             * @return an {@link Optional} carrying the key-type bytes, or empty when absent
             */
            public Optional<byte[]> keyType() {
                return Optional.ofNullable(keyType);
            }

            /**
             * Returns the 32-byte Signal identity public key.
             *
             * <p>Feeds the Signal session as the addressee's long-term public identity.
             *
             * @return the raw bytes
             */
            public byte[] identityKey() {
                return identityKey;
            }

            /**
             * Returns the optional unsigned pre-key projection.
             *
             * <p>The Signal layer tolerates a missing pre-key by falling back to the signed pre-key
             * alone, matching the WA Web behaviour.
             *
             * @return an {@link Optional} carrying the {@link PreKey}
             */
            public Optional<PreKey> preKey() {
                return Optional.ofNullable(preKey);
            }

            /**
             * Returns the signed pre-key projection.
             *
             * <p>Always present in a successful bundle; the caller can dereference without an
             * {@link Optional}.
             *
             * @return the {@link SignedPreKey}
             */
            public SignedPreKey signedPreKey() {
                return signedPreKey;
            }

            /**
             * Returns the optional device-identity attestation bytes.
             *
             * <p>Present only when the original request set
             * {@link SmaxPreKeysFetchKeyBundlesRequest.UserKeyRequest#hasUserReasonIdentity()};
             * the caller verifies the attestation before trusting the bundle for first contact.
             *
             * @return an {@link Optional} carrying the bytes
             */
            public Optional<byte[]> deviceIdentity() {
                return Optional.ofNullable(deviceIdentity);
            }

            /**
             * Tries to parse a {@link UserKeyBundle} from the given {@code <user>} grandchild.
             *
             * <p>Returns {@link Optional#empty()} for any malformed sub-tree (wrong length, missing
             * required child, and so on), letting {@link UserEntry#of(Stanza)} fall through to the
             * error projection.
             *
             * @implNote
             * This implementation enforces the WA Web length contracts on each child: 4 bytes for
             * registration id, 1 byte for key type, 32 bytes for identity key; size mismatches abort
             * the parse. The optional {@code <key>} child is rejected when present but malformed,
             * matching the relay's "optional sub-tree must parse cleanly" contract.
             *
             * @param userStanza the {@code <user>} grandchild
             * @return an {@link Optional} carrying the parsed bundle, or empty on no-match
             * @throws NullPointerException if {@code userStanza} is {@code null}
             */
            @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchKeyBundlesUserSuccessMixin",
                    exports = "parseFetchKeyBundlesUserSuccessMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<UserKeyBundle> of(Stanza userStanza) {
                Objects.requireNonNull(userStanza, "userStanza cannot be null");
                if (!userStanza.hasDescription("user")) {
                    return Optional.empty();
                }
                var jid = userStanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                Long timestamp = null;
                if (userStanza.hasAttribute("t")) {
                    var t = userStanza.getAttributeAsLong("t");
                    if (t.isEmpty()) {
                        return Optional.empty();
                    }
                    timestamp = t.getAsLong();
                }
                var cloudApi = userStanza.hasAttribute("is_cloud_api", "true");
                var registrationBytes = userStanza.getChild("registration")
                        .flatMap(Stanza::toContentBytes)
                        .orElse(null);
                if (registrationBytes == null || registrationBytes.length != 4) {
                    return Optional.empty();
                }
                byte[] keyTypeBytes = null;
                var typeNode = userStanza.getChild("type").orElse(null);
                if (typeNode != null) {
                    var typeContent = typeNode.toContentBytes().orElse(null);
                    if (typeContent == null || !Arrays.equals(typeContent, new byte[]{5})) {
                        keyTypeBytes = null;
                    } else {
                        keyTypeBytes = typeContent;
                    }
                }
                var identityBytes = userStanza.getChild("identity")
                        .flatMap(Stanza::toContentBytes)
                        .orElse(null);
                if (identityBytes == null || identityBytes.length != 32) {
                    return Optional.empty();
                }
                PreKey preKeyValue = null;
                var preKeyNode = userStanza.getChild("key").orElse(null);
                if (preKeyNode != null) {
                    preKeyValue = PreKey.of(preKeyNode).orElse(null);
                    if (preKeyValue == null) {
                        return Optional.empty();
                    }
                }
                var signedPreKeyNode = userStanza.getChild("skey").orElse(null);
                if (signedPreKeyNode == null) {
                    return Optional.empty();
                }
                var signedPreKeyValue = SignedPreKey.of(signedPreKeyNode).orElse(null);
                if (signedPreKeyValue == null) {
                    return Optional.empty();
                }
                var deviceIdentityBytes = userStanza.getChild("device-identity")
                        .flatMap(Stanza::toContentBytes)
                        .orElse(null);
                return Optional.of(new UserKeyBundle(
                        jid,
                        timestamp,
                        cloudApi,
                        registrationBytes,
                        keyTypeBytes,
                        identityBytes,
                        preKeyValue,
                        signedPreKeyValue,
                        deviceIdentityBytes));
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation compares every carried field; byte arrays go through
             * {@link Arrays#equals(byte[], byte[])}.
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (UserKeyBundle) obj;
                return this.cloudApi == that.cloudApi
                        && Objects.equals(this.userJid, that.userJid)
                        && Objects.equals(this.timestamp, that.timestamp)
                        && Arrays.equals(this.registrationId, that.registrationId)
                        && Arrays.equals(this.keyType, that.keyType)
                        && Arrays.equals(this.identityKey, that.identityKey)
                        && Objects.equals(this.preKey, that.preKey)
                        && Objects.equals(this.signedPreKey, that.signedPreKey)
                        && Arrays.equals(this.deviceIdentity, that.deviceIdentity);
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation rolls the scalar fields through {@link Objects#hash(Object...)}
             * and combines them with {@link Arrays#hashCode(byte[])} for the byte-array fields,
             * staying consistent with {@link #equals(Object)}.
             */
            @Override
            public int hashCode() {
                var result = Objects.hash(userJid, timestamp, cloudApi, preKey, signedPreKey);
                result = 31 * result + Arrays.hashCode(registrationId);
                result = 31 * result + Arrays.hashCode(keyType);
                result = 31 * result + Arrays.hashCode(identityKey);
                result = 31 * result + Arrays.hashCode(deviceIdentity);
                return result;
            }

            /**
             * {@inheritDoc}
             *
             * @implNote
             * This implementation renders byte arrays as a length-only summary to avoid leaking key
             * material into logs while keeping the output diff-friendly.
             */
            @Override
            public String toString() {
                return "SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle[userJid=" + userJid
                        + ", timestamp=" + timestamp
                        + ", cloudApi=" + cloudApi
                        + ", registrationId=" + (registrationId != null ? registrationId.length + " bytes" : "null")
                        + ", keyType=" + (keyType != null ? keyType.length + " bytes" : "null")
                        + ", identityKey=" + (identityKey != null ? identityKey.length + " bytes" : "null")
                        + ", preKey=" + preKey
                        + ", signedPreKey=" + signedPreKey
                        + ", deviceIdentity=" + (deviceIdentity != null ? deviceIdentity.length + " bytes" : "null") + ']';
            }

            /**
             * Carries the unsigned pre-key half of the X3DH handshake.
             *
             * <p>Pairs a 3-byte key identifier with 32 bytes of public-key material; consumed by the
             * Signal layer as the one-shot ephemeral key.
             */
            @WhatsAppWebModule(moduleName = "WASmaxInPreKeysPreKeyMixin")
            @WhatsAppWebModule(moduleName = "WASmaxInPreKeysKeyIDMixin")
            @WhatsAppWebModule(moduleName = "WASmaxInPreKeysKeyDataMixin")
            public static final class PreKey {
                /**
                 * Holds the 3-byte pre-key identifier (raw bytes, big-endian).
                 */
                private final byte[] keyId;

                /**
                 * Holds the 32-byte public-key material.
                 */
                private final byte[] keyValue;

                /**
                 * Constructs a pre-key projection.
                 *
                 * <p>Built by {@link #of(Stanza)} after both child elements pass length validation.
                 *
                 * @param keyId    the 3-byte key id
                 * @param keyValue the 32-byte key material
                 * @throws NullPointerException if any argument is {@code null}
                 */
                public PreKey(byte[] keyId, byte[] keyValue) {
                    this.keyId = Objects.requireNonNull(keyId, "keyId cannot be null");
                    this.keyValue = Objects.requireNonNull(keyValue, "keyValue cannot be null");
                }

                /**
                 * Returns the 3-byte pre-key identifier.
                 *
                 * <p>Decoded as an unsigned big-endian integer by the Signal layer.
                 *
                 * @return the raw bytes
                 */
                public byte[] keyId() {
                    return keyId;
                }

                /**
                 * Returns the 32-byte public-key material.
                 *
                 * <p>Feeds the X3DH handshake as the ephemeral half of the key agreement.
                 *
                 * @return the raw bytes
                 */
                public byte[] keyValue() {
                    return keyValue;
                }

                /**
                 * Tries to parse a {@link PreKey} from the given {@code <key>} element.
                 *
                 * <p>Returns {@link Optional#empty()} for any sub-tree whose {@code <id>} or
                 * {@code <value>} bytes are missing or the wrong length.
                 *
                 * @implNote
                 * This implementation enforces a 3-byte id and a 32-byte value per the WA Web
                 * fixture.
                 *
                 * @param keyStanza the {@code <key>} element
                 * @return an {@link Optional} carrying the parsed pre-key, or empty when malformed
                 * @throws NullPointerException if {@code keyStanza} is {@code null}
                 */
                public static Optional<PreKey> of(Stanza keyStanza) {
                    Objects.requireNonNull(keyStanza, "keyStanza cannot be null");
                    var idBytes = keyStanza.getChild("id")
                            .flatMap(Stanza::toContentBytes)
                            .orElse(null);
                    if (idBytes == null || idBytes.length != 3) {
                        return Optional.empty();
                    }
                    var valueBytes = keyStanza.getChild("value")
                            .flatMap(Stanza::toContentBytes)
                            .orElse(null);
                    if (valueBytes == null || valueBytes.length != 32) {
                        return Optional.empty();
                    }
                    return Optional.of(new PreKey(idBytes, valueBytes));
                }

                /**
                 * {@inheritDoc}
                 *
                 * @implNote
                 * This implementation compares both byte arrays via
                 * {@link Arrays#equals(byte[], byte[])}.
                 */
                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    }
                    if (obj == null || obj.getClass() != this.getClass()) {
                        return false;
                    }
                    var that = (PreKey) obj;
                    return Arrays.equals(this.keyId, that.keyId)
                            && Arrays.equals(this.keyValue, that.keyValue);
                }

                /**
                 * {@inheritDoc}
                 *
                 * @implNote
                 * This implementation combines {@link Arrays#hashCode(byte[])} for both fields to
                 * stay consistent with {@link #equals(Object)}.
                 */
                @Override
                public int hashCode() {
                    var result = Arrays.hashCode(keyId);
                    result = 31 * result + Arrays.hashCode(keyValue);
                    return result;
                }

                /**
                 * {@inheritDoc}
                 *
                 * @implNote
                 * This implementation renders byte arrays as a length-only summary to avoid leaking
                 * key material into logs.
                 */
                @Override
                public String toString() {
                    return "SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.PreKey[keyId="
                            + (keyId != null ? keyId.length + " bytes" : "null")
                            + ", keyValue=" + (keyValue != null ? keyValue.length + " bytes" : "null") + ']';
                }
            }

            /**
             * Carries the signed pre-key half of the X3DH handshake.
             *
             * <p>Pairs a 3-byte key id with 32 bytes of public-key material plus a 64-byte
             * signature; consumed by the Signal layer as the long-lived key.
             */
            @WhatsAppWebModule(moduleName = "WASmaxInPreKeysSignedPreKeyMixin")
            @WhatsAppWebModule(moduleName = "WASmaxInPreKeysKeyIDMixin")
            @WhatsAppWebModule(moduleName = "WASmaxInPreKeysKeyDataMixin")
            public static final class SignedPreKey {
                /**
                 * Holds the 3-byte signed-pre-key identifier (raw bytes, big-endian).
                 */
                private final byte[] keyId;

                /**
                 * Holds the 32-byte public-key material.
                 */
                private final byte[] keyValue;

                /**
                 * Holds the 64-byte signature over the key value, produced by the user's identity key.
                 */
                private final byte[] signature;

                /**
                 * Constructs a signed pre-key projection.
                 *
                 * <p>Built by {@link #of(Stanza)} after every child passes length validation.
                 *
                 * @param keyId     the 3-byte key id
                 * @param keyValue  the 32-byte key material
                 * @param signature the 64-byte signature
                 * @throws NullPointerException if any argument is {@code null}
                 */
                public SignedPreKey(byte[] keyId, byte[] keyValue, byte[] signature) {
                    this.keyId = Objects.requireNonNull(keyId, "keyId cannot be null");
                    this.keyValue = Objects.requireNonNull(keyValue, "keyValue cannot be null");
                    this.signature = Objects.requireNonNull(signature, "signature cannot be null");
                }

                /**
                 * Returns the 3-byte signed-pre-key identifier.
                 *
                 * <p>Decoded as an unsigned big-endian integer by the Signal layer.
                 *
                 * @return the raw bytes
                 */
                public byte[] keyId() {
                    return keyId;
                }

                /**
                 * Returns the 32-byte public-key material.
                 *
                 * <p>Feeds the X3DH handshake as the signed half of the key agreement.
                 *
                 * @return the raw bytes
                 */
                public byte[] keyValue() {
                    return keyValue;
                }

                /**
                 * Returns the 64-byte signature.
                 *
                 * <p>Verified against the user's identity key before the Signal layer trusts the
                 * signed pre-key.
                 *
                 * @return the raw bytes
                 */
                public byte[] signature() {
                    return signature;
                }

                /**
                 * Tries to parse a {@link SignedPreKey} from the given {@code <skey>} element.
                 *
                 * <p>Returns {@link Optional#empty()} for any sub-tree whose {@code <id>},
                 * {@code <value>}, or {@code <signature>} bytes are missing or the wrong length.
                 *
                 * @implNote
                 * This implementation enforces a 3-byte id, a 32-byte value, and a 64-byte signature
                 * per the WA Web fixture.
                 *
                 * @param skeyStanza the {@code <skey>} element
                 * @return an {@link Optional} carrying the parsed signed pre-key, or empty when
                 *         malformed
                 * @throws NullPointerException if {@code skeyStanza} is {@code null}
                 */
                public static Optional<SignedPreKey> of(Stanza skeyStanza) {
                    Objects.requireNonNull(skeyStanza, "skeyStanza cannot be null");
                    var idBytes = skeyStanza.getChild("id")
                            .flatMap(Stanza::toContentBytes)
                            .orElse(null);
                    if (idBytes == null || idBytes.length != 3) {
                        return Optional.empty();
                    }
                    var valueBytes = skeyStanza.getChild("value")
                            .flatMap(Stanza::toContentBytes)
                            .orElse(null);
                    if (valueBytes == null || valueBytes.length != 32) {
                        return Optional.empty();
                    }
                    var signatureBytes = skeyStanza.getChild("signature")
                            .flatMap(Stanza::toContentBytes)
                            .orElse(null);
                    if (signatureBytes == null || signatureBytes.length != 64) {
                        return Optional.empty();
                    }
                    return Optional.of(new SignedPreKey(idBytes, valueBytes, signatureBytes));
                }

                /**
                 * {@inheritDoc}
                 *
                 * @implNote
                 * This implementation compares all three byte arrays via
                 * {@link Arrays#equals(byte[], byte[])}.
                 */
                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    }
                    if (obj == null || obj.getClass() != this.getClass()) {
                        return false;
                    }
                    var that = (SignedPreKey) obj;
                    return Arrays.equals(this.keyId, that.keyId)
                            && Arrays.equals(this.keyValue, that.keyValue)
                            && Arrays.equals(this.signature, that.signature);
                }

                /**
                 * {@inheritDoc}
                 *
                 * @implNote
                 * This implementation combines {@link Arrays#hashCode(byte[])} for all three fields
                 * to stay consistent with {@link #equals(Object)}.
                 */
                @Override
                public int hashCode() {
                    var result = Arrays.hashCode(keyId);
                    result = 31 * result + Arrays.hashCode(keyValue);
                    result = 31 * result + Arrays.hashCode(signature);
                    return result;
                }

                /**
                 * {@inheritDoc}
                 *
                 * @implNote
                 * This implementation renders byte arrays as a length-only summary to avoid leaking
                 * key material into logs.
                 */
                @Override
                public String toString() {
                    return "SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.SignedPreKey[keyId="
                            + (keyId != null ? keyId.length + " bytes" : "null")
                            + ", keyValue=" + (keyValue != null ? keyValue.length + " bytes" : "null")
                            + ", signature=" + (signature != null ? signature.length + " bytes" : "null") + ']';
                }
            }
        }

        /**
         * Surfaces a relay-side rejection for a single addressee while the rest of the {@code <list>} may still carry successful bundles.
         *
         * <p>Mirrors WA Web's
         * {@code FetchKeyBundlesUserError}/{@code FetchKeyBundlesUserErrorFallback} disjunction.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchKeyBundlesUserErrorMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchKeyBundlesUserErrorFallbackMixin")
        public static final class UserError implements UserEntry {
            /**
             * Holds the per-user {@link Jid} echoed by the relay.
             */
            private final Jid userJid;

            /**
             * Holds the numeric error code in the {@code [500, 599]} range.
             */
            private final int errorCode;

            /**
             * Holds the human-readable error text.
             */
            private final String errorText;

            /**
             * Constructs a per-user error projection.
             *
             * <p>Built by {@link #of(Stanza)} after the {@code <error>} child passes its code-range
             * check.
             *
             * @param userJid   the per-user {@link Jid}
             * @param errorCode the numeric error code
             * @param errorText the human-readable text
             * @throws NullPointerException if {@code userJid} or {@code errorText} is {@code null}
             */
            public UserError(Jid userJid, int errorCode, String errorText) {
                this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
                this.errorCode = errorCode;
                this.errorText = Objects.requireNonNull(errorText, "errorText cannot be null");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Jid userJid() {
                return userJid;
            }

            /**
             * Returns the numeric error code.
             *
             * <p>Always in the {@code [500, 599]} range; embedders typically surface the pair
             * {@code (code, text)} verbatim to their error handler.
             *
             * @return the error code
             */
            public int errorCode() {
                return errorCode;
            }

            /**
             * Returns the human-readable error text.
             *
             * <p>Useful for logging and user-facing surfacing alongside the numeric code.
             *
             * @return the text
             */
            public String errorText() {
                return errorText;
            }

            /**
             * Tries to parse a {@link UserError} from the given {@code <user>} grandchild.
             *
             * <p>Returns {@link Optional#empty()} when the grandchild does not match the per-user
             * error shape; called as the fallback after {@link UserKeyBundle#of(Stanza)} declines.
             *
             * @implNote
             * This implementation accepts the literal {@code 500} and the {@code [501, 599]}
             * fallback range surfaced by {@code WASmaxInPreKeysFetchKeyBundlesUserErrorFallbackMixin},
             * collapsing the two WA Web disjunction branches into a single accept window.
             *
             * @param userStanza the {@code <user>} grandchild
             * @return an {@link Optional} carrying the parsed error, or empty on no-match
             * @throws NullPointerException if {@code userStanza} is {@code null}
             */
            @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchKeyBundlesUserErrorMixin",
                    exports = "parseFetchKeyBundlesUserErrorMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<UserError> of(Stanza userStanza) {
                Objects.requireNonNull(userStanza, "userStanza cannot be null");
                if (!userStanza.hasDescription("user")) {
                    return Optional.empty();
                }
                var jid = userStanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var errorNode = userStanza.getChild("error").orElse(null);
                if (errorNode == null) {
                    return Optional.empty();
                }
                var text = errorNode.getAttributeAsString("text").orElse(null);
                if (text == null) {
                    return Optional.empty();
                }
                var code = errorNode.getAttributeAsInt("code").orElse(-1);
                if (code < 500 || code > 599) {
                    return Optional.empty();
                }
                return Optional.of(new UserError(jid, code, text));
            }

            /**
             * {@inheritDoc}
             *
             * <p>Compares the JID, the code, and the text.
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (UserError) obj;
                return this.errorCode == that.errorCode
                        && Objects.equals(this.userJid, that.userJid)
                        && Objects.equals(this.errorText, that.errorText);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Hashes all three fields via {@link Objects#hash(Object...)}.
             */
            @Override
            public int hashCode() {
                return Objects.hash(userJid, errorCode, errorText);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "SmaxPreKeysFetchKeyBundlesResponse.Success.UserError[userJid=" + userJid
                        + ", errorCode=" + errorCode
                        + ", errorText=" + errorText + ']';
            }
        }
    }

    /**
     * Carries the {@code ClientError} reply variant, signalling that the outer request was rejected.
     *
     * <p>The relay rejected the outer request as malformed, unauthorised, or referencing no valid
     * JIDs; the entire RPC failed and no per-user data is available.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchKeyBundlesResponseRequestError")
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysRequestErrorsFetch")
    final class ClientError implements SmaxPreKeysFetchKeyBundlesResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply.
         *
         * <p>Built by {@link #of(Stanza, Stanza)} after the
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} envelope check succeeds.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional text
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * <p>Below {@code 500}; embedders typically map the code into a client-facing exception
         * type.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * <p>Useful for logging and as the message of any thrown exception.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not a well-formed client-error
         * envelope; the dispatcher then tries {@link ServerError#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation delegates the envelope and code-range checks to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchKeyBundlesResponseRequestError",
                exports = "parseFetchKeyBundlesResponseRequestError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
         *
         * <p>Compares both the code and the text.
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
         * {@inheritDoc}
         *
         * <p>Hashes both fields via {@link Objects#hash(Object...)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxPreKeysFetchKeyBundlesResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries the {@code ServerError} reply variant, signalling a transient relay-side failure.
     *
     * <p>The relay encountered a transient internal failure while processing the request; embedders
     * should typically retry with backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchKeyBundlesResponseServerError")
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysServerErrors")
    final class ServerError implements SmaxPreKeysFetchKeyBundlesResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply.
         *
         * <p>Built by {@link #of(Stanza, Stanza)} after the
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} envelope check succeeds.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional text
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * <p>In the {@code [500, ...]} range; embedders typically retry the request with backoff.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * <p>Useful for logging and as the message of any thrown exception.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not a well-formed server-error
         * envelope; this is the last branch the top-level dispatcher tries.
         *
         * @implNote
         * This implementation delegates the envelope and code-range checks to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchKeyBundlesResponseServerError",
                exports = "parseFetchKeyBundlesResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
         *
         * <p>Compares both the code and the text.
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
         * {@inheritDoc}
         *
         * <p>Hashes both fields via {@link Objects#hash(Object...)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxPreKeysFetchKeyBundlesResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
