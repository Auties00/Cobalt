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
 * Enumerates the inbound reply variants produced by the relay in response to a {@link SmaxPreKeysFetchMissingPreKeysRequest}.
 *
 * <p>The relay answers with one of three documented shapes: {@link Success} (per-device bundles
 * attached), {@link ClientError} (the outer request was malformed), and {@link ServerError}
 * (transient server-side failure). The {@link Success} branch may still carry
 * {@link Success.UserError} entries for individual addressees the relay could not serve.
 */
public sealed interface SmaxPreKeysFetchMissingPreKeysResponse extends SmaxStanza.Response
        permits SmaxPreKeysFetchMissingPreKeysResponse.Success, SmaxPreKeysFetchMissingPreKeysResponse.ClientError, SmaxPreKeysFetchMissingPreKeysResponse.ServerError {

    /**
     * Tries each {@link SmaxPreKeysFetchMissingPreKeysResponse} variant in priority order and returns the first that parses cleanly.
     *
     * <p>The absence of any match indicates a protocol violation, surfaced as
     * {@link Optional#empty()}.
     *
     * @implNote
     * This implementation tries {@link Success}, then {@link ClientError}, then {@link ServerError}.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} on
     *         no-match
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxPreKeysFetchMissingPreKeysRPC",
            exports = "sendFetchMissingPreKeysRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxPreKeysFetchMissingPreKeysResponse> of(Stanza stanza, Stanza request) {
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
     * <p>Each {@link UserEntry} is either a {@link UserDeviceBundle} carrying device-level pre-key
     * projections or a {@link UserError} when the relay rejected the per-user fetch.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchMissingPreKeysResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysIQResultResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysResponsePaddingMixin")
    final class Success implements SmaxPreKeysFetchMissingPreKeysResponse {
        /**
         * Holds the per-user entries in the order the relay produced them.
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
         * <p>Each element is either a {@link UserDeviceBundle} or a {@link UserError}; switch on the
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
         * <p>Returns {@link Optional#empty()} when the envelope fails validation, when no
         * {@code <list>} child is present, or when any {@code <user>} grandchild fails per-user
         * parsing.
         *
         * @implNote
         * This implementation walks the {@code <list>} child once, collecting per-user entries via
         * {@link UserEntry#of(Stanza)}; any failed entry aborts the whole parse so the dispatcher
         * falls through to the error branches.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchMissingPreKeysResponseSuccess",
                exports = "parseFetchMissingPreKeysResponseSuccess",
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
            return "SmaxPreKeysFetchMissingPreKeysResponse.Success[users=" + users + ']';
        }

        /**
         * Models the per-user reply as a disjunction of the two possible shapes.
         *
         * <p>An entry is either a {@link UserDeviceBundle} carrying per-device pre-key projections,
         * or a {@link UserError} when the relay rejected the per-user fetch.
         */
        public sealed interface UserEntry permits UserDeviceBundle, UserError {

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
             * <p>Returns {@link Optional#empty()} when the stanza matches neither the success nor the
             * error shape.
             *
             * @implSpec
             * Implementations of the permitted variants must accept exactly one wire shape each so
             * that this factory can disambiguate them by trying each in turn.
             *
             * @implNote
             * This implementation tries {@link UserDeviceBundle} first then {@link UserError},
             * matching the WA Web parser's priority.
             *
             * @param userStanza the {@code <user>} grandchild
             * @return an {@link Optional} carrying the parsed entry
             * @throws NullPointerException if {@code userStanza} is {@code null}
             */
            static Optional<UserEntry> of(Stanza userStanza) {
                Objects.requireNonNull(userStanza, "userStanza cannot be null");
                var success = UserDeviceBundle.of(userStanza).orElse(null);
                if (success != null) {
                    return Optional.of(success);
                }
                return UserError.of(userStanza).map(error -> error);
            }
        }

        /**
         * Carries one {@link DeviceKeyBundle} per device the relay was able to resolve for a single user.
         *
         * <p>Mirrors WA Web's {@code FetchMissingPreKeysUserSuccess} branch.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchMissingPreKeysUserSuccessMixin")
        public static final class UserDeviceBundle implements UserEntry {
            /**
             * Holds the per-user {@link Jid} echoed by the relay.
             */
            private final Jid userJid;

            /**
             * Holds the per-device bundle projections (one to one hundred per the WA Web {@code REPEATED_CHILD(1, 100)} schema).
             */
            private final List<DeviceKeyBundle> devices;

            /**
             * Constructs a per-user projection.
             *
             * <p>The list is defensively copied so the constructed value is immutable.
             *
             * @param userJid the user {@link Jid}
             * @param devices the per-device bundles; defaults to an empty list when {@code null}
             * @throws NullPointerException if {@code userJid} is {@code null}
             */
            public UserDeviceBundle(Jid userJid, List<DeviceKeyBundle> devices) {
                this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
                this.devices = List.copyOf(Objects.requireNonNullElse(devices, List.of()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Jid userJid() {
                return userJid;
            }

            /**
             * Returns the per-device bundle projections.
             *
             * <p>Each entry feeds Signal session re-establishment for one device of the addressee
             * user.
             *
             * @return an unmodifiable {@link List} of {@link DeviceKeyBundle}
             */
            public List<DeviceKeyBundle> devices() {
                return devices;
            }

            /**
             * Tries to parse a {@link UserDeviceBundle} from the given {@code <user>} grandchild.
             *
             * <p>Returns {@link Optional#empty()} when any device grandchild fails parsing or when
             * the user stanza contains no devices, which violates the {@code REPEATED_CHILD(1, 100)}
             * minimum and lets the consumer fall through to the {@link UserError} branch.
             *
             * @implNote
             * This implementation walks every {@code <device>} grandchild via
             * {@link DeviceKeyBundle#of(Stanza)}; an empty collected list is rejected so the
             * dispatcher can try the {@link UserError} shape instead.
             *
             * @param userStanza the {@code <user>} grandchild
             * @return an {@link Optional} carrying the projection, or empty on no-match
             * @throws NullPointerException if {@code userStanza} is {@code null}
             */
            @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchMissingPreKeysUserSuccessMixin",
                    exports = "parseFetchMissingPreKeysUserSuccessMixin",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<UserDeviceBundle> of(Stanza userStanza) {
                Objects.requireNonNull(userStanza, "userStanza cannot be null");
                if (!userStanza.hasDescription("user")) {
                    return Optional.empty();
                }
                var jid = userStanza.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var devicesList = new ArrayList<DeviceKeyBundle>();
                for (var deviceNode : userStanza.getChildren("device")) {
                    var device = DeviceKeyBundle.of(deviceNode).orElse(null);
                    if (device == null) {
                        return Optional.empty();
                    }
                    devicesList.add(device);
                }
                if (devicesList.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new UserDeviceBundle(jid, devicesList));
            }

            /**
             * {@inheritDoc}
             *
             * <p>Compares the JID and the devices list.
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (UserDeviceBundle) obj;
                return Objects.equals(this.userJid, that.userJid)
                        && Objects.equals(this.devices, that.devices);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Hashes both fields via {@link Objects#hash(Object...)}.
             */
            @Override
            public int hashCode() {
                return Objects.hash(userJid, devices);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "SmaxPreKeysFetchMissingPreKeysResponse.Success.UserDeviceBundle[userJid=" + userJid
                        + ", devices=" + devices + ']';
            }
        }

        /**
         * Carries every piece of cryptographic material needed to seed a Signal session for one device of a target user.
         *
         * <p>Mirrors the {@link SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle} shape but
         * is keyed on device id rather than on the user JID.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchMissingPreKeysUserSuccessMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysRegistrationIDMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysKeyTypeMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysIdentityKeyMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysPreKeyMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysSignedPreKeyMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysDeviceIdentityMixin")
        public static final class DeviceKeyBundle {
            /**
             * Holds the numeric device id in the {@code [0, 99]} range.
             */
            private final int deviceId;

            /**
             * Holds the optional relay-side timestamp ({@code t} attribute, in seconds since the UNIX epoch).
             */
            private final Long timestamp;

            /**
             * Records whether the {@code is_cloud_api="true"} marker was present.
             */
            private final boolean cloudApi;

            /**
             * Holds the 4-byte registration id (raw bytes, big-endian).
             */
            private final byte[] registrationId;

            /**
             * Holds the optional 1-byte key-type marker (literal {@code [5]} for Curve25519).
             */
            private final byte[] keyType;

            /**
             * Holds the 32-byte Signal identity public key.
             */
            private final byte[] identityKey;

            /**
             * Holds the optional unsigned pre-key projection.
             */
            private final SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.PreKey preKey;

            /**
             * Holds the signed pre-key projection (always present in a successful bundle).
             */
            private final SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.SignedPreKey signedPreKey;

            /**
             * Holds the optional device-identity attestation bytes.
             */
            private final byte[] deviceIdentity;

            /**
             * Constructs a device-key-bundle projection.
             *
             * <p>Built by {@link #of(Stanza)} after the {@code <device>} grandchild passes every
             * per-field check.
             *
             * @param deviceId       the device id
             * @param timestamp      the optional relay timestamp
             * @param cloudApi       whether the cloud-api marker was set
             * @param registrationId the 4-byte registration id
             * @param keyType        the optional 1-byte key-type marker
             * @param identityKey    the 32-byte identity key
             * @param preKey         the optional pre-key
             * @param signedPreKey   the signed pre-key
             * @param deviceIdentity the optional device-identity bytes
             * @throws NullPointerException if any non-optional argument is {@code null}
             */
            public DeviceKeyBundle(int deviceId,
                                   Long timestamp,
                                   boolean cloudApi,
                                   byte[] registrationId,
                                   byte[] keyType,
                                   byte[] identityKey,
                                   SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.PreKey preKey,
                                   SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.SignedPreKey signedPreKey,
                                   byte[] deviceIdentity) {
                this.deviceId = deviceId;
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
             * Returns the numeric device id.
             *
             * <p>Identifies the device-list slot in the addressee account.
             *
             * @return the device id
             */
            public int deviceId() {
                return deviceId;
            }

            /**
             * Returns the optional relay timestamp.
             *
             * <p>Surfaced for audit and replay-window enforcement.
             *
             * @return an {@link Optional} carrying the timestamp
             */
            public Optional<Long> timestamp() {
                return Optional.ofNullable(timestamp);
            }

            /**
             * Returns whether the cloud-api marker was set.
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
             * <p>Decoded as an unsigned big-endian integer by the Signal layer.
             *
             * @return the raw bytes
             */
            public byte[] registrationId() {
                return registrationId;
            }

            /**
             * Returns the optional 1-byte key-type marker.
             *
             * <p>Always {@code [5]} (Curve25519) when present; absent on legacy bundles.
             *
             * @return an {@link Optional} carrying the marker
             */
            public Optional<byte[]> keyType() {
                return Optional.ofNullable(keyType);
            }

            /**
             * Returns the 32-byte identity key.
             *
             * <p>Feeds the Signal session as the addressee device's long-term public identity.
             *
             * @return the raw bytes
             */
            public byte[] identityKey() {
                return identityKey;
            }

            /**
             * Returns the optional pre-key.
             *
             * <p>The Signal layer falls back to the signed pre-key when this is empty.
             *
             * @return an {@link Optional} carrying the pre-key
             */
            public Optional<SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.PreKey> preKey() {
                return Optional.ofNullable(preKey);
            }

            /**
             * Returns the signed pre-key.
             *
             * <p>Always present in a successful bundle; callers can dereference without an
             * {@link Optional}.
             *
             * @return the signed pre-key
             */
            public SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.SignedPreKey signedPreKey() {
                return signedPreKey;
            }

            /**
             * Returns the optional device-identity attestation bytes.
             *
             * <p>Present only when the original request set the identity-reason hint.
             *
             * @return an {@link Optional} carrying the bytes
             */
            public Optional<byte[]> deviceIdentity() {
                return Optional.ofNullable(deviceIdentity);
            }

            /**
             * Tries to parse a {@link DeviceKeyBundle} from the given {@code <device>} element.
             *
             * <p>Returns {@link Optional#empty()} for any malformed sub-tree (wrong length, missing
             * required child, out-of-range id, and so on).
             *
             * @implNote
             * This implementation enforces the WA Web length and range contracts on each field:
             * 4 bytes for registration id, 1 byte for key type, 32 bytes for identity key, device id
             * within {@code [0, 99]}.
             *
             * @param deviceStanza the {@code <device>} element
             * @return an {@link Optional} carrying the parsed bundle, or empty on no-match
             * @throws NullPointerException if {@code deviceStanza} is {@code null}
             */
            @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchMissingPreKeysUserSuccessMixin",
                    exports = "parseFetchMissingPreKeysUserSuccessDevice",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<DeviceKeyBundle> of(Stanza deviceStanza) {
                Objects.requireNonNull(deviceStanza, "deviceStanza cannot be null");
                if (!deviceStanza.hasDescription("device")) {
                    return Optional.empty();
                }
                var idAttribute = deviceStanza.getAttributeAsInt("id");
                if (idAttribute.isEmpty()) {
                    return Optional.empty();
                }
                var id = idAttribute.getAsInt();
                if (id < 0 || id > 99) {
                    return Optional.empty();
                }
                Long timestamp = null;
                if (deviceStanza.hasAttribute("t")) {
                    var t = deviceStanza.getAttributeAsLong("t");
                    if (t.isEmpty()) {
                        return Optional.empty();
                    }
                    timestamp = t.getAsLong();
                }
                var cloudApi = deviceStanza.hasAttribute("is_cloud_api", "true");
                var registrationBytes = deviceStanza.getChild("registration")
                        .flatMap(Stanza::toContentBytes)
                        .orElse(null);
                if (registrationBytes == null || registrationBytes.length != 4) {
                    return Optional.empty();
                }
                byte[] keyTypeBytes = null;
                var typeNode = deviceStanza.getChild("type").orElse(null);
                if (typeNode != null) {
                    var typeContent = typeNode.toContentBytes().orElse(null);
                    if (typeContent != null && Arrays.equals(typeContent, new byte[]{5})) {
                        keyTypeBytes = typeContent;
                    }
                }
                var identityBytes = deviceStanza.getChild("identity")
                        .flatMap(Stanza::toContentBytes)
                        .orElse(null);
                if (identityBytes == null || identityBytes.length != 32) {
                    return Optional.empty();
                }
                SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.PreKey preKeyValue = null;
                var preKeyNode = deviceStanza.getChild("key").orElse(null);
                if (preKeyNode != null) {
                    preKeyValue = SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.PreKey.of(preKeyNode).orElse(null);
                    if (preKeyValue == null) {
                        return Optional.empty();
                    }
                }
                var signedPreKeyNode = deviceStanza.getChild("skey").orElse(null);
                if (signedPreKeyNode == null) {
                    return Optional.empty();
                }
                var signedPreKeyValue = SmaxPreKeysFetchKeyBundlesResponse.Success.UserKeyBundle.SignedPreKey.of(signedPreKeyNode).orElse(null);
                if (signedPreKeyValue == null) {
                    return Optional.empty();
                }
                var deviceIdentityBytes = deviceStanza.getChild("device-identity")
                        .flatMap(Stanza::toContentBytes)
                        .orElse(null);
                return Optional.of(new DeviceKeyBundle(
                        id,
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
                var that = (DeviceKeyBundle) obj;
                return this.deviceId == that.deviceId
                        && this.cloudApi == that.cloudApi
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
             * and combines them with {@link Arrays#hashCode(byte[])} for the byte-array fields.
             */
            @Override
            public int hashCode() {
                var result = Objects.hash(deviceId, timestamp, cloudApi, preKey, signedPreKey);
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
             * material into logs.
             */
            @Override
            public String toString() {
                return "SmaxPreKeysFetchMissingPreKeysResponse.Success.DeviceKeyBundle[deviceId=" + deviceId
                        + ", timestamp=" + timestamp
                        + ", cloudApi=" + cloudApi
                        + ", registrationId=" + (registrationId != null ? registrationId.length + " bytes" : "null")
                        + ", keyType=" + (keyType != null ? keyType.length + " bytes" : "null")
                        + ", identityKey=" + (identityKey != null ? identityKey.length + " bytes" : "null")
                        + ", preKey=" + preKey
                        + ", signedPreKey=" + signedPreKey
                        + ", deviceIdentity=" + (deviceIdentity != null ? deviceIdentity.length + " bytes" : "null") + ']';
            }
        }

        /**
         * Surfaces a relay-side rejection for a single addressee.
         *
         * <p>Mirrors WA Web's
         * {@code FetchMissingPreKeysUserError}/{@code FetchMissingPreKeysUserErrorFallback}
         * disjunction.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchMissingPreKeysUserErrorMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchMissingPreKeysUserErrorFallbackMixin")
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
             * <p>Always in the {@code [500, 599]} range.
             *
             * @return the error code
             */
            public int errorCode() {
                return errorCode;
            }

            /**
             * Returns the human-readable error text.
             *
             * <p>Useful for logging alongside the numeric code.
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
             * error shape; called as the fallback after {@link UserDeviceBundle#of(Stanza)} declines.
             *
             * @implNote
             * This implementation accepts the literal {@code 500} and the {@code [501, 599]}
             * fallback range surfaced by
             * {@code WASmaxInPreKeysFetchMissingPreKeysUserErrorFallbackMixin}, collapsing the two
             * WA Web disjunction branches.
             *
             * @param userStanza the {@code <user>} grandchild
             * @return an {@link Optional} carrying the parsed error
             * @throws NullPointerException if {@code userStanza} is {@code null}
             */
            @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchMissingPreKeysUserErrorMixin",
                    exports = "parseFetchMissingPreKeysUserErrorMixin",
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
                return "SmaxPreKeysFetchMissingPreKeysResponse.Success.UserError[userJid=" + userJid
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
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchMissingPreKeysResponseRequestError")
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysRequestErrorsFetch")
    final class ClientError implements SmaxPreKeysFetchMissingPreKeysResponse {
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
         * <p>Below {@code 500}; embedders typically map the code into a client-facing exception.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * <p>Useful for logging and as the exception message.
         *
         * @return an {@link Optional} carrying the text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not a well-formed client-error
         * envelope.
         *
         * @implNote
         * This implementation delegates the envelope and code-range checks to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchMissingPreKeysResponseRequestError",
                exports = "parseFetchMissingPreKeysResponseRequestError",
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
            return "SmaxPreKeysFetchMissingPreKeysResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries the {@code ServerError} reply variant, signalling a transient relay-side failure.
     *
     * <p>The relay encountered a transient internal failure; embedders should typically retry with
     * backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysFetchMissingPreKeysResponseServerError")
    @WhatsAppWebModule(moduleName = "WASmaxInPreKeysServerErrors")
    final class ServerError implements SmaxPreKeysFetchMissingPreKeysResponse {
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
         * <p>Useful for logging and as the exception message.
         *
         * @return an {@link Optional} carrying the text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not a well-formed server-error
         * envelope.
         *
         * @implNote
         * This implementation delegates the envelope and code-range checks to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPreKeysFetchMissingPreKeysResponseServerError",
                exports = "parseFetchMissingPreKeysResponseServerError",
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
            return "SmaxPreKeysFetchMissingPreKeysResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
