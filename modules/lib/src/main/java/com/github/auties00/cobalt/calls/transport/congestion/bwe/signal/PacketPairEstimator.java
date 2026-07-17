package com.github.auties00.cobalt.calls.transport.congestion.bwe.signal;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Estimates the bottleneck link capacity from packet pair dispersion and tracks a flip counter that
 * decides whether the link can sustain high definition video.
 *
 * <p>A packet pair is two packets sent back to back; the time between their arrivals (the dispersion)
 * divided into their combined size gives an instantaneous link capacity sample. {@link #onPacketPair(int,
 * long, long)} smooths these samples with an exponential moving average into the running link capacity
 * estimate, ignoring samples whose dispersion is not positive or whose arrival is older than the
 * configured window. The estimate crossing above {@link #hdHighThresholdBps} increments a flip counter
 * toward high definition capability, and crossing below {@link #hdLowThresholdBps} decrements it; once
 * the counter reaches {@link #FLIP_COUNT_FOR_HD} the link is reported high definition capable, giving
 * the capability hysteresis so a single sample does not toggle it.
 *
 * <p>The high and low high definition thresholds are supplied by the caller through the constructor
 * rather than held as constants here, because they are configured per session.
 *
 * <p>Instances are not thread safe; the owning sender estimator drives one estimator from the single
 * transport thread.
 *
 * @implNote This implementation smooths raw capacity samples with a single pole exponential moving
 * average keyed by {@link #LINK_CAPACITY_ALPHA} rather than a windowed maximum, and guards the
 * high definition decision with a symmetric flip counter so a lone sample on either side of a threshold
 * cannot toggle the reported capability.
 */
public final class PacketPairEstimator {
    /**
     * The logger for {@link PacketPairEstimator}.
     */
    private static final System.Logger LOGGER = Log.get(PacketPairEstimator.class);

    /**
     * Smoothing factor for the exponential moving average over packet pair capacity samples.
     */
    static final double LINK_CAPACITY_ALPHA = 0.1;

    /**
     * Flip counter magnitude at which the link is reported high definition capable.
     *
     * <p>The counter rises toward this on samples above the high threshold and falls toward its
     * negative on samples below the low threshold, giving the capability hysteresis.
     */
    static final int FLIP_COUNT_FOR_HD = 3;

    /**
     * Maximum age, in milliseconds, of a packet pair sample relative to the window head before it is
     * ignored as stale.
     */
    static final long SAMPLE_WINDOW_MS = 5_000;

    /**
     * Link capacity estimate, in bits per second, above which the flip counter rises toward
     * high definition capability.
     */
    private final long hdHighThresholdBps;

    /**
     * Link capacity estimate, in bits per second, below which the flip counter falls away from
     * high definition capability.
     */
    private final long hdLowThresholdBps;

    /**
     * Running smoothed link capacity estimate, in bits per second, or {@code 0} when not yet seeded.
     */
    private long linkCapacityBps = 0;

    /**
     * Arrival timestamp, in milliseconds, of the most recently accepted sample, or {@code -1} when
     * none.
     *
     * <p>Used to reject samples older than {@link #SAMPLE_WINDOW_MS} relative to the latest.
     */
    private long lastSampleMs = -1;

    /**
     * High definition flip counter, clamped to {@code [-FLIP_COUNT_FOR_HD, FLIP_COUNT_FOR_HD]}.
     *
     * <p>Reaching {@link #FLIP_COUNT_FOR_HD} reports high definition capability; reaching its negative
     * reports incapability.
     */
    private int flipCount = 0;

    /**
     * Constructs a packet pair estimator with the high definition capability thresholds.
     *
     * @param hdHighThresholdBps the estimate above which the flip counter rises, in bits per second
     * @param hdLowThresholdBps  the estimate below which the flip counter falls, in bits per second
     */
    public PacketPairEstimator(long hdHighThresholdBps, long hdLowThresholdBps) {
        this.hdHighThresholdBps = hdHighThresholdBps;
        this.hdLowThresholdBps = hdLowThresholdBps;
    }

    /**
     * Reports one packet pair and updates the link capacity estimate and flip counter.
     *
     * <p>Ignores a pair whose dispersion is not positive or whose arrival is older than
     * {@link #SAMPLE_WINDOW_MS} relative to the latest accepted sample. Otherwise computes the
     * instantaneous capacity as the combined size in bits divided by the dispersion, blends it into the
     * running estimate with {@link #LINK_CAPACITY_ALPHA}, then steps the flip counter up when the
     * estimate is above the high threshold and down when it is below the low threshold.
     *
     * @param combinedBytes the combined size of the two packets, in bytes
     * @param dispersionMs  the arrival time gap between the two packets, in milliseconds; ignored when
     *                      not positive
     * @param arrivalMs     the arrival timestamp of the second packet, in milliseconds
     */
    public void onPacketPair(int combinedBytes, long dispersionMs, long arrivalMs) {
        if (dispersionMs <= 0) {
            return;
        }
        if (lastSampleMs >= 0 && arrivalMs - lastSampleMs > SAMPLE_WINDOW_MS) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "packet pair estimator window reset: gap={0}ms", arrivalMs - lastSampleMs);
            }
            linkCapacityBps = 0;
            flipCount = 0;
        }
        lastSampleMs = arrivalMs;
        var instantBps = (long) combinedBytes * 8 * 1000 / dispersionMs;
        if (linkCapacityBps == 0) {
            linkCapacityBps = instantBps;
        } else {
            linkCapacityBps += (long) (LINK_CAPACITY_ALPHA * (instantBps - linkCapacityBps));
        }
        var wasHdCapable = flipCount >= FLIP_COUNT_FOR_HD;
        if (linkCapacityBps > hdHighThresholdBps) {
            flipCount = Math.min(FLIP_COUNT_FOR_HD, flipCount + 1);
        } else if (linkCapacityBps < hdLowThresholdBps) {
            flipCount = Math.max(-FLIP_COUNT_FOR_HD, flipCount - 1);
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "packet pair sample: instant={0}bps capacity={1}bps flip={2}",
                    instantBps, linkCapacityBps, flipCount);
        }
        var isHdCapable = flipCount >= FLIP_COUNT_FOR_HD;
        if (Log.DEBUG && wasHdCapable != isHdCapable) {
            LOGGER.log(Level.DEBUG, "hd capability changed: capable={0} capacity={1}bps", isHdCapable, linkCapacityBps);
        }
    }

    /**
     * Returns the running link capacity estimate.
     *
     * @return the link capacity estimate, in bits per second, or {@code 0} when not yet seeded
     */
    public long linkCapacityBps() {
        return linkCapacityBps;
    }

    /**
     * Returns whether the link is currently reported high definition capable.
     *
     * @return {@code true} once the flip counter has reached {@link #FLIP_COUNT_FOR_HD}
     */
    public boolean isHdCapable() {
        return flipCount >= FLIP_COUNT_FOR_HD;
    }

    /**
     * Returns the current high definition flip counter.
     *
     * @return the flip counter, in {@code [-FLIP_COUNT_FOR_HD, FLIP_COUNT_FOR_HD]}
     */
    public int flipCount() {
        return flipCount;
    }

    /**
     * Resets the estimator, clearing the link capacity estimate, the flip counter, and the sample
     * timestamp.
     */
    public void reset() {
        linkCapacityBps = 0;
        lastSampleMs = -1;
        flipCount = 0;
    }
}
