package com.github.auties00.cobalt.calls.transport.warp;

import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.nio.ByteOrder;

/**
 * Provides the shared big endian byte buffer primitives the WARP and STUN codecs in this package use
 * to read and write multibyte integer fields.
 *
 * <p>WhatsApp's call control protocols (WARP media control, STUN binding) place their multibyte
 * integer fields in network byte order. This holder centralizes the unsigned {@code u16} and
 * {@code u32} read and write helpers so each codec does not repeat the shift arithmetic. Every method
 * is stateless and operates directly on a caller supplied array at a caller supplied offset.
 *
 * @implNote This implementation delegates every field to the allocation free {@link DataUtils}
 *           accessors with {@link ByteOrder#BIG_ENDIAN}, the network order used for every length and
 *           integer attribute in the WARP and STUN wire formats.
 */
public final class WarpCodecSupport {
    /**
     * Prevents instantiation of this stateless helper holder.
     *
     * <p>Every member is {@code static}, so no instance is ever needed; the constructor throws to
     * enforce that.
     */
    private WarpCodecSupport() {
        throw new AssertionError("WarpCodecSupport is not instantiable");
    }

    /**
     * Writes an unsigned sixteen bit value into a buffer in big endian order.
     *
     * <p>The low sixteen bits of {@code value} are stored across two bytes starting at {@code offset},
     * high byte first; any higher bits are masked away.
     *
     * @param out    the destination buffer
     * @param offset the index of the high byte
     * @param value  the value to write, masked to sixteen bits
     * @return the index immediately after the two written bytes
     */
    public static int putU16(byte[] out, int offset, int value) {
        DataUtils.putShort(out, offset, (short) value, ByteOrder.BIG_ENDIAN);
        return offset + 2;
    }

    /**
     * Writes an unsigned thirty two bit value into a buffer in big endian order.
     *
     * <p>The four bytes of {@code value} are stored starting at {@code offset}, most significant byte
     * first.
     *
     * @param out    the destination buffer
     * @param offset the index of the most significant byte
     * @param value  the value to write
     * @return the index immediately after the four written bytes
     */
    public static int putU32(byte[] out, int offset, int value) {
        DataUtils.putInt(out, offset, value, ByteOrder.BIG_ENDIAN);
        return offset + 4;
    }

    /**
     * Reads an unsigned sixteen bit big endian value from a buffer.
     *
     * <p>The two bytes starting at {@code offset} are combined high byte first and returned as a
     * non negative {@code int}.
     *
     * @param in     the source buffer
     * @param offset the index of the high byte
     * @return the unsigned value in {@code 0..65535}
     */
    public static int readU16(byte[] in, int offset) {
        return DataUtils.getShort(in, offset, ByteOrder.BIG_ENDIAN) & 0xffff;
    }

    /**
     * Reads an unsigned thirty two bit big endian value from a buffer into a {@code long}.
     *
     * <p>The four bytes starting at {@code offset} are combined most significant byte first and
     * widened into a non negative {@code long}, since the value can exceed the range of a signed
     * {@code int}.
     *
     * @param in     the source buffer
     * @param offset the index of the most significant byte
     * @return the unsigned value in {@code 0..4294967295}
     */
    public static long readU32(byte[] in, int offset) {
        return DataUtils.getInt(in, offset, ByteOrder.BIG_ENDIAN) & 0xffffffffL;
    }
}
