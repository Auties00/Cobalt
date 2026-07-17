package com.github.auties00.cobalt.calls.media.audio.neteq;

/**
 * Snapshots the lifetime counters the {@link LiveNetEq} jitter buffer accumulates, drained into call
 * telemetry at the end of a call.
 *
 * <p>The counters are denominated in rendered frames except where noted. {@link #expandedFrames()},
 * {@link #acceleratedFrames()}, {@link #preemptiveExpandedFrames()}, {@link #mergedFrames()},
 * {@link #comfortNoiseFrames()}, and {@link #normalFrames()} count how many frames each
 * {@link NetEqOperation} produced; {@link #bufferFlushes()} counts the times the buffer was flushed for
 * gross excess buffering; {@link #packetsDiscarded()} counts late or duplicate packets dropped on insert;
 * {@link #meanWaitTimeMs()} is the running mean time a packet waited in the buffer before playout; and
 * {@link #targetDelayMs()} and {@link #currentBufferMs()} report the most recent target and actual buffer
 * level. The ratio of {@link #expandedFrames()} to {@link #normalFrames()} is the headline concealment
 * rate downstream telemetry derives the audio quality score from.
 *
 * @param normalFrames             the count of frames rendered by {@link NetEqOperation#NORMAL}
 * @param expandedFrames           the count of concealment frames from {@link NetEqOperation#EXPAND}
 *                                 and {@link NetEqOperation#CODEC_PLC}
 * @param acceleratedFrames        the count of frames compressed in time by
 *                                 {@link NetEqOperation#ACCELERATE} or
 *                                 {@link NetEqOperation#FAST_ACCELERATE}
 * @param preemptiveExpandedFrames the count of frames stretched in time by
 *                                 {@link NetEqOperation#PREEMPTIVE_EXPAND}
 * @param mergedFrames             the count of frames cross faded by {@link NetEqOperation#MERGE}
 * @param comfortNoiseFrames       the count of comfort noise frames from
 *                                 {@link NetEqOperation#RFC3389_CNG} and
 *                                 {@link NetEqOperation#CODEC_INTERNAL_CNG}
 * @param bufferFlushes            the count of buffer flushes for excess buffering
 * @param packetsDiscarded         the count of late or duplicate packets dropped on insert
 * @param meanWaitTimeMs           the running mean packet wait time before playout, in milliseconds
 * @param targetDelayMs            the most recent target playout delay, in milliseconds
 * @param currentBufferMs          the most recent actual buffer level, in milliseconds
 */
public record NetEqStatistics(
        long normalFrames,
        long expandedFrames,
        long acceleratedFrames,
        long preemptiveExpandedFrames,
        long mergedFrames,
        long comfortNoiseFrames,
        long bufferFlushes,
        long packetsDiscarded,
        double meanWaitTimeMs,
        int targetDelayMs,
        int currentBufferMs
) {
    /**
     * Returns an all zero statistics snapshot for a buffer that has rendered no frames.
     *
     * @return the empty statistics snapshot
     */
    public static NetEqStatistics empty() {
        return new NetEqStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0, 0);
    }

    /**
     * Returns the concealment rate, the fraction of rendered frames that were concealed rather than
     * decoded from a real packet.
     *
     * <p>Computed as expanded frames over the sum of normal and expanded frames; returns {@code 0} before
     * any frame has been rendered. A high value indicates poor network conditions and a degraded audio
     * experience.
     *
     * @return the concealment rate in {@code [0, 1]}, or {@code 0} when no frames have been rendered
     */
    public double concealmentRate() {
        var denominator = normalFrames + expandedFrames;
        if (denominator == 0) {
            return 0.0;
        }
        return (double) expandedFrames / denominator;
    }
}
