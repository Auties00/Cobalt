package com.github.auties00.cobalt.wire.linked.signal;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * Carries a single per-user pre-key-bundle request, the input model
 * for the bulk Signal pre-key-bundle fetch operation.
 *
 * <p>Pairs the target user JID with the optional
 * {@code reason="identity"} hint that asks the relay to attach the
 * device-identity attestation to its reply. WhatsApp Web sets the
 * hint when the local cache holds no identity bytes for the addressee
 * yet; subsequent fetches generally omit it.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it travels
 * from caller code to the wire-encoder and is never serialized as
 * protobuf itself.
 */
public final class PreKeyBundleRequest {
    /**
     * The target user JID whose pre-key bundle is being requested.
     */
    private final Jid userJid;

    /**
     * Whether to include the {@code reason="identity"} hint asking the
     * relay to attach the device-identity attestation.
     */
    private final boolean includeIdentityAttestation;

    /**
     * Constructs a new request entry.
     *
     * @param userJid                    the target user JID; never
     *                                   {@code null}
     * @param includeIdentityAttestation whether to ask for the
     *                                   identity attestation
     * @throws NullPointerException if {@code userJid} is {@code null}
     */
    public PreKeyBundleRequest(Jid userJid, boolean includeIdentityAttestation) {
        this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
        this.includeIdentityAttestation = includeIdentityAttestation;
    }

    /**
     * Returns the target user JID.
     *
     * @return the user JID; never {@code null}
     */
    public Jid userJid() {
        return userJid;
    }

    /**
     * Returns whether the identity-attestation hint is set.
     *
     * @return {@code true} when the hint is set, {@code false}
     *         otherwise
     */
    public boolean includeIdentityAttestation() {
        return includeIdentityAttestation;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (PreKeyBundleRequest) obj;
        return this.includeIdentityAttestation == that.includeIdentityAttestation
                && Objects.equals(this.userJid, that.userJid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userJid, includeIdentityAttestation);
    }

    @Override
    public String toString() {
        return "PreKeyBundleRequest[userJid=" + userJid
                + ", includeIdentityAttestation=" + includeIdentityAttestation + ']';
    }
}
