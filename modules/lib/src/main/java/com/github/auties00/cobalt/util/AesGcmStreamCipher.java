package com.github.auties00.cobalt.util;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

import com.github.auties00.cobalt.telemetry.log.Log;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

/**
 * A streaming AES-GCM cipher that releases ciphertext and plaintext incrementally, built on the JDK's own AES
 * primitives so no third-party security provider is required.
 *
 * <p>The cipher implements AES-GCM as specified by NIST SP 800-38D for the single shape WhatsApp's wire
 * protocols use: a 96-bit nonce, no additional authenticated data, and a 128-bit authentication tag. It is
 * keyed once per message through {@link #init(boolean, SecretKey, byte[])}, then driven through repeated
 * {@link #update} calls and a terminating {@link #doFinal}. The same instance is reused across messages; each
 * {@code init} call resets all state.
 *
 * <p>The reason this exists rather than {@link Cipher#getInstance(String) Cipher.getInstance("AES/GCM/NoPadding")}
 * is incrementality. The JDK's GCM buffers the entire message and only emits its output at {@code doFinal}, so
 * a streaming wire codec built on it would have to hold a whole datagram in memory. This cipher instead emits
 * each block of output as it is produced, which lets the datagram streams keep a fixed-size working buffer
 * regardless of datagram length. The trade is that on decryption plaintext is released from {@link #update}
 * before the tag has been verified; the caller must treat decrypted bytes as authentic only once the matching
 * {@link #doFinal} has returned without throwing, exactly as a streaming AEAD requires.
 *
 * <p>GCM is the composition of AES counter-mode encryption with the GHASH universal hash over GF(2^128). The
 * counter-mode keystream, the hash subkey, and the tag mask are all single AES blocks, which this cipher draws
 * from the JDK's {@code AES/ECB/NoPadding} primitive; only GHASH, which the JDK does not expose, is computed
 * here. Both the GHASH multiply and the final tag comparison are constant time.
 *
 * <p>Instances are not thread-safe; one cipher is intended to be owned by a single reader or writer thread, the
 * same ownership model as the datagram streams that drive it.
 */
public final class AesGcmStreamCipher {
    /**
     * The logger for {@link AesGcmStreamCipher}.
     */
    private static final System.Logger LOGGER = Log.get(AesGcmStreamCipher.class);

    /**
     * Holds the length, in bytes, of the GCM authentication tag this cipher produces and verifies.
     */
    public static final int TAG_LENGTH = 16;

    /**
     * Holds the length, in bytes, of the 96-bit GCM nonce this cipher accepts.
     */
    public static final int NONCE_LENGTH = 12;

    /**
     * Holds the AES block size in bytes, the width of the GHASH field element and the counter block.
     */
    private static final int BLOCK_LENGTH = 16;

    /**
     * Holds the reused JDK AES block cipher in ECB mode that supplies the hash subkey, the tag mask, and every
     * counter-mode keystream block.
     */
    private final Cipher aesBlock;

    /**
     * Holds whether the cipher is currently keyed for encryption ({@code true}) or decryption ({@code false}).
     */
    private boolean encrypting;

    /**
     * Holds the high 64 bits of the GHASH hash subkey {@code H = E(0^128)}, derived on every {@code init}.
     */
    private long subkeyHigh;

    /**
     * Holds the low 64 bits of the GHASH hash subkey {@code H = E(0^128)}, derived on every {@code init}.
     */
    private long subkeyLow;

    /**
     * Holds the bit-reversed high 64 bits of the hash subkey, precomputed on every {@code init} for the
     * high-word half of the GHASH Karatsuba multiply.
     */
    private long subkeyHighReversed;

    /**
     * Holds the bit-reversed low 64 bits of the hash subkey, precomputed on every {@code init} for the
     * high-word half of the GHASH Karatsuba multiply.
     */
    private long subkeyLowReversed;

    /**
     * Holds the exclusive-OR of the two subkey halves, the middle Karatsuba operand, precomputed on every
     * {@code init}.
     */
    private long subkeyMid;

