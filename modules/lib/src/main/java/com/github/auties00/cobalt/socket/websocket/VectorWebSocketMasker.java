package com.github.auties00.cobalt.socket.websocket;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.nio.ByteOrder;

/**
 * Masks payload bytes with the JDK Vector API for bulk SIMD throughput.
 *
 * <p>This implementation references the {@code jdk.incubator.vector}
 * incubator module, so it can only link when that module is present in
 * the run-time image. {@link WebSocketMasker#lookup()} names it directly
 * but constructs it only after proving the module is present; merely
 * naming the class forces the verifier to load it, not to link the
 * incubator types in its descriptors, so an image without the module
 * never resolves them. The selector falls back to
 * {@link ScalarWebSocketMasker} when the module is absent or the Vector
 * API refuses to initialise. Once constructed this masker is
 * interchangeable with the scalar one and produces byte-identical output.
 *
 * <p>The class adds a SIMD bulk loop in the middle of the masking
 * sequence and delegates the unaligned head and the sub-vector tail to
 * the scalar primitives in {@link ScalarWebSocketMasker}, so the head,
 * the body and the tail cannot disagree on which key byte applies to each
 * payload index.
 *
 * @implNote This implementation masks with a three-tier strategy: the
 * shared scalar alignment lead-in, a
 * {@link ByteVector#lanewise(VectorOperators.Binary, byte)} bulk loop
 * over whole {@linkplain VectorSpecies#length() vector lengths}, and the
 * shared scalar {@code int}-and-byte tail. Below
 * {@link #VECTORIZE_THRESHOLD} the SIMD step is skipped so small frames
 * pay no vector-setup cost.
 */
final class VectorWebSocketMasker extends WebSocketMasker {
    /**
     * Holds the preferred hardware vector species used for SIMD bulk
     * masking.
     */
    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;

    /**
     * Holds the preferred hardware integer vector species used for
     * building the mask broadcast.
     */
    private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;

    /**
     * Holds the number of bytes processed per SIMD iteration.
     */
    private static final int VECTOR_LENGTH = BYTE_SPECIES.length();

    /**
     * Holds the minimum number of mask-aligned bytes required before the
     * SIMD path is entered.
     *
     * <p>Below this threshold the shared scalar tail handles the entire
     * body, avoiding vector-setup overhead on small frames.
     *
     * @implNote This implementation requires two full vectors' worth of
     * bytes so the per-call cost of building the broadcast mask is
     * amortised over at least two iterations.
     */
    private static final int VECTORIZE_THRESHOLD = VECTOR_LENGTH * 2;

    /**
     * Creates the vector masker.
     *
     * <p>The constructor is package-private and called directly from
     * {@link WebSocketMasker#lookup()}; the enclosing {@code static}
     * initialiser is what touches the Vector API, so a host without the
     * incubator module fails here and is caught by the selector.
     */
    VectorWebSocketMasker() {

    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation aligns the head with
     * {@link ScalarWebSocketMasker#maskAlignLeadIn(byte[], int, int, int, int)},
     * XORs whole vectors against the broadcast mask while at least
     * {@link #VECTORIZE_THRESHOLD} bytes remain, then finishes with
     * {@link ScalarWebSocketMasker#maskScalarTail(byte[], int, int, int, int, int)}.
     * The {@linkplain VectorSpecies#loopBound(int) vector loop bound} is a
     * multiple of {@link #VECTOR_LENGTH}, which is itself a multiple of
     * four, so the mask cycle stays aligned for the tail.
     */
    @Override
    public void applyMask(byte[] array, int offset, int length, int maskKey, int maskOffset) {
        var i = ScalarWebSocketMasker.maskAlignLeadIn(array, offset, length, maskKey, maskOffset);

        var remaining = length - i;
        if (remaining >= VECTORIZE_THRESHOLD) {
            var maskVec = buildAlignedMaskVector(maskKey);
            var vectorBound = i + BYTE_SPECIES.loopBound(remaining);
            for (; i < vectorBound; i += VECTOR_LENGTH) {
                var data = ByteVector.fromArray(BYTE_SPECIES, array, offset + i);
                data.lanewise(VectorOperators.XOR, maskVec)
                        .intoArray(array, offset + i);
            }
        }

        ScalarWebSocketMasker.maskScalarTail(array, offset, length, maskKey, maskOffset, i);
    }

    /**
     * Builds a SIMD vector containing the four-byte mask pattern repeated
     * to fill every lane.
     *
     * @implNote RFC 6455 masks use {@code maskKey[0]} as the
     * highest-order byte (big-endian memory layout).
     * {@link IntVector#reinterpretAsBytes()} always uses the platform's
     * native byte order, so on little-endian hosts this implementation
     * reverses the int before broadcasting so the reinterpreted bytes end
     * up in the RFC 6455 order.
     *
     * @param maskKey the four-byte masking key
     * @return a {@link ByteVector} filled with the repeating mask pattern
     */
    private static ByteVector buildAlignedMaskVector(int maskKey) {
        var nativeKey = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
                ? Integer.reverseBytes(maskKey)
                : maskKey;
        return IntVector.broadcast(INT_SPECIES, nativeKey)
                .reinterpretAsBytes();
    }
}
