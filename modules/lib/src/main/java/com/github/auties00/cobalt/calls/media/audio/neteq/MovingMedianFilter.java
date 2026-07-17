package com.github.auties00.cobalt.calls.media.audio.neteq;

import java.util.Arrays;
import com.github.auties00.cobalt.calls.media.video.jitter.VideoJitterEstimator;

/**
 * Reports a chosen percentile of the most recent samples within a fixed size sliding window.
 *
 * <p>The filter retains the last {@code windowSize} samples in arrival order and answers
 * {@link #filteredValue()} with the order statistic at a fixed percentile of those samples: the value
 * at rank {@code floor(percentile * (count - 1))} of the window sorted ascending, where {@code count}
 * is the number of samples seen so far, capped at the window size. A percentile of {@code 0.5} yields
 * the median, {@code 0.0} the minimum, and {@code 1.0} the maximum. Each {@link #insert(long)} appends
 * a sample and, once the window is full, evicts the oldest so the window always covers the most recent
 * {@code windowSize} insertions. Before any sample has been inserted {@link #filteredValue()} returns
 * {@code 0}.
 *
 * <p>The filter is the shared order statistic primitive the {@link VideoJitterEstimator} uses to track
 * the running percentile of encoded frame sizes, smoothing out the size spikes of keyframes so the
 * Kalman delay model is fed a representative frame magnitude rather than the raw, bursty byte count.
 * Instances are not thread safe; the single estimator thread that owns one drives all insertions and
 * reads.
 *
 * @implNote This implementation holds the live samples in an insertion order ring buffer for eviction
 * and recomputes an ascending sort of the window on each read to locate the target rank, rather than
 * maintaining an incremental ordered multiset. The window the estimator uses is small (a few tens of
 * frames) and the read cadence is one read per inserted frame, so the per read sort is not a hot path
 * and the simpler structure is preferred. The rank formula {@code floor(percentile * (count - 1))}
 * clamped to the live sample count, and the {@code percentile} validity range {@code [0, 1]} enforced
 * at construction, are the invariants that fix the reported order statistic.
 */
public final class MovingMedianFilter {
    /**
     * The maximum number of samples retained in the window.
     *
     * <p>Once this many samples have been inserted, each new insertion evicts the oldest so the window
     * always covers the most recent insertions.
     */
    private final int windowSize;

    /**
     * The percentile, in {@code [0, 1]}, whose order statistic {@link #filteredValue()} reports.
     *
     * <p>{@code 0.5} selects the median; {@code 0.0} the minimum; {@code 1.0} the maximum.
     */
    private final double percentile;

    /**
     * Fixed capacity ring buffer holding the live samples in insertion order.
     *
     * <p>The oldest live sample sits at index {@link #head}; a new sample is written at the logical tail
     * and, when the window is full, overwrites the oldest so the buffer never exceeds {@link #windowSize}
     * live entries.
     */
    private final long[] ring;

    /**
     * Index into {@link #ring} of the oldest live sample.
     */
    private int head;

    /**
     * Number of live samples currently in {@link #ring}, capped at {@link #windowSize}.
     */
    private int count;

    /**
     * Reusable scratch the live samples are copied into and sorted for each {@link #filteredValue()} read.
     */
    private final long[] sortBuf;

    /**
     * Constructs a filter over a window of the given size reporting the given percentile.
     *
     * @param windowSize the number of recent samples the window retains; must be positive
     * @param percentile the percentile whose order statistic is reported; must lie in {@code [0, 1]}
     * @throws IllegalArgumentException if {@code windowSize} is not positive or {@code percentile} is
     *                                  outside {@code [0, 1]}
     */
    public MovingMedianFilter(int windowSize, double percentile) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be positive, got " + windowSize);
        }
        if (percentile < 0.0 || percentile > 1.0) {
            throw new IllegalArgumentException("percentile must be in [0, 1], got " + percentile);
        }
        this.windowSize = windowSize;
        this.percentile = percentile;
        this.ring = new long[windowSize];
        this.sortBuf = new long[windowSize];
    }

    /**
     * Constructs a median filter over a window of the given size.
     *
     * <p>Equivalent to {@link #MovingMedianFilter(int, double)} with a percentile of {@code 0.5}.
     *
     * @param windowSize the number of recent samples the window retains; must be positive
     * @throws IllegalArgumentException if {@code windowSize} is not positive
     */
    public MovingMedianFilter(int windowSize) {
        this(windowSize, 0.5);
    }

    /**
     * Appends a sample to the window, evicting the oldest when the window is full.
     *
     * <p>The sample becomes the most recent entry; if the window already holds {@link #windowSize}
     * samples the oldest is removed first so the window covers only the most recent insertions.
     *
     * @param sample the value to insert into the window
     */
    public void insert(long sample) {
        if (count == windowSize) {
            ring[head] = sample;
            head = (head + 1) % windowSize;
        } else {
            ring[(head + count) % windowSize] = sample;
            count++;
        }
    }

    /**
     * Returns the configured percentile of the samples currently in the window.
     *
     * <p>Sorts the live samples ascending and returns the value at rank
     * {@code floor(percentile * (count - 1))}, where {@code count} is the current sample count. Returns
     * {@code 0} when the window is empty.
     *
     * @return the order statistic at the configured percentile, or {@code 0} when no sample has been
     *         inserted
     */
    public long filteredValue() {
        if (count == 0) {
            return 0;
        }
        var firstLen = Math.min(count, windowSize - head);
        System.arraycopy(ring, head, sortBuf, 0, firstLen);
        if (count > firstLen) {
            System.arraycopy(ring, 0, sortBuf, firstLen, count - firstLen);
        }
        Arrays.sort(sortBuf, 0, count);
        var rank = (int) (percentile * (count - 1));
        return sortBuf[rank];
    }

    /**
     * Returns the number of samples currently in the window.
     *
     * @return the live sample count, in {@code [0, }{@link #windowSize}{@code ]}
     */
    public int size() {
        return count;
    }

    /**
     * Removes all samples, returning the filter to its empty state.
     *
     * <p>After this call {@link #filteredValue()} reports {@code 0} until a new sample is inserted.
     */
    public void reset() {
        head = 0;
        count = 0;
    }
}
