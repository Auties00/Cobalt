package com.github.auties00.cobalt.calls.media.sframe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavioural tests for the SFrame media transform: seal/open round-trip, the corrected
 * {@code [keyId][counter][len]} trailer order with the trailer authenticated as associated data, and
 * the {@code 0x800}-slot replay-window accept/reject matrix.
 *
 * <p>The trailer order is the single most important SFrame correction (SPEC 11.1): the superseded
 * {@code call/rtp/sframe} wrote {@code [counter][keyId]}, the live WASM writes {@code [keyId][counter]}.
 * These tests pin the corrected order by reading the trailer back byte-by-byte and by tampering each
 * authenticated region. The crypto primitives are exercised end-to-end through {@link SFrameSecureFrame}
 * with a chain key installed in an {@link SFrameKeyProvider}; the per-frame counter/replay logic is also
 * exercised directly against {@link SFrameReplayWindow}.
 */
public class SFrameRoundTripTest {
    private static byte[] chainKey() {
        var key = new byte[SFrameKeyProvider.CHAIN_KEY_LENGTH];
        for (var i = 0; i < key.length; i++) {
            key[i] = (byte) (i * 7 + 3);
        }
        return key;
    }

    private static SFrameKeyProvider providerWithChainKey() {
        var provider = new SFrameKeyProvider();
        provider.setChainKey(chainKey(), 1L);
        return provider;
    }

    @Nested
    @DisplayName("seal then open")
    class SealOpen {
        @Test
        @DisplayName("a sealed frame opens back to the original plaintext")
        public void roundTrips() {
            var sender = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var receiver = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var plaintext = "the quick brown fox".getBytes(StandardCharsets.UTF_8);
            var frame = sender.seal(plaintext);
            var opened = receiver.open(frame);
            assertArrayEquals(plaintext, opened);
        }

        @Test
        @DisplayName("the framed bytes are longer than the plaintext by the tag and trailer overhead")
        public void overhead() {
            var sender = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var plaintext = new byte[64];
            var frame = sender.seal(plaintext);
            // ciphertext(64) + tag(4, suite 3) + trailer(keyId 1B + counter 1B + len 1B = 3).
            assertEquals(64 + SFrameCipherSuite.defaultSuite().tagLength() + 3, frame.length);
        }

        @Test
        @DisplayName("opening with no installed chain key yields null and counts a missing key")
        public void missingKey() {
            var sender = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var frame = sender.seal(new byte[8]);
            var receiver = new SFrameSecureFrame(new SFrameKeyProvider(), 0L);
            assertNull(receiver.open(frame));
            assertEquals(1, receiver.stats().missingKeyFrames());
        }
    }

    @Nested
    @DisplayName("trailer [keyId][counter][len] (SPEC 11.1 corrected order)")
    class TrailerOrder {
        @Test
        @DisplayName("writeTrailer emits the key id first, the counter second, the length byte last")
        public void keyIdBeforeCounter() {
            // keyId 7 -> varint [07]; counter 300 (0x12C) -> varint [AC 02]; trailer len = 4.
            var trailer = SFrameHeaderCodec.writeTrailer(7L, 300L);
            assertArrayEquals(new byte[]{0x07, (byte) 0xAC, 0x02, 0x04}, trailer);
            // The last byte equals the trailer length, and the FIRST byte is the key id, not the counter.
            assertEquals(trailer.length, trailer[trailer.length - 1] & 0xFF);
            assertEquals(0x07, trailer[0] & 0xFF);
        }

        @Test
        @DisplayName("writeTrailer then readTrailer round-trips key id and counter")
        public void trailerRoundTrip() {
            var trailer = SFrameHeaderCodec.writeTrailer(42L, 1_000_000L);
            var len = SFrameHeaderCodec.readTrailerLength(trailer, trailer.length);
            assertEquals(trailer.length, len);
            var decoded = SFrameHeaderCodec.readTrailer(trailer, 0, len);
            assertNotNull(decoded);
            assertEquals(42L, decoded.keyId());
            assertEquals(1_000_000L, decoded.counter());
        }

        @Test
        @DisplayName("a sealed frame ends with the [keyId][counter][len] trailer matching the writer")
        public void sealedFrameTrailerLayout() {
            var sender = new SFrameSecureFrame(providerWithChainKey(), 9L);
            var frame = sender.seal(new byte[16]);
            var expectedTrailer = SFrameHeaderCodec.writeTrailer(9L, 1L); // first sealed frame uses counter 1
            var actualTrailer = Arrays.copyOfRange(frame, frame.length - expectedTrailer.length, frame.length);
            assertArrayEquals(expectedTrailer, actualTrailer);
            // The trailer's first byte is the key id (9), proving keyId-before-counter on the wire.
            assertEquals(9, actualTrailer[0] & 0xFF);
        }
    }

    @Nested
    @DisplayName("trailer is authenticated as associated data")
    class TrailerIsAad {
        @Test
        @DisplayName("flipping a trailer byte makes the frame fail authentication")
        public void tamperTrailer() {
            var sender = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var receiver = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var frame = sender.seal(new byte[24]);
            // The trailer is the last 3 bytes; corrupt the key id varint (first trailer byte).
            var trailerStart = frame.length - 3;
            frame[trailerStart] ^= 0x01;
            assertNull(receiver.open(frame), "a tampered trailer must not authenticate");
        }

