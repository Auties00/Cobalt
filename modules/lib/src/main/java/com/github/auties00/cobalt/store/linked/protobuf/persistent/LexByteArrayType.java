package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.h2.mvstore.type.ByteArrayDataType;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * The MVStore key {@link org.h2.mvstore.type.DataType} for the composite {@code byte[]} keys of the
 * three message databases, ordering them by unsigned lexicographic (memcmp) comparison.
 *
 * <p>The chat and newsletter key layouts ({@code jid + 0x00 + suffix}) and the big-endian newsletter
 * {@code serverId} encoding sort correctly only under unsigned byte-by-byte comparison: that is what
 * keeps a per-chat prefix range contiguous and a newsletter's {@code serverId} order numeric. H2's own
 * {@link ByteArrayDataType} serialises {@code byte[]} compactly but inherits the {@link BasicDataType}
 * comparator, which throws, so it cannot serve as a key type. This type borrows the wire format from
 * {@link ByteArrayDataType} and supplies the missing ordering.
 *
 * @implNote
 * This implementation reuses {@link ByteArrayDataType#INSTANCE} for {@link #getMemory(byte[])},
 * {@link #write(WriteBuffer, byte[])} and {@link #read(ByteBuffer)} so the on-disk encoding stays
 * identical to H2's and version-independent, and overrides only {@link #compare(byte[], byte[])} with
 * {@link Arrays#compareUnsigned(byte[], byte[])}, the intrinsified equivalent of the {@code memcmp}
 * order libmdbx applied to the same keys.
 */
final class LexByteArrayType extends BasicDataType<byte[]> {
    /**
     * The shared stateless singleton used as the key type of every message database.
     */
    static final LexByteArrayType INSTANCE = new LexByteArrayType();

    /**
     * The H2 {@code byte[]} data type the wire-format operations delegate to.
     */
    private static final ByteArrayDataType DELEGATE = ByteArrayDataType.INSTANCE;

    /**
     * Constructs the singleton.
     *
     * @implNote This implementation is private; {@link #INSTANCE} is the only instance.
     */
    private LexByteArrayType() {
    }

    /**
     * Returns the estimated in-memory footprint of {@code obj}.
     *
     * @param obj the key bytes
     * @return the footprint in bytes, as reported by {@link ByteArrayDataType}
     */
    @Override
    public int getMemory(byte[] obj) {
        return DELEGATE.getMemory(obj);
    }

    /**
     * Writes {@code obj} to {@code buff} in H2's length-prefixed {@code byte[]} format.
     *
     * @param buff the destination buffer
     * @param obj  the key bytes
     */
    @Override
    public void write(WriteBuffer buff, byte[] obj) {
        DELEGATE.write(buff, obj);
    }

    /**
     * Reads one length-prefixed {@code byte[]} from {@code buff}.
     *
     * @param buff the source buffer
     * @return the decoded key bytes
     */
    @Override
    public byte[] read(ByteBuffer buff) {
        return DELEGATE.read(buff);
    }

    /**
     * Allocates a {@code byte[][]} backing array for {@code size} keys.
     *
     * @param size the number of slots
     * @return a fresh array of the given length
     */
    @Override
    public byte[][] createStorage(int size) {
        return new byte[size][];
    }

    /**
     * Compares two keys by unsigned lexicographic (memcmp) order.
     *
     * @param a the first key
     * @param b the second key
     * @return a negative, zero or positive value as {@code a} sorts before, equal to, or after
     *         {@code b}
     */
    @Override
    public int compare(byte[] a, byte[] b) {
        return Arrays.compareUnsigned(a, b);
    }
}
