package com.github.auties00.cobalt.wire.stanza.smax.prekeys;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Builds the outbound {@code <iq xmlns="encrypt" type="get">} stanza that asks the relay to re-issue stale per-device pre-key bundles.
 *
 * <p>The caller submits the device-level registration ids it currently has and the relay surfaces
 * fresh bundles for any device whose recorded registration id changed (for example, after a device
 * re-pair); this lets the Signal recovery path re-establish sessions after a "missing prekey"
 * decryption failure.
 *
 * @implNote
 * This implementation collapses the {@code WASmaxOutPreKeysClientRequestMixin} envelope shaping and
 * the per-user/per-device payload construction into a single {@link #toStanza()} pass; the JS layer
 * routes the same data through multiple mixin functions.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPreKeysFetchMissingPreKeysRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutPreKeysClientRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutPreKeysRegistrationIDMixin")
public final class SmaxPreKeysFetchMissingPreKeysRequest implements SmaxStanza.Request {
    /**
     * Holds the per-user fetch entries that appear as {@code <user>} children of the request.
     */
    private final List<UserKeyFetchRequest> users;

    /**
     * Constructs a request for the given list of users.
     *
     * <p>The relay rejects empty requests, so an empty list is refused early. The list is
     * defensively copied so the constructed value is immutable.
     *
     * @param users the per-user fetch entries
     * @throws NullPointerException     if {@code users} is {@code null}
     * @throws IllegalArgumentException if {@code users} is empty
     */
    public SmaxPreKeysFetchMissingPreKeysRequest(List<UserKeyFetchRequest> users) {
        Objects.requireNonNull(users, "users cannot be null");
        if (users.isEmpty()) {
            throw new IllegalArgumentException("users cannot be empty");
        }
        this.users = List.copyOf(users);
    }

    /**
     * Returns the list of users carried by this request.
     *
     * <p>The returned list is unmodifiable so callers cannot mutate it after construction.
     *
     * @return an unmodifiable {@link List} of {@link UserKeyFetchRequest}
     */
    public List<UserKeyFetchRequest> users() {
        return users;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation hard-codes {@code xmlns="encrypt"}, {@code type="get"}, and
     * {@code to=s.whatsapp.net} per the
     * {@code WASmaxOutPreKeysFetchMissingPreKeysRequest.makeFetchMissingPreKeysRequest} fixture,
     * then nests one {@code <user>} per entry under a single {@code <key_fetch>} child, each
     * carrying one {@code <device id><registration/></device>} grandchild per device.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPreKeysFetchMissingPreKeysRequest",
            exports = "makeFetchMissingPreKeysRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var userNodes = new ArrayList<Stanza>(users.size());
        for (var user : users) {
            var deviceNodes = new ArrayList<Stanza>(user.devices().size());
            for (var device : user.devices()) {
                var registrationNode = new StanzaBuilder()
                        .description("registration")
                        .content(device.registrationId())
                        .build();
                var deviceNode = new StanzaBuilder()
                        .description("device")
                        .attribute("id", device.deviceId())
                        .content(registrationNode)
                        .build();
                deviceNodes.add(deviceNode);
            }
            var userBuilder = new StanzaBuilder()
                    .description("user")
                    .attribute("jid", user.userJid());
            if (user.hasUserReasonIdentity()) {
                userBuilder.attribute("reason", "identity");
            }
            userBuilder.content(deviceNodes);
            userNodes.add(userBuilder.build());
        }
        var keyFetchNode = new StanzaBuilder()
                .description("key_fetch")
                .content(userNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "encrypt")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(keyFetchNode);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Two requests are equal when their {@link #users} lists are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxPreKeysFetchMissingPreKeysRequest) obj;
        return Objects.equals(this.users, that.users);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Hashes the carried {@link #users} list to stay consistent with {@link #equals(Object)}.
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
        return "SmaxPreKeysFetchMissingPreKeysRequest[users=" + users + ']';
    }

    /**
     * Models a per-user entry in the outbound {@code <key_fetch>} payload.
     *
     * <p>Pairs a target user {@link Jid} with a list of per-device fetch entries plus the optional
     * {@code reason="identity"} hint.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPreKeysFetchMissingPreKeysRequest")
    public static final class UserKeyFetchRequest {
        /**
         * Holds the target user {@link Jid} whose stale pre-keys are being re-fetched.
         */
        private final Jid userJid;

        /**
         * Records whether {@code reason="identity"} is set on the {@code <user>} child.
         */
        private final boolean hasUserReasonIdentity;

        /**
         * Holds the per-device fetch entries (zero to one hundred per the {@code REPEATED_CHILD(0, 100)} schema).
         */
        private final List<DeviceKeyFetchRequest> devices;

        /**
         * Constructs a per-user request entry.
         *
         * <p>Set {@code hasUserReasonIdentity} to {@code true} when the relay must attach the
         * device-identity attestation for any re-fetched device. The {@code devices} list may be
         * empty when the caller only needs to refresh the user-level attestation. The list is
         * defensively copied so the constructed value is immutable.
         *
         * @param userJid               the target user {@link Jid}
         * @param hasUserReasonIdentity whether to set the identity-reason hint
         * @param devices               the per-device entries; defaults to an empty list when
         *                              {@code null}
         * @throws NullPointerException if {@code userJid} is {@code null}
         */
        public UserKeyFetchRequest(Jid userJid, boolean hasUserReasonIdentity, List<DeviceKeyFetchRequest> devices) {
            this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
            this.hasUserReasonIdentity = hasUserReasonIdentity;
            this.devices = List.copyOf(Objects.requireNonNullElse(devices, List.of()));
        }

        /**
         * Returns the target user {@link Jid}.
         *
         * <p>Populates the {@code jid} attribute of the corresponding {@code <user>} child in
         * {@link SmaxPreKeysFetchMissingPreKeysRequest#toStanza()}.
         *
         * @return the user {@link Jid}
         */
        public Jid userJid() {
            return userJid;
        }

        /**
         * Returns whether the identity-reason hint is set.
         *
         * <p>Decides whether {@link SmaxPreKeysFetchMissingPreKeysRequest#toStanza()} emits the
         * {@code reason="identity"} attribute on the {@code <user>} child.
         *
         * @return {@code true} when the hint is set
         */
        public boolean hasUserReasonIdentity() {
            return hasUserReasonIdentity;
        }

        /**
         * Returns the per-device fetch entries.
         *
         * <p>Each entry feeds one {@code <device id><registration/></device>} grandchild of the
         * outbound stanza.
         *
         * @return an unmodifiable {@link List} of {@link DeviceKeyFetchRequest}
         */
        public List<DeviceKeyFetchRequest> devices() {
            return devices;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Compares the JID, the identity-reason flag, and the devices list.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (UserKeyFetchRequest) obj;
            return this.hasUserReasonIdentity == that.hasUserReasonIdentity
                    && Objects.equals(this.userJid, that.userJid)
                    && Objects.equals(this.devices, that.devices);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Hashes all three fields via {@link Objects#hash(Object...)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(userJid, hasUserReasonIdentity, devices);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxPreKeysFetchMissingPreKeysRequest.UserKeyFetchRequest[userJid=" + userJid
                    + ", hasUserReasonIdentity=" + hasUserReasonIdentity
                    + ", devices=" + devices + ']';
        }
    }

    /**
     * Models a per-device entry inside a {@link UserKeyFetchRequest}.
     *
     * <p>Pairs a numeric device id with the 4-byte registration id currently cached for that
     * device; the relay compares the supplied id with its own record and only surfaces a fresh
     * bundle when the two disagree.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPreKeysFetchMissingPreKeysRequest")
    @WhatsAppWebModule(moduleName = "WASmaxOutPreKeysRegistrationIDMixin")
    public static final class DeviceKeyFetchRequest {
        /**
         * Holds the numeric device id in the {@code [0, 99]} range.
         */
        private final int deviceId;

        /**
         * Holds the 4-byte registration id (raw bytes, big-endian) whose freshness needs validating.
         */
        private final byte[] registrationId;

        /**
         * Constructs a per-device fetch entry.
         *
         * <p>The {@code deviceId} is the device-list slot of the addressee account.
         *
         * @param deviceId       the device id
         * @param registrationId the 4-byte registration id
         * @throws NullPointerException if {@code registrationId} is {@code null}
         */
        public DeviceKeyFetchRequest(int deviceId, byte[] registrationId) {
            this.deviceId = deviceId;
            this.registrationId = Objects.requireNonNull(registrationId, "registrationId cannot be null");
        }

        /**
         * Returns the numeric device id.
         *
         * <p>Populates the {@code id} attribute of the corresponding {@code <device>} grandchild in
         * {@link SmaxPreKeysFetchMissingPreKeysRequest#toStanza()}.
         *
         * @return the device id
         */
        public int deviceId() {
            return deviceId;
        }

        /**
         * Returns the 4-byte registration id.
         *
         * <p>Becomes the content of the {@code <registration>} grandchild in
         * {@link SmaxPreKeysFetchMissingPreKeysRequest#toStanza()}.
         *
         * @return the raw bytes
         */
        public byte[] registrationId() {
            return registrationId;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation compares the device id and the registration id via
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
            var that = (DeviceKeyFetchRequest) obj;
            return this.deviceId == that.deviceId
                    && Arrays.equals(this.registrationId, that.registrationId);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation combines {@link Integer#hashCode(int)} and
         * {@link Arrays#hashCode(byte[])} to stay consistent with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            var result = Integer.hashCode(deviceId);
            result = 31 * result + Arrays.hashCode(registrationId);
            return result;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation renders the registration id as a length-only summary to avoid leaking
         * key material into logs.
         */
        @Override
        public String toString() {
            return "SmaxPreKeysFetchMissingPreKeysRequest.DeviceKeyFetchRequest[deviceId=" + deviceId
                    + ", registrationId=" + (registrationId != null ? registrationId.length + " bytes" : "null") + ']';
        }
    }
}
