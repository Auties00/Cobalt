package com.github.auties00.cobalt.calls.media.video.yuv;

import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;

import java.util.Optional;

/**
 * Converts raw video pictures between the planar {@link VideoPixelFormat#I420 I420} layout the call codecs
 * operate on, the semi planar {@link VideoPixelFormat#NV12 NV12} layout several cameras deliver, and packed
 * 32 bit ARGB pixels used by render and capture surfaces, and scales or rotates an
 * {@link VideoPixelFormat#I420 I420} picture.
 *
 * <p>This is a pure Java converter: every color space transform, chroma repack, scale, and rotate is
 * performed in Java rather than by a native library. The color transforms use ITU R BT.601 studio swing
 * (limited range) coefficients matching libyuv, with Y in {@code [16, 235]} and chroma centered at
 * {@code 128}; results outside the gamut are clamped to {@code [0, 255]} per channel. The repack and color
 * methods preserve picture geometry and orientation; only {@link #scale(VideoFrame, int, int)} changes the
 * dimensions and only {@link #rotate(VideoFrame, int)} changes the orientation. libyuv's {@code ARGB} pixel
 * is four bytes ordered B, G, R, A in memory, which on a little endian host is a {@code uint32} numerically
 * equal to Java's {@code 0xAARRGGBB} packed int; this converter produces that same {@code 0xAARRGGBB} layout
 * with the alpha byte set to {@code 0xFF}.
 *
 * <p>Obtain an instance with {@link #create()}, which selects the fastest backend the runtime image
 * supports, and hold that single instance: the converter is stateless and its methods are pure, so one
 * instance is safe to share across threads. Both frame dimensions must be even so the half resolution chroma
 * planes have integral sizes, matching {@link VideoFrame}.
 *
 * @implSpec Every implementation must produce byte identical results to every other for the chroma repack
 * ({@link #toI420(VideoFrame)}, {@link #toNv12(VideoFrame)}) and {@link #rotate(VideoFrame, int) rotate}
 * operations, and results within a small per channel tolerance for the color space and
 * {@link #scale(VideoFrame, int, int) scale} operations.
 */
public sealed interface YuvConverter permits ScalarYuvConverter, VectorYuvConverter {
    /**
     * The incubator module the {@link VectorYuvConverter} backend depends on.
     */
    String VECTOR_MODULE_NAME = "jdk.incubator.vector";

    /**
     * The system property GraalVM sets inside a native image, used to skip the Vector API where it offers no
     * SIMD speedup.
     */
    String NATIVE_IMAGE_PROPERTY = "org.graalvm.nativeimage.imagecode";

    /**
     * Returns a converter using the fastest backend the runtime image supports.
     *
     * <p>Prefers the SIMD {@link VectorYuvConverter} when the {@value #VECTOR_MODULE_NAME} incubator module
     * is present and worth using, and falls back to the always available {@link ScalarYuvConverter}
     * otherwise. It rules out the SIMD path when running as a GraalVM native image (where the Vector API is
     * scalar emulated and strictly slower), when the incubator module is absent from the module graph (the
     * launcher did not pass {@code --add-modules jdk.incubator.vector}), or when the Vector API fails to
     * initialise. Naming {@link VectorYuvConverter} here only forces the verifier to load it (to confirm it
     * is a permitted subtype); the incubator types in its descriptors are linked only when its constructor
     * runs, which is gated behind the check that the module is present, so a process launched without the
     * flag falls back cleanly rather than failing with a {@link NoClassDefFoundError}.
     *
     * @return a converter; never {@code null}
     */
    static YuvConverter create() {
        if (System.getProperty(NATIVE_IMAGE_PROPERTY) != null) {
            return new ScalarYuvConverter();
        }

        var module = YuvConverter.class.getModule();
        var layer = Optional.ofNullable(module.getLayer())
                .orElse(ModuleLayer.boot());
        var vectorModule = layer.findModule(VECTOR_MODULE_NAME);
        if (vectorModule.isEmpty()) {
            return new ScalarYuvConverter();
        }
        module.addReads(vectorModule.get());

        try {
            var converter = new VectorYuvConverter();
            return converter;
        } catch (Throwable t) {
            return new ScalarYuvConverter();
        }
    }

