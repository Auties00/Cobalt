package com.github.auties00.cobalt.wire.linked.signal;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries one per-user entry of a successfully-resolved Signal pre-key
 * bundle returned by the relay's bulk fetch operation.
 *
 * <p>Surfaces the full Signal cryptographic payload — the registration
 * id, the long-term identity public key, the optional one-time pre-key,
 * the signed pre-key, and the optional device-identity attestation —
 * together with the relay's per-entry timestamp and Cloud-API marker.
 *
 * <p>The bulk fetch can also surface per-user errors (a single
 * addressee may fail while the rest of the batch resolves). Those are
 * modelled separately as {@link PreKeyBundleEntryError}; this carrier
 * holds only the success projection.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class PreKeyBundleEntry {
    /**
     * The per-user JID echoed by the relay.
     */
    private final Jid userJid;

    /**
     * The optional relay-side timestamp ({@code t} attribute, in
     * seconds since the UNIX epoch).
     */
    private final Long timestampSeconds;

    /**
     * Whether the {@code is_cloud_api="true"} marker was set on the
     * entry. Indicates a Cloud-API hosted account.
     */
    private final boolean cloudApi;

    /**
     * The 4-byte registration id (raw bytes, big-endian).
     */
    private final byte[] registrationId;

    /**
     * The optional 1-byte key-type marker (literal {@code [5]} for
     * Curve25519). Absent on legacy bundles.
     */
    private final byte[] keyType;

    /**
     * The 32-byte Signal identity public key.
     */
    private final byte[] identityKey;

    /**
     * The optional unsigned one-time pre-key. {@code null} when the
     * relay had no fresh pre-keys to surface.
     */
    private final SignalPreKey preKey;

    /**
     * The signed pre-key (always present in a successful bundle).
     */
    private final SignalSignedPreKey signedPreKey;

    /**
     * The optional device-identity attestation bytes. Present only
     * when the request asked for identity attestation.
     */
    private final byte[] deviceIdentity;

    /**
     * Constructs a new bundle entry.
     *
     * @param userJid          the per-user JID; never {@code null}
     * @param timestampSeconds the optional relay timestamp; may be
     *                         {@code null}
     * @param cloudApi         whether the Cloud-API marker was set
     * @param registrationId   the 4-byte registration id; never
     *                         {@code null}
     * @param keyType          the optional key-type marker; may be
     *                         {@code null}
     * @param identityKey      the 32-byte identity key; never
     *                         {@code null}
     * @param preKey           the optional one-time pre-key; may be
     *                         {@code null}
     * @param signedPreKey     the signed pre-key; never {@code null}
     * @param deviceIdentity   the optional device-identity bytes; may
     *                         be {@code null}
     * @throws NullPointerException if {@code userJid},
     *                              {@code registrationId},
     *                              {@code identityKey}, or
     *                              {@code signedPreKey} are
     *                              {@code null}
     */
    public PreKeyBundleEntry(Jid userJid, Long timestampSeconds, boolean cloudApi,
                             byte[] registrationId, byte[] keyType, byte[] identityKey,
                             SignalPreKey preKey, SignalSignedPreKey signedPreKey,
                             byte[] deviceIdentity) {
        this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
        this.timestampSeconds = timestampSeconds;
        this.cloudApi = cloudApi;
        this.registrationId = Objects.requireNonNull(registrationId, "registrationId cannot be null").clone();
        this.keyType = keyType == null ? null : keyType.clone();
        this.identityKey = Objects.requireNonNull(identityKey, "identityKey cannot be null").clone();
        this.preKey = preKey;
        this.signedPreKey = Objects.requireNonNull(signedPreKey, "signedPreKey cannot be null");
        this.deviceIdentity = deviceIdentity == null ? null : deviceIdentity.clone();
    }

    /**
     * Returns the per-user JID echoed by the relay.
     *
     * @return the JID; never {@code null}
     */
    public Jid userJid() {
        return userJid;
    }

    /**
     * Returns the optional relay timestamp in seconds.
     *
     * @return an {@link Optional} carrying the timestamp, or empty
     */
    public Optional<Long> timestampSeconds() {
        return Optional.ofNullable(timestampSeconds);
    }

    /**
     * Returns whether the Cloud-API marker was set on the entry.
     *
     * @return {@code true} when set
     */
    public boolean cloudApi() {
        return cloudApi;
    }

    /**
     * Returns a defensive copy of the 4-byte registration id.
     *
     * @return the registration bytes; never {@code null}
     */
    public byte[] registrationId() {
        return registrationId.clone();
    }

    /**
     * Returns the optional 1-byte key-type marker as a defensive copy.
     *
     * @return an {@link Optional} carrying the bytes, or empty
     */
    public Optional<byte[]> keyType() {
        return Optional.ofNullable(keyType).map(byte[]::clone);
    }

    /**
     * Returns a defensive copy of the 32-byte Signal identity public
     * key.
     *
     * @return the identity key bytes; never {@code null}
     */
    public byte[] identityKey() {
        return identityKey.clone();
    }

    /**
     * Returns the optional one-time pre-key.
     *
     * @return an {@link Optional} carrying the pre-key, or empty
     */
    public Optional<SignalPreKey> preKey() {
        return Optional.ofNullable(preKey);
    }

    /**
     * Returns the signed pre-key.
     *
     * @return the signed pre-key; never {@code null}
     */
    public SignalSignedPreKey signedPreKey() {
        return signedPreKey;
    }

    /**
     * Returns the optional device-identity attestation bytes as a
     * defensive copy.
     *
     * @return an {@link Optional} carrying the bytes, or empty
     */
    public Optional<byte[]> deviceIdentity() {
        return Optional.ofNullable(deviceIdentity).map(byte[]::clone);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (PreKeyBundleEntry) obj;
        return this.cloudApi == that.cloudApi
                && Objects.equals(this.userJid, that.userJid)
                && Objects.equals(this.timestampSeconds, that.timestampSeconds)
                && Arrays.equals(this.registrationId, that.registrationId)
                && Arrays.equals(this.keyType, that.keyType)
                && Arrays.equals(this.identityKey, that.identityKey)
                && Objects.equals(this.preKey, that.preKey)
                && Objects.equals(this.signedPreKey, that.signedPreKey)
                && Arrays.equals(this.deviceIdentity, that.deviceIdentity);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(userJid, timestampSeconds, cloudApi, preKey, signedPreKey);
        result = 31 * result + Arrays.hashCode(registrationId);
        result = 31 * result + Arrays.hashCode(keyType);
        result = 31 * result + Arrays.hashCode(identityKey);
        result = 31 * result + Arrays.hashCode(deviceIdentity);
        return result;
    }

    @Override
    public String toString() {
        return "PreKeyBundleEntry[userJid=" + userJid
                + ", timestampSeconds=" + timestampSeconds
                + ", cloudApi=" + cloudApi
                + ", registrationId=" + (registrationId.length + " bytes")
                + ", keyType=" + (keyType != null ? keyType.length + " bytes" : "null")
                + ", identityKey=" + (identityKey.length + " bytes")
                + ", preKey=" + preKey
                + ", signedPreKey=" + signedPreKey
                + ", deviceIdentity=" + (deviceIdentity != null ? deviceIdentity.length + " bytes" : "null") + ']';
    }
}
