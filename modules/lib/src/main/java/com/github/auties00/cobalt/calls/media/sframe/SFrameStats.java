package com.github.auties00.cobalt.calls.media.sframe;


/**
 * An immutable snapshot of one SFrame stream's processing counters: the frames and bytes sealed or
 * opened and the error tallies the decode path accumulates.
 *
 * <p>A {@link SFrameSecureFrame} maintains running counters as it seals and opens frames and exposes
 * them through this record, which the call telemetry layer drains into the WAM call event. The
 * counters are cumulative for the lifetime of the stream: {@link #totalFrames()} and
 * {@link #totalBytes()} count every frame and plaintext byte processed, {@link #errorFrames()}
 * counts frames the transform failed to process, and the remaining fields break the decode side
 * errors into three categories: duplicate (replayed) frames, frames whose key id had no installed
 * key, and frames rejected for an invalid parameter or malformed trailer.
 *
 * @param totalFrames        the cumulative count of frames sealed or opened
 * @param totalBytes         the cumulative count of plaintext bytes sealed or opened
 * @param errorFrames        the cumulative count of frames that failed to seal or open
 * @param duplicateFrames    the decode side count of frames rejected as replays of a seen counter
 * @param missingKeyFrames   the decode side count of frames whose key id had no installed key
 * @param invalidParamFrames the decode side count of frames rejected for a malformed trailer or
 *                           invalid parameter
 * @implNote This implementation carries only the aggregate byte total in {@link #totalBytes()} and
 * omits any per frame type byte breakdown. The WAM call event field statistics consume the SFrame
 * packet count fields (duplicate packets, missing key, rejected packets, transmit error packets),
 * which the count fields here cover, and no WAM field reads a per frame type SFrame byte sum, so a
 * per type byte breakdown would surface state no consumer reads.
 */
public record SFrameStats(
        long totalFrames,
        long totalBytes,
        long errorFrames,
        long duplicateFrames,
        long missingKeyFrames,
        long invalidParamFrames
) {
    /**
     * Returns an all zero stats snapshot, the state of a stream that has processed no frames.
     *
     * @return the empty stats snapshot
     */
    public static SFrameStats empty() {
        return new SFrameStats(0, 0, 0, 0, 0, 0);
    }
}
