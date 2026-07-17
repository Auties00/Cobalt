package com.github.auties00.cobalt.calls.media.audio.codec.mlow.tables;

/**
 * Builds the runtime cumulative mass functions (CMFs) and codebook arrays the MLow parameter decoders
 * consume, the port of the table-construction helpers in {@code smpl_helpers.c}.
 *
 * <p>MLow does not ship its entropy-coding tables in their decoder-ready form. The static tables baked
 * into the native sources are <em>delta cumulative mass functions</em> (DCMFs): one unsigned byte per
 * symbol that encodes a relative probability shape. At codec init each loader walks a DCMF through
 * {@link #dcmfToCmf(byte[])} to produce the monotonically increasing {@code uint16_t} CMF array the
 * range decoder reads, the same array shape that
 * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowEntropyWrapper#decodeUpdate}
 * scans. A CMF of {@code n} symbols has {@code n + 1} entries: a leading zero followed by the running
 * cumulative total, so {@code cmf[s + 1] - cmf[s]} is the frequency of symbol {@code s} and
 * {@code cmf[n]} is the grand total.
 *
 * <p>The DCMF-to-CMF transform is bit-exact-critical. The range decoder resolves a symbol by comparing
 * the decoded cumulative value against these exact integer boundaries, so a one-LSB error in any CMF
 * entry selects the wrong symbol and desynchronizes the entire bitstream from that point on. Every
 * operation in {@link #dcmfToCmf(byte[])} is therefore reproduced with the same 32-bit integer
 * arithmetic, the same clamp, and the same truncating (toward zero, here equivalently floor since all
 * operands are non-negative) integer division as the C reference.
 *
 * <p>This class also carries the two companion loaders that turn packed table bytes into runtime float
 * arrays ({@link #unpack8(byte[], float, float)} and {@link #unpack16(int[], float, float)}) and the
 * {@code cmf_to_bits} information-content helper ({@link #cmfToBits(int[])}). Those three operate in
 * floating point and are not bit-exact-critical, but they are part of the same leaf-helper surface the
 * table loaders call, so they live here to keep the loader call sites pointed at one utility.
 *
 * @implNote This implementation ports {@code smpl_dcmf_to_cmf}, {@code smpl_cmf_to_bits},
 * {@code smpl_unpack8}, and {@code smpl_unpack16} from {@code smpl_helpers.c}. The CMF entries are
 * {@code uint16_t} in C and never exceed {@code 32767 + n}, so they are carried in a Java {@code int}
 * array (unsigned-clean) rather than {@code short}, which both avoids sign extension at every read and
 * matches the {@code int[]} table contract of
 * {@link com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy.MlowEntropyWrapper}. The per-symbol
 * mass {@code (dcmf[n] + 1)^2} and the cumulative term
 * {@code cmf[n] * (32767 - dcmfLen) / sum + 1} both fit a signed 32-bit {@code int} for any byte input
 * (the largest product is {@code 65535 * 32767 = 2147385345}, below {@code Integer.MAX_VALUE}), so Java
 * {@code int} arithmetic reproduces the C {@code int} arithmetic exactly, including the truncating
 * division and the {@code 65535} mass clamp.
 */
public final class CmfBuilder {
    /**
     * Per-symbol mass clamp ceiling, the {@code 65535} guard in {@code smpl_dcmf_to_cmf}.
     *
     * <p>The squared mass {@code (dcmf[n] + 1)^2} reaches {@code 65536} when {@code dcmf[n]} is
     * {@code 255}; the reference clamps it to {@code 65535} so the value still fits a {@code uint16_t}.
     */
    private static final int MASS_CLAMP = 65535;

    /**
     * Numerator base of the cumulative scaling step, the {@code 32767} constant in
     * {@code smpl_dcmf_to_cmf}.
     *
     * <p>This is {@code 2^15 - 1}, the largest signed 15-bit value; the scaling step multiplies each
     * mass by {@code (32767 - dcmfLen)} so the cumulative total lands just under {@code 32767} with
     * headroom for the {@code +1} added per symbol.
     */
    private static final int SCALE_BASE = 32767;