    /**
     * Holds the exclusive-OR of the two bit-reversed subkey halves, the middle Karatsuba operand for the
     * high-word half, precomputed on every {@code init}.
     */
    private long subkeyMidReversed;

    /**
     * Holds the high 64 bits of the running GHASH accumulator over the processed ciphertext.
     */
    private long ghashHigh;

    /**
     * Holds the low 64 bits of the running GHASH accumulator over the processed ciphertext.
     */
    private long ghashLow;

    /**
     * Holds the partial GHASH block accumulating ciphertext bytes until a full sixteen-byte block can be
     * absorbed.
     */
    private final byte[] ghashBlock = new byte[BLOCK_LENGTH];

    /**
     * Holds the number of valid bytes currently buffered in {@link #ghashBlock}.
     */
    private int ghashBlockLength;

    /**
     * Holds the running count of ciphertext bytes absorbed into GHASH, folded into the length block at
     * {@link #doFinal}.
     */
    private long ciphertextLength;

    /**
     * Holds the tag mask {@code E(J0)} that is exclusive-ORed with the final GHASH value to form the tag,
     * derived on every {@code init}.
     */
    private final byte[] tagMask = new byte[BLOCK_LENGTH];

    /**
     * Holds the current counter-mode counter block, the nonce followed by a 32-bit block counter starting at
     * two.
     */
    private final byte[] counterBlock = new byte[BLOCK_LENGTH];

    /**
     * Holds the keystream block for the current counter value, refilled when {@link #keystreamOffset} reaches
     * the block size.
     */
    private final byte[] keystreamBlock = new byte[BLOCK_LENGTH];

    /**
     * Holds the number of keystream bytes already consumed from {@link #keystreamBlock}; the block size forces
     * a refill on the next byte.
     */
    private int keystreamOffset;

    /**
     * Holds, on the decryption path, the most recent up-to-{@value #TAG_LENGTH} input bytes that cannot yet be
     * confirmed as ciphertext because they may be the trailing tag.
     */
    private final byte[] tagHoldback = new byte[TAG_LENGTH];

    /**
     * Holds the number of valid bytes currently buffered in {@link #tagHoldback}.
     */
    private int tagHoldbackLength;

    /**
     * Holds scratch space used to rebuild {@link #tagHoldback} when an {@link #update} both drains and refills
     * it.
     */
    private final byte[] holdbackScratch = new byte[TAG_LENGTH];

    /**
     * Constructs a streaming AES-GCM cipher backed by a fresh JDK AES block cipher.
     *
     * <p>The cipher is unkeyed until {@link #init(boolean, SecretKey, byte[])} is called.
     *
     * @throws GeneralSecurityException if the platform cannot provide an {@code AES/ECB/NoPadding} cipher
     */
    public AesGcmStreamCipher() throws GeneralSecurityException {
        this.aesBlock = Cipher.getInstance("AES/ECB/NoPadding");
    }

