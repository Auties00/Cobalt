package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;

/**
 * One key entry inside an end-to-end rekey payload.
 *
 * <p>Each entry binds a freshly-derived key value to one of the three call
 * encryption domains via {@link RekeyKeyType}. The {@linkplain #key() key}
 * bytes are the raw SRTP master key for that domain; the receiver feeds
 * them into the matching context's key-derivation function.
 *
 * <p>Both fields are {@code required} on the wire; a deserialized entry
 * with either field absent is malformed.
 */
@ProtobufMessage(name = "RekeyKeyEntry")
public final class RekeyKeyEntry {
    /**
     * The encryption-domain discriminator for this key.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final RekeyKeyType type;

    /**
     * The raw key bytes for the targeted domain.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final byte[] key;

    /**
     * Constructs a new {@code RekeyKeyEntry}.
     *
     * @param type the encryption-domain discriminator; never {@code null}
     * @param key  the raw key bytes; never {@code null}
     * @throws NullPointerException when either argument is {@code null}
     */
    RekeyKeyEntry(RekeyKeyType type, byte[] key) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.key = Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Returns the encryption-domain discriminator.
     *
     * @return the type; never {@code null}
     */
    public RekeyKeyType type() {
        return type;
    }

    /**
     * Returns the raw key bytes.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] key() {
        return key;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RekeyKeyEntry that
                && this.type == that.type
                && Arrays.equals(this.key, that.key));
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, Arrays.hashCode(key));
    }

    @Override
    public String toString() {
        return "RekeyKeyEntry[type=" + type + ", keyLen=" + key.length + ']';
    }
}