    /**
     * Prevents instantiation of this stateless table-construction utility.
     */
    private CmfBuilder() {
        throw new AssertionError("no instances");
    }

    /**
     * Builds the runtime cumulative mass function from a packed delta cumulative mass function,
     * {@code smpl_dcmf_to_cmf}.
     *
     * <p>Given a DCMF of {@code n} unsigned bytes, the transform proceeds in two passes that mirror the
     * Julia comment in the reference (where {@code .^} is element-wise square and {@code .÷} is
     * truncating integer division):
     * <ul>
     * <li>First pass: each symbol mass is {@code (dcmf[i] + 1)^2}, clamped to {@code 65535}, and their
     * running sum is accumulated. The mass for symbol {@code i} is staged into output slot
     * {@code i + 1}.</li>
     * <li>Second pass: {@code cmf[0]} is set to zero and each later slot is replaced in place by
     * {@code cmf[i - 1] + cmf[i] * (32767 - n) / sum + 1}, reading the staged mass still sitting in
     * slot {@code i} before it is overwritten. The result is strictly increasing, starts at zero, and
     * ends just below {@code 32767 + n}.</li>
     * </ul>
     * The returned array has {@code n + 1} entries. All arithmetic is signed 32-bit integer with C
     * truncating division; no value is unsigned-sensitive because every CMF entry is non-negative and
     * below {@code 32768 + n}.
     *
     * @param dcmf the packed delta cumulative mass function, one unsigned byte per symbol; must be
     *             non-empty
     * @return a freshly allocated CMF array of length {@code dcmf.length + 1}, with a leading zero
     *         followed by the strictly increasing cumulative totals
     * @throws IllegalArgumentException if {@code dcmf} is empty
     */
    public static int[] dcmfToCmf(byte[] dcmf) {
        var dcmfLen = dcmf.length;
        if (dcmfLen == 0) {
            throw new IllegalArgumentException("dcmf must be non-empty");
        }
        var cmf = new int[dcmfLen + 1];
        var sum = 0;
        for (var n = 0; n < dcmfLen; n++) {
            var tmp = dcmf[n] & 0xFF;
            tmp += 1;
            tmp *= tmp;
            if (tmp > MASS_CLAMP) {
                tmp = MASS_CLAMP;
            }
            cmf[n + 1] = tmp;
            sum += tmp;
        }
        cmf[0] = 0;
        for (var n = 1; n < dcmfLen + 1; n++) {
            cmf[n] = cmf[n - 1] + cmf[n] * (SCALE_BASE - dcmfLen) / sum + 1;
        }
        return cmf;
    }

    /**
     * Builds a CMF from one row of a flattened DCMF table, {@code smpl_dcmf_to_cmf} over a sub-range.
     *
     * <p>Several MLow loaders store many DCMFs back to back in one flat byte array and call the
     * transform on consecutive {@code rowLen}-byte windows (for example the conditional acb-gain tables,
     * which hold {@code n + 1} rows of {@code n} bytes). This overload runs the same transform as
     * {@link #dcmfToCmf(byte[])} on the window {@code [offset, offset + rowLen)} without forcing the
     * caller to copy the row out first.
     *
     * @param dcmf   the flat table holding one or more DCMF rows
     * @param offset the index of the first byte of the target row
     * @param rowLen the number of symbols in the row
     * @return a freshly allocated CMF array of length {@code rowLen + 1}
     * @throws IllegalArgumentException  if {@code rowLen} is not positive
     * @throws IndexOutOfBoundsException if {@code [offset, offset + rowLen)} is not fully within
     *                                   {@code dcmf}
     */
    public static int[] dcmfToCmf(byte[] dcmf, int offset, int rowLen) {
        if (rowLen <= 0) {
            throw new IllegalArgumentException("rowLen must be positive");
        }
        if (offset < 0 || offset + rowLen > dcmf.length) {
            throw new IndexOutOfBoundsException("row [" + offset + ", " + (offset + rowLen) + ") out of bounds for length " + dcmf.length);
        }
        var cmf = new int[rowLen + 1];
        var sum = 0;
        for (var n = 0; n < rowLen; n++) {
            var tmp = dcmf[offset + n] & 0xFF;
            tmp += 1;
            tmp *= tmp;
            if (tmp > MASS_CLAMP) {
                tmp = MASS_CLAMP;
            }
            cmf[n + 1] = tmp;
            sum += tmp;
        }
        cmf[0] = 0;
        for (var n = 1; n < rowLen + 1; n++) {
            cmf[n] = cmf[n - 1] + cmf[n] * (SCALE_BASE - rowLen) / sum + 1;
        }
        return cmf;
    }

