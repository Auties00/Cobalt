package com.github.auties00.cobalt.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link AesGcmStreamCipher} against three independent oracles: the canonical McGrew/Viega (now NIST
 * SP 800-38D) AES-GCM known-answer vectors, the JDK's own {@code AES/GCM/NoPadding} provider over a spread of
 * payload sizes and both AES key lengths, and the cipher's own one-shot output replayed through arbitrary
 * streaming chunkings. Authentication is exercised by flipping every byte of a sealed message and asserting the
 * tag rejects it. The random oracles use a fixed seed so a failure reproduces.
 */
@DisplayName("AesGcmStreamCipher")
class AesGcmStreamCipherTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final int TAG = AesGcmStreamCipher.TAG_LENGTH;

    private static byte[] seal(byte[] key, byte[] nonce, byte[] plaintext) throws GeneralSecurityException {
        var cipher = new AesGcmStreamCipher();
        cipher.init(true,new SecretKeySpec(key, "AES"), nonce);
        var out = new byte[plaintext.length + TAG];
        var n = cipher.update(plaintext, 0, plaintext.length, out, 0);
        n += cipher.doFinal(out, n);
        return Arrays.copyOf(out, n);
    }

    private static byte[] open(byte[] key, byte[] nonce, byte[] sealed, int maxChunk, Random rnd)
            throws GeneralSecurityException {
        var cipher = new AesGcmStreamCipher();
        cipher.init(false,new SecretKeySpec(key, "AES"), nonce);
        var out = new byte[Math.max(0, sealed.length - TAG)];
        var written = 0;
        var consumed = 0;
        while (consumed < sealed.length) {
            var chunk = Math.min(sealed.length - consumed, 1 + rnd.nextInt(maxChunk));
            written += cipher.update(sealed, consumed, chunk, out, written);
            consumed += chunk;
        }
        written += cipher.doFinal(out, written);
        return Arrays.copyOf(out, written);
    }

    private static byte[] jdkGcm(int mode, byte[] key, byte[] nonce, byte[] input) throws GeneralSecurityException {
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        return cipher.doFinal(input);
    }

    @Nested
    @DisplayName("Known answer vectors (empty AAD, 96-bit IV, 128-bit tag)")
    class KnownAnswers {
        private static Stream<Arguments> vectors() {
            return Stream.of(
                    Arguments.of("00000000000000000000000000000000",
                            "000000000000000000000000",
                            "",
                            "58e2fccefa7e3061367f1d57a4e7455a"),
                    Arguments.of("00000000000000000000000000000000",
                            "000000000000000000000000",
                            "00000000000000000000000000000000",
                            "0388dace60b6a392f328c2b971b2fe78ab6e47d42cec13bdf53a67b21257bddf"),
                    Arguments.of("feffe9928665731c6d6a8f9467308308",
                            "cafebabefacedbaddecaf888",
                            "d9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72"
                                    + "1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b391aafd255",
                            "42831ec2217774244b7221b784d0d49ce3aa212f2c02a4e035c17e2329aca12e"
                                    + "21d514b25466931c7d8f6a5aac84aa051ba30b396a0aac973d58e091473f5985"
                                    + "4d5c2af327cd64a62cf35abd2ba6fab4"));
        }

        @ParameterizedTest(name = "{2} -> {3}")
        @MethodSource("vectors")
        @DisplayName("seals to the published ciphertext and tag")
        void seals(String keyHex, String nonceHex, String plaintextHex, String sealedHex) throws Exception {
            var sealed = seal(HEX.parseHex(keyHex), HEX.parseHex(nonceHex), HEX.parseHex(plaintextHex));
            assertEquals(sealedHex, HEX.formatHex(sealed));
        }

        @ParameterizedTest(name = "{3} -> {2}")
        @MethodSource("vectors")
        @DisplayName("opens the published ciphertext back to the plaintext")
        void opens(String keyHex, String nonceHex, String plaintextHex, String sealedHex) throws Exception {
            var plaintext = open(HEX.parseHex(keyHex), HEX.parseHex(nonceHex), HEX.parseHex(sealedHex), 1, new Random(1));
            assertEquals(plaintextHex, HEX.formatHex(plaintext));
        }
    }

    @Nested
    @DisplayName("Cross-check against the JDK AES/GCM provider")
    class JdkParity {
        @ParameterizedTest(name = "{0}-byte payloads")
        @ValueSource(ints = {0, 1, 15, 16, 17, 31, 32, 33, 255, 256, 1024, 8191, 8192, 8193, 50000})
        @DisplayName("produces byte-identical ciphertext and round-trips for AES-128 and AES-256")
        void matchesProviderForSize(int size) throws Exception {
            var rnd = new Random(0x6CB01 + size);
            for (var iter = 0; iter < 64; iter++) {
                var key = new byte[(iter & 1) == 0 ? 16 : 32];
                rnd.nextBytes(key);
                var nonce = new byte[AesGcmStreamCipher.NONCE_LENGTH];
                rnd.nextBytes(nonce);
                var plaintext = new byte[size];
                rnd.nextBytes(plaintext);

                var mine = seal(key, nonce, plaintext);
                var reference = jdkGcm(Cipher.ENCRYPT_MODE, key, nonce, plaintext);
                assertArrayEquals(reference, mine, "ciphertext diverged from the JDK provider at size " + size);

                var roundTrip = open(key, nonce, reference, 64, rnd);
                assertArrayEquals(plaintext, roundTrip, "round-trip diverged at size " + size);
            }
        }
    }

    @Nested
    @DisplayName("Streaming")
    class Streaming {
        @Test
        @DisplayName("encryption output is independent of how the plaintext is chunked")
        void encryptChunkInvariant() throws Exception {
            var rnd = new Random(99);
            for (var iter = 0; iter < 500; iter++) {
                var size = rnd.nextInt(5000);
                var key = new byte[16];
                rnd.nextBytes(key);
                var nonce = new byte[12];
                rnd.nextBytes(nonce);
                var plaintext = new byte[size];
                rnd.nextBytes(plaintext);

                var oneShot = seal(key, nonce, plaintext);

                var cipher = new AesGcmStreamCipher();
                cipher.init(true,new SecretKeySpec(key, "AES"), nonce);
                var streamed = new byte[size + TAG];
                var written = 0;
                var fed = 0;
                while (fed < size) {
                    var chunk = Math.min(size - fed, 1 + rnd.nextInt(48));
                    written += cipher.update(plaintext, fed, chunk, streamed, written);
                    fed += chunk;
                }
                written += cipher.doFinal(streamed, written);
                assertEquals(size + TAG, written);
                assertArrayEquals(oneShot, streamed);
            }
        }

        @Test
        @DisplayName("decryption holds back exactly the tag and works one byte at a time")
        void decryptByteAtATime() throws Exception {
            var rnd = new Random(7);
            var key = new byte[16];
            rnd.nextBytes(key);
            var nonce = new byte[12];
            rnd.nextBytes(nonce);
            var plaintext = new byte[321];
            rnd.nextBytes(plaintext);
            var sealed = seal(key, nonce, plaintext);

            var roundTrip = open(key, nonce, sealed, 1, new Random(0));
            assertArrayEquals(plaintext, roundTrip);
        }
    }

    @Nested
    @DisplayName("Authentication")
    class Authentication {
        @Test
        @DisplayName("rejects a single-bit flip at any position with AEADBadTagException")
        void rejectsTampering() throws Exception {
            var rnd = new Random(123);
            var key = new byte[16];
            rnd.nextBytes(key);
            var nonce = new byte[12];
            rnd.nextBytes(nonce);
            var plaintext = new byte[200];
            rnd.nextBytes(plaintext);
            var sealed = seal(key, nonce, plaintext);

            for (var position = 0; position < sealed.length; position++) {
                var tampered = sealed.clone();
                tampered[position] ^= 0x01;
                var index = position;
                assertThrows(AEADBadTagException.class,
                        () -> open(key, nonce, tampered, 16, new Random(index)),
                        "tamper at byte " + position + " was not detected");
            }
        }

        @Test
        @DisplayName("rejects a wrong key with AEADBadTagException")
        void rejectsWrongKey() throws Exception {
            var rnd = new Random(456);
            var key = new byte[16];
            rnd.nextBytes(key);
            var nonce = new byte[12];
            rnd.nextBytes(nonce);
            var plaintext = new byte[64];
            rnd.nextBytes(plaintext);
            var sealed = seal(key, nonce, plaintext);

            var wrongKey = key.clone();
            wrongKey[0] ^= 0x80;
            assertThrows(AEADBadTagException.class, () -> open(wrongKey, nonce, sealed, 16, new Random(1)));
        }
    }

    @Nested
    @DisplayName("Input validation")
    class Validation {
        @Test
        @DisplayName("rejects a nonce that is not 12 bytes")
        void rejectsBadNonce() throws Exception {
            var cipher = new AesGcmStreamCipher();
            var key = new SecretKeySpec(new byte[16], "AES");
            assertThrows(IllegalArgumentException.class, () -> cipher.init(true,key, new byte[11]));
            assertThrows(IllegalArgumentException.class, () -> cipher.init(false,key, new byte[16]));
        }

        @Test
        @DisplayName("the same instance re-keys cleanly across messages")
        void reInitialises() throws Exception {
            var rnd = new Random(2024);
            var cipher = new AesGcmStreamCipher();
            for (var iter = 0; iter < 5; iter++) {
                var key = new byte[16];
                rnd.nextBytes(key);
                var nonce = new byte[12];
                rnd.nextBytes(nonce);
                var plaintext = new byte[1 + rnd.nextInt(500)];
                rnd.nextBytes(plaintext);

                cipher.init(true,new SecretKeySpec(key, "AES"), nonce);
                var out = new byte[plaintext.length + TAG];
                var n = cipher.update(plaintext, 0, plaintext.length, out, 0);
                n += cipher.doFinal(out, n);
                assertArrayEquals(jdkGcm(Cipher.ENCRYPT_MODE, key, nonce, plaintext), Arrays.copyOf(out, n));
            }
        }

        @Test
        @DisplayName("a too-small output buffer raises ShortBufferException")
        void rejectsShortBuffer() throws Exception {
            var cipher = new AesGcmStreamCipher();
            cipher.init(true,new SecretKeySpec(new byte[16], "AES"), new byte[12]);
            var tooSmall = new byte[4];
            assertThrows(javax.crypto.ShortBufferException.class,
                    () -> cipher.update(new byte[8], 0, 8, tooSmall, 0));
        }
    }
}