        @Test
        @DisplayName("flipping a ciphertext byte makes the frame fail authentication")
        public void tamperCiphertext() {
            var sender = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var receiver = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var frame = sender.seal(new byte[24]);
            frame[0] ^= 0x01;
            assertNull(receiver.open(frame), "a tampered ciphertext must not authenticate");
        }

        @Test
        @DisplayName("the cipher authenticates over trailer || ciphertext, not ciphertext alone")
        public void cipherAuthCoversTrailer() {
            var cipher = new SFrameCipher(SFrameCipherSuite.defaultSuite(),
                    new byte[SFrameCipherSuite.AES_KEY_LENGTH], new byte[SFrameCipherSuite.HMAC_KEY_LENGTH]);
            var plaintext = new byte[20];
            var trailerA = SFrameHeaderCodec.writeTrailer(1L, 5L);
            var trailerB = SFrameHeaderCodec.writeTrailer(2L, 5L);
            var body = cipher.seal(trailerA, plaintext, 5L);
            // The body authenticated under trailerA must NOT open under a different trailer.
            assertNull(cipher.open(trailerB, body, 5L));
            assertArrayEquals(plaintext, cipher.open(trailerA, body, 5L));
        }
    }

    @Nested
    @DisplayName("replay window (0x800 slots; reject zero, replays, and too-old)")
    class ReplayMatrix {
        @Test
        @DisplayName("a fresh counter is accepted, the same counter replayed is rejected")
        public void freshThenReplay() {
            var window = new SFrameReplayWindow();
            assertTrue(window.isAcceptable(5L));
            window.accept(5L);
            assertFalse(window.isAcceptable(5L), "an already-accepted counter is a replay");
        }

        @Test
        @DisplayName("counter zero is always rejected")
        public void zeroRejected() {
            var window = new SFrameReplayWindow();
            assertFalse(window.isAcceptable(0L));
        }

        @Test
        @DisplayName("an out-of-order counter inside the window is accepted once")
        public void inWindowOutOfOrder() {
            var window = new SFrameReplayWindow();
            window.accept(100L);
            assertTrue(window.isAcceptable(40L), "40 is within [100-0x800, 100] and unseen");
            window.accept(40L);
            assertFalse(window.isAcceptable(40L), "40 replayed is rejected");
            assertTrue(window.isAcceptable(101L), "a newer counter is still accepted");
        }

        @Test
        @DisplayName("a counter older than the window edge is rejected as too-old")
        public void tooOldRejected() {
            var window = new SFrameReplayWindow();
            var high = SFrameReplayWindow.WINDOW_SIZE + 50L; // 0x832
            window.accept(high);
            // high - 1 is inside the window and unseen.
            assertTrue(window.isAcceptable(high - 1));
            // A counter at the bottom edge (distance == WINDOW_SIZE) has scrolled out: too old.
            assertFalse(window.isAcceptable(high - SFrameReplayWindow.WINDOW_SIZE),
                    "distance == WINDOW_SIZE is out of the window");
            assertFalse(window.isAcceptable(1L), "an ancient counter is too old");
        }

        @Test
        @DisplayName("sliding far ahead clears vacated slots so an old bit cannot linger")
        public void slideClearsVacated() {
            var window = new SFrameReplayWindow();
            // Accept a counter, then a second one sharing its slot (mod 0x800) so its bit is set too.
            window.accept(10L);
            window.accept(10L + SFrameReplayWindow.WINDOW_SIZE);
            // Jump well beyond the window: the whole bitmap rolls over and every vacated bit clears.
            var far = 10L + SFrameReplayWindow.WINDOW_SIZE * 3L;
            assertTrue(window.isAcceptable(far));
            window.accept(far);
            // A counter inside the new window that was never seen there is accepted (no lingering bit).
            assertTrue(window.isAcceptable(far - 5L));
        }

        @Test
        @DisplayName("the secure-frame open path rejects a replayed frame and counts a duplicate")
        public void secureFrameRejectsReplay() {
            var sender = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var receiver = new SFrameSecureFrame(providerWithChainKey(), 0L);
            var frame = sender.seal(new byte[12]);
            assertArrayEquals(new byte[12], receiver.open(frame));
            // Re-deliver the identical framed bytes: same counter => replay.
            assertNull(receiver.open(frame.clone()));
            assertEquals(1, receiver.stats().duplicateFrames());
        }
    }

    @Nested
    @DisplayName("malformed frames")
    class Malformed {
        @Test
        @DisplayName("a frame shorter than the minimum trailer is rejected")
        public void tooShort() {
            var receiver = new SFrameSecureFrame(providerWithChainKey(), 0L);
            assertNull(receiver.open(new byte[2]));
            assertEquals(1, receiver.stats().invalidParamFrames());
        }

        @Test
        @DisplayName("a trailer whose declared length overshoots the frame is rejected")
        public void badTrailerLength() {
            var frame = new byte[]{0x00, 0x00, (byte) 0xFF};
            assertEquals(-1, SFrameHeaderCodec.readTrailerLength(frame, frame.length));
        }
    }
}
