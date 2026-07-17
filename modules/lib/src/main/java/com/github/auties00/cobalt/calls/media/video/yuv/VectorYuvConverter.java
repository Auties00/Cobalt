package com.github.auties00.cobalt.calls.media.video.yuv;

import com.github.auties00.cobalt.calls.stream.VideoFrame;
import com.github.auties00.cobalt.telemetry.log.Log;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * SIMD {@link YuvConverter} backend built on the JDK Vector API.
 *
 * <p>This implementation references the {@code jdk.incubator.vector} incubator module, so it can only link
 * when that module is present in the runtime image. {@link YuvConverter#create()} names it directly but
 * constructs it only after proving the module is present; merely naming the class forces the verifier to
 * load it, not to link the incubator types in its descriptors, so an image without the module never resolves
 * them and the selector falls back to {@link ScalarYuvConverter}.
 *
 * <p>Only {@link #toArgb(VideoFrame)}, the highest frequency conversion on the render path, runs a SIMD
 * kernel; it expands the half resolution chroma to full resolution once per row so every lane is independent,
 * then applies the BT.601 fixed point matrix across whole {@linkplain VectorSpecies#length() vector lengths}
 * and finishes the trailing tail elements with {@link ScalarYuvConverter#yuvToArgb(int, int, int)}. Every
 * other operation delegates to the shared {@link ScalarYuvConverter} frame helpers, so the two backends
 * produce identical geometry, orientation, and chroma repacking, and the SIMD color kernel is written to
 * match the scalar color kernel exactly.
 *
 * @implNote This implementation mirrors {@link ScalarYuvConverter#yuvToArgb(int, int, int)} lanewise, with
 * the coefficients pre broadcast into constant {@link IntVector}s so each stage is a
 * {@link IntVector#lanewise(VectorOperators.Binary, jdk.incubator.vector.Vector) lanewise} operation against
 * a vector operand.
 */
final class VectorYuvConverter implements YuvConverter {
    /**
     * The preferred hardware integer vector species used for the SIMD color kernel.
     */
    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    /**
     * The {@code 0x0101} luma byte replication multiplier, broadcast to every lane.
     */
    private static final IntVector C257 = IntVector.broadcast(SPECIES, 0x0101);

    /**
     * The luma scale, broadcast to every lane.
     */
    private static final IntVector YG = IntVector.broadcast(SPECIES, 18997);

    /**
     * The U to blue coefficient, broadcast to every lane.
     */
    private static final IntVector UB = IntVector.broadcast(SPECIES, -128);

    /**
     * The U to green coefficient, broadcast to every lane.
     */
    private static final IntVector UG = IntVector.broadcast(SPECIES, 25);

    /**
     * The V to green coefficient, broadcast to every lane.
     */
    private static final IntVector VG = IntVector.broadcast(SPECIES, 52);

    /**
     * The V to red coefficient, broadcast to every lane.
     */
    private static final IntVector VR = IntVector.broadcast(SPECIES, -102);

    /**
     * The blue rounding and bias term, broadcast to every lane.
     */
    private static final IntVector BB = IntVector.broadcast(SPECIES, -128 * 128 + -1160);

    /**
     * The green rounding and bias term, broadcast to every lane.
     */
    private static final IntVector BG = IntVector.broadcast(SPECIES, 25 * 128 + 52 * 128 + -1160);

    /**
     * The red rounding and bias term, broadcast to every lane.
     */
    private static final IntVector BR = IntVector.broadcast(SPECIES, -102 * 128 + -1160);

    /**
     * The opaque alpha byte in {@code 0xAARRGGBB} position, broadcast to every lane.
     */
    private static final IntVector ALPHA = IntVector.broadcast(SPECIES, 0xFF000000);

    /**
     * Zero, broadcast to every lane, the lower clamp bound.
     */
    private static final IntVector ZERO = IntVector.broadcast(SPECIES, 0);

    /**
     * {@code 255}, broadcast to every lane, the upper clamp bound.
     */
    private static final IntVector MAX255 = IntVector.broadcast(SPECIES, 255);

    /**
     * The logger for {@link VectorYuvConverter}.
     */
    private static final System.Logger LOGGER = Log.get(VectorYuvConverter.class);

    /**
     * Creates the vector backend.
     *
     * <p>The field initialisers above are what touch the Vector API, so a host without the incubator module
     * fails during class initialisation and is caught by {@link YuvConverter#create()}.
     */
    VectorYuvConverter() {

    }

    @Override
    public int[] toArgb(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        var i420 = ScalarYuvConverter.toI420Frame(frame);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "vector yuv converter: toArgb, width={0} height={1}", i420.width(), i420.height());
        }
        return i420ToArgb(i420.pixels(), i420.width(), i420.height());
    }

    @Override
    public VideoFrame argbToI420(int[] argb, int width, int height, long ptsMicros) {
        return ScalarYuvConverter.argbToI420Frame(argb, width, height, ptsMicros);
    }

    @Override
    public VideoFrame toI420(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        return ScalarYuvConverter.toI420Frame(frame);
    }

    @Override
    public VideoFrame toNv12(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        return ScalarYuvConverter.toNv12Frame(frame);
    }

    @Override
    public VideoFrame scale(VideoFrame src, int dstWidth, int dstHeight) {
        Objects.requireNonNull(src, "src cannot be null");
        ScalarYuvConverter.requireEvenDimension(dstWidth, "dstWidth");
        ScalarYuvConverter.requireEvenDimension(dstHeight, "dstHeight");
        return ScalarYuvConverter.scaleFrame(src, dstWidth, dstHeight);
    }

    @Override
    public VideoFrame rotate(VideoFrame src, int degrees) {
        Objects.requireNonNull(src, "src cannot be null");
        return ScalarYuvConverter.rotateFrame(src, degrees);
    }

    /**
     * Converts a packed I420 buffer to packed ARGB with a SIMD color kernel over full resolution chroma.
     *
     * @param i420   the packed I420 planes: Y, then U, then V
     * @param width  the picture width
     * @param height the picture height
     * @return the packed ARGB pixels in {@code 0xAARRGGBB} order
     */
    private static int[] i420ToArgb(byte[] i420, int width, int height) {
        var chromaWidth = width / 2;
        var lumaSize = width * height;
        var chromaSize = chromaWidth * (height / 2);
        var uOffset = lumaSize;
        var vOffset = lumaSize + chromaSize;
        var argb = new int[width * height];
        var yRow = new int[width];
        var uRow = new int[width];
        var vRow = new int[width];
        var bound = SPECIES.loopBound(width);
        for (var y = 0; y < height; y++) {
            var lumaRow = y * width;
            var chromaRow = (y >> 1) * chromaWidth;
            for (var x = 0; x < width; x++) {
                yRow[x] = i420[lumaRow + x] & 0xFF;
                var chromaIndex = chromaRow + (x >> 1);
                uRow[x] = i420[uOffset + chromaIndex] & 0xFF;
                vRow[x] = i420[vOffset + chromaIndex] & 0xFF;
            }
            var x = 0;
            for (; x < bound; x += SPECIES.length()) {
                var yv = IntVector.fromArray(SPECIES, yRow, x);
                var uv = IntVector.fromArray(SPECIES, uRow, x);
                var vv = IntVector.fromArray(SPECIES, vRow, x);
                var y1 = yv.lanewise(VectorOperators.MUL, C257)
                        .lanewise(VectorOperators.MUL, YG)
                        .lanewise(VectorOperators.LSHR, 16);
                var b = clamp(y1.lanewise(VectorOperators.SUB, uv.lanewise(VectorOperators.MUL, UB))
                        .lanewise(VectorOperators.ADD, BB)
                        .lanewise(VectorOperators.ASHR, 6));
                var g = clamp(y1.lanewise(VectorOperators.SUB, uv.lanewise(VectorOperators.MUL, UG))
                        .lanewise(VectorOperators.SUB, vv.lanewise(VectorOperators.MUL, VG))
                        .lanewise(VectorOperators.ADD, BG)
                        .lanewise(VectorOperators.ASHR, 6));
                var r = clamp(y1.lanewise(VectorOperators.SUB, vv.lanewise(VectorOperators.MUL, VR))
                        .lanewise(VectorOperators.ADD, BR)
                        .lanewise(VectorOperators.ASHR, 6));
                var pixel = r.lanewise(VectorOperators.LSHL, 16)
                        .lanewise(VectorOperators.OR, g.lanewise(VectorOperators.LSHL, 8))
                        .lanewise(VectorOperators.OR, b)
                        .lanewise(VectorOperators.OR, ALPHA);
                pixel.intoArray(argb, lumaRow + x);
            }
            for (; x < width; x++) {
                argb[lumaRow + x] = ScalarYuvConverter.yuvToArgb(yRow[x], uRow[x], vRow[x]);
            }
        }
        return argb;
    }

    /**
     * Clamps every lane to {@code [0, 255]}.
     *
     * @param vector the vector to clamp
     * @return the clamped vector
     */
    private static IntVector clamp(IntVector vector) {
        return vector.lanewise(VectorOperators.MAX, ZERO)
                .lanewise(VectorOperators.MIN, MAX255);
    }
}
