package com.github.auties00.cobalt.wire.linked.signal;

import java.util.Arrays;
import java.util.Objects;

/**
 * Carries one entry of a Signal one-time pre-key — the identifier and
 * the corresponding public key bytes.
 *
 * <p>Each upload bundle carries dozens of these in a list; the relay
 * stores them and hands one out per remote device that wants to start
 * a Signal session with the local user. Once handed out, a one-time
 * pre-key is discarded server-side and the client must replenish the
 * pool before it runs dry.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it travels
 * from caller code to the wire-encoder and is never serialized as
 * protobuf itself.
 */
public final class SignalPreKey {
    /**
     * The pre-key identifier — a small integer (the wire encoding is a
     * three-byte big-endian unsigned integer).
     */
    private final int id;

    /**
     * The pre-key public key bytes (typically thirty-two bytes).
     */
    private final byte[] publicKey;

    /**
     * Constructs a new pre-key entry.
     *
     * @param id        the pre-key identifier
     * @param publicKey the public key bytes; never {@code null}
     * @throws NullPointerException if {@code publicKey} is
     *                              {@code null}
     */
    public SignalPreKey(int id, byte[] publicKey) {
        this.id = id;
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey cannot be null").clone();
    }

    /**
     * Returns the pre-key identifier.
     *
     * @return the identifier
     */
    public int id() {
        return id;
    }

    /**
     * Returns a defensive copy of the public key bytes.
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
        var that = (SignalPreKey) obj;
        return this.id == that.id
                && Arrays.equals(this.publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        return 31 * Integer.hashCode(id) + Arrays.hashCode(publicKey);
    }

    @Override
    public String toString() {
        return "SignalPreKey[id=" + id
                + ", publicKey=" + Arrays.toString(publicKey) + ']';
    }
}
