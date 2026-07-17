package com.github.auties00.cobalt.wire.linked.signal;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Objects;

/**
 * Carries a single per-user entry of a Signal missing-pre-key fetch
 * request, the input model for the bulk
 * "fetch missing pre-keys" operation.
 *
 * <p>Wraps the target user JID, the optional identity-attestation
 * hint, and a list of per-device {@link MissingPreKeyDeviceRequest}
 * entries describing the devices whose pre-key cache needs
 * refreshing.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it travels
 * from caller code to the wire-encoder and is never serialized as
 * protobuf itself.
 */
public final class MissingPreKeyUserRequest {
    /**
     * The target user JID whose missing pre-keys are being re-fetched.
     */
    private final Jid userJid;

    /**
     * Whether to include the identity-attestation hint asking the
     * relay to attach the device-identity attestation.
     */
    private final boolean includeIdentityAttestation;

    /**
     * The per-device fetch entries (0..100 per WhatsApp Web).
     */
    private final List<MissingPreKeyDeviceRequest> devices;

    /**
     * Constructs a new per-user request.
     *
     * @param userJid                    the target user JID; never
     *                                   {@code null}
     * @param includeIdentityAttestation whether to ask for identity
     *                                   attestation
     * @param devices                    the per-device entries; never
     *                                   {@code null}
     * @throws NullPointerException if {@code userJid} or
     *                              {@code devices} are {@code null}
     */
    public MissingPreKeyUserRequest(Jid userJid, boolean includeIdentityAttestation,
                                    List<MissingPreKeyDeviceRequest> devices) {
        this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
        this.includeIdentityAttestation = includeIdentityAttestation;
        Objects.requireNonNull(devices, "devices cannot be null");
        this.devices = List.copyOf(devices);
    }

    /**
     * Returns the target user JID.
     *
     * @return the JID; never {@code null}
     */
    public Jid userJid() {
        return userJid;
    }

    /**
     * Returns whether the identity-attestation hint is set.
     *
     * @return {@code true} when the hint is set
     */
    public boolean includeIdentityAttestation() {
        return includeIdentityAttestation;
    }

    /**
     * Returns the per-device fetch entries.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<MissingPreKeyDeviceRequest> devices() {
        return devices;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (MissingPreKeyUserRequest) obj;
        return this.includeIdentityAttestation == that.includeIdentityAttestation
                && Objects.equals(this.userJid, that.userJid)
                && Objects.equals(this.devices, that.devices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userJid, includeIdentityAttestation, devices);
    }

    @Override
    public String toString() {
        return "MissingPreKeyUserRequest[userJid=" + userJid
                + ", includeIdentityAttestation=" + includeIdentityAttestation
                + ", devices=" + devices + ']';
    }
}
