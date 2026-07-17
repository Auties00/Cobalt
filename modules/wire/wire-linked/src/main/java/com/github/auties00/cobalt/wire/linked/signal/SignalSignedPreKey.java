package com.github.auties00.cobalt.wire.linked.signal;

import java.util.Arrays;
import java.util.Objects;

/**
 * Carries the signed pre-key — identifier, public key and detached
 * signature.
 *
 * <p>Unlike a one-time pre-key, the signed pre-key is rotated on a
 * schedule (typically days) and the relay keeps it around for the
 * whole rotation window. Remote devices that initiate a Signal session
 * with the local user mix this key into the shared-secret derivation;
 * the detached signature lets them verify the binding to the local
 * long-term identity key.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it travels
 * from caller code to the wire-encoder and is never serialized as
 * protobuf itself.
 */
public final class SignalSignedPreKey {
    /**
     * The signed-pre-key identifier — a small integer (the wire
     * encoding is a three-byte big-endian unsigned integer).
     */
    private final int id;

    /**
     * The signed-pre-key public key bytes (typically thirty-two
     * bytes).
     */
    private final byte[] publicKey;

    /**
     * The detached signature bytes (typically sixty-four bytes)
     * produced by signing {@code publicKey} with the local long-term
     * identity key.
     */
    private final byte[] signature;

    /**
     * Constructs a new signed pre-key.
     *
     * @param id        the signed-pre-key identifier
     * @param publicKey the public key bytes; never {@code null}
     * @param signature the detached signature bytes; never
     *                  {@code null}
     * @throws NullPointerException if any reference argument is
     *                              {@code null}
     */
    public SignalSignedPreKey(int id, byte[] publicKey, byte[] signature) {
        this.id = id;
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey cannot be null").clone();
        this.signature = Objects.requireNonNull(signature, "signature cannot be null").clone();
    }

    /**
     * Returns the signed-pre-key identifier.
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

    /**
     * Returns a defensive copy of the detached signature bytes.
     *
     * @return the signature bytes; never {@code null}
     */
    public byte[] signature() {
        return signature.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SignalSignedPreKey) obj;
        return this.id == that.id
                && Arrays.equals(this.publicKey, that.publicKey)
                && Arrays.equals(this.signature, that.signature);
    }

    @Override
    public int hashCode() {
        var result = Integer.hashCode(id);
        result = 31 * result + Arrays.hashCode(publicKey);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }

    @Override
    public String toString() {
        return "SignalSignedPreKey[id=" + id
                + ", publicKey=" + Arrays.toString(publicKey)
                + ", signature=" + Arrays.toString(signature) + ']';
    }
}
