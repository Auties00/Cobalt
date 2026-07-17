package com.github.auties00.cobalt.wam.synthetic;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Holds the shared random-generation, timer-wrapping, session-id and hashing
 * helpers used across the {@code wam.synthetic} anti-detection telemetry
 * classes.
 *
 * <p>The sixteen {@code Synthetic*Telemetry} classes each fabricate a plausible
 * WhatsApp Web WAM burst, and each historically carried its own private copies
 * of the same handful of helpers: an inclusive jitter offset, an inclusive
 * range draw, an inclusive integer count, a probability gate, a lowercase-hex
 * id minter, a millisecond-to-{@link Instant} timer wrapper, a UUID session-id
 * minter, and the two message-digest helpers. Consolidating them here removes
 * roughly forty duplicated private members and gives every synthetic class one
 * canonical, drop-in replacement for each helper so their fabricated values are
 * generated the same way.
 *
 * <p>The range and probability helpers draw from
 * {@link ThreadLocalRandom#current()} rather than the shared
 * {@code SecureRandom} behind
 * {@link com.github.auties00.cobalt.wire.core.util.DataUtils}. Synthetic telemetry emits a
 * high volume of unimportant fabricated numbers on every connect, and none of
 * those figures is a secret; routing them through the strong entropy source
 * would contend on it needlessly, so this holder keeps them on the fast
 * per-thread generator. The two hashing helpers exist because
 * {@code DataUtils} exposes no digest primitive, and {@link #randomHexLower(int)}
 * exists because {@link com.github.auties00.cobalt.wire.core.util.DataUtils#randomHex(int)}
 * emits <em>uppercase</em> hex, whereas the ids WhatsApp Web mints for these
 * surfaces are lowercase.
 *
 * <p>This holder intentionally offers no plain exclusive-bound integer or long
 * range wrapper: where a caller needs a plain {@code [0, bound)} or
 * {@code [min, max)} draw, it should call
 * {@link com.github.auties00.cobalt.wire.core.util.DataUtils#randomInt(int, int)} or
 * {@link com.github.auties00.cobalt.wire.core.util.DataUtils#randomLong(long, long)}
 * directly rather than add a wrapper here. The helpers below cover only the
 * inclusive-bound and hash-shaped idioms {@code DataUtils} does not already
 * express.
 *
 * @implNote
 * This implementation uses {@link ThreadLocalRandom} for every random draw so
 * the high-volume synthetic path never touches the shared strong
 * {@code SecureRandom}; the fabricated figures are cosmetic anti-fingerprinting
 * noise, not cryptographic material, so the weaker but contention-free source is
 * the correct choice.
 *
 * @see com.github.auties00.cobalt.wire.core.util.DataUtils
 */
public final class SyntheticTelemetryUtils {
    /**
     * Prevents instantiation of this static-helper holder.
     *
     * @throws AssertionError always
     */
    private SyntheticTelemetryUtils() {
        throw new AssertionError();
    }

    /**
     * Returns {@code base} increased by a random non-negative offset of at most
     * {@code spread}.
     *
     * <p>The offset is drawn uniformly from the inclusive range
     * {@code [0, spread]}, so the result lands in the inclusive range
     * {@code [base, base + spread]}. Jittering a fabricated count or timing this
     * way keeps successive sessions from reporting a byte-identical value, which
     * would itself be a distinguishing fingerprint, while keeping each value in a
     * plausible band around its base.
     *
     * @param base   the lower bound of the returned value
     * @param spread the inclusive width of the random offset added to
     *               {@code base}
     * @return {@code base} plus a random offset in the inclusive range
     *         {@code [0, spread]}
     */
    public static long jitter(long base, long spread) {
        return base + ThreadLocalRandom.current().nextLong(spread + 1);
    }

    /**
     * Returns a uniformly random {@code long} in the inclusive range
     * {@code [min, max]}.
     *
     * <p>Unlike {@link com.github.auties00.cobalt.wire.core.util.DataUtils#randomLong(long, long)},
     * whose upper bound is exclusive, this helper includes {@code max} so callers
     * can express a closed interval directly.
     *
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound, not less than {@code min}
     * @return a random {@code long} in the inclusive range {@code [min, max]}
     */
    public static long between(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /**
     * Returns a uniformly random {@code int} in the inclusive range
     * {@code [lo, hi]}.
     *
     * <p>Unlike {@link com.github.auties00.cobalt.wire.core.util.DataUtils#randomInt(int, int)},
     * whose upper bound is exclusive, this helper includes {@code hi} so callers
     * can express a closed count interval directly.
     *
     * @param lo the inclusive lower bound
     * @param hi the inclusive upper bound, not less than {@code lo}
     * @return a random {@code int} in the inclusive range {@code [lo, hi]}
     */
    public static int count(int lo, int hi) {
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    /**
     * Returns {@code true} with the given percentage probability.
     *
     * <p>This unifies the two probability-gate idioms the synthetic classes used:
     * a percentage gate (return {@code true} with probability {@code percent / 100})
     * and a reciprocal gate (return {@code true} with probability {@code 1 / n}).
     * The unified helper is percentage-based, so a former reciprocal gate of one
     * in {@code n} maps to {@code chance(100 / n)}. Values at or below zero never
     * open the gate and values at or above one hundred always open it.
     *
     * @param percent the percentage chance of returning {@code true}, nominally in
     *                the inclusive range zero to one hundred
     * @return {@code true} with probability {@code percent / 100}
     */
    public static boolean chance(int percent) {
        return ThreadLocalRandom.current().nextInt(100) < percent;
    }

    /**
     * Returns a fresh random lowercase hexadecimal string encoding the given
     * number of random bytes.
     *
     * <p>The output is {@code 2 * byteCount} characters long. This mirrors the
     * per-interaction correlation ids WhatsApp Web mints (page-load ids,
     * offline-session ids, opaque digests), which are lowercase.
     *
     * <p>This helper exists rather than delegating to
     * {@link com.github.auties00.cobalt.wire.core.util.DataUtils#randomHex(int)} because that
     * method emits uppercase hex (matching WhatsApp's {@code WAHex.toHex}), whereas
     * these synthetic surfaces mint their ids in lowercase; the two are not
     * interchangeable on the wire.
     *
     * @param byteCount the number of random bytes to encode; the returned string
     *                  is twice this many characters long
     * @return the lowercase hexadecimal encoding of {@code byteCount} freshly
     *         sampled random bytes
     */
    public static String randomHexLower(int byteCount) {
        var buffer = new byte[byteCount];
        ThreadLocalRandom.current().nextBytes(buffer);
        return HexFormat.of().formatHex(buffer);
    }

    /**
     * Wraps an elapsed duration in milliseconds as the {@link Instant} a WAM timer
     * field expects.
     *
     * <p>The WAM timer wire type encodes an elapsed duration, and the generated
     * event builders model it as an {@link Instant} whose epoch-millisecond value
     * is the duration, so a timer of {@code n} milliseconds is
     * {@link Instant#ofEpochMilli(long) Instant.ofEpochMilli(n)}.
     *
     * @param millis the elapsed duration in milliseconds
     * @return the {@link Instant} carrying that duration
     */
    public static Instant timer(long millis) {
        return Instant.ofEpochMilli(millis);
    }

    /**
     * Mints a fresh session identifier in the canonical UUID string form.
     *
     * <p>Each call returns a distinct {@link UUID#randomUUID()} string, matching
     * the per-interaction session, thread-session and unified-session ids
     * WhatsApp Web mints as UUIDs; returning a fresh value per call keeps
     * successive sessions from reusing one id and thereby fingerprinting the
     * client.
     *
     * @return a newly generated random UUID string
     */
    public static String newSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Computes the lowercase hexadecimal {@code MD5} digest of the given input.
     *
     * <p>WhatsApp Web derives some anti-abuse fingerprints (for example the canvas
     * fingerprint) as the {@code MD5} of a rendered data URL; hashing a stable seed
     * the same way produces a thirty-two character hex value of the same shape.
     *
     * @param input the string whose UTF-8 bytes are hashed
     * @return the thirty-two character lowercase hex {@code MD5} digest, or a
     *         random lowercase hex string of the same length when the {@code MD5}
     *         algorithm is unavailable
     */
    public static String md5Hex(String input) {
        try {
            var digest = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException _) {
            return randomHexLower(16);
        }
    }

    /**
     * Computes the lowercase hexadecimal {@code SHA-256} digest of the given
     * input.
     *
     * <p>This produces the sixty-four character digest shape WhatsApp Web logs for
     * its HMAC-derived thread and message identifiers.
     *
     * @param input the string whose UTF-8 bytes are hashed
     * @return the sixty-four character lowercase hex {@code SHA-256} digest, or a
     *         random lowercase hex string of the same length when the
     *         {@code SHA-256} algorithm is unavailable
     */
    public static String sha256HexLower(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException _) {
            return randomHexLower(32);
        }
    }
}
