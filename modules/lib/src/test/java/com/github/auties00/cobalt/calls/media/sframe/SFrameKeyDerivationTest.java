package com.github.auties00.cobalt.calls.media.sframe;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.github.auties00.cobalt.calls.crypto.CallE2eKeyDerivation;

/**
 * Known-answer tests for {@link SFrameKeyDerivation}, pinning the per-participant SFrame base-key
 * schedule to live WhatsApp VoIP WASM memory.
 *
 * <p>The vector was read from live memory paused at {@code derive_sframe_key} (fn11063) during the
 * group call {@code 008B72960DAE8E158B0D2201B9A6F98A}
 * ({@code re/calls2-spec/captures/group-sframe-frame.json}). It fixes the HKDF salt/ikm split (salt is
 * the first 16 bytes of the call key, ikm the second 16), the {@code "e2e sframe key"} info label, the
 * device-JID context appended with no separator and no {@code NUL}, and the 32-byte output length. The
 * negative controls reproduce the capture's {@code resolved_details}: the swapped salt/ikm variant and
 * the {@code +NUL}-separator variant must both diverge from the live key.
 */
public class SFrameKeyDerivationTest {
    private static final HexFormat HEX = HexFormat.of();

    // group-sframe-frame.json capturedKeyDerivation.sample1_self
    private static final byte[] SECRET =
            HEX.parseHex("86e0004078464597d59c751fde9a8b61908dcbd04197ffdc7636582be7f439aa");
    private static final String PARTICIPANT_JID = "83116928594056:2@lid";
    private static final byte[] EXPECTED_BASE_KEY =
            HEX.parseHex("409102bf2c1a3816c76a6d64819d0c901556e030d5f33da251c13cdfcf0b9353");

    @Nested
    @DisplayName("live KAT (group call 008B72960DAE8E158B0D2201B9A6F98A, paused at derive_sframe_key)")
    class LiveKat {
        @Test
        @DisplayName("derives the live-captured base key byte-for-byte")
        public void matchesLiveCapture() {
            assertArrayEquals(EXPECTED_BASE_KEY, SFrameKeyDerivation.deriveBaseKey(SECRET, PARTICIPANT_JID));
        }

        @Test
        @DisplayName("yields a different base key for a different participant JID from the same secret")
        public void perParticipant() {
            // sample2_peerParticipant: same secret, different JID => unique base key per participant
            var other = SFrameKeyDerivation.deriveBaseKey(SECRET, "258252122116273:2@lid");
            assertFalse(Arrays.equals(EXPECTED_BASE_KEY, other), "different JID must derive a different key");
        }

        @Test
        @DisplayName("produces a 32-byte base key")
        public void length() {
            assertEquals(SFrameKeyDerivation.BASE_KEY_LENGTH,
                    SFrameKeyDerivation.deriveBaseKey(SECRET, PARTICIPANT_JID).length);
        }

        @Test
        @DisplayName("agrees with the equivalent participant-package derivation on the same vector")
        public void agreesWithParticipantDeriver() {
            // CallE2eKeyDerivation.deriveSframeBaseKey(callKey, Jid) is the same schedule keyed
            // off a JID; both must hit the identical live KAT.
            var typed = com.github.auties00.cobalt.calls.crypto.CallE2eKeyDerivation
                    .deriveSframeBaseKey(SECRET,
                            com.github.auties00.cobalt.wire.core.jid.Jid.of(PARTICIPANT_JID));
            assertArrayEquals(EXPECTED_BASE_KEY, typed);
            assertArrayEquals(typed, SFrameKeyDerivation.deriveBaseKey(SECRET, PARTICIPANT_JID));
        }
    }

    @Nested
    @DisplayName("negative controls (capture resolved_details: each wrong schedule must NOT match)")
    class NegativeControls {
        @Test
        @DisplayName("swapping the salt and ikm halves does NOT reproduce the live key")
        public void swappedSaltIkm() {
            var salt = Arrays.copyOfRange(SECRET, 0, 16);
            var ikm = Arrays.copyOfRange(SECRET, 16, 32);
            var info = infoLabelThenJid(PARTICIPANT_JID, false);
            // Wrong order: ikm = secret[0:16], salt = secret[16:32] (the variant the capture rejected).
            var swapped = VoipCryptoNative.hkdfSha256(salt, ikm, info, 32);
            assertFalse(Arrays.equals(EXPECTED_BASE_KEY, swapped), "swapped salt/ikm must not match");
        }

        @Test
        @DisplayName("inserting a NUL between the label and the JID does NOT reproduce the live key")
        public void nulSeparator() {
            var salt = Arrays.copyOfRange(SECRET, 0, 16);
            var ikm = Arrays.copyOfRange(SECRET, 16, 32);
            var info = infoLabelThenJid(PARTICIPANT_JID, true);
            var withNul = VoipCryptoNative.hkdfSha256(ikm, salt, info, 32);
            assertFalse(Arrays.equals(EXPECTED_BASE_KEY, withNul), "label+NUL+JID must not match");
        }

        @Test
        @DisplayName("zero-padding the JID context to 80 bytes does NOT reproduce the live key")
        public void paddedContext() {
            // SPEC 11.4 cites a 0x50-byte JID salt; the live capture confirms the context is the raw JID
            // (strnlen-bounded, NOT zero-padded to 80). A padded-to-80 info must therefore diverge.
            var salt = Arrays.copyOfRange(SECRET, 0, 16);
            var ikm = Arrays.copyOfRange(SECRET, 16, 32);
            var label = "e2e sframe key".getBytes(StandardCharsets.US_ASCII);
            var jid = PARTICIPANT_JID.getBytes(StandardCharsets.US_ASCII);
            var info = new byte[label.length + 0x50];
            System.arraycopy(label, 0, info, 0, label.length);
            System.arraycopy(jid, 0, info, label.length, jid.length);
            var padded = VoipCryptoNative.hkdfSha256(ikm, salt, info, 32);
            assertFalse(Arrays.equals(EXPECTED_BASE_KEY, padded), "label+JID padded to 0x50 must not match");
        }

        private static byte[] infoLabelThenJid(String jid, boolean withNul) {
            var label = "e2e sframe key".getBytes(StandardCharsets.US_ASCII);
            var jidBytes = jid.getBytes(StandardCharsets.US_ASCII);
            var sep = withNul ? 1 : 0;
            var info = new byte[label.length + sep + jidBytes.length];
            System.arraycopy(label, 0, info, 0, label.length);
            System.arraycopy(jidBytes, 0, info, label.length + sep, jidBytes.length);
            return info;
        }
    }

    @Nested
    @DisplayName("input validation")
    class Validation {
        @Test
        @DisplayName("rejects a call key that is not 32 bytes")
        public void wrongCallKeyLength() {
            assertThrows(IllegalArgumentException.class,
                    () -> SFrameKeyDerivation.deriveBaseKey(new byte[16], PARTICIPANT_JID));
            assertThrows(IllegalArgumentException.class,
                    () -> SFrameKeyDerivation.deriveBaseKey(new byte[33], PARTICIPANT_JID));
        }

        @Test
        @DisplayName("rejects null arguments")
        public void nullArgs() {
            assertThrows(NullPointerException.class,
                    () -> SFrameKeyDerivation.deriveBaseKey(null, PARTICIPANT_JID));
            assertThrows(NullPointerException.class,
                    () -> SFrameKeyDerivation.deriveBaseKey(SECRET, null));
        }
    }
}
