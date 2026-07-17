package com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy;

/**
 * Symbol level entropy helpers layered over {@link MlowRangeDecoder} and {@link MlowRangeEncoder}.
 *
 * <p>The MLow parameter codecs never call the raw range coder primitives directly; they go through
 * thin helpers that pair a {@code decode} with the matching {@code update} (or, on the encode side, a
 * single {@code encode}). {@link #decodeUpdate(MlowRangeDecoder, int[])} resolves a symbol against a
 * cumulative frequency table; {@link #decodeUniform(MlowRangeDecoder, int)} draws one value from a flat
 * distribution; {@link #encodeUpdate(MlowRangeEncoder, int[], int)} and
 * {@link #encodeUniform(MlowRangeEncoder, int, int)} are their encode side mirrors. Every helper is a
 * pure function of the coder state plus the supplied table, so this class is a stateless utility with a
 * private constructor.
 *
 * <p>These helpers, together with {@link MlowRangeDecoder#decodeBits(int)} for raw bits, are the
 * complete entropy surface the rest of the MLow codec pipeline builds on.
 *
 * @implNote The cumulative tables MLow passes are not based at zero: every span is taken relative to
 * {@code cmf[0]}, so each helper subtracts {@code cmf[0]} from each frequency and passes the adjusted
 * total {@code cmf[len - 1] - cmf[0]} to the range coder. The symbol search is a linear scan, sized for
 * the MLow tables which each hold a handful of entries.
 */
public final class MlowEntropyWrapper {
    /**
     * Prevents instantiation of this stateless helper.
     */
    private MlowEntropyWrapper() {
        throw new AssertionError("no instances");
    }

    /**
     * Decodes one symbol against a cumulative frequency table.
     *
     * <p>The table {@code cmf} is a monotonically nondecreasing cumulative frequency array that may be
     * biased by a nonzero base; all spans are measured relative to {@code cmf[0]}. The helper decodes a
     * cumulative value over the adjusted total {@code cmf[len - 1] - cmf[0]}, re adds {@code cmf[0]} to
     * place it on the table's own scale, linearly scans for the symbol whose half open span
     * {@code [cmf[s], cmf[s + 1])} contains it, then advances the decoder past that span. The relative
     * bounds {@code cmf[s] - cmf[0]} and {@code cmf[s + 1] - cmf[0]} and the adjusted total are what reach
     * {@link MlowRangeDecoder#update(long, long, long)}.
     *
     * @param decoder the range decoder to advance
     * @param cmf     the cumulative frequency table; at least two entries, nondecreasing
     * @return the decoded symbol index in {@code [0, cmf.length - 1)}
     */
    public static int decodeUpdate(MlowRangeDecoder decoder, int[] cmf) {
        var last = cmf.length - 1;
        var base = cmf[0] & 0xFFFFFFFFL;
        var total = (cmf[last] & 0xFFFFFFFFL) - base;
        var cmfLow = decoder.decode(total) + base;
        var s = 0;
        for (; s < last; s++) {
            if (Long.compareUnsigned(cmfLow, cmf[s + 1] & 0xFFFFFFFFL) < 0) {
                break;
            }
        }
        decoder.update((cmf[s] & 0xFFFFFFFFL) - base, (cmf[s + 1] & 0xFFFFFFFFL) - base, total);
        return s;
    }

