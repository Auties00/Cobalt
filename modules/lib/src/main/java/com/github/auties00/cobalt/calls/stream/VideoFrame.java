package com.github.auties00.cobalt.calls.stream;

import java.util.Objects;

/**
 * Holds one frame of raw video as a planar 4:2:0 pixel buffer tagged with its layout and dimensions.
 *
 * <p>The {@code pixels} buffer stores a single picture in the layout named by {@code format}. For
 * {@link VideoPixelFormat#I420} the three planes sit back to back: the full resolution Y (luma) plane
 * first, then the half resolution U (chroma blue) plane, then the half resolution V (chroma red)
 * plane. For {@link VideoPixelFormat#NV12} the full resolution Y plane is followed by one interleaved
 * chroma plane carrying U and V samples pairwise at half resolution. Both layouts occupy the same
 * total of {@code width*height + 2*(width/2)*(height/2)} bytes, and both dimensions must be even so
 * the half resolution chroma planes have integral sizes. The compact constructor rejects any buffer
 * whose length does not match the declared dimensions.
 *
 * <p>The {@code width} and {@code height} are carried per frame rather than fixed for the lifetime of
 * a call: a single call may switch resolution as the codec follows the peer's bandwidth adaptation,
 * and the keyframe header of each encoded picture authoritatively sets the picture size. The pixel
 * buffer is referenced as supplied and never copied. A frame the call engine delivers to a consumer
 * through {@link VideoInput#readVideo()} borrows a pooled buffer owned by the producing input: the buffer is
 * valid only until the consumer reads the next frame from that same input, at which point the producer
 * may refill and re offer it, so the consumer must neither retain a reference to it past that point nor
 * mutate it. Symmetrically, a device backed {@link VideoOutput} may lend a buffer it reuses across
 * {@link VideoOutput#takeVideo()} calls, subject to the same rule against retaining or mutating it.
 *
 * @apiNote The call API accepts only the planar 4:2:0 layouts named by {@link VideoPixelFormat}; a
 * source capturing in a packed or already encoded format converts to {@link VideoPixelFormat#I420}
 * or {@link VideoPixelFormat#NV12} before publishing a frame to a {@link VideoOutput}.
 * @param pixels the planar 4:2:0 bytes laid out per {@code format}; never {@code null}, and exactly
 *               {@code width*height + 2*(width/2)*(height/2)} bytes long
 * @param format the pixel layout of {@code pixels}; never {@code null}
 * @param width  the frame width in pixels; even and at least {@code 2}
 * @param height the frame height in pixels; even and at least {@code 2}
 * @param ptsMicros the presentation timestamp in microseconds, never decreasing within a single call
 */
public record VideoFrame(byte[] pixels, VideoPixelFormat format, int width, int height, long ptsMicros) {
    /**
     * Validates the pixel buffer, the layout tag, and the frame dimensions.
     *
     * <p>Rejects a {@code null} buffer or {@code null} {@code format}, a {@code width} or {@code height}
     * that is odd or less than {@code 2}, and a buffer whose length does not equal
     * {@code width*height + 2*(width/2)*(height/2)}. Because {@link VideoPixelFormat#I420} and
     * {@link VideoPixelFormat#NV12} share that byte count, the length check is independent of which of
     * the two layouts is declared. The buffer reference is shared rather than copied; a frame the engine
     * produces from its pooled render path borrows a buffer reused for a later frame, so a consumer must
     * neither retain nor mutate it past the next {@link VideoInput#readVideo()}.
     *
     * @throws NullPointerException     if {@code pixels} or {@code format} is {@code null}
     * @throws IllegalArgumentException if {@code width} or {@code height} is odd or less than
     *                                  {@code 2}, or if {@code pixels} does not have the expected
     *                                  length for the declared dimensions
     */
    public VideoFrame {
        Objects.requireNonNull(pixels, "pixels cannot be null");
        Objects.requireNonNull(format, "format cannot be null");
        if (width < 2 || width % 2 != 0) {
            throw new IllegalArgumentException("width must be even and >= 2, got " + width);
        }
        if (height < 2 || height % 2 != 0) {
            throw new IllegalArgumentException("height must be even and >= 2, got " + height);
        }
        var expected = width * height + 2 * (width / 2) * (height / 2);
        if (pixels.length != expected) {
            throw new IllegalArgumentException(
                    "pixels must be " + expected + " bytes for " + width + "x" + height
                            + " 4:2:0 (Y + chroma), got " + pixels.length);
        }
    }
}
