package com.github.auties00.cobalt.calls.engine.participant;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Known-answer tests for {@link CallSecureSsrcGenerator}, pinning the deterministic per-device SSRC
 * derivation to a live WhatsApp VoIP capture.
 *
 * <p>The call-id and the expected SSRCs were read from the native engine's
 * {@code call_generate_ssrc_for_participant} log lines for call-id
 * {@code 007498E578A915C0F9814AC2CB48D28F}; pinning them fixes the call-id ASCII input keying material,
 * the four-byte little-endian media-type salt, the device-JID identifier (and its {@code "_<id>"} suffix
 * for higher stream ids), and the little-endian output.
 */
public class CallSecureSsrcGeneratorTest {
    private static final String CALL_ID = "007498E578A915C0F9814AC2CB48D28F";
    private static final Jid CALLER = Jid.of("258252122116273:94@lid");
    private static final Jid PEER = Jid.of("39110693621863:0@lid");

    @Nested
    @DisplayName("audio stream (media-type 0/1/4)")
    class Audio {
        @Test
        @DisplayName("caller audio main/fec/nack match the captured values")
        public void caller() {
            var triple = CallSecureSsrcGenerator.audioTriple(CALL_ID, CALLER);
            assertEquals(0xE438BF63, triple.primary());
            assertEquals(0xD3CB93DC, triple.fec());
            assertEquals(0x6DE76824, triple.oobNack());
        }

        @Test
        @DisplayName("audioMainSsrc returns the audio primary for both endpoints")
        public void mainHelper() {
            assertEquals(0xE438BF63, CallSecureSsrcGenerator.audioMainSsrc(CALL_ID, CALLER));
            assertEquals(0x3058A8C4, CallSecureSsrcGenerator.audioMainSsrc(CALL_ID, PEER));
        }

        @Test
        @DisplayName("peer audio main/fec/nack match the captured values")
        public void peer() {
            var triple = CallSecureSsrcGenerator.audioTriple(CALL_ID, PEER);
            assertEquals(0x3058A8C4, triple.primary());
            assertEquals(0x4D2B0011, triple.fec());
            assertEquals(0x5ABCB5F0, triple.oobNack());
        }
    }

    @Nested
    @DisplayName("video streams (media-type 2/3/5, _<id> suffix for stream 1)")
    class Video {
        @Test
        @DisplayName("caller video stream 0 main/fec/nack match the captured values")
        public void stream0() {
            var triple = CallSecureSsrcGenerator.videoTriple(CALL_ID, CALLER, 0);
            assertEquals(0xA4AF8E8F, triple.primary());
            assertEquals(0x2D89D462, triple.fec());
            assertEquals(0x935DF37C, triple.oobNack());
        }

        @Test
        @DisplayName("caller video stream 1 carries the _1 seed suffix")
        public void stream1() {
            var triple = CallSecureSsrcGenerator.videoTriple(CALL_ID, CALLER, 1);
            assertEquals(0xCC5B7B35, triple.primary());
            assertEquals(0xC8FE9CF6, triple.fec());
            assertEquals(0xB49EB3A4, triple.oobNack());
            assertNotEquals(triple.primary(), CallSecureSsrcGenerator.videoTriple(CALL_ID, CALLER, 0).primary());
        }
    }

    @Nested
    @DisplayName("raw primitive and auxiliary streams")
    class Raw {
        @Test
        @DisplayName("the raw ssrc primitive reproduces the captured audio fec(1) and nack(4)")
        public void rawPrimitive() {
            assertEquals(0xD3CB93DC, CallSecureSsrcGenerator.ssrc(CALL_ID, CALLER, 1, 0));
            assertEquals(0x6DE76824, CallSecureSsrcGenerator.ssrc(CALL_ID, CALLER, 4, 0));
        }

        @Test
        @DisplayName("app-data and imu SSRCs use codes 6 and 10 and differ from the audio primary")
        public void auxStreams() {
            var appData = CallSecureSsrcGenerator.appDataSsrc(CALL_ID, CALLER);
            var imu = CallSecureSsrcGenerator.imuDataSsrc(CALL_ID, CALLER);
            assertEquals(CallSecureSsrcGenerator.ssrc(CALL_ID, CALLER, CallSecureSsrcGenerator.MEDIA_TYPE_APP_DATA, 0), appData);
            assertEquals(CallSecureSsrcGenerator.ssrc(CALL_ID, CALLER, CallSecureSsrcGenerator.MEDIA_TYPE_IMU_DATA, 0), imu);
            assertNotEquals(appData, imu);
            assertNotEquals(0xE438BF63, appData);
        }
    }
}
