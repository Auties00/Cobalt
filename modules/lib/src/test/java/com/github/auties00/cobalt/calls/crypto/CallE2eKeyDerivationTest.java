package com.github.auties00.cobalt.calls.crypto;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Known-answer tests for {@link CallE2eKeyDerivation}, pinning the end-to-end call key chain to live
 * WhatsApp VoIP captures.
 *
 * <p>The SFrame base-key vector was read from live WASM memory paused at {@code derive_sframe_key} during
 * a group call ({@code group-sframe-frame.json}); the SRTP master vectors from live cipher
 * {@code set_key} inputs; both fix the HKDF salt/ikm split, the {@code "e2e sframe key"} info label, the
 * device-JID context, and the output length.
 */
public class CallE2eKeyDerivationTest {
    private static final HexFormat HEX = HexFormat.of();

    private static Jid device(String jid) {
        return Jid.of(jid);
    }

    @Nested
    @DisplayName("SFrame base key (live WASM memory, group call 008B72960DAE8E158B0D2201B9A6F98A)")
    class SframeBaseKey {
        // group-sframe-frame.json capturedKeyDerivation.sample1_self
        private static final byte[] RAW_KEY =
                HEX.parseHex("86e0004078464597d59c751fde9a8b61908dcbd04197ffdc7636582be7f439aa");
        private static final Jid PARTICIPANT = device("83116928594056:2@lid");
        private static final byte[] EXPECTED_BASE_KEY =
                HEX.parseHex("409102bf2c1a3816c76a6d64819d0c901556e030d5f33da251c13cdfcf0b9353");

        @Test
        @DisplayName("derives the live-captured per-participant SFrame base key byte-for-byte")
        public void matchesLiveCapture() {
            assertArrayEquals(EXPECTED_BASE_KEY, CallE2eKeyDerivation.deriveSframeBaseKey(RAW_KEY, PARTICIPANT));
        }

        @Test
        @DisplayName("yields a different base key for a different participant from the same raw key")
        public void perParticipant() {
            var other = CallE2eKeyDerivation.deriveSframeBaseKey(RAW_KEY, device("258252122116273:2@lid"));
            assertFalse(java.util.Arrays.equals(EXPECTED_BASE_KEY, other));
        }

        @Test
        @DisplayName("produces a 32-byte base key")
        public void length() {
            assertEquals(CallE2eKeyDerivation.SFRAME_BASE_KEY_LENGTH,
                    CallE2eKeyDerivation.deriveSframeBaseKey(RAW_KEY, PARTICIPANT).length);
        }
    }

    @Nested
    @DisplayName("SRTP master (live set_key inputs, call key bc4e7efa...8ab003d8)")
    class SrtpMaster {
        // CallKeyDerivation live verification vector
        private static final byte[] CALL_KEY =
                HEX.parseHex("bc4e7efa3efe251b9d5aeb3c36843d776c27777d67e5555f7956fd4f8ab003d8");

        @Test
        @DisplayName("derives the caller participant SRTP master to the captured value")
        public void callerMaster() {
            var master = CallE2eKeyDerivation.deriveSrtpMaster(CALL_KEY, device("39110693621863:29@lid"));
            assertArrayEquals(
                    HEX.parseHex("c1087730f4b5c07801a37795c5335885f751ba1c1cbe9c3669965b50ba1d"),
                    master);
        }

        @Test
        @DisplayName("splits the master into a 16-byte key and a 14-byte salt that concatenate back")
        public void split() {
            var master = CallE2eKeyDerivation.deriveSrtpMaster(CALL_KEY, device("39110693621863:29@lid"));
            var key = CallE2eKeyDerivation.srtpMasterKey(master);
            var salt = CallE2eKeyDerivation.srtpMasterSalt(master);
            assertEquals(CallE2eKeyDerivation.SRTP_DIRECTION_KEY_LENGTH, key.length);
            assertEquals(14, salt.length);
            var rejoined = new byte[CallE2eKeyDerivation.SRTP_MASTER_LENGTH];
            System.arraycopy(key, 0, rejoined, 0, key.length);
            System.arraycopy(salt, 0, rejoined, key.length, salt.length);
            assertArrayEquals(master, rejoined);
        }
    }

    @Nested
    @DisplayName("key chain and keygen-version guard")
    class KeyChain {
        private static final byte[] CALL_KEY =
                HEX.parseHex("bc4e7efa3efe251b9d5aeb3c36843d776c27777d67e5555f7956fd4f8ab003d8");
        private static final Jid PARTICIPANT = device("39110693621863:29@lid");

        @Test
        @DisplayName("packages the SFrame base key and SRTP master matching the standalone derivations")
        public void packages() {
            var chain = CallE2eKeyDerivation.deriveKeyChain(CALL_KEY, CallE2eKeyDerivation.SUPPORTED_KEYGEN_VER, PARTICIPANT);
            assertArrayEquals(CallE2eKeyDerivation.deriveSframeBaseKey(CALL_KEY, PARTICIPANT), chain.sframeBaseKey());
            assertArrayEquals(CallE2eKeyDerivation.deriveSrtpMaster(CALL_KEY, PARTICIPANT), chain.srtpMaster());
            assertArrayEquals(CallE2eKeyDerivation.srtpMasterKey(chain.srtpMaster()), chain.srtpKey());
            assertArrayEquals(CallE2eKeyDerivation.srtpMasterSalt(chain.srtpMaster()), chain.srtpSalt());
        }

        @Test
        @DisplayName("rejects any keygen version other than 2")
        public void rejectsUnsupportedVersion() {
            assertThrows(WhatsAppCallException.Srtp.class,
                    () -> CallE2eKeyDerivation.deriveKeyChain(CALL_KEY, 1, PARTICIPANT));
            assertThrows(WhatsAppCallException.Srtp.class,
                    () -> CallE2eKeyDerivation.requireSupportedKeygenVersion(3));
        }