    /**
     * Decodes one symbol against a window of a cumulative frequency table, the offset aware form of
     * {@link #decodeUpdate(MlowRangeDecoder, int[])} used where an interior table pointer
     * ({@code cmf + offset}) is passed.
     *
     * <p>The window is the half open range {@code [offset, offset + len)} of {@code cmf}. Every span is
     * measured relative to the first windowed entry {@code cmf[offset]} and the adjusted total is
     * {@code cmf[offset + len - 1] - cmf[offset]}, exactly as {@link #decodeUpdate(MlowRangeDecoder, int[])}
     * would treat a copied subarray whose element zero is {@code cmf[offset]}. The returned index is
     * relative to the window start, so this is observationally identical to copying
     * {@code [offset, offset + len)} out and calling {@link #decodeUpdate(MlowRangeDecoder, int[])} on it,
     * without the allocation on each call.
     *
     * @param decoder the range decoder to advance
     * @param cmf     the full cumulative frequency table; nondecreasing over the window
     * @param offset  the index of the first windowed entry
     * @param len     the number of windowed entries; at least two
     * @return the decoded symbol index in {@code [0, len - 1)}, relative to the window start
     */
    public static int decodeUpdate(MlowRangeDecoder decoder, int[] cmf, int offset, int len) {
        var last = offset + len - 1;
        var base = cmf[offset] & 0xFFFFFFFFL;
        var total = (cmf[last] & 0xFFFFFFFFL) - base;
        var cmfLow = decoder.decode(total) + base;
        var s = offset;
        for (; s < last; s++) {
            if (Long.compareUnsigned(cmfLow, cmf[s + 1] & 0xFFFFFFFFL) < 0) {
                break;
            }
        }
        decoder.update((cmf[s] & 0xFFFFFFFFL) - base, (cmf[s + 1] & 0xFFFFFFFFL) - base, total);
        return s - offset;
    }

    /**
     * Decodes one value from a uniform distribution over {@code [0, n)}.
     *
     * <p>Every value in {@code [0, n)} has equal probability, so the cumulative value returned by
     * {@link MlowRangeDecoder#decode(long)} is itself the decoded symbol; the helper advances the decoder
     * past the unit width span {@code [cmfLow, cmfLow + 1)} out of total {@code n}.
     *
     * @param decoder the range decoder to advance
     * @param n       the number of equiprobable values; must be at least 1
     * @return the decoded value in {@code [0, n)}
     */
    public static int decodeUniform(MlowRangeDecoder decoder, int n) {
        var cmfLow = decoder.decode(n);
        decoder.update(cmfLow, cmfLow + 1, n);
        return (int) cmfLow;
    }

    /**
     * Encodes one symbol against a cumulative frequency table, the encode side mirror of
     * {@link #decodeUpdate(MlowRangeDecoder, int[])}.
     *
     * <p>The table {@code cmf} is a monotonically nondecreasing cumulative frequency array that may be
     * biased by a nonzero base; all spans are measured relative to {@code cmf[0]}. The helper encodes symbol
     * {@code s} with the relative span {@code [cmf[s] - cmf[0], cmf[s + 1] - cmf[0])} out of the adjusted
     * total {@code cmf[len - 1] - cmf[0]}, the same bounds the decoder resolves {@code s} from.
     *
     * @param encoder the range encoder to advance
     * @param cmf     the cumulative frequency table; at least two entries, nondecreasing
     * @param s       the symbol index to encode, in {@code [0, cmf.length - 1)}
     */
    public static void encodeUpdate(MlowRangeEncoder encoder, int[] cmf, int s) {
        var last = cmf.length - 1;
        var base = cmf[0] & 0xFFFFFFFFL;
        var total = (cmf[last] & 0xFFFFFFFFL) - base;
        encoder.encode((cmf[s] & 0xFFFFFFFFL) - base, (cmf[s + 1] & 0xFFFFFFFFL) - base, total);
    }

    /**
     * Encodes one value into a uniform distribution over {@code [0, n)}, the encode side mirror of
     * {@link #decodeUniform(MlowRangeDecoder, int)}.
     *
     * <p>Every value in {@code [0, n)} is equiprobable, so {@code v} is encoded with the unit width span
     * {@code [v, v + 1)} out of total {@code n}.
     *
     * @param encoder the range encoder to advance
     * @param n       the number of equiprobable values; must be at least 1
     * @param v       the value to encode, in {@code [0, n)}
     */
    public static void encodeUniform(MlowRangeEncoder encoder, int n, int v) {
        encoder.encode(v, v + 1, n);
    }
}
