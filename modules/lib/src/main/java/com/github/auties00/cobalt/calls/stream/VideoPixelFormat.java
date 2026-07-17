package com.github.auties00.cobalt.calls.stream;

/**
 * Enumerates the raw pixel layouts a {@link VideoFrame} crossing the public call API can carry.
 *
 * <p>A video source feeding a {@link VideoOutput} and a sink draining a {@link VideoInput} both
 * describe each frame's byte layout with one of these constants, so the call engine knows how to
 * interpret the frame's pixel buffer before it scales, converts color, or hands the frame to the
 * configured encoder or decoder. The set is restricted to the layouts the public surface accepts:
 * the planar {@link #I420} 4:2:0 form that lays out luma before chroma and that the WhatsApp call
 * codecs operate on natively, and the {@link #NV12} 4:2:0 form whose chroma is interleaved, which
 * several operating system cameras hand back directly. Every other internal or compressed format
 * (packed RGB, RGBA, or an H.264 access unit that is already encoded) is a concern internal to the
 * engine and is deliberately absent from this public enum; a source that captures in such a format
 * converts to {@link #I420} or {@link #NV12} before publishing a frame.
 *
 * <p>This enum is the public counterpart that names the layouts the capture and render path may
 * carry; the engine maps these public constants onto its internal format codes when it consumes or
 * produces a frame. The two are kept distinct so the exported API exposes only the layouts an
 * embedder may legitimately supply, not the full internal format table.
 *
 * @apiNote Most platform cameras expose either {@link #I420} or {@link #NV12}; when unsure, an
 * embedder should query its capture source and pick the matching constant rather than transcoding,
 * because the engine converts as needed and an unnecessary conversion on the host only adds latency.
 */
public enum VideoPixelFormat {
    /**
     * Denotes I420 planar 4:2:0, the three planes laid out as the full resolution Y (luma) plane,
     * then the half resolution U (chroma blue) plane, then the half resolution V (chroma red) plane,
     * each plane fully populated before the next begins.
     *
     * <p>This is the canonical layout the WhatsApp call audio and video stack operates on; a
     * {@link VideoFrame} declaring this format has its plane lengths validated by
     * {@link VideoFrame}'s compact constructor.
     */
    I420,

    /**
     * Denotes NV12 semiplanar 4:2:0, the full resolution Y (luma) plane followed by a single
     * interleaved chroma plane storing U and V samples pairwise at half resolution in each
     * dimension.
     *
     * <p>The total byte count matches {@link #I420} for the same dimensions; only the chroma
     * arrangement differs. Several operating system cameras deliver capture buffers in this layout,
     * so it is accepted on the public surface to spare embedders a repack on the host; the engine
     * converts to its working format internally.
     */
    NV12
}
