package com.github.auties00.cobalt.calls.media.video.yuv;

import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pure-Java {@link YuvConverter}: the scalar and vector backends must agree bit-for-bit, and the
 * conversions must satisfy the BT.601 sanity, chroma-repack round-trip, and rotation-identity invariants. All
 * fixtures are seeded so failures reproduce.
 */
class YuvConverterTest {
    private static final ScalarYuvConverter SCALAR = new ScalarYuvConverter();
    private static final VectorYuvConverter VECTOR = new VectorYuvConverter();

    private static Stream<Arguments> sizes() {
        return Stream.of(
                Arguments.of(2, 2), Arguments.of(4, 4), Arguments.of(6, 4), Arguments.of(16, 16),
                Arguments.of(18, 10), Arguments.of(32, 18), Arguments.of(64, 48), Arguments.of(100, 60),
                Arguments.of(128, 72), Arguments.of(320, 240));
    }

    private static VideoFrame randomI420(int width, int height, long seed) {
        var pixels = new byte[width * height + 2 * (width / 2) * (height / 2)];
        new Random(seed).nextBytes(pixels);
        return new VideoFrame(pixels, VideoPixelFormat.I420, width, height, 0);
    }

    private static int[] randomArgb(int width, int height, long seed) {
        var random = new Random(seed);
        var argb = new int[width * height];
        for (var i = 0; i < argb.length; i++) {
            argb[i] = 0xFF000000 | (random.nextInt() & 0xFFFFFF);
        }
        return argb;
    }

    private static VideoFrame solidI420(int width, int height, int y, int u, int v) {
        var pixels = new byte[width * height + 2 * (width / 2) * (height / 2)];
        var luma = width * height;
        var chroma = (width / 2) * (height / 2);
        Arrays.fill(pixels, 0, luma, (byte) y);
        Arrays.fill(pixels, luma, luma + chroma, (byte) u);
        Arrays.fill(pixels, luma + chroma, pixels.length, (byte) v);
        return new VideoFrame(pixels, VideoPixelFormat.I420, width, height, 0);
    }

    @Nested
    @DisplayName("scalar and vector backends agree bit-for-bit")
    class ScalarVectorEquivalence {
        @ParameterizedTest(name = "{0}x{1}")
        @MethodSource("com.github.auties00.cobalt.calls.media.video.yuv.YuvConverterTest#sizes")
        @DisplayName("toArgb")
        void toArgb(int width, int height) {
            var frame = randomI420(width, height, 1);
            assertArrayEquals(SCALAR.toArgb(frame), VECTOR.toArgb(frame));
        }

        @ParameterizedTest(name = "{0}x{1}")
        @MethodSource("com.github.auties00.cobalt.calls.media.video.yuv.YuvConverterTest#sizes")
        @DisplayName("argbToI420")
        void argbToI420(int width, int height) {
            var argb = randomArgb(width, height, 2);
            assertArrayEquals(
                    SCALAR.argbToI420(argb, width, height, 0).pixels(),
                    VECTOR.argbToI420(argb, width, height, 0).pixels());
        }

        @ParameterizedTest(name = "{0}x{1}")
        @MethodSource("com.github.auties00.cobalt.calls.media.video.yuv.YuvConverterTest#sizes")
        @DisplayName("repack, scale, and rotate")
        void repackScaleRotate(int width, int height) {
            var frame = randomI420(width, height, 3);
            assertArrayEquals(SCALAR.toNv12(frame).pixels(), VECTOR.toNv12(frame).pixels());
            assertArrayEquals(SCALAR.scale(frame, 8, 8).pixels(), VECTOR.scale(frame, 8, 8).pixels());
            for (var degrees : new int[]{0, 90, 180, 270}) {
                assertArrayEquals(SCALAR.rotate(frame, degrees).pixels(), VECTOR.rotate(frame, degrees).pixels());
            }
        }
    }

    @Nested
    @DisplayName("pure-Java sanity")
    class Sanity {
        @Test
        @DisplayName("solid BT.601 luma maps to the expected gray")
        void solidLuma() {
            assertGray(solidI420(4, 4, 16, 128, 128), 0);
            assertGray(solidI420(4, 4, 235, 128, 128), 255);
            assertGray(solidI420(4, 4, 126, 128, 128), 128);
        }

        @ParameterizedTest(name = "{0}x{1}")
        @MethodSource("com.github.auties00.cobalt.calls.media.video.yuv.YuvConverterTest#sizes")
        @DisplayName("NV12 round-trip is loss-free")
        void nv12RoundTrip(int width, int height) {
            var frame = randomI420(width, height, 4);
            assertArrayEquals(frame.pixels(), SCALAR.toI420(SCALAR.toNv12(frame)).pixels());
        }

        @ParameterizedTest(name = "{0}x{1}")
        @MethodSource("com.github.auties00.cobalt.calls.media.video.yuv.YuvConverterTest#sizes")
        @DisplayName("four 90-degree rotations and two 180-degree rotations are identity")
        void rotationIdentity(int width, int height) {
            var frame = randomI420(width, height, 5);
            var rotated = frame;
            for (var i = 0; i < 4; i++) {
                rotated = SCALAR.rotate(rotated, 90);
            }
            assertArrayEquals(frame.pixels(), rotated.pixels());
            assertEquals(width, rotated.width());
            assertEquals(height, rotated.height());
            assertArrayEquals(frame.pixels(), SCALAR.rotate(SCALAR.rotate(frame, 180), 180).pixels());
        }

        private static void assertGray(VideoFrame frame, int expected) {
            var pixel = SCALAR.toArgb(frame)[0];
            assertEquals(0xFF, (pixel >>> 24) & 0xFF, "alpha");
            for (var shift : new int[]{16, 8, 0}) {
                assertTrue(Math.abs(((pixel >> shift) & 0xFF) - expected) <= 1,
                        "channel at " + shift + " near " + expected + " in " + Integer.toHexString(pixel));
            }
        }
    }
}