    /**
     * Converts a planar {@link VideoPixelFormat#I420 I420} or semi planar {@link VideoPixelFormat#NV12 NV12}
     * frame to packed 32 bit ARGB pixels.
     *
     * <p>The result holds {@code width * height} pixels, one {@code int} each in {@code 0xAARRGGBB} order
     * with the alpha byte set to {@code 0xFF} (fully opaque). An {@link VideoPixelFormat#NV12 NV12} input is
     * first repacked to {@link VideoPixelFormat#I420 I420} and then color converted by the BT.601 limited
     * range matrix; results outside the gamut are clamped to {@code [0, 255]} per channel.
     *
     * @param frame the source picture in {@link VideoPixelFormat#I420 I420} or {@link VideoPixelFormat#NV12 NV12}
     * @return the packed ARGB pixels, {@code width * height} ints in {@code 0xAARRGGBB} order
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    int[] toArgb(VideoFrame frame);

    /**
     * Converts packed 32 bit ARGB pixels to a planar {@link VideoPixelFormat#I420 I420} frame.
     *
     * <p>The alpha channel is ignored. Luma is computed per pixel and the {@code 4:2:0} chroma from each
     * {@code 2x2} block, applying the BT.601 limited range matrix.
     *
     * @param argb      the packed ARGB pixels, at least {@code width * height} ints in {@code 0xAARRGGBB} order
     * @param width     the picture width in pixels; even and at least {@code 2}
     * @param height    the picture height in pixels; even and at least {@code 2}
     * @param ptsMicros the presentation timestamp to stamp on the produced frame
     * @return the converted {@link VideoPixelFormat#I420 I420} frame
     * @throws NullPointerException     if {@code argb} is {@code null}
     * @throws IllegalArgumentException if {@code width} or {@code height} is odd or below {@code 2}, or if
     *                                  {@code argb} is shorter than {@code width * height}
     */
    VideoFrame argbToI420(int[] argb, int width, int height, long ptsMicros);

    /**
     * Repacks a frame into the planar {@link VideoPixelFormat#I420 I420} layout, converting from
     * {@link VideoPixelFormat#NV12 NV12} if needed.
     *
     * <p>This is a loss free chroma repack, not a color transform: an {@link VideoPixelFormat#I420 I420}
     * input is returned unchanged, and an {@link VideoPixelFormat#NV12 NV12} input has its interleaved chroma
     * plane split into the separate U and V planes.
     *
     * @param frame the source frame in {@link VideoPixelFormat#I420 I420} or {@link VideoPixelFormat#NV12 NV12}
     * @return an {@link VideoPixelFormat#I420 I420} frame with the same pixels, dimensions, and timestamp
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    VideoFrame toI420(VideoFrame frame);

    /**
     * Repacks a frame into the semi planar {@link VideoPixelFormat#NV12 NV12} layout, converting from
     * {@link VideoPixelFormat#I420 I420} if needed.
     *
     * <p>This is the inverse chroma repack of {@link #toI420(VideoFrame)}: an {@link VideoPixelFormat#NV12 NV12}
     * input is returned unchanged, and an {@link VideoPixelFormat#I420 I420} input has its separate U and V
     * planes interleaved into a single chroma plane.
     *
     * @param frame the source frame in {@link VideoPixelFormat#I420 I420} or {@link VideoPixelFormat#NV12 NV12}
     * @return an {@link VideoPixelFormat#NV12 NV12} frame with the same pixels, dimensions, and timestamp
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    VideoFrame toNv12(VideoFrame frame);

    /**
     * Resamples an {@link VideoPixelFormat#I420 I420} picture to new dimensions with a bilinear filter.
     *
     * <p>The source is first repacked to {@link VideoPixelFormat#I420 I420} via {@link #toI420(VideoFrame)}
     * if it is in {@link VideoPixelFormat#NV12 NV12}, then each plane is resampled to the destination
     * geometry. The returned frame keeps the source timestamp.
     *
     * @param src       the source picture in {@link VideoPixelFormat#I420 I420} or {@link VideoPixelFormat#NV12 NV12}
     * @param dstWidth  the target width in pixels; even and at least {@code 2}
     * @param dstHeight the target height in pixels; even and at least {@code 2}
     * @return the resampled {@link VideoPixelFormat#I420 I420} frame at {@code dstWidth x dstHeight}
     * @throws NullPointerException     if {@code src} is {@code null}
     * @throws IllegalArgumentException if {@code dstWidth} or {@code dstHeight} is odd or below {@code 2}
     */
    VideoFrame scale(VideoFrame src, int dstWidth, int dstHeight);

    /**
     * Rotates an {@link VideoPixelFormat#I420 I420} picture clockwise by a quarter turn multiple.
     *
     * <p>The source is first repacked to {@link VideoPixelFormat#I420 I420} via {@link #toI420(VideoFrame)}
     * if it is in {@link VideoPixelFormat#NV12 NV12}, then each plane is rotated. A {@code 90} or
     * {@code 270} degree rotation transposes the picture, so the returned frame swaps width and height;
     * {@code 0} and {@code 180} keep the source geometry. The returned frame keeps the source timestamp.
     *
     * @param src     the source picture in {@link VideoPixelFormat#I420 I420} or {@link VideoPixelFormat#NV12 NV12}
     * @param degrees the clockwise rotation in degrees; exactly {@code 0}, {@code 90}, {@code 180}, or {@code 270}
     * @return the rotated {@link VideoPixelFormat#I420 I420} frame, with width and height swapped for
     *         {@code 90} and {@code 270}
     * @throws NullPointerException     if {@code src} is {@code null}
     * @throws IllegalArgumentException if {@code degrees} is not {@code 0}, {@code 90}, {@code 180}, or {@code 270}
     */
    VideoFrame rotate(VideoFrame src, int degrees);
}
