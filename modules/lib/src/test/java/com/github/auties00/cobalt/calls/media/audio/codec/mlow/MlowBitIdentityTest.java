package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.MLowAudioDecoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Strict bit-identity oracle for the MLow codec: a SHA-256 over every decoded PCM sample and every
 * encoded packet byte, pinned to a golden constant so the upcoming MLow optimization pass can prove it
 * changed zero samples and zero bytes.
 *
 * <p>The decode side replays the canonical parity corpus ({@code parity-3.json}, {@code parity-4.json})
 * through a fresh {@link MLowAudioDecoder} per file (mirroring {@code MlowDecoderParityTest} so the
 * cross-packet CELP state threads exactly), hashing every output {@code int16} as little-endian bytes.
 * The encode side drives a fresh {@link MlowEncoder} over a deterministic synthetic fixture (a fixed
 * two-sine mix plus a fixed-seed LCG dither, no clock and no {@link Math#random()}), hashing each packet's
 * bytes together with the range coder's {@link MlowEncoder#lastFinalRange()} register. Both hashes are
 * compared to a golden hex constant.
 *
 * <p>Both golden constants start as the sentinel {@value #CAPTURE_SENTINEL}; while a constant holds the
 * sentinel the matching test prints its computed digest ({@code MLOW_DECODE_SHA256=...} /
 * {@code MLOW_ENCODE_SHA256=...}) and passes without asserting, so the maintainer can capture the goldens
 * once on the current tree, paste them back into {@link #EXPECTED_DECODE} / {@link #EXPECTED_ENCODE}, and
 * lock the oracle in. The fixture uses {@link StrictMath} so its input is reproducible across platforms.
 */
@DisplayName("MLow codec bit-identity oracle")
class MlowBitIdentityTest {
    /**
     * Sentinel value a golden constant holds until the real digest is captured and pasted in; while a
     * constant equals this, the matching test prints its digest and passes instead of asserting.
     */
    private static final String CAPTURE_SENTINEL = "CAPTURE";

    /**
     * Golden SHA-256 (lowercase hex) of the decoded PCM over the canonical parity corpus; {@value
     * #CAPTURE_SENTINEL} until captured.
     */
    private static final String EXPECTED_DECODE = "94d028eb2c4539009c8fd2f81e4bf6424cb51dc97f8acf365880300e85c236de";

    /**
     * Golden SHA-256 (lowercase hex) of the encoded packet bytes plus per-packet final-range registers over
     * the synthetic fixture; {@value #CAPTURE_SENTINEL} until captured.
     */
    private static final String EXPECTED_ENCODE = "0a89f74602dcf2c8136d6c59516cb0c1e73989977b964ac49b6f4b54617c8ff6";

    /**
     * The canonical MLow decode parity captures, one continuous decoder stream each; a fresh decoder replays
     * each file in order to thread the cross-packet CELP state.
     */
    private static final String[] DECODE_CAPTURES = {"parity-3.json", "parity-4.json"};

    /**
     * Fixed samples per encoded fixture frame, one 60 ms MLow packet at 16 kHz mono; {@link MlowEncoder}'s
     * {@code encode} accepts whole multiples of its 320-sample (20 ms) internal frame, and a 60 ms packet is
     * three of them, matching the 960 samples {@link MLowAudioDecoder} yields per 60 ms packet.
     */
    private static final int FIXTURE_FRAME_SAMPLES = 960;

    /**
     * Number of fixture frames encoded into the oracle.
     */
    private static final int FIXTURE_FRAMES = 200;

    /**
     * Fixture sample rate in hertz, the MLow low-band scope.
     */
    private static final int FIXTURE_SAMPLE_RATE = 16_000;

    /**
     * First fixture tone frequency in hertz.
     */
    private static final double TONE_A_HZ = 440.0;

    /**
     * First fixture tone amplitude in {@code int16} units.
     */
    private static final double TONE_A_AMPLITUDE = 8000.0;

    /**
     * Second fixture tone frequency in hertz.
     */
    private static final double TONE_B_HZ = 1000.0;

    /**
     * Second fixture tone amplitude in {@code int16} units.
     */
    private static final double TONE_B_AMPLITUDE = 6000.0;

    /**
     * Fixed seed of the fixture dither LCG.
     */
    private static final long LCG_SEED = 0x9E3779B97F4A7C15L;

    /**
     * Multiplier of the fixture dither LCG (the SplitMix64 / PCG constant).
     */
    private static final long LCG_MULTIPLIER = 6364136223846793005L;

    /**
     * Increment of the fixture dither LCG.
     */
    private static final long LCG_INCREMENT = 1442695040888963407L;

    @Test
    @DisplayName("decoded PCM over the parity corpus is bit-identical to the golden digest")
    void decodeBitIdentity() {
        var digest = newSha256();
        for (var capture : DECODE_CAPTURES) {
            var corpus = MlowDecodeCorpus.loadResource(capture);
            var decoder = new MLowAudioDecoder(16_000, 1);
            for (var pair : corpus.pairs()) {
                var pcm = decoder.decode(pair.encoded(), 960, false);
                for (var sample : pcm) {
                    updateLittleEndianShort(digest, sample);
                }
            }
        }
        verifyDigest("DECODE", EXPECTED_DECODE, toHex(digest.digest()));
    }

    @Test
    @DisplayName("encoded packet bytes over the synthetic fixture are bit-identical to the golden digest")
    void encodeBitIdentity() {
        var digest = newSha256();
        var encoder = new MlowEncoder();
        var lcg = LCG_SEED;
        var sampleIndex = 0L;
        for (var frame = 0; frame < FIXTURE_FRAMES; frame++) {
            var pcm = new short[FIXTURE_FRAME_SAMPLES];
            for (var i = 0; i < FIXTURE_FRAME_SAMPLES; i++) {
                var t = sampleIndex / (double) FIXTURE_SAMPLE_RATE;
                var mix = TONE_A_AMPLITUDE * StrictMath.sin(2.0 * StrictMath.PI * TONE_A_HZ * t)
                        + TONE_B_AMPLITUDE * StrictMath.sin(2.0 * StrictMath.PI * TONE_B_HZ * t);
                lcg = lcg * LCG_MULTIPLIER + LCG_INCREMENT;
                var dither = (int) ((lcg >>> 40) & 0x7F) - 64;
                pcm[i] = clampToShort((long) StrictMath.rint(mix) + dither);
                sampleIndex++;
            }
            var encoded = encoder.encode(pcm);
            digest.update(encoded);
            updateLittleEndianLong(digest, encoder.lastFinalRange());
        }
        verifyDigest("ENCODE", EXPECTED_ENCODE, toHex(digest.digest()));
    }

    /**
     * Compares a computed digest to its golden constant, or prints it in capture mode.
     *
     * <p>When {@code expected} is the {@value #CAPTURE_SENTINEL} sentinel this prints the captured digest as
     * {@code MLOW_<key>_SHA256=<hex>} and returns (the test passes), so the maintainer can lift the golden
     * from a single run on the current tree; otherwise it asserts byte-for-byte equality.
     *
     * @param key       the capture key, {@code DECODE} or {@code ENCODE}
     * @param expected  the golden constant, possibly the sentinel
     * @param hexDigest the freshly computed lowercase-hex digest
     */
    private static void verifyDigest(String key, String expected, String hexDigest) {
        if (CAPTURE_SENTINEL.equals(expected)) {
            System.out.println("MLOW_" + key + "_SHA256=" + hexDigest);
            return;
        }
        assertEquals(expected, hexDigest, key + " output changed - NOT bit-identical to golden");
    }

    /**
     * Creates a fresh SHA-256 message digest.
     *
     * @return a new SHA-256 {@link MessageDigest}
     */
    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * Feeds one {@code int16} sample into the digest as two little-endian bytes.
     *
     * @param digest the digest to update
     * @param value  the sample to absorb
     */
    private static void updateLittleEndianShort(MessageDigest digest, short value) {
        digest.update((byte) (value & 0xFF));
        digest.update((byte) ((value >>> 8) & 0xFF));
    }

    /**
     * Feeds one 64-bit value into the digest as eight little-endian bytes.
     *
     * @param digest the digest to update
     * @param value  the value to absorb
     */
    private static void updateLittleEndianLong(MessageDigest digest, long value) {
        var remaining = value;
        for (var i = 0; i < 8; i++) {
            digest.update((byte) (remaining & 0xFF));
            remaining >>>= 8;
        }
    }

    /**
     * Clamps a value to the signed 16-bit range.
     *
     * @param value the value to clamp
     * @return {@code value} saturated to {@code [Short.MIN_VALUE, Short.MAX_VALUE]}
     */
    private static short clampToShort(long value) {
        if (value > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (value < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) value;
    }

    /**
     * Renders a byte array as lowercase hexadecimal.
     *
     * @param bytes the bytes to render
     * @return the lowercase-hex string, two characters per byte
     */
    private static String toHex(byte[] bytes) {
        var builder = new StringBuilder(bytes.length * 2);
        for (var b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }
}
