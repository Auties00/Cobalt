package com.github.auties00.cobalt.calls.media.audio.codec.mlow.entropy;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Encodes symbols into the MLow speech codec bitstream, the inverse of {@link MlowRangeDecoder}.
 *
 * <p>MLow uses the byte wise range coder shared by the Opus and CELT audio codecs, a form of arithmetic
 * coding that emits one whole output byte per renormalization step. This class is an integer bit exact
 * implementation: every register is a 32 bit unsigned quantity held in a {@code long} masked to 32 bits, so
 * the Java arithmetic reproduces 32 bit unsigned wraparound exactly. An encoder built from this class and a
 * {@link MlowRangeDecoder} round trip any symbol stream byte for byte and agree on the final {@code rng}
 * register.
 *
 * <p>The encoder tracks two 32 bit registers. {@code rng} is the number of distinct values the current
 * coding interval can represent. {@code val} is the low end of the current interval (where the decoder
 * tracks the difference between the top of the interval and the input value, minus one). Encoding a symbol
 * is a single transaction: the caller resolves its symbol to a cumulative frequency span {@code [fl, fh)}
 * out of a total {@code ft} and calls {@link #encode(long, long, long)}, which narrows the interval to that
 * span and renormalizes, emitting whole output bytes as they become final. Convenience entry points
 * ({@link #encodeIcdf(int, int[], int)}, {@link #encodeBitLogp(int, int)}, {@link #encodeBits(long, int)},
 * {@link #encodeUint(long, long)}) fold symbol resolution into one call for the mechanical cases.
 *
 * <p>Raw bits ({@link #encodeBits(long, int)} and the trailing fixed width tail of
 * {@link #encodeUint(long, long)}) are written to the OPPOSITE end of the buffer through a separate bit
 * window. The range coded symbols fill bytes from the front via {@code offs}; raw bits fill bytes from the
 * back via {@code endOffs}. The two streams grow toward the middle of the same buffer; an overflow that
 * would cross the streams sets {@link #hasError()} rather than corrupting either side.
 *
 * <p>The carry propagation chain is the subtle heart of the encoder. Because the interval is narrowed from
 * the low end, adding to {@code val} can carry into bytes that have already been computed but not yet
 * written. {@link #carryOut(int)} buffers one pending byte in {@code rem} and counts a run of {@code 0xFF}
 * bytes in {@code ext} so that a later carry can ripple the {@code +1} across all of them at once; a byte is
 * emitted only once it can no longer be affected by a future carry. This is why a freshly initialized
 * encoder seeds {@code rem} to {@code -1} (the no buffered byte sentinel) and why {@link #finish()} must
 * flush both the buffered byte and any outstanding {@code 0xFF} run.
 *
 * <p>This type is not thread safe; an encoder instance owns one frame's emission and is single threaded by
 * construction (one encode runs on one virtual thread).
 *
 * @implNote The normalization threshold is {@code EC_CODE_BOT} = {@code EC_CODE_TOP >> EC_SYM_BITS} =
 * {@code (1 << 31) >> 8} = {@code 2^23} = {@code 0x800000}, not {@code 2^24}: while {@code rng <= 2^23} the
 * encoder emits another output byte and rescales, exactly the threshold the decoder normalizes against.
 * {@code EC_CODE_SHIFT} = {@code EC_CODE_BITS - EC_SYM_BITS - 1} = 23 is the shift that moves the high order
 * byte of {@code val} into a carry out symbol. Unsigned division uses plain {@link Long#divideUnsigned}; an
 * ARM only small division table optimization returns the identical quotient and is not used.
 */
public final class MlowRangeEncoder {
    /**
     * The logger for {@link MlowRangeEncoder}.
     */
    private static final System.Logger LOGGER = Log.get(MlowRangeEncoder.class);

    /**
     * Number of bits emitted at a time by the range coder.
     *
     * <p>The coder works in base {@code 2^8}: one byte per renormalization step.
     */
    private static final int EC_SYM_BITS = 8;

    /**
     * Total number of bits in each state register.
     *
     * <p>Both {@code val} and {@code rng} are 32 bit unsigned quantities.
     */
    private static final int EC_CODE_BITS = 32;

    /**
     * Largest value a single output symbol can take, {@code (1 << 8) - 1} = 255.
     *
     * <p>Also the carry propagation sentinel: a carry out value equal to this cannot yet be resolved, so it
     * extends the pending {@code 0xFF} run instead of flushing the buffer.
     */
    private static final long EC_SYM_MAX = (1L << EC_SYM_BITS) - 1;

    /**
     * Carry bit of the high order range symbol, {@code 1 << (32 - 1)} = {@code 2^31}.
     *
     * <p>After full renormalization {@code rng} always lies in {@code (2^23, 2^31]}. {@code val} is kept
     * below this value by the {@code & (EC_CODE_TOP - 1)} mask in {@link #normalize()}.
     */
    private static final long EC_CODE_TOP = 1L << (EC_CODE_BITS - 1);

    /**
     * Low order bit of the high order range symbol, {@code EC_CODE_TOP >> EC_SYM_BITS} = {@code 2^23} =
     * {@code 0x800000}.
     *
     * <p>This is the renormalization threshold. While {@code rng <= EC_CODE_BOT} the encoder carries out the
     * high order byte of {@code val} and shifts the interval up by one byte. Using {@code 2^24} here would
     * renormalize one byte too eagerly whenever {@code rng} lands in {@code (2^23, 2^24]} and would emit a
     * byte the decoder would not consume, desynchronizing the two coders.
     */
    private static final long EC_CODE_BOT = EC_CODE_TOP >> EC_SYM_BITS;

    /**
     * Bits to shift to move the high order byte of {@code val} into a carry out symbol,
     * {@code EC_CODE_BITS - EC_SYM_BITS - 1} = {@code 32 - 8 - 1} = 23.
     *
     * <p>{@link #normalize()} and {@link #finish()} shift {@code val} (or the rounded end value) right by
     * this amount to obtain the next byte to carry out, leaving the low {@code EC_CODE_SHIFT} bits to be
     * promoted on the next step.
     */
    private static final int EC_CODE_SHIFT = EC_CODE_BITS - EC_SYM_BITS - 1;

    /**
     * Number of bits in the raw bit window, 32.
     *
     * <p>The window holds at most 32 bits. The flush loop in {@link #encodeBits(long, int)} drains whole
     * bytes once the window would otherwise overflow.
     */
    private static final int EC_WINDOW_SIZE = 32;

    /**
     * Number of bits of the range coded portion of a raw unsigned integer, 8.
     *
     * <p>{@link #encodeUint(long, long)} splits large integers into a range coded high part of up to this
     * many bits and a raw low part written by {@link #encodeBits(long, int)}.
     */
    private static final int EC_UINT_BITS = 8;

    /**
     * Mask isolating the low 32 bits, used to emulate 32 bit unsigned wraparound.
     */
    private static final long U32 = 0xFFFFFFFFL;

    /**
     * The output byte buffer holding both the front range coded bytes and the back raw bits.
     */
    private final byte[] buf;

    /**
     * Base index in {@link #buf} of logical byte 0, supporting encode into a sub window of a larger array.
     */
    private final int bufBase;

    /**
     * Number of bytes available to the encoder starting at logical index 0.
     *
     * <p>{@link #shrink(int)} may reduce this after the symbol stream is final, moving the back raw bits up
     * to fit the compacted size.
     */
    private int storage;

    /**
     * Offset of the next range coded byte to write at the front of the buffer.
     */
    private int offs;

    /**
     * Number of raw bit bytes already written at the back of the buffer.
     */
    private int endOffs;

    /**
     * Buffered raw bits awaiting emission, held as a 32 bit unsigned value.
     */
    private long endWindow;

    /**
     * Number of valid bits currently in {@link #endWindow}.
     */
    private int nendBits;

    /**
     * Running count of whole bits written, excluding the partial bits still inside the range coder.
     *
     * <p>{@link #tell()} and {@link #tellFrac()} derive the produced bit estimate from this field. It is
     * seeded in the constructor with the {@code EC_CODE_BITS + 1} bias so the count matches a decoder
     * reading the same stream.
     */
    private int nbitsTotal;

    /**
     * The current interval size, as a 32 bit unsigned value.
     *
     * <p>After every {@link #normalize()} this lies in {@code (EC_CODE_BOT, EC_CODE_TOP]} =
     * {@code (2^23, 2^31]}.
     */
    private long rng;

    /**
     * The low end of the current interval, as a 32 bit unsigned value.
     *
     * <p>This is the encoder counterpart of the decoder's difference register; encoding adds the
     * complementary span of the chosen symbol to it.
     */
    private long val;

    /**
     * The number of outstanding carry propagating {@code 0xFF} symbols, as a 32 bit unsigned value.
     *
     * <p>When {@link #carryOut(int)} sees a symbol equal to {@link #EC_SYM_MAX} it cannot yet decide whether
     * a later carry will turn the buffered run into all zero or leave it as {@code 0xFF}; it increments this
     * counter and defers the emission. A subsequent carry out that is not {@code 0xFF} resolves the whole
     * run.
     */
    private long ext;

    /**
     * The single buffered output byte awaiting carry propagation; {@code -1} until the first carry out, the
     * no buffered byte sentinel.
     *
     * <p>Held as a signed {@code int} so the {@code -1} sentinel is distinguishable from the byte value 0.
     */
    private int rem;

    /**
     * Nonzero once a write has overflowed the buffer.
     *
     * <p>Set when the front and back streams would cross or when {@link #finish()} cannot fit the trailing
     * raw bits. The encoder keeps running after an error so the caller can observe it through
     * {@link #hasError()} rather than catching an exception.
     */
    private int error;

    /**
     * Constructs a range encoder writing into an entire byte array.
     *
     * <p>Equivalent to {@link #MlowRangeEncoder(byte[], int, int)} with {@code offset} 0 and {@code length}
     * equal to {@code buffer.length}.
     *
     * @param buffer the output byte array
     * @throws NullPointerException if {@code buffer} is {@code null}
     */
    public MlowRangeEncoder(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    /**
     * Constructs a range encoder writing into a window of a byte array and primes the interval.
     *
     * <p>The window {@code [offset, offset + length)} becomes the logical buffer; index 0 of the logical
     * buffer is {@code buffer[offset]}. The constructor seeds {@code rng = EC_CODE_TOP} = {@code 2^31},
     * {@code val = 0}, {@code rem = -1} (no buffered byte), {@code ext = 0}, and the {@code nbitsTotal} bias
     * to {@code EC_CODE_BITS + 1} so {@link #tell()} reports the same one bit initial count expected of the
     * stream. No output byte is produced until the first renormalization.
     *
     * @param buffer the backing byte array
     * @param offset the index in {@code buffer} of the first logical byte
     * @param length the number of logical bytes available to the encoder
     * @throws NullPointerException      if {@code buffer} is {@code null}
     * @throws IndexOutOfBoundsException if {@code [offset, offset + length)} is not within {@code buffer}
     */
    public MlowRangeEncoder(byte[] buffer, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > buffer.length) {
            throw new IndexOutOfBoundsException(
                    "window [" + offset + ", " + (offset + length) + ") out of bounds for length " + buffer.length);
        }
        this.buf = buffer;
        this.bufBase = offset;
        this.storage = length;
        this.offs = 0;
        this.endOffs = 0;
        this.endWindow = 0;
        this.nendBits = 0;
        this.nbitsTotal = EC_CODE_BITS + 1;
        this.rng = EC_CODE_TOP;
        this.rem = -1;
        this.val = 0;
        this.ext = 0;
        this.error = 0;
    }

    /**
     * Writes one range coded byte at the front of the buffer.
     *
     * <p>Stores {@code value} at {@code offs} and advances {@code offs}, unless the front and back streams
     * would meet ({@code offs + endOffs >= storage}), in which case it sets {@link #error} and writes
     * nothing.
     *
     * @param value the byte to write, in the low eight bits
     */
    private void writeByte(int value) {
        if (offs + endOffs >= storage) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "mlow range encode: buffer overflow offs={0} endOffs={1} storage={2}",
                        offs, endOffs, storage);
            }
            error = 1;
            return;
        }
        buf[bufBase + offs++] = (byte) value;
    }

    /**
     * Writes one raw bit byte at the back of the buffer.
     *
     * <p>Stores {@code value} at {@code storage - 1 - endOffs} and advances {@code endOffs}, unless the front
     * and back streams would meet, in which case it sets {@link #error} and writes nothing. {@code endOffs}
     * is incremented before indexing, so the first byte written lands at {@code storage - 1}.
     *
     * @param value the byte to write, in the low eight bits
     */
    private void writeByteAtEnd(int value) {
        if (offs + endOffs >= storage) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "mlow range encode: buffer overflow offs={0} endOffs={1} storage={2}",
                        offs, endOffs, storage);
            }
            error = 1;
            return;
        }
        buf[bufBase + storage - ++endOffs] = (byte) value;
    }

    /**
     * Emits a carried out symbol, buffering it against a possible future carry.
     *
     * <p>When {@code c} is not {@link #EC_SYM_MAX} no further carry can propagate through it, so the buffered
     * byte is flushed: the carry bit {@code c >> EC_SYM_BITS} is added to the pending {@code rem} (skipped on
     * the very first carry out, when {@code rem} is still {@code -1}), any outstanding {@code 0xFF} run in
     * {@code ext} is written out as {@code (0xFF + carry) & 0xFF}, and {@code rem} is reloaded with the low
     * byte of {@code c}. When {@code c} equals {@link #EC_SYM_MAX} the decision is deferred by incrementing
     * {@code ext}.
     *
     * @param c the symbol to carry out; the high bits above {@link #EC_SYM_MAX} carry into the buffered byte
     */
    private void carryOut(int c) {
        if ((c & 0xFFFFFFFFL) != EC_SYM_MAX) {
            var carry = c >> EC_SYM_BITS;
            if (rem >= 0) {
                writeByte(rem + carry);
            }
            if (Long.compareUnsigned(ext, 0) > 0) {
                var sym = (int) ((EC_SYM_MAX + carry) & EC_SYM_MAX);
                do {
                    writeByte(sym);
                } while (Long.compareUnsigned(--ext, 0) > 0);
            }
            rem = (int) (c & EC_SYM_MAX);
        } else {
            ext = (ext + 1) & U32;
        }
    }

    /**
     * Renormalizes {@code val} and {@code rng} so the interval again fills the high order byte.
     *
     * <p>While {@code rng <= EC_CODE_BOT} ({@code 2^23}) the interval is too small to add another symbol
     * cleanly, so the loop carries out the high order byte of {@code val} ({@code val >> EC_CODE_SHIFT}),
     * shifts {@code val} left one byte (masking off the carried byte with {@code EC_CODE_TOP - 1}), shifts
     * {@code rng} left one byte, and bumps {@code nbitsTotal}.
     *
     * @implNote The carry out happens before the shift so the byte leaving the top is the one fed to
     * {@link #carryOut(int)}.
     */
    private void normalize() {
        while (Long.compareUnsigned(rng, EC_CODE_BOT) <= 0) {
            carryOut((int) (val >>> EC_CODE_SHIFT));
            val = (val << EC_SYM_BITS) & (EC_CODE_TOP - 1);
            rng = (rng << EC_SYM_BITS) & U32;
            nbitsTotal += EC_SYM_BITS;
        }
    }

    /**
     * Encodes a symbol given its cumulative frequency span.
     *
     * <p>Computes {@code r = rng / ft}. When {@code fl > 0} it adds the complementary high span
     * {@code rng - r * (ft - fl)} to {@code val} and narrows {@code rng} to {@code r * (fh - fl)}; when
     * {@code fl} is zero it only subtracts {@code r * (ft - fh)} from {@code rng}, keeping the interval
     * aligned to the buffer top. Renormalizes before returning. The span {@code [fl, fh)} out of total
     * {@code ft} must be the span the matching decoder will resolve to the same symbol.
     *
     * @param fl the cumulative frequency of all symbols before the one being encoded
     * @param fh the cumulative frequency up to and including the one being encoded
     * @param ft the total frequency of the alphabet; must be positive and at most {@code 2^32 - 1}
     */
    public void encode(long fl, long fh, long ft) {
        var r = celtUdiv(rng, ft);
        if (fl > 0) {
            val = (val + rng - imul32(r, ft - fl)) & U32;
            rng = imul32(r, fh - fl);
        } else {
            rng = (rng - imul32(r, ft - fh)) & U32;
        }
        normalize();
    }

    /**
     * Encodes a symbol whose total frequency is a power of two.
     *
     * <p>Equivalent to {@link #encode(long, long, long)} with {@code ft = 1 << bits}, but derives the scale
     * factor {@code r = rng >> bits} by a shift instead of a division.
     *
     * @param fl   the cumulative frequency of all symbols before the one being encoded
     * @param fh   the cumulative frequency up to and including the one being encoded
     * @param bits the base two logarithm of the total frequency; must be in {@code [0, 31]}
     */
    public void encodeBin(long fl, long fh, int bits) {
        var r = rng >>> bits;
        if (fl > 0) {
            val = (val + rng - imul32(r, (1L << bits) - fl)) & U32;
            rng = imul32(r, fh - fl);
        } else {
            rng = (rng - imul32(r, (1L << bits) - fh)) & U32;
        }
        normalize();
    }

    /**
     * Encodes a single bit whose probability of being one is {@code 1 / (1 << logp)}.
     *
     * <p>Splits the interval at {@code s = rng >> logp}: when {@code value} is nonzero it takes the small sub
     * interval (sets {@code val += rng - s}, {@code rng = s}); otherwise it keeps the large sub interval
     * ({@code rng = rng - s}). Renormalizes before returning.
     *
     * @param value the bit to encode; zero or nonzero
     * @param logp  the negative base two logarithm of the probability of a one bit
     */
    public void encodeBitLogp(int value, int logp) {
        var r = rng;
        var l = val;
        var s = r >>> logp;
        r = (r - s) & U32;
        if (value != 0) {
            val = (l + r) & U32;
        }
        rng = (value != 0) ? s : r;
        normalize();
    }

    /**
     * Encodes one symbol against an inverse cumulative distribution table.
     *
     * <p>The table {@code icdf} never increases and ends with a zero entry; symbol {@code s} occupies the
     * interval {@code [s > 0 ? ft - icdf[s - 1] : 0, ft - icdf[s])} where {@code ft = 1 << ftb}. Scaling each
     * frequency by {@code r = rng >> ftb}, this adds the complementary high span when {@code s > 0} and
     * narrows {@code rng} to {@code r * (icdf[s - 1] - icdf[s])}, or for {@code s == 0} subtracts
     * {@code r * icdf[0]} from {@code rng}. Renormalizes before returning.
     *
     * @param s    the index of the symbol to encode
     * @param icdf the inverse CDF table; entries are unsigned bytes (0 to 255), never increasing, last is 0
     * @param ftb  the number of bits of precision in the distribution, so {@code ft = 1 << ftb}
     */
    public void encodeIcdf(int s, int[] icdf, int ftb) {
        var r = rng >>> ftb;
        if (s > 0) {
            val = (val + rng - imul32(r, icdf[s - 1] & 0xFFL)) & U32;
            rng = imul32(r, (icdf[s - 1] & 0xFFL) - (icdf[s] & 0xFFL));
        } else {
            rng = (rng - imul32(r, icdf[s] & 0xFFL)) & U32;
        }
        normalize();
    }

    /**
     * Encodes a raw unsigned integer with an arbitrary range into the stream.
     *
     * <p>When the range needs more than {@code EC_UINT_BITS} = 8 bits the value is split: the high part is
     * range coded through one {@link #encode(long, long, long)} and the low part is written raw by
     * {@link #encodeBits(long, int)}. For small ranges the whole value is range coded in one transaction.
     * The matching {@link MlowRangeDecoder#decodeUint(long)} reassembles the value identically.
     *
     * @param value the integer to encode; must be in {@code [0, ft)}
     * @param ft    one more than the maximum encodable value; must be at least 2 and at most {@code 2^32 - 1}
     */
    public void encodeUint(long value, long ft) {
        var ftMinus = (ft - 1) & U32;
        var ftb = ecIlog(ftMinus);
        if (ftb > EC_UINT_BITS) {
            ftb -= EC_UINT_BITS;
            var ftHigh = (ftMinus >>> ftb) + 1;
            var fl = (value >>> ftb) & U32;
            encode(fl, fl + 1, ftHigh);
            encodeBits(value & (((1L << ftb) - 1) & U32), ftb);
        } else {
            encode(value, value + 1, ftMinus + 1);
        }
    }

    /**
     * Writes a fixed width run of raw bits to the back of the stream.
     *
     * <p>Raw bits are accumulated least significant first into a 32 bit window. When adding {@code bits}
     * would overflow the window the flush loop drains whole bytes from the low end with
     * {@link #writeByteAtEnd(int)} until they fit, then the new bits are shifted in above the remaining ones.
     * The bytes are written to the END of the buffer, opposite the range coded stream.
     *
     * @param value the bits to write, in the low {@code bits} bits
     * @param bits  the number of raw bits to write; must be in {@code [1, 25]}
     */
    public void encodeBits(long value, int bits) {
        var window = endWindow;
        var used = nendBits;
        if (used + bits > EC_WINDOW_SIZE) {
            do {
                writeByteAtEnd((int) (window & EC_SYM_MAX));
                window = (window >>> EC_SYM_BITS) & U32;
                used -= EC_SYM_BITS;
            } while (used >= EC_SYM_BITS);
        }
        window = (window | ((value << used) & U32)) & U32;
        used += bits;
        endWindow = window;
        nendBits = used;
        nbitsTotal += bits;
    }

    /**
     * Overwrites a few bits at the very start of the stream after they were encoded.
     *
     * <p>Replaces the high {@code nbits} bits of the first coded byte with {@code value}. The patched bits
     * land wherever the first byte currently lives: in the finalized output byte {@code buf[0]} once
     * {@code offs > 0}, in the buffered {@code rem} byte while it still awaits carry propagation, or directly
     * in the top of {@code val} if renormalization has not yet run. If fewer than {@code nbits} bits have
     * been encoded the request cannot be honored and {@link #error} is set. For this to be safe at least
     * {@code nbits} bits must have been encoded with exact power of two probabilities, which the caller
     * guarantees.
     *
     * @param value the replacement bits, in the low {@code nbits} bits, decoded most significant first
     * @param nbits the number of leading bits to overwrite; must be at most {@link #EC_SYM_BITS}
     */
    public void patchInitialBits(int value, int nbits) {
        var shift = EC_SYM_BITS - nbits;
        var mask = ((1 << nbits) - 1) << shift;
        if (offs > 0) {
            buf[bufBase] = (byte) ((buf[bufBase] & ~mask) | (value << shift));
        } else if (rem >= 0) {
            rem = (rem & ~mask) | (value << shift);
        } else if (Long.compareUnsigned(rng, EC_CODE_TOP >> nbits) <= 0) {
            val = (val & ~(((long) mask << EC_CODE_SHIFT) & U32)) | (((long) value << (EC_CODE_SHIFT + shift)) & U32);
        } else {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "mlow range encode: patchInitialBits failed, insufficient bits encoded "
                        + "nbits={0}", nbits);
            }
            error = -1;
        }
    }

    /**
     * Compacts the buffer to a new, smaller size by moving the back raw bits up.
     *
     * <p>Relocates the {@code endOffs} trailing raw bit bytes from the end of the old window to the end of
     * the new {@code size} byte window and records the new storage. The caller must ensure the already
     * written front and back bytes fit in {@code size}; this method does not verify it.
     *
     * @param size the new logical buffer size in bytes; at most the current storage and large enough to hold
     *             the bytes already written
     */
    public void shrink(int size) {
        System.arraycopy(buf, bufBase + storage - endOffs, buf, bufBase + size - endOffs, endOffs);
        storage = size;
    }

    /**
     * Flushes all remaining state to the output buffer and finalizes the stream.
     *
     * <p>Emits the minimum number of bits that pins the encoded symbols unambiguously: it rounds {@code val}
     * up to the nearest multiple whose low {@code l} bits are zero (where {@code l = EC_CODE_BITS -
     * ilog(rng)}), widening by one more bit if the rounded end could fall outside the interval, then carries
     * out the high bytes of that end value. It flushes the buffered {@code rem} byte and any outstanding
     * {@code 0xFF} run, drains whole bytes of the raw bit window to the back, clears the gap between the two
     * streams, and ORs the final partial raw bit byte into the last back byte. After this call the encoder
     * must be reinitialized before reuse.
     *
     * @implNote The rounding uses {@code msk = (EC_CODE_TOP - 1) >> l} and {@code end = (val + msk) & ~msk},
     * with an {@code (end | msk) >= val + rng} test deciding whether to widen by one bit. When the encoder
     * has busted and too few bits remain it truncates the trailing window to {@code l} bits and flags the
     * error; otherwise it ORs the partial window into {@code buf[storage - endOffs - 1]}.
     */
    public void finish() {
        var l = EC_CODE_BITS - ecIlog(rng);
        var msk = (EC_CODE_TOP - 1) >>> l;
        var end = (val + msk) & ~msk & U32;
        if (Long.compareUnsigned((end | msk) & U32, (val + rng) & U32) >= 0) {
            l++;
            msk >>>= 1;
            end = (val + msk) & ~msk & U32;
        }
        while (l > 0) {
            carryOut((int) (end >>> EC_CODE_SHIFT));
            end = (end << EC_SYM_BITS) & (EC_CODE_TOP - 1);
            l -= EC_SYM_BITS;
        }
        if (rem >= 0 || Long.compareUnsigned(ext, 0) > 0) {
            carryOut(0);
        }
        var window = endWindow;
        var used = nendBits;
        while (used >= EC_SYM_BITS) {
            writeByteAtEnd((int) (window & EC_SYM_MAX));
            window = (window >>> EC_SYM_BITS) & U32;
            used -= EC_SYM_BITS;
        }
        if (error == 0) {
            var clearFrom = bufBase + offs;
            var clearLen = storage - offs - endOffs;
            for (var i = 0; i < clearLen; i++) {
                buf[clearFrom + i] = 0;
            }
            if (used > 0) {
                if (endOffs >= storage) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "mlow range encode finish: trailing bits do not fit storage={0}",
                                storage);
                    }
                    error = -1;
                } else {
                    l = -l;
                    if (offs + endOffs >= storage && l < used) {
                        window &= (1L << l) - 1;
                        if (Log.WARNING) {
                            LOGGER.log(Level.WARNING, "mlow range encode finish: truncated trailing bit window "
                                    + "storage={0} used={1}", storage, used);
                        }
                        error = -1;
                    }
                    var idx = bufBase + storage - endOffs - 1;
                    buf[idx] = (byte) ((buf[idx] & 0xFF) | (int) window);
                }
            }
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "mlow range encode finish: bytes={0} error={1}", offs, error != 0);
        }
    }

    /**
     * Encodes a symbol resolved against a cumulative frequency table.
     *
     * <p>The table {@code cmf} is a cumulative frequency array that never decreases and may be biased by a
     * nonzero base, so all spans are measured relative to {@code cmf[0]}. This encodes symbol {@code s} with
     * relative span {@code [cmf[s] - cmf[0], cmf[s + 1] - cmf[0])} out of adjusted total
     * {@code cmf[len - 1] - cmf[0]}.
     *
     * @param cmf the cumulative frequency table; at least two entries, never decreasing
     * @param s   the symbol index to encode, in {@code [0, cmf.length - 1)}
     */
    public void encodeUpdate(int[] cmf, int s) {
        var last = cmf.length - 1;
        var base = cmf[0] & U32;
        var total = (cmf[last] & U32) - base;
        encode((cmf[s] & U32) - base, (cmf[s + 1] & U32) - base, total);
    }

    /**
     * Encodes one value drawn from a uniform distribution over {@code [0, n)}.
     *
     * <p>Every value in {@code [0, n)} is equiprobable, so the value {@code v} is encoded with the unit width
     * span {@code [v, v + 1)} out of total {@code n}.
     *
     * @param n the number of equiprobable values; must be at least 1
     * @param v the value to encode, in {@code [0, n)}
     */
    public void encodeUniform(int n, int v) {
        encode(v, v + 1, n);
    }

    /**
     * Returns the number of whole bits produced so far.
     *
     * <p>Computed as {@code nbitsTotal - ilog(rng)}. The estimate is always slightly larger than the exact
     * value because all rounding error is in the positive direction; it is suitable for symmetric coding
     * decisions a decoder reproduces.
     *
     * @return the number of bits produced, rounded up
     */
    public int tell() {
        return nbitsTotal - ecIlog(rng);
    }

    /**
     * Returns the number of bits produced so far scaled by {@code 2^BITRES} (eighths of a bit).
     *
     * <p>Linearizes the leading bits of {@code rng} and corrects with an eight entry threshold table to
     * recover the fractional bit position to {@code 1/8} bit resolution.
     *
     * @return the number of bits produced, scaled by 8, as a 32 bit unsigned value
     */
    public long tellFrac() {
        var nbits = ((long) nbitsTotal << BITRES) & U32;
        var l = ecIlog(rng);
        var r = (rng >>> (l - 16)) & U32;
        var b = (r >>> 12) - 8;
        b += (Long.compareUnsigned(r, TELL_FRAC_CORRECTION[(int) b]) > 0) ? 1 : 0;
        var lScaled = ((long) l << BITRES) + b;
        return (nbits - lScaled) & U32;
    }

    /**
     * Returns the number of range coded bytes written at the front so far.
     *
     * <p>This is {@code offs}; it grows by one each time {@link #carryOut(int)} flushes a byte. It is the
     * front stream length, not the total packet length, which also includes the back raw bit bytes.
     *
     * @return the number of range coded bytes written, in {@code [0, storage]}
     */
    public int rangeBytes() {
        return offs;
    }

    /**
     * Returns whether a write has overflowed the buffer.
     *
     * <p>Set when the front and back streams would cross or when {@link #finish()} cannot fit the trailing
     * raw bits. The encoder keeps running after an error; the flag records that output was truncated.
     *
     * @return {@code true} if an error occurred during encoding, {@code false} otherwise
     */
    public boolean hasError() {
        return error != 0;
    }

    /**
     * Returns the current value of the {@code rng} register as a 32 bit unsigned value held in a
     * {@code long}.
     *
     * <p>After {@link #finish()} this is the final range register of the packet. Two encoders that produced
     * byte identical output also agree on this value, so it is the standard {@code encFinalRange} cross check
     * that the two range coders ran in lockstep. It is a diagnostic, not part of the byte output contract.
     *
     * @return the {@code rng} register in {@code [0, 2^32)}
     */
    public long finalRange() {
        return rng;
    }

    /**
     * Returns the current value of the {@code val} register as a 32 bit unsigned value.
     *
     * <p>Exposed for state inspection and bit exactness verification against a reference trace; it is not
     * part of the encode contract.
     *
     * @return the {@code val} register in {@code [0, 2^32)}
     */
    long val() {
        return val;
    }

    /**
     * Returns the current value of the {@code rng} register as a 32 bit unsigned value.
     *
     * <p>Exposed for state inspection and bit exactness verification against a reference trace; it is not
     * part of the encode contract.
     *
     * @return the {@code rng} register in {@code [0, 2^32)}
     */
    long rng() {
        return rng;
    }

    /**
     * Resolution of the fractional bit usage measurement, 3, so units are eighths of a bit.
     */
    private static final int BITRES = 3;

    /**
     * Threshold table used by {@link #tellFrac()} to recover the exact fractional bit transition points.
     */
    private static final long[] TELL_FRAC_CORRECTION = {
            35733, 38967, 42495, 46340,
            50535, 55109, 60097, 65535
    };

    /**
     * Computes a 32 bit truncating unsigned multiply.
     *
     * <p>Multiplies two 32 bit unsigned operands and keeps the low 32 bits, reproducing 32 bit unsigned
     * overflow.
     *
     * @param a the first operand, treated as 32 bit unsigned
     * @param b the second operand, treated as 32 bit unsigned
     * @return the low 32 bits of {@code a * b}
     */
    private static long imul32(long a, long b) {
        return (a * b) & U32;
    }

    /**
     * Computes the integer base two logarithm plus one of a nonzero value.
     *
     * <p>Equals {@code 32 - numberOfLeadingZeros(v)}, the position of the most significant set bit plus one.
     * Undefined for {@code v == 0}; callers guarantee a nonzero argument.
     *
     * @param v the value whose bit length is required; must be nonzero in {@code [1, 2^32)}
     * @return the number of significant bits in {@code v}
     */
    private static int ecIlog(long v) {
        return 32 - Integer.numberOfLeadingZeros((int) (v & U32));
    }

    /**
     * Computes unsigned integer division.
     *
     * <p>Plain unsigned division; an ARM only small division table optimization returns the identical
     * quotient and is not used.
     *
     * @param n the dividend, treated as 32 bit unsigned
     * @param d the divisor, treated as 32 bit unsigned; must be positive
     * @return the unsigned quotient {@code n / d}
     */
    private static long celtUdiv(long n, long d) {
        return Long.divideUnsigned(n, d);
    }
}