    /**
     * Computes the per-symbol information content of a CMF in bits, {@code smpl_cmf_to_bits}.
     *
     * <p>For each symbol {@code i} the helper returns {@code -log2((cmf[i + 1] - cmf[i]) / cmf[len - 1])},
     * the self-information of the symbol under the probability model the CMF encodes (frequency over
     * grand total). The result has one fewer entry than the CMF: {@code cmf.length - 1} values, one per
     * symbol. The computation is performed in single precision with {@code log2} to match the reference
     * {@code log2f}, so it is not bit-exact across platforms and is only used for cost estimation, never
     * for symbol resolution.
     *
     * @param cmf the cumulative mass function, at least two entries, strictly increasing
     * @return a freshly allocated array of {@code cmf.length - 1} bit-cost values
     * @throws IllegalArgumentException if {@code cmf} has fewer than two entries
     */
    public static float[] cmfToBits(int[] cmf) {
        var cmfLen = cmf.length;
        if (cmfLen < 2) {
            throw new IllegalArgumentException("cmf must have at least two entries");
        }
        float total = cmf[cmfLen - 1];
        var bits = new float[cmfLen - 1];
        for (var i = 0; i < cmfLen - 1; i++) {
            var p = (cmf[i + 1] - cmf[i]) / total;
            bits[i] = (float) (-(Math.log(p) / Math.log(2.0)));
        }
        return bits;
    }

    /**
     * Expands a packed unsigned-byte table into affine-scaled floats, {@code smpl_unpack8}.
     *
     * <p>Each output is {@code min + in[i] * scale} where {@code in[i]} is read as an unsigned byte in
     * {@code [0, 255]}. The loaders use this to reconstruct quantized codebook values that were stored
     * as a byte index times a fixed step from a fixed origin.
     *
     * @param in    the packed values, read as unsigned bytes
     * @param scale the per-step multiplier
     * @param min   the origin added to every scaled value
     * @return a freshly allocated array of {@code in.length} unpacked floats
     */
    public static float[] unpack8(byte[] in, float scale, float min) {
        var out = new float[in.length];
        for (var i = 0; i < in.length; i++) {
            out[i] = min + (in[i] & 0xFF) * scale;
        }
        return out;
    }

    /**
     * Expands a packed unsigned 16-bit table into affine-scaled floats, {@code smpl_unpack16}.
     *
     * <p>Each output is {@code min + in[i] * scale} where {@code in[i]} is read as an unsigned 16-bit
     * value in {@code [0, 65535]}; the input is carried in an {@code int} array masked to 16 bits so the
     * caller never has to worry about {@code short} sign extension. This is the wider-precision sibling
     * of {@link #unpack8(byte[], float, float)} for codebooks quantized to 16-bit indices.
     *
     * @param in    the packed values, read as unsigned 16-bit quantities (low 16 bits of each
     *              {@code int})
     * @param scale the per-step multiplier
     * @param min   the origin added to every scaled value
     * @return a freshly allocated array of {@code in.length} unpacked floats
     */
    public static float[] unpack16(int[] in, float scale, float min) {
        var out = new float[in.length];
        for (var i = 0; i < in.length; i++) {
            out[i] = min + (in[i] & 0xFFFF) * scale;
        }
        return out;
    }
}
