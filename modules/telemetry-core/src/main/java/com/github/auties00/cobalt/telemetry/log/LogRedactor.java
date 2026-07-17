package com.github.auties00.cobalt.telemetry.log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * The keyed fingerprint engine that masks every sensitive value Cobalt logs.
 *
 * <p>Fingerprinting is the single primitive the whole redaction layer is built on: it turns arbitrary bytes
 * into a short, non-reversible token that is stable within a run, so a reader can follow one value across many
 * records without the value itself ever being written. Each {@link LogRedactable} implementation calls it to
 * render its own masked form, and nothing else does: this class is package-private precisely so that redaction
 * cannot be bypassed or reinvented at a call site. Code outside this package that holds a sensitive value
 * redacts it by wrapping it in the matching {@link LogRedactable} record, never by hashing it itself.
 */
final class LogRedactor {
    /**
     * The number of leading digest bytes rendered as the hexadecimal fingerprint, giving a six-character token
     * that is short enough to read yet wide enough (2^24 values) to make accidental collisions rare in a run.
     */
    private static final int FINGERPRINT_BYTES = 3;

    /**
     * A strong {@link SecureRandom} held statically, so the per-process fingerprint salt draws from a single
     * well-seeded source obtained once rather than from a freshly constructed generator.
     */
    private static final SecureRandom SECURE_RANDOM;

    static {
        try {
            SECURE_RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("A strong SecureRandom is required but unavailable", exception);
        }
    }

    /**
     * The keyed-hash salt mixed into every fingerprint; a fixed value from {@link Log#SALT_PROPERTY} when set,
     * otherwise a per-process random value.
     */
    private static final byte[] SALT = computeSalt();

    /**
     * Prevents instantiation of this static-only holder.
     *
     * @throws AssertionError always, since the class is never meant to be instantiated
     */
    private LogRedactor() {
        throw new AssertionError("No instances");
    }

    /**
     * Computes the fingerprint of {@code data} as the first {@link #FINGERPRINT_BYTES} bytes of
     * {@code SHA-256(salt || data)}, rendered as lowercase hexadecimal.
     *
     * <p>The keyed fingerprint is deterministic within a run (the same bytes yield the same token) and distinct
     * across inputs (distinct bytes yield distinct tokens), so a reader can correlate occurrences across log
     * lines without ever seeing the real value. The per-process salt (see {@link Log#SALT_PROPERTY}) blocks the
     * offline reversal of low-entropy secrets such as phone numbers.
     *
     * @param data the bytes to fingerprint; must not be {@code null}
     * @return the six-character hexadecimal fingerprint
     * @throws AssertionError if the platform lacks the {@code SHA-256} digest, which every conformant JRE
     *                        provides
     */
    static String fingerprint(byte[] data) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(SALT);
            digest.update(data);
            return HexFormat.of().formatHex(digest.digest(), 0, FINGERPRINT_BYTES);
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required but unavailable", exception);
        }
    }

    /**
     * Computes the fingerprint salt from {@link Log#SALT_PROPERTY} when set, otherwise generates a fresh
     * 16-byte per-process random salt.
     *
     * @return the salt bytes
     */
    private static byte[] computeSalt() {
        var configured = System.getProperty(Log.SALT_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return configured.getBytes(StandardCharsets.UTF_8);
        }
        var salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }
}
