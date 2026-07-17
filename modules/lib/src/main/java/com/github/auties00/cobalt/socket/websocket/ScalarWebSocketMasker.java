package com.github.auties00.cobalt.socket.websocket;

import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.nio.ByteOrder;

import static com.github.auties00.cobalt.socket.websocket.WebSocketFrameConstants.maskByte;

/**
 * Masks payload bytes using only scalar instructions, with no reference
 * to the {@code jdk.incubator.vector} incubator module.
 *
 * <p>This is the always-available implementation: it is selected for
 * {@link WebSocketMasker#INSTANCE} whenever the Vector API cannot be used
 * (the incubator module was not added to the run-time image, or the
 * process is a GraalVM native image, where the Vector API runs as a
 * per-lane scalar emulation and the SIMD path would be pure overhead).
 * Because it names no incubator type, this class always loads and links
 * regardless of the module graph.
 *
 * <p>The class also owns the scalar masking primitives shared with
 * {@link VectorWebSocketMasker}: the four-byte alignment lead-in and the
 * {@code int}-wise plus byte-wise tail. The SIMD implementation reuses
 * them around its bulk loop so the two variants cannot drift apart on
 * the unaligned head and the sub-vector tail.
 *
 * @implNote This implementation masks four bytes at a time via
 * {@link DataUtils#getInt(byte[], int, ByteOrder)} and
 * {@link DataUtils#putInt(byte[], int, int, ByteOrder)} in
 * {@link ByteOrder#BIG_ENDIAN} order (one XOR against the whole
 * {@code maskKey}), which the lead-in keeps aligned to the four-byte mask
 * cycle, and finishes any sub-{@code int} remainder byte by byte.
 */
final class ScalarWebSocketMasker extends WebSocketMasker {
    /**
     * Creates the scalar masker.
     *
     * <p>The constructor is package-private and invoked directly by
     * {@link WebSocketMasker#lookup()} on every fallback branch, so it
     * carries no state.
     */
    ScalarWebSocketMasker() {

    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation runs the shared alignment lead-in and
     * then the shared {@code int}-and-byte tail, with no SIMD step in
     * between.
     */
    @Override
    public void applyMask(byte[] array, int offset, int length, int maskKey, int maskOffset) {
        var i = maskAlignLeadIn(array, offset, length, maskKey, maskOffset);
        maskScalarTail(array, offset, length, maskKey, maskOffset, i);
    }

    /**
     * Masks the leading bytes that precede the first four-byte mask-cycle
     * boundary and returns the index at which the aligned body begins.
     *
     * <p>When {@code maskOffset} already sits on a four-byte boundary no
     * byte is touched and {@code 0} is returned. Otherwise up to three
     * bytes (fewer when {@code length} is shorter) are masked so that the
     * returned index lands on a boundary, which lets the caller's
     * {@code int}-wise loop XOR the whole {@code maskKey} at once.
     *
     * @param array      the byte array to mask in place
     * @param offset     the index of the first byte to mask
     * @param length     the number of bytes to mask
     * @param maskKey    the four-byte masking key
     * @param maskOffset the starting position in the four-byte mask cycle
     * @return the number of leading bytes masked, which is the index into
     *         the region at which the aligned body begins
     */
    static int maskAlignLeadIn(byte[] array, int offset, int length, int maskKey, int maskOffset) {
        var i = 0;
        var align = maskOffset & 3;
        if (align != 0) {
            var leading = Math.min(4 - align, length);
            for (; i < leading; i++) {
                array[offset + i] ^= maskByte(maskKey, maskOffset + i);
            }
        }
        return i;
    }

    /**
     * Masks the region from index {@code i} to {@code length} with a
     * four-byte {@code int}-wise loop followed by a byte-wise remainder.
     *
     * <p>The caller must pass an {@code i} for which {@code maskOffset + i}
     * sits on a four-byte boundary (the value returned by
     * {@link #maskAlignLeadIn(byte[], int, int, int, int)}, optionally
     * advanced by a whole number of mask cycles), so that the
     * {@code int}-wise XOR against the unrotated {@code maskKey} is
     * correct.
     *
     * @param array      the byte array to mask in place
     * @param offset     the index of the first byte of the region
     * @param length     the number of bytes in the region
     * @param maskKey    the four-byte masking key
     * @param maskOffset the starting position in the four-byte mask cycle
     * @param i          the boundary-aligned index at which to resume
     *                   masking
     */
    static void maskScalarTail(byte[] array, int offset, int length, int maskKey, int maskOffset, int i) {
        if (length - i >= 4) {
            for (; i + 3 < length; i += 4) {
                var idx = offset + i;
                var val = DataUtils.getInt(array, idx, ByteOrder.BIG_ENDIAN);
                DataUtils.putInt(array, idx, val ^ maskKey, ByteOrder.BIG_ENDIAN);
            }
        }

        for (; i < length; i++) {
            array[offset + i] ^= maskByte(maskKey, maskOffset + i);
        }
    }
}
