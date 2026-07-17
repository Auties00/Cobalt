package com.github.auties00.cobalt.calls.capability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VideoDecoderCapability codec bitmask")
class VideoDecoderCapabilityTest {
    @ParameterizedTest
    @DisplayName("each codec binds its recovered bit position and single-bit mask")
    @CsvSource({
            // Bit order recovered from pjmedia_vid_codec_name_str_to_codec_type (fn5297) and its inverse
            // (fn5296); H265 and VP8/VP9 are swapped relative to the pixel-format descriptor table.
            "H264, 0, 0x01",
            "VP8,  1, 0x02",
            "VP9,  2, 0x04",
            "H265, 3, 0x08",
            "AV1,  4, 0x10"
    })
    void recoveredBitPositions(VideoDecoderCapability codec, int bit, int mask) {
        assertEquals(bit, codec.bit(), () -> codec + " bit position must match the codec-type cascade");
        assertEquals(mask, codec.mask(), () -> codec + " mask must be a single set bit");
        assertEquals(1 << bit, codec.mask(), () -> codec + " mask must equal 1 << bit");
    }

    @Test
    @DisplayName("declares the five codecs in the recovered engine order")
    void declarationOrder() {
        assertArrayOrder(new VideoDecoderCapability[]{
                VideoDecoderCapability.H264,
                VideoDecoderCapability.VP8,
                VideoDecoderCapability.VP9,
                VideoDecoderCapability.H265,
                VideoDecoderCapability.AV1
        });
    }

    private static void assertArrayOrder(VideoDecoderCapability[] expected) {
        var actual = VideoDecoderCapability.values();
        assertEquals(expected.length, actual.length, "codec count");
        for (var i = 0; i < expected.length; i++) {
            var index = i;
            assertSame(expected[i], actual[i], () -> "codec at ordinal " + index);
        }
    }

    @ParameterizedTest
    @EnumSource(VideoDecoderCapability.class)
    @DisplayName("each codec round-trips through its wire token")
    void tokenRoundTrip(VideoDecoderCapability codec) {
        assertSame(codec, VideoDecoderCapability.ofToken(codec.token()).orElseThrow());
    }

    @ParameterizedTest
    @DisplayName("resolves the case-insensitive aliases and numeric ids onto the same bit")
    @CsvSource({
            "h264, H264", "H.264, H264", "AVC, H264", "1, H264",
            "vp8, VP8", "2, VP8",
            "vp9, VP9", "4, VP9",
            "h265, H265", "H.265, H265", "HEVC, H265", "8, H265",
            "av1, AV1", "16, AV1"
    })
    void aliasResolution(String alias, VideoDecoderCapability expected) {
        assertSame(expected, VideoDecoderCapability.ofToken(alias).orElseThrow(),
                () -> "alias " + alias + " must resolve to " + expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not_a_codec", "x265,foo"})
    @DisplayName("falls back to H264 only on null, blank, or unrecognized descriptors")
    void parseFallsBackToH264(String descriptor) {
        assertEquals(EnumSet.of(VideoDecoderCapability.H264), VideoDecoderCapability.parse(descriptor));
    }

    @Test
    @DisplayName("falls back to H264 on a null descriptor")
    void parseNullFallsBack() {
        assertEquals(EnumSet.of(VideoDecoderCapability.H264), VideoDecoderCapability.parse(null));
    }

    @Test
    @DisplayName("parses a multi-codec descriptor and ignores unknown tokens")
    void parseMultiCodec() {
        var parsed = VideoDecoderCapability.parse("H264, VP9 , bogus,AV1");
        assertEquals(EnumSet.of(VideoDecoderCapability.H264, VideoDecoderCapability.VP9, VideoDecoderCapability.AV1),
                parsed);
    }

    @Test
    @DisplayName("emits the descriptor in descending priority order")
    void toDescriptorOrdersByPriority() {
        // priority H264=4 > H265=3 > VP8=2 > VP9=1 > AV1=0, so the highest-priority codec leads.
        var descriptor = VideoDecoderCapability.toDescriptor(EnumSet.allOf(VideoDecoderCapability.class));
        assertEquals("H264,H265,VP8,VP9,AV1", descriptor);
    }

    @Test
    @DisplayName("intersects to the codecs common to both sides")
    void intersectKeepsCommon() {
        Set<VideoDecoderCapability> self = EnumSet.of(VideoDecoderCapability.H264, VideoDecoderCapability.VP9,
                VideoDecoderCapability.AV1);
        Set<VideoDecoderCapability> peer = EnumSet.of(VideoDecoderCapability.VP9, VideoDecoderCapability.H265);
        assertEquals(EnumSet.of(VideoDecoderCapability.VP9), VideoDecoderCapability.intersect(self, peer));
    }

    @Test
    @DisplayName("negotiates the highest-priority common codec")
    void negotiatePicksHighestPriority() {
        Set<VideoDecoderCapability> self = EnumSet.of(VideoDecoderCapability.H264, VideoDecoderCapability.VP8,
                VideoDecoderCapability.VP9);
        Set<VideoDecoderCapability> peer = EnumSet.of(VideoDecoderCapability.VP8, VideoDecoderCapability.VP9,
                VideoDecoderCapability.H264);
        // H264 wins on priority among the common {H264, VP8, VP9}.
        assertSame(VideoDecoderCapability.H264, VideoDecoderCapability.negotiate(self, peer).orElseThrow());
    }

    @Test
    @DisplayName("negotiation yields empty when the sides share no codec")
    void negotiateEmptyWhenDisjoint() {
        Set<VideoDecoderCapability> self = EnumSet.of(VideoDecoderCapability.H264);
        Set<VideoDecoderCapability> peer = EnumSet.of(VideoDecoderCapability.AV1);
        assertTrue(VideoDecoderCapability.negotiate(self, peer).isEmpty());
    }
}
