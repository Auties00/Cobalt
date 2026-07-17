package com.github.auties00.cobalt.calls.media.video.yuv;

import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Scalar {@link YuvConverter} backend, and the home of the raw pixel kernels every backend shares.
 *
 * <p>This implementation performs every conversion in plain Java over the packed plane buffers of a
 * {@link VideoFrame}. It is always available, so {@link YuvConverter#create()} falls back to it whenever the
 * Vector API is absent, and {@link VectorYuvConverter} routes the operations it does not vectorise to the
 * static frame helpers here so the two backends cannot disagree on geometry, orientation, or chroma
 * repacking.
 *
 * @implNote The color coefficients reproduce the ITU R BT.601 studio swing (limited range) reference:
 * {@link #yuvToArgb(int, int, int)} carries the YUV to RGB matrix, and the fixed point RGB to Y, RGB to U,
 * and RGB to V forms feed {@link #argbToY(int[], byte[], int, int)} and
 * {@link #argbToUv(int[], byte[], int, int)}. The repack and rotate kernels are pure byte moves and are
 * exact; the color and {@link #scalePlaneBilinear(byte[], int, int, int, byte[], int, int, int) bilinear}
 * kernels carry a small rounding tolerance that stays below the perceptual and wire observable threshold,
 * since their output feeds a lossy codec or a render surface rather than the wire.
 */
final class ScalarYuvConverter implements YuvConverter {
    /**
     * The luma scale in the BT.601 limited range matrix, computed as
     * {@code round(1.164 * 64 * 256 * 256 / 257)}.
     */
    private static final int YG = 18997;

    /**
     * The luma bias in the BT.601 limited range matrix, computed as {@code 1.164 * 64 * -16 + 64 * 0.5}.
     */
    private static final int YGB = -1160;

    /**
     * The U to blue coefficient, computed as {@code max(-128, round(-2.018 * 64))}.
     */
    private static final int UB = -128;

    /**
     * The U to green coefficient, computed as {@code round(0.391 * 64)}.
     */
    private static final int UG = 25;

    /**
     * The V to green coefficient, computed as {@code round(0.813 * 64)}.
     */
    private static final int VG = 52;

    /**
     * The V to red coefficient, computed as {@code round(-1.596 * 64)}.
     */
    private static final int VR = -102;

    /**
     * The blue rounding and bias term, computed as {@code UB * 128 + YGB}.
     */
    private static final int BB = UB * 128 + YGB;

    /**
     * The green rounding and bias term, computed as {@code UG * 128 + VG * 128 + YGB}.
     */
    private static final int BG = UG * 128 + VG * 128 + YGB;

    /**
     * The red rounding and bias term, computed as {@code VR * 128 + YGB}.
     */
    private static final int BR = VR * 128 + YGB;

    /**
     * The logger for {@link ScalarYuvConverter}.
     */
    private static final System.Logger LOGGER = Log.get(ScalarYuvConverter.class);

    /**
     * Constructs the scalar backend.
     */
    ScalarYuvConverter() {

    }

    @Override
    public int[] toArgb(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "scalar yuv converter: toArgb, format={0} width={1} height={2}",
                    frame.format(), frame.width(), frame.height());
        }
        return toArgbFrame(frame);
    }

    @Override
    public VideoFrame argbToI420(int[] argb, int width, int height, long ptsMicros) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "scalar yuv converter: argbToI420, width={0} height={1}", width, height);
        return argbToI420Frame(argb, width, height, ptsMicros);
    }

    @Override
    public VideoFrame toI420(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        return toI420Frame(frame);
    }

    @Override
    public VideoFrame toNv12(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        return toNv12Frame(frame);
    }

    @Override
    public VideoFrame scale(VideoFrame src, int dstWidth, int dstHeight) {
        Objects.requireNonNull(src, "src cannot be null");
        requireEvenDimension(dstWidth, "dstWidth");
        requireEvenDimension(dstHeight, "dstHeight");
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "scalar yuv converter: scale, srcWidth={0} srcHeight={1} dstWidth={2} dstHeight={3}",
                    src.width(), src.height(), dstWidth, dstHeight);
        }
        return scaleFrame(src, dstWidth, dstHeight);
    }

    @Override
    public VideoFrame rotate(VideoFrame src, int degrees) {
        Objects.requireNonNull(src, "src cannot be null");
        if (Log.TRACE) LOGGER.log(Level.TRACE, "scalar yuv converter: rotate, degrees={0}", degrees);
        return rotateFrame(src, degrees);
    }

    /**
     * Converts a frame to packed ARGB, normalising an {@link VideoPixelFormat#NV12 NV12} source to
     * {@link VideoPixelFormat#I420 I420} first.
     *
     * @param frame the source frame
     * @return the packed ARGB pixels in {@code 0xAARRGGBB} order
     */
    static int[] toArgbFrame(VideoFrame frame) {
        var i420 = toI420Frame(frame);
        return i420ToArgb(i420.pixels(), i420.width(), i420.height());
    }

    /**
     * Converts packed ARGB to an {@link VideoPixelFormat#I420 I420} frame.
     *
     * @param argb      the packed ARGB pixels in {@code 0xAARRGGBB} order
     * @param width     the picture width; even and at least {@code 2}
     * @param height    the picture height; even and at least {@code 2}
     * @param ptsMicros the timestamp to stamp on the frame
     * @return the converted I420 frame
     */
    static VideoFrame argbToI420Frame(int[] argb, int width, int height, long ptsMicros) {
        Objects.requireNonNull(argb, "argb cannot be null");
        requireEvenDimension(width, "width");
        requireEvenDimension(height, "height");
        if (argb.length < width * height) {
            throw new IllegalArgumentException(
                    "argb must hold at least " + (width * height) + " pixels, got " + argb.length);
        }
        var out = new byte[i420ByteCount(width, height)];
        argbToY(argb, out, width, height);
        argbToUv(argb, out, width, height);
        return new VideoFrame(out, VideoPixelFormat.I420, width, height, ptsMicros);
    }

    /**
     * Repacks a frame to {@link VideoPixelFormat#I420 I420}, returning an I420 source unchanged.
     *
     * @param frame the source frame
     * @return an I420 frame with the same pixels, dimensions, and timestamp
     */
    static VideoFrame toI420Frame(VideoFrame frame) {
        if (frame.format() == VideoPixelFormat.I420) {
            return frame;
        }
        var width = frame.width();
        var height = frame.height();
        var out = new byte[i420ByteCount(width, height)];
        nv12ToI420(frame.pixels(), out, width, height);
        return new VideoFrame(out, VideoPixelFormat.I420, width, height, frame.ptsMicros());
    }

    /**
     * Repacks a frame to {@link VideoPixelFormat#NV12 NV12}, returning an NV12 source unchanged.
     *
     * @param frame the source frame
     * @return an NV12 frame with the same pixels, dimensions, and timestamp
     */
    static VideoFrame toNv12Frame(VideoFrame frame) {
        if (frame.format() == VideoPixelFormat.NV12) {
            return frame;
        }
        var width = frame.width();
        var height = frame.height();
        var out = new byte[i420ByteCount(width, height)];
        i420ToNv12(frame.pixels(), out, width, height);
        return new VideoFrame(out, VideoPixelFormat.NV12, width, height, frame.ptsMicros());
    }

    /**
     * Resamples a frame to new dimensions with a bilinear filter, normalising to
     * {@link VideoPixelFormat#I420 I420} first.
     *
     * @param src       the source frame
     * @param dstWidth  the target width; even and at least {@code 2}
     * @param dstHeight the target height; even and at least {@code 2}
     * @return the resampled I420 frame
     */
    static VideoFrame scaleFrame(VideoFrame src, int dstWidth, int dstHeight) {
        var source = toI420Frame(src);
        var srcWidth = source.width();
        var srcHeight = source.height();
        var srcChromaWidth = srcWidth / 2;
        var srcChromaHeight = srcHeight / 2;
        var dstChromaWidth = dstWidth / 2;
        var dstChromaHeight = dstHeight / 2;
        var pixels = source.pixels();
        var out = new byte[i420ByteCount(dstWidth, dstHeight)];
        var srcLuma = srcWidth * srcHeight;
        var srcChroma = srcChromaWidth * srcChromaHeight;
        var dstLuma = dstWidth * dstHeight;
        var dstChroma = dstChromaWidth * dstChromaHeight;
        scalePlaneBilinear(pixels, 0, srcWidth, srcHeight, out, 0, dstWidth, dstHeight);
        scalePlaneBilinear(pixels, srcLuma, srcChromaWidth, srcChromaHeight,
                out, dstLuma, dstChromaWidth, dstChromaHeight);
        scalePlaneBilinear(pixels, srcLuma + srcChroma, srcChromaWidth, srcChromaHeight,
                out, dstLuma + dstChroma, dstChromaWidth, dstChromaHeight);
        return new VideoFrame(out, VideoPixelFormat.I420, dstWidth, dstHeight, source.ptsMicros());
    }

    /**
     * Rotates a frame clockwise by a quarter turn multiple, normalising to {@link VideoPixelFormat#I420 I420}
     * first.
     *
     * @param src     the source frame
     * @param degrees the clockwise rotation; {@code 0}, {@code 90}, {@code 180}, or {@code 270}
     * @return the rotated I420 frame
     */
    static VideoFrame rotateFrame(VideoFrame src, int degrees) {
        if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270) {
            throw new IllegalArgumentException("degrees must be 0, 90, 180, or 270, got " + degrees);
        }
        var source = toI420Frame(src);
        var srcWidth = source.width();
        var srcHeight = source.height();
        var swap = degrees == 90 || degrees == 270;
        var dstWidth = swap ? srcHeight : srcWidth;
        var dstHeight = swap ? srcWidth : srcHeight;
        var srcChromaWidth = srcWidth / 2;
        var srcChromaHeight = srcHeight / 2;
        var pixels = source.pixels();
        var out = new byte[i420ByteCount(dstWidth, dstHeight)];
        var srcLuma = srcWidth * srcHeight;
        var srcChroma = srcChromaWidth * srcChromaHeight;
        var dstLuma = dstWidth * dstHeight;
        var dstChroma = (dstWidth / 2) * (dstHeight / 2);
        rotatePlane(pixels, 0, srcWidth, srcHeight, out, 0, degrees);
        rotatePlane(pixels, srcLuma, srcChromaWidth, srcChromaHeight, out, dstLuma, degrees);
        rotatePlane(pixels, srcLuma + srcChroma, srcChromaWidth, srcChromaHeight,
                out, dstLuma + dstChroma, degrees);
        return new VideoFrame(out, VideoPixelFormat.I420, dstWidth, dstHeight, source.ptsMicros());
    }

    /**
     * Converts a packed I420 buffer to packed ARGB with nearest neighbour chroma upsampling.
     *
     * @param i420   the packed I420 planes: Y, then U, then V
     * @param width  the picture width
     * @param height the picture height
     * @return the packed ARGB pixels in {@code 0xAARRGGBB} order
     */
    static int[] i420ToArgb(byte[] i420, int width, int height) {
        var chromaWidth = width / 2;
        var lumaSize = width * height;
        var chromaSize = chromaWidth * (height / 2);
        var uOffset = lumaSize;
        var vOffset = lumaSize + chromaSize;
        var argb = new int[width * height];
        for (var y = 0; y < height; y++) {
            var lumaRow = y * width;
            var chromaRow = (y >> 1) * chromaWidth;
            for (var x = 0; x < width; x++) {
                var yy = i420[lumaRow + x] & 0xFF;
                var chromaIndex = chromaRow + (x >> 1);
                var uu = i420[uOffset + chromaIndex] & 0xFF;
                var vv = i420[vOffset + chromaIndex] & 0xFF;
                argb[lumaRow + x] = yuvToArgb(yy, uu, vv);
            }
        }
        return argb;
    }

    /**
     * Converts one BT.601 limited range YUV sample to a packed {@code 0xAARRGGBB} pixel.
     *
     * @param y the luma sample in {@code [0, 255]}
     * @param u the blue chroma sample in {@code [0, 255]}
     * @param v the red chroma sample in {@code [0, 255]}
     * @return the packed opaque ARGB pixel
     */
    static int yuvToArgb(int y, int u, int v) {
        var y1 = (y * 0x0101 * YG) >>> 16;
        var b = clamp((y1 - u * UB + BB) >> 6);
        var g = clamp((y1 - u * UG - v * VG + BG) >> 6);
        var r = clamp((y1 - v * VR + BR) >> 6);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Fills the luma plane of {@code out} from packed ARGB pixels.
     *
     * @param argb   the packed ARGB pixels in {@code 0xAARRGGBB} order
     * @param out    the destination I420 buffer whose Y plane is written
     * @param width  the picture width
     * @param height the picture height
     */
    static void argbToY(int[] argb, byte[] out, int width, int height) {
        var count = width * height;
        for (var i = 0; i < count; i++) {
            var pixel = argb[i];
            var r = (pixel >> 16) & 0xFF;
            var g = (pixel >> 8) & 0xFF;
            var b = pixel & 0xFF;
            out[i] = (byte) rgbToY(r, g, b);
        }
    }

    /**
     * Fills the U and V planes of {@code out} from packed ARGB pixels, averaging each {@code 2x2} block.
     *
     * @param argb   the packed ARGB pixels in {@code 0xAARRGGBB} order
     * @param out    the destination I420 buffer whose U and V planes are written
     * @param width  the picture width
     * @param height the picture height
     */
    static void argbToUv(int[] argb, byte[] out, int width, int height) {
        var chromaWidth = width / 2;
        var chromaHeight = height / 2;
        var uOffset = width * height;
        var vOffset = uOffset + chromaWidth * chromaHeight;
        for (var cy = 0; cy < chromaHeight; cy++) {
            var row0 = (cy * 2) * width;
            var row1 = row0 + width;
            for (var cx = 0; cx < chromaWidth; cx++) {
                var x0 = cx * 2;
                var p00 = argb[row0 + x0];
                var p01 = argb[row0 + x0 + 1];
                var p10 = argb[row1 + x0];
                var p11 = argb[row1 + x0 + 1];
                var b = ((p00 & 0xFF) + (p01 & 0xFF) + (p10 & 0xFF) + (p11 & 0xFF)) >> 2;
                var g = (((p00 >> 8) & 0xFF) + ((p01 >> 8) & 0xFF) + ((p10 >> 8) & 0xFF) + ((p11 >> 8) & 0xFF)) >> 2;
                var r = (((p00 >> 16) & 0xFF) + ((p01 >> 16) & 0xFF) + ((p10 >> 16) & 0xFF) + ((p11 >> 16) & 0xFF)) >> 2;
                out[uOffset + cy * chromaWidth + cx] = (byte) clamp((112 * b - 74 * g - 38 * r + 0x8080) >> 8);
                out[vOffset + cy * chromaWidth + cx] = (byte) clamp((112 * r - 94 * g - 18 * b + 0x8080) >> 8);
            }
        }
    }

    /**
     * Computes one BT.601 limited range luma sample from an RGB triple.
     *
     * @param r the red channel in {@code [0, 255]}
     * @param g the green channel in {@code [0, 255]}
     * @param b the blue channel in {@code [0, 255]}
     * @return the luma sample in {@code [0, 255]}
     */
    static int rgbToY(int r, int g, int b) {
        return clamp((66 * r + 129 * g + 25 * b + 0x1080) >> 8);
    }

    /**
     * Splits an NV12 interleaved chroma buffer into the separate U and V planes of an I420 buffer, copying
     * the luma plane verbatim.
     *
     * @param src    the packed NV12 planes: Y, then interleaved UV
     * @param dst    the destination I420 buffer
     * @param width  the picture width
     * @param height the picture height
     */
    static void nv12ToI420(byte[] src, byte[] dst, int width, int height) {
        var lumaSize = width * height;
        var chromaWidth = width / 2;
        var chromaHeight = height / 2;
        var chromaSize = chromaWidth * chromaHeight;
        System.arraycopy(src, 0, dst, 0, lumaSize);
        var uOffset = lumaSize;
        var vOffset = lumaSize + chromaSize;
        for (var cy = 0; cy < chromaHeight; cy++) {
            var srcRow = lumaSize + cy * width;
            var dstRow = cy * chromaWidth;
            for (var cx = 0; cx < chromaWidth; cx++) {
                dst[uOffset + dstRow + cx] = src[srcRow + cx * 2];
                dst[vOffset + dstRow + cx] = src[srcRow + cx * 2 + 1];
            }
        }
    }

    /**
     * Interleaves the separate U and V planes of an I420 buffer into an NV12 chroma buffer, copying the luma
     * plane verbatim.
     *
     * @param src    the packed I420 planes: Y, then U, then V
     * @param dst    the destination NV12 buffer
     * @param width  the picture width
     * @param height the picture height
     */
    static void i420ToNv12(byte[] src, byte[] dst, int width, int height) {
        var lumaSize = width * height;
        var chromaWidth = width / 2;
        var chromaHeight = height / 2;
        var chromaSize = chromaWidth * chromaHeight;
        System.arraycopy(src, 0, dst, 0, lumaSize);
        var uOffset = lumaSize;
        var vOffset = lumaSize + chromaSize;
        for (var cy = 0; cy < chromaHeight; cy++) {
            var srcRow = cy * chromaWidth;
            var dstRow = lumaSize + cy * width;
            for (var cx = 0; cx < chromaWidth; cx++) {
                dst[dstRow + cx * 2] = src[uOffset + srcRow + cx];
                dst[dstRow + cx * 2 + 1] = src[vOffset + srcRow + cx];
            }
        }
    }

    /**
     * Rotates one plane clockwise by a quarter turn multiple through a pure index remap.
     *
     * @param src     the source buffer
     * @param srcOff  the source plane offset
     * @param sw      the source plane width
     * @param sh      the source plane height
     * @param dst     the destination buffer
     * @param dstOff  the destination plane offset
     * @param degrees the clockwise rotation; {@code 0}, {@code 90}, {@code 180}, or {@code 270}
     */
    static void rotatePlane(byte[] src, int srcOff, int sw, int sh, byte[] dst, int dstOff, int degrees) {
        switch (degrees) {
            case 0 -> System.arraycopy(src, srcOff, dst, dstOff, sw * sh);
            case 180 -> {
                for (var y = 0; y < sh; y++) {
                    for (var x = 0; x < sw; x++) {
                        dst[dstOff + (sh - 1 - y) * sw + (sw - 1 - x)] = src[srcOff + y * sw + x];
                    }
                }
            }
            case 90 -> {
                // Destination is sw rows by sh columns; dst[r][c] = src[sh - 1 - c][r].
                for (var r = 0; r < sw; r++) {
                    for (var c = 0; c < sh; c++) {
                        dst[dstOff + r * sh + c] = src[srcOff + (sh - 1 - c) * sw + r];
                    }
                }
            }
            case 270 -> {
                // Destination is sw rows by sh columns; dst[r][c] = src[c][sw - 1 - r].
                for (var r = 0; r < sw; r++) {
                    for (var c = 0; c < sh; c++) {
                        dst[dstOff + r * sh + c] = src[srcOff + c * sw + (sw - 1 - r)];
                    }
                }
            }
            default -> throw new IllegalArgumentException("degrees must be 0, 90, 180, or 270, got " + degrees);
        }
    }

    /**
     * Resamples one plane to new dimensions with a center aligned bilinear filter.
     *
     * @param src    the source buffer
     * @param srcOff the source plane offset
     * @param sw     the source plane width
     * @param sh     the source plane height
     * @param dst    the destination buffer
     * @param dstOff the destination plane offset
     * @param dw     the destination plane width
     * @param dh     the destination plane height
     */
    static void scalePlaneBilinear(byte[] src, int srcOff, int sw, int sh, byte[] dst, int dstOff, int dw, int dh) {
        for (var dy = 0; dy < dh; dy++) {
            var syf = (dy + 0.5) * sh / dh - 0.5;
            syf = syf < 0 ? 0 : Math.min(syf, sh - 1);
            var y0 = (int) syf;
            var y1 = Math.min(y0 + 1, sh - 1);
            var fy = syf - y0;
            for (var dx = 0; dx < dw; dx++) {
                var sxf = (dx + 0.5) * sw / dw - 0.5;
                sxf = sxf < 0 ? 0 : Math.min(sxf, sw - 1);
                var x0 = (int) sxf;
                var x1 = Math.min(x0 + 1, sw - 1);
                var fx = sxf - x0;
                var p00 = src[srcOff + y0 * sw + x0] & 0xFF;
                var p01 = src[srcOff + y0 * sw + x1] & 0xFF;
                var p10 = src[srcOff + y1 * sw + x0] & 0xFF;
                var p11 = src[srcOff + y1 * sw + x1] & 0xFF;
                var top = p00 + (p01 - p00) * fx;
                var bottom = p10 + (p11 - p10) * fx;
                dst[dstOff + dy * dw + dx] = (byte) clamp((int) Math.round(top + (bottom - top) * fy));
            }
        }
    }

    /**
     * Clamps a value to {@code [0, 255]}.
     *
     * @param value the value to clamp
     * @return the value clamped to {@code [0, 255]}
     */
    static int clamp(int value) {
        return value < 0 ? 0 : Math.min(value, 255);
    }

    /**
     * Returns the byte count of a packed I420 buffer for the given dimensions.
     *
     * @param width  the picture width
     * @param height the picture height
     * @return {@code width*height + 2*(width/2)*(height/2)}
     */
    static int i420ByteCount(int width, int height) {
        return width * height + 2 * (width / 2) * (height / 2);
    }

    /**
     * Validates that a dimension is even and at least {@code 2}.
     *
     * @param value the dimension to validate
     * @param name  the dimension name for the thrown message
     * @throws IllegalArgumentException if {@code value} is odd or below {@code 2}
     */
    static void requireEvenDimension(int value, String name) {
        if (value < 2 || value % 2 != 0) {
            throw new IllegalArgumentException(name + " must be even and >= 2, got " + value);
        }
    }
}
