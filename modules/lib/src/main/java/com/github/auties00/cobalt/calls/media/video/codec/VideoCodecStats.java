package com.github.auties00.cobalt.calls.media.video.codec;

/**
 * Holds a snapshot of one {@link VideoCodec}'s lifetime counters.
 *
 * <p>The counters separate the encode and decode directions and call out the two events the rate
 * controller and the loss recovery path react to: {@link #keyFramesEncoded()} tracks how often the
 * encoder emitted an instantaneous decoder refresh (a full intra picture), which the bandwidth
 * estimator treats as a cost spike, and {@link #keyFrameRequests()} tracks how often the decode side
 * asked for one after a loss it could not conceal. The byte sums ({@link #bytesEncoded()} and
 * {@link #bytesDecoded()}) accumulate the compressed payload bytes crossing each direction, and the
 * frame counts ({@link #framesEncoded()} and {@link #framesDecoded()}) accumulate the pictures.
 * Counters do not decrease for the lifetime of a codec instance and reset only when a fresh codec is
 * opened.
 *
 * @param framesEncoded    the total number of pictures handed to the encoder that produced output
 * @param framesDecoded    the total number of pictures the decoder reconstructed
 * @param keyFramesEncoded the total number of intra (key) pictures the encoder emitted
 * @param keyFrameRequests the total number of key frame requests the decode side raised
 * @param bytesEncoded     the total compressed payload bytes the encoder produced
 * @param bytesDecoded     the total compressed payload bytes the decoder consumed
 * @implNote This implementation surfaces the keyframe emitted and keyframe requested tallies as two
 * distinct fields rather than folding them into a single frame type histogram, because the send path
 * consults them independently: the bandwidth estimator reads the emit count, and the receiver reads the
 * request count. The byte and frame sums are accumulated on the Java side around each native encode and
 * decode call, since the underlying encoders do not expose a cumulative byte counter of their own.
 */
public record VideoCodecStats(
        long framesEncoded,
        long framesDecoded,
        long keyFramesEncoded,
        long keyFrameRequests,
        long bytesEncoded,
        long bytesDecoded
) {
    /**
     * Returns an all zero stats snapshot, the value a freshly opened codec reports before its first
     * encode or decode call.
     *
     * @return a stats snapshot with every counter at zero
     */
    public static VideoCodecStats empty() {
        return new VideoCodecStats(0, 0, 0, 0, 0, 0);
    }
}
