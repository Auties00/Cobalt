package com.github.auties00.cobalt.wire.linked.signal;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Arrays;
import java.util.Objects;

/**
 * Carries the long-term identity public key for a single device.
 *
 * <p>Returned by the bulk identity-key fetch operation as the projected
 * value for each successfully-resolved device JID. The key-bundle type
 * marker is the single byte the relay attaches to the {@code <type/>}
 * grandchild and identifies the Signal key family the public key
 * belongs to.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class IdentityKey {
    /**
     * The device JID this key belongs to.
     */
    private final Jid deviceJid;

    /**
     * The single-byte Signal key-bundle type marker.
     */
    private final byte keyBundleType;

    /**
     * The long-term identity public key bytes (typically thirty-two
     * bytes).
     */
    private final byte[] publicKey;

    /**
     * Constructs a new identity key entry.
     *
     * @param deviceJid     the device JID; never {@code null}
     * @param keyBundleType the key-bundle type marker
     * @param publicKey     the identity public key bytes; never
     *                      {@code null}
     * @throws NullPointerException if any reference argument is
     *                              {@code null}
     */
    public IdentityKey(Jid deviceJid, byte keyBundleType, byte[] publicKey) {
        this.deviceJid = Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        this.keyBundleType = keyBundleType;
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey cannot be null").clone();
    }

    /**
     * Returns the device JID this key belongs to.
     *
     * @return the device JID; never {@code null}
     */
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
     * Returns a defensive copy of the identity public key bytes.
     *
     * @return the public key bytes; never {@code null}
     */
    public byte[] publicKey() {
        return publicKey.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IdentityKey) obj;
        return this.keyBundleType == that.keyBundleType
                && Objects.equals(this.deviceJid, that.deviceJid)
                && Arrays.equals(this.publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(deviceJid, keyBundleType);
        result = 31 * result + Arrays.hashCode(publicKey);
        return result;
    }

    @Override
    public String toString() {
        return "IdentityKey[deviceJid=" + deviceJid
                + ", keyBundleType=" + keyBundleType
                + ", publicKey=" + Arrays.toString(publicKey) + ']';
    }
}