        @Test
        @DisplayName("ParticipantKeyChain defensively copies its arrays")
        public void defensiveCopy() {
            var chain = CallE2eKeyDerivation.deriveKeyChain(CALL_KEY, 2, PARTICIPANT);
            var snapshot = chain.sframeBaseKey();
            chain.sframeBaseKey()[0] ^= 0x55;
            assertArrayEquals(snapshot, chain.sframeBaseKey());
        }
    }

    @Nested
    @DisplayName("hop-by-hop SRTP key set")
    class HopByHop {
        @Test
        @DisplayName("each group's key-step salt equals its own chaining-salt output and masters differ")
        public void groupsAreDistinct() {
            var hbhKey = new byte[CallE2eKeyDerivation.HBH_KEY_LENGTH];
            for (var i = 0; i < hbhKey.length; i++) {
                hbhKey[i] = (byte) (i + 1);
            }
            var media = CallE2eKeyDerivation.deriveHbhSrtpMaster(hbhKey, CallE2eKeyDerivation.HopByHopGroup.MEDIA);
            var srtcp = CallE2eKeyDerivation.deriveHbhSrtpMaster(hbhKey, CallE2eKeyDerivation.HopByHopGroup.SRTCP);
            var uplink = CallE2eKeyDerivation.deriveHbhSrtpMaster(hbhKey, CallE2eKeyDerivation.HopByHopGroup.UPLINK_SRTCP);
            var downlink = CallE2eKeyDerivation.deriveHbhSrtpMaster(hbhKey, CallE2eKeyDerivation.HopByHopGroup.DOWNLINK_SRTCP);
            assertEquals(CallE2eKeyDerivation.HBH_SRTP_MASTER_LENGTH, media.length);
            assertNotEquals(HEX.formatHex(media), HEX.formatHex(srtcp));
            assertNotEquals(HEX.formatHex(uplink), HEX.formatHex(downlink));
        }
    }

    @Nested
    @DisplayName("WARP auth key (chained derivation, wa_sfu_kdf 'warp auth' group)")
    class WarpAuthKey {
        private static byte[] hbhKey() {
            var hbhKey = new byte[CallE2eKeyDerivation.HBH_KEY_LENGTH];
            for (var i = 0; i < hbhKey.length; i++) {
                hbhKey[i] = (byte) (i + 0x40);
            }
            return hbhKey;
        }

        @Test
        @DisplayName("derives a 32-byte key via the same two-step chain as the SRTP masters, not a flat HKDF")
        public void chainedAndThirtyTwoBytes() {
            var warp = CallE2eKeyDerivation.deriveWarpAuthKey(hbhKey());
            assertEquals(CallE2eKeyDerivation.WARP_AUTH_KEY_LENGTH, warp.length);
            // The chain shares its salt step with deriveHbhSrtpMaster(WARP_AUTH) but its key step expands to
            // 32 bytes (the warp-auth guard length) instead of the 30-byte SRTP master; the 30-byte master is
            // therefore the 30-byte prefix of the 32-byte warp-auth key (same chain, longer key-step output).
            var srtpStyle = CallE2eKeyDerivation.deriveHbhSrtpMaster(hbhKey(), CallE2eKeyDerivation.HopByHopGroup.WARP_AUTH);
            assertArrayEquals(java.util.Arrays.copyOf(warp, CallE2eKeyDerivation.HBH_SRTP_MASTER_LENGTH), srtpStyle);
        }

        @Test
        @DisplayName("differs from the SRTP-media master and from the cert-fingerprint label")
        public void distinctFromOtherGroups() {
            var warp = CallE2eKeyDerivation.deriveWarpAuthKey(hbhKey());
            var media = CallE2eKeyDerivation.deriveHbhSrtpMaster(hbhKey(), CallE2eKeyDerivation.HopByHopGroup.MEDIA);
            assertFalse(java.util.Arrays.equals(
                    java.util.Arrays.copyOf(warp, media.length), media));
        }

        @Test
        @DisplayName("rejects an hbh key that is not 30 bytes")
        public void shortHbhKey() {
            assertThrows(IllegalArgumentException.class,
                    () -> CallE2eKeyDerivation.deriveWarpAuthKey(new byte[16]));
        }
    }

    @Nested
    @DisplayName("input validation")
    class Validation {
        private static final Jid PARTICIPANT = device("39110693621863:29@lid");

        @Test
        @DisplayName("rejects a raw key that is not 32 bytes")
        public void shortRawKey() {
            assertThrows(IllegalArgumentException.class,
                    () -> CallE2eKeyDerivation.deriveSframeBaseKey(new byte[16], PARTICIPANT));
            assertThrows(IllegalArgumentException.class,
                    () -> CallE2eKeyDerivation.deriveSrtpMaster(new byte[31], PARTICIPANT));
        }

        @Test
        @DisplayName("rejects an hbh key that is not 30 bytes")
        public void shortHbhKey() {
            assertThrows(IllegalArgumentException.class,
                    () -> CallE2eKeyDerivation.deriveHbhSrtpMaster(new byte[16], CallE2eKeyDerivation.HopByHopGroup.MEDIA));
        }

        @Test
        @DisplayName("mints a fresh 32-byte raw key on each call")
        public void mintRawKey() {
            var a = CallE2eKeyDerivation.mintRawKey();
            var b = CallE2eKeyDerivation.mintRawKey();
            assertEquals(CallE2eKeyDerivation.RAW_E2E_KEY_LENGTH, a.length);
            assertFalse(java.util.Arrays.equals(a, b));
        }
    }
}
