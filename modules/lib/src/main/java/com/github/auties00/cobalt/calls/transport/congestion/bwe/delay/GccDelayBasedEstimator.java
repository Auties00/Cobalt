package com.github.auties00.cobalt.calls.transport.congestion.bwe.delay;

import com.github.auties00.cobalt.calls.transport.congestion.bwe.BandwidthEstimator;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.combine.BitrateCombiner;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.signal.CongestionSignalDetector;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * The GoogCC delay based bandwidth estimator: groups received packets by send burst, fits an
 * overuse detector trendline over their inter arrival delay, and drives an AIMD rate control from the
 * resulting classification.
 *
 * <p>Each received media packet is reported through {@link #onPacketReceived(long, long, int)} with
 * its transmit timestamp, its arrival timestamp, and its size. Packets sent within
 * {@link #BURST_TIME_MS} of the current group's first send time are coalesced into one packet group;
 * when a later packet opens a new group, the inter group send time spacing and arrival time spacing
 * are handed to the {@link TrendlineEstimator}, whose {@link BandwidthUsage} classification feeds the
 * {@link AimdRateControl}. The rate control decreases multiplicatively on overuse and increases
 * additively or multiplicatively otherwise, producing the delay based target the
 * {@link BitrateCombiner} fuses with the sender side and remote estimates. Acknowledged throughput,
 * tracked over a sliding window of received bytes, bounds the rate control candidate so the estimate
 * never runs far ahead of demonstrated capacity.
 *
 * <p>Instances are not thread safe; the call session drives one estimator from the single transport
 * thread.
 *
 * @implNote This implementation follows the GoogCC delay based pipeline: an inter arrival grouper feeds
 * a {@link TrendlineEstimator}, whose {@link BandwidthUsage} classification drives an
 * {@link AimdRateControl}. The burst grouping window {@link #BURST_TIME_MS} and the acknowledged
 * throughput window length {@link #THROUGHPUT_WINDOW_MS} are the standard WebRTC defaults; the server
 * pushed voip settings enable the delay based estimator but do not carry these window lengths.
 */
public final class GccDelayBasedEstimator implements BandwidthEstimator {
    /**
     * The logger for {@link GccDelayBasedEstimator}.
     */
    private static final System.Logger LOGGER = Log.get(GccDelayBasedEstimator.class);

    /**
     * Maximum send time spacing, in milliseconds, within which consecutive packets are coalesced into
     * one packet group.
     *
     * <p>Packets whose transmit timestamps fall within this window of the group's first send time are
     * treated as one burst, so the trendline sees per group rather than per packet deltas.
     */
    static final long BURST_TIME_MS = 5;

    /**
     * Default lower bound, in bits per second, on the delay based estimate.
     */
    static final long DEFAULT_MIN_BITRATE_BPS = 30_000;

    /**
     * Default upper bound, in bits per second, on the delay based estimate.
     */
    static final long DEFAULT_MAX_BITRATE_BPS = 8_000_000;

    /**
     * Default initial delay based estimate, in bits per second, before any overuse feedback.
     */
    static final long DEFAULT_START_BITRATE_BPS = 300_000;

    /**
     * Window duration, in milliseconds, over which received bytes are summed into acknowledged
     * throughput.
     */
    static final long THROUGHPUT_WINDOW_MS = 1000;

    /**
     * The overuse detector trendline driven by inter group delay deltas.
     */
    private final TrendlineEstimator trendline;

    /**
     * The AIMD rate control driven by the trendline classification.
     */
    private final AimdRateControl rateControl;

    /**
     * First send timestamp, in milliseconds, of the group currently being accumulated, or {@code -1}
     * when no group is open.
     */
    private long currentGroupSendMs = -1;

    /**
     * First arrival timestamp, in milliseconds, of the group currently being accumulated.
     */
    private long currentGroupArrivalMs = -1;

    /**
     * First send timestamp, in milliseconds, of the previous completed group, or {@code -1} when none.
     */
    private long previousGroupSendMs = -1;

    /**
     * First arrival timestamp, in milliseconds, of the previous completed group.
     */
    private long previousGroupArrivalMs = -1;

    /**
     * Total payload bytes accumulated in the group currently being built.
     */
    private int currentGroupBytes = 0;

    /**
     * Sum of received bytes within the current throughput window.
     */
    private long throughputWindowBytes = 0;

    /**
     * Start timestamp, in milliseconds, of the current throughput window, or {@code -1} when none.
     */
    private long throughputWindowStartMs = -1;

    /**
     * Most recently computed acknowledged throughput, in bits per second.
     */
    private long ackedThroughputBps = 0;

    /**
     * Most recent inter group arrival time spacing, in milliseconds, or {@code -1} before two groups have
     * closed.
     *
     * <p>The arrival side inter arrival timing window: the gap between the arrival of this packet group and
     * the previous one, recorded when a group closes.
     */
    private long lastArrivalDeltaMs = -1;

    /**
     * Most recent inter group send time spacing, in milliseconds, or {@code -1} before two groups have
     * closed.
     *
     * <p>The send side spacing the arrival spacing is measured against; the one way inter arrival
     * propagation variation is {@link #lastArrivalDeltaMs} minus this value.
     */
    private long lastSendDeltaMs = -1;

    /**
     * Constructs an estimator with the default bitrate bounds and start rate.
     */
    public GccDelayBasedEstimator() {
        this(DEFAULT_MIN_BITRATE_BPS, DEFAULT_MAX_BITRATE_BPS, DEFAULT_START_BITRATE_BPS);
    }

    /**
     * Constructs an estimator with explicit bitrate bounds and start rate.
     *
     * @param minBitrateBps   the lower bound on the estimate, in bits per second; must be positive
     * @param maxBitrateBps   the upper bound on the estimate, in bits per second; must be at least the
     *                        minimum
     * @param startBitrateBps the initial estimate, in bits per second; clamped into the bounds
     * @throws IllegalArgumentException if the bounds are not a positive ordered pair
     */
    public GccDelayBasedEstimator(long minBitrateBps, long maxBitrateBps, long startBitrateBps) {
        this.trendline = new TrendlineEstimator();
        this.rateControl = new AimdRateControl(minBitrateBps, maxBitrateBps);
        seedStart(startBitrateBps, minBitrateBps, maxBitrateBps);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "gcc delay based estimator: constructed, min={0} max={1} start={2}",
                    minBitrateBps, maxBitrateBps, startBitrateBps);
        }
    }

    /**
     * Seeds the rate control with the start bitrate by issuing one normal classification step.
     *
     * <p>The rate control begins at the minimum; a single {@link BandwidthUsage#NORMAL} step with the
     * start bitrate as acknowledged throughput nudges it toward the requested start without exceeding
     * the bounds.
     *
     * @param startBitrateBps the requested start estimate, in bits per second
     * @param minBitrateBps   the lower bound, in bits per second
     * @param maxBitrateBps   the upper bound, in bits per second
     */
    private void seedStart(long startBitrateBps, long minBitrateBps, long maxBitrateBps) {
        var clampedStart = Math.clamp(startBitrateBps, minBitrateBps, maxBitrateBps);
        rateControl.update(BandwidthUsage.NORMAL, clampedStart, 0);
    }

    /**
     * Reports one received media packet and advances the estimate when the packet closes a group.
     *
     * <p>Adds the packet to the running throughput window. When the packet's transmit timestamp is
     * within {@link #BURST_TIME_MS} of the open group's first send time it is coalesced into that
     * group; otherwise the open group is closed, its inter group send and arrival spacing relative to
     * the previous group are fed to the {@link TrendlineEstimator}, the resulting classification drives
     * the {@link AimdRateControl} against the current acknowledged throughput, and a new group opens at
     * this packet.
     *
     * @param sendTimeMs    the packet's transmit timestamp, in milliseconds
     * @param arrivalTimeMs the packet's arrival timestamp, in milliseconds
     * @param payloadBytes  the packet's payload size, in bytes
     */
    public void onPacketReceived(long sendTimeMs, long arrivalTimeMs, int payloadBytes) {
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "gcc delay based estimator: packet received, sendTimeMs={0} arrivalTimeMs={1} payloadBytes={2}",
                    sendTimeMs, arrivalTimeMs, payloadBytes);
        }
        updateThroughput(arrivalTimeMs, payloadBytes);
        if (currentGroupSendMs < 0) {
            openGroup(sendTimeMs, arrivalTimeMs, payloadBytes);
            return;
        }
        if (sendTimeMs - currentGroupSendMs <= BURST_TIME_MS) {
            currentGroupBytes += payloadBytes;
            return;
        }
        closeGroup(arrivalTimeMs);
        openGroup(sendTimeMs, arrivalTimeMs, payloadBytes);
    }

    /**
     * Closes the open group, feeding its inter group deltas to the trendline and rate control.
     *
     * <p>When a previous group exists the send time and arrival time spacing between the two groups
     * are handed to the {@link TrendlineEstimator}, and its classification updates the
     * {@link AimdRateControl} against the current acknowledged throughput. The closed group becomes the
     * previous group.
     *
     * @param nowMs the timestamp, in milliseconds, at which the group closes
     */
    private void closeGroup(long nowMs) {
        if (previousGroupSendMs >= 0) {
            var sendDeltaMs = (double) (currentGroupSendMs - previousGroupSendMs);
            var arrivalDeltaMs = (double) (currentGroupArrivalMs - previousGroupArrivalMs);
            lastSendDeltaMs = currentGroupSendMs - previousGroupSendMs;
            lastArrivalDeltaMs = currentGroupArrivalMs - previousGroupArrivalMs;
            var usage = trendline.update(sendDeltaMs, arrivalDeltaMs, currentGroupBytes, nowMs);
            rateControl.update(usage, ackedThroughputBps, nowMs);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "gcc delay based estimator: group closed, usage={0} sendDeltaMs={1} arrivalDeltaMs={2} target={3}",
                        usage, sendDeltaMs, arrivalDeltaMs, rateControl.currentBitrateBps());
            }
        }
        previousGroupSendMs = currentGroupSendMs;
        previousGroupArrivalMs = currentGroupArrivalMs;
    }

    /**
     * Opens a new packet group at the given packet.
     *
     * @param sendTimeMs    the packet's transmit timestamp, in milliseconds
     * @param arrivalTimeMs the packet's arrival timestamp, in milliseconds
     * @param payloadBytes  the packet's payload size, in bytes
     */
    private void openGroup(long sendTimeMs, long arrivalTimeMs, int payloadBytes) {
        currentGroupSendMs = sendTimeMs;
        currentGroupArrivalMs = arrivalTimeMs;
        currentGroupBytes = payloadBytes;
    }

    /**
     * Accumulates a received packet into the acknowledged throughput window.
     *
     * <p>Bytes are summed within a {@link #THROUGHPUT_WINDOW_MS} window; when the window elapses the
     * summed bytes are converted to a bits per second figure stored as {@link #ackedThroughputBps} and
     * a fresh window opens at the current arrival time.
     *
     * @param arrivalTimeMs the packet's arrival timestamp, in milliseconds
     * @param payloadBytes  the packet's payload size, in bytes
     */
    private void updateThroughput(long arrivalTimeMs, int payloadBytes) {
        if (throughputWindowStartMs < 0) {
            throughputWindowStartMs = arrivalTimeMs;
        }
        throughputWindowBytes += payloadBytes;
        var elapsed = arrivalTimeMs - throughputWindowStartMs;
        if (elapsed >= THROUGHPUT_WINDOW_MS) {
            ackedThroughputBps = throughputWindowBytes * 8 * 1000 / elapsed;
            throughputWindowBytes = 0;
            throughputWindowStartMs = arrivalTimeMs;
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "gcc delay based estimator: throughput window closed, ackedThroughputBps={0}",
                        ackedThroughputBps);
            }
        }
    }

    /**
     * Returns the most recently computed acknowledged throughput.
     *
     * @return the acknowledged throughput, in bits per second
     */
    public long ackedThroughputBps() {
        return ackedThroughputBps;
    }

    /**
     * Returns the most recent inter group arrival time spacing the delay based pipeline measured.
     *
     * <p>This is the arrival side inter arrival timing window: the gap, in milliseconds, between the
     * arrival of the most recently closed packet group and the group before it. It is the timing signal a
     * congestion timing check feeds on; the {@link CongestionSignalDetector} remote timing
     * ({@link CongestionSignalDetector#ENABLE_REMOTE_TIMING}) and local timing
     * ({@link CongestionSignalDetector#ENABLE_LOCAL_TIMING}) bits compare a min windowed form of this
     * timing measurement against their thresholds. Returns {@code -1} before two groups have closed.
     *
     * @return the last inter group arrival spacing, in milliseconds, or {@code -1} when none yet
     */
    public long interArrivalTimingMs() {
        return lastArrivalDeltaMs;
    }

    /**
     * Returns the most recent one way inter arrival propagation delay variation the pipeline measured.
     *
     * <p>This is the arrival spacing minus the send spacing of the last closed packet group: positive when
     * the group arrived later than its transmit cadence would imply (delay building), negative when it
     * caught up. It is the same one way delay growth the trendline integrates, exposed here as a per round
     * timing measurement. Returns {@code 0} before two groups have closed.
     *
     * @return the last inter group one way propagation delay variation, in milliseconds, or {@code 0} when
     *         none yet
     */
    public long oneWayDelayVariationMs() {
        if (lastArrivalDeltaMs < 0 || lastSendDeltaMs < 0) {
            return 0;
        }
        return lastArrivalDeltaMs - lastSendDeltaMs;
    }

    // TODO: the congestion timing bits in CongestionSignalDetector compare their thresholds against
    //  min windowed forms of the rate control stream stat struct built by the rate control state updater,
    //  NOT this GoogCC delay based estimator. Those min over window semantics are not modeled here;
    //  interArrivalTimingMs() and oneWayDelayVariationMs() expose the inter group inter arrival timing
    //  this delay based pipeline genuinely tracks (the closest faithful source), but the exact stat struct
    //  min windows the bits compare are not modeled and need the rate control stat accounting threaded in
    //  before CongestionSignalDetector.evaluate can act on the timing bits.

    /**
     * Returns the latest delay based bandwidth usage classification.
     *
     * @return the trendline's current {@link BandwidthUsage}
     */
    public BandwidthUsage bandwidthUsage() {
        return trendline.state();
    }

    /**
     * {@inheritDoc}
     *
     * @return the AIMD rate control's current estimate, in bits per second
     */
    @Override
    public long currentTargetBps() {
        return rateControl.currentBitrateBps();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation clears the inter arrival grouping and throughput window and resets
     * the {@link TrendlineEstimator} and {@link AimdRateControl}.
     */
    @Override
    public void reset() {
        trendline.reset();
        rateControl.reset();
        currentGroupSendMs = -1;
        currentGroupArrivalMs = -1;
        previousGroupSendMs = -1;
        previousGroupArrivalMs = -1;
        currentGroupBytes = 0;
        throughputWindowBytes = 0;
        throughputWindowStartMs = -1;
        ackedThroughputBps = 0;
        lastArrivalDeltaMs = -1;
        lastSendDeltaMs = -1;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "gcc delay based estimator: reset");
    }

    /**
     * Updates the upper bound the rate control clamps the estimate to.
     *
     * @param maxBitrateBps the new upper bound, in bits per second
     */
    public void setMaxBitrateBps(long maxBitrateBps) {
        rateControl.setMaxBitrateBps(maxBitrateBps);
    }
}