    /**
     * Keys the cipher for the given direction under {@code key} and {@code nonce} and resets all per-message
     * state.
     *
     * <p>When {@code forEncryption} is {@code true}, subsequent {@link #update} calls encrypt streamed plaintext
     * into ciphertext one-to-one and the terminating {@link #doFinal} emits the {@value #TAG_LENGTH}-byte
     * authentication tag. When it is {@code false}, subsequent {@link #update} calls decrypt streamed ciphertext
     * into plaintext while holding back the trailing {@value #TAG_LENGTH} bytes as the candidate tag, and
     * {@link #doFinal} verifies that tag, throwing if it does not authenticate. The hash subkey, tag mask, and
     * initial counter are recomputed from {@code key} and {@code nonce} on every call.
     *
     * @param forEncryption {@code true} to key for encryption, {@code false} to key for decryption
     * @param key           the AES key
     * @param nonce         the {@value #NONCE_LENGTH}-byte nonce, which must never repeat under the same key
     * @throws GeneralSecurityException if {@code key} is not a valid AES key for this platform
     * @throws NullPointerException     if {@code key} or {@code nonce} is {@code null}
     * @throws IllegalArgumentException if {@code nonce} is not exactly {@value #NONCE_LENGTH} bytes
     */
    public void init(boolean forEncryption, SecretKey key, byte[] nonce) throws GeneralSecurityException {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(nonce, "nonce cannot be null");
        if (nonce.length != NONCE_LENGTH) {
            throw new IllegalArgumentException("nonce must be " + NONCE_LENGTH + " bytes, got " + nonce.length);
        }
        this.encrypting = forEncryption;
        aesBlock.init(Cipher.ENCRYPT_MODE, key);
        var subkey = new byte[BLOCK_LENGTH];
        try {
            aesBlock.update(new byte[BLOCK_LENGTH], 0, BLOCK_LENGTH, subkey, 0);
            var j0 = new byte[BLOCK_LENGTH];
            System.arraycopy(nonce, 0, j0, 0, NONCE_LENGTH);
            DataUtils.putInt(j0, NONCE_LENGTH, 1, ByteOrder.BIG_ENDIAN);
            aesBlock.update(j0, 0, BLOCK_LENGTH, tagMask, 0);
        } catch (ShortBufferException exception) {
            throw new IllegalStateException("AES block output did not fit a block buffer", exception);
        }
        this.subkeyHigh = DataUtils.getLong(subkey, 0, ByteOrder.BIG_ENDIAN);
        this.subkeyLow = DataUtils.getLong(subkey, 8, ByteOrder.BIG_ENDIAN);
        this.subkeyHighReversed = Long.reverse(subkeyHigh);
        this.subkeyLowReversed = Long.reverse(subkeyLow);
        this.subkeyMid = subkeyLow ^ subkeyHigh;
        this.subkeyMidReversed = subkeyLowReversed ^ subkeyHighReversed;
        System.arraycopy(nonce, 0, counterBlock, 0, NONCE_LENGTH);
        DataUtils.putInt(counterBlock, NONCE_LENGTH, 2, ByteOrder.BIG_ENDIAN);
        this.ghashHigh = 0;
        this.ghashLow = 0;
        this.ghashBlockLength = 0;
        this.ciphertextLength = 0;
        this.keystreamOffset = BLOCK_LENGTH;
        this.tagHoldbackLength = 0;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "aes-gcm init: encrypting={0} nonce={1}", forEncryption, nonce);
    }

    /**
     * Processes a span of input, emitting the bytes that are ready and returning how many were written.
     *
     * <p>On the encryption path every input byte becomes one ciphertext byte, so exactly {@code inputLength}
     * bytes are written. On the decryption path the trailing {@value #TAG_LENGTH} bytes seen so far are held
     * back as the candidate tag and not yet decrypted, so a call may write fewer than {@code inputLength} bytes
     * (and writes nothing until more than a tag's worth of input has accumulated); those held bytes are emitted
     * by later calls or recognised as the tag at {@link #doFinal}. Decrypted plaintext is returned before the
     * tag is verified, so the caller must not act on it as authentic until {@link #doFinal} returns.
     *
     * @param input       the source bytes
     * @param inputOffset the offset of the first source byte
     * @param inputLength the number of source bytes to process
     * @param output      the destination buffer for the produced bytes
     * @param outputOffset the offset at which to write the first produced byte
     * @return the number of bytes written to {@code output}
     * @throws ShortBufferException if {@code output} has too little room for the produced bytes
     * @throws IllegalStateException if the cipher has not been keyed by an {@code init} call
     */
    public int update(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset)
            throws ShortBufferException {
        if (encrypting) {
            ensureCapacity(output, outputOffset, inputLength);
            applyKeystream(input, inputOffset, output, outputOffset, inputLength);
            absorbCiphertext(output, outputOffset, inputLength);
            ciphertextLength += inputLength;
            return inputLength;
        }
        var total = tagHoldbackLength + inputLength;
        var releasable = Math.max(0, total - TAG_LENGTH);
        ensureCapacity(output, outputOffset, releasable);
        var produced = 0;
        var fromHoldback = Math.min(releasable, tagHoldbackLength);
        if (fromHoldback > 0) {
            applyKeystream(tagHoldback, 0, output, outputOffset, fromHoldback);
            absorbCiphertext(tagHoldback, 0, fromHoldback);
            ciphertextLength += fromHoldback;
            produced += fromHoldback;
        }
        var fromInput = releasable - fromHoldback;
        if (fromInput > 0) {
            applyKeystream(input, inputOffset, output, outputOffset + produced, fromInput);
            absorbCiphertext(input, inputOffset, fromInput);
            ciphertextLength += fromInput;
            produced += fromInput;
        }
        var keep = total - releasable;
        for (var i = 0; i < keep; i++) {
            var logical = releasable + i;
            holdbackScratch[i] = logical < tagHoldbackLength
                    ? tagHoldback[logical]
                    : input[inputOffset + (logical - tagHoldbackLength)];
        }
        System.arraycopy(holdbackScratch, 0, tagHoldback, 0, keep);
        tagHoldbackLength = keep;
        return produced;
    }

    /**
     * Finalises the message, emitting the authentication tag when encrypting and verifying it when decrypting.
     *
     * <p>When encrypting, this absorbs any partial trailing ciphertext block and the length block into GHASH,
     * masks the result, and writes the {@value #TAG_LENGTH}-byte tag to {@code output}, returning
     * {@value #TAG_LENGTH}. When decrypting, the {@value #TAG_LENGTH} bytes held back by the final
     * {@link #update} are the message tag: this recomputes the expected tag and compares it in constant time,
     * writing no further plaintext and returning zero on success.
     *
     * @param output       the destination buffer for the tag (encryption) or unused (decryption)
     * @param outputOffset the offset at which to write the tag
     * @return the number of bytes written to {@code output}: {@value #TAG_LENGTH} when encrypting, zero when
     *         decrypting
     * @throws ShortBufferException  if {@code output} has too little room for the tag when encrypting
     * @throws AEADBadTagException   if the tag does not authenticate when decrypting
     */
    public int doFinal(byte[] output, int outputOffset) throws ShortBufferException, AEADBadTagException {
        if (ghashBlockLength > 0) {
            for (var i = ghashBlockLength; i < BLOCK_LENGTH; i++) {
                ghashBlock[i] = 0;
            }
            absorbBlock(DataUtils.getLong(ghashBlock, 0, ByteOrder.BIG_ENDIAN),
                    DataUtils.getLong(ghashBlock, 8, ByteOrder.BIG_ENDIAN));
            ghashBlockLength = 0;
        }
        absorbBlock(0L, ciphertextLength * 8L);
        var tag = new byte[TAG_LENGTH];
        DataUtils.putLong(tag, 0, ghashHigh ^ DataUtils.getLong(tagMask, 0, ByteOrder.BIG_ENDIAN), ByteOrder.BIG_ENDIAN);
        DataUtils.putLong(tag, 8, ghashLow ^ DataUtils.getLong(tagMask, 8, ByteOrder.BIG_ENDIAN), ByteOrder.BIG_ENDIAN);
        if (encrypting) {
            ensureCapacity(output, outputOffset, TAG_LENGTH);
            System.arraycopy(tag, 0, output, outputOffset, TAG_LENGTH);
            if (Log.TRACE) LOGGER.log(Level.TRACE, "aes-gcm doFinal: encryption complete, ciphertextLength={0}", ciphertextLength);
            return TAG_LENGTH;
        }
        if (tagHoldbackLength != TAG_LENGTH
                || !MessageDigest.isEqual(tag, Arrays.copyOf(tagHoldback, TAG_LENGTH))) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "aes-gcm tag verification failed, ciphertextLength={0}", ciphertextLength);
            throw new AEADBadTagException("GCM tag mismatch");
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "aes-gcm doFinal: decryption verified, ciphertextLength={0}", ciphertextLength);
        return 0;
    }

    /**
     * Exclusive-ORs {@code length} bytes of counter-mode keystream into the input, writing the result to the
     * output.
     *
     * <p>The keystream is produced one AES block at a time by encrypting the current counter block and then
     * advancing it with {@code inc32}; bytes are drawn from the current block until it is exhausted, then the
     * next block is generated.
     *
     * @param input        the source bytes
     * @param inputOffset  the offset of the first source byte
     * @param output       the destination buffer
     * @param outputOffset the offset at which to write the first output byte
     * @param length       the number of bytes to process
     */
    private void applyKeystream(byte[] input, int inputOffset, byte[] output, int outputOffset, int length) {
        for (var i = 0; i < length; i++) {
            if (keystreamOffset == BLOCK_LENGTH) {
                try {
                    aesBlock.update(counterBlock, 0, BLOCK_LENGTH, keystreamBlock, 0);
                } catch (ShortBufferException exception) {
                    throw new IllegalStateException("AES block output did not fit the keystream buffer", exception);
                }
                incrementCounter();
                keystreamOffset = 0;
            }
            output[outputOffset + i] = (byte) (input[inputOffset + i] ^ keystreamBlock[keystreamOffset++]);
        }
    }

    /**
     * Advances the counter block by one with GCM's {@code inc32} semantics, incrementing only the trailing
     * 32-bit block counter and wrapping within it.
     */
    private void incrementCounter() {
        var next = DataUtils.getInt(counterBlock, NONCE_LENGTH, ByteOrder.BIG_ENDIAN) + 1;
        DataUtils.putInt(counterBlock, NONCE_LENGTH, next, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Absorbs a span of ciphertext bytes into the GHASH accumulator, buffering a trailing partial block until
     * it fills.
     *
     * @param data   the ciphertext bytes
     * @param offset the offset of the first byte
     * @param length the number of bytes to absorb
     */
    private void absorbCiphertext(byte[] data, int offset, int length) {
        for (var i = 0; i < length; i++) {
            ghashBlock[ghashBlockLength++] = data[offset + i];
            if (ghashBlockLength == BLOCK_LENGTH) {
                absorbBlock(DataUtils.getLong(ghashBlock, 0, ByteOrder.BIG_ENDIAN),
                        DataUtils.getLong(ghashBlock, 8, ByteOrder.BIG_ENDIAN));
                ghashBlockLength = 0;
            }
        }
    }

    /**
     * Folds one full sixteen-byte block into the GHASH accumulator: exclusive-ORs it in, then multiplies the
     * accumulator by the hash subkey in GF(2^128).
     *
     * <p>The multiply is the BearSSL {@code ctmul64} construction. A Karatsuba split computes the 128-bit
     * carryless product from three 64-by-64 carryless products of the low halves, the middle operand (the
     * exclusive-OR of the halves), and the high halves; three further products over the bit-reversed operands
     * recover the high words of the product (the bit reversal turns the high half of a carryless product into
     * the low half of the reversed product). Each 64-by-64 product runs through
     * {@link #carrylessMultiply(long, long)}, and the subkey-derived operands ({@link #subkeyMid}, the
     * bit-reversed {@link #subkeyHighReversed}, {@link #subkeyLowReversed}, {@link #subkeyMidReversed}) are
     * precomputed once per {@code init}. The 255-bit product is then reduced modulo the GCM polynomial
     * {@code x^128 + x^7 + x^2 + x + 1}. The routine has no data-dependent branch and no table lookup, so it is
     * constant time wherever the platform's 64-bit multiply is.
     *
     * @param blockHigh the high 64 bits of the block to fold in
     * @param blockLow  the low 64 bits of the block to fold in
     */
    private void absorbBlock(long blockHigh, long blockLow) {
        var low = ghashLow ^ blockLow;
        var high = ghashHigh ^ blockHigh;
        var lowReversed = Long.reverse(low);
        var highReversed = Long.reverse(high);
        var mid = low ^ high;
        var midReversed = lowReversed ^ highReversed;

        var z0 = carrylessMultiply(low, subkeyLow);
        var z1 = carrylessMultiply(high, subkeyHigh);
        var z2 = carrylessMultiply(mid, subkeyMid);
        var z0High = carrylessMultiply(lowReversed, subkeyLowReversed);
        var z1High = carrylessMultiply(highReversed, subkeyHighReversed);
        var z2High = carrylessMultiply(midReversed, subkeyMidReversed);
        z2 ^= z0 ^ z1;
        z2High ^= z0High ^ z1High;
        z0High = Long.reverse(z0High) >>> 1;
        z1High = Long.reverse(z1High) >>> 1;
        z2High = Long.reverse(z2High) >>> 1;

        var v0 = z0;
        var v1 = z0High ^ z2;
        var v2 = z1 ^ z2High;
        var v3 = z1High;
        v3 = (v3 << 1) | (v2 >>> 63);
        v2 = (v2 << 1) | (v1 >>> 63);
        v1 = (v1 << 1) | (v0 >>> 63);
        v0 = v0 << 1;
        v2 ^= v0 ^ (v0 >>> 1) ^ (v0 >>> 2) ^ (v0 >>> 7);
        v1 ^= (v0 << 63) ^ (v0 << 62) ^ (v0 << 57);
        v3 ^= v1 ^ (v1 >>> 1) ^ (v1 >>> 2) ^ (v1 >>> 7);
        v2 ^= (v1 << 63) ^ (v1 << 62) ^ (v1 << 57);
        ghashLow = v2;
        ghashHigh = v3;
    }

    /**
     * Returns the low 64 bits of the carryless (GF(2) polynomial) product of two 64-bit values, the building
     * block of the GHASH Karatsuba multiply.
     *
     * <p>Each operand is split into the four bit-groups whose indices share a residue modulo four, so set bits
     * within a group are spaced four apart; an ordinary integer multiply of two such groups cannot let a carry
     * reach the next group's bit position, and masking each partial product back to its group recovers the
     * carryless result. The routine is branch-free and table-free, hence constant time on any platform whose
     * 64-bit multiply is.
     *
     * @param x the first operand
     * @param y the second operand
     * @return the low 64 bits of the carryless product {@code x * y}
     */
    private static long carrylessMultiply(long x, long y) {
        var x0 = x & 0x1111111111111111L;
        var x1 = x & 0x2222222222222222L;
        var x2 = x & 0x4444444444444444L;
        var x3 = x & 0x8888888888888888L;
        var y0 = y & 0x1111111111111111L;
        var y1 = y & 0x2222222222222222L;
        var y2 = y & 0x4444444444444444L;
        var y3 = y & 0x8888888888888888L;
        var z0 = (x0 * y0) ^ (x1 * y3) ^ (x2 * y2) ^ (x3 * y1);
        var z1 = (x0 * y1) ^ (x1 * y0) ^ (x2 * y3) ^ (x3 * y2);
        var z2 = (x0 * y2) ^ (x1 * y1) ^ (x2 * y0) ^ (x3 * y3);
        var z3 = (x0 * y3) ^ (x1 * y2) ^ (x2 * y1) ^ (x3 * y0);
        z0 &= 0x1111111111111111L;
        z1 &= 0x2222222222222222L;
        z2 &= 0x4444444444444444L;
        z3 &= 0x8888888888888888L;
        return z0 | z1 | z2 | z3;
    }

    /**
     * Verifies that {@code output} has room for {@code length} bytes starting at {@code offset}.
     *
     * @param output the destination buffer
     * @param offset the write offset
     * @param length the number of bytes to be written
     * @throws ShortBufferException if the buffer is too small
     */
    private static void ensureCapacity(byte[] output, int offset, int length) throws ShortBufferException {
        if (output.length - offset < length) {
            throw new ShortBufferException("output buffer needs " + length + " bytes at offset " + offset);
        }
    }
}
